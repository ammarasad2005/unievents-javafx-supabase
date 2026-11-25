@echo off
REM Double-clickable Windows launcher — runs setup-and-run.ps1 bypassing PowerShell ExecutionPolicy
title UniEvents — Setup ^& Launcher
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup-and-run.ps1"
if errorlevel 1 pause
