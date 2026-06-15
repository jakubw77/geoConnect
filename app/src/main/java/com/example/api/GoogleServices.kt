package com.example.api

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

// --- GOOGLE PEOPLE API MODELS ---

data class PeopleResponse(
    val connections: List<Person>?
)

data class Person(
    val resourceName: String?,
    val names: List<Name>?,
    val emailAddresses: List<EmailAddress>?,
    val photos: List<Photo>?
)

data class Name(
    val displayName: String?
)

data class EmailAddress(
    val value: String?
)

data class Photo(
    val url: String?,
    val default: Boolean?
)

data class GoogleUserProfile(
    val name: String?,
    val picture: String?,
    val email: String?
)

// --- GOOGLE DIRECTIONS SERVICE MODELS ---

data class DirectionsResponse(
    val routes: List<Route>?,
    val status: String
)

data class Route(
    val bounds: Bounds?,
    val legs: List<Leg>?,
    val overview_polyline: OverviewPolyline?,
    val summary: String?
)

data class Bounds(
    val northeast: LatLngLiteral?,
    val southwest: LatLngLiteral?
)

data class LatLngLiteral(
    val lat: Double,
    val lng: Double
)

data class Leg(
    val distance: TextValue?,
    val duration: TextValue?,
    val end_address: String?,
    val start_address: String?,
    val end_location: LatLngLiteral?,
    val start_location: LatLngLiteral?,
    val steps: List<Step>?
)

data class TextValue(
    val text: String,
    val value: Int // meters or seconds
)

data class Step(
    val distance: TextValue?,
    val duration: TextValue?,
    val end_location: LatLngLiteral?,
    val html_instructions: String?,
    val polyline: OverviewPolyline?,
    val start_location: LatLngLiteral?,
    val travel_mode: String?
)

data class OverviewPolyline(
    val points: String?
)

// --- RETROFIT INTERFACES ---

interface GooglePeopleApi {
    @GET("v1/people/me/connections")
    suspend fun getConnections(
        @Header("Authorization") authHeader: String,
        @Query("personFields") personFields: String = "names,photos,emailAddresses",
        @Query("pageSize") pageSize: Int = 100
    ): PeopleResponse

    @GET("v1/people/me")
    suspend fun getMyProfile(
        @Header("Authorization") authHeader: String,
        @Query("personFields") personFields: String = "names,photos,emailAddresses"
    ): Person
}

interface GoogleDirectionsApi {
    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("mode") mode: String = "driving",
        @Query("key") apiKey: String
    ): DirectionsResponse
}

// --- RETROFIT CLIENT BUILDERS ---

object GoogleApiClients {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val peopleApi: GooglePeopleApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://people.googleapis.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GooglePeopleApi::class.java)
    }

    val directionsApi: GoogleDirectionsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GoogleDirectionsApi::class.java)
    }
}
