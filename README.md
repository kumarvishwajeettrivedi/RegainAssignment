# Regain - Real-Time App Usage Limiter

 It helps users manage their screen time by monitoring app usage in real-time and enforcing self-selected time limits through a blocking overlay system.

## Features

- **Real-Time App Detection** - Instantly detects when any app is opened using AccessibilityService
- **Session-Based Limits** - Users choose usage duration (5/10/15/20 minutes) before opening restricted apps
- **Persistent Tracking** - Tracks total daily usage per app using both local database and system UsageStats
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
| Local Database | Room with KTX extensions |
| Async Operations | Kotlin Coroutines + Flow |
| Background Work | WorkManager |
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
| `BIND_ACCESSIBILITY_SERVICE` | Detect app launches in real-time |
| `POST_NOTIFICATIONS` | Show session timer notification |
| `FOREGROUND_SERVICE` | Keep tracking service alive |
| `QUERY_ALL_PACKAGES` | List all installed apps |
| `RECEIVE_BOOT_COMPLETED` | Reinitialize after device restart |

## Setup & Installation

1. Open the project in Android Studio (Hedgehog or later)

2. Sync Gradle files

3. Run on a physical device (recommended for full functionality)

4. Grant all 3 required permissions when prompted:
   - **Usage Access** - Required to track app usage statistics
   - **Display Over Other Apps** - Required to show blocking overlays
   - **Notifications** - Required to show foreground service notifications (Android 13+)

> **Note:** Accessibility Service is configured separately in Settings → Accessibility → Regain

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
│   └── UsageCheckReceiver.kt           # AlarmManager receiver for app checks
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

1. **App Detection**: `UsageCheckReceiver` triggers every ~2 seconds via `AlarmManager` to check the current foreground app using `UsageStatsManager`

2. **Session Initialization**: When a limited app is detected without an active session, `SoftBlockActivity` displays a bottom-sheet overlay prompting the user to select usage duration (5/10/15/20 minutes)

3. **In-Memory Tracking**: Selected session data is stored in `UsageRepositoryImpl`'s `activeSessions` ConcurrentHashMap for fast access and accurate time calculation

4. **Foreground Notification**: `UsageMonitorService` runs as a foreground service, displaying a notification with a countdown timer showing remaining session time

5. **Periodic Checks**: `UsageCheckReceiver` calculates elapsed time (`currentTime - lastChecked`), updates `totalSessionTime`, and persists to Room database every check

6. **Session Pause/Resume**: On screen lock, session state is saved to database and removed from memory. On unlock, it's restored with accurate remaining time

7. **Time's Up**: When session expires, `SoftBlockActivity` shows a full-screen blocking overlay with options to close the app or request an extension

8. **Daily Reset**: `MidnightResetWorker` (scheduled via WorkManager) resets all usage counters at midnight

9. **Data Persistence**: All session state, remaining time, and total daily usage are persisted in Room database for recovery after app restarts or device reboots

