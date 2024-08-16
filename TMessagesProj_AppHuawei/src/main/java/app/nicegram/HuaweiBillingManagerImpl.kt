package app.nicegram

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import androidx.fragment.app.Fragment
import com.appvillis.core_network.ApiService
import com.appvillis.core_network.data.body.HuaweiSubscriptionRequestBody
import com.appvillis.core_network.data.body.HuaweiTopUpRequestBody
import com.appvillis.feature_nicegram_billing.R
import com.appvillis.feature_nicegram_billing.domain.BillingManager
import com.appvillis.feature_nicegram_billing.domain.InApp
import com.appvillis.rep_user.domain.UserBalanceRepository
import com.appvillis.rep_user.domain.UserRepository
import com.google.android.exoplayer2.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.huawei.hms.iap.Iap
import com.huawei.hms.iap.IapApiException
import com.huawei.hms.iap.entity.ConsumeOwnedPurchaseReq
import com.huawei.hms.iap.entity.InAppPurchaseData
import com.huawei.hms.iap.entity.OrderStatusCode
import com.huawei.hms.iap.entity.OwnedPurchasesReq
import com.huawei.hms.iap.entity.ProductInfo
import com.huawei.hms.iap.entity.ProductInfoReq
import com.huawei.hms.iap.entity.PurchaseIntentReq
import com.huawei.hms.support.api.client.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONException
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class HuaweiBillingManagerImpl(
    private val context: Context,
    private val preferences: SharedPreferences,
    private val appCoroutineScope: CoroutineScope,
    private val userBalanceRepository: UserBalanceRepository,
    private val userRepository: UserRepository,
    private val apiService: ApiService
) : BillingManager, BillingManager.BillingResultActivity.BillingResultListener {
    companion object {
        private const val TAG = "HuaweiBillingManagerImpl"
        private const val PREF_HAS_GIFTED_PREM = "PREF_HAS_GIFTED_PREM"
        private const val PURCHASE_REQUEST_CODE = 11110
        private const val PREF_SENT_PURCHASE_TOKENS = "PREF_SENT_PURCHASE_TOKENS"
    }

    private val gson = Gson()

    private var _subIds = listOf<String>()

    private val _eventBillingIsReady = MutableStateFlow<Boolean>(false)
    override val eventBillingIsReady: StateFlow<Boolean> get() = _eventBillingIsReady

    override val userHasActiveSub: Boolean
        get() = currentSubPurchaseToken != null

    private var _userHasGiftedPremium = false
    override val userHasActiveSub: Boolean
        get() = _userHasGiftedPremium

    override var userHasGiftedPremium: Boolean
        get() = false
        set(value) {
            _userHasGiftedPremium = value
        }

    private val _userSubState = MutableStateFlow(this.userHasActiveSub)
    override val userSubState: StateFlow<Boolean>
        get() = _userSubState

    private val _billingStateFlow = MutableSharedFlow<BillingManager.BillingState>()
    override val billingStateFlow: Flow<BillingManager.BillingState>
        get() = _billingStateFlow

    private var _currentSubPurchaseToken: String? = "null"
    override val currentSubPurchaseToken
        get() = _currentSubPurchaseToken

    override fun initializeBilling() {
        Log.d("BILLING_TEST", "initializeBilling")
        connectToBilling()
        _userHasGiftedPremium = preferences.getBoolean(PREF_HAS_GIFTED_PREM, false)
    }

    private fun connectToBilling() {
        val task = Iap.getIapClient(context).isEnvReady
        task.addOnSuccessListener { result ->
            val carrierId = result.carrierId
            Log.d("BILLING_TEST", "isEnv Ready TRUE")
            _eventBillingIsReady.tryEmit(true)
            queryAllPurchases()
        }.addOnFailureListener { e ->
            Log.d("BILLING_TEST", "isEnv Ready FALSE")
            if (e is IapApiException) {
                e.printStackTrace()
                val status: Status = e.status
                if (status.statusCode == OrderStatusCode.ORDER_HWID_NOT_LOGIN) {
                    Log.d("BILLING_TEST", "error status ${status.statusCode}")
                    // HUAWEI ID is not signed in.
                    if (status.hasResolution()) {
                        Log.d("BILLING_TEST", "status.hasResolution()")
                        try {
                            // 6666 is a constant defined by yourself.
                            // Open the sign-in screen returned.
                            //status.startResolutionForResult(activity, 6666)
                        } catch (exp: IntentSender.SendIntentException) {
                        }
                    }
                } else if (status.statusCode == OrderStatusCode.ORDER_ACCOUNT_AREA_NOT_SUPPORTED) {
                    // The current country/region does not support HUAWEI IAP.
                    Log.d("BILLING_TEST", "error status ${status.statusCode}")
                }
            } else {
                // Other external errors.

            }
        }
    }

    override fun restore() {
        queryAllPurchases()
    }

    override suspend fun queryStoreProductInfo(productIds: List<String>, isSubs: Boolean): List<InApp.StoreInfo> {
        Log.d("BILLING_TEST", "queryStoreProductInfo $productIds")
        val req = ProductInfoReq()
        req.priceType = if (isSubs) 2 else 0 // 0: consumable; 1: non-consumable; 2: subscription
        req.productIds = productIds

        return suspendCoroutine {
            val task = Iap.getIapClient(context).obtainProductInfo(req)
            task.addOnSuccessListener { result ->
                Log.d("BILLING_TEST", "obtainProductInfo $result")
                val productList = result.productInfoList
                it.resume(productList.map { info -> HuaweiStoreInfo(info) })
                productList.forEach { Log.d("BILLING_TEST", "info ${it.productId} ${it.price}") }
            }.addOnFailureListener { e ->
                Log.d("BILLING_TEST", "obtainProductInfo fail")
                if (e is IapApiException) {
                    val apiException = e
                    val returnCode = apiException.statusCode
                    e.printStackTrace()
                } else {
                    // Other external errors.
                }
                it.resumeWith(Result.failure(e))
            }
        }

    }

    private fun queryAllPurchases() {
        // Construct an OwnedPurchasesReq object.
        Log.d("BILLING_TEST", "queryAllPurchases subs")
        val ownedPurchasesReq = OwnedPurchasesReq()
        ownedPurchasesReq.priceType = 2 //if (isSubs) 2 else 0 // 0: consumable; 1: non-consumable; 2: subscription
        val task = Iap.getIapClient(context).obtainOwnedPurchases(ownedPurchasesReq)
        task.addOnSuccessListener { result ->
            if (result?.inAppPurchaseDataList != null) {
                for (i in result.inAppPurchaseDataList.indices) {
                    val inAppPurchaseData = result.inAppPurchaseDataList[i]
                    val inAppSignature = result.inAppSignature[i]
                    Log.d("BILLING_TEST", "result $result")
                    Log.d("BILLING_TEST", "inAppPurchaseData $inAppPurchaseData")

                    processPurchase(inAppPurchaseData, inAppSignature)
                }
            }
        }.addOnFailureListener { e ->
            Log.d("BILLING_TEST", "obtainOwnedPurchases failure")
            Log.e("BILLING_TEST", "obtainOwnedPurchases failure", e)
            e.printStackTrace()
            if (e is IapApiException) {
                val apiException = e as IapApiException
                val status: Status = apiException.status
                val returnCode = apiException.statusCode
            } else {
                // Other external errors.
            }
        }

        Log.d("BILLING_TEST", "queryAllPurchases inapps")
        val ownedPurchasesReqConsumables = OwnedPurchasesReq()
        ownedPurchasesReqConsumables.priceType = 0 //if (isSubs) 2 else 0 // 0: consumable; 1: non-consumable; 2: subscription
        val taskConsumables = Iap.getIapClient(context).obtainOwnedPurchases(ownedPurchasesReqConsumables)
        taskConsumables.addOnSuccessListener { result ->
            if (result?.inAppPurchaseDataList != null) {
                for (i in result.inAppPurchaseDataList.indices) {
                    val inAppPurchaseData = result.inAppPurchaseDataList[i]
                    val inAppSignature = result.inAppSignature[i]
                    Log.d("BILLING_TEST", "result cons $result")
                    Log.d("BILLING_TEST", "inAppPurchaseData cons $inAppPurchaseData")

                    processPurchase(inAppPurchaseData, inAppSignature)
                }
            }
        }.addOnFailureListener { e ->
            Log.d("BILLING_TEST", "obtainOwnedPurchases cons failure")
            Log.e("BILLING_TEST", "obtainOwnedPurchases cons failure", e)
            e.printStackTrace()
            if (e is IapApiException) {
                val apiException = e as IapApiException
                val status: Status = apiException.status
                val returnCode = apiException.statusCode
            } else {
                // Other external errors.
            }
        }
    }

    private val currentSendingTokens = mutableSetOf<String>()

    private fun sendPurchaseToServer(skuId: String, subscriptionId: String, purchaseToken: String, isSub: Boolean) {
        Log.d("BILLING_TEST","Sending purchase to server, id: $skuId token: $purchaseToken subId: $subscriptionId")
        if (wasPurchaseTokenAlreadySent(purchaseToken)) {
            Log.d("BILLING_TEST","Purchase token was already sent")
            return
        }

        if (currentSendingTokens.contains(purchaseToken)) {
            Log.d("BILLING_TEST","Purchase token already sending")
            return
        }

        if (userRepository.isUserLoggedIn) currentSendingTokens.add(purchaseToken)

        appCoroutineScope.launch {
            _billingStateFlow.emit(BillingManager.BillingState.Loading)
            try {
                var newBalance = 0L

                if (userRepository.isUserLoggedIn) {
                    Log.d("BILLING_TEST", "user logged in")
                    val response =
                        if (isSub) apiService.buyNicegramSubscriptionHuawei(
                            HuaweiSubscriptionRequestBody(
                                token = purchaseToken,
                                subscriptionId = subscriptionId,
                                subscriptionSku = skuId
                            )
                        ) else apiService.buyNicegramTopUpHuawei(
                            HuaweiTopUpRequestBody(token = purchaseToken, productId = skuId)
                        )

                    val result = response.data ?: throw java.lang.Exception(response.message ?: "")
                    newBalance = result.balance
                    Log.d("BILLING_TEST", "response status ${response.status}")

                    if (response.status == 200) {
                        Log.d("BILLING_TEST", "savePurchaseTokenAsSent")
                        savePurchaseTokenAsSent(purchaseToken)
                    }

                    if (!isSub) {
                        Log.d("BILLING_TEST", "ConsumeOwnedPurchaseReq $purchaseToken")
                        val req = ConsumeOwnedPurchaseReq().apply {
                            setPurchaseToken(purchaseToken)
                        }
                        Iap.getIapClient(context).consumeOwnedPurchase(req)
                    }
                }


                _billingStateFlow.emit(
                    BillingManager.BillingState.Success(
                        newBalance,
                        isSub
                    )
                )
            } catch (e: Exception) {
                currentSendingTokens.remove(purchaseToken)

                Timber.e(e)
                e.printStackTrace()
                var error = "Error"
                if (e is IOException) {
                    error = context.getString(R.string.Error_Network)
                } else if (e is HttpException) {
                    error = context.getString(R.string.Error_Default) + " Code: ${e.code()}"
                }
                Log.e("BILLING_TEST", "error $error", e)
                _billingStateFlow.emit(
                    BillingManager.BillingState.Error(
                        error
                    )
                )
            }
        }
    }

    class InAppPurchaseJson(val subIsvalid: Boolean, val productId: String, val kind: Int, val purchaseToken: String)

    override fun launchPayment(fragment: Fragment, storeInfo: InApp.StoreInfo) {
        appCoroutineScope.launch {
            _billingStateFlow.emit(BillingManager.BillingState.Loading)
        }

        val req = PurchaseIntentReq()
        req.productId = storeInfo.id
        req.priceType = if (_subIds.contains(storeInfo.id)) 2 else 0
        req.developerPayload = "test payload"
        val task = Iap.getIapClient(fragment.requireActivity()).createPurchaseIntent(req)
        task.addOnSuccessListener { result ->
            Log.d("BILLING_TEST", "createPurchaseIntent $result")
            // Obtain the order creation result.
            val status: Status = result.status
            if (status.hasResolution()) {
                try {
                    // Open the checkout screen returned.
                    (fragment.requireActivity() as? BillingManager.BillingResultActivity)?.addResultListener(this)
                    status.startResolutionForResult(fragment.requireActivity(), PURCHASE_REQUEST_CODE)
                } catch (exp: IntentSender.SendIntentException) {
                    exp.printStackTrace()
                }
            }
        }.addOnFailureListener { e ->
            appCoroutineScope.launch {
                _billingStateFlow.emit(BillingManager.BillingState.Error(e.message.toString()))
            }

            Log.d("BILLING_TEST", "createPurchaseIntent fail")
            e.printStackTrace()
            if (e is IapApiException) {
                val apiException = e as IapApiException
                val status: Status = apiException.status
                val returnCode = apiException.statusCode
            } else {
                // Other external errors.
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("BILLING_TEST", "onActivityResult billing manager")

        if (requestCode == PURCHASE_REQUEST_CODE) {
            if (data == null) {
                Log.e("BILLING_TEST", "data is null")
                return
            }
            // Call the parsePurchaseResultInfoFromIntent method to parse the payment result.
            val purchaseResultInfo = Iap.getIapClient(context).parsePurchaseResultInfoFromIntent(data)
            when (purchaseResultInfo.returnCode) {
                OrderStatusCode.ORDER_STATE_CANCEL -> {
                    Log.d("BILLING_TEST", "ORDER_STATE_CANCEL")
                    appCoroutineScope.launch {
                        _billingStateFlow.emit(BillingManager.BillingState.Idle)
                    }
                }

                OrderStatusCode.ORDER_STATE_FAILED, OrderStatusCode.ORDER_PRODUCT_OWNED, OrderStatusCode.ORDER_STATE_DEFAULT_CODE -> {
                    Log.d("BILLING_TEST", "return code ${purchaseResultInfo.returnCode}")
                    appCoroutineScope.launch {
                        _billingStateFlow.emit(BillingManager.BillingState.Error("return code: ${purchaseResultInfo.returnCode}" + purchaseResultInfo.errMsg))
                    }
                }

                OrderStatusCode.ORDER_STATE_SUCCESS -> {
                    // The payment is successful.
                    Log.d("BILLING_TEST", "ORDER_STATE_SUCCESS")
                    val inAppPurchaseData = purchaseResultInfo.inAppPurchaseData
                    val inAppPurchaseDataSignature = purchaseResultInfo.inAppDataSignature
                    Log.d("BILLING_TEST", "inAppPurchaseData $inAppPurchaseData")
                    Log.d("BILLING_TEST", "inAppPurchaseDataSignature $inAppPurchaseDataSignature")

                    processPurchase(inAppPurchaseData, inAppPurchaseDataSignature)
                }

                else -> {
                    Log.d("BILLING_TEST", "else ${purchaseResultInfo.returnCode}")
                    appCoroutineScope.launch {
                        _billingStateFlow.emit(BillingManager.BillingState.Idle)
                    }
                }
            }
        }
    }

    private fun processPurchase(inAppPurchaseData: String, inAppPurchaseDataSignature: String) {
        try {
            val inAppPurchaseDataBean = InAppPurchaseData(inAppPurchaseData)
            val purchaseState = inAppPurchaseDataBean.purchaseState
            val isSubValid = inAppPurchaseDataBean.isSubValid
            val purchaseDataParsed = gson.fromJson(inAppPurchaseData, InAppPurchaseJson::class.java)

            if (purchaseDataParsed.kind == 2 && isSubValid && purchaseState == 0) {
                Log.d(
                    "BILLING_TEST",
                    "Purchase, productId: ${purchaseDataParsed.productId} kind: ${purchaseDataParsed.kind} token: ${purchaseDataParsed.purchaseToken}"
                )
                Timber.d("BILLING_TEST _currentSubPurchaseToken set to ${purchaseDataParsed.purchaseToken}")

                _currentSubPurchaseToken = purchaseDataParsed.purchaseToken
                _userSubState.value = this.userHasActiveSub
                appCoroutineScope.launch {
                    _billingStateFlow.emit(
                        BillingManager.BillingState.Success(
                            userBalanceRepository.gemBalance,
                            true
                        )
                    )
                }

                sendPurchaseToServer(purchaseDataParsed.productId, inAppPurchaseDataBean.subscriptionId, purchaseDataParsed.purchaseToken, true)
            } else if (purchaseDataParsed.kind == 0 && purchaseState == 0) {
                sendPurchaseToServer(purchaseDataParsed.productId, "", purchaseDataParsed.purchaseToken, false)
            }

            Log.d("BILLING_TEST", "purchase state $purchaseState")
        } catch (e: JSONException) {
            e.printStackTrace()
            Log.e("BILLING_TEST", "ERROR", e)
        }
    }

    override fun onPaymentScreenResume() {
        queryAllPurchases()
    }

    override fun setSubsIds(ids: List<String>) {
        _subIds = ids

        Iap.getIapClient(context).isEnvReady.addOnSuccessListener {
            queryAllPurchases()
        }
    }

    private fun savePurchaseTokenAsSent(token: String) {
        val newList = sentTokens.toMutableList().apply {
            add(token)
        }

        preferences.edit().putString(PREF_SENT_PURCHASE_TOKENS, gson.toJson(newList)).apply()
    }

    private fun wasPurchaseTokenAlreadySent(token: String): Boolean {
        return sentTokens.contains(token)
    }

    private val sentTokens: List<String> get() {
        return try {
            val json = preferences.getString(PREF_SENT_PURCHASE_TOKENS, null)
            val listType = object : TypeToken<List<String>?>() {}.type
            val result = gson.fromJson<List<String>>(json, listType)
            Log.d("BILLING_TEST", "already sentTokens ${result.size} $result")
            return result
        } catch (e: Exception) {
            Timber.e(e)
            listOf()
        }
    }

    class HuaweiStoreInfo(private val productInfo: ProductInfo) : InApp.StoreInfo {
        override val priceWithCurrency: String
            get() = productInfo.price
        override val id: String
            get() = productInfo.productId
    }
}