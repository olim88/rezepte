package com.example.rezepte.ui.theme

import com.example.rezepte.CookingStage
import com.example.rezepte.CookingStep
import com.example.rezepte.CookingStepContainer
import com.example.rezepte.CookingStepTemperature
import com.example.rezepte.HobOption
import com.example.rezepte.Instruction
import com.example.rezepte.Instructions
import com.example.rezepte.TinOrPanOptions

class CreateAutomations {
    companion object {
        fun autoGenerateStepsFromInstructions(instructions: Instructions) : Pair<List<CookingStep>, Instructions>{
            val generatedSteps : MutableList<CookingStep> = mutableListOf()
            var ovenStepIndex = -1
            var lastStepStage : CookingStage? = null
            for (instruction in instructions.list){
                //look for oven related steps
                val stage = getInstructionStage(instruction.text,lastStepStage)
                //get the temperature
                var temperature : CookingStepTemperature? = null
                if (stage == CookingStage.oven || stage == CookingStage.hob){
                    temperature = getInstructionTemp(instruction.text,  stage == CookingStage.oven)
                }
                //get the time
                val time = getInstructionTime(instruction.text)
                //get the container
                val container = getInstructionContainer(instruction.text)
                //either link to existing or create step
                if (ovenStepIndex>= 0 &&stage == CookingStage.oven){ //if its oven there is usually only one use of the oven so combine the data together
                    //if there are multiple times create new step for new time
                    if (time != "" && generatedSteps[ovenStepIndex].time !=""){
                        //create the step and if there is missing categories assume it is the same as the main oven step and copy them across
                        generatedSteps.add(CookingStep(generatedSteps.size,time,stage,container,temperature))
                        if(generatedSteps[generatedSteps.size-1].time== "") {generatedSteps[generatedSteps.size-1].time = generatedSteps[ovenStepIndex].time}
                        if(generatedSteps[generatedSteps.size-1].container== null) {generatedSteps[generatedSteps.size-1].container = generatedSteps[ovenStepIndex].container}
                        if(generatedSteps[generatedSteps.size-1].cookingTemperature== null) {generatedSteps[generatedSteps.size-1].cookingTemperature = generatedSteps[ovenStepIndex].cookingTemperature}
                        instruction.linkedCookingStepIndex = generatedSteps.size-1
                    }
                    else {
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
                }
                else if ( lastStepStage!= null && stage == lastStepStage &&!(time != "" && generatedSteps[generatedSteps.size-1].time !="")){//if its the same type of stage as the one before it combine it if they do not have seperate times
                    if(generatedSteps[generatedSteps.size-1].time== "") {generatedSteps[generatedSteps.size-1].time = time}
                    if(generatedSteps[generatedSteps.size-1].container== null) {generatedSteps[generatedSteps.size-1].container = container}
                    if(generatedSteps[generatedSteps.size-1].cookingTemperature== null) {generatedSteps[generatedSteps.size-1].cookingTemperature = temperature}
                    instruction.linkedCookingStepIndex = generatedSteps.size-1
                }
                else{ //if there is not a step to link it to create a new step
                    generatedSteps.add(CookingStep(generatedSteps.size,time,stage,container,temperature))
                    //set values for the oven step if its the first one
                    if (stage == CookingStage.oven){
                        ovenStepIndex = generatedSteps.size-1
                    }
                    instruction.linkedCookingStepIndex = generatedSteps.size-1

                }
                //set value of last step
                lastStepStage = stage


            }






            return  Pair(generatedSteps,instructions)
        }
        private fun getInstructionStage(text:String,lastStep: CookingStage?) : CookingStage {
            val cleanText = getCleanText(text)
            if (cleanText.contains(" (hob)|(simmer)|(pan)|(sauté)|(skillet)|(boil)|(fry) ".toRegex())) { return CookingStage.hob}
            if (cleanText.contains(" (oven)|(bake) ".toRegex())) { return CookingStage.oven}
            if (cleanText.contains(" fridge ")) { return CookingStage.fridge}
            if (cleanText.contains(" (wait)|(sit for)|(leave) ".toRegex())) { return CookingStage.wait}
            //if the last stage was hob infer cook as hob else infer it as oven
            if (cleanText.contains(" cook ")){
                return if (lastStep == CookingStage.hob){
                    CookingStage.hob
                } else{
                    CookingStage.oven
                }
            }
            return  CookingStage.prep // most likely if can not find word hinting at what it is
        }
        private fun getInstructionTemp(text: String,isOven: Boolean) : CookingStepTemperature?{ //todo see if fan can be found if that is what the user wants
            val words = getWords(text)
            if (isOven) {//if looking for temerature for oven
                for ((index, word) in words.withIndex()) {
                    if (word.matches("[0-9]+[°º]?c".toRegex())) {//should be a temperature
                        if (index < words.count() - 1 && words[index + 1].lowercase() == "fan") {//if fan or not
                            return CookingStepTemperature(
                                word.replace("[°º]?c".toRegex(), "").toInt(),
                                HobOption.zero,
                                true
                            )
                        }
                        return CookingStepTemperature(
                            word.replace("[°º]?c".toRegex(), "").toInt(),
                            HobOption.zero,
                            false
                        )
                    }
                }
            }
            else{ //if looking for temperature for hob
                for ((index, word) in words.withIndex()) {
                    if(word.lowercase() == "heat" && index >0){//if it fits the key word and is not the first word
                        val value = try{
                            HobOption.valueOf(words[index-1])
                        }catch (e : Exception){
                            when(words[index-1]){//other descriptors that need converting into the format used
                                "gently" -> HobOption.lowMedium
                                "medium-low" -> HobOption.lowMedium
                                "medium-high" -> HobOption.highMedium
                                else -> HobOption.zero
                            }
                        }
                        if (value != HobOption.zero){//only return if a value is found as there may be another place where it is said
                            return CookingStepTemperature(null,value ,null)
                        }
                    }
                }

            }
            return null
        }
        private fun getInstructionContainer(text: String) : CookingStepContainer?{
            val cleanText = getCleanText(text)
            val option = when{
                cleanText.contains(" frying pan ") -> TinOrPanOptions.fryingPan
                cleanText.contains("pan ") -> TinOrPanOptions.saucePan
                cleanText.contains(" wok ") -> TinOrPanOptions.saucePan
                cleanText.contains(" bowl ") -> TinOrPanOptions.bowl
                cleanText.contains(" trays? ".toRegex()) -> TinOrPanOptions.tray
                cleanText.contains("roasting tin ") -> TinOrPanOptions.roastingTin
                cleanText.contains(" rectangular tin ") -> TinOrPanOptions.rectangleTin
                cleanText.contains(" tins? ".toRegex()) -> TinOrPanOptions.roundTin
                else -> TinOrPanOptions.none
            }

            //if tin see if size can be found
            var size : Int? = null
            if (option == TinOrPanOptions.roundTin || option == TinOrPanOptions.rectangleTin){
                val words = getWords(text)
                for (word in words){
                    if (word.matches("[0-9]+cm".toRegex())){
                        size = word.removeSuffix("cm").toIntOrNull()
                    }
                }
            }
            if ( option != TinOrPanOptions.none){
                return CookingStepContainer(option,size)
            }
            return null
        }

        private fun getInstructionTime(text: String) : String{
            val words = getWords(text)
            for ((index,word) in words.withIndex()){
                if (word!= "" && word.matches("(seconds)|(min(ute)?s?)|(hours?)".toRegex())){
                    return "${words[index-1]} $word"
                }
            }

            return  ""
        }
        private fun getWords(text: String) : List<String> { return text.lowercase().split("[\\s,/.;?()]+".toRegex())}

        private  fun getCleanText(text: String) : String { return  text.lowercase().replace("[.;,()|/]".toRegex()," ")}


        fun autoSplitInstructions(instructions: Instructions,strength: InstructionSplitStrength) : Instructions{


            //go though each instructions and split it in to the amount needed
            when(strength ){
                InstructionSplitStrength.Sentences -> {
                    //split at every full stop found in the instructions
                    var newInstructions = mutableListOf<Instruction>() //create new list
                    var index = 0
                    for (instruction in instructions.list){
                        instruction.text.split(".").forEach {
                            if (!it.matches("\\s*".toRegex())){
                                newInstructions.add(Instruction(index,"$it.",null))
                                index ++
                            }
                        }
                    }
                    return  Instructions(newInstructions)
                }
                InstructionSplitStrength.Intelligent -> {
                    //split at full stops only if criteria is met so instructions are not separated when they do not need to be
                    var newInstructions = mutableListOf<Instruction>() //create new list
                    var index = 0
                    for (instruction in instructions.list){
                        val sentences = instruction.text.split(".")
                        var nextInstruction = ""
                        for (sentenceIndex in 0..sentences.count()-1){
                            val sentence = sentences[sentenceIndex]//current sentence looking to split of from what is before it
                            if (!sentence.matches("\\s*".toRegex())) {
                                if (getIsNewSentence(sentence)) {
                                    //add the next instruction to the instructions and start fresh with this sentence
                                    if (nextInstruction != "") {
                                        newInstructions.add(Instruction(index, nextInstruction, null))
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

                    return  Instructions(newInstructions)

                }
                else -> {return instructions}
            }




        }
        private fun getIsNewSentence(sentence : String) : Boolean{//todo not that smart yet need to examin more recipes to work out what i need to do
            if (sentence.length < 28) return false // to short to think about splitting off
            if (sentence.startsWith(")")) return false //if its ending inside a bracket do not split it


            return  true // if passes all checks return try
        }
        enum class InstructionSplitStrength {
            Off,
            Intelligent,
            Sentences,
        }
    }
}