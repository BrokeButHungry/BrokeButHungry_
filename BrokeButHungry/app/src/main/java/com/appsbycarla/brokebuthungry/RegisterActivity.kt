//RegisterActivity.kt
package com.appsbycarla.brokebuthungry

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class RegisterActivity : AppCompatActivity() {

    private lateinit var passwordError: TextView
    private lateinit var firstName: EditText
    private lateinit var lastName: EditText
    private lateinit var userName: EditText
    private lateinit var userPassword: EditText
    private lateinit var confirmPassword: EditText
    private lateinit var userEmail: EditText
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize references to UI elements
        passwordError = findViewById(R.id.passwordError)
        firstName = findViewById(R.id.firstName)
        lastName = findViewById(R.id.lastName)
        userName = findViewById(R.id.userName)
        userPassword = findViewById(R.id.userPassword)
        confirmPassword = findViewById(R.id.confirmPassword)
        userEmail = findViewById(R.id.userEmail)
        registerButton = findViewById(R.id.registerButton)

        userPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        registerButton.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        val fName = firstName.text.toString().trim()
        val lName = lastName.text.toString().trim()
        val username = userName.text.toString().trim()
        val password = userPassword.text.toString().trim()
        val confirmPasswordText = confirmPassword.text.toString().trim()
        val email = userEmail.text.toString().trim()

        if (fName.isEmpty() || lName.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPasswordText.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if the passwords match
        if (password != confirmPasswordText) {
            passwordError.visibility = View.VISIBLE
            return
        } else {
            passwordError.visibility = View.GONE  // Hide the error message if passwords match
        }

        CoroutineScope(Dispatchers.IO).launch {
            val success = sendRegistrationRequest(fName, lName, username, password, email)
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(this@RegisterActivity, "Account created successfully!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@RegisterActivity, "Failed to create account", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun sendRegistrationRequest(firstName: String, lastName: String, username: String, password: String, email: String): Boolean {
        val url = URL("https://brokebuthungry.appsbycarla.com/api.php")
        (url.openConnection() as? HttpURLConnection)?.run {
            try {
                requestMethod = "POST"
                doOutput = true
                val postData = "request=createLogin&fname=${URLEncoder.encode(firstName, "UTF-8")}" +
                        "&lname=${URLEncoder.encode(lastName, "UTF-8")}" +
                        "&username=${URLEncoder.encode(username, "UTF-8")}" +
                        "&password=${URLEncoder.encode(password, "UTF-8")}" +
                        "&email=${URLEncoder.encode(email, "UTF-8")}"
                outputStream.write(postData.toByteArray())

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = inputStream.bufferedReader().use { it.readText() }
                    Log.d("RegisterActivity", "Server Response: $response")
                    // Here, parse the response to check if registration was successful
                    return true // Update this based on actual server response
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                disconnect()
            }
        }
        return false
    }


}