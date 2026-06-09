package olim.tests.RecipeMaking

import olim.android.rezepte.recipeMaking.MakeFormatting.Companion.numberRegex
import org.junit.Test

class MakeFormattingTest {


    @Test
    fun testNumberRegex() {
        assert((numberRegex.matches("2")))
        assert((numberRegex.matches("23.4")))
        assert((numberRegex.matches("1/8")))
        assert((numberRegex.matches("0.5")))
        assert((numberRegex.matches("⅞")))
        assert((numberRegex.matches("1⅞")))
    }
}