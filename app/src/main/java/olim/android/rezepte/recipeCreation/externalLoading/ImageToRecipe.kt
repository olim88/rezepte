package olim.android.rezepte.recipeCreation.externalLoading

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.os.Parcelable
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text.TextBlock
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.parcelize.Parcelize
import olim.android.rezepte.CookingSteps
import olim.android.rezepte.Ingredient
import olim.android.rezepte.Ingredients
import olim.android.rezepte.Instruction
import olim.android.rezepte.Instructions
import olim.android.rezepte.Recipe
import olim.android.rezepte.getEmptyRecipe
import olim.android.rezepte.recipeCreation.CreateAutomations
import kotlin.math.abs
import kotlin.math.min

private val TextBlock.lineHeight: Int?
    get() {
        //get the height of the block
        if (this.cornerPoints == null) return null
        val height =
            ((this.cornerPoints?.get(3)?.y!! - this.cornerPoints?.get(0)?.y!!) + (this.cornerPoints?.get(
                2
            )?.y!! - this.cornerPoints?.get(1)?.y!!)) / 2 //get avg height as should be more acc than bound height hopefully
        return height / this.lines.count()
    }

public enum class ScanBoxType(val color: Color) {
    Title(Color.Red),
    Servings(Color.Green),
    Ingredients(Color.Blue),
    Instructions(Color.Black),
    Extra(Color.Yellow),
}

enum class Edge {
    Top,
    Bottom,
    Left,
    Right,
}

@Parcelize
data class ScanBox(
    var start: Point,
    var end: Point,
    val type: ScanBoxType,
    var lastUpdate: Int = 0
) : Parcelable {
    fun height(): Int {
        return end.y - start.y
    }

    fun width(): Int {
        return end.x - start.x
    }

    operator fun plusAssign(offset: Offset) {
        start = Point((start.x + offset.x).toInt(), (start.y + offset.y).toInt());
        end = Point((end.x + offset.x).toInt(), (end.y + offset.y).toInt())
    }

    fun contains(x: Int, y: Int): Boolean {
        return x in start.x..end.x && y in start.y..end.y
    }

    fun contains(x: Float, y: Float): Boolean {
        return contains(x.toInt(), y.toInt())
    }

    private fun edgeContains(x: Int, y: Int, edge: Edge): Boolean {
        val edgeSize = 5;
        return when (edge) {
            Edge.Top -> x in start.x..end.x &&
                    y.toDouble() in (start.y - height() * 0.1)..(start.y + height() * 0.2)

            Edge.Bottom -> x in start.x..end.x &&
                    y.toDouble() in (end.y - height() * 0.2)..(end.y + height() * 0.1)

            Edge.Left -> y in start.y..end.y &&
                    x.toDouble() in (start.x - width() * 0.1)..(start.x + width() * 0.2)

            Edge.Right -> y in start.y..end.y &&
                    x.toDouble() in (end.x - width() * 0.2)..(end.x + width() * 0.1)
        }
    }

    fun edgeContains(x: Float, y: Float, edge: Edge): Boolean {
        return edgeContains(x.toInt(), y.toInt(), edge)
    }

    private fun expand(direction: Edge, amount: Int) {
        when (direction) {
            Edge.Top -> start.y += amount
            Edge.Bottom -> end.y += amount
            Edge.Left -> start.x += amount
            Edge.Right -> end.x += amount
        }
    }

    fun expand(direction: Edge, amount: Float, updateTime: Int) {
        expand(direction, amount.toInt())
        lastUpdate = updateTime
    }

    /**
     * If an input is in the top right (where delete button is) remove the box
     */
    fun detectDelete(x: Float, y: Float): Boolean {
        return y in (start.y - height() * 0.2)..(start.y + height() * 0.2) && //todo this is probably to big. for all these hitbox checks i should probably use some preset size instead of a percentage
                x in (end.x - width() * 0.2)..(end.x + width() * 0.2)

    }


}

@Parcelize
data class RecipeBounds(
    private val scanBoxes: MutableList<ScanBox> = mutableListOf()


) : Parcelable {

    fun add(newBox: ScanBox?) {
        newBox?.let { scanBoxes.add(it) }

    }

    fun addAll(newBox: List<ScanBox>?) {
        newBox?.let { scanBoxes.addAll(it) }

    }

    fun find(type: ScanBoxType): ScanBox? =
        scanBoxes.firstOrNull { it.type == type }

    fun findAll(type: ScanBoxType): List<ScanBox> = scanBoxes.filter { it.type == type }
    fun title(): ScanBox? = find(ScanBoxType.Title)
    fun servings(): ScanBox? = find(ScanBoxType.Servings)
    fun ingredients(): List<ScanBox> = findAll(ScanBoxType.Ingredients) //todo sort from top left to get correct order
    fun instructions(): List<ScanBox> = findAll(ScanBoxType.Instructions)
    fun extra(): List<ScanBox> = findAll(ScanBoxType.Extra)

    fun size(): Int {
        return scanBoxes.size
    }

    fun removeDeleted(targetX: Float, targetY: Float): Boolean {
        return scanBoxes.removeAll {
            it.detectDelete(targetX, targetY)
        }
    }

    /**
     *  order by last update so we are moving the most recently moved
     */
    fun scanBoxesByLastUpdate(): List<ScanBox> =
        scanBoxes.sortedByDescending { it.lastUpdate }

}


@Parcelize
data class Block(
    val center: Point,
    val text: String,
    val lines : List<String>
) : Parcelable

class ImageToRecipe{
    companion object {
        private val recognizer by lazy {
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        }

        fun convert(
            imageUri: Uri,
            context: Context,
            settings: Map<String, String>,
            error: () -> Unit,
            callback: (Recipe) -> Unit
        ) {
            val image = InputImage.fromFilePath(context, imageUri)
            convert(image, imageUri, settings, error, callback, context)
        }

        fun convert(
            imageBitmap: Bitmap,
            settings: Map<String, String>,
            error: () -> Unit,
            callback: (Recipe) -> Unit,
            context: Context
        ) {
            val image = InputImage.fromBitmap(imageBitmap, 0)
            convert(image, null, settings, error, callback, context)
        }

        private fun convert(
            inputImage: InputImage,
            imageUri: Uri?,
            settings: Map<String, String>,
            error: () -> Unit,
            callback: (Recipe) -> Unit,
            context: Context
        ) {
            findBoundingBoxes(inputImage, error) { bounds, text ->
                val intent = Intent(context, BoxSelectionActivity::class.java)

                intent.putExtra("bounds", bounds)
                intent.putParcelableArrayListExtra("blocks", ArrayList(text))
                intent.putExtra("image_uri", imageUri)
                intent.putExtra("image_width", inputImage.width)
                intent.putExtra("image_height", inputImage.height)


                context.startActivity(intent)

                callback(boundsToRecipe(bounds, text, settings))

            }
        }


        fun getBox(corners: Array<Point>?, type: ScanBoxType): ScanBox? {
            if (corners.isNullOrEmpty()) {
                return null
            }
            corners.sortBy { point -> point.x + point.y }
            return ScanBox(corners.first(), corners.last(), type)
        }

        fun getBox(textBlocks: List<TextBlock>, type: ScanBoxType): ScanBox? {
            val boxes = textBlocks.map { block -> getBox(block.cornerPoints, type) }
            val starts = boxes.mapNotNull { box -> box?.start }
            val ends = boxes.mapNotNull { box -> box?.end }
            starts.sortedBy { point -> point.x + point.y }
            ends.sortedBy { point -> point.x + point.y }
            if (starts.isEmpty() || ends.isEmpty()) {
                return null
            }
            return ScanBox(starts.first(), ends.last(), type)
        }

        fun blockInBox(scanBox: ScanBox?, block: Block?): Boolean {
            if (scanBox == null || block == null) {
                return false
            }
            val point = block.center
            if (point.x >= scanBox.start.x && point.x <= scanBox.end.x) {
                if (point.y >= scanBox.start.y && point.y <= scanBox.end.y) {
                    return true
                }
            }
            return false
        }

        fun findCenter(block: TextBlock): Point {
            val points = block.cornerPoints!!

            val avgX = points.sumOf { it.x } / points.size.toFloat()
            val avgY = points.sumOf { it.y } / points.size.toFloat()

            val point = Point(avgX.toInt(), avgY.toInt())
            return point
        }

        fun blockInBoxes(ScanBoxes: List<ScanBox>, block: Block?): Boolean {
            return ScanBoxes.any { box -> blockInBox(box, block) }
        }

        fun findBoundingBoxes(
            inputImage: InputImage,
            error: () -> Unit,
            callback: (RecipeBounds, List<Block>) -> Unit

        ) {
            val recipeBounds = RecipeBounds()
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    // Task completed successfully
                    //get only the blocks with high enough probability
                    val textBlocks = mutableListOf<TextBlock>()
                    visionText.textBlocks.forEach {
                        var confidence = 0f
                        for (line in it.lines) {
                            confidence += line.confidence
                        }
                        //if it is confident enough overall add it to the text blocks
                        if (confidence / it.lines.count() > 0.4) {
                            textBlocks.add(it)
                        }
                    }
                    //if there are no text blocks found call error and return
                    if (textBlocks.isEmpty()) {
                        error()
                        return@addOnSuccessListener
                    }
                    //find and sort the needed elements in the image
                    //the indexes of the textBlocks based on top left going from top to bottom and left to right
                    val verticallySorted = (0..<textBlocks.count()).toMutableList()
                    val horizontalSorted = (0..<textBlocks.count()).toMutableList()
                    //the indexes of the line height for the textblocks
                    val lineHeightSorted = (0..<textBlocks.count()).toMutableList()

                    // sort to order the indexes
                    horizontalSorted.sortBy {
                        textBlocks[it].cornerPoints?.get(0)?.x
                    }
                    verticallySorted.sortBy {
                        textBlocks[it].cornerPoints?.get(0)?.y
                    }
                    lineHeightSorted.sortBy {
                        textBlocks[it].lineHeight
                    }
                    lineHeightSorted.reverse()


                    //find the title (going to be the largest thing in the top 6 blocks)
                    var titleIndex = -1
                    for (index in lineHeightSorted) {
                        //println("line ${textBlocks[index].text},${verticallySorted.slice(0..min(5,verticalySorted.count()-1))}")
                        //if in to 5 return index
                        if (verticallySorted.slice(0..min(5, verticallySorted.count() - 1))
                                .contains(index)
                        ) {
                            recipeBounds.add(
                                getBox(
                                    textBlocks[index].cornerPoints,
                                    ScanBoxType.Title
                                )
                            )
                            titleIndex = index
                            break
                        }
                    }
                    //find the servings (look at top 6 elements on the page)
                    var servingsIndex = -1
                    for (index in 0..min(5, verticallySorted.count() - 1)) {
                        //if they have a keyword set the servings and are not to long
                        if (textBlocks[verticallySorted[index]].text.lowercase()
                                .contains("ma[kr]es?|serving?s|serves".toRegex()) && textBlocks[verticallySorted[index]].text.length < 30
                        ) {
                            recipeBounds.add(
                                getBox(
                                    textBlocks[verticallySorted[index]].cornerPoints,
                                    ScanBoxType.Servings
                                )
                            )

                            servingsIndex = verticallySorted[index]
                            break
                        }
                    }
                    //find columns of items
                    val columns = mutableMapOf<Int, MutableList<Int>>(Pair(0, mutableListOf()))
                    var currentCol = 0
                    for (blockIndex in horizontalSorted) {
                        //skip first and add to first colum
                        if (blockIndex == horizontalSorted[0]) {
                            columns[currentCol]!!.add(blockIndex)
                            continue
                        }
                        //do not add the servings
                        if (blockIndex == servingsIndex) {
                            continue
                        }
                        if (textBlocks[blockIndex].cornerPoints != null) {
                            //if the corners are on a similar x value add to the current col else increase col number and add it to that
                            val thisX = textBlocks[blockIndex].cornerPoints!![0].x
                            val range = textBlocks[blockIndex].lineHeight!!
                            if (textBlocks[columns[currentCol]!!.last()].cornerPoints!![0].x in thisX - range..thisX + range) {
                                columns[currentCol]!!.add(blockIndex)
                            } else {
                                currentCol += 1
                                columns[currentCol] = mutableListOf(blockIndex)
                            }
                        }
                    }
                    //sort the elements in the colums by each of the heights
                    //reorder based on the vertical height of the text blocks
                    for (col in columns) {
                        col.value.sortBy {
                            textBlocks[it].cornerPoints?.get(0)?.y
                        }
                    }
                    //even though they are going to be horizontally in a colum there may be big spaces between and this needs to be separated out
                    // but there can still be large gaps so if its a bigger gap than one line see if there is a font size change
                    // or there could be a change in the gaps
                    val newColumns = mutableMapOf<Int, MutableList<Int>>()
                    val currentColSpacing =
                        mutableListOf<Int>()//the spacing between the current coll
                    currentCol = 0

                    for (colList in columns.values) { //loop though each col
                        for (index in colList) {
                            //if first in col continue and add to first
                            if (index == colList[0]) {
                                newColumns[currentCol] = mutableListOf(index)
                                continue
                            }
                            //if there is more than a line between 1 blocks start a new colum
                            //or large change in font size
                            //or there has been a consistent gap and there is a change in that
                            if (textBlocks[index].cornerPoints != null && textBlocks[newColumns[currentCol]!!.last()].cornerPoints != null) {
                                val scorePlusgap = equateIsColl(
                                    textBlocks,
                                    currentColSpacing,
                                    newColumns[currentCol]!!,
                                    index,
                                    newColumns[currentCol]!!.last()
                                )
                                if (scorePlusgap.first > 1f) {
                                    //make sure that the first item in the list still fits in the list if long enough
                                    if (scorePlusgap.third && newColumns[currentCol]!!.count() > 3) {//if avg coll
                                        //remove the first gap from the spaceing as that is what we are looking at
                                        currentColSpacing.removeAt(0)
                                        val firstScore = equateIsColl(
                                            textBlocks,
                                            currentColSpacing,
                                            newColumns[currentCol]!!,
                                            newColumns[currentCol]!![1],
                                            newColumns[currentCol]!![0]
                                        )
                                        //println("checking first :${firstScore.first}")
                                        //remove first if dose not fit
                                        if (firstScore.first > 1) {
                                            newColumns[currentCol]!!.removeAt(0)
                                        }
                                    }

                                    //start new col
                                    currentCol += 1
                                    newColumns[currentCol] = mutableListOf(index)
                                    currentColSpacing.removeAll(currentColSpacing)

                                    continue

                                }
                                //extend the colum if not got to continue state
                                newColumns[currentCol]?.add(index)
                                currentColSpacing.add(scorePlusgap.second)
                            }
                        }
                        if (newColumns[currentCol]!!.count() > 3) { //if long enough to check
                            //remove the first gap from the spaceing as that is what we are looking at
                            currentColSpacing.removeAt(0)
                            //check first item in col
                            val firstScore = equateIsColl(
                                textBlocks,
                                currentColSpacing,
                                newColumns[currentCol]!!,
                                newColumns[currentCol]!![1],
                                newColumns[currentCol]!![0]
                            )
                            //println("checking first :${firstScore.first}")
                            //remove first if dose not fit
                            if (firstScore.first > 1) {
                                newColumns[currentCol]!!.removeAt(0)
                            }
                        }
                        //start the new col
                        currentCol += 1
                        currentColSpacing.removeAll(currentColSpacing)
                    }
                    //println("new$newColumns")
                    //see if there are 2 columns in a horizontal row that have and that they have similar length elements and combine them as they probably contain the same thing
                    //loop though col and if there is a coll that the top index is next to it in the vertically sorted list and the both are at least 2 items long
                    for (col in newColumns) {
                        if (col.value.count() < 2) continue // don't look at short col
                        val colVerticalIndex = verticallySorted.indexOf(col.value[0])
                        for (compare in newColumns) {
                            if (col == compare) continue //if not looking at same col
                            if (compare.value.count() > 1) { //if compared is long enough
                                if (abs(colVerticalIndex - verticallySorted.indexOf(compare.value[0])) == 1) {//they are starting at the same level
                                    //check the lengths of items in both col and if they are similar continue to combine (so ingredients next to instructions should not get combined)
                                    val colAvgLenght =
                                        col.value.fold(0) { total, item -> total + textBlocks[item].text.length } / col.value.count()
                                    val compAvgLenght =
                                        compare.value.fold(0) { total, item -> total + textBlocks[item].text.length } / compare.value.count()
                                    //if dif if smaller than smallest avglength continue
                                    if (abs(colAvgLenght - compAvgLenght) < min(
                                            colAvgLenght,
                                            compAvgLenght
                                        )
                                    ) {
                                        //combine them
                                        //println("combining:${compare.key} to ${col.key} ")
                                        newColumns[col.key]?.addAll(compare.value) //add compared to the original col
                                        newColumns[compare.key] = mutableListOf()//empty other list
                                    }
                                }
                            }
                        }
                    }

                    var longest = mutableListOf<Int>()
                    var secondLongest = mutableListOf<Int>()
                    for (col in newColumns.values) {
                        //if contains title continue
                        if (col.contains(titleIndex)) {
                            continue
                        }
                        //if longer than longest replace and move longest to second
                        if (longest.count() < col.count()) {
                            secondLongest = longest
                            longest = col
                            continue
                        }
                        //if not longer than longest but longer than second replace that
                        if (secondLongest.count() < col.count()) {
                            secondLongest = col
                        }
                    }
                    //the ingredients should be shorter on average so that is how we can distinguish them
                    var longestTotal = 0
                    for (blockIndex in longest) {
                        longestTotal += textBlocks[blockIndex].text.length
                    }
                    var secondLongestTotal = 0
                    for (blockIndex in secondLongest) {
                        secondLongestTotal += textBlocks[blockIndex].text.length
                    }
                    //if can not find second longest there can not be found a good recipe so error
                    if (secondLongest.isEmpty()) {
                        error()
                        return@addOnSuccessListener
                    }
                    //if the longest is the ingredients
                    val ingredients =
                        if (longestTotal / longest.count() > secondLongestTotal / secondLongest.count()) {
                            secondLongest
                        } else {
                            longest
                        }
                    val instructions =
                        if (longestTotal / longest.count() > secondLongestTotal / secondLongest.count()) {
                            longest
                        } else {
                            secondLongest
                        }

                    getBox(
                        textBlocks.withIndex().filter { (index, _) -> ingredients.contains(index) }
                            .map { (_, block) -> block }, ScanBoxType.Ingredients
                    )?.let {
                        recipeBounds.addAll(listOf(it))
                    }
                    getBox(
                        textBlocks.withIndex().filter { (index, _) -> instructions.contains(index) }
                            .map { (_, block) -> block }, ScanBoxType.Instructions
                    )?.let {
                        recipeBounds.addAll(listOf(it))
                    }
                    val blocks = textBlocks.map { Block(findCenter(it), it.text, it.lines.map { line -> line.text }) }

                    callback(recipeBounds, blocks)
                }
                .addOnFailureListener { e ->
                    // Task failed with an exception
                    Log.d("can't load image", "$e")
                }
        }

        fun boundsToRecipe(
            bounds: RecipeBounds,
            blocks: List<Block>,
            settings: Map<String, String>,
        ): Recipe {
            val recipe = getEmptyRecipe()

            //find title
            blocks.firstOrNull { block -> blockInBox(bounds.title(), block) }?.let {
                recipe.data.name = it.text
            }
            // find servings
            blocks.firstOrNull { block -> blockInBox(bounds.servings(), block) }?.let {
                recipe.data.serves = it.text
            }

            // find ingredients and instructions and extra
            val ingredients = blocks.filter { block -> blockInBoxes(bounds.ingredients(), block) }
            val instructions = blocks.filter { block -> blockInBoxes(bounds.instructions(), block) }
            val extra = blocks.filter { block -> blockInBoxes(bounds.extra(), block) }

            //clean up the text then create the elements then add to recipe

            //sometimes ingredients are split into multiple elements or combined into one. try to fix this
            //if there is an ingredient that does not contain a number and is short combine it to the ingredient before it

            val ingredientTextList: MutableList<String> = mutableListOf()
            var currentIngredient = ""
            for ((listIndex, block) in (ingredients).withIndex()) {
                //look at lines and if one of the list starts with a number and is not the first line split of that line and start new
                val lines = block.lines

                for ((lineIndex, line) in lines.withIndex()) {
                    val text = cleanIngredient(line)

                    //val text = textBlocks[blockIndex].text
                    if (listIndex == 0 && lineIndex == 0) { //skip first
                        currentIngredient = text
                        continue

                    }
                    if ((text.length < (if (lineIndex == 0) 20 else 30) && !text[0].isDigit())) { //if its a short line and dose not start with number combine (if its a start of a  new block more likely to be new line
                        //combine to above ingredient
                        currentIngredient += " $text"
                    } else {
                        //end previce ingredient and start new
                        ingredientTextList.add(currentIngredient)
                        currentIngredient = text
                    }
                }
            }
            //add last bit to ingredient text
            ingredientTextList.add(currentIngredient)
            //ingredients
            val recipeIngredients = mutableListOf<Ingredient>()
            for ((listIndex, ingredientText) in (ingredientTextList).withIndex()) {
                recipeIngredients.add(
                    Ingredient(
                        listIndex,
                        cleanIngredient(ingredientText)
                    )
                )
            }
            recipe.ingredients = Ingredients(recipeIngredients)
            //instructions
            val recipeInstructions = mutableListOf<Instruction>()
            for ((listIndex, block) in (instructions).withIndex()) {
                recipeInstructions.add(
                    Instruction(
                        listIndex,
                        cleanInstruction(block.text),
                        null
                    )
                )
            }
            recipe.instructions = Instructions(recipeInstructions)

            //check settings to see if extra needs to be done
            when (settings["Creation.Image Loading.Split instructions"]) {
                "intelligent" -> recipe.instructions =
                    CreateAutomations.Companion.autoSplitInstructions(
                        recipe.instructions,
                        CreateAutomations.Companion.InstructionSplitStrength.Intelligent
                    )

                "sentences" -> recipe.instructions =
                    CreateAutomations.Companion.autoSplitInstructions(
                        recipe.instructions,
                        CreateAutomations.Companion.InstructionSplitStrength.Sentences
                    )

                else -> {}
            }

            if (settings["Creation.Image Loading.Generate cooking steps"] == "true") {
                val stepsAndLinks =
                    CreateAutomations.Companion.autoGenerateStepsFromInstructions(
                        recipe.instructions,
                        settings
                    )
                recipe.data.cookingSteps = CookingSteps(stepsAndLinks.first.toMutableList())
                recipe.instructions = stepsAndLinks.second
            }
            //add extra to notes
            recipe.data.notes = extra.joinToString(" ") { it.text }


            return recipe
        }


        private fun capitalize(str: String): String {
            return str.trim().split("\\s+".toRegex())
                .joinToString(" ") { it.capitalize() }
        }

        private fun cleanTitle(title: String): String {
            var newTitle = cleanText(title)
            //correct common miss spelling
            newTitle.replace(" mare", " make")
            //correct the case
            newTitle = capitalize(title.lowercase())

            return newTitle
        }

        private fun cleanIngredient(ingredient: String): String {
            //clean stuff up that is only needed for ingredients
            var newIngredient = cleanText(ingredient)

            return newIngredient
        }

        private fun cleanInstruction(instruction: String): String {
            //clean stuff up that is only needed for instructions
            var newInstruction = cleanText(instruction)
            //remove numbering from the start (find the index there is a letter and remove before that
            val firstLetter = newInstruction.indexOfFirst { char -> char.isLetter() }
            if (firstLetter > 0) {//if there is nothing before it
                newInstruction = newInstruction.substring(firstLetter)
            }

            return newInstruction
        }

        private fun FixNumber(text: String, oldChar: Char, newChar: Char): String {
            var newText = text
            if (newText.startsWith("$oldChar ")) {//if starting with replace replace it
                newText = newText.replaceFirst(oldChar, newChar)
            }
            if (newText.endsWith("$oldChar ")) {//if ending with replace replace it
                newText = newText.dropLast(1)
                newText += newChar
            }
            newText = newText.replace(" $oldChar ", " $newChar ")
            return newText
        }

        private fun cleanText(text: String): String {
            //clean up general things in text
            var newText = text
            //fix numbers (often number is taken as a letter on its own)
            newText = FixNumber(newText, 'l', '1')
            newText = FixNumber(newText, '|', '1')
            newText = FixNumber(newText, 's', '5')
            newText = FixNumber(newText, 'G', '6')

            //fix other elements
            newText = newText.replace("|", "/")

            return newText
        }

        private fun equateIsColl(
            textBlocks: List<TextBlock>,
            colSpaceList: List<Int>,
            currentNewCol: List<Int>,
            index: Int,
            index2: Int
        ): Triple<Float, Int, Boolean> {
            val gap = textBlocks[index].cornerPoints!![0].y - textBlocks[index2].cornerPoints!![3].y
            val minLineHeight = min(textBlocks[index].lineHeight!!, textBlocks[index2].lineHeight!!)
            val lineHeightAvg =
                (textBlocks[index].lineHeight!! + textBlocks[index2].lineHeight!!) / 2
            val lineHeightDiff =
                abs(textBlocks[index].lineHeight!! - textBlocks[index2].lineHeight!!)
            val avgGap = if (colSpaceList.isNotEmpty()) {
                colSpaceList.fold(0) { total, item -> total + item } / colSpaceList.count()//if there has been a consistent gap and then gap is bigger start new
            } else {
                -1
            }
            val isAvg = if (colSpaceList.count() > 1) {
                abs(avgGap - colSpaceList[0]) < minLineHeight / 2
            } else {
                false
            }
            val avgStrength =
                if (isAvg) { // how much the avage spaceing stats shuold be trusted as the less elements the more inacurate it is
                    when (colSpaceList.count()) {
                        2 -> 0.63f
                        3 -> 0.75f
                        4 -> 0.9f

                        else -> 1f
                    }


                } else {
                    -1f
                }

            //add to score for each criticra so if one is very large or 2 are like kinda it will will split it
            var score = 0f
            //larger gap
            score += (gap / minLineHeight) / 3f //3 line gap = 1
            // large font change
            score += lineHeightDiff / lineHeightAvg.toFloat() //double size = 1
            //average gap change
            if (isAvg) {
                score += (abs(gap - avgGap) / minLineHeight.toFloat()) * avgStrength * 1.5f// 66% change  = 1
            }
            //println("index:$index: gap:$gap, minLineHeight:$minLineHeight, avg:$lineHeightAvg, lineHDIff:$lineHeightDiff, avgGap:$avgGap, isAvg:$isAvg, space:$colSpaceList, score:$score")
            return Triple(score, gap, isAvg)
        }
    }
}


