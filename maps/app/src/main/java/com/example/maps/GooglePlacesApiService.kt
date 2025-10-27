package com.example.maps
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GooglePlacesApiService {

    @GET("maps/api/place/nearbysearch/json")
    suspend fun getNearbyPlaces(
        @Query("location") location: String,
        @Query("radius") radius: Int,       // en metros
        @Query(value="type") type: String,     // "hospital", "restaurant", "tourist_attraction"
        @Query("key") apiKey: String
    ): Response<PlacesModels>
    @GET("maps/api/place/details/json")
    suspend fun getPlaceDetails(
        @Query("place_id") placeId: String,
        @Query("fields") fields: String,
        @Query("key") apiKey: String,
        @Query("language") language: String = "es"
    ): Response<PlaceDetailsResponse>
}