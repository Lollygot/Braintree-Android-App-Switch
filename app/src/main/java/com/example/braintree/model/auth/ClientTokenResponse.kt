package com.example.braintree.model.auth

import kotlinx.serialization.Serializable

@Serializable
data class ClientTokenResponse(
    val token: String = ""
)