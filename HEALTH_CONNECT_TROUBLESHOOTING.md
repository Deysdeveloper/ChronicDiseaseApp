# Health Connect Troubleshooting Guide

## Problem: App has access to Health Connect but not reading blood pressure data

### Symptoms

- Health Connect permissions are granted in the app
- App shows "76" readings in Health Connect
- App is showing sample/demo data instead of real Health Connect data
- Blood pressure values are not being displayed from Health Connect

---

## Step-by-Step Troubleshooting

### 1. **Check Logcat for Detailed Information**

The app now includes comprehensive logging. Filter logcat by the tag `HealthDataRepository` to see
detailed information:

```bash
adb logcat | grep HealthDataRepository
```

Look for these log messages:

- `"=== PERMISSION CHECK ==="` - Shows permission status
- `"getBloodPressureData: Starting blood pressure data retrieval"` - Start of BP fetch
- `"getBloodPressureData: Permission check result: true/false"` - Permission status
- `"getBloodPressureData: Received X records from Health Connect"` - Number of records found
- `"getBloodPressureData: No blood pressure records found"` - No data available
- `"Generating sample blood pressure data"` - Fallback to sample data

### 2. **Verify Health Connect Installation and Data**

**Check if Health Connect has data:**

1. Open the **Health Connect** app on your device
2. Go to **Data and access** → **Browse data**
3. Select **Vitals** → **Blood pressure**
4. Verify that blood pressure readings are actually present
5. Check the date range of the readings (app queries last 30 days)

**Important:** If you added data through Samsung Health, make sure:

- Samsung Health is set up to sync with Health Connect
- Open Samsung Health → Settings → Connected services → Health Connect
- Enable sync for Blood Pressure data

### 3. **Verify App Permissions in Health Connect**

1. Open **Health Connect** app
2. Go to **Apps and devices**
3. Find your **Chronic Disease App**
4. Verify these permissions are granted:
    - ✅ Blood Pressure (Read)
    - ✅ Heart Rate (Read)
    - ✅ Oxygen Saturation (Read)
    - ✅ Steps (Read)

### 4. **Force Refresh the App**

In the app:

1. Click the **Refresh button** (circular arrow icon) on the dashboard
2. Click the **Settings/Debug button** (gear icon) next to refresh
3. Check the logcat for permission and data retrieval logs

### 5. **Check Time Range**

The app queries data from different time ranges:

- **Blood Pressure**: Last 30 days
- **Heart Rate**: Last 24 hours
- **SpO2**: Last 24 hours
- **Steps**: Last 24 hours

If your blood pressure reading is older than 30 days, it won't be retrieved. You can modify this in
the code if needed.

### 6. **Common Issues and Solutions**

#### Issue: "Health Connect client is null"

**Solution:** Health Connect SDK not available on device

- Ensure device has Android 14+ or Health Connect app installed
- Update Health Connect app from Play Store

#### Issue: "Permissions not granted"

**Solution:** Re-grant permissions

1. Go to Settings → Apps → Chronic Disease App → Permissions
2. Remove all permissions
3. Open the app again and re-grant when prompted

#### Issue: "No blood pressure records found"

**Solution:** Data not synced to Health Connect

1. Verify data exists in Health Connect app (not just Samsung Health)
2. Try manual sync: Samsung Health → Settings → Connected services → Health Connect → Sync now
3. Add a new blood pressure reading in Samsung Health and wait a few minutes

#### Issue: "Using Sample Data"

**Solution:** This appears when:

- No permissions granted
- No data available in Health Connect
- Error occurred during data fetch (check logs)

### 7. **Check Data Sources in Health Connect**

Health Connect may have data from multiple sources. Ensure your primary source (Samsung Health,
Galaxy Watch 4, etc.) is properly connected:

1. Open **Health Connect**
2. Go to **Apps and devices**
3. Verify **Samsung Health** is listed and has permissions
4. Check if priority source is set correctly

### 8. **Manual Permission Check via ADB**

You can check which permissions the app has:

```bash
adb shell dumpsys package com.example.chronicdiseaseapp | grep permission
```

Look for:

- `android.permission.health.READ_BLOOD_PRESSURE`
- `android.permission.health.READ_HEART_RATE`
- `android.permission.health.READ_OXYGEN_SATURATION`

---

## Code Modifications for Extended Time Range

If you need to query blood pressure data from a longer period, modify `HealthDataRepository.kt`:

```kotlin
// Change from 30 days to 90 days
val startTime = endTime.minusSeconds(90 * 24 * 60 * 60) // 90 days ago
```

---

## Debugging Commands

### View all Health Connect data via ADB:

```bash
adb shell content query --uri content://com.google.android.apps.healthdata/records/BloodPressureRecord
```

### Clear app data and restart:

```bash
adb shell pm clear com.example.chronicdiseaseapp
adb shell am start -n com.example.chronicdiseaseapp/.MainActivity
```

### Check Health Connect service status:

```bash
adb shell dumpsys activity service HealthConnectService
```

---

## Expected Log Output (Success Case)

When working correctly, you should see:

```
D/HealthDataRepository: getBloodPressureData: Starting blood pressure data retrieval
D/HealthDataRepository: === PERMISSION CHECK ===
D/HealthDataRepository: Permission Blood Pressure granted: true
D/HealthDataRepository: ======================
D/HealthDataRepository: getBloodPressureData: Permission check result: true
D/HealthDataRepository: getBloodPressureData: Querying Health Connect from [start] to [end]
D/HealthDataRepository: getBloodPressureData: Received 76 records from Health Connect
D/HealthDataRepository: getBloodPressureData: Reading 0 - 120/80 at [timestamp]
D/HealthDataRepository: getBloodPressureData: Successfully retrieved 76 blood pressure readings from Health Connect
```

---

## Contact & Support

If the issue persists after following all steps:

1. Save the logcat output: `adb logcat > debug.log`
2. Take screenshots of Health Connect permissions
3. Check if other health apps can read the same data
4. Verify device compatibility (Android 14+ or Health Connect app installed)

---

## Quick Fix Checklist

- [ ] Health Connect app is installed and updated
- [ ] Blood pressure data exists in Health Connect (not just Samsung Health)
- [ ] App has all required permissions in Health Connect
- [ ] Data is within the query time range (last 30 days)
- [ ] Samsung Health is synced with Health Connect
- [ ] App has been force-refreshed using the refresh button
- [ ] Logcat shows detailed debug information
- [ ] Device is running Android 14+ or has Health Connect app

---

## Next Steps

Run the app and click the refresh button while monitoring logcat:

```bash
adb logcat -s HealthDataRepository:D HealthDataViewModel:D
```

This will show you exactly what's happening during the data retrieval process.
