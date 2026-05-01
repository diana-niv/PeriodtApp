# Periodt.

A period tracker app that helps women track their mood and health throughout and around their cycle.

## About The Project

Periodt helps women understand their cycles better by tracking symptoms, moods, and activities alongside cycle predictions.

This project has two main parts:
- **App:** An Android application built with Kotlin and Room (SQLite), featuring a cycle wheel, calendar view, symptom logging, and cycle predictions.
- **Documentation:** A project documentation page covering the app concept, database design, usability testing, and final reflections.

📄 [View Project Documentation](https://ccl3-ws2025-d6353a.pages.nwt.fhstp.ac.at/)

Built with:
- Kotlin (Android)
- Room (SQLite)
- Android Studio

## Getting Started

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (latest stable version recommended)
- Android SDK (API level compatible with the project — check `build.gradle`)
- An Android device or emulator

### Installation

1. Clone or download the repository.
2. Open the project folder in Android Studio.
3. Let Gradle sync and resolve dependencies.
4. Run the app on an emulator or physical device via **Run > Run 'app'**.

Alternatively, you can download and install the APK directly:

📦 [Download APK](https://ccl3-ws2025-d6353a.pages.nwt.fhstp.ac.at/apk%20file/app-debug.apk)

> Note: You may need to enable **Install from unknown sources** on your device to install the APK manually.

## Roadmap

### Features

- Track your current cycle stage via an interactive cycle wheel
- Log symptoms, mood, activities, cravings, energy levels, and more
- View all your logs in a calendar view with per-day popups
- See predictions for future periods, ovulation, and fertile windows
- Edit and delete period dates and symptom logs
- Cycle and period length averages displayed on the profile page

### Technical Implementation

- **Database layer** using Room (SQLite) with the following tables: `Cycles`, `CycleLogs`, `MoodLogs`, `SymptomLogs`, `ActivityLogs`, `CravingLogs`, `SexTypeLogs`, `EnergyLogs`
- **`PopulatedDailyLog`** in-memory data class using `@Embedded` and `@Relation` to aggregate daily log data across tables
- **Prediction logic** that calculates predicted period start, ovulation date, fertile window, average cycle length, and irregularity detection based on historical data
- CRUD operations for period dates and symptom logs
- Calendar view connected to the symptom logging flow
- Dynamic cycle wheel that adapts based on cycle phase lengths and the selected day

## Contributing

This is a university assignment and is not open for external contributions.

## License

No license.

## Contact

Diana Ivanova - diana.nik.ivanova@gmail.com 

Angelina Hess - cc241006@ustp-students.at