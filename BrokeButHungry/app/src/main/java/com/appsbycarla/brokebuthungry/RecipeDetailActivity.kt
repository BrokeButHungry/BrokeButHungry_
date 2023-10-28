package com.appsbycarla.brokebuthungry

import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import org.json.JSONObject
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import android.widget.TextView
import android.widget.ImageView
import androidx.core.text.HtmlCompat
import com.bumptech.glide.Glide
import android.webkit.WebView


class RecipeDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recipe_detail)

        val recipeId = intent.getStringExtra("RECIPE_ID")
        if (recipeId != null) {
            fetchAndDisplayRecipeDetails(recipeId)
            fetchAndDisplayNutritionLabel(recipeId)
        }
    }

    /**
     * Fetches the detailed information of a specified recipe using its ID and then updates the UI to display these details.
     * Uses Kotlin's coroutines to fetch the data in a background thread, while updating the UI on the main thread.
     *
     * Author: Carla Hernandez
     * @param recipeId The unique identifier of the recipe for which details are to be fetched.
     */
    private fun fetchAndDisplayRecipeDetails(recipeId: String) {
        // fetch the data in the background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val (recipeTitle, recipeImage, details) = fetchData(recipeId)

                // Switch to the main thread to update the UI
                withContext(Dispatchers.Main) {
                    findViewById<TextView>(R.id.recipeTitleTextView).text = HtmlCompat.fromHtml(recipeTitle, HtmlCompat.FROM_HTML_MODE_LEGACY)

                    // using Glide to load images
                    Glide.with(this@RecipeDetailActivity).load(recipeImage).into(findViewById<ImageView>(R.id.recipeImageView))

                    findViewById<TextView>(R.id.recipeDetailsTextView).text =
                        HtmlCompat.fromHtml(details ?: "No details available.", HtmlCompat.FROM_HTML_MODE_LEGACY)

                }
            } catch (e: Exception) {
                // If there's an error, show a message to the user
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RecipeDetailActivity, "Failed to load recipe details.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Fetching nutritional label for each recipe via Spoonacular API
     */

    private fun fetchNutritonalLabel(recipeId: String): String {
        val apiKey = "420aea8d55f9424b962c04001ef88f3a"
        val apiUrl = "https://api.spoonacular.com/recipes/$recipeId/nutritionLabel?apiKey=$apiKey"

        val url = URL(apiUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.readText()
            reader.close()
            return response
        } else {
            throw Exception("Failed to fetch nutrition label. HTTP Code: ${connection.responseCode}")
        }
    }

    private fun displayNutritionLabel(nutritionLabelHtml: String) {
        val webView: WebView = findViewById(R.id.nutritionLabelWebView)
        webView.settings.javaScriptEnabled = true
        webView.loadDataWithBaseURL(null, nutritionLabelHtml, "text/html", "utf-8", null)
    }

    private fun fetchAndDisplayNutritionLabel(recipeId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val nutritionLabelHtml = fetchNutritonalLabel(recipeId)
                withContext(Dispatchers.Main) {
                    // Switch to main thread to update the UI with the nutrition label HTML
                    displayNutritionLabel(nutritionLabelHtml)
                }
            } catch (e: Exception) {
                // Handle error if the nutrition label fetching fails
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RecipeDetailActivity, "Failed to fetch nutrition label.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Asynchronously fetches detailed information about a specified recipe based on its unique ID from Spoonacular API.
     * Shows the recipe title, image, ingredients, and instructions and returns them as a Triple.
     *
     * Author: Carla Hernandez
     * @param recipeId The unique identifier of the recipe for which details are to be fetched.
     * @return A Triple containing:
     *  - First: The formatted title of the recipe (String).
     *  - Second: The URL to the recipe's image (String?) which can be null.
     *  - Third: A formatted string that combines the ingredients list and the instructions (String?) which can be null.
     */
    private suspend fun fetchData(recipeId: String): Triple<String, String?, String?> {
        val apiKey = "b3d0fd73ebb946ca9d282a96c16e4b31" //"fa02fa2847654f40adab114f3f574335"
        val apiUrl = "https://api.spoonacular.com/recipes/$recipeId/information?apiKey=$apiKey"

        val url = URL(apiUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.readText()
            reader.close()

            val jsonResponse = JSONObject(response)

            val recipeTitle = "<br><b>${jsonResponse.getString("title")}</b><br><br>"
            val ingredientsArray = jsonResponse.getJSONArray("extendedIngredients")
            val instructions = jsonResponse.getString("instructions")
            val recipeImage = jsonResponse.getString("image")
            val totalIngredients = ingredientsArray.length() // Added

            val sb = StringBuilder()
            sb.append("<br><b>$recipeTitle</b><br><br>") // Added
            sb.append("<b>Total Number of Ingredients:</b> $totalIngredients<br>") // Added
            sb.append("<b>Ingredients:</b><br>")

            for (i in 0 until ingredientsArray.length()) {
                val ingredient = ingredientsArray.getJSONObject(i)
                val ingredientInfo = ingredient.getString("original")
                sb.append(ingredientInfo).append("<br>")
            }

            sb.append("<br><b>Instructions:</b><br><br>")
                .append(instructions.replace("\n", "<br>"))

            return Triple(recipeTitle, recipeImage, sb.toString())
        } else {
            throw Exception("Failed to fetch recipe details. HTTP Code: ${connection.responseCode}")
        }
    }
}