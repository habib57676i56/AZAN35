package com.example.data.firebase

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val OVERRIDE_PREFS = "mihrab_appcheck_override"
private const val OVERRIDE_KEY = "custom_debug_token"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            val initialized = FirebaseApp.initializeApp(context)
            if (initialized == null && FirebaseApp.getApps(context).isEmpty()) {
                Log.w(TAG, "FirebaseApp not initialized (google-services.json missing); skipping App Check.")
                return
            }
        }

        if (BuildConfig.DEBUG) {
            val persistenceKey = FirebaseApp.getInstance().persistenceKey
            val debugPrefs = context.getSharedPreferences(
                "$DEBUG_STORE_PREFIX$persistenceKey",
                Context.MODE_PRIVATE
            )
            val intentDebugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)?.trim()
            val overrideToken = context.getSharedPreferences(OVERRIDE_PREFS, Context.MODE_PRIVATE)
                .getString(OVERRIDE_KEY, null)?.trim()

            val tokenToWrite = when {
                !intentDebugToken.isNullOrEmpty() -> intentDebugToken
                !overrideToken.isNullOrEmpty() -> overrideToken
                else -> null
            }

            if (!tokenToWrite.isNullOrEmpty()) {
                debugPrefs.edit()
                    .putString(DEBUG_SECRET_KEY, tokenToWrite)
                    .commit()
                Log.d(TAG, "App Check debug secret configured successfully.")
            } else {
                Log.w(TAG, "No debug token found in Intent extra or override.")
            }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            // Pre-warm or generate local debug UUID immediately so it can be inspected/copied in Settings
            runCatching {
                FirebaseAppCheck.getInstance().getAppCheckToken(false)
            }
            Log.d(TAG, "DebugAppCheckProviderFactory installed.")
        } else {
            val resId = context.resources.getIdentifier(
                RECAPTCHA_SITE_KEY_RES,
                "string",
                context.packageName
            )
            if (resId != 0) {
                val siteKey = context.getString(resId)
                if (siteKey.isNotBlank()) {
                    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
                } else {
                    Log.e(TAG, "recaptcha_site_key resource string is blank.")
                }
            } else {
                Log.e(TAG, "recaptcha_site_key string resource not found in package.")
            }
        }
    } catch (e: Throwable) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}

fun getAppCheckDebugSecret(context: Context): String? {
    return runCatching {
        if (FirebaseApp.getApps(context).isEmpty()) return null
        val persistenceKey = FirebaseApp.getInstance().persistenceKey
        val debugPrefs = context.getSharedPreferences(
            "$DEBUG_STORE_PREFIX$persistenceKey",
            Context.MODE_PRIVATE
        )
        debugPrefs.getString(DEBUG_SECRET_KEY, null)?.takeIf { it.isNotBlank() }
    }.getOrNull()
}

fun getCustomAppCheckOverride(context: Context): String {
    return context.getSharedPreferences(OVERRIDE_PREFS, Context.MODE_PRIVATE)
        .getString(OVERRIDE_KEY, "") ?: ""
}

fun saveCustomAppCheckDebugToken(context: Context, token: String) {
    val clean = token.trim()
    context.getSharedPreferences(OVERRIDE_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(OVERRIDE_KEY, clean)
        .commit()

    runCatching {
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            val persistenceKey = FirebaseApp.getInstance().persistenceKey
            val debugPrefs = context.getSharedPreferences(
                "$DEBUG_STORE_PREFIX$persistenceKey",
                Context.MODE_PRIVATE
            )
            if (clean.isNotBlank()) {
                debugPrefs.edit().putString(DEBUG_SECRET_KEY, clean).commit()
            } else {
                debugPrefs.edit().remove(DEBUG_SECRET_KEY).commit()
            }
            if (BuildConfig.DEBUG) {
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                FirebaseAppCheck.getInstance().getAppCheckToken(true)
            }
        }
    }
}

fun clearCustomAppCheckDebugToken(context: Context) {
    saveCustomAppCheckDebugToken(context, "")
}
