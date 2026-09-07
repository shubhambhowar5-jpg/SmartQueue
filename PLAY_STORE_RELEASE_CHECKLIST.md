# SmartQueue Play Store Release Checklist

This project is prepared for a future Play Store release without changing the working Restaurant queue logic.

## Before release
- Create/confirm the final unique Android application ID and update Firebase configuration together.
- Create a Play App Signing/release keystore and keep it private (never commit it).
- Increase `versionCode` for each Play Store update and update `versionName` as needed.
- Finish and merge all five sections.
- Test Firebase Authentication and Firestore real-time updates.
- Test customer and staff access on release builds.
- Build a signed Android App Bundle (`.aab`).
- Complete Play Console app listing, privacy/data-safety declarations, screenshots, and required policy forms.

## Current release configuration
- App name: SmartQueue
- Current versionCode: 1
- Current versionName: 1.0
- Release minification: disabled for stability during development
- Release debuggable: false

Do not commit signing keys, passwords, or other secrets.
