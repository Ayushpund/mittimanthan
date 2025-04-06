package com.example.pict.models
data class SoilHealth(
    val pH: Float = 6.5f,
    val nitrogen: Int = 45,
    val phosphorus: Int = 30,
    val potassium: Int = 25,
    val organicMatter: Float = 3.5f,
    val soilType: String = "Loamy"
)

data class CropPrediction(
    val suggestedCrop: String = "Wheat",
    val plantingTime: String = "October-November"
)

data class FertilizerRecommendation(
    val primaryFertilizer: String = "NPK 14-14-14",
    val secondaryFertilizer: String = "Organic Compost"
)

data class IrrigationSchedule(
    val frequency: String = "Every 3 days",
    val waterAmount: Int = 500
)

data class Weather(
    val temperature: Float = 25.0f,
    val humidity: Int = 65,
    val rainProbability: Int = 30,
    val windSpeed: Float = 12.5f
) 