package com.example.pict.models

import com.example.pict.R

data class Crop(
    val name: String,
    val imageResId: Int,
    val suitabilityScore: Int, // 0-100
    val plantingSeason: String,
    val waterRequirement: String,
    val soilType: String
)

class CropRecommendations {
    val recommendedCrops: List<Crop> = listOf(
        Crop("Rice", R.drawable.rice, 85, "June-July", "High", "Clay Loam"),
        Crop("Maize", R.drawable.maize, 78, "June-July", "Moderate", "Loamy"),
        Crop("Wheat", R.drawable.wheat, 90, "October-November", "Moderate", "Loamy"),
        Crop("Cotton", R.drawable.cotton, 75, "March-May", "Moderate", "Black Soil"),
        Crop("Groundnut", R.drawable.groundnuts, 82, "June-July", "Low", "Sandy Loam"),
        Crop("Potato", R.drawable.potato, 88, "October-November", "Moderate", "Sandy Loam"),
        Crop("Mango", R.drawable.mango, 70, "June-July", "Moderate", "Deep Loamy"),
        Crop("Grapes", R.drawable.grapes, 72, "January-February", "Moderate", "Well-drained"),
        Crop("Orange", R.drawable.orange, 68, "June-July", "Moderate", "Loamy"),
        Crop("Banana", R.drawable.banana, 85, "June-July", "High", "Rich Loamy"),
        Crop("Pomegranate", R.drawable.pomogrenate, 75, "July-August", "Low", "Well-drained"),
        Crop("Chickpea", R.drawable.chickpea, 80, "October-November", "Low", "Sandy Loam"),
        Crop("Lentil", R.drawable.lentil, 77, "October-November", "Low", "Loamy"),
        Crop("Mung Bean", R.drawable.mung, 82, "June-July", "Low", "Sandy Loam"),
        Crop("Blackgram", R.drawable.blackgram, 79, "June-July", "Low", "Clay Loam"),
        Crop("Pigeon Peas", R.drawable.pigeon_peas, 81, "June-July", "Low", "Well-drained"),
        Crop("Coffee", R.drawable.cofee, 65, "May-June", "High", "Well-drained"),
        Crop("Carrot", R.drawable.carrot, 85, "October-November", "Moderate", "Sandy Loam"),
        Crop("Watermelon", R.drawable.watermelon, 78, "January-February", "Moderate", "Sandy Loam"),
        Crop("Muskmelon", R.drawable.muskmelon, 76, "January-February", "Moderate", "Sandy Loam"),
        Crop("Apple", R.drawable.apple, 70, "December-January", "Moderate", "Well-drained"),
        Crop("Papaya", R.drawable.papaya, 82, "June-July", "Moderate", "Well-drained"),
        Crop("Coconut", R.drawable.coconut, 75, "June-July", "High", "Sandy Loam"),
        Crop("Jute", R.drawable.jute, 72, "March-April", "High", "Loamy"),
        Crop("Millet", R.drawable.millet, 85, "June-July", "Low", "Sandy Loam")
    )

    fun getTopRecommendedCrops(count: Int = 5): List<Crop> {
        return recommendedCrops.sortedByDescending { it.suitabilityScore }.take(count)
    }

    fun searchCrops(query: String): List<Crop> {
        return recommendedCrops.filter { 
            it.name.lowercase().contains(query.lowercase()) 
        }
    }
} 