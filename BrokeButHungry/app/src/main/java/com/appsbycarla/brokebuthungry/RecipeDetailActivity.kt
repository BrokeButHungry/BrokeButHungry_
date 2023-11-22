package com.appsbycarla.brokebuthungry

import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import org.json.JSONObject
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import android.widget.TextView
import android.widget.ImageView
import androidx.core.text.HtmlCompat
import com.bumptech.glide.Glide
import android.webkit.WebView
import android.widget.CheckBox
import android.widget.LinearLayout
import org.w3c.dom.Text


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
     * Co-author: Calla Punsalang
     * @param recipeId The unique identifier of the recipe for which details are to be fetched.
     */

    private fun fetchAndDisplayRecipeDetails(recipeId: String) {
        // fetch the data in the background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                //recipeTitle, recipeImage, totalIngredientsString, instructionListString, ingredientListArray
                val (recipeTitle, recipeImage, totalIngredientString, instructionList, ingredientList) = fetchData(recipeId)

                val ingredientListArray = ingredientList as ArrayList<String>
                val instructionListArray = instructionList as ArrayList<String>

                // Switch to the main thread to update the UI
                withContext(Dispatchers.Main) {
                    findViewById<TextView>(R.id.recipeTitleTextView).text = HtmlCompat.fromHtml(
                        recipeTitle.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)

                    // using Glide to load images
                    Glide.with(this@RecipeDetailActivity).load(recipeImage).into(findViewById<ImageView>(R.id.recipeImageView))

                    val ingredientLinearLayout = findViewById<LinearLayout>(R.id.ingredientListLinearLayout)

                    for(individualIngredient in ingredientListArray) {
                        val ingredientBox = CheckBox(this@RecipeDetailActivity)
                        ingredientBox.text = individualIngredient
                        ingredientLinearLayout.addView(ingredientBox)
                    }

                    findViewById<TextView>(R.id.totalIngredientString).text =
                        HtmlCompat.fromHtml((totalIngredientString ?: "Total number of ingredients not available.").toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)

                    val instructionLinearLayout = findViewById<LinearLayout>(R.id.instructionListLinearLayout)

                    for(individualInstruction in instructionListArray) {
                        val instructionBox = CheckBox(this@RecipeDetailActivity)
                        instructionBox.text = individualInstruction
                        instructionLinearLayout.addView(instructionBox)
                    }
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
     * Co-author: Calla Punsalang
     * @param recipeId The unique identifier of the recipe for which details are to be fetched.
     * @return An array containing:
     *  - First: The formatted title of the recipe (String).
     *  - Second: The URL to the recipe's image (String?) which can be null.
     *  - Third: Total number of ingredients
     *  - Fourth: Instruction list string
     *  - Fifth: Ingredient list string
     */

    private suspend fun fetchData(recipeId: String): Array<Any> {
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
            val instructions = jsonResponse.getJSONArray("analyzedInstructions")
            val analyzedInstructions = instructions.getJSONObject(0)
            val analyzedInstructionSteps = analyzedInstructions.getJSONArray("steps")
            val recipeImage = jsonResponse.getString("image")
            val totalIngredients = ingredientsArray.length() // Added
            val totalIngredientsString = "<b>Total Number of Ingredients:</b> $totalIngredients<br><br>"

            val ingredientListArray = ArrayList<String>()
            val instructionListArray = ArrayList<String>()

            for(i in 0 until ingredientsArray.length()) {
                val ingredient = ingredientsArray.getJSONObject(i)
                val ingredientInfo = ingredient.getString("original")
                ingredientListArray.add(ingredientInfo)
            }

            for(i in 0 until analyzedInstructionSteps.length()) {
                val analyzedInstructions = analyzedInstructionSteps.getJSONObject(i)
                val instructionNumber = analyzedInstructions.getInt("number").toString()
                val instructionStep = analyzedInstructions.getString("step")
                val numberAndInstruction = buildString {
                    append(instructionNumber)
                    append(". ")
                    append(instructionStep)
                 }
                instructionListArray.add(numberAndInstruction)
            }

                return arrayOf(recipeTitle, recipeImage, totalIngredientsString, instructionListArray, ingredientListArray)
        } else {
            throw Exception("Failed to fetch recipe details. HTTP Code: ${connection.responseCode}")
        }
    }
}