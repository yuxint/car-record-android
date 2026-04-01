package com.tx.carrecord.core.datastore.logging

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class AppLogFileStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val appContext = context.applicationContext
    private val maxFileSizeInBytes = 10 * 1024 * 1024
    private val logsDirectory = File(appContext.filesDir, "logs")
    private val logFileName = "app.log"
    private val dateTimeFormatter = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd HH:mm:ss.SSS",
        Locale.getDefault(),
    )

    suspend fun filePath(): String = withContext(Dispatchers.IO) {
        resolveLogFile().absolutePath
    }

    suspend fun appendLine(line: String) = withContext(Dispatchers.IO) {
        val logFile = resolveLogFile()
        ensureFileExists(logFile)

        val normalizedLine = if (line.endsWith('\n')) line else "$line\n"
        val existingText = logFile.takeIf { it.exists() }?.readText(Charsets.UTF_8).orEmpty()
        val mergedText = buildString {
            append(existingText)
            if (existingText.isNotEmpty() && existingText.endsWith('\n').not()) {
                append('\n')
            }
            append(normalizedLine)
        }
        logFile.writeText(trimToMaxSize(mergedText), Charsets.UTF_8)
    }

    suspend fun readAll(): String = withContext(Dispatchers.IO) {
        val logFile = resolveLogFile()
        if (logFile.exists().not()) {
            return@withContext ""
        }
        logFile.readText(Charsets.UTF_8)
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        val logFile = resolveLogFile()
        ensureFileExists(logFile)
        logFile.writeText("", Charsets.UTF_8)
    }

    suspend fun currentFileSizeInBytes(): Int = withContext(Dispatchers.IO) {
        val logFile = resolveLogFile()
        if (logFile.exists().not()) {
            return@withContext 0
        }
        logFile.length().toInt()
    }

    fun formatDateTime(now: LocalDateTime = LocalDateTime.now()): String {
        return dateTimeFormatter.format(now)
    }

    private fun resolveLogFile(): File = File(logsDirectory, logFileName)

    private fun ensureFileExists(logFile: File) {
        if (logsDirectory.exists().not()) {
            logsDirectory.mkdirs()
        }
        if (logFile.exists().not()) {
            logFile.createNewFile()
        }
    }

    private fun trimToMaxSize(content: String): String {
        val contentBytes = content.toByteArray(Charsets.UTF_8)
        if (contentBytes.size <= maxFileSizeInBytes) {
            return content
        }

        val lines = content
            .split('\n')
            .filter { it.isNotEmpty() }
            .asReversed()

        val retainedLines = ArrayDeque<String>()
        var retainedSize = 0

        for (line in lines) {
            val lineBytes = (line + '\n').toByteArray(Charsets.UTF_8)
            if (retainedLines.isNotEmpty() && retainedSize + lineBytes.size > maxFileSizeInBytes) {
                break
            }
            retainedLines.addFirst(line)
            retainedSize += lineBytes.size
        }

        if (retainedLines.isEmpty()) {
            val startIndex = contentBytes.size - maxFileSizeInBytes
            return String(contentBytes.copyOfRange(startIndex, contentBytes.size), Charsets.UTF_8)
        }

        return retainedLines.joinToString(separator = "\n", postfix = "\n")
    }
}
