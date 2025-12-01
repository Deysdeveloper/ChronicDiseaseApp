# Firebase Realtime Database Integration for Health Vitals

## Overview

This document explains how health vitals data from Health Connect (Samsung Health/Galaxy Watch) is
automatically stored in Firebase Realtime Database on a **per-user basis**.

---

## 🔥 Data Structure in Firebase

### Database Path Structure

```
firebase-realtime-database/
└── users/
    ├── {userId1}/                    // Firebase Auth UID (e.g., debo's UID)
    │   └── vitals/
    │       ├── heartRate/
    │       │   ├── {timestamp1}/
    │       │   │   ├── value: 72
    │       │   │   ├── timestamp: 1701234567890
    │       │   │   ├── source: "Health Connect"
    │       │   │   └── id: "uuid-123"
    │       │   └── {timestamp2}/
    │       │       └── ...
    │       ├── bloodPressure/
    │       │   └── {timestamp}/
    │       │       ├── systolic: 120
    │       │       ├── diastolic: 80
    │       │       ├── timestamp: 1701234567890
    │       │       ├── source: "Health Connect"
    │       │       └── id: "uuid-456"
    │       ├── spO2/
    │       │   └── {timestamp}/
    │       │       ├── value: 98
    │       │       ├── timestamp: 1701234567890
    │       │       ├── source: "Health Connect"
    │       │       └── id: "uuid-789"
    │       └── steps/
    │           └── {timestamp}/
    │               ├── value: 8543
    │               ├── timestamp: 1701234567890
    │               ├── source: "Health Connect"
    │               └── id: "uuid-012"
    │
    └── {userId2}/                    // Another user's UID
        └── vitals/
            └── ... (same structure for different user)
```

### Key Features:

- ✅ **User Isolation**: Each user's data is stored under their unique Firebase Auth UID
- ✅ **Timestamp Keys**: Using timestamps as keys allows easy sorting and querying
- ✅ **Data Privacy**: User A cannot access User B's data (enforced by Firebase Security Rules)
- ✅ **Real-time Sync**: Data syncs automatically when fetched from Health Connect

---

## 🛠️ Implementation Details

### 1. **VitalsRepository.kt**

Located at: `app/src/main/java/com/example/chronicdiseaseapp/repository/VitalsRepository.kt`

This repository handles all Firebase Realtime Database operations:

#### Key Methods:

**Saving Data:**

- `saveHeartRateData(readings: List<HealthReading>)` - Saves heart rate readings
- `saveBloodPressureData(readings: List<HealthReading>)` - Saves BP readings
- `saveSpO2Data(readings: List<HealthReading>)` - Saves oxygen saturation
- `saveStepsData(readings: List<HealthReading>)` - Saves step counts

**Reading Data:**

- `getHeartRateDataFromFirebase(limitToLast: Int = 100)` - Fetches latest 100 HR readings
- `getBloodPressureDataFromFirebase(limitToLast: Int = 100)` - Fetches BP data
- `getSpO2DataFromFirebase(limitToLast: Int = 100)` - Fetches SpO2 data
- `getStepsDataFromFirebase(limitToLast: Int = 100)` - Fetches steps data

**Utility Methods:**

- `getVitalsSummary()` - Gets latest value for each vital
- `cleanupOldData(daysToKeep: Int = 30)` - Deletes data older than N days

---

### 2. **HealthDataRepository.kt** (Modified)

Located at: `app/src/main/java/com/example/chronicdiseaseapp/repository/HealthDataRepository.kt`

**What Changed:**

- Added `VitalsRepository` instance
- After fetching data from Health Connect, it automatically saves to Firebase
- Logs success/failure of Firebase operations

**Example Flow:**

```kotlin
// 1. Fetch from Health Connect
val readings = healthConnectClient.readRecords(...)

// 2. Automatically save to Firebase for current user
vitalsRepository.saveHeartRateData(readings)

// 3. Return readings to ViewModel
emit(readings)
```

---

### 3. **HealthDataViewModel.kt** (Modified)

Located at: `app/src/main/java/com/example/chronicdiseaseapp/viewModel/HealthDataViewModel.kt`

**New Methods Added:**

```kotlin
// Load data from Firebase instead of Health Connect
viewModel.loadHealthDataFromFirebase()

// Get quick summary of latest vitals
viewModel.getVitalsSummary { summary ->
    val heartRate = summary["heartRate"]
    val systolic = summary["bloodPressureSystolic"]
    val diastolic = summary["bloodPressureDiastolic"]
    val spO2 = summary["spO2"]
    val steps = summary["steps"]
}

// Clean up old data (keep last 30 days)
viewModel.cleanupOldVitals(daysToKeep = 30)
```

---

## 🔄 Automatic Data Flow

### When User Opens the App:

```
1. User logs in (Firebase Auth)
   ↓
2. HealthDataViewModel.loadHealthData() is called
   ↓
3. HealthDataRepository fetches data from Health Connect
   ↓
4. For each vital type (HR, BP, SpO2, Steps):
   a. Read from Health Connect
   b. Save to Firebase under users/{currentUserId}/vitals/{vitalType}/
   c. Emit data to ViewModel
   ↓
5. UI displays the data
```

### User Isolation Example:

**User: Debo (UID: abc123)**

- Path: `users/abc123/vitals/heartRate/...`
- Debo can only read/write to `users/abc123/`

**User: John (UID: xyz789)**

- Path: `users/xyz789/vitals/heartRate/...`
- John can only read/write to `users/xyz789/`

**Result:** Debo and John's data are completely separate!

---

## 🔐 Firebase Security Rules (IMPORTANT!)

You **MUST** set up Firebase Security Rules to ensure users can only access their own data:

### Recommended Security Rules:

```json
{
  "rules": {
    "users": {
      "$userId": {
        // Users can only read/write their own data
        ".read": "$userId === auth.uid",
        ".write": "$userId === auth.uid",
        
        "vitals": {
          ".indexOn": ["timestamp"],
          
          "heartRate": {
            "$timestamp": {
              ".validate": "newData.hasChildren(['value', 'timestamp', 'source', 'id'])"
            }
          },
          
          "bloodPressure": {
            "$timestamp": {
              ".validate": "newData.hasChildren(['systolic', 'diastolic', 'timestamp', 'source', 'id'])"
            }
          },
          
          "spO2": {
            "$timestamp": {
              ".validate": "newData.hasChildren(['value', 'timestamp', 'source', 'id'])"
            }
          },
          
          "steps": {
            "$timestamp": {
              ".validate": "newData.hasChildren(['value', 'timestamp', 'source', 'id'])"
            }
          }
        }
      }
    }
  }
}
```

### How to Apply Security Rules:

1. Go to Firebase Console: https://console.firebase.google.com/
2. Select your project: `chronicdiseaseapp`
3. Navigate to **Realtime Database** > **Rules**
4. Paste the above rules
5. Click **Publish**

---

## 📊 Usage Examples

### Example 1: Load Fresh Data from Health Connect (Auto-saves to Firebase)

```kotlin
// In your Composable
val viewModel: HealthDataViewModel = viewModel()

// This fetches from Health Connect AND saves to Firebase automatically
viewModel.loadHealthData()

// Observe the data
val heartRateData by viewModel.heartRateData.observeAsState()
```

### Example 2: Load Historical Data from Firebase

```kotlin
// Load from Firebase instead of Health Connect
// Useful when Health Connect is not available or for viewing history
viewModel.loadHealthDataFromFirebase()

// Data will be populated in the same LiveData variables
val heartRateData by viewModel.heartRateData.observeAsState()
```

### Example 3: Get Quick Summary

```kotlin
viewModel.getVitalsSummary { summary ->
    val latestHeartRate = summary["heartRate"] as? Int
    val latestSpO2 = summary["spO2"] as? Int
    
    Log.d("Vitals", "Latest HR: $latestHeartRate bpm, SpO2: $latestSpO2%")
}
```

### Example 4: Cleanup Old Data (Optional)

```kotlin
// Clean up data older than 30 days (keeps database size manageable)
viewModel.cleanupOldVitals(daysToKeep = 30)
```

---

## 🧪 Testing the Integration

### Step 1: Check Logs

Look for these log messages when the app runs:

```
HealthDataRepository: getHeartRateData: Successfully retrieved X heart rate readings from Health Connect
HealthDataRepository: getHeartRateData: ✅ Saved to Firebase Realtime Database
VitalsRepository: ✅ Saved X heart rate readings for user: abc123
```

### Step 2: Verify in Firebase Console

1. Open Firebase Console: https://console.firebase.google.com/
2. Go to **Realtime Database**
3. Navigate to: `users/{your-uid}/vitals/`
4. You should see `heartRate`, `bloodPressure`, `spO2`, `steps` nodes
5. Expand any node to see timestamp-keyed entries

### Step 3: Test with Multiple Users

1. Login as User A (e.g., debo@example.com)
2. Load health data - check Firebase path: `users/{debo-uid}/vitals/`
3. Logout and login as User B (e.g., john@example.com)
4. Load health data - check Firebase path: `users/{john-uid}/vitals/`
5. Verify they are stored in different paths!

---

## 🚀 Benefits of This Implementation

### 1. **Automatic Synchronization**

- No manual save calls needed
- Data is saved immediately after fetching from Health Connect

### 2. **User Privacy**

- Each user's data isolated by Firebase Auth UID
- Cannot access other users' data

### 3. **Historical Data Access**

- Even if Health Connect is unavailable, can load from Firebase
- Useful for viewing past trends

### 4. **Real-time Updates**

- Uses Firebase listeners for real-time data updates
- Any changes sync instantly across devices

### 5. **Scalability**

- Timestamp-based keys allow efficient querying
- `limitToLast(100)` prevents loading too much data

### 6. **Data Management**

- `cleanupOldData()` method to manage database size
- Keep only relevant recent data

---

## 📝 Data Retention Policy

### Current Settings:

- Data saved immediately when fetched from Health Connect
- No automatic deletion (manual cleanup via `cleanupOldVitals()`)

### Recommended Policy:

```kotlin
// In your app initialization or periodic background task
viewModel.cleanupOldVitals(daysToKeep = 30)  // Keep last 30 days
```

### Future Enhancement Ideas:

- Automatic cleanup every 7 days using WorkManager
- User-configurable retention period in settings
- Export data before deletion

---

## 🐛 Troubleshooting

### Issue 1: Data Not Saving to Firebase

**Symptoms:** Logs show "❌ Failed to save to Firebase"

**Solutions:**

- Check internet connection
- Verify user is logged in (`FirebaseAuth.getInstance().currentUser != null`)
- Check Firebase Realtime Database is enabled in console
- Verify Firebase Security Rules allow write access

### Issue 2: Cannot Read Data from Firebase

**Symptoms:** No data when calling `loadHealthDataFromFirebase()`

**Solutions:**

- Check Firebase Security Rules (`.read` permission)
- Verify user UID matches the database path
- Check Firebase Console to see if data exists
- Look for error logs in Logcat

### Issue 3: "User not logged in" Error

**Symptoms:** Operations fail with "User not logged in"

**Solutions:**

- Ensure user is authenticated before loading vitals
- Check `FirebaseAuth.getInstance().currentUser` is not null
- Re-authenticate if session expired

### Issue 4: Duplicate Data

**Symptoms:** Same data saved multiple times

**Solutions:**

- Using timestamp as key prevents duplicates naturally
- If Health Connect returns duplicate timestamps, data overwrites (not duplicates)

---

## 📚 Code Files Modified

1. ✅ **VitalsRepository.kt** - NEW FILE
    - Handles all Firebase Realtime Database operations
    - User-specific data access

2. ✅ **HealthDataRepository.kt** - MODIFIED
    - Added automatic Firebase saving after Health Connect fetch
    - Integrated VitalsRepository

3. ✅ **HealthDataViewModel.kt** - MODIFIED
    - Added `loadHealthDataFromFirebase()` method
    - Added `getVitalsSummary()` method
    - Added `cleanupOldVitals()` method

4. ✅ **build.gradle.kts** - ALREADY HAD
    - Firebase Realtime Database dependency already added

---

## 🎯 Next Steps

### For You (Developer):

1. **Set up Firebase Security Rules** (see section above)
2. **Test with multiple user accounts**
3. **Monitor database size** in Firebase Console
4. **Implement periodic cleanup** (optional, using WorkManager)

### Optional Enhancements:

1. **Add data export feature**
   ```kotlin
   fun exportVitalsToCSV(): File { ... }
   ```

2. **Add data visualization from Firebase**
    - Charts showing trends over time
    - Weekly/monthly aggregations

3. **Add doctor access to patient vitals**
    - Modify security rules to allow doctors to read assigned patients' data
   ```json
   "users": {
     "$patientId": {
       "vitals": {
         ".read": "$patientId === auth.uid || root.child('doctors').child(auth.uid).child('patients').child($patientId).exists()"
       }
     }
   }
   ```

4. **Add offline support**
    - Firebase automatically handles offline caching
    - Enable with: `FirebaseDatabase.getInstance().setPersistenceEnabled(true)`

---

## 📞 Summary

✅ **What's Done:**

- Vitals automatically saved to Firebase Realtime Database
- Each user has isolated data under `users/{userId}/vitals/`
- Can read data from both Health Connect and Firebase
- Supports heart rate, blood pressure, SpO2, and steps

✅ **How It Works:**

- When app fetches from Health Connect, it saves to Firebase
- Uses current user's UID for data isolation
- Real-time listeners for live updates

✅ **User Data Separation:**

- Debo's data: `users/{debo-uid}/vitals/`
- John's data: `users/{john-uid}/vitals/`
- Complete isolation enforced by Firebase Security Rules

🎉 **Ready to Use!** Just login and load health data - it will automatically save to Firebase under
the logged-in user's UID.

---

## 🔗 Related Documentation

- [Firebase Realtime Database Docs](https://firebase.google.com/docs/database)
- [Firebase Security Rules Guide](https://firebase.google.com/docs/database/security)
- [Health Connect Integration](./README_SAMSUNG_HEALTH_INTEGRATION.md)
