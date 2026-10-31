package com.example.musicapp.platform

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

object CrashReporter {
    fun sanitize(input: String): String {
        var result = input
        result = result.replace(Regex("""(?i)\b(password|passwd|pwd)=[^&\s]+""")) { match ->
            val key = match.value.substringBefore('=')
            "$key=***"
        }
        result = result.replace(Regex("""(?i)\b(token|access_token|auth_token)=[^&\s]+""")) { match ->
            val key = match.value.substringBefore('=')
            "$key=***"
        }
        result = result.replace(Regex("""(?i)\b(salt)=[^&\s]+""")) { match ->
            val key = match.value.substringBefore('=')
            "$key=***"
        }
        result = result.replace(Regex("""(?i)\b(key|api_key|apikey)=[^&\s]+""")) { match ->
            val key = match.value.substringBefore('=')
            "$key=***"
        }
        result = result.replace(Regex("""(?i)\b([upst])=[^&\s]+""")) { match ->
            val key = match.value.substringBefore('=')
            "$key=***"
        }
        result = result.replace(Regex("""(?i)(Authorization:\s*(?:Bearer|Basic)\s+)[^\r\n\s]+""")) { match ->
            "${match.groupValues[1]}***"
        }
        result = result.replace(Regex("""(?i)\b(Bearer\s+)[A-Za-z0-9_\-\.]+""")) { match ->
            "${match.groupValues[1]}***"
        }
        result = result.replace(Regex("""(?i)\b(Basic\s+)[A-Za-z0-9+/=]+""")) { match ->
            "${match.groupValues[1]}***"
        }
        return result
    }

    fun init() {
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                handleUncaughtException(thread, throwable)
            } catch (t: Throwable) {
                t.printStackTrace()
            } finally {
                originalHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    fun handleUncaughtException(thread: Thread, throwable: Throwable, showDialog: Boolean = true): File? {
        val dateStr = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val logsDir = File(System.getProperty("user.home"), "JVXMusic/logs").apply { mkdirs() }
        val crashFile = File(logsDir, "crash-$dateStr.log")

        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val rawStack = sw.toString()
        val sanitizedStack = sanitize(rawStack)
        val sanitizedMsg = sanitize(throwable.message ?: throwable.javaClass.simpleName)

        val logContent = buildString {
            appendLine("=== JVXMusic Crash Log ===")
            appendLine("Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
            appendLine("Thread: ${thread.name} (id=${thread.id})")
            appendLine("OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")} (${System.getProperty("os.arch")})")
            appendLine("Java: ${System.getProperty("java.version")} (${System.getProperty("java.vendor")})")
            appendLine("Message: $sanitizedMsg")
            appendLine("Stacktrace:")
            appendLine(sanitizedStack)
        }

        runCatching {
            crashFile.writeText(logContent)
        }

        if (showDialog && !java.awt.GraphicsEnvironment.isHeadless()) {
            val dialogMessage = "Произошла непредвиденная ошибка.\n" +
                    "Сведения сохранены в: ${crashFile.name}\n\n" +
                    sanitizedMsg.take(300)

            try {
                if (SwingUtilities.isEventDispatchThread()) {
                    JOptionPane.showMessageDialog(null, dialogMessage, "Ошибка воспроизведения", JOptionPane.ERROR_MESSAGE)
                } else {
                    SwingUtilities.invokeAndWait {
                        JOptionPane.showMessageDialog(null, dialogMessage, "Ошибка воспроизведения", JOptionPane.ERROR_MESSAGE)
                    }
                }
            } catch (_: Throwable) {
                // Toolkit might be unavailable in headless environments
            }
        }

        return crashFile
    }
}
