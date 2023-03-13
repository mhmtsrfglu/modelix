package org.modelix.managers

import org.apache.commons.io.FileUtils
import org.apache.maven.shared.invoker.DefaultInvocationRequest
import org.apache.maven.shared.invoker.DefaultInvoker
import org.apache.maven.shared.invoker.InvocationOutputHandler
import org.modelix.workspaces.Workspace
import org.zeroturnaround.zip.ZipUtil
import java.io.File
import java.util.*

class MavenDownloader(val workspace: Workspace, val workspaceDir: File) {

    fun downloadAndCopyFromMaven(coordinates: String, outputHandler: ((String)->Unit)? = null): File {
        if (workspace.mavenRepositories.isNotEmpty()) {
            downloadFromMaven(coordinates, outputHandler)
        }
        deleteFilesForcingOnlineMode()
        return copyArtifacts(coordinates, outputHandler)
    }

    fun copyArtifacts(coordinates: String, outputHandler: ((String)->Unit)? = null): File {
        val request = DefaultInvocationRequest()
        request.isOffline = true
        request.goals = listOf("dependency:copy")
        request.isBatchMode = true
        val outputDir = File(workspaceDir, "maven-" + coordinates.replace(Regex("[^a-zA-Z0-9.]"), "_"))
        if (outputDir.exists()) FileUtils.deleteDirectory(outputDir)
        outputDir.mkdirs()
        val properties = Properties()
        properties["outputDirectory"] = outputDir.absolutePath
        properties["artifact"] = addPackagingIfMissing(coordinates)
        request.properties = properties

        invokeMaven(request, outputHandler?.let { { outputHandler(it) } })
        outputDir.listFiles()?.forEach { child ->
            if (child.isFile && child.extension.lowercase() == "zip") {
                ZipUtil.unpack(child, outputDir)
                FileUtils.deleteQuietly(child)
            }
        }
        return outputDir
    }

    fun downloadFromMaven(coordinates: String, outputHandler: ((String)->Unit)? = null) {
        val request = DefaultInvocationRequest()
        request.goals = listOf("dependency:get")
        request.isBatchMode = true
        val properties = Properties()
        properties["remoteRepositories"] = workspace.mavenRepositories.joinToString(",") { it.url }
        properties["transitive"] = "false"
        properties["artifact"] = addPackagingIfMissing(coordinates)
        request.properties = properties

        invokeMaven(request, outputHandler?.let { { outputHandler(it) } })
    }

    private fun invokeMaven(request: DefaultInvocationRequest, outputHandler: InvocationOutputHandler?) {
        val invoker = DefaultInvoker()
        val candidates = mutableListOf(File("/usr/share/maven"))
        File("/usr/local/Cellar/maven/").listFiles()?.let { candidates += it }
        invoker.mavenHome = candidates.firstOrNull { it.exists() } ?: throw RuntimeException("maven not found in $candidates")
        if (outputHandler != null) invoker.setOutputHandler(outputHandler)
        invoker.execute(request)
    }

    private fun addPackagingIfMissing(coordinates: String): String {
        return if (coordinates.split(":").size == 3) coordinates + ":zip" else coordinates
    }

    private fun deleteFilesForcingOnlineMode() {
        // Delete all .repositories and.sha1 files to avoid requiring an internet connection
        // https://manios.org/2019/08/21/force-maven-offline-execute-goal-dependencies
        val mavenHome = File(System.getProperty("user.home")).resolve(".m2")
        mavenHome.walk()
            .filter { it.isFile && (it.extension == "repositories" || it.extension == "sha1") }
            .forEach { it.delete() }
    }
}