package com.example.maps
data class Location(val lat: Double, val lng: Double)
data class Geometry(val location: Location)
data class OpeningHours(val weekday_text: List<String>?)

data class PlaceResult(
    val place_id: String,
    val name: String,
    val geometry: Geometry,
    val vicinity: String,
    val rating: Double?,
    val user_ratings_total: Int?
)

data class PlacesModels(val results: List<PlaceResult>)
data class PlaceDetailsResult(
    val name: String,
    val vicinity: String,
    val formatted_phone_number: String?,
    val website: String?,
    val rating: Double?,
    val user_ratings_total: Int?,
    val opening_hours: OpeningHours?
)

data class PlaceDetailsResponse(val result: PlaceDetailsResult)