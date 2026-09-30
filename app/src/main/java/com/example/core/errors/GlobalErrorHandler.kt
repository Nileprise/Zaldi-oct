package com.example.core.errors

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DiagnosticSeverity {
    INFO,
    SOCKET,
    WARN,
    ERROR
}

data class DiagnosticEvent(
    val id: Long = System.nanoTime(),
    val timestamp: String,
    val module: String,
    val severity: DiagnosticSeverity,
    val message: String,
    val details: String? = null
)

/**
 * Global exception handler and telemetry ring-buffer (`core/errors`).
 * Captures network failures, WebSocket state changes, GPS accuracy alerts, and uncaught exceptions.
 */
object GlobalErrorHandler {
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val _events = MutableStateFlow<List<DiagnosticEvent>>(
        listOf(
            DiagnosticEvent(
                timestamp = timeFormat.format(Date()),
                module = "core/init",
                severity = DiagnosticSeverity.INFO,
                message = "Zaldi Driver Core initialized",
                details = "SQLite Room storage & Geohash engine ready"
            )
        )
    )
    val events: StateFlow<List<DiagnosticEvent>> = _events.asStateFlow()

    fun logEvent(
        module: String,
        severity: DiagnosticSeverity,
        message: String,
        details: String? = null
    ) {
        val entry = DiagnosticEvent(
            timestamp = timeFormat.format(Date()),
            module = module,
            severity = severity,
            message = message,
            details = details
        )
        _events.value = (listOf(entry) + _events.value).take(60)
    }

    fun recordException(module: String, throwable: Throwable, contextNote: String = "") {
        logEvent(
            module = module,
            severity = DiagnosticSeverity.ERROR,
            message = if (contextNote.isNotBlank()) "$contextNote: ${throwable.message}" else (throwable.message ?: "Unexpected error"),
            details = throwable.javaClass.simpleName
        )
    }

    fun clearLogs() {
        _events.value = emptyList()
    }
}
