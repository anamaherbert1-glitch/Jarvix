@echo off
chcp 65001 >nul 2>&1
title JARVIX
cd /d "%~dp0"
echo.
echo Demarrage JARVIX...
echo.

if not exist "server\jarvix_gui.py" (
  echo ERREUR: dossier incomplet. Decompresse tout le ZIP.
  pause
  exit /b 1
)

cd server

set PY=python
where python >nul 2>&1
if errorlevel 1 set PY=py -3
where %PY% >nul 2>&1
if errorlevel 1 (
  echo Python introuvable.
  echo Installe: https://www.python.org/downloads/
  echo Coche: Add python.exe to PATH
  start https://www.python.org/downloads/
  pause
  exit /b 1
)

echo Python:
%PY% --version
echo.
echo Lancement de l'interface...
%PY% jarvix_gui.py
echo.
echo Code sortie: %ERRORLEVEL%
if errorlevel 1 (
  echo.
  echo ECHEC. Installe les paquets:
  echo   %PY% -m pip install pyttsx3 SpeechRecognition ddgs pyaudio
  echo Puis relance.
)
echo.
pause
