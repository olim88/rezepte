package olim.android.rezepte.recipeCreation

import olim.android.rezepte.CookingStage
import olim.android.rezepte.CookingStep
import olim.android.rezepte.CookingStepContainer
import olim.android.rezepte.CookingStepTemperature
import olim.android.rezepte.HobOption
import olim.android.rezepte.Instruction
import olim.android.rezepte.Instructions
import olim.android.rezepte.TinOrPanOptions

class CreateAutomations {

    companion object {

        private val blankRegex = Regex("^\\s*$")
        private val sentenceSplitRegex = Regex("(?<=\\.)\\s+")
        private val timeRegex = Regex("(seconds)|(min(ute)?s?)|(hours?)")
        private val waitRegex =
            Regex(" (wait)|(sit for)|(leave)|(off the heat)|(allow to)|(set aside)|(cool) ")
        private val hobRegex = Regex(" (hob)|(simmer)|(pan)|(sauté)|(skillet)|(boil)|(fry) ")
        private val ovenRegex = Regex(" (oven)|(bake)|(roast) ")
        private val temperatureRegex =
            Regex("""(?:\s*(fan))?(-?\d+)([°º]?[cf]|℃|℉)(?:\s*(fan))?""", RegexOption.IGNORE_CASE)
        private val dimensionsRegex = Regex(
            """(\d+\.?\d*)\s*(?:[x×X](\d+\.?\d*))?\s*((cm|centimeter)|(in|inch))""",
            RegexOption.IGNORE_CASE
        )
        private val volumeRegex = Regex("""(\d+\.?\d*)(l|litre|pint|pt)""",RegexOption.IGNORE_CASE)

        private val cleanRegex = Regex("""[;,()|/\-_]""")

        fun autoGenerateStepsFromInstructions(
            instructions: Instructions,
            settings: Map<String, String>
        ): Pair<List<CookingStep>, Instructions> {
            val generatedSteps: MutableList<CookingStep> = mutableListOf()
            var ovenStepIndex = -1
            var lastStepStage: CookingStage? = null
            for (instruction in instructions.list) {
                //look for oven related steps
                val stage = getInstructionStage(instruction.text, lastStepStage)
                //get the temperature
                var temperature: CookingStepTemperature? = null
                if (stage == CookingStage.oven || stage == CookingStage.hob) {
                    temperature = getInstructionTemp(
                        instruction.text,
                        stage == CookingStage.oven,
                        settings["Units.Fan Oven"] == "true",
                        settings["Units.Temperature"] == "false"
                    )
                }
                //get the time
                val time = getInstructionTime(instruction.text)
                //get the container
                val container = getInstructionContainer(
                    instruction.text,
                    settings["Units.metric Lengths"] == "true",
                    settings["Units.metric Volume"] == "true"
                )
                //either link to existing or create step
                if (ovenStepIndex >= 0 && stage == CookingStage.oven) { //if its oven there is usually only one use of the oven so combine the data together
                    //if there are multiple times create new step for new time
                    if (time != "" && generatedSteps[ovenStepIndex].time != "") {
                        //create the step and if there is missing categories assume it is the same as the main oven step and copy them across
                        generatedSteps.add(
                            CookingStep(
                                generatedSteps.size, time, stage, container, temperature
                            )
                        )
                        if (generatedSteps[generatedSteps.size - 1].time == "") {
                            generatedSteps[generatedSteps.size - 1].time =
                                generatedSteps[ovenStepIndex].time
                        }
                        if (generatedSteps[generatedSteps.size - 1].container == null) {
                            generatedSteps[generatedSteps.size - 1].container =
                                generatedSteps[ovenStepIndex].container
                        }
                        if (generatedSteps[generatedSteps.size - 1].cookingTemperature == null) {
                            generatedSteps[generatedSteps.size - 1].cookingTemperature =
                                generatedSteps[ovenStepIndex].cookingTemperature
                        }
                        instruction.linkedCookingStepIndex = generatedSteps.size - 1
                    } else {
                        //if value dose not exist set it to the value for the found step
                        if (generatedSteps[ovenStepIndex].time == "") {
                            generatedSteps[ovenStepIndex].time = time
                        }
                        if (generatedSteps[ovenStepIndex].container == null) {
                            generatedSteps[ovenStepIndex].container = container
                        }
                        if (generatedSteps[ovenStepIndex].cookingTemperature == null) {
                            generatedSteps[ovenStepIndex].cookingTemperature = temperature
                        }
                        instruction.linkedCookingStepIndex = ovenStepIndex
                    }
                } else if (lastStepStage != null && stage == lastStepStage && !(time != "" && generatedSteps[generatedSteps.size - 1].time != "")) {//if its the same type of stage as the one before it combine it if they do not have separate times
                    if (generatedSteps[generatedSteps.size - 1].time == "") {
                        generatedSteps[generatedSteps.size - 1].time = time
                    }
                    if (generatedSteps[generatedSteps.size - 1].container == null) {
                        generatedSteps[generatedSteps.size - 1].container = container
                    }
                    if (generatedSteps[generatedSteps.size - 1].cookingTemperature == null) {
                        generatedSteps[generatedSteps.size - 1].cookingTemperature = temperature
                    }
                    instruction.linkedCookingStepIndex = generatedSteps.size - 1
                } else { //if there is not a step to link it to create a new step
                    generatedSteps.add(
                        CookingStep(
                            generatedSteps.size, time, stage, container, temperature
                        )
                    )
                    //set values for the oven step if its the first one
                    if (stage == CookingStage.oven) {
                        ovenStepIndex = generatedSteps.size - 1
                    }
                    instruction.linkedCookingStepIndex = generatedSteps.size - 1
                }
                //set value of last step
                lastStepStage = stage
            }
            return Pair(generatedSteps, instructions)
        }

        private fun getInstructionStage(text: String, lastStep: CookingStage?): CookingStage {
            val cleanText = getCleanText(text)
            if (cleanText.contains(hobRegex)) {
                return CookingStage.hob
            }
            if (cleanText.contains(ovenRegex)) {
                return CookingStage.oven
            }
            if (cleanText.contains(" fridge ")) {
                return CookingStage.fridge
            }
            if (cleanText.contains(waitRegex)) {
                return CookingStage.wait
            }
            //if the last stage was hob infer cook as hob else infer it as oven
            if (cleanText.contains(" cook ")) {
                return if (lastStep == CookingStage.hob) {
                    CookingStage.hob
                } else {
                    CookingStage.oven
                }
            }
            return CookingStage.prep // most likely if can not find word hinting at what it is
        }


        fun getInstructionTemp(
            text: String, isOven: Boolean, fanPreferred: Boolean, fahrenheitPreferred: Boolean
        ): CookingStepTemperature? {
            val words = getWords(text)
            if (isOven) {//if looking for temperature for oven

                val tempOptions = temperatureRegex.findAll(text).map { match ->
                    val (fan1, number, unit, fan2) = match.destructured

                    var temperature = number.toInt()
                    var originalUnit: String? = null
                    //convert fahrenheit to celsius
                    if (unit.contains("f", ignoreCase = true) || unit == "℉") {
                        originalUnit = "F"
                        temperature = ((5f / 9f) * (number.toFloat() - 32)).toInt()
                    }

                    CookingStepTemperature(
                        temperature,
                        HobOption.zero,
                        fan1.isNotEmpty() || fan2.isNotEmpty(),
                        originalUnit
                    )
                }.toList()
                //if the user want's a fan temperature priorities it over the other options
                if (fanPreferred) {
                    tempOptions.firstOrNull { it.isFan == true }?.let {
                        return it
                    }
                }
                //if the user want's a fahrenheit temperature priorities it over the other options
                if (fahrenheitPreferred) {
                    tempOptions.firstOrNull { it.originalUnit == "F" }?.let {
                        return it
                    }
                }
                //try to find non F and non fan option
                tempOptions.firstOrNull { it.originalUnit != "F" && it.isFan != true }?.let {
                    return it
                }
                //else return first
                tempOptions.firstOrNull().let {
                    return it
                }

            } else { //if looking for temperature for hob
                for ((index, word) in words.withIndex()) {
                    if (word.lowercase() == "heat" && index > 0) {//if it fits the key word and is not the first word
                        val value = try {
                            HobOption.valueOf(words[index - 1])
                        } catch (e: Exception) {
                            when (words[index - 1]) {//other descriptors that need converting into the format used
                                "gently" -> HobOption.lowMedium
                                "medium-low" -> HobOption.lowMedium
                                "medium-high" -> HobOption.highMedium
                                else -> HobOption.zero
                            }
                        }
                        if (value != HobOption.zero) {//only return if a value is found as there may be another place where it is said
                            return CookingStepTemperature(null, value, null)
                        }
                    }
                }
            }
            return null
        }


        fun getInstructionContainer(text: String, metricPreferred: Boolean, metricVolumePreferred: Boolean): CookingStepContainer? {
            val cleanText = getCleanText(text)
            val option = when {
                cleanText.contains(" frying pan ") -> TinOrPanOptions.fryingPan
                cleanText.contains("pan ") -> TinOrPanOptions.saucePan
                cleanText.contains(" wok ") -> TinOrPanOptions.saucePan
                cleanText.contains(" bowl ") -> TinOrPanOptions.bowl
                cleanText.contains(" trays? ".toRegex()) -> TinOrPanOptions.tray
                cleanText.contains(" loaf tin ") -> TinOrPanOptions.loafTin
                cleanText.contains(" roasting tin ") -> TinOrPanOptions.roastingTin
                cleanText.contains(" rectangular tin ") -> TinOrPanOptions.rectangleTin
                cleanText.contains(" muffin ") -> TinOrPanOptions.muffinTin
                cleanText.contains(" cupcake ") -> TinOrPanOptions.cupcakeTin
                cleanText.contains("tins?".toRegex()) -> TinOrPanOptions.roundTin
                cleanText.contains(" dish ") -> TinOrPanOptions.dish
                else -> TinOrPanOptions.none
            }

            data class Dimension(
                val d1: Float?,
                val d2: Float?,
                val metric: Boolean
            )
            //find dimensions if any
            val dimensions = dimensionsRegex.findAll(cleanText).map {
                val (d1, d2, unit) = it.destructured
                if (unit == "cm" || unit == "centimeter") {
                    Dimension(d1.toFloatOrNull(), d2.toFloatOrNull(), true)

                } else {
                    Dimension(
                        d1.toFloatOrNull()?.times(2.54f),
                        d2.toFloatOrNull()?.times(2.54f),
                        false
                    )
                }

            }.sortedBy {
                // prefer the uses main dimension
                if (metricPreferred) {
                    !it.metric
                } else {
                    it.metric
                }
            }
            dimensions.firstOrNull()?.let {
                //if got two dimensions but thinking its a round tin change it to rectangular todo maybe have this as one value and logic elsewhere changes
                if (it.d2 != null && option == TinOrPanOptions.roundTin) {
                    return CookingStepContainer(TinOrPanOptions.rectangleTin, it.d1, it.d2, null)
                }

                return CookingStepContainer(option, it.d1, it.d2, null)

            }

            //if no dimensions where found look for volume
            data class Volume(
                val volume: Float?,
                val metric: Boolean
            )
            val volume = volumeRegex.findAll(cleanText).map {
                val (volume, unit) = it.destructured
                if (unit == "l" || unit == "litre") {
                    Volume(volume.toFloatOrNull(), true)
                }
                else {
                    Volume(volume.toFloatOrNull()?.times(0.5682612f), false)
                }
            }.sortedBy {
                if (metricVolumePreferred) {
                    !it.metric
                } else {
                    it.metric
                }
            }
            volume.firstOrNull()?.let {
                return CookingStepContainer(option, null, null, it.volume)
            }

            return null
        }

        private fun getInstructionTime(text: String): String {
            val words = getWords(text)
            for ((index, word) in words.withIndex()) {
                if (word != "" && word.matches(timeRegex)) {
                    return "${words[index - 1]} $word"
                }
            }

            return ""
        }

        fun getWords(text: String): List<String> {
            return text.lowercase().split("[\\s,/.;?()]+".toRegex())
        }

        private fun getCleanText(text: String): String {
            return text.lowercase().replace(cleanRegex, " ")
        }

        fun autoSplitInstructions(
            instructions: Instructions, strength: InstructionSplitStrength
        ): Instructions {
            //go though each instruction and split it in to the amount needed
            when (strength) {
                InstructionSplitStrength.Sentences -> {
                    //split at every full stop found in the instructions
                    val newInstructions = mutableListOf<Instruction>() //create new list
                    var index = 0
                    for (instruction in instructions.list) {

                        instruction.text.split(sentenceSplitRegex).forEach {
                            if (!it.matches(blankRegex)) {
                                newInstructions.add(Instruction(index, "$it.", null))
                                index++
                            }
                        }
                    }
                    return Instructions(newInstructions)
                }

                InstructionSplitStrength.Intelligent -> {
                    //split at full stops only if criteria is met so instructions are not separated when they do not need to be
                    val newInstructions = mutableListOf<Instruction>() //create new list
                    var index = 0
                    for (instruction in instructions.list) {
                        val sentences = instruction.text.split(".")
                        var nextInstruction = ""
                        for (sentenceIndex in 0..<sentences.count()) {
                            val sentence =
                                sentences[sentenceIndex]//current sentence looking to split of from what is before it
                            if (!sentence.matches(blankRegex)) {
                                if (getIsNewSentence(sentence.removePrefix(" "))) {
                                    //add the next instruction to the instructions and start fresh with this sentence
                                    if (nextInstruction != "") {
                                        newInstructions.add(
                                            Instruction(
                                                index, nextInstruction, null
                                            )
                                        )
                                        index++
                                    }
                                    nextInstruction = ""
                                }

                                //add to next instruction
                                nextInstruction += "$sentence."
                            }
                        }
                        //output last bit of instruction if some left
                        if (nextInstruction != "") {
                            newInstructions.add(Instruction(index, nextInstruction, null))
                            index++
                        }
                    }

                    return Instructions(newInstructions)

                }

                else -> {
                    return instructions
                }
            }
        }

        private val falseStartingList = setOf(
            ")",//if its ending inside a bracket do not split it
            "this",//if starting with this is is probably describing the last step and should not be new
            "there",
            "again",
            "you",
            "your",
            "whichever",
            "however",
            "any",
            "but",
            "don't",
            "it is",
            "if this"

        )

        private fun getIsNewSentence(sentence: String): Boolean {//todo could be smarter
            if (sentence.length < 28) return false // to short to think about splitting off
            return !falseStartingList.any {
                sentence.startsWith(
                    it,
                    ignoreCase = true
                )
            } // if passes all checks return try
        }

        enum class InstructionSplitStrength {
            Intelligent, Sentences,
        }
    }
}