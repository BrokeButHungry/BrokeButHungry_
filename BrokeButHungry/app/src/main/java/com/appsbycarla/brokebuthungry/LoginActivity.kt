//LoginActivity.kt
package com.appsbycarla.brokebuthungry

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var resetPasswordButton: Button
    private lateinit var loginErrorTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread.sleep(1500)
        installSplashScreen()

        setContentView(R.layout.activity_login)

        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        registerButton = findViewById(R.id.registerButton)
        resetPasswordButton = findViewById(R.id.resetPasswordButton)
        loginErrorTextView = findViewById(R.id.loginErrorTextView)

        loginButton.setOnClickListener {
            performLogin()
        }

        // Navigate to RegisterActivity when the register button is clicked
        registerButton.setOnClickListener {
            // Start RegisterActivity
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Navigate to ResetPasswordActivity when the reset password button is clicked
        resetPasswordButton.setOnClickListener {
            // Start ResetPasswordActivity
            val intent = Intent(this, ResetPasswordActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Initiates the login process.
     * Author: Carla Hernandez
     * - Validates the input fields for username and password.
     * - Calls `sendLoginRequest` to perform the network request.
     * - Handles UI updates based on the login success or failure.
     */
    private fun performLogin() {
        val username = usernameEditText.text.toString()
        val password = passwordEditText.text.toString()

        if (username.isNotEmpty() && password.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                val result = sendLoginRequest(username, password)
                withContext(Dispatchers.Main) {
                    if (result) {
                        // Login successful, go to MainActivity
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // Login failed, show error message
                        runOnUiThread {
                            val errorTextView = findViewById<TextView>(R.id.loginErrorTextView)
                            errorTextView.text = "Username or password entered is incorrect. Try again."
                            errorTextView.visibility = View.VISIBLE
                        }
                    }
                }
            }
        } else {
            // Prompt the user to enter username and password
        }
    }

    /**
     * Sends a login request to the server.
     * Author: Carla Hernandez
     * @param username The username entered by the user.
     * @param password The password entered by the user.
     * @return Boolean indicating whether the login was successful.
     */
    private fun sendLoginRequest(username: String, password: String): Boolean {
        Log.d("LoginActivity", "Attempting to login with username: $username")
        val url = URL("https://brokebuthungry.appsbycarla.com/api.php")
        (url.openConnection() as? HttpURLConnection)?.run {
            try {
                requestMethod = "POST"
                doOutput = true
                val postData = "request=login&username=${URLEncoder.encode(username, "UTF-8")}&password=${URLEncoder.encode(password, "UTF-8")}"
                outputStream.write(postData.toByteArray())

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = inputStream.bufferedReader().use { it.readText() }
                    Log.d("LoginActivity", "Response Code: $responseCode")
                    Log.d("LoginActivity", "Response: $response")

                    val jsonResponse = JSONObject(response)
                    val status = jsonResponse.optString("status", "")
                    return status.equals("success", ignoreCase = true)
                }
            } catch (e: IOException) {
                // Handle IO Exception
                e.printStackTrace()
            } finally {
                disconnect()
            }
        }
        Log.d("LoginActivity", "Login request failed")
        return false
    }

}

