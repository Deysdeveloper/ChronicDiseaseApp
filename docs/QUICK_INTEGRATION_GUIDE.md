# Quick Integration Guide - Health Connect Fix

## 🚀 3 Steps to Integrate the Fix

### Step 1: Update Your Sign-Up Screen

Find your sign-up/login success handler and add the health sync start call.

**Location:** `app/src/main/java/com/example/chronicdiseaseapp/screens/SignUpScreen.kt` or
`LoginScreen.kt`

**Find this code:**

```kotlin
firebaseAuth.createUserWithEmailAndPassword(email, password)
    .addOnSuccessListener { authResult ->
        // Save user profile...
        // Navigate to home...
    }
```

**Update to:**

```kotlin
firebaseAuth.createUserWithEmailAndPassword(email, password)
    .addOnSuccessListener { authResult ->
        val uid = authResult.user?.uid
        if (uid != null) {
            // ✅ Start health sync AFTER successful sign-in
            // Note: You'll need to pass healthDataViewModel to the screen
            healthDataViewModel.startHealthDataSync()
            
            // Save user profile...
            // Navigate to home...
        }
    }
```

---

### Step 2: Update HomeScreen to Stop Sync on Sign-Out

**Location:** `app/src/main/java/com/example/chronicdiseaseapp/screens/patientScreen/HomeScreen.kt`

**Add AuthStateManager listener:**

```kotlin
@Composable
fun HomeScreen(
    // ... existing parameters ...
    healthDataViewModel: HealthDataViewModel = viewModel()
) {
    // Add this block to monitor auth state
    val authStateManager = remember { AuthStateManager.getInstance() }
    
    DisposableEffect(Unit) {
        val callback = object : AuthStateManager.AuthStateCallback {
            override fun onSignedIn(userId: String) {
                Log.d("HomeScreen", "User signed in: $userId")
                healthDataViewModel.startHealthDataSync()
            }
            
            override fun onSignedOut() {
                Log.d("HomeScreen", "User signed out")
                healthDataViewModel.stopHealthDataSync()
            }
        }
        
        authStateManager.registerCallback(callback)
        
        onDispose {
            authStateManager.unregisterCallback(callback)
        }
    }
    
    // ... rest of your composable ...
}
```

---

### Step 3: Add Import Confirmation Dialog (Optional but Recommended)

**Location:** `app/src/main/java/com/example/chronicdiseaseapp/screens/patientScreen/HomeScreen.kt`

**Add SharedPreferences tracking and dialog:**

```kotlin
@Composable
fun HomeScreen(
    navController: NavController,
    healthDataViewModel: HealthDataViewModel = viewModel()
) {
    val context = LocalContext.current
    val sharedPrefs = remember {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }
    
    // Check if we've asked about import
    val hasAskedImport = remember {
        sharedPrefs.getBoolean(PREF_HEALTH_IMPORT_ASKED, false)
    }
    
    var showImportDialog by remember { mutableStateOf(!hasAskedImport) }
    
    // Show dialog on first sign-in
    if (showImportDialog) {
        HealthDataImportDialog(
            onImportConfirmed = {
                Log.d("HomeScreen", "User confirmed health data import")
                healthDataViewModel.startHealthDataSync()
                sharedPrefs.edit().putBoolean(PREF_HEALTH_IMPORT_ASKED, true).apply()
                showImportDialog = false
            },
            onDismiss = {
                Log.d("HomeScreen", "User skipped health data import")
                sharedPrefs.edit().putBoolean(PREF_HEALTH_IMPORT_ASKED, true).apply()
                showImportDialog = false
            }
        )
    }
    
    // ... rest of your composable ...
}
```

**Add import:**

```kotlin
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.example.chronicdiseaseapp.screens.patientScreen.PREF_HEALTH_IMPORT_ASKED
import com.example.chronicdiseaseapp.screens.patientScreen.HealthDataImportDialog
```

---

## ✅ That's It!

With these 3 simple changes:

- ✅ Health Connect only reads data AFTER authentication
- ✅ Data sync stops on sign-out
- ✅ Users give explicit consent for device data import

---

## 🧪 Quick Test

1. **Sign up as new user**
    - Check logs for: `🟢 startHealthDataSync() called`
    - Verify Firebase has correct UID

2. **Sign out**
    - Check logs for: `🔴 stopHealthDataSync() called`
    - Verify data is cleared from ViewModel

3. **Sign in again**
    - Check logs for: `🟢 User signed in: <uid>`
    - Verify health sync restarts

---

## 📝 Next Steps

- [ ] Integrate Step 1 in your sign-up/login screens
- [ ] Integrate Step 2 in HomeScreen
- [ ] Integrate Step 3 for user consent (recommended)
- [ ] Test with multiple users
- [ ] Update Firebase security rules (see main docs)

---

## 🆘 Need Help?

See the complete documentation:

- `docs/HEALTH_CONNECT_CROSS_USER_DATA_FIX.md` - Full technical details
- `docs/DISEASE_KNOWLEDGE_NO_CACHE_EXPLANATION.md` - Disease Knowledge feature

**Common Issues:**

**Q: ViewModel not available in sign-up screen?**
A: Pass it as a parameter or use `viewModel()` from the composable.

**Q: Import dialog not showing?**
A: Check SharedPreferences key is correct and not already set.

**Q: Data still mixing between users?**
A: Make sure you're calling `stopHealthDataSync()` on sign-out.

---

_Quick Start Guide v1.0_
