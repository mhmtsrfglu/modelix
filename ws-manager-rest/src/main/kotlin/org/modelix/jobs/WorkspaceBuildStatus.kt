package org.modelix.jobs

enum class WorkspaceBuildStatus {
    New, // The job was created, but is not yet queued for building
    Queued, // The job is queued for building, but there is currently some other job running.
    Running, // The job is currently executed.
    FailedBuild, // If the build failed we still create the ZIP to let users fix the modules.
    FailedZip, // Failed to create the ZIP file. There is nothing to download.
    AllSuccessful, // The modules were built successfully and the ZIP is ready for download.
    ZipSuccessful // The build failed, but the ZIP is ready for download.
}