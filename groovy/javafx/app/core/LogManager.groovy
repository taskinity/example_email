// app/core/LogManager.groovy - zarządzanie logami
package app.core

import java.text.SimpleDateFormat
import java.util.concurrent.ConcurrentLinkedQueue
import java.io.File

class LogManager {
    def outputCallback
    def statusCallback
    def logQueue = new ConcurrentLinkedQueue()
    def logLevel = 'INFO'
    def logFile
    def dateFormat = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss.SSS')

    // Log levels
    def LOG_LEVELS = [
        'DEBUG': 0,
        'INFO': 1,
        'WARNING': 2,
        'ERROR': 3
    ]

    LogManager() {
        setupLogFile()
        startLogProcessor()
    }

    def setupLogFile() {
        def logDir = new File('logs')
        if (!logDir.exists()) {
            logDir.mkdirs()
        }

        def today = new Date().format('yyyy-MM-dd')
        logFile = new File(logDir, "pipeline-app-${today}.log")

        // Log startup
        writeToFile("=== Pipeline App Started at ${new Date()} ===")
    }

    def startLogProcessor() {
        // Background thread to process log queue
        Thread.start {
            while (true) {
                try {
                    if (!logQueue.isEmpty()) {
                        def logEntry = logQueue.poll()
                        if (logEntry) {
                            processLogEntry(logEntry)
                        }
                    }
                    Thread.sleep(10)
                } catch (Exception e) {
                    System.err.println("Log processor error: ${e.message}")
                }
            }
        }
    }

    def processLogEntry(Map logEntry) {
        def message = formatLogMessage(logEntry)

        // Write to file
        writeToFile(message)

        // Send to GUI if callback is set
        if (outputCallback) {
            outputCallback(formatForGUI(logEntry))
        }

        // Print to console for debugging
        println message
    }

    def formatLogMessage(Map logEntry) {
        def timestamp = dateFormat.format(logEntry.timestamp)
        return "[$timestamp] [${logEntry.level}] ${logEntry.message}"
    }

    def formatForGUI(Map logEntry) {
        def timestamp = new Date(logEntry.timestamp).format('HH:mm:ss.SSS')
        def icon = getLogIcon(logEntry.level)
        return "[$timestamp] $icon ${logEntry.message}"
    }

    def getLogIcon(String level) {
        switch (level) {
            case 'DEBUG': return '🔍'
            case 'INFO': return 'ℹ️'
            case 'WARNING': return '⚠️'
            case 'ERROR': return '❌'
            default: return '📝'
        }
    }

    def writeToFile(String message) {
        try {
            logFile.append("$message\n")
        } catch (Exception e) {
            System.err.println("Failed to write to log file: ${e.message}")
        }
    }

    def log(String level, String message) {
        if (shouldLog(level)) {
            def logEntry = [
                timestamp: new Date(),
                level: level,
                message: message,
                thread: Thread.currentThread().name
            ]

            logQueue.offer(logEntry)
        }
    }

    def shouldLog(String level) {
        def currentLevelValue = LOG_LEVELS[logLevel] ?: 1
        def messageLevelValue = LOG_LEVELS[level] ?: 1
        return messageLevelValue >= currentLevelValue
    }

    // Public logging methods
    def debug(String message) {
        log('DEBUG', message)
    }

    def info(String message) {
        log('INFO', message)
    }

    def warning(String message) {
        log('WARNING', message)
    }

    def error(String message) {
        log('ERROR', message)
    }

    // GUI integration methods
    def setOutputArea(Closure callback) {
        this.outputCallback = callback
    }

    def setStatusUpdater(Closure callback) {
        this.statusCallback = callback
    }

    def updateStatus(String status) {
        if (statusCallback) {
            statusCallback(status)
        }
    }

    // Configuration methods
    def setLogLevel(String level) {
        if (LOG_LEVELS.containsKey(level)) {
            this.logLevel = level
            info("Log level set to: $level")
        } else {
            warning("Invalid log level: $level")
        }
    }

    def getLogLevel() {
        return logLevel
    }

    // Utility methods
    def getLogHistory(int lines = 100) {
        try {
            def allLines = logFile.readLines()
            def startIndex = Math.max(0, allLines.size() - lines)
            return allLines[startIndex..-1]
        } catch (Exception e) {
            error("Failed to read log history: ${e.message}")
            return []
        }
    }

    def clearLogs() {
        try {
            logFile.text = ""
            info("Log file cleared")
        } catch (Exception e) {
            error("Failed to clear log file: ${e.message}")
        }
    }

    def getLogStats() {
        def stats = [
            totalLines: 0,
            errorCount: 0,
            warningCount: 0,
            infoCount: 0,
            debugCount: 0,
            fileSize: 0
        ]

        try {
            if (logFile.exists()) {
                def lines = logFile.readLines()
                stats.totalLines = lines.size()
                stats.fileSize = logFile.size()

                lines.each { line ->
                    if (line.contains('[ERROR]')) stats.errorCount++
                    else if (line.contains('[WARNING]')) stats.warningCount++
                    else if (line.contains('[INFO]')) stats.infoCount++
                    else if (line.contains('[DEBUG]')) stats.debugCount++
                }
            }
        } catch (Exception e) {
            error("Failed to calculate log stats: ${e.message}")
        }

        return stats
    }

    def exportLogs(String filename = null) {
        if (!filename) {
            filename = "pipeline-logs-export-${new Date().format('yyyyMMdd-HHmmss')}.log"
        }

        try {
            def exportFile = new File(filename)
            exportFile.text = logFile.text
            info("Logs exported to: $filename")
            return exportFile
        } catch (Exception e) {
            error("Failed to export logs: ${e.message}")
            return null
        }
    }

    // Performance logging
    def timeOperation(String operationName, Closure operation) {
        def startTime = System.currentTimeMillis()
        info("⏱️ Starting operation: $operationName")

        try {
            def result = operation()
            def duration = System.currentTimeMillis() - startTime
            info("✅ Operation '$operationName' completed in ${duration}ms")
            return result
        } catch (Exception e) {
            def duration = System.currentTimeMillis() - startTime
            error("❌ Operation '$operationName' failed after ${duration}ms: ${e.message}")
            throw e
        }
    }

    // Structured logging
    def logStructured(String level, String event, Map data = [:]) {
        def structuredData = [
            event: event,
            timestamp: new Date().time,
            data: data
        ]

        def message = "$event: ${structuredData.inspect()}"
        log(level, message)
    }

    def logMetrics(String category, Map metrics) {
        logStructured('INFO', "METRICS_$category", metrics)
    }

    def logPerformance(String operation, long duration, Map context = [:]) {
        def perfData = context + [duration_ms: duration]
        logStructured('INFO', "PERFORMANCE_$operation", perfData)
    }

    def logError(String operation, Exception error, Map context = [:]) {
        def errorData = context + [
            error_class: error.class.simpleName,
            error_message: error.message,
            stack_trace: error.stackTrace.take(5).collect { it.toString() }
        ]
        logStructured('ERROR', "ERROR_$operation", errorData)
    }

    // Shutdown cleanup
    def shutdown() {
        info("🛑 LogManager shutting down...")

        // Process remaining logs
        while (!logQueue.isEmpty()) {
            def logEntry = logQueue.poll()
            if (logEntry) {
                processLogEntry(logEntry)
            }
        }

        writeToFile("=== Pipeline App Shutdown at ${new Date()} ===")
        info("✅ LogManager shutdown complete")
    }
}