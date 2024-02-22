package com.appvillis.nicegram

import com.appvillis.feature_nicegram_billing.NicegramBillingHelper
import com.appvillis.rep_placements.domain.GetPinChatsPlacementsUseCase
import javax.annotation.Nullable

object NicegramPinChatsPlacementHelper {
    const val AI_ID = "lily_ai"

    var getPinChatsPlacementsUseCase: GetPinChatsPlacementsUseCase? = null

    private val hasNgPremium get() = NicegramBillingHelper.userHasNgPremiumSub
    private val filterByPremiumPlacements get() = (getPinChatsPlacementsUseCase?.invoke() ?: emptyList())
        .filter { placement ->
            val isUserTypeMatch =
                if (hasNgPremium) placement.showToPremium else true // Предполагаем, что все могут видеть непремиум-контент
            isUserTypeMatch
        }

    fun getPossiblePinChatsPlacements() = filterByPremiumPlacements

    @Nullable
    fun getPinChatsPlacementWithId(pinPlacementId: String) = filterByPremiumPlacements.find { it.id == pinPlacementId }
}
