package com.example.braintree.model.transaction

import kotlinx.serialization.Serializable

@Serializable
data class SaleResponse(
    val transactionId: String = ""
)