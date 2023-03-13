package org.modelix.managers

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.GitCommand
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.transport.Transport
import org.eclipse.jgit.transport.TransportHttp
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.transport.http.HttpConnection
import org.eclipse.jgit.transport.http.HttpConnectionFactory
import org.eclipse.jgit.transport.http.JDKHttpConnection
import org.eclipse.jgit.transport.http.JDKHttpConnectionFactory
import org.modelix.utils.copyFiles
import org.modelix.workspaces.GitRepository
import java.io.File
import java.io.OutputStream
import java.net.*
import java.util.zip.ZipOutputStream

class GitRepositoryManager(val config: GitRepository, val workspaceDirectory: File) {
    val repoDirectory = File(workspaceDirectory, "git-" + toValidFileName(config.url))

    fun toValidFileName(text: String): String {
        val allowed = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-., ".toSet()
        return text.map { if (allowed.contains(it)) it else '_' }.joinToString("")
    }

    fun updateRepo(): String {
        val existed = repoDirectory.exists()
        val git = openRepo()
        if (existed) {
            applyCredentials(git.fetch()).call()
            git.checkout().setName(config.commitHash ?: ("origin/" + config.branch)).call()
//            if (config.commitHash == null) {
//                applyCredentials(git.pull()).call()
//            }
        }
        return git.repository.exactRef("HEAD").objectId.name
    }

    private fun openRepo(): Git {
        if (repoDirectory.exists()) {
            try {
                return Git.open(repoDirectory)
            } catch (e: RepositoryNotFoundException) {
                return cloneRepo()
            }
        } else {
            return cloneRepo()
        }
    }

    private fun <C : GitCommand<T>, T, E : TransportCommand<C, T>> applyCredentials(cmd: E): E {
        val encryptedCredentials = config.credentials
        if (encryptedCredentials != null) {
            val decrypted = encryptedCredentials.decrypt()
            cmd.setCredentialsProvider(UsernamePasswordCredentialsProvider(decrypted.user, decrypted.password))
            cmd.setTransportConfigCallback { transport ->
                transport?.setAuthenticator(object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(decrypted.user, decrypted.password.toCharArray())
                    }
                })
            }
        }
        return cmd
    }

    /**
     * The credentialsProvider only works with WWW-Authenticate: Basic, but not with WWW-Authenticate: Negotiate.
     * This is handled by the JDK.
     */
    private fun Transport.setAuthenticator(authenticator: Authenticator) {
        val transport = this as TransportHttp
        val originalFactory = transport.httpConnectionFactory as JDKHttpConnectionFactory
        transport.httpConnectionFactory = object : HttpConnectionFactory {
            override fun create(url: URL?): HttpConnection {
                return modify(originalFactory.create(url))
            }

            override fun create(url: URL?, proxy: Proxy?): HttpConnection {
                return modify(originalFactory.create(url, proxy))
            }

            fun modify(conn: HttpConnection): HttpConnection {
                val jdkConn = conn as JDKHttpConnection
                val field = jdkConn.javaClass.getDeclaredField("wrappedUrlConnection")
                field.isAccessible = true
                val wrapped = field.get(jdkConn) as HttpURLConnection
                wrapped.setAuthenticator(authenticator)
                return conn
            }
        }
    }

    private fun cloneRepo(): Git {
        val cmd = Git.cloneRepository()
        applyCredentials(cmd)
        cmd.setURI(config.url)
        if (config.branch != null) {
            cmd.setBranch(config.branch)
        }
        val directory = repoDirectory
        directory.mkdirs()
        cmd.setDirectory(directory)
        return cmd.call()
    }

    fun zip(roots: List<File>, outputStream: OutputStream) {
        if (outputStream is ZipOutputStream) throw IllegalArgumentException()
        ZipOutputStream(outputStream).use { output ->
            zip(roots, output)
        }
    }


    fun zip(subfolders: List<String>?, output: ZipOutputStream, includeGitDir: Boolean) {
        updateRepo()
        val roots: List<File> = getRootFolders(subfolders)
        val gitDir = File(repoDirectory, ".git").toPath()
        roots.filter { it.exists() }.forEach { root ->
            output.copyFiles(
                inputDir = root,
                filter = { !it.startsWith(gitDir) },
                mapPath = { workspaceDirectory.toPath().relativize(it) },
                extractZipFiles = false
            )
        }
        if (includeGitDir) {
            output.copyFiles(
                inputDir = gitDir.toFile(),
                mapPath = { workspaceDirectory.toPath().relativize(it) },
                extractZipFiles = false
            )
        }
    }

    fun getRootFolders(subfolders: List<String>?) =
        if (subfolders == null || subfolders.isEmpty()) {
            listOf(repoDirectory)
        } else {
            subfolders.map { File(repoDirectory, it) }
        }
}