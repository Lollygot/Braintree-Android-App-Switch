package com.example.braintree.network

import com.example.braintree.model.auth.ClientTokenResponse
import com.example.braintree.model.transaction.SaleResponse
import com.example.braintree.model.auth.ClientTokenRequest
import com.example.braintree.model.customer.CreateRequest
import com.example.braintree.model.customer.CreateResponse
import com.example.braintree.model.transaction.SaleRequest
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST

private const val BASE_URL = "http://10.0.2.2:8888"

private val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

private val retrofit = Retrofit.Builder()
    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    .baseUrl(BASE_URL)
    .build()

interface ServerSideApiService {
    @POST("/braintree/sdk/customer/new")
    suspend fun createCustomer(
        @Body request: CreateRequest
    ): CreateResponse

    @POST("/braintree/sdk/transaction/new")
    suspend fun createTransaction(
        @Body request: SaleRequest
    ): SaleResponse

    @POST("/braintree/sdk/auth/client")
    suspend fun getClientToken(
        @Body request: ClientTokenRequest = ClientTokenRequest()
    ): ClientTokenResponse
}

object ServerSideApi {
    val retrofitService :ServerSideApiService by lazy {
        retrofit.create(ServerSideApiService::class.java)
    }
}