# Zaldi Driver & Dispatch Platform (Android)

Zaldi is an end-to-end mobility, logistics dispatch, and fleet telemetry application built with **Kotlin**, **Jetpack Compose (Material 3)**, **Room SQLite**, **Google Play Services Location**, and **OkHttp**.

## Key Features

- **15-Second Order Ping & Race-Condition Lock (`zaldi_ping`)**:
  - Real-time dispatch socket alerts with a 15-second animated countdown modal, guaranteed fare breakdown, and atomic `SELECT ... FOR UPDATE NOWAIT` trip claim verification.
- **Customer 'Book Ride' Console (`/api/internal/dispatch`)**:
  - Pickup and drop-off location inputs with GPS coordinates, Haversine distance & dynamic fare estimation, vehicle tier selection (`BIKE`, `AUTO`, `MINI_TRUCK`, `HEAVY_TRUCK`), and live HTTP `POST /api/internal/dispatch` request/response payload inspector.
- **Web-Based Admin Panel (`React 18 + Tailwind CSS` & Native Bridge)**:
  - Live monitoring of active trips (`PENDING_PING`, `ACCEPTED`, `IN_PROGRESS`, `COMPLETED`) and real-time driver availability (`5km Redis GEO` index) with one-tap Online/Offline status toggles synced via `@JavascriptInterface` to SQLite.
- **Foreground GPS Tracking & Offline Dead-Zone Queue**:
  - Android Foreground Service (`ZaldiLocationForegroundService`) using `FusedLocationProviderClient` with a 10-meter distance filter, Geohash encoding, and automatic Room SQLite caching when offline.
- **Turn-by-Turn Route Navigation & Earnings Ledger**:
  - Live route canvas, waypoint progression, KYC document verification workflow, and detailed driver payout ledger.

## Project Structure

- `app/src/main/java/com/example/MainActivity.kt` — Main entry point, runtime permission handlers, top telemetry bar, and bottom navigation.
- `app/src/main/java/com/example/ZaldiDriverViewModel.kt` — State management across Dispatch, Route Navigation, Book Ride, Admin Panel, KYC, and Earnings.
- `app/src/main/java/com/example/core/network/` — WebSocket client (`ZaldiSocketClient.kt`) and `/api/internal/dispatch` HTTP client (`ZaldiDispatchNetworkClient.kt`).
- `app/src/main/java/com/example/features/booking/` — Customer 'Book Ride' screen (`CustomerBookRideScreen.kt`) and React + Tailwind CSS Web Admin Panel (`ZaldiWebAdminPanel.kt`).
- `app/src/main/java/com/example/services/` — Foreground GPS location service (`ZaldiLocationForegroundService.kt`) and Room database (`ZaldiDatabase.kt`, `ZaldiDao.kt`, `ZaldiRepository.kt`).

## Building Locally

1. Open the project in **Android Studio** (Ladybug or newer) with JDK 17+.
2. Sync Gradle and run the `app` configuration on an Android device or emulator (API 24+), or build from the command line:
   ```bash
   ./gradlew assembleDebug
   ```
