# How to Sync Samsung Health Data to Health Connect

## Current Status ✅

Your app now has the correct permissions and is successfully querying Health Connect. The logs show:

- ✅ All required permissions granted
- ✅ Health Connect client initialized successfully
- ✅ App is querying Health Connect for blood pressure data (last 30 days)
- ❌ **No blood pressure records found in Health Connect** (0 records received)

## The Problem

You mentioned having 76 blood pressure readings, but the app is receiving **0 records** from Health
Connect. This means:

- The data exists in **Samsung Health** app
- The data is **NOT synced** to **Health Connect** yet

---

## Solution: Sync Samsung Health to Health Connect

### Step 1: Open Samsung Health

1. Open the **Samsung Health** app on your device
2. Make sure you're logged in

### Step 2: Connect to Health Connect

1. In Samsung Health, tap the **menu icon** (three horizontal lines) or **Profile** icon
2. Go to **Settings** ⚙️
3. Scroll down and find **Connected services** or **Health Connect**
4. Tap on **Health Connect**

### Step 3: Enable Data Sharing

You should see a list of data types that can be synced:

- ✅ **Blood Pressure** - Enable this
- ✅ **Heart Rate** - Enable this
- ✅ **Oxygen Saturation (SpO2)** - Enable this
- ✅ **Steps** - Enable this
- ✅ **Active Calories** - Enable this (optional)

Make sure all the data types you want to sync are **toggled ON**.

### Step 4: Trigger Manual Sync

1. In the Health Connect settings within Samsung Health
2. Look for a **"Sync now"** button or similar option
3. Tap it to trigger immediate synchronization
4. Wait for the sync to complete (may take a few minutes for 76 readings)

### Step 5: Verify Data in Health Connect App

1. Open the **Health Connect** app (the standalone app)
2. Tap **Data and access** → **Browse data**
3. Go to **Vitals** → **Blood pressure**
4. You should now see your 76 blood pressure readings listed here
5. Check the dates to confirm they're within the last 30 days (app queries 30 days of BP data)

### Step 6: Refresh Your Chronic Disease App

1. Open your **Chronic Disease App**
2. Tap the **Refresh button** (circular arrow icon) on the dashboard
3. The app should now pull the real blood pressure data from Health Connect

---

## Alternative Method: Direct Health Connect Setup

If Samsung Health doesn't have a clear Health Connect option:

### Method A: Through Health Connect App

1. Open **Health Connect** app
2. Go to **Apps and devices**
3. Tap **Samsung Health**
4. Enable data sharing for all health data types
5. Return to Samsung Health and check if data appears in Health Connect

### Method B: Permissions Management

1. Go to **Settings** on your device
2. **Apps** → **Health Connect**
3. Check **Permissions** and **Data sharing**
4. Ensure Samsung Health has permission to write to Health Connect

---

## Troubleshooting

### Issue: Can't find Health Connect in Samsung Health settings

**Solution:**

- Update Samsung Health to the latest version from Galaxy Store or Play Store
- Update Health Connect app from Play Store
- Some older versions of Samsung Health may not support Health Connect

### Issue: Data syncs but app still shows 0 records

**Check the date range:**

- The app queries **last 30 days** for blood pressure
- If your 76 readings are older than 30 days, they won't appear
- Solution: Update the code to query a longer time range (see code modification below)

### Issue: Sync is stuck or not working

**Try these steps:**

1. Force stop Samsung Health: Settings → Apps → Samsung Health → Force stop
2. Force stop Health Connect: Settings → Apps → Health Connect → Force stop
3. Open Health Connect first, then open Samsung Health
4. Try the sync again

### Issue: Only recent data syncs, old data missing

- Health Connect may prioritize recent data
- Try adding a new blood pressure reading in Samsung Health
- Check if the new reading appears in your app (proves sync is working)

---

## Code Modification: Extend Time Range

If your blood pressure readings are older than 30 days, you can modify the app to query a longer
period:

Open `app/src/main/java/com/example/chronicdiseaseapp/repository/HealthDataRepository.kt` and find
the `getBloodPressureData()` function:

**Current code (30 days):**

```kotlin
val startTime = endTime.minusSeconds(30 * 24 * 60 * 60) // 30 days ago
```

**Change to 90 days:**

```kotlin
val startTime = endTime.minusSeconds(90 * 24 * 60 * 60) // 90 days ago
```

**Or 1 year:**

```kotlin
val startTime = endTime.minusSeconds(365 * 24 * 60 * 60) // 365 days ago
```

Then rebuild and install the app.

---

## Verify Sync is Working

After following the steps above, check the logs again:

```bash
adb logcat -c
adb shell am start -n com.example.chronicdiseaseapp/.MainActivity
sleep 3
adb logcat -d | grep "getBloodPressureData"
```

**Look for:**

- `"Received X records from Health Connect"` where X > 0
- `"Reading 0 - 120/80 at [timestamp]"` showing actual blood pressure values
- `"Successfully retrieved X blood pressure readings"` success message

**Success looks like:**

```
D/HealthDataRepository: getBloodPressureData: Received 76 records from Health Connect
D/HealthDataRepository: getBloodPressureData: Reading 0 - 120/80 at Fri Nov 29 10:30:00 IST 2025
D/HealthDataRepository: getBloodPressureData: Reading 1 - 118/78 at Thu Nov 28 09:15:00 IST 2025
...
D/HealthDataRepository: getBloodPressureData: Successfully retrieved 76 blood pressure readings from Health Connect
```

---

## Expected Behavior After Sync

Once data is synced:

1. App dashboard will show **real blood pressure values** instead of sample data
2. The yellow "Using Sample Data" banner will disappear
3. You'll see a green "Connected to Health Connect" banner instead
4. Blood pressure card will show your actual latest reading
5. All 76 readings will be stored and can be used for trends/analytics

---

## Quick Checklist

- [ ] Samsung Health app is updated to latest version
- [ ] Health Connect app is installed and updated
- [ ] Blood pressure data exists in Samsung Health
- [ ] Health Connect is enabled in Samsung Health settings
- [ ] Blood pressure data type is toggled ON for sharing
- [ ] Manual sync has been triggered
- [ ] Data is visible in Health Connect app under Vitals → Blood pressure
- [ ] Chronic Disease App has been refreshed
- [ ] App logs show "Received X records" where X > 0

---

## Still Not Working?

If you've followed all steps and still see 0 records:

1. **Check device compatibility:**
    - Android 14+ has Health Connect built-in
    - Android 13 and below need Health Connect app from Play Store

2. **Verify Health Connect is working:**
   ```bash
   adb shell dumpsys activity service com.google.android.apps.healthdata
   ```

3. **Try a test reading:**
    - Add a NEW blood pressure reading in Samsung Health RIGHT NOW
    - Wait 1 minute
    - Check if it appears in Health Connect app
    - Refresh your Chronic Disease App
    - If this new reading appears, sync is working (old readings may need time)

4. **Check app package name in logs:**
    - Ensure you're looking at the right app logs
    - Package: `com.example.chronicdiseaseapp`

---

## Contact

If you need further assistance, provide:

1. Logcat output from the app
2. Screenshot of Health Connect permissions screen
3. Screenshot of Samsung Health → Health Connect settings
4. Android version of your device
5. Whether a test reading (added just now) syncs or not
