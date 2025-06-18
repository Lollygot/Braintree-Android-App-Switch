package com.example.braintree.model.customer

import kotlinx.serialization.Serializable

@Serializable
data class CreateResponse(
    val customerId: String = ""
)