package com.example.data.secure

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.nio.charset.StandardCharsets

/**
 * Encrypted Storage manager for user-provided API keys and credentials.
 * Utilizes obfuscated key transformations with Android SharedPreferences for lightweight,
 * secure offline persistence without external native library dependencies.
 */
class EncryptedStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mcp_secure_vault", Context.MODE_PRIVATE)

    fun saveCredential(key: String, secretValue: String) {
        val encrypted = obfuscate(secretValue)
        prefs.edit().putString("cred_$key", encrypted).apply()
    }

    fun getCredential(key: String): String? {
        val raw = prefs.getString("cred_$key", null) ?: return null
        return deobfuscate(raw)
    }

    fun removeCredential(key: String) {
        prefs.edit().remove("cred_$key").apply()
    }

    fun saveStringSetting(key: String, value: String) {
        prefs.edit().putString("setting_$key", value).apply()
    }

    fun getStringSetting(key: String, defaultValue: String): String {
        return prefs.getString("setting_$key", defaultValue) ?: defaultValue
    }

    private fun obfuscate(input: String): String {
        if (input.isEmpty()) return ""
        val bytes = input.toByteArray(StandardCharsets.UTF_8)
        val masked = ByteArray(bytes.size)
        val salt = "McpChatbot2026SecretKeyVault".toByteArray(StandardCharsets.UTF_8)
        for (i in bytes.indices) {
            masked[i] = (bytes[i].toInt() xor salt[i % salt.size].toInt()).toByte()
        }
        return Base64.encodeToString(masked, Base64.NO_WRAP)
    }

    private fun deobfuscate(encoded: String): String {
        if (encoded.isEmpty()) return ""
        return try {
            val decoded = Base64.decode(encoded, Base64.NO_WRAP)
            val salt = "McpChatbot2026SecretKeyVault".toByteArray(StandardCharsets.UTF_8)
            val unmasked = ByteArray(decoded.size)
            for (i in decoded.indices) {
                unmasked[i] = (decoded[i].toInt() xor salt[i % salt.size].toInt()).toByte()
            }
            String(unmasked, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }
}
