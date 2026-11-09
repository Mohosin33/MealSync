# MealSync

**Smart mess management for shared meals, members, and money.**

MealSync is an Android app for managing a shared household or “mess.” Managers can create a group and manage its records; members can join with an invitation code and track meals and balances.

## Features

- Record lunch, dinner, and guest meals.
- Manage expenses, deposits, balances, and monthly archives.
- Invite members by sharing a code or composing an SMS.
- Track member presence and receive meal-cutoff reminders.
- Generate and share PDF reports.
- Firebase Authentication, Cloud Firestore, and Realtime Database.

## Tech

Java · Android Views · View Binding · RecyclerView · Firebase · WorkManager

The app also responds to airplane-mode changes, reads contacts with permission, and stores selected profile photos locally.

## Setup

1. Open the project in Android Studio.
2. Register `com.example.mealsync` in Firebase and add `google-services.json` to `app/`.
3. Enable Firebase Authentication, Cloud Firestore, and Realtime Database.
4. Review `firestore.rules` and `database.rules.json` before deploying.

Requires Android 8.0 (API 26) or newer.

## Build and test

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```
