# Recipe app

An offline-first Android app for saving recipes from any source: text, photos, links (including
YouTube, TikTok and Instagram links, saved as link + title + thumbnail, never the video) and PDFs.
The main entry point is the Android share sheet.

Status: **all 6 roadmap phases done** (search/tags/export-import was the last). See "Roadmap".

## Stack

Kotlin, Jetpack Compose (Material 3), MVVM with a repository layer, Room, Hilt, Coroutines/Flow,
Coil, Navigation Compose. Gradle Kotlin DSL with a version catalog (`gradle/libs.versions.toml`).

## Architecture

```
app/src/main/java/dev/malkolm/recipeapp/
  ui/       Composables + ViewModels. State flows down as UiState, events flow up as function calls.
  domain/   Pure Kotlin models and repository interfaces (no Android imports).
  data/     Room database (local/), repository implementations (repository/), file storage.
  di/       Hilt modules.
```

Composables contain no business logic. ViewModels expose a `StateFlow<UiState>` and talk only to
repository interfaces from `domain`, so a cloud-sync implementation can be added later without
changing the UI.

## Building

Requirements: Android Studio (recent enough for AGP 9.4) and the Android SDK with API 37.

From Android Studio just press Run. From a terminal, Gradle 9 needs Java 17+, so if your default
`java` is older, point `JAVA_HOME` at Android Studio's bundled JDK first:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
```

## Checks (the same ones CI runs)

```
./gradlew ktlintCheck lintDebug testDebugUnitTest
./gradlew ktlintFormat        # auto-fix code style
```

## Roadmap

1. Project skeleton (Hilt, version catalog, ktlint, CI, Git) - done
2. Room entities, DAOs, repository, migrations, tests - done (see `docs/database.md`)
3. Recipe list, detail and add/edit screens (text and link) - done
4. Photo picker and camera, image storage - done
5. Share-sheet intent filter - done
6. Search, tags, export/import backup - done
