# JARVIX

Assistant personnel **PC** (interface graphique) + option Android.

## Installation PC (recommandé)

1. Télécharge le dépôt (Code → Download ZIP) ou clone
2. Décompresse
3. Double-clique sur **`LANCER_JARVIX.bat`**
4. Si besoin, lance d’abord **`DIAGNOSTIC.bat`**

Installation complète (raccourci Bureau) : **`INSTALLER_JARVIX.bat`**

### Prérequis
- Python 3 : https://www.python.org/downloads/ (coche **Add python.exe to PATH**)
- Internet pour la recherche web et la reconnaissance vocale

```bat
cd server
pip install pyttsx3 SpeechRecognition pyaudio ddgs
python jarvix_gui.py
```

### Fonctionnalités PC
- Interface graphique (chat, boutons)
- Cerveau IA local (`server/ai_brain_local.py`)
- Apprentissage via fichiers dans `server/knowledge/`
- Recherche web **Google** (prioritaire) + DuckDuckGo
- Ouverture du navigateur
- Micro + synthèse vocale
- Commande **stop** pour couper la voix

### Fichiers utiles
| Fichier | Rôle |
|---------|------|
| `LANCER_JARVIX.bat` | Lance l’interface |
| `DIAGNOSTIC.bat` | Vérifie Python et les modules |
| `INSTALLER_JARVIX.bat` | Installe + raccourcis |
| `server/jarvix_gui.py` | Interface graphique |
| `server/jarvix_pc.py` | Mode terminal |
| `server/voice_engine.py` | Micro + voix |
| `server/web_tools.py` | Recherche Google / web |

## Android (optionnel)
Build APK via GitHub Actions : workflow **Build JARVIS APK**.

## Licence / limites
Prototype. Aucune action sensible automatique. La recherche web nécessite Internet.
