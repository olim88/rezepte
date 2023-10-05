package com.example.rezepte

import com.gitlab.mvysny.konsumexml.Konsumer
import com.gitlab.mvysny.konsumexml.childInt
import com.gitlab.mvysny.konsumexml.konsumeXml

class xmlExtraction {
    fun GetData(xmlData : String) : Recipe{
        //extract data
        val extractedData = xmlData.konsumeXml().use { k ->
            k.child("recipe") {
                Recipe.xml(this)

            }
        }
        return  extractedData
    }
}


data class Data(val name: String, val temperature: Int, val author : String,val serves : String, val speed : CookingSteps,val website: String?, val linked: LinkedRecipes?){
    companion object {
        fun xml(k: Konsumer): Data {
            k.checkCurrent("data")
            return Data(k.childText("name"),
                k.childInt("temperature"),
                k.childText("author"),
                k.childText("servings"),
                k.child("cookingSteps"){CookingSteps.xml(this)},
                k.childTextOrNull("website"),
                k.childOrNull("linkedRecipes"){LinkedRecipes.xml(this)})
        }

    }
}
data class CookingStepContainer( val type : TinOrPanOptions, val isRound: Boolean?, val size : Int?){
    companion object {
        fun xml(k: Konsumer): CookingStepContainer {

            return CookingStepContainer( enumValueOf(k.childText("value")),(k.childTextOrNull("roundTin")== "true"),k.childTextOrNull("tinSize")?.toInt())
        }

    }
}
data class CookingStepTemperature( val temperature : Int?, val hobTemperature: HobOption, val isFan : Boolean?){
    companion object {
        fun xml(k: Konsumer): CookingStepTemperature {

            return CookingStepTemperature(k.childTextOrNull("temperature")?.toInt(),enumValueOf(k.childText("hobTemperature")),k.childText("isFan")=="true")
        }

    }
}
data class CookingStep(val index: Int, val time : String, val type: CookingStage, val container : CookingStepContainer?,val cookingTemperature: CookingStepTemperature?){
    companion object {
        fun xml(k: Konsumer): CookingStep {

            return CookingStep(k.attributes["index"].toInt(),k.childText("value"),
                enumValueOf(k.childText("value")),
                k.childOrNull("cookingStepContainer"){CookingStepContainer.xml(this)},
                k.childOrNull("cookingStepTemperature"){CookingStepTemperature.xml(this)},
                )
        }

    }
}
data class CookingSteps(val list: List<CookingStep>){
    companion object {
        fun xml(k: Konsumer): CookingSteps {
            k.checkCurrent("cookingSteps")
            return CookingSteps(k.child("list") { children("entry") { CookingStep.xml(this) } })
        }

    }
}

data class LinkedRecipe(val name : String, val subRecipe: Boolean){
    companion object {
        fun xml(k: Konsumer): LinkedRecipe {

            return LinkedRecipe(k.childText("value"), k.childText("isSubRecipe")=="true")
        }

    }
}
data class LinkedRecipes(val list: List<LinkedRecipe>){
    companion object {
        fun xml(k: Konsumer): LinkedRecipes {
            k.checkCurrent("linkedRecipes")
            return LinkedRecipes(k.child("list") { children("entry") { LinkedRecipe.xml(this) } })
        }

    }
}

data class Ingredient(val index: Int, val text : String, var striked: Boolean){
    companion object {
        fun xml(k: Konsumer): Ingredient {

            return Ingredient(k.attributes["index"].toInt(),k.childText("value"),false)
        }

    }
}
data class Ingredients(val list: List<Ingredient>){
    companion object {
        fun xml(k: Konsumer): Ingredients {
            k.checkCurrent("ingredients")
            return Ingredients(k.child("list") { children("entry") { Ingredient.xml(this) } })
        }

    }
}
data class Instruction(val index: Int, val text: String, var striked: Boolean){
    companion object {
        fun xml(k: Konsumer): Instruction {

            return Instruction(k.attributes["index"].toInt(),k.childText("value"),false)
        }

    }
}
data class Instructions(val list: List<Instruction>){
    companion object {
        fun xml(k: Konsumer): Instructions {
            k.checkCurrent("instructions")
            return Instructions(k.child("list") { children("entry") { Instruction.xml(this) } })
        }

    }
}
data class Recipe(val data : Data, val ingredients : Ingredients, val instructions : Instructions){
    companion object {
        fun xml(k: Konsumer): Recipe {
            k.checkCurrent("recipe")
            return Recipe(k.child("data"){Data.xml(this)},k.child("ingredients"){Ingredients.xml(this)},k.child("instructions"){Instructions.xml(this)})
        }

    }
}

enum class CookingStage{
    prep,
    pan,
    oven,
    fridge,
    wait,
}
enum class TinOrPanOptions{
    fryingPan,
    wok,
    saucePan,
    tray,
    roundTin,
    rectangleTin,

}
enum class HobOption{
    zero,
    low,
    lowMedium,
    medium,
    highMedium,
    high,
    max,
}
