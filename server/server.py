#!/usr/bin/env python3
"""JARVIX local computer server. Standard library only; never expose directly to Internet."""
import hmac, json, os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

HOST=os.environ.get("JARVIX_HOST","127.0.0.1")
PORT=int(os.environ.get("JARVIX_PORT","8765"))
TOKEN=os.environ.get("JARVIX_TOKEN","")

class Handler(BaseHTTPRequestHandler):
 def send_json(self,status,data):
  payload=json.dumps(data,ensure_ascii=False).encode()
  self.send_response(status); self.send_header("Content-Type","application/json; charset=utf-8")
  self.send_header("Content-Length",str(len(payload))); self.send_header("Cache-Control","no-store")
  self.end_headers(); self.wfile.write(payload)
 def do_GET(self):
  if self.path=="/health": return self.send_json(200,{"service":"JARVIX","status":"online","version":"0.2.0"})
  return self.send_json(404,{"error":"not_found"})
 def do_POST(self):
  if self.path!="/v1/command": return self.send_json(404,{"error":"not_found"})
  if not TOKEN or not hmac.compare_digest(self.headers.get("Authorization",""),"Bearer "+TOKEN):
   return self.send_json(401,{"error":"unauthorized"})
  try:
   size=int(self.headers.get("Content-Length","0"))
   if size<1 or size>16384: return self.send_json(413,{"error":"invalid_body_size"})
   command=str(json.loads(self.rfile.read(size)).get("command","")).strip()
   if not command or len(command)>500: return self.send_json(400,{"error":"command must be 1..500 chars"})
   return self.send_json(200,{"accepted":True,"command":command,"message":"Received; no sensitive action executed automatically."})
  except (ValueError,TypeError): return self.send_json(400,{"error":"invalid_json"})
 def log_message(self,fmt,*args): print("[JARVIX]",fmt%args)

if __name__=="__main__":
 if len(TOKEN)<24: raise SystemExit("Set JARVIX_TOKEN to a secret of at least 24 characters.")
 print(f"JARVIX listening on {HOST}:{PORT}; keep the token private.")
 ThreadingHTTPServer((HOST,PORT),Handler).serve_forever()
