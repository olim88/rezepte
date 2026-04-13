package olim.tests.recipeCreation.externalLoading

import android.graphics.Point
import olim.android.rezepte.recipeCreation.externalLoading.Box
import olim.android.rezepte.recipeCreation.externalLoading.ImageToRecipe
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
@RunWith(RobolectricTestRunner::class)
class ImageToRecipeTest {


    @Test
    fun testGetBox(){
        val box = ImageToRecipe.getBox(arrayOf(Point(0, 0), Point(0, 1), Point(1, 1), Point(1, 0)))
        Assert.assertEquals(Box(Point(0, 0), Point(1, 1)), box)
    }
}
