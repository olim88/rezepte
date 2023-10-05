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


data class Data(val name: String, val temperature: Int, val author : String,val serves : String, val speed : String){
    companion object {
        fun xml(k: Konsumer): Data {
            k.checkCurrent("data")
            return Data(k.childText("name"),k.childInt("temperature"),k.childText("author"),k.childText("servings"),k.childText("speed"))
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