package com.example.timetable

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.mindrot.jbcrypt.BCrypt

// хранит хеш пин-кода в зашифрованном файле.
// шифрование через androidx.security, хеш через bcrypt cost=10.
object PinManager {

    private const val FILE = "pin_prefs"
    private const val KEY_HASH = "pin_hash"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        val mk = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            context.applicationContext,
            FILE,
            mk,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun isPinSet(): Boolean = prefs?.getString(KEY_HASH, null) != null

    fun setPin(raw: String) {
        val hash = BCrypt.hashpw(raw, BCrypt.gensalt(10))
        prefs?.edit()?.putString(KEY_HASH, hash)?.apply()
    }

    fun verifyPin(raw: String): Boolean {
        val hash = prefs?.getString(KEY_HASH, null) ?: return false
        return try {
            BCrypt.checkpw(raw, hash)
        } catch (_: Exception) {
            false
        }
    }

    fun clear() {
        prefs?.edit()?.remove(KEY_HASH)?.apply()
    }
}
