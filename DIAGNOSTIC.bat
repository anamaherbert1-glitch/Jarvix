@echo off
chcp 65001 >nul 2>&1
title JARVIX - Diagnostic
color 0A
cd /d "%~dp0"

echo.
echo ========================================
echo   JARVIX - DIAGNOSTIC (ne ferme pas)
echo ========================================
echo.
echo Dossier : %CD%
echo.

echo [1] Python ?
where python 2>nul
if errorlevel 1 (
  where py 2>nul
  if errorlevel 1 (
    echo  RESULTAT: Python INTROUVABLE
    echo  ACTION: installe Python sur https://www.python.org/downloads/
    echo  IMPORTANT: coche "Add python.exe to PATH"
    echo.
    start https://www.python.org/downloads/
    goto FIN
  ) else (
    echo  RESULTAT: py trouve
    set PY=py -3
  )
) else (
  echo  RESULTAT: python trouve
  set PY=python
)
%PY% --version
echo.

echo [2] Fichiers JARVIX ?
if exist "server\jarvix_gui.py" (echo  OK jarvix_gui.py) else (echo  MANQUANT server\jarvix_gui.py)
if exist "server\ai_brain_local.py" (echo  OK ai_brain_local.py) else (echo  MANQUANT ai_brain_local.py)
if exist "server\voice_engine.py" (echo  OK voice_engine.py) else (echo  MANQUANT voice_engine.py)
if exist "server\web_tools.py" (echo  OK web_tools.py) else (echo  MANQUANT web_tools.py)
echo.

echo [3] tkinter (interface) ?
%PY% -c "import tkinter; print('  OK tkinter')" 2>nul
if errorlevel 1 (
  echo  ECHEC tkinter
  echo  Reinstalle Python en cochant tcl/tk / PATH
)
echo.

echo [4] Test cerveau local ?
cd server
%PY% -c "from ai_brain_local import chat; print('  Reponse:', chat('bonjour')['reply'][:80])" 2>nul
if errorlevel 1 (
  echo  ECHEC import cerveau
  %PY% -c "from ai_brain_local import chat; print(chat('bonjour'))"
)
cd ..
echo.

echo [5] Paquets optionnels ?
%PY% -c "import pyttsx3; print('  OK voix')" 2>nul || echo  - pyttsx3 absent (voix)
%PY% -c "import speech_recognition; print('  OK micro')" 2>nul || echo  - SpeechRecognition absent (micro)
%PY% -c "import ddgs; print('  OK recherche')" 2>nul || echo  - ddgs absent (recherche)
echo.

echo ========================================
echo  Si tout est OK, lance ensuite:
echo    LANCER_JARVIX.bat
echo ========================================
echo.
:FIN
echo Appuie sur une touche pour fermer...
pause >nul
