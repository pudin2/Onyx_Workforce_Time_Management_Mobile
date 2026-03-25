# Onyx Workforce Time Management Mobile

Mobile application for workforce time management within the Onyx platform.

This project supports employee workday control in operational environments, including attendance-related reporting, event capture, geolocation support, local data handling, and background synchronization with backend infrastructure.

---

## Overview

**Onyx Workforce Time Management Mobile** is the Android mobile component of the Onyx ecosystem. It is designed to support field and operational employees by enabling structured workday reporting, mobile data capture, and automated file synchronization.

The application is part of a broader business platform that includes ERP, web services, and operational control components.

---

## Key Features

- Workforce time management support
- Workday and attendance-related event capture
- Report visualization from mobile records
- Geolocation support for captured records
- Local JSON-based file handling
- Background synchronization using FTP
- Periodic and immediate upload jobs
- App update check flow
- Mobile-first Android interface built with Jetpack Compose

---

## Technology Stack

- **Language:** Kotlin
- **Platform:** Android
- **UI Framework:** Jetpack Compose
- **Background Processing:** WorkManager
- **Data Handling:** JSON
- **Connectivity:** FTP-based synchronization
- **Location Services:** Google Play Services Location
- **Build System:** Gradle Kotlin DSL

---

## Project Structure

```text
Onyx_Workforce_Time_Management_Mobile/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/horas_extra_tecnipalma/
│   │   │   │   ├── AsyncUpload.kt
│   │   │   │   ├── ConfigScreen.kt
│   │   │   │   ├── FileLogger.kt
│   │   │   │   ├── HomeActivity.kt
│   │   │   │   ├── LocationUtils.kt
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── ReporteScreen.kt
│   │   │   │   └── Worker.kt
│   │   │   ├── res/
│   │   │   ├── assets/
│   │   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
└── settings.gradle.kts
```

---

## Main Functional Components

### `MainActivity`
Application entry point.

### `HomeActivity`
Main screen container for the application workflow.

### `ReporteScreen`
Handles report-related views and record visualization.

### `ConfigScreen`
Supports application settings and configuration flows.

### `LocationUtils`
Provides location-related utilities used by the application.

### `AsyncUpload`
Manages background file synchronization and FTP upload logic.

### `Worker`
Registers scheduled and immediate background execution using WorkManager.

### `FileLogger`
Provides internal logging support for traceability and troubleshooting.

---

## Background Synchronization

The application includes a background worker that:

- runs an immediate synchronization job at startup
- schedules periodic synchronization
- uploads local JSON files to an FTP destination
- checks remote assets
- validates app update metadata

This design supports delayed synchronization scenarios for operational environments where connectivity may be unstable or intermittent.

---

## Permissions

The project currently includes permissions related to:

- internet access
- network state access
- fine and coarse location
- notifications
- package installation requests

These permissions support connectivity, geolocation, notifications, and update-related flows.

---

## Configuration Notes

Before using the application in a real environment, review and adapt the following:

- server endpoints
- FTP credentials
- remote directories
- update distribution flow
- environment-specific assets
- package naming and branding consistency

---

## Refactoring Note

Although the repository is already named **Onyx Workforce Time Management Mobile**, parts of the internal codebase still preserve legacy identifiers from the previous project naming convention.

---

## Repository Purpose

This repository represents the Android mobile application layer of the Onyx platform and provides the basis for expanding employee workday control and operational mobility capabilities.
