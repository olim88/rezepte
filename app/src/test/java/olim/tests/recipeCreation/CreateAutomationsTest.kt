package olim.tests.recipeCreation

import olim.android.rezepte.CookingStepTemperature
import olim.android.rezepte.HobOption
import olim.android.rezepte.recipeCreation.CreateAutomations
import org.junit.Assert
import org.junit.Test

class CreateAutomationsTest {

    @Test
    fun testOvenFanTemp() {
        Assert.assertEquals(
            CookingStepTemperature(102, HobOption.zero, true),
            CreateAutomations.getInstructionTemp("123C/ 102C Fan/100F", true, true, false)
        )
    }

    @Test
    fun testOvenNotFanTemp() {
        Assert.assertEquals(
            CookingStepTemperature(123, HobOption.zero, false),
            CreateAutomations.getInstructionTemp("102C Fan/ 123C/ ", true, false, false)
        )
    }

    @Test
    fun testOvenFahrenheitTemp() {
        Assert.assertEquals(
            CookingStepTemperature(37, HobOption.zero, false, "F"),
            CreateAutomations.getInstructionTemp("102C Fan/ 123C/100F ", true, false, true)
        )
    }

    @Test
    fun testOvenOnlyFahrenheitTemp() {
        Assert.assertEquals(
            CookingStepTemperature(37, HobOption.zero, false, "F"),
            CreateAutomations.getInstructionTemp("100F ", true, false, false)
        )
    }
}