package org.modelix.utils

import org.zeroturnaround.zip.ZipUtil
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.extension
import kotlin.io.path.isDirectory

fun ZipOutputStream.copyFiles(inputDir: File,
                              filter: (Path) -> Boolean = { true },
                              mapPath: (Path) -> Path = { inputDir.toPath().relativize(it) },
                              extractZipFiles: Boolean = true) {
    Files.walk(inputDir.toPath()).filter { !it.isDirectory() && filter(it) }.forEach { inputFile ->
        if (extractZipFiles && inputFile.extension.lowercase() == "zip") {
            copyZipContent(inputFile.toFile(), mapPath(inputFile))
        } else {
            FileInputStream(inputFile.toFile()).use { input ->
                copyFile(input, mapPath(inputFile))
            }
        }

    }
}

private fun ZipOutputStream.copyFile(input: InputStream, outputPath: Path) {
    BufferedInputStream(input).use { origin ->
        val entry = ZipEntry(outputPath.toString())
        putNextEntry(entry)
        origin.copyTo(this, 1024)
    }
}

fun ZipOutputStream.copyZipContent(input: File, outputDir: Path) {
    ZipUtil.iterate(input) {stream, entry ->
        if (entry.isDirectory) return@iterate
        copyFile(stream, outputDir.resolve(entry.name))
    }
}
