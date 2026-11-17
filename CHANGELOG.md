# Changelog

## [1.2.0] — 2026-11-17

Measured on the emulator with one 3:33 song, "Original" format:

| | 1.1.0 | 1.2.0 |
|---|---|---|
| Data downloaded | 11.28 MB (360p video) | 3.27 MB (audio only) |
| Tap "Download" → file ready | 9.3 s | 3.8 s |
| Source audio | AAC ~96 kbps | Opus ~128 kbps |

### Performance
- Removed the forced `player_client=android`: it made YouTube return only a 360p video stream. Now downloads the audio-only stream.
- Download reuses the analysis result (`--load-info-json`) instead of extracting the video a second time; falls back to the link if the saved data is stale.

## [1.1.0] — 2026-11-17

### Added
- Responsive layout for 4" phones up to 13" tablets, including a two-pane layout in landscape.
- Edge-to-edge drawing with proper status/navigation bar insets.
- Gradle version catalog (`gradle/libs.versions.toml`) and `gradlew.bat` for Windows.
- `.gitignore`, `CHANGELOG.md`, screenshots in `docs/screenshots/`.

### Changed
- `HomeScreen.kt` (1330 lines) split into `ui/screens/home/` by section:
  `HomeScreen`, `HomeHeader`, `InputSection`, `TrackDetailsCard`, `ResultCards`, `AmbientEffects`.
- Project root cleaned: screenshots moved to `docs/`, APKs to `releases/` (git-ignored).

### Performance
- Release APK 108 MB → ~34 MB: one APK per CPU type plus R8 code/resource shrinking.
- Idle screen no longer animates (background orbs, vinyl disc), so no continuous 60fps redraw.
- Download progress UI throttled to 5 updates/s.
- yt-dlp self-update limited to once per 24h (was every launch).
- `--no-playlist` when analyzing, so `&list=` share links don't resolve whole playlists.

### Fixed
- Downloads failed with `Operation not permitted` on Android 11+: yt-dlp wrote the cover `.webp` into `Music/`, which scoped storage forbids. Now downloads in the app cache, then copies the audio.
- Share intent on a cold start could run before the engine was initialized.
- yt-dlp self-update ran concurrently with the first request and broke it.
- Engine init failures (`Error`, not `Exception`) crashed the app instead of showing a message.
- Output file detection guessed by modification time (with an operator-precedence bug).
- Button labels ("Analisis", "Unduh Audio Sekarang", …) were nearly invisible: the global typography color overrode button content colors.
- Max content width on tablets was ignored due to modifier order, stretching cards across the screen.
- "Tempel" label clipped on 4" phones.

## [1.0.0]
- First release: on-device yt-dlp + FFmpeg, Original / MP3 320k / FLAC, album art & metadata, YouTube share-sheet integration.
