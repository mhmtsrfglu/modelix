package org.modelix.jobs

import org.modelix.workspaces.Workspace
import java.io.File

class WorkspaceBuildJob(val workspace: Workspace, val downloadFile: File) {
    var status: WorkspaceBuildStatus = WorkspaceBuildStatus.New
    var output: List<String> = ArrayList()
    val outputHandler: (String)->Unit = { append(it) }
    var lastOutput: Long = 0

    fun appendException(e: Throwable) {
        append(e::class.qualifiedName + ": " + e.message)
        e.stackTrace.map { "  $it" }.forEach { append(it) }
    }

    inline fun runSafely(statusOnException: WorkspaceBuildStatus? = null, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            appendException(e)
            if (statusOnException != null) status = statusOnException
        }
    }

    fun append(text: String) {
        output += text
        lastOutput = System.currentTimeMillis()
    }
}

