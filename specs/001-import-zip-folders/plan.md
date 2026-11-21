# Implementation Plan: Import Audio Tracks from ZIP Archives and Folders

**Branch**: `001-import-zip-folders` | **Date**: 2026-11-21 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-import-zip-folders/spec.md`

## Summary

Implement unified, secure, high-performance extraction and import of audio tracks (`.mp3`, `.wav`, `.m4a`, `.aac`, `.flac`, `.ogg`) and synchronized lyrics (`.lrc`) from ZIP archives and folder hierarchies. The solution protects against Zip Slip path traversal vulnerabilities, safely supports Cyrillic encodings, recurses into nested archives and subfolders, and updates the application's unified media library.

## Technical Context

- **Language/Version**: Kotlin 2.2.10 (Multiplatform JVM + Android)
- **Primary Dependencies**: Compose Multiplatform (Material 3), `java.util.zip.*`, `net.jthink:jaudiotagger:3.0.1`
- **Storage**: App data directory via `AppStorage.dir/uploads` (isolated per platform and test sandbox)
- **Testing**: `kotlin.test`, Gradle `:shared:desktopTest --rerun-tasks`
- **Target Platform**: Desktop (JVM 17 on Windows, Linux, macOS) & Android
- **Project Type**: Desktop & Mobile Music Player Application
- **Performance Goals**: Batch unpack & index 50 tracks in <3s; zero UI stutter during Drag & Drop
- **Constraints**: Offline-capable, zero external native binaries required for zip extraction, strict Zip-Slip sandbox enforcement

## Architecture & Module Layout

```text
shared/
├── src/
│   ├── commonMain/kotlin/com/example/musicapp/
│   │   ├── platform/Platform.kt                   # expect/actual declarations (launchAudioImport, launchFolderImport, importedTrack)
│   │   ├── ui/TopBar.kt                           # TopBar menu and buttons ("Файл / ZIP", "Папка")
│   │   └── ui/screens/HomeScreen.kt               # Empty state & library action buttons ("Файлы / ZIP")
│   ├── desktopMain/kotlin/com/example/musicapp/
│   │   └── platform/
│   │       ├── ZipAudioExtractor.kt               # Core extraction engine with Zip Slip protection and LRC matching
│   │       ├── DesktopPlatform.kt                 # Desktop platform implementations with ZipAudioExtractor integration
│   │       └── DesktopStorage.kt                  # AppStorage isolation for production and tests
│   └── desktopTest/kotlin/com/example/musicapp/
│       └── platform/
│           └── ZipAudioExtractorTest.kt           # Complete test suite (6 tests covering root, nested, lyrics, zip slip)
desktopApp/
└── src/main/kotlin/com/example/musicapp/desktop/
    └── Main.kt                                    # Desktop window Drag & Drop listener connected to ZipAudioExtractor
```

## Security & Reliability Design

1. **Zip Slip Protection**:
   - Every `ZipEntry` path is canonicalized against `destinationDir.canonicalFile`.
   - Any entry with relative segments (`..`) or paths that do not start with `destinationDir.canonicalPath + File.separator` is immediately dropped.
   - Entry names are sanitized to eliminate illegal filesystem characters.
2. **Synchronized Lyrics Association**:
   - Matching `.lrc` files in the same folder or archive path (e.g. `song.mp3` and `song.lrc`) are extracted with identical unique ID prefixes.
   - `readLrcContent(audioPath)` immediately finds the accompanying lyrics.
3. **Encoding Fallback**:
   - Tries UTF-8 first.
   - Falls back to CP866 (common for Windows Russian ZIP archives) and platform default charset if UTF-8 encounters malformed byte sequences.
4. **Intermediate Cleanup**:
   - Nested zip archives extracted to temporary files are cleaned up in a `finally` block after recursive processing.

## Verification & Test Plan

1. **Unit Tests (`ZipAudioExtractorTest.kt`)**:
   - `testExtractTracksFromZip`: Validates root and subfolder audio extractions, ignoring non-audio files.
   - `testExtractWithAccompanyingLyrics`: Validates synchronized lyrics pairing and extraction.
   - `testZipSlipProtection`: Confirms directory traversal attacks are neutralized.
   - `testNestedZipExtraction`: Confirms multi-level archive recursion works smoothly.
   - `testImportDirectoryWithAudioAndZips`: Confirms mixed directory trees (audio + folders + zips) import completely.
   - `testImportFileOrArchiveSingleFile`: Confirms single file handling with local lyrics preservation.
2. **Build Verification**:
   - `./gradlew :shared:desktopTest --rerun-tasks --no-daemon` must exit with code 0 and 100% test pass rate.
