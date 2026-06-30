# Contributing

Thank you for taking the time to contribute to Gas Leak Detector.

## Reporting Issues

Before opening an issue, search existing ones to avoid duplicates.
Include the following when filing a bug:

- Android version and device model
- App version (visible in Settings > About)
- Steps to reproduce
- Expected vs actual behaviour
- Logcat output if available (`adb logcat -s GasLeakDetector`)

## Branch Naming

| Type | Pattern | Example |
|---|---|---|
| Feature | `feature/<short-description>` | `feature/fcm-push-notification` |
| Bug fix | `fix/<short-description>` | `fix/fcm-crash-killed-process` |
| Chore | `chore/<short-description>` | `chore/upgrade-firebase-bom` |

## Commit Style

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
type(scope): short imperative summary

Optional body - what and why, not how.
```

Allowed types: `feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `style`, `perf`.

## Pull Requests

1. Fork the repository and create your branch from `main`.
2. Run `./gradlew testDebugUnitTest` and ensure all tests pass.
3. Run `./gradlew assembleDebug` and confirm the build succeeds.
4. Write or update tests for any logic change.
5. Update `CHANGELOG.md` under `[Unreleased]`.
6. Open a PR against `main` with a clear title and description.

## Code Style

- Java - follow the existing style in the project (4-space indent, `/* block comments */` for class-level docs).
- XML - 4-space indent, attribute-per-line for elements with more than 2 attributes.
- No auto-formatter commits (do not mix formatting changes with logic changes).

## Security Issues

Do not open a public issue for security vulnerabilities.
Email the maintainer directly at `pan2512811@gmail.com`.
