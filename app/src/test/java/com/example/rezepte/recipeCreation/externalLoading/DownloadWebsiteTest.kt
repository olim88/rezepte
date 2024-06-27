package com.example.rezepte.recipeCreation.externalLoading

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadWebsiteTest {

    @Test
    fun BBCTest() {
        val data = DownloadWebsite.main("https://www.bbc.co.uk/food/recipes/chicken_caesar_pasta_47125", mapOf())
        assertEquals(data.first.data.website, "https://www.bbc.co.uk/food/recipes/chicken_caesar_pasta_47125" )
        assertEquals(data.first.data.name, "Chicken Caesar pasta salad")
        assertEquals(data.first.data.serves, "Serves 4")
        assertEquals(data.first.ingredients.list[0].text, "4 chicken thighs, skinless and boneless")
        assertEquals(data.first.ingredients.list.count(), 12)
        assertEquals(data.first.instructions.list[0].text, "Put the chicken and the hot stock into a saucepan and bring to the boil over a high heat. Reduce the heat to low and simmer for 10–15 minutes, until just cooked through. Using a slotted spoon, remove the chicken from the pan and set aside to cool.")
        assertEquals(data.first.instructions.list.count(), 5)
        assertEquals(data.second, "https://food-images.files.bbci.co.uk/food/recipes/chicken_caesar_pasta_47125_16x9.jpg")
    }

    @Test
    fun htmlInstructionsTest() {
        val data = DownloadWebsite.main("https://www.jamieoliver.com/recipes/cauliflower-recipes/harissa-cauliflower-traybake/", mapOf())
        assertEquals(data.first.instructions.list[0].text, "Preheat the oven to 220°C/425°F/gas 7.")
        assertEquals(data.first.instructions.list.count(), 7)
    }

    @Test
    fun noImageTest() {
        //if dose not error out and get name assume works
        val data = DownloadWebsite.main("https://www.bbc.co.uk/food/recipes/creamymushroomsontoa_79618", mapOf())
        assertEquals(data.first.data.name, "Creamy mushrooms on toast")
    }

    @Test
    fun multipleSchemaTest() {
        val data = DownloadWebsite.main("https://minimalistbaker.com/curried-butternut-squash-soup/#wprm-recipe-container-35467", mapOf())
        assertEquals(data.first.data.name, "Curried Butternut Squash Soup")
    }

    @Test
    fun todoSchemaTest() {
        val data = DownloadWebsite.main("https://www.jamieoliver.com/features/how-to-roast-pumpkin-seeds/", mapOf())
        assertEquals(data.first.data.name, "How to roast pumpkin seeds")
        assertEquals(data.first.instructions.list[0].text, "Scrape the seeds from your pumpkin with a wide, flat spoon.")
        assertEquals(data.first.instructions.list.count(), 11)
    }

    @Test
    fun customNigellaTest(){
        val data = DownloadWebsite.main("https://www.nigella.com/recipes/salmon-fishcakes", mapOf())
        assertEquals(data.first.data.name, "Salmon Fishcakes")
        assertEquals(data.first.data.serves, "Makes: 7-9 x 7cm/3 inch fishcakes")
        assertEquals(data.first.ingredients.list[0].text, "approx. 500 grams cold mashed potatoes")
        assertEquals(data.first.ingredients.list.count(), 12)
        assertEquals(data.first.instructions.list[0].text, "In a large bowl, and preferably with your hands, mix together all the fishcake ingredients.")
        assertEquals(data.first.instructions.list.count(), 3)
        assertEquals(data.second, "https://www.nigella.com/assets/uploads/recipes/public-thumbnail/salmon-fishcakes-562a2a33bd6db.jpg")
    }
}