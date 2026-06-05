package olim.tests.recipeCreation

import olim.android.rezepte.CookingStage
import olim.android.rezepte.CookingStep
import olim.android.rezepte.CookingStepContainer
import olim.android.rezepte.CookingStepTemperature
import olim.android.rezepte.HobOption
import olim.android.rezepte.Instruction
import olim.android.rezepte.Instructions
import olim.android.rezepte.TinOrPanOptions
import olim.android.rezepte.recipeCreation.CreateAutomations
import org.junit.Assert
import org.junit.Test

class CreateAutomationsTest {

    @Test
    fun testOvenFanTemp() {
        Assert.assertEquals(
            CookingStepTemperature(102, HobOption.Zero, true),
            CreateAutomations.getInstructionTemp("123C/ 102C Fan/100F", true, true, false)
        )
    }

    @Test
    fun testOvenNotFanTemp() {
        Assert.assertEquals(
            CookingStepTemperature(123, HobOption.Zero, false),
            CreateAutomations.getInstructionTemp("102C Fan/ 123C/ ", true, false, false)
        )
    }

    @Test
    fun testOvenFahrenheitTemp() {
        Assert.assertEquals(
            CookingStepTemperature(37, HobOption.Zero, false, "F"),
            CreateAutomations.getInstructionTemp("102C Fan/ 123C/100F ", true, false, true)
        )
    }

    @Test
    fun testOvenOnlyFahrenheitTemp() {
        Assert.assertEquals(
            CookingStepTemperature(37, HobOption.Zero, false, "F"),
            CreateAutomations.getInstructionTemp("100F ", true, false, false)
        )
    }

    @Test
    fun testDimensions1DMetric() {
        Assert.assertEquals(
            CookingStepContainer(TinOrPanOptions.RoundTin, 20f, null, null),
            CreateAutomations.getInstructionContainer("20cm(8in)round tin ", true, true)
        )
    }

    @Test
    fun testDimensions1DImp() {
        Assert.assertEquals(
            CookingStepContainer(TinOrPanOptions.RoundTin, 20.32f, null, null),
            CreateAutomations.getInstructionContainer("20cm(8in) round tin ", false, true)
        )
    }

    @Test
    fun testDimensions2DMetric() {
        Assert.assertEquals(
            CookingStepContainer(TinOrPanOptions.RectangleTin, 20f, 25f, null),
            CreateAutomations.getInstructionContainer("20x25 cm(8 x8 in) tin ", true, true)
        )
    }

    @Test
    fun testDimensions2DImp() {
        Assert.assertEquals(
            CookingStepContainer(TinOrPanOptions.RectangleTin, 20.32f, 20.32f, null),
            CreateAutomations.getInstructionContainer("20x20 cm(8 x8- in) tin ", false, true)
        )
    }

    @Test
    fun testVolumeMetric() {
        Assert.assertEquals(
            CookingStepContainer(TinOrPanOptions.Dish, null, null, 5f),
            CreateAutomations.getInstructionContainer("5l dish ", false, true)
        )
    }


    @Test
    fun testVolumeImp() {
        Assert.assertEquals(
            CookingStepContainer(TinOrPanOptions.Dish, null, null, 1.7047837f),
            CreateAutomations.getInstructionContainer("5l(3pint) dish ", false, false)
        )
    }

    @Test
    fun testRealInstructionStep() {
        Assert.assertEquals(
            CookingStep(
                0,
                "25 minutes",
                CookingStage.oven,
                CookingStepContainer(TinOrPanOptions.RoundTin, 20f, null, null),
                CookingStepTemperature(160, HobOption.Zero, true, null)
            ),
            CreateAutomations.autoGenerateStepsFromInstructions(
                instructions = Instructions(
                    listOf(
                        Instruction(1, "Preheat the oven to 180C/160C Fan/Gas 4. Grease and line two 20cm/8in sandwich tins", null),
                        Instruction(2, "Bake the cakes on the middle shelf of the oven for 25 minutes. Check them after 20 minutes. The cakes are done when they’re golden-brown and coming away from the edge of the tins. Press them gently to check – they should be springy to the touch. ", null)
                    )
                ),
                settings = mapOf(
                    "Units.Temperature" to "false",
                    "Units.metric Lengths" to "true",
                    "Units.metric Volume" to "true",
                    "Units.Fan Oven" to "true"
                )
            ).first[0]
        )

    }

}