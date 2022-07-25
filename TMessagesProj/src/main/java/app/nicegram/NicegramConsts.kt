package app.nicegram

import org.telegram.PhoneFormat.PhoneFormat

object NicegramConsts {
    const val PRIVACY_POLICY_URL = "https://appvillis.com/nicegram-privacy.html"
    const val UNBLOCK_URL = "https://my.nicegram.app/#/"

    private const val TEST_BACKEND_NUMBER = "99966241"
    private const val TEST_BACKEND_START = "7999"
    private const val TEST_BACKEND_REPLACE = "999"

    fun String.isTestNumber() = PhoneFormat.stripExceptNumbers(this).contains(TEST_BACKEND_NUMBER)
    fun String.replaceTestCode() =
        PhoneFormat.stripExceptNumbers(this).replace(TEST_BACKEND_START, TEST_BACKEND_REPLACE)
}