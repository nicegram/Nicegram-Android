package app.nicegram

import com.google.android.exoplayer2.util.Log
import org.telegram.messenger.LocaleController.LocaleInfo

object NicegramLocales {
    val locales = listOf(
        LocaleInfo().apply {
            name = "简体中文 (聪聪)"
            nameEnglish = "Chinese (Simplified) @congcong"
            shortName = "zh_hans_raw"
            pluralLangCode = "zh"
            pathToFile = "unofficial"
            builtIn = false
        },
        LocaleInfo().apply {
            name = "正體中文"
            nameEnglish = "Chinese (zh-Hant-TW)"
            shortName = "zh_hant_raw"
            pluralLangCode = "zh"
            pathToFile = "unofficial"
            builtIn = false
        }
    )

    private val forbiddenKeys = listOf(
        "TelegramFeatures",
        "TelegramFeaturesUrl",
        "NicegramDialogPolicyText",
        // region Telegram premium
        "UnlockPremiumReactionsDescription",
        "LimitReachedPublicLinks",
        "LimitReachedFolders",
        "LimitReachedChatInFolders",
        "LimitReachedCommunities",
        "SubscribeToPremiumOfficialAppNeededDescription",
        "TelegramPremium",
        "TelegramPremiumSubtitle",
        "TelegramPremiumSubscribedSubtitle",
        "LimitReachedFileSize",
        "AboutPremiumTitle",
        "AboutPremiumDescription",
        "AboutPremiumDescription2",
        "LimitReachedAccounts",
        "TelegramPremiumUserDialogTitle",
        "AboutTelegramPremium",
        "AdditionalReactionsDescription",
        "PremiumPreviewAppIconDescription2",
        "UnlockPremiumStickersDescription",
        "LimitReachedPinDialogs",
        "TelegramPremiumUserDialogSubtitle",
        "PremiumPreviewNoAdsDescription2",
        "StoriesPremiumHint",
        "TelegramFAQ",
        "TelegramFaq",
        "ArticleByAuthor"
        // endregion
    )

    fun String.replaceTgTitle(key: String) = if (forbiddenKeys.contains(key)) this else this.replace("Telegram", "Nicegram")
}