package app.nicegram.bridge

import com.appvillis.nicegram_wallet.wallet_contacts.domain.ContactsRetriever
import com.appvillis.nicegram_wallet.wallet_contacts.domain.WalletContact
import com.google.android.exoplayer2.util.Log
import org.telegram.messenger.AccountInstance
import org.telegram.messenger.ContactsController
import org.telegram.messenger.ImageLocation
import org.telegram.messenger.MediaDataController
import org.telegram.messenger.MessagesController
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.TLRPC
import org.telegram.ui.Components.AvatarDrawable
import java.io.File

class ContactsRetrieverImpl : ContactsRetriever {

    override suspend fun getContacts(): List<WalletContact> {
        Log.d("ContactsRetriever", "retrieving contacts...")
        val currentAccount = UserConfig.selectedAccount
        val fileLoader = AccountInstance.getInstance(currentAccount).fileLoader
        val selfId = UserConfig.getInstance(currentAccount).clientUserId
        val topContacts = MediaDataController.getInstance(currentAccount).hints
        Log.d("ContactsRetriever", "top contacts: ${topContacts.size}")
        Log.d("ContactsRetriever", "contacts: ${ContactsController.getInstance(currentAccount).contacts.size}")

        val result = ContactsController.getInstance(currentAccount).contacts
            .sortByTopContacts(topContacts.map { it.peer.user_id })
            .map { MessagesController.getInstance(currentAccount).getUser(it.user_id) }
            .mapNotNull {
                if (it.id == selfId) return@mapNotNull null
                var attachPath: File? = null
                if (it.photo != null) {
                    val imageLocation = ImageLocation.getForUserOrChat(it, ImageLocation.TYPE_SMALL)
                    attachPath = fileLoader.getPathToAttach(it.photo.photo_small, true)
                    if (!attachPath.exists()) fileLoader.loadFile(imageLocation, it, "jpg", 1, 1)

                }
                val img = attachPath?.toString() ?: ""
                val strBuilder = StringBuilder()
                AvatarDrawable.getAvatarSymbols(it.first_name, it.last_name, null, strBuilder)
                val avatarSymbols = strBuilder.toString()

                WalletContact.create(
                    id = it.id.toString(),
                    firstName = it.first_name,
                    lastName = it.last_name,
                    username = it.username,
                    img = img,
                    avatarSymbols = avatarSymbols
                )
            }

        Log.d("ContactsRetriever", "result: ${result.size}")
        Log.d("ContactsRetriever", "result: $result")
        return result
    }

    private fun List<TLRPC.TL_contact>.sortByTopContacts(topContactsInOrder: List<Long>): List<TLRPC.TL_contact> {
        val orderById = topContactsInOrder.withIndex().associate { (index, it) -> it to index }
        return this.sortedWith(compareBy(nullsLast()) { orderById[it.user_id] })
    }

}
