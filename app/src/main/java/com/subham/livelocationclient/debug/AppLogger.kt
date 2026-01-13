package com.subham.livelocationclient.debug

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
object AppLogger {

    private const val MAX_LOGS = 300
    private val logs = CopyOnWriteArrayList<String>()

    private val formatter =
        SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun d(tag: String, message: String) {
        add("D", tag, message)
        Log.d(tag, message)
    }

    fun e(tag: String, message: String) {
        add("E", tag, message)
        Log.e(tag, message)
    }

    private fun add(level: String, tag: String, message: String) {
        val timestamp = formatter.format(Date())
        val entry = "$timestamp [$level/$tag] $message"

        logs.add(entry)

        // Drop oldest logs if buffer exceeds limit
        while (logs.size > MAX_LOGS) {
            logs.removeAt(0)
        }
    }

    fun snapshot(): List<String> {
        return logs.toList()
    }

    fun clear() {
        logs.clear()
    }
}