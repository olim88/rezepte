package com.example.rezepte

import kotlin.math.abs
import kotlin.math.roundToInt

class MakeFormatting {
    companion object{
        private val Float.vulgarFraction: Pair<String, Float>
            get() {
                val whole = toInt()
                val sign = if (whole < 0) -1 else 1
                val fraction = this - whole

                for (i in 1 until fractions.size) {
                    if (abs(fraction) > (fractionValues[i] + fractionValues[i - 1]) / 2) {
                        return if (fractionValues[i - 1] == 1.0) {
                            "${whole + sign}" to (whole + sign).toFloat()
                        } else if (whole != 0) {
                            "$whole${fractions[i - 1]}" to whole + sign * fractionValues[i - 1].toFloat()
                        }else {
                            fractions[i - 1] to  sign * fractionValues[i - 1].toFloat()
                        }

                    }
                }
                return "$whole" to whole.toFloat()
            }
        private val String.vulgarFraction : Float
            get() {
                //split
                val number = Regex("-?[0-9]+(/|\\d*\\.)?[0-9]*").find(this)
                val fraction = if (number == null){
                    this
                }else{
                    this.replace(number.value,"")
                }
                //convert fraction to number
                val fractionalValue = if (fraction != ""){
                    fractionValues[fractions.indexOf(fraction)]
                }else {
                    null
                }
                var output = 0f
                if (number != null ) {
                    output += if (number.value.contains("/")){//if it is a fractional value work that out
                        val numbers = number.value.split("/")
                        if (numbers[1] != ""){
                            numbers[0].toFloat()/numbers[1].toFloat()
                        }else {
                            numbers[0].toFloat()
                        }

                    }else {
                        number.value.toFloatOrNull() ?: 0f //convert to float but if its somehow invalid return 0

                    }
                }
                if (fractionalValue != null){
                    output += fractionalValue.toFloat()
                }
                return output
            }

        private val String.containedNumbers : List<String>
            //find all numbers in a string with regex
            get() {
                return  numberRegex.findAll(this)
                    .map(MatchResult::value)
                    .toList()
            }
        private fun String.replace(replacements: List<String>, replacement : String): String {
            var result = this
            replacements.forEach { oldWord -> result = result.replaceFirst(oldWord, replacement ) } //todo could replace part of a word instead of the unit i think
            return result
        }
        private fun ignoreCaseOpt(ignoreCase: Boolean) =
            if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()

        private fun String?.indexesOf(pat: String, ignoreCase: Boolean = true): List<Int> = //returns every index a value is found
            pat.toRegex(ignoreCaseOpt(ignoreCase))
                .findAll(this?: "")
                .map { it.range.first }
                .toList()
        private fun String.replaceNumberBeforeValues(
            oldNumber: String,
            newNumber: String,
            values: List<String>
        ): String {
            //get all index of both bits of information
            val valuesIndexes = mutableListOf<Int>()
            for(value in values){ //add the index of the whole list of posible values
                valuesIndexes.addAll(this.indexesOf(value, true))
            }
            val oldNumberIndexes = this.indexesOf(oldNumber, true)
            //if one can not be found return now
            if (valuesIndexes.isEmpty() || oldNumberIndexes.isEmpty()) return this
            //chose the number closest to and before and index of the value
            var bestNumberIndex = -1
            var bestDistance = -1 //smallest distance found
            for (valueIndex in valuesIndexes) {
                for (oldNumberIndex in oldNumberIndexes) {
                    if (oldNumberIndex > valueIndex) break // if to big break and look at next one
                    //else if distance if better than best replace distance and the best value
                    if (valueIndex - oldNumberIndex < bestDistance || bestDistance == -1) {
                        bestDistance = valueIndex - oldNumberIndex
                        bestNumberIndex = oldNumberIndex
                    }
                }
            }
            if (bestDistance == -1) return this //could not find sutible pair the value is before the old number
            //replace old number with new number
            return this.replaceRange(bestNumberIndex,bestNumberIndex+oldNumber.length,newNumber)
        }


        private val fractions = arrayOf(
            "",                           // 16/16
            "\u00B9\u2075/\u2081\u2086",  // 15/16
            "\u215E",                     // 7/8
            "\u00B9\u00B3/\u2081\u2086",  // 13/16
            "\u00BE",                     // 3/4
            "\u00B9\u00B9/\u2081\u2086",  // 11/16
            "\u215D",                     // 5/8
            "\u2079/\u2081\u2086",        // 9/16
            "\u00BD",                     // 1/2
            "\u2077/\u2081\u2086",        // 7/16
            "\u215C",                     // 3/8
            "\u2075/\u2081\u2086",        // 5/16
            "\u00BC",                     // 1/4
            "\u00B3/\u2081\u2086",        // 3/16
            "\u215B",                     // 1/8
            "\u00B9/\u2081\u2086",        // 1/16
            ""                            // 0/16
        )
        private val numberRegex = Regex("([0-9]+(/|\\d*\\.)?[0-9]*(((${fractions.slice(1..fractions.count()-2).joinToString(")|(")}))?))|(((${fractions.slice(1..fractions.count()-2).joinToString(")|(")})))") //the regex to show that it is a number

        private val fractionValues = arrayOf(
            1.0,
            15.0 / 16, 7.0 / 8, 13.0 / 16, 3.0 / 4, 11.0 / 16,
            5.0 / 8, 9.0 / 16, 1.0 / 2, 7.0 / 16, 3.0 / 8,
            5.0 / 16, 1.0 / 4, 3.0 / 16, 1.0 / 8, 1.0 / 16,
            0.0
        )
        private fun combineNumbersWithThereFractions(string:String): String{
            //combines split number and fraction pairs
            var output = string
            //clean up and split words from numbers
            val cleanWords= removeUnnecessarySlash(slitTextFromNumbers(getWordsWithNumbers(string)))
            for ((index,word) in cleanWords.withIndex()){
                if (index == 0 ) continue //skip first as could not be valid
                if (word.matches("((${fractions.slice(1..fractions.count()-2).joinToString(")|(")}))|([0-9]+/[0-9]+)".toRegex())){//if is fractional number
                    //if word before is a whole number (going from right ot left now)
                    if (cleanWords[index-1].matches("[0-9]+".toRegex())){
                        //get the combined value of both numbers
                        val combined = word.vulgarFraction + cleanWords[index-1].vulgarFraction
                        //if they exist in the string with just a space between combine the values of the numbers
                        output =output.replace("${cleanWords[index-1]} $word","$combined")
                    }
                }
            }
            return  output
        }
        fun getCorrectUnitsAndValuesInIngredients(string :String, multiplier: Float, settings : Map<String,String>) : String {
            //make sure that the numbers all have the correct unit
            var output = convertUnitOfString(string,settings)
            //remove a singular space between a fraction and a number as it is not necessary and is the best way to make sure they are combined
            output = combineNumbersWithThereFractions(output)
            //replace numbers with multiplied value
            return multiplyByOnlyValues(output,multiplier,settings["Units.Fractional Numbers"]== "true",if (settings["Units.Round Numbers"] == "true") 0.02f else -1f)
        }

        fun getCorrectUnitsAndValues(string :String, multiplier: Float, settings : Map<String,String>) : String {

            //make sure that the numbers all have the correct unit
            var output = convertUnitOfString(string,settings)
            //remove a singular space between a fraction and a number as it is not necessary and is the best way to make sure they are combined
            output = combineNumbersWithThereFractions(output)
            //replace numbers with multiplied value
            return multiplyBy(output,multiplier,settings["Units.Fractional Numbers"]== "true",if (settings["Units.Round Numbers"] == "true") 0.02f else -1f)
        }
        fun listUnitsInValue(string: String): List<String>{
            //clean the string from brackets and other things in the way
            val cleanWords= removeUnnecessarySlash(slitTextFromNumbers(getWordsWithNumbers(string)))
            //find the existing units in the string
            var unitType : CookingUnit? = null
            val unitIndexes: MutableMap<CookingUnit,Int> = mutableMapOf()
            for (option in unitsLut.entries){
                val unitIndex = cleanWords.indexOf(option.value)
                if (unitIndex != -1){
                    unitIndexes[option.key]  = unitIndex
                    unitType = option.key

                }
            }
            //find units with the values (measurements)
            val measurements = mutableListOf<String>()
            if (unitType == null) return measurements // can not find units so return empty list

            //check all the units found
            for (unit in unitIndexes){
                //if there units starting an item just ignore them
                if (unit.value == 0) continue
                val value = cleanWords[unit.value-1]
                //make sure that there is a number before the unit then combine them and add it to measurements
                if (value.matches(numberRegex)) {
                    measurements.add("$value ${cleanWords[unit.value]}")
                }


            }

            return measurements
        }
        fun getConversions(measurementToConvert: String, wholeIngredient: String, settings: Map<String,String>) : List<String>{
            val possibleConversions = mutableListOf<String>()
            //find what type of unit it is
            val splitMeasurement= removeUnnecessarySlash(slitTextFromNumbers(getWordsWithNumbers(measurementToConvert)))
            var unitType : CookingUnit? = null
            for (option in unitsLut.entries){
                val unitIndex = splitMeasurement.indexOf(option.value)
                if (unitIndex != -1){
                    unitType = option.key

                }
            }
            //based on what type of unit it is convert to other different units and and add to output list
            val converted = when (unitType){
                CookingUnit.Teaspoon -> {
                    val ml = splitMeasurement[0].vulgarFraction * teaSpoonVolume(settings)
                    getVolumeConversions(ml,wholeIngredient,settings["Units.metric Volume"] == "true",settings)

                }
                CookingUnit.Tablespoon -> {
                    val ml = splitMeasurement[0].vulgarFraction * tableSpoonVolume(settings)
                    getVolumeConversions(ml,wholeIngredient,settings["Units.metric Volume"] == "true",settings)

                }
                CookingUnit.Cup -> {
                    val ml = splitMeasurement[0].vulgarFraction * cupVolume(settings)
                    getVolumeConversions(ml,wholeIngredient,settings["Units.metric Volume"] == "true",settings)
                }
                CookingUnit.Millilitres -> {
                    val ml = splitMeasurement[0].vulgarFraction
                    getVolumeConversions(ml,wholeIngredient,settings["Units.metric Volume"] == "true",settings)
                }
                CookingUnit.Litres -> {
                    val ml = splitMeasurement[0].vulgarFraction * 1000
                    getVolumeConversions(ml,wholeIngredient,settings["Units.metric Volume"] == "true",settings)
                }
                CookingUnit.Pint -> {
                    val ml = splitMeasurement[0].vulgarFraction * pintVolume(settings)
                    getVolumeConversions(ml,wholeIngredient,settings["Units.metric Volume"] == "true",settings)
                }
                CookingUnit.FluidOunce -> {
                    val ml = splitMeasurement[0].vulgarFraction  * fluidOunceVolume(settings)
                    getVolumeConversions(ml,wholeIngredient,settings["Units.metric Volume"] == "true",settings)
                }
                CookingUnit.Grams -> {
                    val grams = splitMeasurement[0].vulgarFraction
                    getWeightConversions(grams,wholeIngredient,settings)
                }
                CookingUnit.KiloGrams -> {
                    val grams = splitMeasurement[0].vulgarFraction * 1000
                    getWeightConversions(grams ,wholeIngredient,settings)
                }
                CookingUnit.Ounce -> {
                    val grams = splitMeasurement[0].vulgarFraction * 0.03527396f
                    getWeightConversions(grams ,wholeIngredient,settings)
                }
                CookingUnit.Pound -> {
                    val grams = splitMeasurement[0].vulgarFraction * 0.4535924f
                    getWeightConversions(grams ,wholeIngredient,settings)
                }


                else -> return listOf() //no unit return empty list
            }
            // for the items in the converted list add them to the possible conversions output with unit
            for (value in converted){
                if (value.key != unitType){
                    val roundingPercent = if (settings["Units.Round Numbers"] == "true"){
                        0.02f
                    }else { -1f}
                    val vulgarValue = roundSmallGaps( value.value,roundingPercent ).vulgarFraction

                    possibleConversions.add("${if (settings["Units.Fractional Numbers"]== "true") vulgarValue.first else value.value}${unitsLut[value.key]?.get(0)}")
                }

            }


            //return the converted list
            return  possibleConversions
        }
        private fun getVolumeConversions(ml: Float, whole:String, metricWeight: Boolean, settings: Map<String, String>) : Map<CookingUnit,Float>{
            //return all the conversions for a volume mesure used for tsp tbsp ...
            val output = mutableMapOf<CookingUnit,Float>()
            //weight
            val weightMulti = getWeightConversions(whole)
            if (weightMulti != -1f){
                if(metricWeight){
                    output[CookingUnit.Grams] = ml * weightMulti
                }else{
                    output[CookingUnit.Ounce] = ml * weightMulti * 0.03527396f
                }
            }
            if (ml < 1000){
                //ml
                output[CookingUnit.Millilitres] = ml
            }else {
                //litre
                output[CookingUnit.Litres] = ml /1000
            }
            if (ml < tableSpoonVolume(settings) )
            {
                //tsp
                output[CookingUnit.Teaspoon] = ml / teaSpoonVolume(settings)
            }else if  ( ml < cupVolume(settings)/4){
                //tbsp
                output[CookingUnit.Tablespoon] = ml / tableSpoonVolume(settings)
            }else  {
                //cups
                output[CookingUnit.Cup] = ml / cupVolume(settings)
            }
            //pint ( show if more than 1/4 )
            if (ml > 142.0653f){
                output[CookingUnit.Pint] = ml * 0.001759754f
            }
            //floz
            output[CookingUnit.FluidOunce] = ml * 1/fluidOunceVolume(settings)

            return output
        }
        private  fun getWeightConversions(weight: Float, whole:String, settings: Map<String, String>) : Map<CookingUnit,Float>{
            //return all the conversions for a weight measure used for grams kg ...
            val output = mutableMapOf<CookingUnit,Float>()

            //ml
            val weightMulti = getWeightConversions(whole)
            if (weightMulti != -1f){
                val ml = weight / weightMulti
                output[CookingUnit.Millilitres] = ml
                //most sensible cups tsp or tbsp
                if (ml < tableSpoonVolume(settings)){
                    //convert to teaspoon
                    output[CookingUnit.Teaspoon] = ml / teaSpoonVolume(settings)
                }else if (ml < cupVolume(settings)/4){
                    //convert to table spoon
                    output[CookingUnit.Tablespoon] = ml / tableSpoonVolume(settings)
                }else {
                    //convert to cups
                    output[CookingUnit.Cup] = ml / cupVolume(settings)
                }
            }
            if (weight < 1000){
                //grams
                output[CookingUnit.Grams] = weight
            } else {
                //kg
                output[CookingUnit.KiloGrams] = weight /1000f
            }
            //oz
            output[CookingUnit.Ounce] = weight * 0.03527396f
            //pound ( show pounds if more than half pound
            if (weight > 226.7962){
                output[CookingUnit.Pound] = weight * 0.002204623f
            }


            return  output
        }

        private enum class CookingUnit {
            Teaspoon,
            Tablespoon,
            Cup,
            Millilitres,
            Litres,
            FluidOunce,
            Pint,
            Grams,
            KiloGrams,
            Ounce,
            Pound,
            Centimeter,
            Meter,
            ImperialLength,
            TeperatureC,
            TeperatureF,

        }
        private val unitsLut = mapOf(
            Pair(CookingUnit.Teaspoon , listOf("tsp","tspn","teaspoons","teaspoon","tea spoon") ),
            Pair(CookingUnit.Tablespoon , listOf("tbsp","tbspn","tablespoons","tablespoon","table spoon") ),
            Pair(CookingUnit.Cup , listOf("cups","cup") ),
            Pair(CookingUnit.Millilitres , listOf("ml","millilitres") ),
            Pair(CookingUnit.Litres , listOf("litres","litre","l") ),
            Pair(CookingUnit.FluidOunce , listOf("fl oz","fl","floz") ),
            Pair(CookingUnit.Pint , listOf("pt","pints","pint") ),
            Pair(CookingUnit.Grams , listOf("grams","g") ),
            Pair(CookingUnit.KiloGrams , listOf("kg","kilograms") ),
            Pair(CookingUnit.Ounce , listOf("oz","ounce") ),
            Pair(CookingUnit.Pound , listOf("lb","pound") ),
            Pair(CookingUnit.Centimeter , listOf("cm","centimeter") ),
            Pair(CookingUnit.Meter , listOf("m","meter") ),
            Pair(CookingUnit.ImperialLength , listOf("in","inch") ),

            )

        private fun multiplyBy (wholeString: String, multiplier: Float, isVulgar: Boolean, roundPercentage : Float): String{
            var output = wholeString
            for (number in wholeString.containedNumbers){
                //if there is an "x" in the string before the number this signifies the number is being multiplied by another value and to avoid having both multiplied do not multiply a number after a "x"
                var value = if(wholeString.indexOf(" x ") != -1 && wholeString.indexOf(" x ") < wholeString.indexOf(number)) {
                    number.vulgarFraction
                }else {
                    number.vulgarFraction * multiplier
                }

                value = roundSmallGaps(value, roundPercentage)

                output = if (isVulgar){
                    output.replace(number,value.vulgarFraction.first)
                }else {
                    output.replace(number, (value.vulgarFraction.second).toString().replace(".0 ", " "))
                }
            }
            return output
        }

        /**
         * multiplies the numbers in a string only if they have a measurement after them so it can work inside instructions
         *
         *
         * @param wholeString The string to multiple the numbers in.
         * @param multiplier The number to multiple them by.
         * @param isVulgar How to format the numbers when finished.
         * @param roundPercentage what difference from a whole number to start rounding the number.
         */
        private fun multiplyByOnlyValues (wholeString: String, multiplier: Float, isVulgar: Boolean, roundPercentage : Float): String{
            //clean the string from brackets and other things in the way
            val cleanWords= removeUnnecessarySlash(slitTextFromNumbers(getWordsWithNumbers(wholeString)))
            val units= getUnitsInString(wholeString)
            var output = wholeString

            for (unit in units){

                //if there units starting an item just ignore them
                if (unit.value == 0) continue
                val value = cleanWords[unit.value-1]
                //make sure that there is a number being passed
                if (value.matches(numberRegex)) {
                    val newNumber = roundSmallGaps(value.vulgarFraction * multiplier, roundPercentage).vulgarFraction
                    val replacement : String=   if (isVulgar) newNumber.first else newNumber.second.toString()
                    output = output.replaceNumberBeforeValues(value,replacement, unitsLut[unit.key]!!)


                }
            }

            return output
        }
        private fun roundSmallGaps (value: Float, roundPercentage: Float ) : Float{
            if (roundPercentage> 0 && abs(value - value.roundToInt()) /value < roundPercentage){//if enabled e.g. bigger than 0 and then if the rounding to a whole number effects it less than the percentage round it
                return value.roundToInt().toFloat()
            }

            return value
        }

        private fun getWordsWithNumbers(text: String) : List<String> { return text.lowercase().split("([\\s,;?()-]+)".toRegex())} //split at chars that signify a change in word.

        private fun slitTextFromNumbers(startingWords:List<String>) : List<String>{
            val outputWords = mutableListOf<String>()
            for (word in startingWords){
                //if the word is a mix of text and numbers
                if (!word.matches(numberRegex) && word.containedNumbers.isNotEmpty()){
                    val text = word.split(numberRegex)
                    val numbers = word.containedNumbers
                    //output to new words in correct order
                    for (i in 0..numbers.size-1){
                        outputWords.add(text[i])
                        outputWords.add(numbers[i])
                    }
                    outputWords.add(text.last())
                }else {//it dose not contain numbers or is just a number so does not need to be split
                    outputWords.add(word)
                }
            }
            return  outputWords
        }
        private fun removeUnnecessarySlash(startingWords: List<String>) : List<String>{ // if word is letters with a "/" remove the slash as it is not a number
            val outputWords = mutableListOf<String>()
            for (word in startingWords){
                if (word.matches("([a-zA-Z]+/)|(/[a-zA-Z]+)|([a-zA-Z]+/[a-zA-Z]+)".toRegex())){ //if the word is matching the check remove the / as not needed
                    outputWords.add(word.replace("/",""))
                }else{//add the words not matching
                    outputWords.add(word)
                }
            }
            return  outputWords
        }
        private fun getUnitsInString(string:String) : MutableMap<CookingUnit, Int> {
            //clean the string from brackets and other things in the way
            val cleanWords= removeUnnecessarySlash(slitTextFromNumbers(getWordsWithNumbers(string)))
            //find the existing units in the string
            val unitIndexes: MutableMap<CookingUnit,Int> = mutableMapOf()
            for (option in unitsLut.entries){
                val unitIndex = cleanWords.indexOf(option.value)
                if (unitIndex != -1){
                    unitIndexes[option.key]  = unitIndex
                }
            }
            return unitIndexes
        }

        private fun convertUnitOfString(string: String, settings: Map<String,String>) : String{
            //clean the string from brackets and other things in the way
            val cleanWords= removeUnnecessarySlash(slitTextFromNumbers(getWordsWithNumbers(string)))
            //find the existing units in the string
            val unitIndexes = getUnitsInString(string)
            if (unitIndexes.isEmpty()) return string // can not find units so not converting them
            var output = string
            //check all the units found
            for (unit in unitIndexes){
                //if there units starting an item just ignore them
                if (unit.value == 0) continue
                val value = cleanWords[unit.value-1]
                //make sure that there is a number being passed
                if (value.matches(numberRegex)) {
                    output = fixUnits(output, value, unit.key, settings)

                }


            }
            return output
        }
        private fun teaSpoonVolume(settings: Map<String, String>):Float{
            val setting = settings["Units.Conversions.Teaspoon volume"] ?: return 0f

            val output = when{
                setting.startsWith("metric") -> 5f
                setting.startsWith("us") -> 4.9289217f
                setting.startsWith("uk") -> 5.919388f

                else -> 0f //if setting is corrupted or thing
            }
            return output
        }
        private fun tableSpoonVolume(settings: Map<String, String>):Float{
            val setting = settings["Units.Conversions.Tablespoon volume"] ?: return 0f

            val output = when{
                setting.startsWith("metric") -> 15f
                setting.startsWith("us") -> 14.786765f
                setting.startsWith("uk") -> 14.20654f

                else -> 0f //if setting is corrupted or thing
            }
            return output
        }
        private fun cupVolume(settings: Map<String, String>):Float{
            val setting = settings["Units.Conversions.Cup volume"] ?: return 0f

            val output = when{
                setting.startsWith("metric") -> 250f
                setting.startsWith("us") -> 236.59f
                setting.startsWith("uk") -> 284.1306f

                else -> 0f //if setting is corrupted or thing
            }
            return output
        }
        private fun fluidOunceVolume(settings: Map<String, String>):Float{
            val setting = settings["Units.Conversions.Fl oz volume"] ?: return 0f

            val output = when{
                setting.startsWith("imperial") -> 28.4131f
                setting.startsWith("us") -> 29.5735f

                else -> 0f //if setting is corrupted or thing
            }
            return output
        }
        private fun pintVolume(settings: Map<String, String>):Float{
            val setting = settings["Units.Conversions.Pint volume"] ?: return 0f

            val output = when{
                setting.startsWith("imperial") -> 568.2612f
                setting.startsWith("us") -> 473.17648f

                else -> 0f //if setting is corrupted or thing
            }
            return output
        }

        private fun fixUnits(wholeString: String, startingValue: String, cookingUnitType: CookingUnit, settings: Map<String, String>): String{
            //make sure unit is correct for the users settings
            var output = wholeString
            when ( cookingUnitType){
                CookingUnit.Teaspoon -> {
                    if (settings["Units.Tea Spoons"] == "true") return wholeString // unit is correct
                    //convert to volume
                    output = output.replaceNumberBeforeValues(startingValue,(startingValue.vulgarFraction*teaSpoonVolume(settings)).toString(),unitsLut[CookingUnit.Teaspoon]!!)

                    //replace units
                    unitsLut[CookingUnit.Teaspoon]?.let { output = output.replace (it, "ml") }
                    //see if it can be turned into grams
                    output = mlToWeight((startingValue.vulgarFraction* teaSpoonVolume(settings)).toString(),output,settings["Units.metric Weight"] == "true" )
                    return  output
                }
                CookingUnit.Tablespoon -> {
                    if (settings["Units.Table Spoons"] == "true") return wholeString // unit is correct
                    //convert to volume
                    output = output.replaceNumberBeforeValues(startingValue,(startingValue.vulgarFraction* tableSpoonVolume(settings)).toString(),unitsLut[CookingUnit.Tablespoon]!!)
                    //replace units
                    unitsLut[CookingUnit.Tablespoon]?.let { output =  output.replace (it, "ml") }
                    //see if it can be turned into grams
                    output = mlToWeight((startingValue.vulgarFraction*tableSpoonVolume(settings)).toString(),output,settings["Units.metric Weight"] == "true" )
                    return  output
                }
                CookingUnit.Cup -> {
                    if (settings["Units.Cups"] == "true") return wholeString // unit is correct
                    //convert to volume
                    output = output.replaceNumberBeforeValues(startingValue,(startingValue.vulgarFraction* cupVolume(settings)).toString(),unitsLut[CookingUnit.Cup]!!)
                    //replace units
                    unitsLut[CookingUnit.Cup]?.let { output =  output.replace (it, "ml") }
                    //see if it can be turned into grams
                    output = mlToWeight((startingValue.vulgarFraction*cupVolume(settings)).toString(),output,settings["Units.metric Weight"] == "true" )
                    return  output
                }
                CookingUnit.Millilitres -> {
                    //if could be converted to spoons or cups and that settings is enabled convert it and the settings to do this is enabled
                    if (settings["Units.Convert To Spoons/Cups"] == "true"  && settings["Units.Tea Spoons"] == "true" && startingValue.vulgarFraction < tableSpoonVolume(settings)) {
                        //convert to tea spoons and replace ml
                        output = output.replaceNumberBeforeValues(
                            startingValue,
                            (startingValue.vulgarFraction * (1 / teaSpoonVolume(settings))).toString(),
                            unitsLut[CookingUnit.Millilitres]!!
                        )
                        //replace unit
                        unitsLut[CookingUnit.Millilitres]?.let {
                            output = output.replace(it, "teaspoons")
                        }
                        return output
                    } else if (settings["Units.Convert To Spoons/Cups"] == "true"  && settings["Units.Table Spoons"] == "true" && startingValue.vulgarFraction < cupVolume(settings)* 0.25f) {
                        //convert to table spoons and replace ml
                        output = output.replaceNumberBeforeValues(
                            startingValue,
                            (startingValue.vulgarFraction * (1 / tableSpoonVolume(settings))).toString(),
                            unitsLut[CookingUnit.Millilitres]!!
                        )
                        //replace unit
                        unitsLut[CookingUnit.Millilitres]?.let {
                            output = output.replace(it, "tablespoons")
                        }
                        return output
                    } else if (settings["Units.Convert To Spoons/Cups"] == "true"  && settings["Units.Cups"] == "true") {
                        //convert to cup  and replace ml
                        output = output.replaceNumberBeforeValues(
                            startingValue,
                            (startingValue.vulgarFraction * (1 / cupVolume(settings))).toString(),
                            unitsLut[CookingUnit.Millilitres]!!
                        )
                        //replace unit
                        unitsLut[CookingUnit.Millilitres]?.let {
                            output = output.replace(it, "cups")
                        }
                        return output

                    }
                    //if not using metric convert it to  fl oz
                    else if(settings["Units.metric Volume"] == "false" ){
                        //convert to fl oz
                        output = output.replaceNumberBeforeValues(startingValue,(startingValue.vulgarFraction* 1/fluidOunceVolume(settings)).toString(),unitsLut[CookingUnit.Millilitres]!!)

                        //replace unit
                        unitsLut[CookingUnit.Millilitres]?.let { output =  output.replace (it, "fl oz") }
                        return output
                    }

                    //if already correct
                    return wholeString
                }
                CookingUnit.Litres -> {
                    //if not using metric convert ot pint
                    if(settings["Units.metric Volume"] == "false" ){
                        //convert to pint
                        output = output.replaceNumberBeforeValues(startingValue,(startingValue.vulgarFraction* pintVolume(settings)).toString(),unitsLut[CookingUnit.Litres]!!)

                        //replace unit
                        unitsLut[CookingUnit.Litres]?.let { output =  output.replace (it, "pint") }
                        return output
                    }
                    //if already correct
                    return wholeString
                }
                CookingUnit.FluidOunce -> {
                    //if could be converted to spoons or cups and that settings is enabled convert it and the settings to do this is enabled

                    if (settings["Units.Convert To Spoons/Cups"] == "true"  && settings["Units.Tea Spoons"] == "true" && startingValue.vulgarFraction* fluidOunceVolume(settings)<  tableSpoonVolume(settings)) { //if smaller than a table spoon use tsp
                        //convert to tsp
                        output = output.replaceNumberBeforeValues(
                            startingValue,
                            (startingValue.vulgarFraction * fluidOunceVolume(settings) * (1 / teaSpoonVolume(settings))).toString(),
                            unitsLut[CookingUnit.FluidOunce]!!
                        )
                        //replace unit
                        unitsLut[CookingUnit.FluidOunce]?.let {
                            output = output.replace(it, "tablespoons")
                        }
                    }else if (settings["Units.Convert To Spoons/Cups"] == "true"  && settings["Units.Table Spoons"] == "true" && startingValue.vulgarFraction* fluidOunceVolume(settings) < cupVolume(settings) * 0.25f) {//if smaller than a 1/4 cup use table spoon
                        //convert to tbsp
                        output = output.replaceNumberBeforeValues(
                            startingValue,
                            (startingValue.vulgarFraction * fluidOunceVolume(settings) * (1 / tableSpoonVolume(settings))).toString(),
                            unitsLut[CookingUnit.FluidOunce]!!
                        )
                        //replace unit
                        unitsLut[CookingUnit.FluidOunce]?.let {
                            output = output.replace(it, "tablespoons")
                        }
                    }else if (settings["Units.Convert To Spoons/Cups"] == "true"  && settings["Units.Cups"] == "true") {
                        //convert to cups
                        output = output.replaceNumberBeforeValues(
                            startingValue,
                            (startingValue.vulgarFraction * fluidOunceVolume(settings) * (1 / cupVolume(settings))).toString(),
                            unitsLut[CookingUnit.FluidOunce]!!
                        )
                        //replace unit
                        unitsLut[CookingUnit.FluidOunce]?.let {
                            output = output.replace(it, "tablespoons")
                        }

                    }
                    else if(settings["Units.metric Volume"] == "true" ){
                        //convert to ml
                        output = output.replaceNumberBeforeValues(startingValue,(startingValue.vulgarFraction* fluidOunceVolume(settings)).toString(),unitsLut[CookingUnit.FluidOunce]!!)

                        //replace unit
                        unitsLut[CookingUnit.FluidOunce]?.let { output =  output.replace (it, "ml") }
                        return output
                    }
                    //if already correct
                    return wholeString
                }
                CookingUnit.Pint -> {
                    if(settings["Units.metric Volume"] == "true" ){
                        if (startingValue.vulgarFraction* pintVolume(settings)/1000 > 1){ //if more than 1 litre convert to litres
                            //convert to l
                            output = output.replaceNumberBeforeValues(startingValue,(startingValue.vulgarFraction* pintVolume(settings)/1000).toString(),unitsLut[CookingUnit.Pint]!!)

                            //replace unit
                            unitsLut[CookingUnit.Pint]?.let { output =  output.replace (it, "l") }
                        }else {
                            //convert to ml
                            output = output.replaceNumberBeforeValues(startingValue,(startingValue.vulgarFraction* pintVolume(settings)).toString(),unitsLut[CookingUnit.Pint]!!)

                            //replace unit
                            unitsLut[CookingUnit.Pint]?.let { output =  output.replace (it, "ml") }
                        }
                        
                        return output
                    }
                    //if already correct
                    return wholeString
                }
                CookingUnit.Grams -> {
                    if(settings["Units.metric Weight"] == "false" ){
                        //convert to oz
                        output = output.replaceNumberBeforeValues(startingValue,(startingValue.vulgarFraction* 0.03527396f).toString(),unitsLut[CookingUnit.Grams]!!)

                        //replace unit
                        unitsLut[CookingUnit.Grams]?.let { output =  output.replace (it, "oz") }
                        return output
                    }
                    //if already correct
                    return wholeString

                }
                CookingUnit.KiloGrams -> {
                    if(settings["Units.metric Weight"] == "false" ){
                        //convert to pounds
                        output = output.replaceNumberBeforeValues(startingValue,(startingValue.vulgarFraction* 2.204623f).toString(),unitsLut[CookingUnit.KiloGrams]!!)

                        //replace unit
                        unitsLut[CookingUnit.KiloGrams]?.let { output =  output.replace (it, "lb") }
                        return output
                    }
                    //if already correct
                    return wholeString
                }
                CookingUnit.Ounce -> {
                    if(settings["Units.metric Weight"] == "true" ){
                        //convert to grams
                        output = output.replaceNumberBeforeValues(startingValue,(startingValue.vulgarFraction* 28.34952f).toString(),unitsLut[CookingUnit.Ounce]!!)

                        //replace unit
                        unitsLut[CookingUnit.Ounce]?.let { output =  output.replace (it, "g") }
                        return output
                    }
                    //if already correct
                    return wholeString
                }
                CookingUnit.Pound -> {
                    if(settings["Units.metric Weight"] == "true" ){
                        if (startingValue.vulgarFraction* 0.4535924f > 1){
                            //convert to kilograms
                            output = output.replaceNumberBeforeValues(startingValue,(startingValue.vulgarFraction* 0.4535924f).toString(),unitsLut[CookingUnit.Pound]!!)

                            //replace unit
                            unitsLut[CookingUnit.Pound]?.let { output =  output.replace (it, "kg") }
                            return output
                        }else {
                            //smaller than kg so convert to grams
                            //convert to grams
                            output = output.replaceNumberBeforeValues(startingValue,(startingValue.vulgarFraction* 453.5924f).toString(),unitsLut[CookingUnit.Pound]!!)

                            //replace unit
                            unitsLut[CookingUnit.Pound]?.let { output =  output.replace (it, "g") }
                            return output
                        }
                    }
                    //if already correct
                    return wholeString
                }


                else -> return wholeString //if not covered
            }
        }

        private fun List<String>.indexOf(words: List<String>) : Int {
            for (value in words){
                val index = this.indexOf(value)
                if (index != -1) return  index
            }
            return -1
        }
        //https://coolconversion.com/cooking-volume-weight/
        private val volumeWeightLut = mapOf(
            Pair("water", 1F),
            Pair("olive oil", 0.9F),
            Pair("coconut oil", 0.924F),
            Pair("vegetable oil", 0.921F),
            Pair("sunflower oil", 0.921F),
            Pair("oil",0.947F),
            Pair("milk", 1.04F),
            Pair("greek yogurt", 1.18F),
            Pair("butter", 0.955F),
            Pair("margarine", 1.06F),
            Pair("caster sugar", 0.845F),
            Pair("brown sugar", 0.93F),
            Pair("icing sugar", 0.528F),
            Pair("sugar", 0.845F), //granulated
            Pair("bread flour", 0.575F),
            Pair("whole flour", 0.507F),
            Pair("flour", 0.528F),
            Pair("baking powder", 0.972F),
            Pair("lemon juice", 0.972F),
            Pair("basmati rice", 0.761F),
            Pair("brown rice", 0.803F),
            Pair("cocoa powder", 0.507F),
            Pair("oats",  0.351F),
            Pair("honey", 1.437F),
            Pair("golden syrup", 1.479F),
            Pair("treacle",1.22F),
            Pair("peanut butter", 1.01F),
            Pair("cornstarch", 0.507F),
            Pair("chopped onion", 0.22F),
            )
        private fun mlToWeight(valueToChange: String,input: String,metric: Boolean): String{
            val conversionValue = getWeightConversions(input)
            //if not found return ingredient
            if (conversionValue == -1f) return input
            var output:String
            if (metric){
                //use the multiple to convert to g
                output = input.replaceNumberBeforeValues(valueToChange,(valueToChange.vulgarFraction * conversionValue).toString(),listOf("ml"))
                //replace units
                output = output.replace(" ml ", " g ")
            }else {
                //use the multiple to oz
                output = input.replaceNumberBeforeValues(valueToChange,(valueToChange.vulgarFraction * conversionValue* 0.03527396f).toString(),listOf("ml"))
                //replace units
                output = output.replace(" ml ", " oz ")
            }
            return  output



        }
        private fun getWeightConversions(wholeString: String): Float {
            //look at each of the ingredient options
            for (ingredient in volumeWeightLut) {
                //work out what type of ingredient it is
                if (wholeString.contains(ingredient.key, true)) {
                    return ingredient.value
                }
            }
            return -1f
        }
        fun getCookingStepDisplayText(step: CookingStep, settings: Map<String, String>): String{
            //what the device is text
            var text = when (step.type){
                CookingStage.prep ->  "prepare"
                CookingStage.wait -> "wait"
                CookingStage.hob -> "on the hob"
                else -> "In the ${step.type.text}"
            }
            //time text if one is set
            if (step.time != ""){
                text += " for ${step.time}"
            }
            //if there is a container
            if (step.container != null){
                //starting container text
                if (step.container!!.type != TinOrPanOptions.tray){ //tray needs different framing
                    text += " in a"
                }else {
                    text += " on a"
                }

                //if the container has a size
                if (step.container!!.dimensionTwo!= null){ //2 dimension size
                    //if the user wants it in inches show the inches else output it in cm
                    text += if (settings["Units.metric Lengths"] == "false"){
                        val convertedValueOne = (step.container!!.dimensionOne!! * 0.3937008f).vulgarFraction //convert to fraction the display the fraction or float depending on settings
                        val convertedValueTwo = (step.container!!.dimensionTwo!! * 0.3937008f).vulgarFraction
                        " ${if (settings["Units.Fractional Numbers"]== "true")convertedValueOne.first else convertedValueOne.second}x${if (settings["Units.Fractional Numbers"]== "true")convertedValueTwo.first else convertedValueTwo.second} inches"
                    }else {
                        " ${if (settings["Units.Fractional Numbers"]== "true")  step.container!!.dimensionOne!!.vulgarFraction.first else step.container!!.dimensionOne!!.vulgarFraction.second}x${if (settings["Units.Fractional Numbers"]== "true")  step.container!!.dimensionTwo!!.vulgarFraction.first else step.container!!.dimensionTwo!!.vulgarFraction.second} cm"
                    }


                }
                else if (step.container!!.dimensionOne!= null){ //one dimension size
                    //if the user wants it in inches show the inches else output it in cm
                    text += if (settings["Units.metric Lengths"] == "false"){
                        val convertedVal = (step.container!!.dimensionOne!! * 0.3937008f).vulgarFraction //convert to fraction the display the fraction or float depending on settings
                        " ${if (settings["Units.Fractional Numbers"]== "true")convertedVal.first else convertedVal.second} inch"
                    }else {
                        " ${if (settings["Units.Fractional Numbers"]== "true")  step.container!!.dimensionOne!!.vulgarFraction.first else step.container!!.dimensionOne!!.vulgarFraction.second} cm"
                    }

                }
                //if the container has a volume
                if (step.container!!.volume!= null){
                    //if the user wants it in pints show the inches else output it in litres
                    text += if (settings["Units.metric Volume"] == "false"){
                        val convertedVal = (step.container!!.volume!! * 1.759754f).vulgarFraction //convert to fraction the display the fraction or float depending on settings
                        " ${if (settings["Units.Fractional Numbers"]== "true")convertedVal.first else convertedVal.second} pint"
                    }else {
                        " ${if (settings["Units.Fractional Numbers"]== "true")step.container!!.volume!!.vulgarFraction.first else step.container!!.volume!!.vulgarFraction.second} litre"
                    }

                }
                //container name
                text += " ${step.container!!.type.text}"
            }

            //if there is a temperature
            if (step.cookingTemperature != null){
                //if its a  oven
                text += if (step.cookingTemperature!!.temperature != null){//oven
                    //get temperature in correct unit according to settings
                    val temperature  =if (settings["Units.Temperature"]=="true"){
                        "${step.cookingTemperature!!.temperature}°C"
                    }else {
                        "${((step.cookingTemperature!!.temperature?.times((9f/5f)) ?: 0f) + 32).toInt()}°F"
                    }
                    " at $temperature ${if (step.cookingTemperature!!.isFan == true) "fan" else ""}"
                } else{ // hob
                    " at ${step.cookingTemperature!!.hobTemperature.text} heat"
                }

            }
            text += "."
            return  text
        }

    }
}