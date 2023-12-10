package com.example.rezepte

import com.gitlab.mvysny.konsumexml.Konsumer
import com.gitlab.mvysny.konsumexml.konsumeXml

class XmlExtraction {
    companion object {
        fun getData(xmlData: String): Recipe {
            //extract data
            val extractedData = xmlData.konsumeXml().use { k ->
                k.child("recipe") {
                    Recipe.xml(this)

                }
            }
            return extractedData
        }

        fun getSearchData(xmlData: String): SearchData {
            //extract data
            val extractedData = xmlData.konsumeXml().use { k ->
                k.child("searchData") {
                    SearchData.xml(this)

                }
            }
            return extractedData
        }
    }
}


data class Data(var name: String, var author : String, var serves : String, var cookingSteps : CookingSteps, var website: String?, var notes :String?, var linked: LinkedRecipes?){
    companion object {
        fun xml(k: Konsumer): Data {
            k.checkCurrent("data")
            return Data(k.childText("name"),
                k.childText("author"),
                k.childText("servings"),
                k.child("cookingSteps"){CookingSteps.xml(this)},
                k.childTextOrNull("website"),
                k.childTextOrNull("notes"),
                k.childOrNull("linkedRecipes"){LinkedRecipes.xml(this)})
        }

    }
}
data class CookingStepContainer( var type : TinOrPanOptions, var dimensionOne : Float?, var dimensionTwo : Float?, var volume : Float?){
    companion object {
        fun xml(k: Konsumer): CookingStepContainer {

            return CookingStepContainer( enumValueOf(k.childText("type")),k.childTextOrNull("tinSize")?.toFloatOrNull(),k.childTextOrNull("tinSizeTwo")?.toFloatOrNull(),k.childTextOrNull("tinVolume")?.toFloatOrNull())
        }

    }
}
data class CookingStepTemperature( var temperature : Int?, var hobTemperature: HobOption, var isFan : Boolean?){
    companion object {
        fun xml(k: Konsumer): CookingStepTemperature {

            return CookingStepTemperature(k.childTextOrNull("temperature")?.toInt(),enumValueOf(k.childText("hobTemperature")),k.childTextOrNull("isFan")=="true")
        }

    }
}
data class CookingStep(var index: Int, var time : String, var type: CookingStage, var container : CookingStepContainer?,var cookingTemperature: CookingStepTemperature?){
    companion object {
        fun xml(k: Konsumer): CookingStep {

            return CookingStep(k.attributes["index"].toInt(),k.childText("time"),
                enumValueOf(k.childText("cookingStage")),
                k.childOrNull("cookingStepContainer"){CookingStepContainer.xml(this)},
                k.childOrNull("cookingStepTemperature"){CookingStepTemperature.xml(this)},
                )
        }

    }
}
data class CookingSteps(var list: MutableList<CookingStep>){
    companion object {
        fun xml(k: Konsumer): CookingSteps {
            k.checkCurrent("cookingSteps")
            return CookingSteps((k.child("list") { children("entry") { CookingStep.xml(this) } }.toMutableList()))
        }

    }
}

data class LinkedRecipe(var name : String){
    companion object {
        fun xml(k: Konsumer): LinkedRecipe {

            return LinkedRecipe(k.childText("value"))
        }

    }
}
data class LinkedRecipes(var list: MutableList<LinkedRecipe>){
    companion object {
        fun xml(k: Konsumer): LinkedRecipes {
            k.checkCurrent("linkedRecipes")
            return LinkedRecipes(k.child("list") { children("entry") { LinkedRecipe.xml(this) }.toMutableList() })
        }

    }
}

data class Ingredient(var index: Int, var text : String){
    companion object {
        fun xml(k: Konsumer): Ingredient {

            return Ingredient(k.attributes["index"].toInt(),k.childText("value"))
        }

    }
}
data class Ingredients(var list: List<Ingredient>){
    companion object {
        fun xml(k: Konsumer): Ingredients {
            k.checkCurrent("ingredients")
            return Ingredients(k.child("list") { children("entry") { Ingredient.xml(this) } })
        }

    }
}
data class Instruction(var index: Int, var text: String, var linkedCookingStepIndex: Int?){
    companion object {
        fun xml(k: Konsumer): Instruction {

            return Instruction(k.attributes["index"].toInt(),k.childText("value"),k.childTextOrNull("linkedStep")?.toIntOrNull())
        }

    }
}
data class Instructions(var list: List<Instruction>){
    companion object {
        fun xml(k: Konsumer): Instructions {
            k.checkCurrent("instructions")
            return Instructions(k.child("list") { children("entry") { Instruction.xml(this) } })
        }

    }
}
data class Recipe(var data : Data, var ingredients : Ingredients, var instructions : Instructions){
    companion object {
        fun xml(k: Konsumer): Recipe {
            k.checkCurrent("recipe")
            return Recipe(k.child("data"){Data.xml(this)},k.child("ingredients"){Ingredients.xml(this)},k.child("instructions"){Instructions.xml(this)})
        }

    }
}

enum class CookingStage(val text: String){
    prep("prep"),
    hob("hob"),
    oven("oven"),
    fridge("fridge"),
    wait("wait"),
}
enum class TinOrPanOptions(val text: String,val sizeing: TinOrPanSizeOptions){
    none("none",TinOrPanSizeOptions.None),
    fryingPan("frying pan",TinOrPanSizeOptions.None),
    wok("wok",TinOrPanSizeOptions.None),
    saucePan("sauce pan",TinOrPanSizeOptions.None),
    bowl("bowl",TinOrPanSizeOptions.None),
    tray("tray",TinOrPanSizeOptions.None),
    roastingTin("roasting tin",TinOrPanSizeOptions.None),
    roundTin("round tin",TinOrPanSizeOptions.OneDimension),
    rectangleTin("rectangular tin",TinOrPanSizeOptions.TwoDimension),
    loafTin("loaf tin",TinOrPanSizeOptions.TwoDimension),
    dish("dish",TinOrPanSizeOptions.Volume),

}
enum class TinOrPanSizeOptions{
    None,
    OneDimension,
    TwoDimension,
    Volume
}

enum class HobOption(val text: String){
    zero ("none"),
    low("low"),
    lowMedium("medium low"),
    medium("medium"),
    highMedium("medium high"),
    high("high"),
    max("max"),
}

fun GetEmptyRecipe() : Recipe{
    return Recipe(
        Data(
            "",
            "",
            "",
            CookingSteps(mutableListOf() ),
            null,
            null,
            null
        ),
        Ingredients(listOf()),
        Instructions(listOf())
    )
}

data class SearchData(var data : MutableList<BasicData>){
    companion object {
        fun xml(k: Konsumer): SearchData {
            k.checkCurrent("searchData")
            return SearchData(k.child("list") { children("entry") { BasicData.xml(this) } }.toMutableList())
        }
    }
}
data class BasicData(var name: String, var servings:String, var author: String ){
    companion object{
        fun xml(k: Konsumer): BasicData {

            return BasicData(k.childText("name"),k.childText("servings"),k.childText("author"))
        }
    }
}
fun getEmptySeachData():SearchData {
    return SearchData(mutableListOf())
}