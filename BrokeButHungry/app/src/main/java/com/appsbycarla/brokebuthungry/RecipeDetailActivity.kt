package com.appsbycarla.brokebuthungry


import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.bumptech.glide.Glide
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


class RecipeDetailActivity : AppCompatActivity() {

    private var isNutritionValueExpanded = false
    private var isPriceBreakdownExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recipe_detail)

        val recipeId = intent.getStringExtra("RECIPE_ID")
        if (recipeId != null) {
            fetchAndDisplayRecipeDetails(recipeId)
            fetchAndDisplayNutritionLabel(recipeId)
            fetchAndDisplayPriceBreakdownImage(recipeId)
        }
        val nutritionValueTitle: TextView = findViewById(R.id.nutritionValueTitle)
        val nutritionValueLayout: LinearLayout = findViewById(R.id.nutritionValueLayout)
        val nutritionLabelWebView: WebView = findViewById(R.id.nutritionLabelWebView)

        nutritionValueTitle.setOnClickListener {
            toggleNutritionValue(nutritionValueLayout, nutritionLabelWebView)
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
                        ingredientBox.setTextColor(Color.BLACK)
                        ingredientBox.textSize = 18F
                        ingredientBox.text = individualIngredient

                        ingredientBox.setOnCheckedChangeListener{ _, isChecked ->
                            if(isChecked) {
                                ingredientBox.paintFlags = ingredientBox.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                            }
                            else {
                                ingredientBox.paintFlags = ingredientBox.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                            }

                        }

                        ingredientLinearLayout.addView(ingredientBox)
                    }

                    findViewById<TextView>(R.id.totalIngredientString).text =
                        HtmlCompat.fromHtml((totalIngredientString ?: "Total number of ingredients not available.").toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)

                    val instructionLinearLayout = findViewById<LinearLayout>(R.id.instructionListLinearLayout)

                    for(individualInstruction in instructionListArray) {
                        val instructionBox = CheckBox(this@RecipeDetailActivity)
                        instructionBox.setTextColor((Color.BLACK))
                        instructionBox.textSize = 18F
                        instructionBox.text = (individualInstruction + "\n")

                        instructionBox.setOnCheckedChangeListener{ _, isChecked ->
                            if(isChecked) {
                                instructionBox.paintFlags = instructionBox.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                            }
                            else {
                                instructionBox.paintFlags = instructionBox.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                            }

                        }

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
         val apiKey = "b3d0fd73ebb946ca9d282a96c16e4b31"
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
        val apiKey = "d567d4fc3c5e43edbcd15915fd46719b" //"b3d0fd73ebb946ca9d282a96c16e4b31"
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
//---------------------------------------------------------------------------------------------------------------
    fun toggleIngredientList(view: View) {
        val ingredientScrollView: ScrollView = findViewById(R.id.ingredientScrollView)

        if (ingredientScrollView.visibility == View.VISIBLE) {
            // If visible, hide the ingredient list
            ingredientScrollView.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    ingredientScrollView.visibility = View.GONE
                }
                .start()
        } else {
            // If hidden, show the ingredient list
            ingredientScrollView.visibility = View.VISIBLE
            ingredientScrollView.alpha = 0f
            ingredientScrollView.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }

    fun toggleInstructionList(view: View) {
        val instructionScrollView: ScrollView = findViewById(R.id.instructionListScrollView)

        if (instructionScrollView.visibility == View.VISIBLE) {
            // If visible, hide the instruction list
            instructionScrollView.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    instructionScrollView.visibility = View.GONE
                }
                .start()
        } else {
            // If hidden, show the instruction list
            instructionScrollView.visibility = View.VISIBLE
            instructionScrollView.alpha = 0f
            instructionScrollView.animate().alpha(1f).setDuration(300).start()
        }
    }

    private fun toggleNutritionValue(nutritionValueLayout: LinearLayout, nutritionLabelWebView: WebView) {
        if (isNutritionValueExpanded) {
            collapse(nutritionValueLayout)
            nutritionLabelWebView.visibility = View.GONE
            isNutritionValueExpanded = false
        } else {
            expand(nutritionValueLayout)
            nutritionLabelWebView.visibility = View.VISIBLE
            isNutritionValueExpanded = true
        }
    }

    fun togglePriceBreakdown(view: View) {
        val priceBreakdownLayout: LinearLayout = findViewById(R.id.priceBreakdownLayout)
        val priceBreakdownImageView: ImageView = findViewById(R.id.priceBreakdownImageView)

        if (isPriceBreakdownExpanded) {
            collapse(priceBreakdownLayout)
            priceBreakdownImageView.visibility = View.GONE
            isPriceBreakdownExpanded = false
        } else {
            val apiKey = "d567d4fc3c5e43edbcd15915fd46719b" // Replace with your actual Spoonacular API key
            val mode = 2 // You can change this according to your requirement

            CoroutineScope(Dispatchers.Main).launch {
                fetchAndDisplayPriceBreakdownImage(apiKey)
                expand(priceBreakdownLayout)
                priceBreakdownImageView.visibility = View.VISIBLE
                isPriceBreakdownExpanded = true
            }
        }
    }

    private fun expand(view: View) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layoutParams.height = view.measuredHeight
    }

    private fun collapse(view: View) {
        view.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                view.visibility = View.GONE
                view.alpha = 1f // Reset alpha for future animations
            }
            .start()
    }

    private fun fetchAndDisplayPriceBreakdownImage(recipeId: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // Make the API request to fetch the image
                val apiKey = "d567d4fc3c5e43edbcd15915fd46719b"
                val mode = 2 // You can change this according to your requirement
                val url = URL("https://api.spoonacular.com/recipes/$recipeId/priceBreakdownWidget.png?mode=$mode")

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "image/png")
                connection.setRequestProperty("apiKey", apiKey)

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    // Process the image response
                    val inputStream = connection.inputStream
                    val bitmap = BitmapFactory.decodeStream(inputStream)

                    // Switch to the main thread to update UI
                    launch(Dispatchers.Main) {
                        // Display the image in the ImageView
                        findViewById<ImageView>(R.id.priceBreakdownImageView).setImageBitmap(bitmap)
                    }
                } else {
                    // Handle error cases
                    Log.e("PriceBreakdown", "Failed to fetch price breakdown image. HTTP Code: ${connection.responseCode}")
                }
            } catch (e: Exception) {
                // Handle exceptions (e.g., IOException)
                e.printStackTrace()
            }
        }
    }
}