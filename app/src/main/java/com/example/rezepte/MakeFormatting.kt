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
        private val numberRegex = Regex("([0-9]+(/|\\d*\\.)?[0-9]*([${fractions.joinToString("")}]?))|([${fractions.joinToString("")}])") //the regex to show that it is a number

        private val fractionValues = arrayOf(
            1.0,
            15.0 / 16, 7.0 / 8, 13.0 / 16, 3.0 / 4, 11.0 / 16,
            5.0 / 8, 9.0 / 16, 1.0 / 2, 7.0 / 16, 3.0 / 8,
            5.0 / 16, 1.0 / 4, 3.0 / 16, 1.0 / 8, 1.0 / 16,
            0.0
        )

        fun getCorrectUnitsAndValues(string :String, multiplier: Float, settings : Map<String,String>) : String {

            //replace numbers with multiplied value
            val output = convertUnitOfString(string,settings) //make sure that the numbers all have the correct unit
            return multiplyBy(output,multiplier,settings["Units.Fractional Numbers"]== "true",if (settings["Units.Fractional Numbers"] == "true") 0.02f else -1f)
        }
        private enum class CookingUnit {
            Teaspoon,
            Tablespoon,
            Cup,
            Milliliters,
            Liters,
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
            Pair(CookingUnit.Milliliters , listOf("ml","millilitres") ),
            Pair(CookingUnit.Liters , listOf("liters","l") ),
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
                if (number== "/") continue
                var value = number.vulgarFraction * multiplier
                if (roundPercentage> 0 && abs(value - value.roundToInt()) /value < roundPercentage){//if enabled e.g. bigger than 0 and then if the rounding to a whole number effects it less than the percentage round it
                    value = value.roundToInt().toFloat()
                }

                output = if (isVulgar){
                    output.replace(number,value.vulgarFraction.first)
                }else {
                    output.replace(number, (value.vulgarFraction.second).toString().replace(".0 ", " "))
                }
            }
            return output
        }
        private fun getWordsWithNumbers(text: String) : List<String> { return text.lowercase().split("[\\s,;?()-]+".toRegex())}

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

        private fun convertUnitOfString(string: String, settings: Map<String,String>) : String{
            //clean the string from brackets and other things in the way
            val cleanWords= slitTextFromNumbers(getWordsWithNumbers(string))
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
            if (unitType == null) return string // can not find units so not converting them
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

        private fun fixUnits(wholeString: String, startingValue: String, cookingUnitType: CookingUnit, settings: Map<String, String>): String{
            //make sure unit is correct for the users settings
            var output = wholeString
            when ( cookingUnitType){
                CookingUnit.Teaspoon -> {
                    if (settings["Units.Tea Spoons"] == "true") return wholeString // unit is correct
                    //convert to volume
                    output = output.replace(startingValue,(startingValue.vulgarFraction*teaSpoonVolume(settings)).toString())

                    //replace units
                    unitsLut[CookingUnit.Teaspoon]?.let { output = output.replace (it, "ml") }
                    //see if it can be turned into grams
                    output = mlToWeight((startingValue.vulgarFraction* teaSpoonVolume(settings)).toString(),output,settings["Units.metric Weight"] == "true" )
                    return  output
                }
                CookingUnit.Tablespoon -> {
                    if (settings["Units.Table Spoons"] == "true") return wholeString // unit is correct
                    //convert to volume
                    output = output.replace(startingValue,(startingValue.vulgarFraction* tableSpoonVolume(settings)).toString())
                    //replace units
                    unitsLut[CookingUnit.Tablespoon]?.let { output =  output.replace (it, "ml") }
                    //see if it can be turned into grams
                    output = mlToWeight((startingValue.vulgarFraction*tableSpoonVolume(settings)).toString(),output,settings["Units.metric Weight"] == "true" )
                    return  output
                }
                CookingUnit.Cup -> {
                    if (settings["Units.Cups"] == "true") return wholeString // unit is correct
                    //convert to volume
                    output = output.replace(startingValue,(startingValue.vulgarFraction* cupVolume(settings)).toString())
                    //replace units
                    unitsLut[CookingUnit.Cup]?.let { output =  output.replace (it, "ml") }
                    //see if it can be turned into grams
                    output = mlToWeight((startingValue.vulgarFraction*cupVolume(settings)).toString(),output,settings["Units.metric Weight"] == "true" )
                    return  output
                }
                CookingUnit.Milliliters -> {
                    //if could be converted to spoons or cups and that settings is enabled convert it
                    if(settings["Units.Tea Spoons"] == "true" && startingValue.vulgarFraction < 15f){
                        //convert to tea spoons and replace ml
                        output = output.replace(startingValue,(startingValue.vulgarFraction*(1/teaSpoonVolume(settings))).toString())
                        //replace unit
                        unitsLut[CookingUnit.Milliliters]?.let { output =  output.replace (it, "teaspoons") }
                        return output
                    }
                    else if(settings["Units.Table Spoons"] == "true" && startingValue.vulgarFraction < 60f){
                        //convert to table spoons and replace ml
                        output = output.replace(startingValue,(startingValue.vulgarFraction*(1/tableSpoonVolume(settings))).toString())
                        //replace unit
                        unitsLut[CookingUnit.Milliliters]?.let { output =  output.replace (it, "tablespoons") }
                        return output
                    }
                    else if(settings["Units.Cups"] == "true" ){
                        //convert to cup  and replace ml
                        output = output.replace(startingValue,(startingValue.vulgarFraction*(1/cupVolume(settings))).toString())
                        //replace unit
                        unitsLut[CookingUnit.Milliliters]?.let { output =  output.replace (it, "cups") }
                        return output
                    }
                    //if not using metric convert it to  fl oz
                    else if(settings["Units.metric Volume"] == "false" ){
                        //convert to fl oz
                        output = output.replace(startingValue,(startingValue.vulgarFraction* 0.035195).toString())

                        //replace unit
                        unitsLut[CookingUnit.Milliliters]?.let { output =  output.replace (it, "fl oz") }
                        return output
                    }

                    //if already correct
                    return wholeString
                }
                CookingUnit.Liters -> {
                    //if not using metric convert ot pint
                    if(settings["Units.metric Volume"] == "false" ){
                        //convert to pint
                        output = output.replace(startingValue,(startingValue.vulgarFraction* 1.7598f).toString())

                        //replace unit
                        unitsLut[CookingUnit.Liters]?.let { output =  output.replace (it, "pint") }
                        return output
                    }
                    //if already correct
                    return wholeString
                }
                CookingUnit.FluidOunce -> {
                    if(settings["Units.metric Volume"] == "true" ){
                        //convert to ml
                        output = output.replace(startingValue,(startingValue.vulgarFraction* 28.4131f).toString())

                        //replace unit
                        unitsLut[CookingUnit.FluidOunce]?.let { output =  output.replace (it, "ml") }
                        return output
                    }
                    //if already correct
                    return wholeString
                }
                CookingUnit.Pint -> {
                    if(settings["Units.metric Volume"] == "true" ){
                        //convert to l
                        output = output.replace(startingValue,(startingValue.vulgarFraction* 0.568262f).toString())

                        //replace unit
                        unitsLut[CookingUnit.Pint]?.let { output =  output.replace (it, "l") }
                        return output
                    }
                    //if already correct
                    return wholeString
                }
                CookingUnit.Grams -> {
                    if(settings["Units.metric Weight"] == "false" ){
                        //convert to oz
                        output = output.replace(startingValue,(startingValue.vulgarFraction* 0.03527396f).toString())

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
                        output = output.replace(startingValue,(startingValue.vulgarFraction* 2.204623f).toString())

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
                        output = output.replace(startingValue,(startingValue.vulgarFraction* 28.34952f).toString())

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
                            output = output.replace(startingValue,(startingValue.vulgarFraction* 0.4535924f).toString())

                            //replace unit
                            unitsLut[CookingUnit.Pound]?.let { output =  output.replace (it, "kg") }
                            return output
                        }else {
                            //smaller than kg so convert to grams
                            //convert to grams
                            output = output.replace(startingValue,(startingValue.vulgarFraction* 453.5924f).toString())

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
            //look at each of the ingredient options
            var output:String
            for ( ingredient in volumeWeightLut){
                //work out what type of ingredient it is
                if (input.contains(ingredient.key,true)){
                    if (metric){
                        //use the multiple to convert to g
                        output = input.replace(valueToChange,(valueToChange.vulgarFraction * ingredient.value).toString())
                        //replace units
                        output = output.replace(" ml ", " g ")
                    }else {
                        //use the multiple to oz
                        output = input.replace(valueToChange,(valueToChange.vulgarFraction * ingredient.value* 0.03527396f).toString())
                        //replace units
                        output = output.replace(" ml ", " oz ")
                    }
                    return  output
                }
            }
            //if not found just keep the volume
            return input
        }
    }
}