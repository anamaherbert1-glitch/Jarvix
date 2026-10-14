package com.example.jarvis

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.concurrent.thread

/** HTTPS-only AI client. The device token is encrypted with an Android Keystore key. */
object AiBrainClient {
 private const val PREFS = "jarvix_ai_config"
 private const val URL_KEY = "backend_url"
 private const val TOKEN_KEY = "encrypted_device_token"
 private const val KEY_ALIAS = "jarvix_ai_device_token_v1"
 private val main = Handler(Looper.getMainLooper())

 fun backendUrl(context: Context): String =
  context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(URL_KEY, "").orEmpty()

 fun isConfigured(context: Context): Boolean =
  backendUrl(context).startsWith("https://") &&
   context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains(TOKEN_KEY)

 @Synchronized fun save(context: Context, url: String, token: String): String? {
  val normalized = url.trim().trimEnd('/')
  if (!normalized.startsWith("https://")) return "L'adresse du backend doit commencer par https://"
  if (normalized.contains(' ') || normalized.contains('?') || normalized.contains('#')) return "Adresse du backend invalide."
  val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
  if (token.isNotBlank()) {
   if (token.trim().length < 32) return "Le jeton appareil doit contenir au moins 32 caractères."
   try {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, secretKey())
    val encrypted = cipher.doFinal(token.trim().toByteArray(Charsets.UTF_8))
    val packed = cipher.iv + encrypted
    prefs.edit().putString(TOKEN_KEY, Base64.encodeToString(packed, Base64.NO_WRAP)).putString(URL_KEY, normalized).apply()
   } catch (_: Exception) { return "Impossible de sécuriser le jeton sur cet appareil." }
  } else {
   if (!prefs.contains(TOKEN_KEY)) return "Entre le jeton appareil fourni par l'administrateur du backend."
   prefs.edit().putString(URL_KEY, normalized).apply()
  }
  return null
 }

 fun ask(context: Context, message: String, callback: (String) -> Unit) {
  val url = backendUrl(context)
  val token = try { readToken(context) } catch (_: Exception) { null }
  if (!url.startsWith("https://") || token.isNullOrBlank()) {
   main.post { callback("Configure d'abord le cerveau IA avec une URL HTTPS et un jeton appareil.") }
   return
  }
  thread(name = "jarvix-ai-request") {
   var connection: HttpURLConnection? = null
   val reply = try {
    connection = URL("$url/api/ai/chat").openConnection() as HttpURLConnection
    connection.connectTimeout = 10000
    connection.readTimeout = 45000
    connection.requestMethod = "POST"
    connection.doOutput = true
    connection.setRequestProperty("Authorization", "Bearer $token")
    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
    connection.outputStream.use { it.write(JSONObject().put("message", message).put("history", org.json.JSONArray()).toString().toByteArray(Charsets.UTF_8)) }
    val code = connection.responseCode
    val stream = if (code in 200..299) connection.inputStream else connection.errorStream
    val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
    if (code in 200..299) JSONObject(body).optString("reply").ifBlank { "Le cerveau IA n'a pas renvoyé de réponse." }
    else if (code == 401) "Jeton appareil refusé. Vérifie la configuration avec l'administrateur."
    else if (code == 503) "Le cerveau IA n'est pas encore configuré côté serveur."
    else "Erreur du cerveau IA (HTTP $code). Réessaie plus tard."
   } catch (_: Exception) {
    "Connexion au cerveau IA impossible. Vérifie l'URL HTTPS et la connexion Internet."
   } finally { connection?.disconnect() }
   main.post { callback(reply) }
  }
 }

 private fun readToken(context: Context): String? {
  val encoded = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(TOKEN_KEY, null) ?: return null
  val packed = Base64.decode(encoded, Base64.NO_WRAP)
  if (packed.size <= 12) return null
  val cipher = Cipher.getInstance("AES/GCM/NoPadding")
  cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, packed.copyOfRange(0, 12)))
  return String(cipher.doFinal(packed.copyOfRange(12, packed.size)), Charsets.UTF_8)
 }

 private fun secretKey(): SecretKey {
  val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
  (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
  val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
  generator.init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
   .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
   .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
   .setRandomizedEncryptionRequired(true)
   .build())
  return generator.generateKey()
 }
}
