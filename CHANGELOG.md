# Changelog

## [Unreleased] — 2026-11-17

### Added
- Responsive layout for 4" phones up to 13" tablets, including a two-pane layout in landscape.
- Edge-to-edge drawing with proper status/navigation bar insets.
- Gradle version catalog (`gradle/libs.versions.toml`) and `gradlew.bat` for Windows.
- `.gitignore`, `CHANGELOG.md`, screenshots in `docs/screenshots/`.

### Changed
- `HomeScreen.kt` (1330 lines) split into `ui/screens/home/` by section:
  `HomeScreen`, `HomeHeader`, `InputSection`, `TrackDetailsCard`, `ResultCards`, `AmbientEffects`.
- Project root cleaned: screenshots moved to `docs/`, APKs to `releases/` (git-ignored).

### Fixed
- Button labels ("Analisis", "Unduh Audio Sekarang", …) were nearly invisible: the global typography color overrode button content colors.
- Max content width on tablets was ignored due to modifier order, stretching cards across the screen.
- "Tempel" label clipped on 4" phones.

## [1.0.0]
- First release: on-device yt-dlp + FFmpeg, Original / MP3 320k / FLAC, album art & metadata, YouTube share-sheet integration.
