package com.tx.carrecord.core.datastore.logging

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLogger @Inject constructor(
    private val logFileStore: AppLogFileStore,
) {
    suspend fun info(
        message: String,
        payload: Any? = null,
    ) {
        append(
            level = AppLogLevel.INFO,
            message = message,
            payload = payload,
        )
    }

    suspend fun warn(
        message: String,
        payload: Any? = null,
    ) {
        append(
            level = AppLogLevel.WARN,
            message = message,
            payload = payload,
        )
    }

    suspend fun error(
        message: String,
        payload: Any? = null,
        includeStack: Boolean = true,
    ) {
        append(
            level = AppLogLevel.ERROR,
            message = message,
            payload = payload,
            includeStack = includeStack,
        )
    }

    private suspend fun append(
        level: AppLogLevel,
        message: String,
        payload: Any? = null,
        includeStack: Boolean = false,
    ) {
        val caller = resolveCaller()
        val formattedLine = formatLine(
            level = level,
            message = message,
            payload = payload,
            includeStack = includeStack,
            caller = caller,
        )
        logFileStore.appendLine(formattedLine)
    }

    private fun formatLine(
        level: AppLogLevel,
        message: String,
        payload: Any?,
        includeStack: Boolean,
        caller: StackTraceElement,
    ): String {
        val timestamp = logFileStore.formatDateTime()
        val fileName = caller.fileName ?: caller.className.substringAfterLast('.')
        val fields = mutableListOf(
            timestamp,
            "[${level.displayTitle}]",
            "[$fileName:${caller.lineNumber}]",
            "[${caller.methodName}]",
            message,
        )

        if (payload != null) {
            fields += "payload=$payload"
        }

        if (includeStack) {
            val stack = Thread.currentThread().stackTrace
                .dropWhile { element ->
                    element.className.contains(AppLogger::class.java.simpleName) ||
                        element.className.contains(AppLogFileStore::class.java.simpleName) ||
                        element.className.startsWith("java.lang.Thread")
                }
                .drop(1)
                .take(8)
                .joinToString(separator = " <- ")
            if (stack.isNotEmpty()) {
                fields += "stack=$stack"
            }
        }

        return fields.joinToString(separator = " ")
    }

    private fun resolveCaller(): StackTraceElement {
        val stackTrace = Throwable().stackTrace
        return stackTrace.firstOrNull { element ->
            element.className != AppLogger::class.java.name &&
                element.className != AppLogFileStore::class.java.name &&
                element.className.startsWith("kotlin.coroutines").not() &&
                element.className.startsWith("java.lang.Thread").not()
        } ?: stackTrace.first()
    }
}
