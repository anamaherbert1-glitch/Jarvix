package com.example.jarvis

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Button
import android.widget.EditText
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
 private lateinit var bubble: LivingBubbleView
 private lateinit var bubbleLabel: TextView
 private lateinit var tts: TextToSpeech
 private var ttsReady = false
 private var serverOn = false
 private val voiceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
  if (result.resultCode == Activity.RESULT_OK) {
   val command = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
   if (command.isNotBlank()) handleCommand(command) else update("Je n’ai pas entendu de commande.")
  } else update("Reconnaissance vocale annulée ou indisponible.")
 }
 private val microphonePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
  if (granted) listen() else update("Permission microphone refusée.")
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
  bubble = LivingBubbleView(this)
  root.addView(bubble, LinearLayout.LayoutParams(-1, dp(240)))
  bubbleLabel = TextView(this).apply {
   text = "EN ATTENTE • TOUCHE PARLER"
   textSize = 12f
   gravity = android.view.Gravity.CENTER
   setTextColor(android.graphics.Color.rgb(125,190,255))
   setPadding(0, 0, 0, dp(12))
  }
  root.addView(bubbleLabel)
  status = TextView(this).apply { text="SYSTÈME EN ATTENTE\nServeur local arrêté."; textSize=16f; setTextColor(-1); setPadding(0,22,0,22) }
  root.addView(status)
  addButton(root,"🎙 PARLER") { listen() }
  addButton(root,"🧠 CONFIGURER LE CERVEAU IA") { configureAiBrain() }
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
  if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) { microphonePermission.launch(Manifest.permission.RECORD_AUDIO); return }
  if (!SpeechRecognizer.isRecognitionAvailable(this)) { update("Aucun service vocal disponible."); return }
  val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE,"fr-FR"); putExtra(RecognizerIntent.EXTRA_PROMPT,"Parle à JARVIS") }
  try { voiceLauncher.launch(intent); setBubbleMood(LivingBubbleView.Mood.LISTENING, "JE T’ÉCOUTE"); update("Écoute en cours…") } catch(_: ActivityNotFoundException) { update("Service vocal introuvable.") }
 }
 private fun handleCommand(raw:String) {
  val c=raw.lowercase(Locale.FRENCH); reactToText(c); update("COMMANDE : $raw")
  when {
   c.contains("whatsapp")->openPackage("com.whatsapp","WhatsApp")
   c.contains("chrome")||c.contains("navigateur")->openPackage("com.android.chrome","Chrome")
   c.contains("youtube")->openPackage("com.google.android.youtube","YouTube")
   c.contains("fichier")||c.contains("document")||c.contains("photo")->filePicker.launch(arrayOf("*/*"))
   c.contains("notification")->readLatestNotification()
   c.contains("serveur")->toggleLocalServer()
   c.contains("heure")->speak("Il est "+SimpleDateFormat("HH:mm",Locale.FRANCE).format(Date()))
   c.contains("date")->speak("Nous sommes le "+SimpleDateFormat("d MMMM yyyy",Locale.FRANCE).format(Date()))
   else->askAi(raw)
  }
 }
 private fun configureAiBrain() {
  val layout = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(40,16,40,0) }
  val urlInput = EditText(this).apply {
   hint = "URL HTTPS du backend"
   setSingleLine(true)
   inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
   setText(AiBrainClient.backendUrl(this@MainActivity))
  }
  val tokenInput = EditText(this).apply {
   hint = "Jeton appareil (32 caractères minimum)"
   setSingleLine(true)
   inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
  }
  layout.addView(urlInput)
  layout.addView(tokenInput)
  val dialog = AlertDialog.Builder(this)
   .setTitle("Connexion au cerveau IA")
   .setMessage("Utilise une URL HTTPS et un jeton appareil dédié fourni par l’administrateur. N’entre pas le jeton administrateur.")
   .setView(layout)
   .setNegativeButton("Annuler", null)
   .setPositiveButton("Enregistrer", null)
   .create()
  dialog.setOnShowListener {
   dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
    val error = AiBrainClient.save(this, urlInput.text.toString(), tokenInput.text.toString())
    if (error != null) { update(error) }
    else { update("Configuration IA enregistrée sur cet appareil."); speak("Le cerveau IA est configuré."); dialog.dismiss() }
   }
  }
  dialog.show()
 }

 private fun askAi(message: String) {
  if (!AiBrainClient.isConfigured(this)) {
   update("Le cerveau IA n’est pas configuré. Appuie sur CONFIGURER LE CERVEAU IA.")
   speak("Configure d’abord le cerveau IA.")
   return
  }
  setBubbleMood(LivingBubbleView.Mood.THINKING, "JE RÉFLÉCHIS")
  update("JARVIX réfléchit…")
  AiBrainClient.ask(this, message) { reply ->
   update(reply)
   if (ttsReady) tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "jarvix-ai-reply")
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
 private fun reactToText(text: String) {
  when {
   listOf("merci", "super", "génial", "genial", "bravo", "content", "heureux", "parfait").any { text.contains(it) } ->
    setBubbleMood(LivingBubbleView.Mood.HAPPY, "JE RESSENS UN TON POSITIF")
   listOf("triste", "peur", "stress", "inquiet", "inquiète", "aide-moi", "aide moi", "problème", "probleme", "fatigué", "fatigue").any { text.contains(it) } ->
    setBubbleMood(LivingBubbleView.Mood.CONCERNED, "JE T’ÉCOUTE AVEC ATTENTION")
   listOf("calme", "doucement", "tranquille", "respire").any { text.contains(it) } ->
    setBubbleMood(LivingBubbleView.Mood.CALM, "MODE CALME")
   else -> setBubbleMood(LivingBubbleView.Mood.THINKING, "J’ANALYSE TA DEMANDE")
  }
 }

 private fun setBubbleMood(mood: LivingBubbleView.Mood, label: String) {
  if (::bubble.isInitialized) bubble.setMood(mood)
  if (::bubbleLabel.isInitialized) bubbleLabel.text = label
 }

 private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

 private fun update(message:String) { status.text=message }
 private fun speak(message:String) {
  setBubbleMood(LivingBubbleView.Mood.SPEAKING, "JE TE RÉPONDS")
  if(ttsReady) tts.speak(message,TextToSpeech.QUEUE_FLUSH,null,"jarvis-utterance")
  update(message)
 }
 override fun onInit(statusCode:Int) {
  ttsReady=statusCode==TextToSpeech.SUCCESS
  if(ttsReady) {
   tts.language=Locale.FRENCH
   tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
    override fun onStart(utteranceId: String?) { runOnUiThread { setBubbleMood(LivingBubbleView.Mood.SPEAKING, "JE TE RÉPONDS") } }
    override fun onDone(utteranceId: String?) { runOnUiThread { setBubbleMood(LivingBubbleView.Mood.IDLE, "EN ATTENTE • TOUCHE PARLER") } }
    @Deprecated("Deprecated in Java")
    override fun onError(utteranceId: String?) { runOnUiThread { setBubbleMood(LivingBubbleView.Mood.IDLE, "JE SUIS LÀ") } }
   })
  }
 }
 override fun onDestroy() { LocalServer.stop(); if(::tts.isInitialized) { tts.stop(); tts.shutdown() }; super.onDestroy() }
}
