#!/usr/bin/env python3
"""JARVIX social OAuth gateway foundation.

OAuth credentials and access tokens stay on this server, never in the Android APK.
This module intentionally does not publish, send messages, or reply to comments yet.
Those actions require provider-specific adapters, reviewed scopes, and explicit approval.
"""
from __future__ import annotations
import base64, hashlib, hmac, json, os, secrets, sqlite3, time
from pathlib import Path
from typing import Any
from urllib.parse import urlencode
import httpx
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from fastapi import FastAPI, Header, HTTPException, Query
from fastapi.responses import HTMLResponse, RedirectResponse
from pydantic import BaseModel, Field

ROOT = Path(__file__).resolve().parent
DB_PATH = Path(os.environ.get("JARVIX_SOCIAL_DB", str(ROOT / "jarvix_social.sqlite3")))
ADMIN_TOKEN = os.environ.get("JARVIX_ADMIN_TOKEN", "")
TOKEN_KEY_B64 = os.environ.get("JARVIX_TOKEN_ENCRYPTION_KEY", "")
PUBLIC_BASE_URL = os.environ.get("JARVIX_PUBLIC_BASE_URL", "").rstrip("/")
META_VERSION = os.environ.get("META_GRAPH_VERSION", "v23.0")

PROVIDERS: dict[str, dict[str, Any]] = {
 "facebook": {"label":"Facebook Pages","kind":"oauth","client_id_env":"FACEBOOK_APP_ID","client_secret_env":"FACEBOOK_APP_SECRET","authorize":f"https://www.facebook.com/{META_VERSION}/dialog/oauth","token":f"https://graph.facebook.com/{META_VERSION}/oauth/access_token","scopes":["pages_show_list","pages_read_engagement","pages_manage_posts","read_insights"],"features":["page_posts","page_comments","page_insights"],"note":"Fonctions Page uniquement selon les permissions et l'examen Meta."},
 "instagram": {"label":"Instagram Professional","kind":"oauth","client_id_env":"INSTAGRAM_APP_ID","client_secret_env":"INSTAGRAM_APP_SECRET","authorize":"https://www.instagram.com/oauth/authorize","token":"https://api.instagram.com/oauth/access_token","scopes":["instagram_business_basic","instagram_business_content_publish","instagram_business_manage_comments","instagram_business_manage_insights"],"features":["professional_media_publish","comments","insights"],"note":"Compte professionnel requis; capacités et scopes dépendent de l'app Meta."},
 "tiktok": {"label":"TikTok","kind":"oauth","client_id_env":"TIKTOK_CLIENT_KEY","client_secret_env":"TIKTOK_CLIENT_SECRET","authorize":"https://www.tiktok.com/v2/auth/authorize/","token":"https://open.tiktokapis.com/v2/oauth/token/","scopes":["user.info.basic","video.list","video.publish","video.upload"],"features":["profile","video_publish_or_upload"],"note":"Publication soumise à l'approbation TikTok, à l'audit et aux règles de publication."},
 "youtube": {"label":"YouTube","kind":"oauth","client_id_env":"GOOGLE_CLIENT_ID","client_secret_env":"GOOGLE_CLIENT_SECRET","authorize":"https://accounts.google.com/o/oauth2/v2/auth","token":"https://oauth2.googleapis.com/token","scopes":["openid","https://www.googleapis.com/auth/youtube.readonly","https://www.googleapis.com/auth/youtube.upload"],"features":["channel_stats","video_upload","comments_read"],"note":"YouTube Data API; certaines opérations nécessitent la validation du projet Google."},
 "x": {"label":"X","kind":"oauth","client_id_env":"X_CLIENT_ID","client_secret_env":"X_CLIENT_SECRET","authorize":"https://twitter.com/i/oauth2/authorize","token":"https://api.x.com/2/oauth2/token","scopes":["users.read","tweet.read","tweet.write","like.read","offline.access"],"features":["posts","profile","some_metrics"],"note":"Accès et prix dépendent du niveau d'accès X; les messages privés nécessitent des droits distincts."},
 "linkedin": {"label":"LinkedIn","kind":"oauth","client_id_env":"LINKEDIN_CLIENT_ID","client_secret_env":"LINKEDIN_CLIENT_SECRET","authorize":"https://www.linkedin.com/oauth/v2/authorization","token":"https://www.linkedin.com/oauth/v2/accessToken","scopes":["openid","profile","w_member_social"],"features":["member_posts"],"note":"Les API de statistiques, commentaires et organisations nécessitent souvent des produits/permissions approuvés."},
 "whatsapp": {"label":"WhatsApp Business Platform","kind":"oauth","client_id_env":"FACEBOOK_APP_ID","client_secret_env":"FACEBOOK_APP_SECRET","authorize":f"https://www.facebook.com/{META_VERSION}/dialog/oauth","token":f"https://graph.facebook.com/{META_VERSION}/oauth/access_token","scopes":["whatsapp_business_management","whatsapp_business_messaging","business_management"],"features":["business_messages"],"note":"API WhatsApp Business uniquement; onboarding Embedded Signup et compte professionnel requis. Pas de compte personnel."},
 "telegram": {"label":"Telegram Bot API","kind":"manual_bot","features":["bot_messages","bot_updates"],"note":"Telegram Bot API n'utilise pas OAuth utilisateur. Crée un bot via @BotFather; cela ne connecte pas ton compte personnel."}
}
app = FastAPI(title="JARVIX Social Connect", version="0.1.0", docs_url=None, redoc_url=None)
from ai_brain import router as ai_router
app.include_router(ai_router)

def db():
 DB_PATH.parent.mkdir(parents=True, exist_ok=True)
 con = sqlite3.connect(DB_PATH)
 con.row_factory = sqlite3.Row
 con.execute("PRAGMA journal_mode=WAL")
 con.executescript("""
 CREATE TABLE IF NOT EXISTS oauth_states(state TEXT PRIMARY KEY, provider TEXT NOT NULL, verifier TEXT, created_at INTEGER NOT NULL);
 CREATE TABLE IF NOT EXISTS social_tokens(provider TEXT PRIMARY KEY, encrypted_token BLOB NOT NULL, token_type TEXT, scope TEXT, expires_at INTEGER, connected_at INTEGER NOT NULL);
 CREATE TABLE IF NOT EXISTS action_drafts(id TEXT PRIMARY KEY, provider TEXT NOT NULL, action TEXT NOT NULL, payload TEXT NOT NULL, status TEXT NOT NULL, created_at INTEGER NOT NULL, confirmed_at INTEGER);
 """)
 return con

def require_admin(authorization: str | None):
 if not ADMIN_TOKEN or len(ADMIN_TOKEN) < 32:
  raise HTTPException(503, "Configure JARVIX_ADMIN_TOKEN (32 caractères minimum).")
 supplied = (authorization or "").removeprefix("Bearer ").strip()
 if not hmac.compare_digest(supplied, ADMIN_TOKEN):
  raise HTTPException(401, "Non autorisé.")

def encryption():
 if not TOKEN_KEY_B64:
  raise HTTPException(503, "Configure JARVIX_TOKEN_ENCRYPTION_KEY.")
 try:
  key = base64.urlsafe_b64decode(TOKEN_KEY_B64 + "=" * (-len(TOKEN_KEY_B64) % 4))
  if len(key) != 32: raise ValueError("invalid key length")
  return AESGCM(key)
 except Exception as exc:
  raise HTTPException(503, "Clé de chiffrement invalide (32 octets base64 URL-safe).") from exc

def redirect_uri(provider: str):
 if not PUBLIC_BASE_URL.startswith("https://"):
  raise HTTPException(503, "JARVIX_PUBLIC_BASE_URL doit être une URL HTTPS publique.")
 return f"{PUBLIC_BASE_URL}/oauth/{provider}/callback"

def provider_credentials(provider: str):
 cfg = PROVIDERS[provider]
 client_id = os.environ.get(cfg.get("client_id_env", ""), "")
 client_secret = os.environ.get(cfg.get("client_secret_env", ""), "")
 if not client_id or not client_secret:
  raise HTTPException(503, f"Identifiants serveur manquants pour {provider}.")
 return client_id, client_secret

def pkce_pair():
 verifier = secrets.token_urlsafe(64)
 challenge = base64.urlsafe_b64encode(hashlib.sha256(verifier.encode()).digest()).rstrip(b"=").decode()
 return verifier, challenge

@app.get("/health")
def health(): return {"service":"JARVIX Social Connect","status":"online"}

@app.get("/api/social/providers")
def list_providers(authorization: str | None = Header(default=None)):
 require_admin(authorization)
 return {"providers":[{"id":k,"name":v["label"],"kind":v["kind"],"features":v.get("features",[]),"note":v.get("note","")} for k,v in PROVIDERS.items()]}

@app.get("/oauth/{provider}/start")
def oauth_start(provider: str, authorization: str | None = Header(default=None)):
 require_admin(authorization)
 if provider not in PROVIDERS: raise HTTPException(404,"Réseau non pris en charge.")
 cfg = PROVIDERS[provider]
 if cfg["kind"] != "oauth": raise HTTPException(400,"Ce réseau utilise une connexion de bot, pas OAuth utilisateur.")
 client_id, _ = provider_credentials(provider)
 state = secrets.token_urlsafe(32)
 verifier, challenge = pkce_pair()
 with db() as con:
  con.execute("INSERT INTO oauth_states(state,provider,verifier,created_at) VALUES(?,?,?,?)",(state,provider,verifier,int(time.time())))
  con.execute("DELETE FROM oauth_states WHERE created_at < ?",(int(time.time())-600,))
 params = {"client_id":client_id,"redirect_uri":redirect_uri(provider),"response_type":"code","state":state}
 if provider == "tiktok":
  params["client_key"] = client_id
  params["scope"] = ",".join(cfg["scopes"])
  params["code_challenge"] = challenge
  params["code_challenge_method"] = "S256"
 else:
  params["scope"] = " ".join(cfg["scopes"])
  if provider in {"x","youtube","linkedin"}:
   params["code_challenge"] = challenge
   params["code_challenge_method"] = "S256"
  if provider == "youtube":
   params.update({"access_type":"offline","include_granted_scopes":"true","prompt":"consent"})
  if provider == "instagram":
   params.update({"enable_fb_login":"0","force_authentication":"1"})
 return RedirectResponse(cfg["authorize"] + "?" + urlencode(params),status_code=302)

@app.post("/api/oauth/{provider}/start")
def oauth_start_url(provider: str, authorization: str | None = Header(default=None)):
 # Browser admin UIs call this endpoint with the bearer header, then open authorization_url.
 require_admin(authorization)
 if provider not in PROVIDERS: raise HTTPException(404,"Réseau non pris en charge.")
 cfg = PROVIDERS[provider]
 if cfg["kind"] != "oauth": raise HTTPException(400,"Ce réseau utilise une connexion de bot, pas OAuth utilisateur.")
 client_id, _ = provider_credentials(provider)
 state = secrets.token_urlsafe(32)
 verifier, challenge = pkce_pair()
 with db() as con:
  con.execute("INSERT INTO oauth_states(state,provider,verifier,created_at) VALUES(?,?,?,?)",(state,provider,verifier,int(time.time())))
  con.execute("DELETE FROM oauth_states WHERE created_at < ?",(int(time.time())-600,))
 params = {"client_id":client_id,"redirect_uri":redirect_uri(provider),"response_type":"code","state":state}
 if provider == "tiktok":
  params.update({"client_key":client_id,"scope":",".join(cfg["scopes"]),"code_challenge":challenge,"code_challenge_method":"S256"})
 else:
  params["scope"] = " ".join(cfg["scopes"])
  if provider in {"x","youtube","linkedin"}:
   params.update({"code_challenge":challenge,"code_challenge_method":"S256"})
  if provider == "youtube": params.update({"access_type":"offline","include_granted_scopes":"true","prompt":"consent"})
  if provider == "instagram": params.update({"enable_fb_login":"0","force_authentication":"1"})
 return {"provider":provider,"authorization_url":cfg["authorize"] + "?" + urlencode(params),"expires_in_seconds":600}

@app.get("/oauth/{provider}/callback",response_class=HTMLResponse)
async def oauth_callback(provider: str, code: str = Query(default=""), state: str = Query(default=""), error: str = Query(default=""), error_description: str = Query(default="")):
 if provider not in PROVIDERS or PROVIDERS[provider]["kind"] != "oauth": raise HTTPException(404,"Réseau non pris en charge.")
 if error: return HTMLResponse("<h2>Connexion annulée</h2><p>Reviens dans JARVIX et réessaie si nécessaire.</p>",status_code=400)
 if not code or not state: raise HTTPException(400,"Code OAuth ou state absent.")
 with db() as con:
  row = con.execute("SELECT * FROM oauth_states WHERE state=? AND provider=?",(state,provider)).fetchone()
  con.execute("DELETE FROM oauth_states WHERE state=?",(state,))
 if not row or int(time.time())-row["created_at"] > 600: raise HTTPException(400,"État OAuth invalide ou expiré. Recommence.")
 cfg = PROVIDERS[provider]
 client_id, client_secret = provider_credentials(provider)
 uri = redirect_uri(provider)
 body = {"grant_type":"authorization_code","code":code,"redirect_uri":uri}
 headers = {"Accept":"application/json"}
 if provider in {"facebook","whatsapp"}:
  async with httpx.AsyncClient(timeout=20) as client:
   response = await client.get(cfg["token"],params={"client_id":client_id,"client_secret":client_secret,"redirect_uri":uri,"code":code},headers=headers)
   if response.status_code >= 400: raise HTTPException(502,"Échange OAuth refusé; vérifie les scopes et l'URL de redirection.")
   token_data = response.json()
 elif provider == "instagram":
  body.update({"client_id":client_id,"client_secret":client_secret})
  async with httpx.AsyncClient(timeout=20) as client:
   response = await client.post(cfg["token"],data=body,headers=headers)
   if response.status_code >= 400: raise HTTPException(502,"Échange OAuth Instagram refusé; vérifie les identifiants et redirect URI.")
   token_data = response.json()
 else:
  if provider == "tiktok": body.update({"client_key":client_id,"client_secret":client_secret,"code_verifier":row["verifier"]})
  else:
   body.update({"client_id":client_id,"client_secret":client_secret})
   if provider in {"x","youtube","linkedin"}: body["code_verifier"] = row["verifier"]
  async with httpx.AsyncClient(timeout=20) as client:
   response = await client.post(cfg["token"],data=body,headers=headers)
   if response.status_code >= 400: raise HTTPException(502,f"Échange OAuth refusé par {cfg['label']}; vérifie scopes, redirect URI et identifiants.")
   token_data = response.json()
 return _store_token(provider,token_data)

def _store_token(provider: str, token_data: dict[str, Any]):
 access_token = token_data.get("access_token")
 if not access_token: raise HTTPException(502,"Le fournisseur n'a pas renvoyé de jeton d'accès.")
 cipher = encryption()
 nonce = secrets.token_bytes(12)
 # Encrypt the complete token response so a refresh token, when supplied, is not discarded.
 secret_bundle = json.dumps({k:token_data[k] for k in ("access_token","refresh_token","token_type","scope","expires_in") if k in token_data},separators=(",",":")).encode()
 encrypted = nonce + cipher.encrypt(nonce,secret_bundle,provider.encode())
 expires = token_data.get("expires_in")
 with db() as con:
  con.execute("""INSERT INTO social_tokens(provider,encrypted_token,token_type,scope,expires_at,connected_at)
  VALUES(?,?,?,?,?,?) ON CONFLICT(provider) DO UPDATE SET encrypted_token=excluded.encrypted_token,
  token_type=excluded.token_type,scope=excluded.scope,expires_at=excluded.expires_at,connected_at=excluded.connected_at""",
  (provider,encrypted,token_data.get("token_type","Bearer"),str(token_data.get("scope","")),int(time.time())+int(expires) if expires else None,int(time.time())))
 return HTMLResponse("<h2>Autorisation reçue</h2><p>Jeton enregistré chiffré côté serveur. Vérifie l'état depuis l'interface autorisée.</p><p>Tu peux fermer cette fenêtre.</p>")

@app.get("/api/social/status")
def social_status(authorization: str | None = Header(default=None)):
 require_admin(authorization)
 with db() as con: rows = {r["provider"]:r for r in con.execute("SELECT provider,scope,expires_at,connected_at FROM social_tokens")}
 result=[]
 for provider,cfg in PROVIDERS.items():
  row=rows.get(provider)
  result.append({"id":provider,"name":cfg["label"],"kind":cfg["kind"],"connected":bool(row),"scopes":row["scope"] if row else None,"expires_at":row["expires_at"] if row else None,"connected_at":row["connected_at"] if row else None,"note":cfg.get("note","")})
 return {"providers":result}

class DraftRequest(BaseModel):
 provider: str
 action: str = Field(min_length=1,max_length=40)
 payload: dict[str,Any]

@app.post("/api/actions/draft")
def create_action_draft(request: DraftRequest, authorization: str | None = Header(default=None)):
 require_admin(authorization)
 if request.provider not in PROVIDERS: raise HTTPException(400,"Fournisseur inconnu.")
 if request.action not in {"publish_post","publish_video","reply_comment","send_message"}: raise HTTPException(400,"Action non autorisée.")
 draft_id=secrets.token_urlsafe(18)
 with db() as con:
  con.execute("INSERT INTO action_drafts(id,provider,action,payload,status,created_at) VALUES(?,?,?,?,?,?)",(draft_id,request.provider,request.action,json.dumps(request.payload,ensure_ascii=False),"awaiting_confirmation",int(time.time())))
 return {"id":draft_id,"status":"awaiting_confirmation","message":"Brouillon créé. Rien n'a été envoyé."}

@app.post("/api/actions/{draft_id}/confirm")
def confirm_action(draft_id: str, authorization: str | None = Header(default=None)):
 require_admin(authorization)
 with db() as con:
  row=con.execute("SELECT id,status FROM action_drafts WHERE id=?",(draft_id,)).fetchone()
  if not row: raise HTTPException(404,"Brouillon introuvable.")
  if row["status"]!="awaiting_confirmation": raise HTTPException(409,"Ce brouillon a déjà été traité.")
  con.execute("UPDATE action_drafts SET status='confirmed_not_executed',confirmed_at=? WHERE id=?",(int(time.time()),draft_id))
 return {"id":draft_id,"status":"confirmed_not_executed","message":"Confirmation enregistrée; adaptateur fournisseur non activé, aucune action exécutée."}

@app.delete("/api/social/{provider}")
def disconnect_provider(provider: str, authorization: str | None = Header(default=None)):
 require_admin(authorization)
 if provider not in PROVIDERS: raise HTTPException(404,"Réseau inconnu.")
 with db() as con: con.execute("DELETE FROM social_tokens WHERE provider=?",(provider,))
 return {"provider":provider,"connected":False}
