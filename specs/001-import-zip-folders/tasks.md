# Tasks: Import Audio Tracks from ZIP Archives and Folders

**Input**: Design documents from `specs/001-import-zip-folders/` (spec.md, plan.md)

**Prerequisites**: `spec.md`, `plan.md`

## Phase 1: Core Engine & Security (Foundational)

- [x] T001 Implement `ZipAudioExtractor.kt` in `shared/src/desktopMain/kotlin/com/example/musicapp/platform/ZipAudioExtractor.kt`
- [x] T002 Implement Zip Slip path traversal security checks in `ZipAudioExtractor.kt`
- [x] T003 Implement synchronized lyrics (`.lrc`) matching and extraction in `ZipAudioExtractor.kt`
- [x] T004 Implement recursive unpacking for nested `.zip` archives in `ZipAudioExtractor.kt`
- [x] T005 Implement encoding fallback (UTF-8, CP866, system default) in `ZipAudioExtractor.kt`
- [x] T006 Implement recursive directory scanner `importDirectory` in `ZipAudioExtractor.kt`

## Phase 2: Platform Integration (Desktop & Mobile)

- [x] T007 Update `launchAudioImport` in `shared/src/desktopMain/kotlin/com/example/musicapp/platform/DesktopPlatform.kt` to accept `.zip` files and extract tracks
- [x] T008 Update `launchFolderImport` in `shared/src/desktopMain/kotlin/com/example/musicapp/platform/DesktopPlatform.kt` to use `ZipAudioExtractor.importDirectory`
- [x] T009 Update `importLocalAudioFile` in `shared/src/desktopMain/kotlin/com/example/musicapp/platform/DesktopPlatform.kt` to unpack archives and copy accompanying `.lrc` files
- [x] T010 Update Drag & Drop handler in `desktopApp/src/main/kotlin/com/example/musicapp/desktop/Main.kt` to use `ZipAudioExtractor.importFileOrArchive`

## Phase 3: User Interface & Experience

- [x] T011 Update TopBar buttons and dropdown menus in `shared/src/commonMain/kotlin/com/example/musicapp/ui/TopBar.kt` ("Файл / ZIP", "Папка")
- [x] T012 Update Home screen empty state button in `shared/src/commonMain/kotlin/com/example/musicapp/ui/screens/HomeScreen.kt` ("Файлы / ZIP")

## Phase 4: Test Suite & Verification

- [x] T013 Create unit test `testExtractTracksFromZip` in `shared/src/desktopTest/kotlin/com/example/musicapp/platform/ZipAudioExtractorTest.kt`
- [x] T014 Create unit test `testExtractWithAccompanyingLyrics` in `ZipAudioExtractorTest.kt`
- [x] T015 Create unit test `testZipSlipProtection` in `ZipAudioExtractorTest.kt`
- [x] T016 Create unit test `testNestedZipExtraction` in `ZipAudioExtractorTest.kt`
- [x] T017 Create unit test `testImportDirectoryWithAudioAndZips` in `ZipAudioExtractorTest.kt`
- [x] T018 Create unit test `testImportFileOrArchiveSingleFile` in `ZipAudioExtractorTest.kt`
- [x] T019 Run `./gradlew :shared:desktopTest --rerun-tasks` and verify 100% pass rate
