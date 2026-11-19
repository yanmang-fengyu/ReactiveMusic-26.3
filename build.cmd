@echo off
rem Build + install to the 26.3 instance mods folder, keep window open
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build.ps1" -Install -Pause %*
