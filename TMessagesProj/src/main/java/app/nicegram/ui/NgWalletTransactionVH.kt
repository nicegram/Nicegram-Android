package app.nicegram.ui

import android.content.Context
import android.view.ViewGroup
import com.appvillis.nicegram_wallet.wallet_inchat.external.TransactionTgMessageView
import org.telegram.ui.Components.RecyclerListView

class NgWalletTransactionVH(val view: TransactionTgMessageView) : RecyclerListView.Holder(view) {
    companion object {
        fun createView(context: Context, parent: ViewGroup): TransactionTgMessageView {
            return TransactionTgMessageView(context)
        }
    }

    fun onBind(text: String, incoming: Boolean) {
        view.setData(text, incoming)
    }
}