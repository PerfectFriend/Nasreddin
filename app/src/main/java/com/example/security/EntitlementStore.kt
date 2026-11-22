package com.example.security

import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class EntitlementSnapshot(
    val isPremiumUnlocked: Boolean = false,
    val seriesUnlocked: Boolean = false,
    val adUnlockedMaxStory: Int = DEFAULT_FREE_MAX_STORY
) {
    companion object {
        const val DEFAULT_FREE_MAX_STORY = 10
        const val CATALOG_MAX_STORY = 50
    }
}

interface SecretKeyProvider {
    fun getOrCreateKey(): SecretKey?
}

/**
 * Hardware/OS-backed AES-256-GCM key provider using [AndroidKeyStore] on real Android devices
 * (API 24+). In Robolectric JVM tests (where AndroidKeyStore is absent), uses a test-scoped
 * deterministic AES key so the exact AES-GCM authenticated encryption & tamper rejection pipeline
 * runs cleanly without failing silently.
 */
class AndroidKeyStoreSecretKeyProvider(
    private val keyAlias: String = KEY_ALIAS,
    private val packageSalt: String = "com.aistudio.nasreddin.vdqz"
) : SecretKeyProvider {

    override fun getOrCreateKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existing = (keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.secretKey
            if (existing != null) return existing

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        } catch (e: Exception) {
            if (isRobolectricEnvironment()) {
                val digest = MessageDigest.getInstance("SHA-256")
                    .digest("$packageSalt:$keyAlias".toByteArray(Charsets.UTF_8))
                SecretKeySpec(digest, "AES")
            } else {
                Log.e(TAG, "AndroidKeyStore unavailable; failing entitlement keystore closed", e)
                null
            }
        }
    }

    private fun isRobolectricEnvironment(): Boolean {
        return Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
    }

    companion object {
        private const val TAG = "KeyStoreProvider"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "hodja_entitlement_aes256_gcm_v1"
    }
}

/**
 * Encrypted, tamper-evident persistence for Hybrid Monetization entitlements backed by
 * AES-256-GCM authenticated encryption + hardware/OS AndroidKeyStore keys.
 *
 * Security invariants:
 * - Never trusts plain editable boolean flags (`premium_unlocked`, `series_unlocked`) for PRO/Series;
 *   removes legacy plaintext PRO/Series keys upon write and relies on Play Billing startup restore.
 * - Fails closed (`false / false / 10`) on tampered ciphertext, corrupted Base64, or invalid GCM tag
 *   (`AEADBadTagException`) rather than crashing or granting elevated entitlements.
 */
class EntitlementStore(
    private val prefs: SharedPreferences,
    private val keyProvider: SecretKeyProvider = AndroidKeyStoreSecretKeyProvider()
) {
    private val random = SecureRandom()

    fun load(): EntitlementSnapshot {
        val encoded = prefs.getString(KEY_ENCRYPTED_ENTITLEMENTS, null)
        if (encoded.isNullOrBlank()) {
            // Preserve only non-privileged ad-unlocked batch progress from pre-Plan-B installs if valid
            val legacyAdMax = prefs.getInt(LEGACY_KEY_AD_MAX_STORY, EntitlementSnapshot.DEFAULT_FREE_MAX_STORY)
                .coerceIn(EntitlementSnapshot.DEFAULT_FREE_MAX_STORY, EntitlementSnapshot.CATALOG_MAX_STORY)
            val initial = EntitlementSnapshot(
                isPremiumUnlocked = false,
                seriesUnlocked = false,
                adUnlockedMaxStory = legacyAdMax
            )
            if (prefs.contains(LEGACY_KEY_AD_MAX_STORY) ||
                prefs.contains(LEGACY_KEY_PREMIUM) ||
                prefs.contains(LEGACY_KEY_SERIES)
            ) {
                save(initial)
            }
            return initial
        }

        val secretKey = keyProvider.getOrCreateKey() ?: return EntitlementSnapshot()
        return try {
            val parts = encoded.split(PAYLOAD_DELIMITER)
            if (parts.size != 2) {
                Log.w(TAG, "Malformed encrypted entitlement blob; resetting to safe defaults")
                return EntitlementSnapshot()
            }
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherBytes = Base64.decode(parts[1], Base64.NO_WRAP)
            if (iv.size != GCM_IV_BYTES || cipherBytes.size <= GCM_TAG_BITS / 8) {
                Log.w(TAG, "Invalid IV or ciphertext length; resetting to safe defaults")
                return EntitlementSnapshot()
            }

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.updateAAD(AAD_CONTEXT.toByteArray(Charsets.UTF_8))
            val plainBytes = cipher.doFinal(cipherBytes)
            val json = JSONObject(String(plainBytes, Charsets.UTF_8))

            EntitlementSnapshot(
                isPremiumUnlocked = json.optBoolean(FIELD_PREMIUM, false),
                seriesUnlocked = json.optBoolean(FIELD_SERIES, false),
                adUnlockedMaxStory = json.optInt(
                    FIELD_AD_MAX,
                    EntitlementSnapshot.DEFAULT_FREE_MAX_STORY
                ).coerceIn(EntitlementSnapshot.DEFAULT_FREE_MAX_STORY, EntitlementSnapshot.CATALOG_MAX_STORY)
            )
        } catch (e: Exception) {
            Log.w(TAG, "Entitlement integrity verification failed; failing closed", e)
            EntitlementSnapshot()
        }
    }

    fun save(snapshot: EntitlementSnapshot): Boolean {
        val secretKey = keyProvider.getOrCreateKey() ?: return false
        return try {
            val clampedAdMax = snapshot.adUnlockedMaxStory.coerceIn(
                EntitlementSnapshot.DEFAULT_FREE_MAX_STORY,
                EntitlementSnapshot.CATALOG_MAX_STORY
            )
            val payload = JSONObject().apply {
                put(FIELD_PREMIUM, snapshot.isPremiumUnlocked)
                put(FIELD_SERIES, snapshot.seriesUnlocked)
                put(FIELD_AD_MAX, clampedAdMax)
                put(FIELD_UPDATED_AT, System.currentTimeMillis())
            }.toString().toByteArray(Charsets.UTF_8)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv ?: ByteArray(GCM_IV_BYTES).also { random.nextBytes(it) }
            cipher.updateAAD(AAD_CONTEXT.toByteArray(Charsets.UTF_8))
            val cipherBytes = cipher.doFinal(payload)

            val encoded = "${Base64.encodeToString(iv, Base64.NO_WRAP)}$PAYLOAD_DELIMITER${Base64.encodeToString(cipherBytes, Base64.NO_WRAP)}"
            prefs.edit()
                .putString(KEY_ENCRYPTED_ENTITLEMENTS, encoded)
                .remove(LEGACY_KEY_PREMIUM)
                .remove(LEGACY_KEY_SERIES)
                .remove(LEGACY_KEY_AD_MAX_STORY)
                .apply()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt and save entitlements", e)
            false
        }
    }

    companion object {
        private const val TAG = "EntitlementStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_BYTES = 12
        private const val GCM_TAG_BITS = 128
        private const val PAYLOAD_DELIMITER = ":"
        private const val AAD_CONTEXT = "hodja-volume1-entitlements"
        const val KEY_ENCRYPTED_ENTITLEMENTS = "encrypted_entitlements_v1"
        private const val LEGACY_KEY_PREMIUM = "premium_unlocked"
        private const val LEGACY_KEY_SERIES = "series_unlocked"
        private const val LEGACY_KEY_AD_MAX_STORY = "ad_unlocked_max_story"
        private const val FIELD_PREMIUM = "premium"
        private const val FIELD_SERIES = "series"
        private const val FIELD_AD_MAX = "ad_max"
        private const val FIELD_UPDATED_AT = "updated_at"
    }
}
