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
import android.os.AsyncTask
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
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


class    MainActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var recipeNameTextView: TextView
    private lateinit var caloriesTextView: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the content view to the activity's layout defined in activity_main.xml
        setContentView(R.layout.activity_main)

        // Initialize references to UI elements using their IDs
        searchEditText = findViewById(R.id.searchEditText)
        searchButton = findViewById(R.id.searchButton)
        recipeNameTextView = findViewById(R.id.recipeNameTextView)

        // NEW Search Nearby Groceries Button
        val searchNearbyButton: Button = findViewById(R.id.btnSearchNearby)
        searchNearbyButton.setOnClickListener {
            val intent = Intent(this, SearchNearbyActivity::class.java)
            intent.putExtra("query", "supermarket")
            startActivity(intent)
        }

        // Set a click listener for the searchButton
        searchButton.setOnClickListener {
            // Get the user's query from the searchEditText
            val query = searchEditText.text.toString()
            // Check if the query is not empty
            if (query.isNotEmpty()) {
                // Clear previous results
                recipeNameTextView.text= "Searching..."

                searchRecipeWithCoroutine(query)
            }
        }
    }
    /**
     * Searches for recipes matching the given query and returns their IDs.
     *
     * @param query The query string to search for.
     * @return A list of recipe IDs matching the query or null if no results.
     */
    private fun searchRecipe(query: String): List<String>? {
        val apiKey = "fa02fa2847654f40adab114f3f574335" //"b3d0fd73ebb946ca9d282a96c16e4b31" replace with your Spoonacular API key
        val apiUrl = "https://api.spoonacular.com/recipes/complexSearch?query=$query&apiKey=$apiKey"

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
            val ids = mutableListOf<String>()

            // Extracting recipe IDs from the results.
            for (i in 0 until results.length()) {
                val recipe = results.getJSONObject(i)
                ids.add(recipe.getString("id"))
            }

            return if (ids.isNotEmpty()) ids else null
        }
        return null
    }

    /**
     * Searches for recipes using coroutines and updates the UI with the results.
     *
     * @param query The query string to search for.
     */
    private fun searchRecipeWithCoroutine(query: String) {
        lifecycleScope.launch {
            try {
                // Fetching the recipe IDs that match the given query.
                val recipeIds = withContext(Dispatchers.IO) { searchRecipe(query) }
                val recipeInfos = mutableListOf<String>()

                // Fetching detailed recipe information for each ID.
                recipeIds?.forEach { recipeId ->
                    val recipeInfo = withContext(Dispatchers.IO) { getRecipeInformation(recipeId) }
                    recipeInfos.add(recipeInfo)
                }

                // Switching to Main thread to update UI
                withContext(Dispatchers.Main) {
                    recipeNameTextView.text = HtmlCompat.fromHtml(recipeInfos.joinToString("<br><br>"), HtmlCompat.FROM_HTML_MODE_LEGACY)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    recipeNameTextView.text = "Error occurred: ${e.message}"
                }
            }
        }
    }

    private fun getRecipeInformation(recipeId: String): String {
        val apiKey = "fa02fa2847654f40adab114f3f574335" //"b3d0fd73ebb946ca9d282a96c16e4b31" replace with your Spoonacular API key
        val apiUrl = "https://api.spoonacular.com/recipes/$recipeId/information?apiKey=$apiKey"

        try {
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val jsonResponse = JSONObject(response)

                val recipeTitle = jsonResponse.getString("title")
                val ingredientsArray = jsonResponse.getJSONArray("extendedIngredients")
                val instructions = jsonResponse.getString("instructions")

                val sb = StringBuilder()

                sb.append("<br><b>$recipeTitle</b><br><br>")
                sb.append("<b>Ingredients:</b><br>")

                for (i in 0 until ingredientsArray.length()) {
                    val ingredient = ingredientsArray.getJSONObject(i)
                    val ingredientInfo = ingredient.getString("original")
                    sb.append(ingredientInfo).append("<br>")
                }

                sb.append("<br><b>Instructions:</b><br><br>").append(instructions.replace("\n", "<br>"))

                return sb.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "Error retrieving recipe information."
    }

    private fun getRecipeInformationWithCoroutine(recipeId: String) {
        lifecycleScope.launch {
            try {
                val recipeInfo = withContext(Dispatchers.IO) { getRecipeInformation(recipeId) }
                // If you need to update UI with recipeInfo, do it here.
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    inner class FetchRecipesTask : AsyncTask<String, Void, String>() {

        // This method is executed in the background thread.
        override fun doInBackground(vararg params: String?): String {
            // Extract the query from the task parameters.
            val query = params[0]

            // API credentials (app ID and app key) for accessing the Edamam API.
            val apiId = "456948c8"
            val apiKey = "da192cac6c51d23c7025297c6c6f78b4"

            // Construct the URL for the API request using the query and credentials.
            val apiUrl = "https://api.edamam.com/search?q=$query&app_id=$apiId&app_key=$apiKey"

            try {
                // Create a URL object from the constructed URL string.
                val url = URL(apiUrl)

                // Open a connection to the URL as an HttpURLConnection.
                val connection = url.openConnection() as HttpURLConnection

                // Set the request method to "GET."
                connection.requestMethod = "GET"

                // Get the HTTP response code from the server.
                val responseCode = connection.responseCode

                // Check if the response code indicates a successful response (HTTP OK).
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read the response data from the input stream.
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?

                    // Read each line of the response and append it to the StringBuilder.
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    // Close the reader.
                    reader.close()

                    // Return the response data as a string.
                    return response.toString()
                }
            } catch (e: Exception) {
                // Handle exceptions, such as network errors or invalid URLs.
                e.printStackTrace()
            }
            // If there was an error or no data was received, return an empty string.
            return ""
        }

        // This method is executed on the main UI thread after doInBackground completes.
        override fun onPostExecute(result: String) {
            if (result.isNotEmpty()) {
                // If the result is not empty, call the displayResults method to show the data.
                displayResults(result)
            } else {
                // If there are no results, display a message in the resultsTextView.
                recipeNameTextView.text = "No results found."
            }
        }
    }

    private fun displayResults(jsonResult: String) {
        // Locate the TextView elements in your activity
        val recipeNameTextView = findViewById<TextView>(R.id.recipeNameTextView)

        try {
            // Parse the incoming JSON string into a JSONObject
            val jsonObject = JSONObject(jsonResult)

            // Get the "hits" array from the JSONObject
            val hitsArray = jsonObject.getJSONArray("hits")

            // Create a StringBuilder to store all recipe names and calories
            val recipeInfo = StringBuilder()

            // Loop through each item in the "hits" array
            for (i in 0 until hitsArray.length()) {
                // Get the "recipe" object for the current item
                val recipeObject = hitsArray.getJSONObject(i).getJSONObject("recipe")

                // Extract relevant data from the "recipe" object
                val recipeLabel = recipeObject.getString("label")
                val recipeCalories = recipeObject.getDouble("calories")

                // Append the recipe name and calories to the recipeInfo StringBuilder
                recipeInfo.append("$recipeLabel\nCalories: $recipeCalories\n\n")
            }

            // Set the combined recipe names and calories in the recipeNameTextView
            recipeNameTextView.text = recipeInfo.toString()

            // Set the text color to white
            recipeNameTextView.setTextColor(ContextCompat.getColor(this, android.R.color.white))

            // Adjust the text size for recipe name (if needed)
            recipeNameTextView.textSize = resources.getDimension(R.dimen.recipe_label_text_size)
        } catch (e: Exception) {
            // Handle exceptions, such as JSON parsing errors
            e.printStackTrace()
            // Display an error message in the TextView if an exception occurs
            recipeNameTextView.text = "Error parsing JSON."
        }
    }
}

