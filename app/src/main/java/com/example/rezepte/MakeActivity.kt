package com.example.rezepte

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt


class MakeActivity : AppCompatActivity()
{
    private lateinit var ExtractedData : Recipe
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.makelayout);

        findViewById<Button>(R.id.btnReturnHome).setOnClickListener {
            Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show()
            //start login

            //move to home
            val intent = Intent(this,MainActivity::class.java)
            startActivity(intent);

        }
        //strike though next when clicked
        findViewById<TextView>(R.id.ingredientsText).setOnClickListener{ //ingredients
            for ( i in 0 until ExtractedData.ingredients.list.count()){
                if (!ExtractedData.ingredients.list[i].striked ){
                    ExtractedData.ingredients.list[i].striked = true
                    break
                }
            }
            updateIngredients()
        }
        findViewById<TextView>(R.id.ingredientsText).setOnLongClickListener() {

            for ( i in (0 until ExtractedData.ingredients.list.count()).reversed()){
                print(i)
                if (ExtractedData.ingredients.list[i].striked ){
                    ExtractedData.ingredients.list[i].striked = false
                    break
                }
            }
            updateIngredients()
            true
        }

        findViewById<TextView>(R.id.recipeView).setOnClickListener{ //instructions
            for ( i in 0 until ExtractedData.instructions.list.count()){
                if (!ExtractedData.instructions.list[i].striked ){
                    ExtractedData.instructions.list[i].striked = true
                    break
                }
            }
            updateInstructions()
        }
        findViewById<TextView>(R.id.recipeView).setOnLongClickListener() {
            for ( i in (0 until ExtractedData.instructions.list.count()).reversed()){
                print(i)
                if (ExtractedData.instructions.list[i].striked ){
                    ExtractedData.instructions.list[i].striked = false
                    break
                }
            }
            updateInstructions()
            true
        }


        //edit values with multiply
        findViewById<TextView>(R.id.multiInput).doOnTextChanged { text, start, before, count ->
            val multiplier : Float = if (text.isNullOrEmpty()) {
                1f
            } else{
                text.toString().toFloat()
            }
            //adjust servings
            //find numbers
            findViewById<TextView>(R.id.ServingsTextBox).text = "Servings: ${multiplyNumbersInString(ExtractedData.data.serves,multiplier,RoundingOption.Int)}"
            //adjust ingredients
            var next = true //used to make the next ingredient larger
            var ingredients = SpannableStringBuilder()
            for (ingredient in ExtractedData.ingredients.list){
                val string = SpannableString("${intToRoman(ingredient.index + 1)}: ${multiplyNumbersInString(ingredient.text,multiplier,RoundingOption.Little)}\n")
                ingredients.append(string)
                if (ingredient.striked){
                    ingredients.setSpan(StrikethroughSpan(), ingredients.length-string.length,ingredients.length, 0)
                }
                else if (next){
                    next = false
                    ingredients.setSpan(RelativeSizeSpan(1.5f), ingredients.length-string.length,ingredients.length, 0)
                }

            }
            findViewById<TextView>(R.id.ingredientsText).text = ingredients


        }
        //if the keyboad is closed remove the focus on the input
        window.decorView.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            window.decorView.getWindowVisibleDisplayFrame(r)
            val height = window.decorView.height
            if (height - r.bottom <= height * 0.1399) {
                findViewById<TextView>(R.id.multiInput).clearFocus()
            }
            
        }
        

        //get the name of the recipe
        val extras = intent.extras
        var recipeName: String? = null
        if (extras != null) {
            recipeName = extras.getString("recipe name")
        }
        //if there is a name load that recipes data
        if (recipeName != null) {

            //edit recipe button
            findViewById<Button>(R.id.btnEditRecipe).setOnClickListener{
                //move to create
                val intent = Intent(this,CreateActivity::class.java)

                intent.putExtra("recipe name",recipeName)
                startActivity(intent);
            }
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
                ExtractedData = xmlExtraction().GetData(data)

                withContext(Dispatchers.Main){
                    //show data
                    findViewById<TextView>(R.id.recipeName).text = ExtractedData.data.name
                    findViewById<TextView>(R.id.AuthorTextBox).text = "Author: ${ExtractedData.data.author}"
                    findViewById<TextView>(R.id.ServingsTextBox).text = "Servings: ${ExtractedData.data.serves}"
                    findViewById<TextView>(R.id.TimeTextBox).text = "Time: ${ExtractedData.data.cookingSteps}"
                    findViewById<TextView>(R.id.CookingTempText).text = " "//"Temperature: ${ExtractedData.data.temperature}"
                    //compile Ingredients/Instructions into string
                    updateIngredients()

                    updateInstructions()






                }
                //load image after text is loaded
                val image = downloader.GetImage("/image/",recipeName)
                withContext(Dispatchers.Main){
                    //if there is a linked image show it
                    if (image != null){
                        findViewById<ImageView>(R.id.recipeImage).setImageBitmap(image)
                    }
                }

            }
        }




    }

    private fun updateInstructions() {
        var instructions = SpannableStringBuilder()
        var next = true //used to make the next instructions larger
        for (instruction in ExtractedData.instructions.list){
            val string = SpannableString("${intToRoman(instruction.index + 1)}: ${instruction.text}\n")
            instructions.append(string)
            if (instruction.striked){
                instructions.setSpan(StrikethroughSpan(), instructions.length-string.length,instructions.length, 0)
            }
            else if (next){
                next = false
                instructions.setSpan(RelativeSizeSpan(1.5f), instructions.length-string.length,instructions.length, 0)
            }
        }
        findViewById<TextView>(R.id.recipeView).text = instructions
    }

    private fun updateIngredients() {
        //update the ingredients list
        var next = true //used to make the next ingredient larger
        var ingredients = SpannableStringBuilder()
        for (ingredient in ExtractedData.ingredients.list){
            val string = SpannableString("${intToRoman(ingredient.index + 1)}: ${ingredient.text}\n")
            ingredients.append(string)
            if (ingredient.striked){
                ingredients.setSpan(StrikethroughSpan(), ingredients.length-string.length,ingredients.length, 0)
            }
            else if (next){
                next = false
                ingredients.setSpan(RelativeSizeSpan(2f), ingredients.length-string.length,ingredients.length, 0)
            }
        }
        findViewById<TextView>(R.id.ingredientsText).text = ingredients
    }

    private fun intToRoman(num: Int): String? {
        val M = arrayOf("", "M", "MM", "MMM")
        val C = arrayOf("", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM")
        val X = arrayOf("", "X", "XX", "XXX ", "XL", "L", "LX", "LXX", "LXXX", "XC")
        val I = arrayOf("", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX")
        return M[num / 1000] + C[num % 1000 / 100] + X[num % 100 / 10] + I[num % 10]
    }

    private fun multiplyNumbersInString(string :String, multiplier: Float, rounding: RoundingOption) : String {
        val numbers = Regex("[0-9]+(\\d*\\.)?[0-9]*").findAll(string)
            .map(MatchResult::value)
            .toList()
        //replace numbers with multiplied value
        var output = string
        for (number in numbers){
            var value = number.toFloat() * multiplier
            when (rounding){
                RoundingOption.Int -> value = value.roundToInt().toFloat()
                RoundingOption.Little -> value = (value*10).roundToInt().toFloat()/10
                RoundingOption.Some -> value = (value*100).roundToInt().toFloat()/1000
                else -> continue
            }
            output = output.replace(number,(value).toString().replace(".0",""))
        }
        return output
    }
    enum class RoundingOption{
        None,
        Some,
        Little,
        Int,
    }

}

