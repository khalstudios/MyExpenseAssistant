# MyExpenseAssistant Agent Guide

## Project

- Android expense tracker written in Kotlin with Jetpack Compose and Material 3.
- Package and namespace: `com.expenseassistant`.
- Minimum SDK 26; compile and target SDK 35; JDK 17.
- Room schemas are generated to `app/schemas` through KSP.

## Architecture

- Keep capture, parsing, categorization, storage, and UI responsibilities separate.
- Capture services only process packages explicitly listed in `PaymentApps`.
- `PaymentTextParser` must reject failed, pending, collect-request, and promotional text.
- Categorization follows this precedence: learned merchant rules, keyword knowledge base, then structural heuristics.
- Persist changes through repositories and ViewModels; composables should receive state and invoke callbacks rather than access storage directly.

## Compose UI

- Follow existing Material 3 and local reusable-component patterns.
- Keep screens stateful only for transient editing UI; retain persisted state in ViewModels or repository-backed flows.
- Preserve unsaved-edit handling on transaction forms.
- Prefer focused, accessible controls with content descriptions for icon-only buttons.
- Keep visual changes scoped to the requested screen or component; do not refactor unrelated UI surfaces.

## Privacy And Permissions

- The app is intentionally offline: do not add internet access or telemetry without explicit approval.
- Notification and accessibility capture are policy-sensitive. Keep accessibility optional, narrowly package-scoped, and clearly user initiated.
- Do not broaden captured packages or permissions without updating relevant configuration, documentation, and tests.

## Testing And Validation

- When changing parser behavior, update or add focused cases in `PaymentTextParserTest`.
- When changing persistence models, migrations, or Room queries, verify schema output and affected repository tests.
- For Kotlin and Compose changes, run:

```powershell
./gradlew :app:assembleDebug :app:testDebugUnitTest --no-daemon --no-configuration-cache
```

- For device deployment, install `app/build/outputs/apk/debug/app-debug.apk` with ADB after a successful build.
- Consult `RUN_ON_PHONE.txt` for the complete local build, test, install, launch, and verification workflow.

## Change Discipline

- Make the smallest change that addresses the request.
- Do not revert or overwrite unrelated user changes in a dirty worktree.
- Preserve existing public APIs unless the requested behavior requires a compatible update.
- Update `README.md` when user-visible behavior, setup, privacy, or capture support changes.
