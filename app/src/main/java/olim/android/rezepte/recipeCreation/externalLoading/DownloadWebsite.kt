package olim.android.rezepte.recipeCreation.externalLoading

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import olim.android.rezepte.CookingSteps
import olim.android.rezepte.Ingredient
import olim.android.rezepte.Ingredients
import olim.android.rezepte.Instruction
import olim.android.rezepte.Instructions
import olim.android.rezepte.Recipe
import olim.android.rezepte.getEmptyRecipe
import olim.android.rezepte.recipeCreation.CreateAutomations
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL

class DownloadWebsite {
    companion object {
        private var json = Json {
            ignoreUnknownKeys = true
        }

        fun main(websiteUrl: String, settings: Map<String, String>): Pair<Recipe, String> {
            var recipe = getEmptyRecipe()
            var imageLink = ""
            //set the website of the recipe
            recipe.data.website = websiteUrl
            //get the website
            val doc: Document = Jsoup.connect(websiteUrl).get()
            //get the data
            for (tag in doc.select("script[type=application/ld+json]")) { //there can be multiple tags with this make sure we find the one with the recipe by looking at all of them
                var data = tag?.let { Json.parseToJsonElement(it.html()) }
                //uses formatting from stander https://developers.google.com/search/docs/appearance/structured-data/recipe#recipe-properties
                //the json may all be in a list for some reason so if that is the case sort it out
                if (data is JsonArray) {
                    data = data[0]
                }
                if (data != null) {//there is the data needed from the website and we just need to convert the json into a recipe and load that in the app
                    if (data.jsonObject.contains("@graph")) { //graph seems to be multiple in one so extract it out of this first then check each option in that

                        val options =
                            json.decodeFromJsonElement<List<JsonElement>>(data.jsonObject["@graph"]!!)
                        //loop though the scheme until end or one with recipe is found
                        for (option in options) {
                            val output = findAndExtractRecipe(option, recipe)
                            if (output != null) {
                                recipe = output.first
                                imageLink = output.second
                                break
                            }
                        }
                    } else {
                        val output = findAndExtractRecipe(data, recipe)
                        if (output != null) {
                            recipe = output.first
                            imageLink = output.second
                        }
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
                "intelligent" -> recipe.instructions = CreateAutomations.Companion.autoSplitInstructions(
                    recipe.instructions,
                    CreateAutomations.Companion.InstructionSplitStrength.Intelligent
                )

                "sentences" -> recipe.instructions = CreateAutomations.Companion.autoSplitInstructions(
                    recipe.instructions,
                    CreateAutomations.Companion.InstructionSplitStrength.Sentences
                )

                else -> {}
            }

            if (settings["Creation.Website Loading.Generate cooking steps"] == "true") {
                val stepsAndLinks =
                    CreateAutomations.Companion.autoGenerateStepsFromInstructions(recipe.instructions)
                recipe.data.cookingSteps = CookingSteps(stepsAndLinks.first.toMutableList())
                recipe.instructions = stepsAndLinks.second
            }
            //return the recipe and image link if there is one
            return Pair(recipe, imageLink)
        }

        /**
         * searches in the scheme for a recipe or how to and then extracts the recipe from that
         * @param data the json for a websites scheme data
         * @param recipe the recipe to add data to
         * @return the recipe loaded from the scheme and image link for the recipe
         */
        private fun findAndExtractRecipe(
            data: JsonElement,
            recipe: Recipe
        ): Pair<Recipe, String>? {
            //get the @type of the recipe could be a list or string or just not there ( if it is not there just quit)
            if (!data.jsonObject.contains("@type")) {
                return null
            }
            val type = try {
                Json.decodeFromJsonElement<String>(data.jsonObject["@type"]!!)
            } catch (e: Exception) {
                try {
                    Json.decodeFromJsonElement<List<String>>(data.jsonObject["@type"]!!)[0]
                } catch (e: Exception) {
                    "null"//make sure it does not try to pass a recipe
                }
            }


            if (type == "Recipe") { //make sure the website data is for a recipe
                val extractedData = json.decodeFromJsonElement<ExtractedData>(data)
                return extractRecipe(extractedData, recipe);


            } else if (type == "HowTo") { //if a website that is a how to is used only has instructions and image but still works
                val howToData = json.decodeFromJsonElement<ExtractedHowTo>(data)
                val extractedData = ExtractedData(howToData.image, howToData.name, howToData.publisher, listOf(), howToData.step, null)
                return extractRecipe(extractedData, recipe);
            }
            return null
        }

        /**
         * extracts a recipe and convert it into usable format
         * @param extractedData the data from the website for the recipe
         * @param recipe base recipe to start with
         * @return the filled out recipe and string for image link in the recipe
         */
        private fun extractRecipe(extractedData: ExtractedData, recipe: Recipe): Pair<Recipe, String> {

            recipe.data.name = extractedData.name
            //find the author
            recipe.data.author = if (extractedData.author is JsonArray) {
                json.decodeFromJsonElement<List<Author>>(extractedData.author)[0].name

            } else {
                json.decodeFromJsonElement<Author>(extractedData.author).name
            }

            //find the servings either string int  and could be in a list or not given
            if (extractedData.recipeYield != null) {
                val servingsItem = if (extractedData.recipeYield is JsonArray) {
                    json.decodeFromJsonElement<List<JsonElement>>(extractedData.recipeYield)[0]
                } else {
                    extractedData.recipeYield
                }

                if (servingsItem.jsonPrimitive.intOrNull != null) {
                    recipe.data.serves = "${servingsItem.jsonPrimitive.int} servings"
                } else {
                    recipe.data.serves = servingsItem.jsonPrimitive.content
                }
            }

            //handle ingredients
            val ingredients: MutableList<Ingredient> = mutableListOf()
            for ((index, ingredient) in extractedData.recipeIngredient.withIndex()) {
                if (ingredient is JsonPrimitive) {
                    ingredients.add(Ingredient(index, ingredient.jsonPrimitive.content))
                } else if (ingredient is JsonArray) {
                    //the is multiple lists of ingredients add whole list
                    for ((subIndex, subIngredient) in ingredient.jsonArray.withIndex()) {
                        ingredients.add(
                            Ingredient(
                                (index + 1) * subIndex,
                                subIngredient.jsonPrimitive.content
                            )
                        )
                    }
                }

            }
            recipe.ingredients = Ingredients((ingredients))

            //handle instructions different formats
            val instructions: MutableList<Instruction> = mutableListOf()
            if (extractedData.recipeInstructions is JsonArray) {
                //if just list of strings or "Howto" elements loop though each one and convert to correct format
                for ((index, instruction) in extractedData.recipeInstructions.jsonArray.withIndex()) {
                    if (instruction is JsonPrimitive) {
                        instructions.add(Instruction(index, instruction.content, null))
                    }else {
                        instructions.add(
                            Instruction(
                                index,
                                json.decodeFromJsonElement<InstructionJson>(instruction).text,
                                null
                            )
                        )
                    }
                }
            } else {
                //the instructions is a html list so decode that into normal list
                val instructionsData =
                    extractedData.recipeInstructions.jsonPrimitive.content
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
            recipe.instructions = Instructions((instructions))

            //get the image from the data could be a list so just grab first one
            var imageLink = "";
            if (extractedData.image != null) {
                imageLink = if (extractedData.image is JsonArray) {
                    extractedData.image.jsonArray[0].jsonPrimitive.content
                } else if (extractedData.image is JsonPrimitive) {
                    extractedData.image.jsonPrimitive.content
                } else {
                    json.decodeFromJsonElement<Image>(extractedData.image).url
                }
            }
            return Pair(recipe, imageLink)
        }


        /**
         * some instructions are formatted using html extract the form this and just return the text of the instructions
         *  @param instructions the html formatted instructions
         *  @return plain text of the instructions
         */
        private fun convertHtmlInstructions(instructions: String): List<String> {
            val output: MutableList<String> = mutableListOf();
            //if the instructions have no html tags in it assume it is just plain text and add it all as one instruction
            if (instructions.contains("<")) {
                val html = Jsoup.parse(instructions)
                for (element in html.getElementsByTag("li")) {
                    output.add(element.text())
                }
            } else {
                output.add(instructions)
            }

            return output
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
    val image: JsonElement? = null,
    val name: String,
    val author: JsonElement,
    val recipeIngredient: List<JsonElement>,
    val recipeInstructions: JsonElement,
    val recipeYield: JsonElement?
)

@Serializable
private data class Author(val name: String)

@Serializable
private data class Image(val url: String)

@Serializable
private data class InstructionJson(val text: String)

@Serializable
private data class ExtractedHowTo(
    val image: JsonElement? = null,
    val name: String,
    val publisher: JsonElement,
    val step: JsonElement
)


