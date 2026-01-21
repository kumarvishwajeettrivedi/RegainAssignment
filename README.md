# Regain - Real-Time App Usage Limiter

 It helps users manage their screen time by monitoring app usage in real-time and enforcing self-selected time limits through a blocking overlay system.

## Features

- **Real-Time App Detection** - Detects foreground apps using `UsageStatsManager` and a Foreground Service
- **Session-Based Limits** - Users choose usage duration (5/10/15/20 minutes) before opening restricted apps
- **Persistent Tracking** - Tracks total daily usage per app using local database
- **Blocking Overlays** - Full-screen overlays prevent app usage after time expires
- **Pause on Screen Lock** - Sessions automatically pause when screen is off and resume on unlock
- **Midnight Reset** - All usage counters reset at midnight via WorkManager
- **Foreground Notification** - Shows remaining session time in status bar

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
│  │ UsageMonitorService        │  │ UsageCheckReceiver        │  │
│  │ - Foreground Service Loop  │  │ - Logic Hub               │  │
│  │ - Status Notification      │  │ - App Detection           │  │
│  │                            │  │ - Triggers Block Logic    │  │
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
| Local Database | Room with KTX extensions |
| Async Operations | Kotlin Coroutines + Flow |
| Background Work | WorkManager + Foreground Service |
| Navigation | Hilt Navigation Compose |
| Data Serialization | Gson |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 36 (Android 16) |
| Compile SDK | 36 |

### Key Dependencies

- **AndroidX Core KTX** - Kotlin extensions for Android framework
- **AndroidX Lifecycle Runtime KTX** - Lifecycle-aware components
- **Jetpack Compose BOM** - Bill of Materials for Compose versioning
- **Room Database** - `room-runtime`, `room-ktx`, `room-compiler`
- **Dagger Hilt** - `hilt-android`, `hilt-compiler`, `hilt-work`, `hilt-navigation-compose`
- **WorkManager** - `work-runtime-ktx` for background task scheduling
- **Gson** - JSON serialization/deserialization

## Required Permissions

| Permission | Purpose |
|------------|---------|
| `PACKAGE_USAGE_STATS` | Query app usage statistics |
| `SYSTEM_ALERT_WINDOW` | Display overlay on top of apps |
| `FOREGROUND_SERVICE` | Keep tracking service alive |
| `POST_NOTIFICATIONS` | Show session timer notification |
| `QUERY_ALL_PACKAGES` | List all installed apps |
| `RECEIVE_BOOT_COMPLETED` | Reinitialize after device restart |
| `SCHEDULE_EXACT_ALARM` | Precise checks for real-time accuracy |

## Setup & Installation

1. Open the project in Android Studio (Hedgehog or later)

2. Sync Gradle files

3. Run on a physical device (recommended for full functionality)

4. Grant all required permissions when prompted:
   - **Usage Access** - Required to track app usage statistics
   - **Display Over Other Apps** - Required to show blocking overlays
   - **Notifications** - Required to show foreground service notifications (Android 13+)

## Project Structure

```
app/src/main/java/com/example/regainassignment/
├── MainActivity.kt                     # Entry point, navigation setup
├── RegainApplication.kt                # Hilt setup, WorkManager scheduling
├── data/
│   ├── local/
│   │   ├── AppEntity.kt                # Room entity with session state
│   │   ├── AppDao.kt                   # Room DAO with session queries
│   │   └── AppDatabase.kt              # Room database singleton
│   └── repository/
│       ├── UsageRepository.kt          # Repository interface
│       └── UsageRepositoryImpl.kt      # Implementation with in-memory cache
├── di/
│   ├── DatabaseModule.kt               # Hilt database module
│   └── RepositoryModule.kt             # Hilt repository module
├── service/
│   └── UsageMonitorService.kt          # Foreground service with notification timer
├── receiver/
│   ├── BootReceiver.kt                 # Boot completed receiver
│   └── UsageCheckReceiver.kt           # BroadcastReceiver for app detection & logic
├── ui/
│   ├── AppListScreen.kt                # Main app list with toggle switches
│   ├── HomeViewModel.kt                # ViewModel for app list
│   ├── PermissionsScreen.kt            # Permission request screen
│   ├── block/
│   │   └── SoftBlockActivity.kt        # Blocking overlay activity
│   └── theme/
│       ├── Color.kt                    # Material 3 color scheme
│       ├── Theme.kt                    # App theme configuration
│       └── Type.kt                     # Typography definitions
├── util/
│   ├── PermissionUtils.kt              # Permission check helpers
│   └── TimeFormatter.kt                # Time display utilities
└── worker/
    └── MidnightResetWorker.kt          # Daily usage reset at midnight
```

## How It Works

1. **Continuous Monitoring**: `UsageMonitorService` runs as a foreground service, ensuring the app remains active. It triggers `UsageCheckReceiver` every 2 seconds via broadcast.

2. **App Detection**: `UsageCheckReceiver` queries `UsageStatsManager` to identify the current foreground app and determine if it has changed.

3. **Session Logic**: 
   - If a **restricted app** is detected:
     - **No Active Session**: Launches `SoftBlockActivity` (bottom sheet) to ask the user for a duration.
     - **Active Session**: Updates the session timer in `AppDatabase` and `UsageRepository`.
     - **Time Expired**: Launches `SoftBlockActivity` (full screen) to block access.

4. **Foreground Notification**: `UsageMonitorService` maintains a notification with a real-time countdown. It handles the visual timer locally for smoothness while synchronizing with the database states.

5. **Pause/Resume**:
   - **Screen Lock**: The receiver detects non-interactive state and pauses the session.
   - **App Switch**: Switching to another app automatically pauses the session of the previous app.

6. **Daily Reset**: `MidnightResetWorker` (scheduled via WorkManager) resets 'Usage Today' counters at midnight.

7. **Data Persistence**: All session states, limits, and usage logs are stored in Room database to survive app restarts and device reboots.

