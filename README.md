# Zaldi 3-App Suite: Customer App, Driver App & Admin App (Android)

Zaldi is a complete 3-application mobility, logistics dispatch, and fleet telemetry suite built with **Kotlin**, **Jetpack Compose (Material 3)**, **Room SQLite**, **Google Play Services Location**, **Firebase Auth**, and **OkHttp**.

## 3 Separate Applications Included

1. **Application #1 — Zaldi Customer App (`apps/customer` • `MainActivity`)**
   - **Launcher Label**: `Zaldi Customer`
   - **Dedicated Customer Tabs**: `Book Ride` (`MapView`, `PickupInput`, `DropInput`, `LocationSuggestions`) • `Vehicles` (`VehicleSelector`, `FareCard`) • `Track` (`TrackingMap`, `DriverMarker`, `RouteLine`, `ETA`, 4-digit OTP PIN) • `Payment` (`PaymentCard`, UPI/Wallet/Card/Cash, 5-Star Rating) • `History` (`RideHistory`) • `Profile` (`Profile`, `OTP`, `Notifications`, `Support`).
2. **Application #2 — Zaldi Driver App (`apps/driver` • `DriverAppActivity`)**
   - **Launcher Label**: `Zaldi Driver`
   - **Dedicated Driver Tabs**: `Driver Home` (`OnlineToggle`, `DriverMap` Surge Heatmap, `BookingRequest` 15s Offer Overlay) • `Active Trip` (`Navigation`, OTP PIN Verification, Proof of Delivery) • `Earnings` (`EarningsCard`, Offline SQLite Earnings Logs & Sync, Instant Payout) • `KYC & Docs` (`DriverStatus`) • `Vehicle & Profile` (`VehicleCard`, Fleet Switcher, Reviews).
3. **Application #3 — Zaldi Admin App (`apps/admin` • `AdminAppActivity`)**
   - **Launcher Label**: `Zaldi Admin`
   - **Dedicated Admin Tabs**: `Dashboard` (`DashboardCard`, `MapPanel`, Live Fleet KPIs, React WebView Admin Console) • `Bookings` (`BookingTable`, `LiveTracking`, Active Trip Controller, `POST /api/internal/dispatch` Console) • `Drivers` (`DriverTable`, `VehicleTable`, KYC Approval, Online Toggle, Register New Driver) • `Matching` (`Pricing`, `ServiceAreas`, 8-Step Redis Driver Matching Engine) • `Reports` (`PaymentTable`, `CustomerTable`, Financial Ledger, Reviews & Complaints).

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
