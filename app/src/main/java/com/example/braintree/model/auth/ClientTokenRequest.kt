package com.example.braintree.model.auth

import kotlinx.serialization.Serializable

@Serializable
data class ClientTokenRequest(
    val merchantAccountId: String = ""
)