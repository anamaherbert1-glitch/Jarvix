@echo off
chcp 65001 >nul
title Desinstallation JARVIX
echo.
echo Suppression de JARVIX...
set "INSTALL_DIR=%LOCALAPPDATA%\JARVIX"
set "DESKTOP=%USERPROFILE%\Desktop"
if not exist "%DESKTOP%\JARVIX.bat" if exist "%USERPROFILE%\OneDrive\Desktop\JARVIX.bat" set "DESKTOP=%USERPROFILE%\OneDrive\Desktop"

if exist "%DESKTOP%\JARVIX.bat" del /f /q "%DESKTOP%\JARVIX.bat"
if exist "%APPDATA%\Microsoft\Windows\Start Menu\Programs\JARVIX" rmdir /s /q "%APPDATA%\Microsoft\Windows\Start Menu\Programs\JARVIX"
if exist "%INSTALL_DIR%" rmdir /s /q "%INSTALL_DIR%"

echo JARVIX a ete desinstalle.
pause
