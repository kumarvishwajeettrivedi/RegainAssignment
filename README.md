# Regain - Real-Time App Usage Limiter

A native Android application that helps users manage their screen time by monitoring app usage in real-time and enforcing self-selected time limits through a blocking overlay system.

## Features

- **Real-Time App Detection** - Instantly detects when any app is opened using AccessibilityService
- **Session-Based Limits** - Users choose usage duration (5/10/15/20 minutes) before opening restricted apps
- **Persistent Tracking** - Tracks total daily usage per app using both local database and system UsageStats
- **Blocking Overlays** - Full-screen overlays prevent app usage after time expires
- **Pause on Screen Lock** - Sessions automatically pause when screen is off and resume on unlock
- **Midnight Reset** - All usage counters reset at midnight via WorkManager
- **Foreground Notification** - Shows remaining session time in status bar

## Screenshots

*Coming soon*

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      Presentation Layer                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐   │
│  │ MainActivity │  │ AppListScreen│  │ PermissionsScreen    │   │
│  └──────────────┘  └──────────────┘  └──────────────────────┘   │
│                           │                                      │
│                    ┌──────────────┐                              │
│                    │ HomeViewModel│                              │
│                    └──────────────┘                              │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────────┐
│                       Service Layer                              │
│  ┌────────────────────────────┐  ┌───────────────────────────┐  │
│  │ CoreAccessibilityService   │  │ OverlayManager            │  │
│  │ - Real-time app detection  │  │ - Blocker overlay         │  │
│  │ - Session tracking         │  │ - Time's up overlay       │  │
│  │ - Foreground notification  │  │ - Gatekeeper overlay      │  │
│  └────────────────────────────┘  └───────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────────┐
│                        Data Layer                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐   │
│  │UsageRepository│  │  AppDatabase │  │ AppEntity            │   │
│  │ (Interface)   │  │  (Room)      │  │ - packageName        │   │
│  └──────────────┘  └──────────────┘  │ - totalUsageToday    │   │
│         │                            │ - isLimitEnabled     │   │
│  ┌──────────────┐                    │ - remainingSessionTime│   │
│  │UsageRepoImpl │                    └──────────────────────┘   │
│  └──────────────┘                                                │
└─────────────────────────────────────────────────────────────────┘
```

## Tech Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin |
| UI Framework | Jetpack Compose |
| Design System | Material 3 |
| Architecture | MVVM + Repository Pattern |
| Dependency Injection | Dagger Hilt |
| Local Database | Room |
| Async Operations | Kotlin Coroutines + Flow |
| Background Work | WorkManager |
| Navigation | Compose Navigation |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 36 (Android 16) |

## Required Permissions

| Permission | Purpose |
|------------|---------|
| `PACKAGE_USAGE_STATS` | Query app usage statistics |
| `SYSTEM_ALERT_WINDOW` | Display overlay on top of apps |
| `BIND_ACCESSIBILITY_SERVICE` | Detect app launches in real-time |
| `POST_NOTIFICATIONS` | Show session timer notification |
| `FOREGROUND_SERVICE` | Keep tracking service alive |
| `QUERY_ALL_PACKAGES` | List all installed apps |
| `RECEIVE_BOOT_COMPLETED` | Reinitialize after device restart |

## Setup & Installation

1. Clone the repository
```bash
git clone https://github.com/yourusername/regain-assignment.git
```

2. Open in Android Studio (Arctic Fox or later)

3. Sync Gradle files

4. Run on a physical device (Accessibility testing is limited on emulators)

5. Grant all 4 permissions when prompted:
   - Usage Access
   - Display Over Other Apps
   - Accessibility Service
   - Notifications (Android 13+)

## Project Structure

```
app/src/main/java/com/example/regainassignment/
├── MainActivity.kt                 # Entry point, navigation setup
├── RegainApplication.kt            # Hilt setup, WorkManager scheduling
├── data/
│   ├── local/
│   │   ├── AppEntity.kt            # Room entity
│   │   ├── AppDao.kt               # Room DAO
│   │   └── AppDatabase.kt          # Room database
│   └── repository/
│       ├── UsageRepository.kt      # Repository interface
│       └── UsageRepositoryImpl.kt  # Implementation
├── di/
│   ├── DatabaseModule.kt           # Hilt DB module
│   └── RepositoryModule.kt         # Hilt Repository module
├── service/
│   ├── CoreAccessibilityService.kt # Main tracking service
│   └── OverlayManager.kt           # Overlay window management
├── ui/
│   ├── AppListScreen.kt            # Main app list
│   ├── HomeViewModel.kt            # ViewModel
│   ├── PermissionsScreen.kt        # Permission requests
│   └── overlay/
│       └── OverlayUI.kt            # Overlay Compose components
├── util/
│   └── PermissionUtils.kt          # Permission helpers
├── worker/
│   └── MidnightResetWorker.kt      # Daily reset worker
└── receiver/
    └── BootReceiver.kt             # Boot completed receiver
```

## How It Works

1. **App Detection**: `CoreAccessibilityService` listens for `TYPE_WINDOW_STATE_CHANGED` events to detect when any app comes to foreground

2. **Limit Check**: When a limited app is opened, the service checks if there's remaining session time

3. **Time Selection**: If no active session, a bottom-sheet overlay prompts the user to select usage duration

4. **Countdown**: Session time counts down in memory, with periodic saves to Room database

5. **Time's Up**: When session expires, a blocking overlay appears with options to close or extend

6. **Data Persistence**: Usage data is persisted in Room and synced with system UsageStats

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Built as an assignment for Regain
- Inspired by Digital Wellbeing and screen time management apps
