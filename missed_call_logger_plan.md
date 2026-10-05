# 📱 Missed Call Logger — Android App + Google Sheets (Real-Time)

## 💡 The Idea

**"MissedCall Logger"** — A lightweight Android app that runs silently in the background. Whenever you miss a call, it **automatically logs** the caller's name/number, date, time, and call duration to a **Google Sheet** in real-time. No manual input needed.

---

## 🏗️ Architecture Overview

```mermaid
graph LR
    A["📞 Incoming Call"] --> B["Android BroadcastReceiver"]
    B --> C{"Call Missed?"}
    C -->|Yes| D["Extract Caller Info"]
    D --> E["HTTP POST to Google Apps Script"]
    E --> F["Google Apps Script Web App"]
    F --> G["📊 Google Sheet Updated"]
    C -->|No (Answered)| H["Do Nothing"]
```

> [!IMPORTANT]
> We use **Google Apps Script as a middleware webhook** — this eliminates complex OAuth setup on the Android side. The Android app simply sends an HTTP POST request.

---

## 🧩 Two Main Components

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Android App** | Kotlin + Android Studio | Detects missed calls, sends data |
| **Google Apps Script** | JavaScript (Apps Script) | Receives data, writes to Google Sheet |

---

## 📋 Step-by-Step Implementation

### **PHASE 1: Google Sheet + Apps Script Setup (15 mins)**

#### Step 1.1 — Create the Google Sheet
1. Go to [sheets.google.com](https://sheets.google.com)
2. Create a new spreadsheet named **"Missed Call Log"**
3. In Row 1, add these headers:

| A | B | C | D | E |
|---|---|---|---|---|
| **Sr. No** | **Phone Number** | **Caller Name** | **Date** | **Time** |

4. Rename the sheet tab to **"MissedCalls"**

#### Step 1.2 — Create the Apps Script
1. In your Google Sheet → click **Extensions → Apps Script**
2. Delete the default code and paste the script (see `google_apps_script.js` file)
3. Click **Deploy → New Deployment**
4. Select type: **Web App**
5. Set:
   - Execute as: **Me**
   - Who has access: **Anyone**
6. Click **Deploy** and **copy the Web App URL**

> [!CAUTION]
> Keep this URL secret! Anyone with this URL can write to your sheet.

---

### **PHASE 2: Android App Development (45 mins)**

#### Step 2.1 — Create Android Studio Project
1. Open Android Studio → **New Project**
2. Select **Empty Activity**
3. Configure:
   - Name: `MissedCallLogger`
   - Package: `com.yourname.missedcalllogger`
   - Language: **Kotlin**
   - Minimum SDK: **API 26 (Android 8.0)**

#### Step 2.2 — Add Permissions to `AndroidManifest.xml`
```xml
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.READ_CALL_LOG" />
<uses-permission android:name="android.permission.READ_CONTACTS" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_PHONE_CALL" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

#### Step 2.3 — Key Files to Create

| File | Purpose |
|------|---------|
| `CallReceiver.kt` | BroadcastReceiver to detect phone state changes |
| `MissedCallService.kt` | Foreground service to keep monitoring alive |
| `GoogleSheetHelper.kt` | HTTP POST helper to send data to Apps Script |
| `MainActivity.kt` | UI to toggle service ON/OFF + show status |
| `BootReceiver.kt` | Auto-start service after device reboot |

#### Step 2.4 — Add Dependencies to `build.gradle`
```groovy
dependencies {
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.google.code.gson:gson:2.10.1'
    implementation 'androidx.work:work-runtime-ktx:2.9.0'
}
```

---

### **PHASE 3: Testing & Deployment (15 mins)**

1. Install on your Android phone via USB debugging
2. Grant all permissions when prompted
3. Start the service from the app
4. Call your phone from another phone → don't answer
5. Check your Google Sheet — the missed call should appear! ✅

---

## 🔒 Permissions Explained

| Permission | Why Needed |
|-----------|-----------|
| `READ_PHONE_STATE` | Detect incoming/missed calls |
| `READ_CALL_LOG` | Read missed call details |
| `READ_CONTACTS` | Get caller name from contacts |
| `INTERNET` | Send data to Google Sheets |
| `RECEIVE_BOOT_COMPLETED` | Auto-start after phone reboot |
| `FOREGROUND_SERVICE` | Keep the service running reliably |

---

## 📊 Google Sheet Output Example

| Sr. No | Phone Number | Caller Name | Date | Time |
|--------|-------------|-------------|------|------|
| 1 | +91 98765 43210 | Rahul Sharma | 2026-10-05 | 11:45:30 AM |
| 2 | +91 87654 32109 | Unknown | 2026-10-05 | 12:02:15 PM |
| 3 | +91 76543 21098 | Mom | 2026-10-05 | 01:30:45 PM |

---

## 🚀 Future Enhancements

- **WhatsApp notification** when a call is missed
- **Auto-callback scheduling** for missed calls
- **Dashboard** in the app showing missed call statistics
- **Multiple Google Sheet support** for different categories
- **SMS auto-reply** to missed callers
