package com.example.rezepte


import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import it.skrape.core.htmlDocument
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.extractIt
import it.skrape.fetcher.skrape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.redundent.kotlin.xml.xml
import java.io.File


class CreateActivity : ComponentActivity()
{
    private var ACCESS_TOKEN: String? = null
    private val IMAGE_REQUEST_CODE = 10
    private var imageUri : Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.createlayout);

        //if there is a recipe to load the data of
        val extras = intent.extras
        var recipeName: String? = null
        var preloadOption: String? = null
        var preloadData: String? = null
        if (extras != null) {
            recipeName = extras.getString("recipe name")
            preloadOption = extras.getString("preload option")
            preloadData = extras.getString("preload data")
        }
        //if there is a preload option get ready to preload data
        if (preloadOption != null){
            if (preloadOption == "website"){
                //scrape the website for to recipe information and auto fill the categorise
                val data = skrape(HttpFetcher) {
                    request {
                        // Tell skrape{it} which URL to fetch data from
                        if (preloadData != null) {
                            url = preloadData
                        }
                    }


                    // Main function where you'll parse web data
                    extractIt<MyExtractedData> { it ->
                    htmlDocument{
                            // Main function where you'll parse web data
                       //todo get good

                        }

                    }

                }

            }
        }
        //if there is a given name load the data of that recipe
        if (recipeName != null){

            //get token
            val token = DbTokenHandling(
                getSharedPreferences(
                    "com.example.rezepte.dropboxintegration",
                    MODE_PRIVATE
                )
            ).retrieveAccessToken()
            //load saved data about recipe
            val downloader = DownloadTask(DropboxClient.getClient(token))
            GlobalScope.launch {


                //get data
                val data : String = downloader.GetXml("/xml/$recipeName.xml")

                val extractedData = xmlExtraction().GetData(data)

                withContext(Dispatchers.Main){
                    //show data
                    findViewById<TextView>(R.id.recipeNameInput).text = extractedData.data.name
                    findViewById<TextView>(R.id.authorInput).text = extractedData.data.author
                    findViewById<TextView>(R.id.servingsInput).text = extractedData.data.serves
                    findViewById<TextView>(R.id.timeInput).text = extractedData.data.speed
                    findViewById<TextView>(R.id.ovenTemp).text = extractedData.data.temperature.toString()

                    var ingredients = StringBuilder()
                    for (ingredient in extractedData.ingredients.list){
                        ingredients.append("${ingredient.text}\n")
                    }
                    var instructions = StringBuilder()
                    for (instruction in extractedData.instructions.list){
                        instructions.append("${instruction.text}\n")
                    }
                    findViewById<TextView>(R.id.ingridientsinput).text = ingredients.toString().trimEnd('\n')
                    findViewById<TextView>(R.id.mainInstructionsInput).text = instructions.toString().trimEnd('\n')

                }
                //load image after text is loaded
                val image = downloader.GetImage("/image/",recipeName)
                withContext(Dispatchers.Main){
                    //if there is a linked image show it
                    if (image != null){
                        findViewById<ImageView>(R.id.RecipeImage).setImageBitmap(image)
                    }
                }
            }
        }
        //delete button
        findViewById<Button>(R.id.btnDeleteRecipe).setOnClickListener{
            //comfirm delete
            if (recipeName != null) //only need to delete files if it was save before
            {
                AlertDialog.Builder(this)
                    .setTitle("Delete Recipe")
                    .setMessage("Do you really want to delete this recipe?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes,
                        DialogInterface.OnClickListener { dialog, whichButton ->
                            Toast.makeText(
                                this@CreateActivity,
                                "deleted recipe",
                                Toast.LENGTH_SHORT
                            ).show()
                            //delete the recipe
                            //remove files
                            val token = DbTokenHandling( //get token
                                getSharedPreferences(
                                    "com.example.rezepte.dropboxintegration",
                                    MODE_PRIVATE
                                )
                            ).retrieveAccessToken()
                            val downloader = DownloadTask(DropboxClient.getClient(token))
                            GlobalScope.launch {
                                //remove xml
                                downloader.RemoveFile("/xml/", "$recipeName.xml")
                                //remove image
                                downloader.RemoveImage("/image/", recipeName)

                            }


                            //go home
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent);
                        })

                    .setNegativeButton(android.R.string.no, null).show()
            }
            else //otherwise just return home
            {
                //go home
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent);
            }
        }

        findViewById<Button>(R.id.finishButton).setOnClickListener {
            Toast.makeText(this, "New Recipe Added", Toast.LENGTH_SHORT).show()

            //export saved recipe
            val data: String= parseData()

            //todo
            //get image if one is set
            var file : File? = null
            if (imageUri != null){
                file = File(URI_to_Path.getPath(application, imageUri!!))
            }


            var name = (findViewById<View>(R.id.recipeNameInput)as TextView ).text
            UploadTask(DropboxClient.getClient(ACCESS_TOKEN), file, applicationContext,name.toString(),data.toString()).execute()
            //if the name has changed delete old files
            if ( name != recipeName && recipeName != null){
                val token = DbTokenHandling( //get token
                    getSharedPreferences(
                        "com.example.rezepte.dropboxintegration",
                        MODE_PRIVATE
                    )
                ).retrieveAccessToken()
                val downloader = DownloadTask(DropboxClient.getClient(token))
                GlobalScope.launch {
                    //remove xml
                    downloader.RemoveFile("/xml/", "$recipeName.xml")
                    //remove image
                    downloader.RemoveImage("/image/", recipeName)

                }
            }

            //move to home
            val intent = Intent(this,MainActivity::class.java)
            startActivity(intent);
        }
        findViewById<Button>(R.id.quitButton).setOnClickListener {
            Toast.makeText(this, "Forgetting Recipe", Toast.LENGTH_SHORT).show()

            //move to home
            val intent = Intent(this,MainActivity::class.java)
            startActivity(intent);
        }
        //add image button
        findViewById<Button>(R.id.btnAddImage).setOnClickListener {
            selectImage()
        }
        //set access token
        ACCESS_TOKEN = retrieveAccessToken()
    }
    private fun retrieveAccessToken(): String? {
        //check if ACCESS_TOKEN is stored on previous app launches
        val prefs = getSharedPreferences("com.example.rezepte.dropboxintegration", MODE_PRIVATE)
        val accessToken = prefs.getString("access-token", null)
        return if (accessToken == null) {
            Log.d("AccessToken Status", "No token found")
            null
        } else {
            //accessToken already exists
            Log.d("AccessToken Status", "Token exists")
            accessToken
        }
    }


    private fun selectImage() {
        //Select image to upload
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        startActivityForResult(
            Intent.createChooser(
                intent,
                "Upload to Dropbox"
            ), IMAGE_REQUEST_CODE
        )
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return
        // Check which request we're responding to
        if (requestCode == IMAGE_REQUEST_CODE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                if (data != null){
                    imageUri = data.data!!
                    //set recipie image to selected image
                    val output = findViewById<ImageView>(R.id.RecipeImage) as ImageView
                    output.setImageURI(imageUri)


                }
            }
        }
    }
    private fun parseData(): String    {
        //get info from layout
        var name = if (findViewById<TextView>(R.id.recipeNameInput).text.toString() !="") findViewById<TextView>(R.id.recipeNameInput).text else "null"
        var temperature = if (findViewById<TextView>(R.id.ovenTemp).text.toString() != "") findViewById<TextView>(R.id.ovenTemp).text else "0"
        var author = if (findViewById<TextView>(R.id.authorInput).text.toString() != "") findViewById<TextView>(R.id.authorInput).text else " "
        var cookingTime = if (findViewById<TextView>(R.id.timeInput).text.toString() != "") findViewById<TextView>(R.id.timeInput).text else " "
        var servings = if (findViewById<TextView>(R.id.servingsInput).text.toString() != "") findViewById<TextView>(R.id.servingsInput).text else " "
        var ingredients = if (findViewById<TextView>(R.id.ingridientsinput).text.toString() != "") findViewById<TextView>(R.id.ingridientsinput).text else " "
        var instructions = if (findViewById<TextView>(R.id.mainInstructionsInput).text.toString() != "") findViewById<TextView>(R.id.mainInstructionsInput).text else " "
        val recipe = xml("recipe") {
            "data" {
                "name" {
                    - name.toString()
                }
                "temperature" {
                    - temperature.toString()
                }
                "author" {
                    - author.toString()
                }
                "servings" {
                    - servings.toString()
                }
                "speed" {
                    - cookingTime.toString()
                }
            }
            "ingredients" {
                "list" {
                    var index = 0
                    for (ingredient in ingredients.split("\n")) {
                        "entry" {
                            attribute("index", index)
                            "value" {
                                -ingredient
                            }
                        }
                        index += 1
                    }
                }
            }
            "instructions" {
                "list" {


                    var index = 0
                    for (instruction in instructions.split("\n")) {
                        "entry" {
                            attribute("index", index)
                            "value" {
                                -instruction
                            }
                        }
                        index += 1
                    }
                }
            }


        }

        return recipe.toString(true)


    }




}

data class MyExtractedData(
    var text: String = "",
)