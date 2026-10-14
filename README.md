# JARVIX Android + local server

Prototype Android natif : commandes vocales, réponses parlées, ouverture de quelques applications, sélection de fichiers et notifications après autorisation.

## Téléphone comme serveur
1. Compile l’APK via GitHub Actions et installe-le.
2. Mets les appareils sur le même Wi-Fi.
3. Dans JARVIX, touche « DÉMARRER / ARRÊTER LE SERVEUR DU TÉLÉPHONE ».
4. L’application affiche l’adresse locale et une clé Bearer. Garde cette clé privée.
5. API : `GET /health`; `POST /v1/command` avec `Authorization: Bearer <clé>` et JSON `{"command":"..." }`.

Le serveur Android est un prototype et ne fonctionne que tant que le processus de l’application reste actif. Android peut suspendre l’application en arrière-plan.

## Ordinateur comme serveur
Le fichier `server/server.py` utilise Python standard. Installe Python 3 et définis une clé de 24 caractères minimum.
Linux/macOS:
```bash
export JARVIX_TOKEN='remplace-par-une-cle-secrete-longue'
python3 server/server.py
```
Windows PowerShell:
```powershell
$env:JARVIX_TOKEN='remplace-par-une-cle-secrete-longue'
python server/server.py
```
Par défaut il écoute uniquement sur `127.0.0.1`. Pour le réseau local de confiance, définis aussi `JARVIX_HOST=0.0.0.0` et limite les connexions avec le pare-feu. Ne l’expose pas directement à Internet.

## Limites
- Le serveur ordinateur et l’application Android ne sont pas encore reliés automatiquement entre eux; cette version pose les deux points de départ API.
- L’API reçoit les commandes mais n’exécute aucune commande système arbitraire.
- Microphone, fichiers et notifications restent soumis aux autorisations Android.
- Aucune clé d’IA n’est incluse. Le cerveau IA sera ajouté via une connexion sécurisée.
- Code non encore testé sur appareil réel; vérifie la compilation dans GitHub Actions.
