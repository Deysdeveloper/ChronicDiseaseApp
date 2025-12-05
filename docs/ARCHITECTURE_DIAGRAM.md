# Architecture Diagram - Health Connect Fix

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         USER DEVICE                              │
│                                                                  │
│  ┌──────────────┐                                               │
│  │ Health       │  Device-scoped data (ALL users on device)     │
│  │ Connect      │  ← THIS IS THE PROBLEM WE'RE SOLVING          │
│  └──────┬───────┘                                               │
│         │                                                        │
│         │ Read (after auth ✅)                                  │
│         ↓                                                        │
│  ┌──────────────────────────────────────────────────┐          │
│  │     HealthDataRepository                          │          │
│  │                                                    │          │
│  │  1. CHECK: auth.currentUser?.uid ⚠️              │          │
│  │     ├─ null? → STOP, return empty ❌             │          │
│  │     └─ Valid? → Continue ✅                       │          │
│  │                                                    │          │
│  │  2. Read Health Connect data                      │          │
│  │                                                    │          │
│  │  3. ADD device metadata:                          │          │
│  │     - deviceId = device's Android ID              │          │
│  │     - importedAt = timestamp                      │          │
│  │     - importedByUser = currentUser.uid            │          │
│  │                                                    │          │
│  │  4. Save with withContext(Dispatchers.IO)         │          │
│  └───────────────────┬──────────────────────────────┘          │
│                      │                                           │
│                      ↓                                           │
│  ┌──────────────────────────────────────────────────┐          │
│  │     VitalsRepository                              │          │
│  │                                                    │          │
│  │  - saveHeartRateData(readings)                    │          │
│  │  - saveBloodPressureData(readings)                │          │
│  │  - saveSpO2Data(readings)                         │          │
│  │  - saveStepsData(readings)                        │          │
│  │                                                    │          │
│  │  Each save includes:                              │          │
│  │    ✅ value                                       │          │
│  │    ✅ timestamp                                   │          │
│  │    ✅ deviceId                                    │          │
│  │    ✅ importedAt                                  │          │
│  │    ✅ importedByUser (UID)                        │          │
│  └───────────────────┬──────────────────────────────┘          │
│                      │                                           │
└──────────────────────┼───────────────────────────────────────────┘
                       │
                       │ Firebase RTDB write
                       ↓
┌─────────────────────────────────────────────────────────────────┐
│                    FIREBASE CLOUD                                │
│                                                                  │
│  ┌──────────────────────────────────────────────────┐          │
│  │     Firebase Realtime Database                    │          │
│  │                                                    │          │
│  │  users/                                            │          │
│  │    └─ <User A UID>/                               │          │
│  │         └─ vitals/                                 │          │
│  │              ├─ heartRate/                         │          │
│  │              │    └─ <timestamp>: {                │          │
│  │              │         value: 72,                  │          │
│  │              │         deviceId: "abc123",         │          │
│  │              │         importedByUser: "User A UID"│          │
│  │              │       }                              │          │
│  │              ├─ bloodPressure/                     │          │
│  │              ├─ spO2/                               │          │
│  │              └─ steps/                              │          │
│  │                                                    │          │
│  │    └─ <User B UID>/  ← ISOLATED ✅               │          │
│  │         └─ vitals/   ← NEVER sees User A data ✅ │          │
│  │                                                    │          │
│  │  Security Rules:                                   │          │
│  │    users/$uid: {                                   │          │
│  │      .read: "auth.uid == $uid"  ← Enforced        │          │
│  │      .write: "auth.uid == $uid" ← Enforced        │          │
│  │    }                                                │          │
│  └────────────────────────────────────────────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔄 User Lifecycle Flow

### Sign-Up / Sign-In

```
┌──────────────┐
│ User Opens   │
│ App          │
└──────┬───────┘
       │
       ↓
┌──────────────────────────────────────┐
│ User Signs Up / Logs In              │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│ FirebaseAuth.currentUser set ✅      │
│ UID = "abc123..."                    │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│ AuthStateManager detects sign-in     │
│ Callback: onSignedIn(uid)            │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│ (Optional) Show Import Dialog        │
│ "Import device health data?"         │
└──────┬───────────────────────────────┘
       │
       ↓ User clicks "Yes, Import"
       │
┌──────────────────────────────────────┐
│ healthDataViewModel                  │
│   .startHealthDataSync() ✅          │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│ loadHealthData()                     │
│   - getHeartRateData()               │
│   - getSpO2Data()                    │
│   - getBloodPressureData()           │
│   - getStepsData()                   │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│ Each method checks auth FIRST:       │
│ if (auth.currentUser?.uid == null)   │
│   return empty ❌                     │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│ Read Health Connect                  │
│ Add device metadata                  │
│ Save to Firebase under correct UID ✅│
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│ Health data displayed to user        │
│ All readings tagged with:            │
│   - deviceId                          │
│   - importedByUser = current UID     │
└──────────────────────────────────────┘
```

### Sign-Out

```
┌──────────────┐
│ User Clicks  │
│ Sign Out     │
└──────┬───────┘
       │
       ↓
┌──────────────────────────────────────┐
│ FirebaseAuth.signOut()               │
│ currentUser = null                   │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│ AuthStateManager detects sign-out    │
│ Callback: onSignedOut()              │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│ healthDataViewModel                  │
│   .stopHealthDataSync() 🔴           │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│ Clear all ViewModel data:            │
│   - heartRateData = empty            │
│   - spO2Data = empty                 │
│   - bloodPressureData = empty        │
│   - stepsData = empty                │
│   - healthMetrics = default          │
│   - isInitialized = false            │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│ Ready for next user ✅               │
│ No data leaks 🔒                     │
└──────────────────────────────────────┘
```

---

## 🛡️ Auth Check Guard

### Before (Vulnerable)

```
loadHealthData()
    ↓
Read Health Connect immediately ❌
    ↓
auth.currentUser might be null ❌
    ↓
Try to save with null UID ❌
    ↓
Error OR saved with wrong UID ❌
```

### After (Protected)

```
loadHealthData()
    ↓
CHECK: auth.currentUser?.uid ⚠️
    ├─ null? ────────────────────┐
    │                             ↓
    │                    Log error, return empty ❌
    │                    No Firebase write
    │                    User sees "No data" (correct!)
    │
    └─ Valid UID? ───────────────┐
                                  ↓
                         Read Health Connect ✅
                                  ↓
                         Add device metadata ✅
                                  ↓
                         Save with CORRECT UID ✅
```

---

## 🔐 Data Isolation

### Multi-User on Same Device

```
┌─────────────────────────────────────────────────────┐
│                    DEVICE                            │
│                                                      │
│  Health Connect Storage (device-scoped):            │
│    All readings from all users mixed together       │
│    ├─ Heart rate: 72 bpm (User A, yesterday)        │
│    ├─ Heart rate: 68 bpm (User B, today)            │
│    ├─ Blood pressure: 120/80 (User A, last week)    │
│    └─ SpO2: 98% (User B, today)                     │
│                                                      │
└─────────────────────────────────────────────────────┘
                        │
                        │ Our app reads this mixed data
                        ↓
┌─────────────────────────────────────────────────────┐
│              HealthDataRepository                    │
│                                                      │
│  Auth Check:                                         │
│    Current User = User B (UID: "xyz789")            │
│                                                      │
│  Metadata Added:                                     │
│    deviceId = "device123"                            │
│    importedByUser = "xyz789" (User B)                │
│    importedAt = 2024-12-22T10:00:00Z                 │
│                                                      │
└─────────────────────────────────────────────────────┘
                        │
                        ↓
┌─────────────────────────────────────────────────────┐
│              Firebase Realtime Database              │
│                                                      │
│  users/                                              │
│    ├─ User A (uid: abc123)/                          │
│    │    └─ vitals/                                   │
│    │         └─ heartRate/                           │
│    │              └─ 1234567890: {                   │
│    │                   value: 72,                    │
│    │                   importedByUser: "abc123" ✅  │
│    │                 }                                │
│    │                                                 │
│    └─ User B (uid: xyz789)/                          │
│         └─ vitals/                                   │
│              └─ heartRate/                           │
│                   └─ 9876543210: {                   │
│                        value: 68,                    │
│                        importedByUser: "xyz789" ✅  │
│                      }                                │
│                                                      │
│  ✅ User A cannot read User B's data (rules)        │
│  ✅ User B cannot read User A's data (rules)        │
│  ✅ Each user's data clearly tagged                 │
│                                                      │
└─────────────────────────────────────────────────────┘
```

---

## 📱 Component Interaction Diagram

```
┌────────────────┐         ┌────────────────┐
│   UI Layer     │         │  AuthState     │
│  (Composable)  │◄────────┤   Manager      │
│                │         │  (Singleton)   │
└───────┬────────┘         └────────────────┘
        │                          │
        │ startHealthDataSync()    │ onSignedIn(uid)
        ↓                          │ onSignedOut()
┌────────────────┐                 │
│ HealthData     │◄────────────────┘
│   ViewModel    │
│                │
│ - startHealthDataSync() ────┐
│ - stopHealthDataSync()      │
│ - loadHealthData()          │
└───────┬────────┘             │
        │                      │
        │ getHeartRateData()   │
        ↓                      │
┌────────────────┐             │
│ HealthData     │             │
│  Repository    │             │
│                │             │
│ ✅ Auth check  │             │
│ ✅ Device ID   │             │
│ ✅ Metadata    │             │
└───────┬────────┘             │
        │                      │
        │ saveHeartRateData()  │
        ↓                      │
┌────────────────┐             │
│   Vitals       │             │
│  Repository    │             │
│                │             │
│ ✅ Save with   │             │
│    device      │             │
│    metadata    │             │
└───────┬────────┘             │
        │                      │
        ↓                      ↓
┌──────────────────────────────┐
│    Firebase Realtime DB      │
│                              │
│  users/<uid>/vitals/...      │
│    ✅ Isolated per user      │
│    ✅ Device tracked         │
│    ✅ Import tracked         │
└──────────────────────────────┘
```

---

## 🎬 Sequence Diagram: Sign-In Flow

```
User    SignUpScreen    AuthStateManager    ViewModel    Repository    Firebase
 │            │                │               │             │            │
 │ Sign Up    │                │               │             │            │
 ├───────────>│                │               │             │            │
 │            │                │               │             │            │
 │            │ createUser()   │               │             │            │
 │            ├────────────────┼───────────────┼─────────────┼───────────>│
 │            │                │               │             │            │
 │            │<───────────────┼───────────────┼─────────────┼────────────│
 │            │ Success(uid)   │               │             │            │
 │            │                │               │             │            │
 │            │  Detect Auth   │               │             │            │
 │            │  Change        │               │             │            │
 │            │                ├──>onSignedIn(uid)           │            │
 │            │                │               │             │            │
 │            │ startHealthDataSync()          │             │            │
 │            ├────────────────┼──────────────>│             │            │
 │            │                │               │             │            │
 │            │                │               ├─>loadHealthData()        │
 │            │                │               │             │            │
 │            │                │               │ getHeartRateData()       │
 │            │                │               ├────────────>│            │
 │            │                │               │             │            │
 │            │                │               │  Auth Check │            │
 │            │                │               │  ✅ Valid   │            │
 │            │                │               │             │            │
 │            │                │               │  Read       │            │
 │            │                │               │  Health     │            │
 │            │                │               │  Connect    │            │
 │            │                │               │             │            │
 │            │                │               │  Add        │            │
 │            │                │               │  Metadata   │            │
 │            │                │               │             │            │
 │            │                │               │ saveHeartRateData()      │
 │            │                │               ├─────────────┼───────────>│
 │            │                │               │             │            │
 │            │                │               │<────────────┼────────────│
 │            │                │               │   Success   │            │
 │            │                │               │             │            │
 │            │<───────────────┼───────────────│             │            │
 │<───────────│   Update UI    │               │             │            │
 │ Show Data  │                │               │             │            │
```

---

## 🛑 Sequence Diagram: Sign-Out Flow

```
User    HomeScreen    AuthStateManager    ViewModel    Repository    Firebase
 │          │              │                 │             │            │
 │ Sign Out │              │                 │             │            │
 ├─────────>│              │                 │             │            │
 │          │              │                 │             │            │
 │          │ signOut()    │                 │             │            │
 │          ├──────────────┼─────────────────┼─────────────┼───────────>│
 │          │              │                 │             │            │
 │          │              │  Detect Auth    │             │            │
 │          │              │  Change         │             │            │
 │          │              ├──>onSignedOut() │             │            │
 │          │              │                 │             │            │
 │          │ stopHealthDataSync()           │             │            │
 │          ├──────────────┼────────────────>│             │            │
 │          │              │                 │             │            │
 │          │              │                 ├─>Clear Data │            │
 │          │              │                 │  - heartRate = []        │
 │          │              │                 │  - spO2 = []             │
 │          │              │                 │  - bloodPressure = []    │
 │          │              │                 │  - steps = []            │
 │          │              │                 │  - isInitialized = false │
 │          │              │                 │             │            │
 │          │<─────────────┼─────────────────│             │            │
 │<─────────│ Update UI    │                 │             │            │
 │ Show     │ (No Data)    │                 │             │            │
 │ Login    │              │                 │             │            │
```

---

## 📊 Before vs After Comparison

### Data Flow BEFORE Fix

```
App Start → ViewModel init → loadHealthData() immediately
                                      ↓
                            auth.currentUser = null ❌
                                      ↓
                         Read Health Connect (mixed device data)
                                      ↓
                            Try to save to Firebase
                                      ↓
                              Error OR wrong UID
                                      ↓
                         NEW USER SEES OLD DATA ❌
```

### Data Flow AFTER Fix

```
App Start → ViewModel init → Wait for explicit start ⏸️
                                      ↓
                              User signs in
                                      ↓
                   AuthStateManager: onSignedIn(uid) ✅
                                      ↓
                          startHealthDataSync() called
                                      ↓
                            auth.currentUser = valid ✅
                                      ↓
                         Read Health Connect (mixed device data)
                                      ↓
                            Add device metadata:
                            - deviceId
                            - importedByUser = uid ✅
                                      ↓
                         Save to Firebase under correct UID ✅
                                      ↓
                         USER SEES ONLY THEIR DATA ✅
```

---

_Architecture Diagram v1.0 - December 2024_
