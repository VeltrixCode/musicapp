# Feature Specification: Import Audio Tracks from ZIP Archives and Folders

**Feature Branch**: `001-import-zip-folders`

**Created**: 2026-11-21

**Status**: Ready for Planning

**Input**: User description: "Оформить формальную спецификацию и тесты для импорта треков из ZIP-архивов и папок"

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Direct ZIP Archive Import (Priority: P1)

A user has music albums or track collections compressed into a `.zip` file. The user wants to import them directly into their library without manually unzipping them with external software first. The user selects the `.zip` archive via the file picker dialog or drags and drops it onto the application window. The application unpacks all supported audio tracks, extracts any synchronized lyrics (`.lrc`) matching the songs, and adds every track into the library with rich metadata.

**Why this priority**:
Primary capability requested by users. Most downloaded music albums and backups are distributed in ZIP format.

**Independent Test**:
Provide a ZIP archive containing two or more songs and verify all songs appear in the library with correct titles, duration, and playable audio.

**Acceptance Scenarios**:
1. **Given** a valid `.zip` archive containing multiple audio tracks (`.mp3`, `.wav`, `.flac`, etc.) in root or nested folders, **When** the user selects the archive through the file selection dialog or drops it onto the app, **Then** all audio tracks within the archive are extracted and added to the user's library.
2. **Given** a `.zip` archive containing both an audio track (`track.mp3`) and a synchronized lyrics file (`track.lrc`), **When** the archive is imported, **Then** the lyrics file is preserved alongside the track and displayed during playback.
3. **Given** a `.zip` archive containing non-audio auxiliary files (e.g., `.txt`, `.nfo`, `.url`), **When** the archive is imported, **Then** only supported audio formats and matching lyrics are extracted, while irrelevant files are ignored.

---

### User Story 2 - Recursive Folder and Subfolder Import (Priority: P1)

A user has a large music folder on disk structured into artist, album, and year subdirectories, optionally containing both standalone audio files and nested ZIP archives. The user selects "Импорт папки" (Import Folder) or drops the folder into the app. The application traverses the entire directory tree, uncompresses any ZIP archives discovered within the folders, copies standalone tracks, links accompanying `.lrc` files, and adds all discovered songs in a single batch operation.

**Why this priority**:
Core library-building workflow. Allows migrating existing organized local music libraries in one step.

**Independent Test**:
Provide a folder containing nested subfolders with songs and embedded `.zip` files; verify the entire hierarchy is flattened into library tracks and a summary notification is shown.

**Acceptance Scenarios**:
1. **Given** a folder hierarchy containing music in multiple nested subdirectories, **When** the user chooses the root folder for import, **Then** all supported audio tracks from all levels are added to the library.
2. **Given** a folder that contains one or more `.zip` archives alongside audio files, **When** the folder is imported, **Then** both loose audio files and archived audio files from the ZIP archives are imported.
3. **Given** multiple tracks are imported in bulk from a folder, **When** the operation finishes, **Then** the application notifies the user with the count of successfully imported tracks.

---

### User Story 3 - Sandbox Isolation & Zip Slip Security (Priority: P2)

A user receives a ZIP archive from an untrusted source or an archive with malformed path entries (e.g. `../../target_file.mp3`). The application prevents path traversal attempts, strictly confining all extracted files to the isolated application data directory, and skips or sanitizes unsafe entries.

**Why this priority**:
Crucial security requirement to protect user files and system integrity from directory traversal attacks (CVE Zip Slip pattern).

**Independent Test**:
Import a crafted ZIP archive with `../` path entries; verify that no file is created outside the target application uploads directory and legitimate safe files inside the archive are processed normally.

**Acceptance Scenarios**:
1. **Given** a ZIP archive containing path traversal entries (`../`), **When** the archive is imported, **Then** the application refuses to write outside the isolated sandbox and skips the traversal entry.
2. **Given** archive entries with special or illegal characters, **When** imported, **Then** filenames are sanitized safely without causing filesystem crashes.

---

### User Story 4 - International Character Encoding & Mojibake Prevention (Priority: P3)

A user imports a ZIP archive created in Windows Explorer or third-party archivers with Cyrillic (CP866 / Windows-1251) or UTF-8 filenames. The application correctly identifies the character encoding, preserving readable song titles and artist names.

**Why this priority**:
Essential usability for international and Cyrillic song titles, preventing garbled filename artifacts.

**Independent Test**:
Import a ZIP file created with Cyrillic filenames; verify track titles and file names retain proper readable Russian characters.

**Acceptance Scenarios**:
1. **Given** an archive created on Windows with Russian filenames in legacy CP866 or Windows-1251 encoding, **When** imported, **Then** the filenames and track titles are read as valid legible text rather than replacement symbols.

---

## Edge Cases

- **Empty Archive**: A ZIP archive with no files or zero audio tracks produces a graceful notification ("No audio files found") and does not alter the library or crash.
- **Corrupted Archive**: A truncated or corrupted `.zip` file is detected and safely aborted without leaving corrupted partial files or crashing the app.
- **Deeply Nested Archives**: A ZIP archive containing another `.zip` inside is recursively unpacked up to a safe depth limit.
- **Duplicate Track Filenames Across Subfolders**: If `disc1/track01.mp3` and `disc2/track01.mp3` are in the same archive, each is assigned an isolated unique identifier so neither overwrites the other.
- **Missing or Read-Only Source**: If user permissions deny access or a folder is inaccessible, the error is caught and reported clearly to the user.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST support importing audio tracks from `.zip` archive files directly via the file picker dialog.
- **FR-002**: System MUST support importing audio tracks and archives via Drag and Drop into the application window.
- **FR-003**: System MUST support recursive scanning and import of folders containing audio files and nested ZIP archives.
- **FR-004**: System MUST support the following audio formats inside archives: `.mp3`, `.wav`, `.m4a`, `.aac`, `.flac`, `.ogg`.
- **FR-005**: System MUST detect and extract accompanying synchronized lyrics (`.lrc`) files with matching names located alongside audio files in archives or folders.
- **FR-006**: System MUST enforce Zip Slip protection, ensuring all unpacked contents remain inside the designated application sandbox directory.
- **FR-007**: System MUST assign unique identifiers to each extracted track to prevent naming collisions for identical filenames across subdirectories.
- **FR-008**: System MUST support both UTF-8 and legacy Cyrillic (CP866 / Windows-1251) archive encodings.
- **FR-009**: System MUST read ID3 and container audio tags (artist, title, album, year, genre, duration) from extracted files and fall back to clean filename titles when tags are missing.
- **FR-010**: System MUST clean up temporary intermediate archive files created during nested archive extractions.

---

### Key Entities

- **Audio Track**: An entity representing a playable audio item, containing title, artist, album, duration, file location, and optional synchronized lyrics.
- **Archive Package**: A compressed container (`.zip`) storing one or more audio files, folder structures, and associated metadata/lyrics.
- **Library Collection**: The centralized user collection containing imported tracks, playlists, favorites, and playback state.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of valid audio files (`.mp3`, `.wav`, `.m4a`, `.aac`, `.flac`, `.ogg`) within a ZIP archive are extracted and added to the library without manual user intervention.
- **SC-002**: Accompanying `.lrc` lyrics files matching audio files are preserved and displayable in 100% of test cases.
- **SC-003**: Zero files can be written outside the target sandbox directory when processing malicious path-traversal archives.
- **SC-004**: An archive with up to 50 tracks imports in under 3 seconds on standard local desktop hardware.
- **SC-005**: All unit and integration test suites pass with 100% success rate (`BUILD SUCCESSFUL`).

---

## Assumptions

- Target environment supports standard Java / Kotlin runtime zip utilities (`java.util.zip`).
- Audio tag reading engine (`jaudiotagger`) is available to parse ID3 and container metadata from extracted files.
- Extracted files are stored in the application's isolated `uploads` directory managed by `AppStorage`.
