package app.nicegram

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
        "ArticleByAuthor",
        "SentAppCodeTitle",
        "SentSmsCode",
        "SentAppCode",
        "SentAppCodeWithPhone",
        "TelegramBusiness",
        "TelegramBusinessSubtitleTemp",
        "AuthAnotherClientInfo4",
        "SessionsListInfo",
        "FolderShowTagsInfoPremium",
        "ContactJoined",
        "ShareTelegram",
        "InviteFriendsHelp",
        "InviteText2",
        "TelegramContacts_zero",
        "TelegramContacts_one",
        "TelegramContacts_two",
        "TelegramContacts_few",
        "TelegramContacts_many",
        "TelegramContacts_other",
        "InviteTextNum_zero",
        "InviteTextNum_one",
        "InviteTextNum_two",
        "InviteTextNum_few",
        "InviteTextNum_many",
        "InviteTextNum_other",
        // endregion
    )

    fun String.replaceTgTitle(key: String) = if (forbiddenKeys.contains(key)) this else this.replace("Telegram", "Nicegram")
}