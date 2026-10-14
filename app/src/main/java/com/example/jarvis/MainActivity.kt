package com.example.jarvis

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
 private lateinit var status: TextView
 private lateinit var tts: TextToSpeech
 private var ttsReady = false
 private val voiceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
  if (result.resultCode == Activity.RESULT_OK) {
   val words = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
   val command = words?.firstOrNull().orEmpty()
   if (command.isNotBlank()) handleCommand(command) else update("Je n’ai pas entendu de commande.")
  } else update("Reconnaissance vocale annulée ou indisponible.")
 }
 private val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
  if (uri != null) {
   try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) { }
   update("Fichier sélectionné. L’accès est limité au fichier choisi.")
   speak("Fichier sélectionné.")
  } else update("Aucun fichier sélectionné.")
 }
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  tts = TextToSpeech(this, this)
  val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28, 36, 28, 28); setBackgroundColor(android.graphics.Color.rgb(5,10,20)) }
  root.addView(TextView(this).apply { text = "J.A.R.V.I.S"; textSize = 30f; setTextColor(android.graphics.Color.rgb(78,190,255)) })
  status = TextView(this).apply { text = "SYSTÈME EN ATTENTE\nAppuie sur PARLER."; textSize = 16f; setTextColor(-1); setPadding(0,22,0,22) }
  root.addView(status)
  addButton(root, "🎙 PARLER") { listen() }
  addButton(root, "📂 CHOISIR UN FICHIER") { filePicker.launch(arrayOf("*/*")) }
  addButton(root, "🔔 AUTORISER LES NOTIFICATIONS") { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
  addButton(root, "🔊 LIRE LA DERNIÈRE NOTIFICATION") { readLatestNotification() }
  addButton(root, "💬 OUVRIR WHATSAPP") { openPackage("com.whatsapp", "WhatsApp") }
  addButton(root, "🌐 OUVRIR CHROME") { openPackage("com.android.chrome", "Chrome") }
  addButton(root, "▶ OUVRIR YOUTUBE") { openPackage("com.google.android.youtube", "YouTube") }
  setContentView(ScrollView(this).apply { addView(root) })
 }
 private fun addButton(root: LinearLayout, label: String, action: () -> Unit) {
  root.addView(Button(this).apply { text = label; setOnClickListener { action() } }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 })
 }
 private fun listen() {
  if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 41); return }
  if (!SpeechRecognizer.isRecognitionAvailable(this)) { update("Aucun service vocal disponible."); return }
  val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR"); putExtra(RecognizerIntent.EXTRA_PROMPT, "Parle à JARVIS") }
  try { voiceLauncher.launch(intent); update("Écoute en cours…") } catch (_: ActivityNotFoundException) { update("Service vocal introuvable.") }
 }
 private fun handleCommand(raw: String) {
  val command = raw.lowercase(Locale.FRENCH)
  update("COMMANDE : " + raw)
  when {
   command.contains("whatsapp") -> openPackage("com.whatsapp", "WhatsApp")
   command.contains("chrome") || command.contains("navigateur") -> openPackage("com.android.chrome", "Chrome")
   command.contains("youtube") -> openPackage("com.google.android.youtube", "YouTube")
   command.contains("fichier") || command.contains("document") || command.contains("photo") -> filePicker.launch(arrayOf("*/*"))
   command.contains("notification") -> readLatestNotification()
   command.contains("heure") -> speak("Il est " + SimpleDateFormat("HH:mm", Locale.FRANCE).format(Date()))
   command.contains("date") -> speak("Nous sommes le " + SimpleDateFormat("d MMMM yyyy", Locale.FRANCE).format(Date()))
   command.contains("aide") -> speak("Tu peux me demander d’ouvrir WhatsApp, Chrome ou YouTube, de choisir un fichier ou de lire la dernière notification après autorisation.")
   else -> speak("J’ai reconnu : " + raw + ". Mon cerveau IA n’est pas encore connecté.")
  }
 }
 private fun readLatestNotification() {
  val latest = JarvisNotificationListener.latest(this)
  if (latest.isBlank()) {
   update("Aucune notification disponible. Active l’accès dans les paramètres Android et attends une nouvelle notification.")
   speak("Aucune notification disponible.")
  } else speak("Dernière notification. " + latest)
 }
 private fun openPackage(packageName: String, appName: String) {
  val launch = packageManager.getLaunchIntentForPackage(packageName)
  if (launch != null) { startActivity(launch); update("Ouverture de " + appName); speak("J’ouvre " + appName) }
  else { update(appName + " n’est pas installé ou accessible."); speak(appName + " n’est pas disponible.") }
 }
 private fun update(message: String) { status.text = message }
 private fun speak(message: String) { if (ttsReady) tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "jarvis-utterance"); update(message) }
 override fun onInit(statusCode: Int) { ttsReady = statusCode == TextToSpeech.SUCCESS; if (ttsReady) tts.language = Locale.FRENCH }
 @Deprecated("Deprecated in Android API; retained for compatibility")
 override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
  super.onRequestPermissionsResult(requestCode, permissions, grantResults)
  if (requestCode == 41 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) listen() else if (requestCode == 41) update("Permission microphone refusée.")
 }
 override fun onDestroy() { if (::tts.isInitialized) { tts.stop(); tts.shutdown() }; super.onDestroy() }
}
