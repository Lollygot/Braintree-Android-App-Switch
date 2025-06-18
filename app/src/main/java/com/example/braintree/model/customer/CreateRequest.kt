package com.example.braintree.model.customer

import kotlinx.serialization.Serializable

@Serializable
data class CreateRequest(
    val paymentMethodNonce: String
)