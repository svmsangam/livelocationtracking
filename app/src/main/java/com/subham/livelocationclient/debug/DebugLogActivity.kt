package com.subham.livelocationclient.debug


import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.subham.livelocationclient.R



class DebugLogActivity : AppCompatActivity() {

    private lateinit var tvLogs: TextView
    private val handler = Handler(Looper.getMainLooper())

    private val refreshRunnable = object : Runnable {
        override fun run() {
            val logs = AppLogger.snapshot()
            tvLogs.text = logs.joinToString("\n")
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_log)

        tvLogs = findViewById(R.id.tvLogs)
    }

    override fun onStart() {
        super.onStart()
        handler.post(refreshRunnable)
    }

    override fun onStop() {
        handler.removeCallbacks(refreshRunnable)
        super.onStop()
    }
}
