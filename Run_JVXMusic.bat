@echo off
if exist "%~dp0desktopApp\build\compose\binaries\main\app\JVXMusic\JVXMusic.exe" (
    cd /d "%~dp0desktopApp\build\compose\binaries\main\app\JVXMusic"
    start "" "JVXMusic.exe"
) else if exist "%~dp0desktopApp\build\compose\binaries\main\app\VibeMusic\VibeMusic.exe" (
    cd /d "%~dp0desktopApp\build\compose\binaries\main\app\VibeMusic"
    start "" "VibeMusic.exe"
) else (
    cd /d "%~dp0desktopApp\build\compose\binaries\main\app\MusicApp"
    start "" "MusicApp.exe"
)
