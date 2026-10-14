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
 private var serverOn = false
 private val voiceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
  if (result.resultCode == Activity.RESULT_OK) {
   val command = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
   if (command.isNotBlank()) handleCommand(command) else update("Je n’ai pas entendu de commande.")
  } else update("Reconnaissance vocale annulée ou indisponible.")
 }
 private val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
  if (uri != null) {
   try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) {}
   update("Fichier sélectionné. Accès limité au fichier choisi."); speak("Fichier sélectionné.")
  } else update("Aucun fichier sélectionné.")
 }
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  tts = TextToSpeech(this, this)
  val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(28,36,28,28); setBackgroundColor(android.graphics.Color.rgb(5,10,20)) }
  root.addView(TextView(this).apply { text="J.A.R.V.I.S"; textSize=30f; setTextColor(android.graphics.Color.rgb(78,190,255)) })
  status = TextView(this).apply { text="SYSTÈME EN ATTENTE\nServeur local arrêté."; textSize=16f; setTextColor(-1); setPadding(0,22,0,22) }
  root.addView(status)
  addButton(root,"🎙 PARLER") { listen() }
  addButton(root,"🖥 DÉMARRER / ARRÊTER LE SERVEUR DU TÉLÉPHONE") { toggleLocalServer() }
  addButton(root,"📂 CHOISIR UN FICHIER") { filePicker.launch(arrayOf("*/*")) }
  addButton(root,"🔔 AUTORISER LES NOTIFICATIONS") { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
  addButton(root,"🔊 LIRE LA DERNIÈRE NOTIFICATION") { readLatestNotification() }
  addButton(root,"💬 OUVRIR WHATSAPP") { openPackage("com.whatsapp","WhatsApp") }
  addButton(root,"🌐 OUVRIR CHROME") { openPackage("com.android.chrome","Chrome") }
  addButton(root,"▶ OUVRIR YOUTUBE") { openPackage("com.google.android.youtube","YouTube") }
  setContentView(ScrollView(this).apply { addView(root) })
 }
 private fun addButton(root: LinearLayout,label: String,action: () -> Unit) {
  root.addView(Button(this).apply { text=label; setOnClickListener { action() } },LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=10 })
 }
 private fun toggleLocalServer() {
  if (!serverOn) try {
   val token=LocalServer.start(this); serverOn=true
   val ip=LocalServer.getLanIp()
   update("SERVEUR DU TÉLÉPHONE ACTIF\nAdresse : ${if(ip==null) "IP Wi-Fi introuvable" else "http://$ip:8765"}\nClé secrète : $token\nGarde la clé privée et laisse JARVIS ouvert.")
  } catch(e: Exception) { update("Démarrage impossible : ${e.message ?: "erreur"}") }
  else { LocalServer.stop(); serverOn=false; update("Serveur du téléphone arrêté.") }
 }
 private fun listen() {
  if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) { requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),41); return }
  if (!SpeechRecognizer.isRecognitionAvailable(this)) { update("Aucun service vocal disponible."); return }
  val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE,"fr-FR"); putExtra(RecognizerIntent.EXTRA_PROMPT,"Parle à JARVIS") }
  try { voiceLauncher.launch(intent); update("Écoute en cours…") } catch(_: ActivityNotFoundException) { update("Service vocal introuvable.") }
 }
 private fun handleCommand(raw:String) {
  val c=raw.lowercase(Locale.FRENCH); update("COMMANDE : $raw")
  when {
   c.contains("whatsapp")->openPackage("com.whatsapp","WhatsApp")
   c.contains("chrome")||c.contains("navigateur")->openPackage("com.android.chrome","Chrome")
   c.contains("youtube")->openPackage("com.google.android.youtube","YouTube")
   c.contains("fichier")||c.contains("document")||c.contains("photo")->filePicker.launch(arrayOf("*/*"))
   c.contains("notification")->readLatestNotification()
   c.contains("serveur")->toggleLocalServer()
   c.contains("heure")->speak("Il est "+SimpleDateFormat("HH:mm",Locale.FRANCE).format(Date()))
   c.contains("date")->speak("Nous sommes le "+SimpleDateFormat("d MMMM yyyy",Locale.FRANCE).format(Date()))
   else->speak("J’ai reconnu : $raw. Le cerveau IA n’est pas encore connecté.")
  }
 }
 private fun readLatestNotification() {
  val latest=JarvisNotificationListener.latest(this)
  if(latest.isBlank()) { update("Aucune notification disponible. Active l’accès dans les paramètres Android."); speak("Aucune notification disponible.") }
  else speak("Dernière notification. "+latest)
 }
 private fun openPackage(packageName:String,appName:String) {
  val launch=packageManager.getLaunchIntentForPackage(packageName)
  if(launch!=null) { startActivity(launch); update("Ouverture de "+appName); speak("J’ouvre "+appName) }
  else { update(appName+" n’est pas installé ou accessible."); speak(appName+" n’est pas disponible.") }
 }
 private fun update(message:String) { status.text=message }
 private fun speak(message:String) { if(ttsReady) tts.speak(message,TextToSpeech.QUEUE_FLUSH,null,"jarvis-utterance"); update(message) }
 override fun onInit(statusCode:Int) { ttsReady=statusCode==TextToSpeech.SUCCESS; if(ttsReady) tts.language=Locale.FRENCH }
 @Deprecated("Deprecated in Android API; retained for compatibility")
 override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray) {
  super.onRequestPermissionsResult(requestCode,permissions,grantResults)
  if(requestCode==41 && grantResults.firstOrNull()==PackageManager.PERMISSION_GRANTED) listen() else if(requestCode==41) update("Permission microphone refusée.")
 }
 override fun onDestroy() { LocalServer.stop(); if(::tts.isInitialized) { tts.stop(); tts.shutdown() }; super.onDestroy() }
}
