package com.example.jarvis

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
import java.net.NetworkInterface
import java.security.SecureRandom

/** Authenticated LAN API. Prototype accepts commands but never runs arbitrary shell code. */
object LocalServer {
 private const val PORT=8765
 private const val PREFS="jarvix_server"
 private const val TOKEN_KEY="access_token"
 private var server:NanoHTTPD?=null
 @Synchronized fun start(context:Context):String {
  if(server?.isAlive==true) return token(context)
  val secret=token(context)
  server=object:NanoHTTPD("0.0.0.0",PORT) {
   override fun serve(session:IHTTPSession):Response {
    if(session.method==Method.GET && session.uri=="/health")
     return newFixedLengthResponse(Response.Status.OK,"application/json",JSONObject().put("service","JARVIX").put("status","online").put("version","0.2.0").toString())
    if(session.method==Method.POST && session.uri=="/v1/command") {
     if(session.headers["authorization"]!="Bearer $secret") return newFixedLengthResponse(Response.Status.UNAUTHORIZED,"application/json","{\"error\":\"unauthorized\"}")
     return try {
      val body=HashMap<String,String>(); session.parseBody(body)
      val command=JSONObject(body["postData"].orEmpty()).optString("command").trim()
      if(command.isBlank() || command.length>500) newFixedLengthResponse(Response.Status.BAD_REQUEST,"application/json","{\"error\":\"command must be 1..500 chars\"}")
      else newFixedLengthResponse(Response.Status.OK,"application/json",JSONObject().put("accepted",true).put("command",command).put("message","Commande reçue; aucune action sensible exécutée automatiquement.").toString())
     } catch(_:Exception) { newFixedLengthResponse(Response.Status.BAD_REQUEST,"application/json","{\"error\":\"invalid_json\"}") }
    } else newFixedLengthResponse(Response.Status.NOT_FOUND,"application/json","{\"error\":\"not_found\"}")
   }
  }
  server!!.start(NanoHTTPD.SOCKET_READ_TIMEOUT,false)
  return secret
 }
 @Synchronized fun stop() { server?.stop(); server=null }
 fun getLanIp():String? = try {
  NetworkInterface.getNetworkInterfaces().toList().flatMap { it.inetAddresses.toList() }
   .firstOrNull { !it.isLoopbackAddress && it.hostAddress?.contains(':')==false }?.hostAddress
 } catch(_:Exception) { null }
 private fun token(context:Context):String {
  val prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
  prefs.getString(TOKEN_KEY,null)?.let { if(it.isNotBlank()) return it }
  val bytes=ByteArray(24); SecureRandom().nextBytes(bytes)
  val generated=bytes.joinToString("") { "%02x".format(it) }
  prefs.edit().putString(TOKEN_KEY,generated).apply()
  return generated
 }
}
