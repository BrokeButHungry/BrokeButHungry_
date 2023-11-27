//ResetPasswordActivity.kt
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
import android.widget.Toast
import com.appsbycarla.brokebuthungry.R

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var firstName: EditText
    private lateinit var lastName: EditText
    private lateinit var userName: EditText
    private lateinit var userEmail: EditText
    private lateinit var newPassword: EditText
    private lateinit var resetPasswordButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resetpassword)

        firstName = findViewById(R.id.firstName)
        lastName = findViewById(R.id.lastName)
        userName = findViewById(R.id.userName)
        userEmail = findViewById(R.id.userEmail)
        newPassword = findViewById(R.id.newPassword)
        resetPasswordButton = findViewById(R.id.resetPasswordButton)

        resetPasswordButton.setOnClickListener {
            resetUserPassword()
        }
    }

    private fun resetUserPassword() {
        val fName = firstName.text.toString().trim()
        val lName = lastName.text.toString().trim()
        val username = userName.text.toString().trim()
        val email = userEmail.text.toString().trim()
        val password = newPassword.text.toString().trim()

        if (fName.isEmpty() || lName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Perform network request to reset password
        CoroutineScope(Dispatchers.IO).launch {
            val success = sendPasswordResetRequest(fName, lName, username, email, password)
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(this@ResetPasswordActivity, "Password reset successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@ResetPasswordActivity, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@ResetPasswordActivity, "Failed to reset password", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendPasswordResetRequest(firstName: String, lastName: String, username: String, email: String, newPassword: String): Boolean {
        val url = URL("https://brokebuthungry.appsbycarla.com/api.php")
        (url.openConnection() as? HttpURLConnection)?.run {
            try {
                requestMethod = "POST"
                doOutput = true
                val postData = "request=resetPassword&fname=${URLEncoder.encode(firstName, "UTF-8")}" +
                        "&lname=${URLEncoder.encode(lastName, "UTF-8")}" +
                        "&username=${URLEncoder.encode(username, "UTF-8")}" +
                        "&email=${URLEncoder.encode(email, "UTF-8")}" +
                        "&password=${URLEncoder.encode(newPassword, "UTF-8")}"
                outputStream.write(postData.toByteArray())

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = inputStream.bufferedReader().use { it.readText() }
                    Log.d("ResetPasswordActivity", "Server Response: $response")

                    val jsonResponse = JSONObject(response)
                    val status = jsonResponse.optString("status", "")
                    return status == "success"
                } else
                    Log.d("ResetPasswordActivity", "Error Response Code: $responseCode")
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                disconnect()
            }
        }
        return false
    }

}