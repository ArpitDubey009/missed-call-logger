# 📞 Missed Call Logger

An Android app that **automatically logs missed calls to Google Sheets** in real-time.

## Features
- ✅ Auto-detects missed calls in background
- ✅ Logs to Google Sheets via Apps Script webhook
- ✅ Auto-creates monthly sheets (Oct-2026, Nov-2026, etc.)
- ✅ Auto-increments serial numbers across all sheets
- ✅ Auto-updates Dashboard with stats & top callers
- ✅ Offline support — auto-syncs when internet returns
- ✅ Auto-restarts after phone reboot
- ✅ Auto-archives sheets older than 6 months
- ✅ Shows caller name from contacts

## Setup

### 1. Google Sheet
1. Create a new Google Sheet
2. Go to Extensions → Apps Script
3. Paste the contents of `google_apps_script.js`
4. Deploy as Web App (Execute as: Me, Access: Anyone)
5. Copy the deployment URL

### 2. Android App
1. Open this `android/` folder in Android Studio
2. Sync Gradle and build the project
3. Install on your Android phone
4. Paste the Apps Script URL in the app
5. Tap "Start Monitoring"

### 3. Test
- Call your phone from another phone, don't answer
- Check your Google Sheet — the missed call appears!

## Tech Stack
- **Android**: Kotlin, BroadcastReceiver, Foreground Service
- **Backend**: Google Apps Script (JavaScript)
- **Database**: Google Sheets
- **Networking**: OkHttp3

## Permissions Required
| Permission | Reason |
|-----------|--------|
| READ_PHONE_STATE | Detect incoming/missed calls |
| READ_CALL_LOG | Read call details |
| READ_CONTACTS | Get caller name |
| INTERNET | Send data to Google Sheets |
| RECEIVE_BOOT_COMPLETED | Auto-start on reboot |
| FOREGROUND_SERVICE | Keep monitoring alive |

## License
MIT License — Free to use and modify.
