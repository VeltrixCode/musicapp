@echo off
if exist "%~dp0desktopApp\build\compose\binaries\main\app\VibeMusic\VibeMusic.exe" (
    cd /d "%~dp0desktopApp\build\compose\binaries\main\app\VibeMusic"
    start "" "VibeMusic.exe"
) else (
    cd /d "%~dp0desktopApp\build\compose\binaries\main\app\MusicApp"
    start "" "MusicApp.exe"
)
