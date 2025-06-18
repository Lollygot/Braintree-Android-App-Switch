package com.example.braintree

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPaymentIntent
import com.braintreepayments.api.paypal.PayPalPaymentUserAction
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalVaultRequest
import com.example.braintree.model.auth.ClientTokenRequest
import com.example.braintree.model.customer.CreateRequest
import com.example.braintree.model.transaction.SaleRequest
import com.example.braintree.network.ServerSideApi
import com.example.braintree.ui.theme.BraintreeTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private const val TAG = "Braintree"
private const val AMOUNT = "50.00"

// persistent data store
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "persistentStorage")
private val CHECKOUT_FLOW = stringPreferencesKey("checkoutFlow")
private val PENDING_REQUEST = stringPreferencesKey("pendingRequest")

private enum class CheckoutFlow {
    ONE_TIME_CHECKOUT,
    VAULT_WITHOUT_PURCHASE
}

class MainActivity : ComponentActivity() {
    private lateinit var paypalLauncher: PayPalLauncher
    private lateinit var paypalClient: PayPalClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.v(TAG, "Received onCreate callback")

        Log.i(TAG, "Creating PayPal launcher")
        paypalLauncher = PayPalLauncher()
        Log.v(TAG, "PayPal launcher successfully created")

        val clientToken: String
        runBlocking {
            Log.i(TAG, "Getting client token")
            val clientTokenResponse = ServerSideApi.retrofitService.getClientToken(
                ClientTokenRequest(
                    merchantAccountId = "test-USD"
                )
            )
            Log.v(TAG, "Received clientTokenResponse: $clientTokenResponse")
            val token = clientTokenResponse.token

            if (token == "") {
                Log.e(TAG, "No client token received")
                throw Error("Error fetching client token")
            }

            clientToken = token
        }

        Log.i(TAG, "Creating PayPal client")
        paypalClient = PayPalClient(
            context = this,
            authorization = clientToken,
            appLinkReturnUrl = Uri.parse("https://playgroundappswitch.android.com"),
            deepLinkFallbackUrlScheme = "com.example.braintree.braintree"
        )
        Log.v(TAG, "PayPal client successfully created")

        enableEdgeToEdge()
        setContent {
            BraintreeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    App(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        Log.v(TAG, "Received onResume callback")

        handleReturnToApp(intent)
    }

    @Composable
    private fun App(
        modifier: Modifier = Modifier
    ) {
        Column(modifier = modifier) {
            Button(
                onClick = {
                    onPayPalButtonClick(
                        checkoutFlow = CheckoutFlow.ONE_TIME_CHECKOUT
                    )
                }
            ) {
                Text(
                    text = "One-time Checkout"
                )
            }

            HorizontalDivider()

            Button(
                onClick = {
                    onPayPalButtonClick(
                        checkoutFlow = CheckoutFlow.VAULT_WITHOUT_PURCHASE
                    )
                }
            ) {
                Text(
                    text = "Vault without Purchase"
                )
            }
        }
    }

    private fun onPayPalButtonClick(checkoutFlow: CheckoutFlow) {
        Log.v(TAG, "Received onPayPalButtonClick callback with checkoutFlow: $checkoutFlow")

        val request: PayPalRequest = when(checkoutFlow) {
            CheckoutFlow.ONE_TIME_CHECKOUT -> {
                PayPalCheckoutRequest(
                    amount = AMOUNT,
                    currencyCode = "USD",
                    hasUserLocationConsent = true,
                    intent = PayPalPaymentIntent.SALE,
                    localeCode = "en_US",
                    merchantAccountId = "test-USD",
                    userAction = PayPalPaymentUserAction.USER_ACTION_COMMIT,

                    // app switch parameters
                    enablePayPalAppSwitch = true,
                    // seems to be required for app switch for some reason
                    userAuthenticationEmail = "sb-impd140174035@personal.example.com"
                )
            }
            CheckoutFlow.VAULT_WITHOUT_PURCHASE -> {
                PayPalVaultRequest(
                    hasUserLocationConsent = true,
                    localeCode = "en_US",
                    merchantAccountId = "test-USD",

                    // app switch parameters
                    enablePayPalAppSwitch = true,
                    // seems to be required for app switch for some reason
                    userAuthenticationEmail = "sb-impd140174035@personal.example.com"
                )
            }
        }

        Log.i(TAG, "Creating paymentAuthRequest")
        paypalClient.createPaymentAuthRequest(this, request) { paymentAuthRequest ->
            when (paymentAuthRequest) {
                is PayPalPaymentAuthRequest.ReadyToLaunch -> {
                    Log.v(TAG, "paymentAuthRequest is ReadyToLaunch")

                    Log.i(TAG, "Launching paymentAuthRequest")
                    val pendingRequest = paypalLauncher.launch(this, paymentAuthRequest)
                    when (pendingRequest) {
                        is PayPalPendingRequest.Started -> {
                            Log.v(TAG, "pendingRequest is Started")

                            Log.i(TAG, "Storing pending request information")
                            writePendingRequestInformationToDataStore(
                                checkoutFlow = checkoutFlow,
                                pendingRequest = pendingRequest.pendingRequestString
                            )
                        }
                        is PayPalPendingRequest.Failure -> {
                            Log.v(TAG, "pendingRequest is Failure")

                            Log.e(TAG, "Received pendingRequest error: ${pendingRequest.error}")
                            throw pendingRequest.error
                        }
                    }
                }
                is PayPalPaymentAuthRequest.Failure -> {
                    Log.v(TAG, "paymentAuthRequest is Failure")

                    Log.e(TAG, "Received paymentAuthRequest error: ${paymentAuthRequest.error}")
                    throw paymentAuthRequest.error
                }
            }
        }
    }

    private fun writePendingRequestInformationToDataStore(
        checkoutFlow: CheckoutFlow,
        pendingRequest: String
    ) {
        Log.v(TAG, "Received writePendingRequestToDataStore call with checkoutFlow argument: $checkoutFlow and pendingRequest argument: $pendingRequest")

        Log.i(TAG, "Writing pending request information to persistent data store")
        val context = this
        runBlocking {
            context.dataStore.edit { persistentStorage ->
                persistentStorage[CHECKOUT_FLOW] = checkoutFlow.name
                persistentStorage[PENDING_REQUEST] = pendingRequest
            }
        }
        Log.v(TAG, "Completed writing pending request information to persistent data store")
    }

    private fun handleReturnToApp(intent: Intent) {
        Log.v(TAG, "Received handleReturnToApp callback")

        Log.i(TAG, "Getting stored pending request information from persistent data store")
        val pendingRequestInformation = getPendingRequestInformationFromDataStore()
        val checkoutFlow = pendingRequestInformation.first
        val pendingRequest = pendingRequestInformation.second

        if (pendingRequest == "") {
            Log.e(TAG, "No pendingRequest found")
            return
        }
        if (checkoutFlow == null) {
            Log.e(TAG, "No checkoutFlow found")
            return
        }

        Log.i(TAG, "Getting paymentAuthResult from paypalLauncher")
        Log.v(TAG, "Using stored pendingRequest: $pendingRequest")
        val paymentAuthResult = paypalLauncher.handleReturnToApp(
            pendingRequest = PayPalPendingRequest.Started(pendingRequest),
            intent = intent
        )
        when (paymentAuthResult) {
            is PayPalPaymentAuthResult.Success -> {
                Log.v(TAG, "paymentAuthResult is Success")

                Log.i(TAG, "Clearing pending request information")
                clearPendingRequestInformationInDataStore()
                completePayPalFlow(checkoutFlow, paymentAuthResult)
            }

            is PayPalPaymentAuthResult.NoResult -> {
                Log.v(TAG, "paymentAuthResult is NoResult")

                Log.i(TAG, "Customer cancelled payment flow or did not complete authentication flow")
            }

            is PayPalPaymentAuthResult.Failure -> {
                Log.v(TAG, "paymentAuthResult is Failure")

                Log.e(TAG, "Received paymentAuthResult error: ${paymentAuthResult.error}")
                throw paymentAuthResult.error
            }
        }
    }

    // Pair.first is the checkoutFlow, Pair.second is the pendingRequest
    private fun getPendingRequestInformationFromDataStore(): Pair<CheckoutFlow?, String> {
        Log.v(TAG, "Received getPendingRequestFromDataStore call")

        var checkoutFlow: CheckoutFlow?
        var pendingRequest: String
        Log.i(TAG, "Getting pendingRequest from persistent storage")
        val context = this
        runBlocking {
            val dataStore = context.dataStore.data.first()
            pendingRequest = dataStore[PENDING_REQUEST] ?: ""
            checkoutFlow = when (dataStore[CHECKOUT_FLOW]) {
                "ONE_TIME_CHECKOUT" -> {
                    CheckoutFlow.ONE_TIME_CHECKOUT
                }
                "VAULT_WITHOUT_PURCHASE" -> {
                    CheckoutFlow.VAULT_WITHOUT_PURCHASE
                }
                else -> {
                    null
                }
            }
        }
        Log.i(TAG, "Read checkoutFlow: $checkoutFlow and pendingRequest: $pendingRequest")

        return Pair(checkoutFlow, pendingRequest)
    }

    private fun clearPendingRequestInformationInDataStore() {
        Log.v(TAG, "Received clearPendingRequestInformationInDataStore call")

        Log.i(TAG, "Clearing pending request information in persistent data store")
        val context = this
        runBlocking {
            context.dataStore.edit { persistentStorage ->
                persistentStorage[CHECKOUT_FLOW] = ""
                persistentStorage[PENDING_REQUEST] = ""
            }
        }
        Log.v(TAG, "Completed clearing pending request information in persistent data store")
    }

    private fun completePayPalFlow(
        checkoutFlow: CheckoutFlow,
        paymentAuthResult: PayPalPaymentAuthResult.Success
    ) {
        Log.v(TAG, "Received completePayPalFlow call")

        Log.i(TAG, "Tokenising paymentAuthResult")
        paypalClient.tokenize(paymentAuthResult) { result ->
            when (result) {
                is PayPalResult.Success -> {
                    Log.v(TAG, "Tokenisation result is Success")

                    Log.i(TAG, "Received payment method nonce: ${result.nonce.string}")
                    runBlocking {
                        when (checkoutFlow) {
                            CheckoutFlow.ONE_TIME_CHECKOUT -> {
                                Log.i(TAG, "Creating transaction")
                                runBlocking {
                                    val saleResponse = ServerSideApi.retrofitService.createTransaction(
                                        SaleRequest(
                                            amount = AMOUNT,
                                            paymentMethodNonce = result.nonce.string,
                                            merchantAccountId = "test-USD"
                                        )
                                    )
                                    Log.i(TAG, "Successfully created and submitted for settlement transaction with ID: ${saleResponse.transactionId}")
                                }
                            }
                            CheckoutFlow.VAULT_WITHOUT_PURCHASE -> {
                                Log.i(TAG, "Creating customer")
                                runBlocking {
                                    val createResponse = ServerSideApi.retrofitService.createCustomer(
                                        CreateRequest(
                                            paymentMethodNonce = result.nonce.string
                                        )
                                    )
                                    Log.i(TAG, "Successfully create customer with ID: ${createResponse.customerId}")
                                }
                            }
                        }
                    }
                }
                is PayPalResult.Failure -> {
                    Log.v(TAG, "Tokenisation result is Failure")

                    Log.e(TAG, "Received tokenisation result error: ${result.error}")
                }
                is PayPalResult.Cancel -> {
                    Log.v(TAG, "Tokenisation result is Cancel")

                    Log.i(TAG, "Customer cancelled payment flow")
                }
            }
        }
    }
}