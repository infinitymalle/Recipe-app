# Security

What the app already does, and what must be done before it is released or goes online.

## Before releasing (Play Store, or any online feature)

Work through this list before the first public release, and again before adding a server.

### GitHub repository

- [ ] **Turn on Dependabot alerts and security updates**: repo **Settings → Code security →
      Dependabot alerts** and **Dependabot security updates**. (Version-update pull requests
      are already configured in `.github/dependabot.yml`; the alerts are a separate switch.)
- [ ] **Turn on secret scanning and push protection** (same settings page), so a key or password
      committed by mistake is caught.
- [ ] **Protect the `main` branch**: require CI to pass before merging, no force pushes.
- [ ] **Pin GitHub Actions to commit hashes** instead of version tags in
      `.github/workflows/ci.yml`, so a tampered action release cannot run in CI. Dependabot keeps
      pinned hashes up to date too.

### The app

- [ ] **Sign releases with a private upload key** that is never committed to the repo. Use
      Play App Signing, and keep the upload key's password outside the project (e.g. in
      `~/.gradle/gradle.properties` or CI secrets).
- [ ] **Turn on code shrinking for release** (`optimization { enable = true }` in
      `app/build.gradle.kts`) and test the release build; it removes unused code. It is not a
      security wall (apps can always be decompiled), just less to attack.
- [ ] **Decide what Android's cloud backup may include** (`res/xml/backup_rules.xml` and
      `data_extraction_rules.xml`, currently the defaults: everything, encrypted by Google).
      Exclude anything added later that must not leave the phone, like login tokens.
- [ ] **Review every new way data enters the app** (new share-sheet types, deep links, imports)
      against the rules in `data/AppFiles.kt`.

### When a server is added (sync, shared shopping lists)

- [ ] **HTTPS only**: add a network security config with `cleartextTrafficPermitted="false"`.
- [ ] **Use an existing login service** (e.g. Firebase Auth, Supabase Auth); do not build password
      storage yourself.
- [ ] **The server checks everything itself**: who may read or change each recipe and shopping
      list, and that every field is valid. Never rely on the app to enforce it; anyone can send
      requests without the app.
- [ ] **No secret keys in the app.** Anything shipped in the APK can be extracted. Keys that must
      stay secret belong on the server.
- [ ] **Store login tokens with the Android Keystore**, not in plain SharedPreferences, and
      exclude them from backups.
- [ ] **Rate-limit the server** (logins, uploads) and cap upload sizes, like the zip import does.
- [ ] **Write a privacy policy** (required by Google Play once data leaves the phone) and offer a
      way to delete an account and its data.

## Already in place

- Imported backups and shared recipe files are refused if a recipe id or photo path could reach
  outside the photo folders (`AppFiles.isSafeId`, `AppFiles.isSafeRelativePath`), and zip
  entries cannot escape the files directory ("Zip Slip").
- Exported and shared files only ever contain photos from the photo folders.
- Zip imports are capped in file count and size (`ZipLimits` in `RecipeBackupService.kt`).
- Shared recipes are imported as copies with fresh ids, so they cannot overwrite existing recipes.
- Files from the share sheet are only read from `content://` URIs, never `file://`.
- Recipe links only open for http/https.
- The FileProvider and the timer service are not exported; the provider only exposes the photo
  and share folders.
- Calendar access (for the meal planner) is only asked for when the user chooses a calendar in
  Settings. The app lists the writable calendars, and only ever writes, updates or deletes events
  whose ids it stored itself (`plan_entries.calendarEventId`); it never reads or changes the
  user's other events. Stopping calendar sync removes the app's upcoming events.
- Database access goes through Room with bound parameters (no SQL built from user text).
- CI runs with read-only repository permissions.
