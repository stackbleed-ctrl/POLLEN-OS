
# POLLEN-OS Repair Pack

This pack is designed to repair the immediate repo blockers without forcing a full rewrite.

## What this pack fixes
- Adds an `app/` Android module so `settings.gradle.kts` -> `include(":app")` is valid
- Adds `gradlew` and `gradlew.bat` scripts
- Adds a safer `app/src/main/AndroidManifest.xml`
- Adds proper Android resource file locations under `app/src/main/res`
- Adds a CI workflow for assemble / test / lint
- Adds a migration script to move the existing flat repo into a standard Android layout

## Important
The Gradle wrapper **JAR** is not included here because it is a generated binary artifact.
After copying these files into your repo, run one of the following:

```bash
chmod +x gradlew scripts/bootstrap_wrapper.sh scripts/restructure_repo.sh
gradle wrapper
```

or inside Android Studio / IntelliJ, open the project and let Gradle regenerate the wrapper.

## Fastest path
1. Copy this pack into repo root
2. Commit the files
3. Run `bash scripts/restructure_repo.sh`
4. Run `gradle wrapper`
5. Run `./gradlew assembleDebug`
6. Fix any package/import issues that remain
