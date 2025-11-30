# Logcat Monitoring Guide - Check Health Data Fetching

This guide shows you how to monitor logcat to see if your app is fetching data from Health Connect.

---

## Method 1: Automated Script (Recommended) 🚀

We've created a script that does everything automatically!

### Run the Script:

```bash
cd /Users/debojyotidey/AndroidStudioProjects/ChronicDiseaseApp
./check_health_data_logs.sh
```

**What it does:**

1. ✅ Clears old logs
2. ✅ Starts your app
3. ✅ Monitors data fetching
4. ✅ Shows a detailed summary report
5. ✅ Gives you specific recommendations

**Output will show:**

- Permission status (granted or not)
- How many blood pressure records were fetched
- Heart rate, SpO2, and steps data status
- Whether using real data or sample data
- Specific actions to fix issues

---

## Method 2: Manual Commands (Step by Step) 📱

If you prefer to run commands manually:

### Step 1: Clear Logs

```bash
adb logcat -c
```

### Step 2: Start the App

```bash
adb shell am start -n com.example.chronicdiseaseapp/.MainActivity
```

### Step 3: Monitor Live Logs

```bash
adb logcat | grep -E "(HealthDataRepository|HealthDataViewModel)"
```

**Keep this running and watch the output!**

---

## Method 3: Quick Check (One Command) ⚡

Run app and check logs in one go:

```bash
adb logcat -c && \
adb shell am start -n com.example.chronicdiseaseapp/.MainActivity && \
sleep 3 && \
adb logcat -d | grep -E "(HealthDataRepository|HealthDataViewModel)" | tail -50
```

---

## What to Look For in Logs 🔍

### ✅ SUCCESS - Data is Being Fetched:

```
D/HealthDataRepository: Required permissions granted: true
D/HealthDataRepository: getBloodPressureData: Received 76 records from Health Connect
D/HealthDataRepository: getBloodPressureData: Reading 0 - 120/80 at [timestamp]
D/HealthDataRepository: getBloodPressureData: Successfully retrieved 76 blood pressure readings
```

**This means:** 🎉

- ✅ Permissions are working
- ✅ Health Connect has data
- ✅ App is reading real data
- ✅ Your blood pressure readings are showing

---

### ⚠️ WARNING - No Data Found:

```
D/HealthDataRepository: Required permissions granted: true
D/HealthDataRepository: getBloodPressureData: Received 0 records from Health Connect
W/HealthDataRepository: getBloodPressureData: No blood pressure records found in Health Connect
W/HealthDataRepository: Generating sample blood pressure data - no real data available
```

**This means:**

- ✅ Permissions are OK
- ❌ Health Connect has no data
- ⚠️ App is using sample data
- 💡 **Action:** Sync Samsung Health to Health Connect

---

### ❌ ERROR - Permissions Not Granted:

```
D/HealthDataRepository: Required permissions granted: false
W/HealthDataRepository: getBloodPressureData: Permissions not granted, using sample data
```

**This means:**

- ❌ Required permissions missing
- ⚠️ App cannot access Health Connect
- 💡 **Action:** Grant permissions in Health Connect app

---

## Specific Log Patterns to Check

### 1. Permission Check

Look for this section:

```
=== PERMISSION CHECK ===
Required permissions granted: true/false
--- Required Permissions ---
  Heart Rate: true
  Blood Pressure: true
  Oxygen Saturation (SpO2): true
  Steps: true
--- Optional Permissions ---
  Active Calories: false (optional - not required)
======================
```

**Good:** `Required permissions granted: true`  
**Bad:** `Required permissions granted: false`

---

### 2. Blood Pressure Fetch

```
getBloodPressureData: Starting blood pressure data retrieval
getBloodPressureData: Permission check result: true
getBloodPressureData: Querying Health Connect from [start] to [end]
getBloodPressureData: Received X records from Health Connect
```

**Good:** `Received 76 records` (or any number > 0)  
**Bad:** `Received 0 records`

---

### 3. Data Source Indicator

```
source = "Health Connect"  ← Real data ✅
source = "Sample Data (No Health Connect Data)"  ← Sample data ⚠️
```

---

## Filter by Specific Data Type

### Check Blood Pressure Only:

```bash
adb logcat | grep "getBloodPressureData"
```

### Check Heart Rate Only:

```bash
adb logcat | grep "getHeartRateData"
```

### Check SpO2 Only:

```bash
adb logcat | grep "getSpO2Data"
```

### Check Steps Only:

```bash
adb logcat | grep "getStepsData"
```

---

## Save Logs to File

### Save Current Logs:

```bash
adb logcat -d > health_logs.txt
```

### Save and Filter:

```bash
adb logcat -d | grep -E "(HealthDataRepository|HealthDataViewModel)" > health_logs_filtered.txt
```

### View Saved Logs:

```bash
cat health_logs_filtered.txt
```

---

## Real-Time Monitoring with Refresh

Monitor logs and refresh the app every 10 seconds:

```bash
while true; do
  echo "=== Refreshing app ==="
  adb shell am start -n com.example.chronicdiseaseapp/.MainActivity
  sleep 3
  adb logcat -d | grep -E "(HealthDataRepository|HealthDataViewModel)" | tail -20
  sleep 10
done
```

Press `Ctrl+C` to stop.

---

## Troubleshooting Based on Logs

### If you see: "Health Connect client is null"

**Problem:** Health Connect SDK not available  
**Solution:**

- Install Health Connect app from Play Store
- Update to Android 14+ (has built-in Health Connect)

### If you see: "Required permissions granted: false"

**Problem:** Permissions not granted  
**Solution:**

1. Open Health Connect app
2. Go to Apps and devices → Chronic Disease App
3. Grant all required permissions
4. Restart app

### If you see: "Received 0 records from Health Connect"

**Problem:** No data in Health Connect  
**Solution:**

1. Open Samsung Health
2. Go to Settings → Health Connect
3. Enable data sharing for Blood Pressure, Heart Rate, SpO2, Steps
4. Tap "Sync now"
5. Verify data in Health Connect app
6. Refresh your Chronic Disease App

### If you see: "Permission X granted: false"

**Problem:** Specific permission missing  
**Solution:** Grant that specific permission in Health Connect

---

## Understanding the Log Flow

When app starts, you should see this sequence:

```
1. Health Connect initialized successfully
2. getHeartRateData: Starting heart rate data retrieval
3. === PERMISSION CHECK ===
4. Required permissions granted: true
5. getHeartRateData: Querying Health Connect from [date] to [date]
6. getHeartRateData: Successfully retrieved X readings
7. [Same for SpO2, Blood Pressure, Steps]
8. Updated health metrics: HR=X, BP=X/X, SpO2=X, Steps=X
```

**If any step fails, the log will show the reason.**

---

## Quick Diagnostics Commands

### Check if app is installed:

```bash
adb shell pm list packages | grep chronicdiseaseapp
```

### Check app permissions:

```bash
adb shell dumpsys package com.example.chronicdiseaseapp | grep permission
```

### Check Health Connect status:

```bash
adb shell dumpsys activity service HealthConnectService
```

### Force stop and restart app:

```bash
adb shell am force-stop com.example.chronicdiseaseapp
adb shell am start -n com.example.chronicdiseaseapp/.MainActivity
```

---

## Live Dashboard Monitor

Create a live monitoring dashboard:

```bash
watch -n 5 'adb logcat -d | grep "HealthDataRepository" | tail -20'
```

This updates every 5 seconds showing the last 20 relevant log lines.

---

## Color-Coded Log Viewer (Advanced)

For better readability:

```bash
adb logcat | grep -E "(HealthDataRepository|HealthDataViewModel)" | \
  sed 's/Successfully retrieved/\x1b[32m&\x1b[0m/g' | \
  sed 's/Received 0 records/\x1b[31m&\x1b[0m/g' | \
  sed 's/Permission.*granted: true/\x1b[32m&\x1b[0m/g' | \
  sed 's/Permission.*granted: false/\x1b[31m&\x1b[0m/g'
```

**Colors:**

- 🟢 Green = Success messages
- 🔴 Red = Errors/failures

---

## Summary Checklist

Use logs to verify:

- [ ] Health Connect initialized successfully
- [ ] All required permissions granted: true
- [ ] Blood Pressure: Received > 0 records
- [ ] Heart Rate: Successfully retrieved readings
- [ ] SpO2: Successfully retrieved readings
- [ ] Steps: Successfully retrieved data
- [ ] No "Sample Data" warnings
- [ ] Source shows "Health Connect" not "Sample Data"

---

## Example: Complete Success Log

```
11-29 10:38:53.543 D/HealthDataRepository: Health Connect initialized successfully
11-29 10:38:53.544 D/HealthDataRepository: === PERMISSION CHECK ===
11-29 10:38:53.544 D/HealthDataRepository: Required permissions granted: true
11-29 10:38:53.544 D/HealthDataRepository: --- Required Permissions ---
11-29 10:38:53.544 D/HealthDataRepository:   Heart Rate: true
11-29 10:38:53.544 D/HealthDataRepository:   Blood Pressure: true
11-29 10:38:53.544 D/HealthDataRepository:   Oxygen Saturation (SpO2): true
11-29 10:38:53.544 D/HealthDataRepository:   Steps: true
11-29 10:38:53.719 D/HealthDataRepository: getBloodPressureData: Received 76 records
11-29 10:38:53.720 D/HealthDataRepository: Reading 0 - 120/80 at Fri Nov 29 10:30:00
11-29 10:38:53.728 D/HealthDataRepository: Successfully retrieved 76 blood pressure readings
```

**This is perfect! ✅ Everything is working!**

---

## Next Steps

1. **Run the automated script:** `./check_health_data_logs.sh`
2. **Read the report** it generates
3. **Follow the recommendations** it provides
4. **Check the saved logs:** `cat health_data_logs.txt`

---

**Need help interpreting logs? Share the output and we can diagnose the issue!**
