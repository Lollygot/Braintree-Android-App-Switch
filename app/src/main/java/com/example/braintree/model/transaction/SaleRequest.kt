package com.example.braintree.model.transaction

import kotlinx.serialization.Serializable

@Serializable
data class SaleRequest(
    val amount: String,
    val paymentMethodNonce: String,
    val merchantAccountId: String = "",
    val storeInVaultOnSuccess: Boolean = false,
    val submitForSettlement: Boolean = true
)