package com.example.rezepte.recipeCreation.externalLoading

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.rezepte.CookingSteps
import com.example.rezepte.GetEmptyRecipe
import com.example.rezepte.Ingredient
import com.example.rezepte.Ingredients
import com.example.rezepte.Instruction
import com.example.rezepte.Instructions
import com.example.rezepte.Recipe
import com.example.rezepte.recipeCreation.CreateAutomations
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL

class DownloadWebsite {
    companion object {
        private var json = Json {
            ignoreUnknownKeys = true
        }

        fun main(websiteUrl: String, settings: Map<String, String>): Pair<Recipe, String> {
            var recipe = GetEmptyRecipe()
            var imageLink = ""
            //set the website of the recipe
            recipe.data.website = websiteUrl
            //get the website
            val doc: Document = Jsoup.connect(websiteUrl).get()
            //get the data
            for (tag in doc.select("script[type=application/ld+json]")) { //there can be mutliple tags with this make sure we find the one with the recipe by looking at all of them
                var data = tag?.let { Json.parseToJsonElement(it.html()) }
                //uses formatting from stander https://developers.google.com/search/docs/appearance/structured-data/recipe#recipe-properties
                //the json may all be in a list for some reason so if that is the case sort it out
                try {
                    if (data != null) {
                        data = data.jsonArray[0]
                    }

                } catch (e: Exception) {
                }
                if (data != null) {//there is the data needed from the website and we just need to convert the json into a recipe and load that in the app{

                    //get the @type of the recipe could be a list or string or just not there ( if it is not there just quit)
                    val type = try {
                        Json.decodeFromJsonElement<String>(data.jsonObject["@type"]!!)
                    } catch (e: Exception) {
                        try {

                            Json.decodeFromJsonElement<List<String>>(data.jsonObject["@type"]!!)[0]
                        } catch (e: Exception) {
                            "null"//make sure it does not try to pas a recipe
                        }
                    }

                    if (type == "Recipe") { //make sure the website data is for a recipe
                        val extractedData = json.decodeFromJsonElement<ExtractedData>(data)
                        //set recipe values based on the extraced data
                        recipe.data.name = extractedData.name
                        //find the author
                        try {
                            recipe.data.author =
                                json.decodeFromJsonElement<Author>(extractedData.author).name
                        } catch (e: Exception) {
                            recipe.data.author =
                                json.decodeFromJsonElement<List<Author>>(extractedData.author)[0].name
                        }
                        //find the servings either string int  and could be in a list
                        val servingsItem = try {
                            json.decodeFromJsonElement<List<JsonElement>>(extractedData.recipeYield)[0]
                        } catch (e: Exception) {
                            extractedData.recipeYield
                        }
                        try {
                            val servings = json.decodeFromJsonElement<Int>(servingsItem)
                            recipe.data.serves = "$servings servings"
                        } catch (e: Exception) {
                            recipe.data.serves = json.decodeFromJsonElement(servingsItem)
                        }
                        //handle ingredients
                        val ingredients: MutableList<Ingredient> = mutableListOf()
                        for ((index, ingredient) in extractedData.recipeIngredient.withIndex()) {
                            ingredients.add(Ingredient(index, ingredient))
                        }
                        recipe.ingredients = Ingredients((ingredients))
                        //handle instructions diffrent formats try just a list then howto steps, or formatted as html list
                        val instructions: MutableList<Instruction> = mutableListOf()
                        try {
                            var instructionsData =
                                json.decodeFromJsonElement<List<String>>(extractedData.recipeInstructions)
                            for ((index, instruction) in instructionsData.withIndex()) {
                                instructions.add(Instruction(index, instruction, null))
                            }
                        } catch (e: Exception) {
                            try {
                                var instructionsData =
                                    json.decodeFromJsonElement<List<InstructionJson>>(extractedData.recipeInstructions)
                                for ((index, instruction) in instructionsData.withIndex()) {
                                    instructions.add(Instruction(index, instruction.text, null))
                                }
                            } catch (e: Exception) {
                                var instructionsData =
                                    json.decodeFromJsonElement<String>(extractedData.recipeInstructions)
                                var index = 0
                                for ((index, instruction) in convertHtmlInstructions(instructionsData).withIndex()) {
                                    instructions.add(
                                        Instruction(
                                            index,
                                            instruction,
                                            null
                                        )
                                    )
                                }
                            }
                        }
                        recipe.instructions = Instructions((instructions))
                        //get the image from the data
                        imageLink = try {
                            extractedData.image.jsonArray[0].toString().removeSuffix("\"")
                                .removePrefix("\"")
                        } catch (e: Exception) {
                            json.decodeFromJsonElement<Image>(extractedData.image).url
                        }

                        break
                    } else if (type == "HowTo") { //if a website that is a how to is used only has instructions and image but still works
                        val extractedData = json.decodeFromJsonElement<ExtractedHowTo>(data)
                        //set recipe values based on the extracted data
                        recipe.data.name = extractedData.name
                        //find the author
                        try {
                            recipe.data.author =
                                json.decodeFromJsonElement<Author>(extractedData.publisher).name
                        } catch (e: Exception) {
                            recipe.data.author =
                                json.decodeFromJsonElement<List<Author>>(extractedData.publisher)[0].name
                        }


                        //handle the steps and create instructions from them
                        val instructions: MutableList<Instruction> = mutableListOf()
                        try {
                            var instructionsData =
                                json.decodeFromJsonElement<List<String>>(extractedData.step)
                            for ((index, instruction) in instructionsData.withIndex()) {
                                instructions.add(Instruction(index, instruction, null))
                            }
                        } catch (e: Exception) {
                            try {
                                var instructionsData =
                                    json.decodeFromJsonElement<List<InstructionJson>>(extractedData.step)
                                for ((index, instruction) in instructionsData.withIndex()) {
                                    instructions.add(Instruction(index, instruction.text, null))
                                }
                            } catch (e: Exception) {
                                var instructionsData =
                                    json.decodeFromJsonElement<String>(extractedData.step)
                                for ((index, instruction) in convertHtmlInstructions(instructionsData).withIndex()) {
                                    instructions.add(
                                        Instruction(
                                            index,
                                            instruction,
                                            null
                                        )
                                    )
                                }
                            }
                        }
                        recipe.instructions = Instructions((instructions))
                        //get the image from the data
                        imageLink = try {
                            extractedData.image.jsonArray[0].toString().removeSuffix("\"")
                                .removePrefix("\"")
                        } catch (e: Exception) {
                            json.decodeFromJsonElement<Image>(extractedData.image).url
                        }
                        break
                    }
                }

            }
            //else error? or  custom
            if (websiteUrl.contains("www.nigella.com/recipes")) {//custom scape nigella.com as it dose not give out the needed info
                //find the title of the recipe
                recipe.data.name =
                    doc.getElementsByAttributeValueMatching("itemprop", "name")[0].text()
                //author is going to be nigella
                recipe.data.author = "Nigellla"
                //find the servings
                recipe.data.serves =
                    doc.getElementsByAttributeValueMatching("itemprop", "recipeYield")[0].text()
                //find the ingredients
                val ingredients: MutableList<Ingredient> = mutableListOf()
                for ((index, ingredient) in doc.getElementsByAttributeValueMatching(
                    "itemprop",
                    "recipeIngredient"
                ).withIndex()) {
                    ingredients.add(Ingredient(index, ingredient.text()))
                }
                recipe.ingredients = Ingredients(ingredients)
                //find the steps
                val instructions: MutableList<Instruction> = mutableListOf()
                val instructionsData =
                    doc.getElementsByAttributeValueMatching("itemprop", "recipeInstructions").html()
                for ((index, instruction) in convertHtmlInstructions(instructionsData).withIndex()) {
                    instructions.add(
                        Instruction(
                            index,
                            instruction,
                            null
                        )
                    )
                }
                recipe.instructions = Instructions(instructions)
                //find image linked to recipe
                imageLink = "https://www.nigella.com" + doc.getElementsByClass("image")[0].child(0)
                    .getElementsByAttributeValueMatching("itemprop", "image")[0].attr("src")


            }
            //check settings to see if extra needs to be done
            when (settings["Creation.Website Loading.Split instructions"]) {
                "intelligent" -> recipe.instructions = CreateAutomations.autoSplitInstructions(
                    recipe.instructions,
                    CreateAutomations.Companion.InstructionSplitStrength.Intelligent
                )

                "sentences" -> recipe.instructions = CreateAutomations.autoSplitInstructions(
                    recipe.instructions,
                    CreateAutomations.Companion.InstructionSplitStrength.Sentences
                )

                else -> {}
            }

            if (settings["Creation.Website Loading.Generate cooking steps"] == "true") {
                val stepsAndLinks =
                    CreateAutomations.autoGenerateStepsFromInstructions(recipe.instructions)
                recipe.data.cookingSteps = CookingSteps(stepsAndLinks.first.toMutableList())
                recipe.instructions = stepsAndLinks.second
            }
            //return the recipe and image link if there is one
            return Pair(recipe, imageLink)
        }

        /**
         * some instructions are formatted using html extract the form this and just return the text of the instructions
         *  @param instructions the html formatted instructions
         *  @return plain text of the instructions
         */
        private fun convertHtmlInstructions(instructions: String) : List<String> {
            var output : MutableList<String> = mutableListOf();
            val html = Jsoup.parse(instructions)
            for (element in html.getElementsByTag("li")) {
                output.add(element.text())
            }
            return  output
        }

        fun downloadImageToBitmap(url: String): Bitmap? {
            var image: Bitmap? = null
            try {
                val input = URL(url).openStream()
                image = BitmapFactory.decodeStream(input)
            } catch (e: Exception) {
                Log.e("Error", e.message!!)
                e.printStackTrace()
            }
            return image
        }
    }
}


@Serializable
private data class ExtractedData(
    val image: JsonElement,
    val name: String,
    val author: JsonElement,
    val recipeIngredient: List<String>,
    val recipeInstructions: JsonElement,
    val recipeYield: JsonElement
)

@Serializable
private data class Author(val name: String)

@Serializable
private data class Image(val url: String)

@Serializable
private data class InstructionJson(val text: String)

@Serializable
private data class ExtractedHowTo(
    val image: JsonElement,
    val name: String,
    val publisher: JsonElement,
    val step: JsonElement
)


