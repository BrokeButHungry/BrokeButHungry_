/*
09.09.23 Comments by Carla Ramos.
Algorithm:
1. Initialize UI elements, including an EditText for user input, a Button for initiating a search, and a TextView for displaying search results.
2. Set a click listener for the search button, which triggers an API request when clicked.
3. In the background thread (AsyncTask FetchRecipesTask):
    - Construct a URL for an API request using user input and API credentials.
    - Make an HTTP GET request to the API.
    - Read and collect the response data, returning it as a string.
    - Handle exceptions, such as network errors or invalid URLs.
4. Update the UI on the main thread (in onPostExecute):
    - Check if the API response is not empty.
    - If not empty, parse and display the recipe information in the TextView.
    - If empty, display a "No results found" message in the TextView.
5. Display recipe information in the TextView by parsing a JSON response, extracting data, and formatting it.
 */

//MainActivity.kt
package com.appsbycarla.brokebuthungry
import android.content.Intent // for search nearby
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import kotlinx.coroutines.*
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


class    MainActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var recipeNameTextView: TextView
    private lateinit var recipeImageView: ImageView
    private lateinit var recipesLayout: LinearLayout
    data class Recipe(val id: String, val title: String?, val imageUrl: String?, val ingredients: Int?)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the content view to the activity's layout defined in activity_main.xml
        setContentView(R.layout.activity_main)

        // Initialize references to UI elements using their IDs
        searchEditText = findViewById(R.id.searchEditText)
        searchButton = findViewById(R.id.searchButton)
//        recipeNameTextView = findViewById(R.id.recipeNameTextView)
//        recipeImageView = findViewById(R.id.recipeImageView)
        recipesLayout = findViewById(R.id.recipesLayout)

        /*val searchNearbyButton: Button = findViewById(R.id.btnSearchNearby)
        searchNearbyButton.setOnClickListener {
            val intent = Intent(this, SearchNearbyActivity::class.java)
            intent.putExtra("query", "supermarket")
            startActivity(intent)
        }*/
        val mapButton: Button = findViewById(R.id.btnSearchNearby)
        mapButton.setOnClickListener {
            val gmmIntentUri = Uri.parse("geo:0,0?q=supermarket")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
        }

        // Set a click listener for the searchButton
        searchButton.setOnClickListener {
            // Get the user's query from the searchEditText
            val query = searchEditText.text.toString()
            // Check if the query is not empty
            if (query.isNotEmpty()) {
                // Clear previous results
//                recipeNameTextView.text = "Searching..."

                searchRecipeWithCoroutine(query)
            }
        }
    }

    /**
     * Searches for recipes using coroutines and updates the UI with the results.
     * Author: Carla Hernandez
     * @param query The query string to search for.
     */
    private fun searchRecipeWithCoroutine(query: String) {
        lifecycleScope.launch {
            try {
                // Fetch the recipes for the given query.
                val recipes = withContext(Dispatchers.IO) { searchRecipe(query) }

                withContext(Dispatchers.Main) {
                    recipesLayout.removeAllViews() // Clear previous results
                    if (recipes != null) {
                        for (recipe in recipes) {
                            addRecipeToLayout(recipe)
                        }
                    } else {
                        val noResultTextView = TextView(this@MainActivity)
                        noResultTextView.text = "No matching recipes found."
                        recipesLayout.addView(noResultTextView)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val errorTextView = TextView(this@MainActivity)
                errorTextView.text = "Error occurred: ${e.message}"
                recipesLayout.addView(errorTextView)
            }
        }
    }


    /**
     * Searches for recipes matching the given query and returns their IDs based on ingredent#
     * Author: Carla Hernandez and James Cowman
     * @param query The query string to search for.
     * @return A list of recipe IDs matching the query or null if no results.
     */
    private fun searchRecipe(query: String): List<Recipe>? {
        val apiKey = "fa02fa2847654f40adab114f3f574335" //"b3d0fd73ebb946ca9d282a96c16e4b31"
        val apiUrl = "https://api.spoonacular.com/recipes/complexSearch?query=$query&number=6&apiKey=$apiKey"

        val url = URL(apiUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        // Check if the connection is successful.
        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.readText()
            reader.close()

            val jsonResponse = JSONObject(response)
            val results = jsonResponse.getJSONArray("results")
            val recipes = mutableListOf<Recipe>()


            // Extracting recipe information from the results.
            for (i in 0 until results.length()) {
                val recipeJson = results.getJSONObject(i)
                val id = recipeJson.getString("id")
                val title = "<br><h2 style=\"color:red;\">${recipeJson.getString("title")}</h2>"
                val imageUrl = recipeJson.getString("image")
                val ingredientsCount = fetchingredientsCount(id)
                recipes.add(Recipe(id, title, imageUrl, ingredientsCount))
                //TODo
            }
            val recipesSorted = recipes.sortedBy{ it.ingredients}

            return if (recipesSorted.isNotEmpty()) recipesSorted else null
        }
        return null
    }


    /**
     * Adds a recipe to the recipesLayout LinearLayout.
     * This includes setting up a TextView for the recipe title and an ImageView for the recipe image.
     * Both views are clickable and lead to a detailed view of the recipe.
     *
     * Author: Carla Hernandez
     * @param recipe The recipe object containing the title, image URL, and ID.
     */
    private fun addRecipeToLayout(recipe: Recipe) {
        // Create a TextView for the recipe title
        val titleTextView = TextView(this)

        titleTextView.text = HtmlCompat.fromHtml(recipe.title ?: "", HtmlCompat.FROM_HTML_MODE_LEGACY)
        titleTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        titleTextView.setOnClickListener {
            openRecipeDetailActivity(recipe.id)
        }

        titleTextView.textSize = 16f
        titleTextView.setPadding(0, 16, 0, 8)
        recipesLayout.addView(titleTextView)

        val recipeImage = ImageView(this)
        if (recipe.imageUrl != null) {
            Glide.with(this).load(recipe.imageUrl).into(recipeImage)
        }
        recipeImage.setOnClickListener {
            openRecipeDetailActivity(recipe.id)
        }

        recipesLayout.addView(recipeImage)
    }

    /**
     * Opens the RecipeDetailActivity to display details of a selected recipe.
     * The recipe ID is passed as an extra to the RecipeDetailActivity to fetch and display the corresponding recipe details.
     *
     * Author: Carla Hernandez
     * @param recipeId The unique identifier of the recipe to be displayed in the RecipeDetailActivity.
     */
    private fun openRecipeDetailActivity(recipeId: String) {
        val intent = Intent(this, RecipeDetailActivity::class.java)
        intent.putExtra("RECIPE_ID", recipeId)
        startActivity(intent)
    }
}
/**
 * Opens the spoonacular API to fetch an ingredients count of a selected recipe.
 * Author: James Cowman
 * @param recipeId The unique identifier of the recipe to be displayed in the RecipeDetailActivity.
 */
private fun fetchingredientsCount(recipeId: String): Int {
    val apiKey = "fa02fa2847654f40adab114f3f574335" //"b3d0fd73ebb946ca9d282a96c16e4b31"
    val apiUrl = "https://api.spoonacular.com/recipes/$recipeId/information?apiKey=$apiKey"

    val url = URL(apiUrl)
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"

    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        val response = reader.readText()
        reader.close()

        val jsonResponse = JSONObject(response)
        val ingredientsArray = jsonResponse.getJSONArray("extendedIngredients")
        return ingredientsArray.length()
    } else {
        throw Exception("Failed to fetch recipe details. HTTP Code: ${connection.responseCode}")
    }
}


