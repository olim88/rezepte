package com.example.rezepte

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.Text.TextBlock
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.math.min

private val Text.TextBlock.lineHeight: Int?
    get() {
        //get the hight of the block
        return (this.boundingBox?.height() ?: return null) /this.lines.count()


    }

class ImageToRecipe {
    companion object{

        private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        fun convert (imageUri : Uri,context : Context, callback: (Recipe) -> Unit) {
            val image  = InputImage.fromFilePath(context,imageUri)
            val recipe = GetEmptyRecipe()
            val result = recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    // Task completed successfully
                    //get only the blocks with high enough probability
                    val textBlocks = mutableListOf<TextBlock>()
                    visionText.textBlocks.forEach {
                        var confidence = 0f
                        for (line in it.lines){
                            confidence += line.confidence
                        }
                        //if it is confident enough overall add it to the text blocks
                        if (confidence/it.lines.count() > 0.4){
                            textBlocks.add(it)
                        }
                    }
                    //find and sort the needed elements in the image
                    //the indexes of the textBlocks based on top left going from top to bottom and left to right
                    val verticalySorted = (0..textBlocks.count()-1).toMutableList()
                    val horizontalSorted  = (0..textBlocks.count()-1).toMutableList()
                    //the indexes of the line height for the textblocks
                    val lineHeightSorted = (0..textBlocks.count()-1).toMutableList()

                    // sort to order the indexes
                    horizontalSorted.sortBy {
                        textBlocks[it].cornerPoints?.get(0)?.x
                    }
                    verticalySorted.sortBy {
                        textBlocks[it].cornerPoints?.get(0)?.y
                    }
                    lineHeightSorted.sortBy {
                        textBlocks[it].lineHeight
                    }
                    lineHeightSorted.reverse()
                    //debug
                    println(horizontalSorted)
                    println(verticalySorted)
                    println(lineHeightSorted)
                    println(textBlocks[verticalySorted[0]].lineHeight)
                    for (int in 0..textBlocks.count()-1){
                        println("$int: ${textBlocks[int].text}")
                    }

                    //find the title (going to be the largest thing in the top 5 blocks)
                    var titleIndex = -1
                     for (index in lineHeightSorted){
                         println("line ${textBlocks[index].text},${verticalySorted.slice(0..5)}")
                        //if in to 5 return index
                        if (verticalySorted.slice(0..5).contains(index)){
                            recipe.data.name  = cleanTitle(textBlocks[index].text)
                            titleIndex= index
                            break
                        }
                    }
                    //find the servings (look at top 5 elements on the page)
                    var servingsIndex = -1
                    for (index in 0..min(5,verticalySorted.count()-1)){
                        //if they have a keyword set the servings and are not to long
                        if (textBlocks[verticalySorted[index]].text.lowercase().contains("ma[kr]es?|serving?s|serves".toRegex()) && textBlocks[verticalySorted[index]].text.length < 30 ){
                            recipe.data.serves  = cleanTitle(textBlocks[verticalySorted[index]].text)
                            servingsIndex = index
                            break
                        }
                    }
                    //find columns of items
                    val colums = mutableMapOf<Int,MutableList<Int>>(Pair(0, mutableListOf()))
                    var currentCol = 0
                    for (blockIndex in horizontalSorted){
                        //skip first and add to first colum
                        if (blockIndex == horizontalSorted[0]){
                            colums[currentCol]!!.add(blockIndex)
                            continue
                        }
                        //do not add the servings
                        if (blockIndex == servingsIndex){
                            continue
                        }
                        if (textBlocks[blockIndex].cornerPoints != null){
                            //if the corrners are on a similar x value add to the current col else increase col number and add it to that
                            val thisX = textBlocks[blockIndex].cornerPoints!![0].x
                            val range = textBlocks[blockIndex].lineHeight!!
                            if (textBlocks[colums[currentCol]!!.last()].cornerPoints!![0].x in thisX-range..thisX+range){
                                colums[currentCol]!!.add(blockIndex)
                            }else {
                                currentCol += 1
                                colums[currentCol] = mutableListOf(blockIndex)
                            }
                        }

                    }
                    //sort the elements in the colums by each of the heights
                    //reorder based on the vertical height of the text blocks
                    for (col in colums){
                         col.value.sortBy {
                            textBlocks[it].cornerPoints?.get(0)?.y
                        }
                    }
                    //even though they are going to be horizontally in a colum there may be big spaces between and this needs to be separated out
                    val newColumns = mutableMapOf<Int,MutableList<Int>>()
                    currentCol =0
                    println("old$colums")
                    for (colList in colums.values){ //loop though each col
                        for (index in colList){
                            //if first in col continue and add to first
                            if (index == colList[0]) {
                                newColumns[currentCol] = mutableListOf(index)
                                continue
                            }
                            //if there is more than a line between 2 blocks start a new colum

                            if (textBlocks[index].cornerPoints != null && textBlocks[newColumns[currentCol]!!.last()].cornerPoints != null){
                                println("$index:${textBlocks[index].cornerPoints!![0].y - textBlocks[newColumns[currentCol]!!.last()].cornerPoints!![3].y}")
                                println("${textBlocks[index].lineHeight!!},${textBlocks[newColumns[currentCol]!!.last()].lineHeight!!}")
                                if (textBlocks[index].cornerPoints!![0].y - textBlocks[newColumns[currentCol]!!.last()].cornerPoints!![3].y > min(textBlocks[index].lineHeight!!,textBlocks[newColumns[currentCol]!!.last()].lineHeight!!)   ){
                                    //there is to big of a gap start new col
                                    currentCol +=1
                                    newColumns[currentCol] = mutableListOf(index)
                                }else {
                                    //extend the colum
                                    newColumns[currentCol]?.add(index)
                                }
                            }

                        }
                        currentCol += 1
                    }

                    println("new$newColumns")
                    var longest = mutableListOf<Int>()
                    var secondLongest = mutableListOf<Int>()
                    for (col in newColumns.values){
                        //if contains title continue
                        if (col.contains(titleIndex)) {
                            continue
                        }
                        //if longer than longest replace and move longest to second
                        if (longest.count() < col.count()){
                            secondLongest = longest
                            longest = col
                            continue
                        }
                        //if not longer than longest but longer than second replace that
                        if (secondLongest.count() < col.count()){
                            secondLongest = col
                        }
                    }
                    //the ingredients should be shorter on average so that is how we can distinguish them
                    var longestTotal = 0
                    for (blockIndex in longest){
                        longestTotal += textBlocks[blockIndex].text.length
                    }
                    var secondLongestTotal = 0
                    for (blockIndex in secondLongest){
                        secondLongestTotal += textBlocks[blockIndex].text.length
                    }
                    //if the longest is the ingredients
                    val ingredients = if (longestTotal/longest.count() > secondLongestTotal/secondLongest.count()){
                        secondLongest
                    }else {
                        longest
                    }
                    val instructions = if (longestTotal/longest.count() > secondLongestTotal/secondLongest.count()){
                        longest
                    }else {
                        secondLongest
                    }

                    //clean up the text then create the elements then add to recipe
                    //ingredients
                    val recipeIngredients = mutableListOf<Ingredient>()
                    for ((listIndex,blockIndex) in (ingredients).withIndex()){
                        recipeIngredients.add(Ingredient(listIndex, cleanIngredient(textBlocks[blockIndex].text)))
                    }
                    recipe.ingredients = Ingredients(recipeIngredients)
                    //instructions
                    val recipeInstructions = mutableListOf<Instruction>()
                    for ((listIndex,blockIndex) in (instructions).withIndex()){
                        recipeInstructions.add(Instruction(listIndex, cleanInstruction(textBlocks[blockIndex].text),null))
                    }
                    recipe.instructions = Instructions(recipeInstructions)




                    callback(recipe)



                }
                .addOnFailureListener { e ->
                    // Task failed with an exception

                    Log.d("can't load image","$e")

                }



        }
        private fun capitalize(str: String): String {
            return str.trim().split("\\s+".toRegex())
                .joinToString(" ") { it.capitalize() }
        }
        private  fun cleanTitle(title: String): String {
            var newTitle = cleanText(title )
            //correct common miss spelling
            newTitle.replace (" mare"," make")
            //correct the case
            newTitle = capitalize(title.lowercase())

            return newTitle
        }
        private  fun cleanIngredient(ingredient: String): String {
            //clean stuff up that is only needed for ingredients
            var newIngredient = cleanText(ingredient )



            return  newIngredient
        }
        private  fun cleanInstruction(instruction: String): String {
            //clean stuff up that is only needed for instructions
            var newInstruction = cleanText(instruction)
            //remove numbering from the start todo do this with regex
            newInstruction = newInstruction.removePrefix("1 ")
            newInstruction = newInstruction.removePrefix("2 ")
            newInstruction = newInstruction.removePrefix("3 ")
            newInstruction = newInstruction.removePrefix("4 ")
            newInstruction = newInstruction.removePrefix("5 ")



            return  newInstruction
        }
        private  fun cleanText(text: String): String {
            //clean up general things in text
            var newText = text
            //fix numbers (often number is taken as a letter on its own)
            newText = newText.replace(" l "," 1 ")
            newText = newText.replace(" | "," 1 ")
            newText = newText.replace(" s "," 5 ")
            newText = newText.replace(" G "," 6 ")
            //fix other elements
            newText = newText.replace("|","/") 


            return  newText
        }
    }

}


