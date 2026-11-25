package com.mj.virtualdress

import android.content.Context

object PrivacyManager {
    private const val PREFS = "vto_privacy"
    private const val CONSENT = "processing_consent"
    private const val LAST_ACTIVE = "last_active"
    private const val SESSION_TIMEOUT_MS = 30 * 60 * 1000L

    fun hasConsent(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(CONSENT, false)

    fun grantConsent(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(CONSENT, true)
            .putLong(LAST_ACTIVE, System.currentTimeMillis())
            .apply()
    }

    fun touch(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(LAST_ACTIVE, System.currentTimeMillis())
            .apply()
    }

    fun shouldExpire(context: Context): Boolean {
        val last = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(LAST_ACTIVE, 0L)
        return last > 0L && System.currentTimeMillis() - last > SESSION_TIMEOUT_MS
    }

    fun clearTemporarySession(context: Context) {
        CustomerSessionStore.clear()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(LAST_ACTIVE)
            .apply()
    }

    fun privacySummary(): String =
        "Photos are processed only to provide virtual try-on. The app does not intentionally keep customer photos permanently. Results are kept in the temporary session until cleared or expired. Saving to Gallery is an explicit user action. Use secure HTTPS for deployed AI servers."
}
