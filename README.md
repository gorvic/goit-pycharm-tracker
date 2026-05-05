# GoIT Tracker

A lightweight and reliable activity tracking plugin for PyCharm that measures your coding time and sends it to the GoIT API.

The plugin is designed to provide accurate session tracking, support offline work, and offer flexible activity detection modes depending on your workflow.

---

## Features

* Accurate coding session tracking
* Soft activity tracking (thinking, reading, navigation)
* Hard key press activity tracking (default)
* Offline queue with automatic retry
* Local session logging and statistics
* Reliable API communication with retry logic
* Fully configurable tracking behavior
* Native PyCharm UI integration

---

## Installation

### From JetBrains Marketplace

*(recommended once published)*

1. Open **Settings → Plugins**
2. Search for **GoIT Tracker**
3. Click **Install**

---

### Manual installation

1. Download plugin `.zip`
2. Open **Settings → Plugins**
3. Click **⚙ → Install Plugin from Disk**
4. Select the `.zip` file

---

## Configuration

Open:

```text
Settings → Tools → GoIT Tracker
```

### Required fields

* **User Token (UID)** — your GoIT identifier

---

## User Token (UID) Setup

The plugin requires a valid GoIT User Token (UID) to send tracking data to the API.

Since the UID is not generated inside PyCharm, it must be imported from the official GoIT VSCode extension.

### Recommended Method (Automatic Import)

The easiest and most reliable way to configure the UID is to import it from the GoIT VSCode plugin package.

**Steps:**
1. Download the official GoIT VSCode extension (.vsix file)
2. Open PyCharm: Settings → Tools → GoIT PyCharm Tracker
3. Click: Import from VSCode plugin (.vsix/.zip)
4. Select the downloaded .vsix file

#### What happens during import

**The plugin will:**
```text
✔ Extract package.json from the VSCode extension
✔ Read USER_TOKEN from extension configuration
✔ Apply compatible default settings:
  - event
  - eventType
  - endpoint
```
**After a successful import:**
```text
✔ UID is automatically filled
✔ No manual configuration is required
```
#### Important Notes

Only .vsix or .zip files are supported
The file must be a valid GoIT VSCode extension
If the UID cannot be found, the import will fail silently or show an error

### Alternative (Manual setup)

Manual configuration is technically possible, but not recommended, because:

- UID format is not documented
- Incorrect values will result in API errors

#### Recommendation
Always use VSCode plugin import to ensure correct configuration.

---

## Activity Tracking Modes

The plugin supports two fundamentally different tracking algorithms.

---

### 1. Hard Key Press Activity (default)

```text
Default mode: ENABLED
```

#### How it works

Tracking is triggered only by explicit coding actions:

* typing in the editor
* file save
* file creation and deletion

#### Behavior

```text
Typing → activity detected
No typing → idle timer starts
Idle timeout reached → session is closed
```

#### Pros

* High precision for actual coding time
* Minimal noise
* Predictable and strict behavior

#### Cons

* Does not count:
  * reading code
  * thinking
  * navigation

---

### 2. Soft Activity Tracking

```text
Optional mode (enabled in settings)
```

#### How it works

Tracking is triggered by any meaningful IDE interaction:

* typing
* caret movement
* scrolling
* file switching
* editor interaction

Additionally, a heartbeat mechanism keeps the session active while the user is still engaged.

#### Behavior

```text
User interacts → activity timestamp updated
User pauses → session continues until idle timeout
Idle timeout reached → session is closed
```

#### Pros

* Tracks real working time:

  * thinking
  * reading
  * navigation
* More realistic representation of development time

#### Cons

* May include passive activity
* Less strict than hard mode

---

## Hard vs Soft Comparison

| Feature                | Hard Mode | Soft Mode |
| ---------------------- |-----------| --------- |
| Keyboard activity      | ✔         | ✔         |
| Thinking time          | -         | ✔         |
| Scrolling / navigation | -         | ✔         |
| Session continuity     | Strict    | Flexible  |
| Noise level            | Low       | Medium    |
| Coding accuracy        | High      | Medium    |
| Real-world tracking    | Medium    | High      |

---

## Timing Parameters

### Idle Timeout

Defines how long inactivity is allowed before a session is closed.

Example:

```text
10 minutes → session closes after 10 minutes of inactivity
```

---

### Tick Interval

Internal timer frequency used to:

* check idle state
* extend sessions
* close sessions

---

### Flush Interval

Defines how often queued events are sent to the API.

---

## Offline Mode

If there is no internet connection:

```text
✔ sessions are stored locally
✔ events are queued
✔ automatic retry when connection is restored
```

No data is lost.

---

## Local Storage

The plugin stores:

* event queue
* session logs
* runtime data

Storage location is configurable.

---

## Data Safety

* No duplicate event sending
* Events are removed only after successful delivery
* Safe retry mechanism

---

## UI Integration

* Status bar widget for quick enable/disable
* Notifications for state changes and errors
* Integrated settings panel

---

## Recommended Usage

```text
Use HARD mode → for strict coding time tracking  
Use SOFT mode → for real working time tracking  
```

---

## Version

Current version: **1.6.0**

---

## Changelog

### 1.6.0

* Added Activity tracking mode setting with Hard key press activity and Soft activity options.
* Soft activity mode tracks caret movement, scrolling and file switching in addition to document edits.
* Soft activity mode counts thinking and reading time until the configured idle timeout.
* Hard key press activity remains the default and preserves the 1.5.0 behavior.

### 1.5.0

* Added status bar button visibility setting and activate-on-startup option.
* Exposed tracking timeout settings in the GUI.
* Added logging mode, maximum log size cleanup and custom data directory.
* Added clear logs, open sessions log, clear queue and API connection test actions.
* Added general and detailed local tracking statistics.

### 1.4.0

* Added VSCode plugin import for extracting user token and compatible defaults from package.json.
* Added Show logs and Open data folder buttons.
* Added detailed tracking diagnostics in tracker.log.
* Added explicit queue send/fail/remove logging.
* Added session start/activity/close logging.

### 1.3.0

* Added background delivery and persistent retry queue.
* Queued events are removed only after HTTP 2xx response.
* Added safe offline behavior and startup retry.

### 1.2.0

* Added GUI settings, status bar toggle, session logging, project-level tracking and multi-window support.

### 1.1.0

* Tracking starts disabled and requires manual enabling per project.

### 1.0.0

* Initial PyCharm implementation of GoIT-compatible tracking.

---

