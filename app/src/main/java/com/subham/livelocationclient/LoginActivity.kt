package com.subham.livelocationclient

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var responseText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        usernameInput = findViewById(R.id.usernameInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginSubmitButton)
        responseText = findViewById(R.id.loginResponseText)

        loginButton.setOnClickListener {
            login()
        }
    }

    private fun login() {
        val username = usernameInput.text.toString()
        val password = passwordInput.text.toString()

        val url = "http://192.168.100.166:8181/realms/live_location_tracking/protocol/openid-connect/token"

        val formBody = FormBody.Builder()
            .add("client_id", "android_app")
            .add("username", username)
            .add("password", password)
            .add("grant_type", "password")
            .build()

        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    responseText.text = "Error: ${e.message}"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()

                runOnUiThread {
                    responseText.text = body ?: "Empty response"
                }
            }
        })
    }
}