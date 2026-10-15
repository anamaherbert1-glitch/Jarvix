# JARVIX

Assistant personnel **PC** (interface graphique) + option Android.

## Installation PC

1. **Code** → **Download ZIP** (ou clone)
2. Décompresse le dossier
3. Double-clique **`DIAGNOSTIC.bat`** (vérifie Python)
4. Double-clique **`LANCER_JARVIX.bat`**

### Python requis
https://www.python.org/downloads/  
Coche **Add python.exe to PATH**

```bat
cd server
pip install pyttsx3 SpeechRecognition pyaudio ddgs
```

Puis, si les modules complets sont présents :
```bat
python jarvix_gui.py
```

### Fichiers d'aide
| Fichier | Rôle |
|---------|------|
| `LANCER_JARVIX.bat` | Lance JARVIX |
| `DIAGNOSTIC.bat` | Diagnostic (fenêtre qui reste ouverte) |
| `LISEZMOI_INSTALLATION.txt` | Instructions |
| `DESINSTALLER_JARVIX.bat` | Désinstallation |

### Fonctionnalités prévues
- Interface graphique (chat, boutons)
- Cerveau IA local + knowledge/
- Recherche Google / web
- Micro + voix + commande stop

## Android
Workflow GitHub Actions : **Build JARVIS APK**

## Note développement
Les scripts d'installation et le README sont à jour sur `main`.  
Les modules Python complets (`jarvix_gui.py`, `ai_brain_local.py`, etc.) sont fournis dans les livraisons ZIP de développement ; finalisation progressive sur le dépôt.
