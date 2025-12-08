# Implementation Plan: Doctor-Patient Connection System

## ✅ Files Already Created

1. **`DoctorConnection.kt`** - Complete ✅
    - ConnectionRequest data model
    - ConnectionStatus enum
    - DoctorInfo data model
    - DoctorFeedback data model
    - FeedbackType and FeedbackSeverity enums
    - VitalsSnapshot data model

2. **`DoctorConnectionRepository.kt`** - Complete ✅
    - getAllDoctors()
    - sendConnectionRequest()
    - cancelConnectionRequest()
    - getPendingConnectionRequests()
    - acceptConnectionRequest()
    - rejectConnectionRequest()
    - getConnectedPatients()
    - addFeedback()
    - getPatientFeedback()
    - markFeedbackAsRead()

3. **`DoctorConnectionViewModel.kt`** - Complete ✅
    - Load and display all doctors
    - Send/cancel connection requests
    - Search doctors
    - Observe feedback
    - Mark feedback as read

## 📝 Files to Create

### 1. DoctorsListScreen.kt (Patient Side)

**Location**:
`app/src/main/java/com/example/chronicdiseaseapp/screens/patientScreen/DoctorsListScreen.kt`

**Purpose**: Replace the "Insights" tab - show all doctors, allow connection requests

**Key Components**:

- Top bar with search
- Filter by specialization
- Doctor cards with:
    - Photo
    - Name, Specialization
    - Hospital
    - Connection status button
        - "Connect" (if not connected)
        - "Pending" (if request sent)
        - "Connected" (if accepted)
        - "Cancelled" (if rejected)
- Dialog for connection request message
- Feedback section showing doctor feedback

**Code Structure**:

```kotlin
@Composable
fun DoctorsListScreen(
    viewModel: DoctorConnectionViewModel = viewModel()
) {
    val doctors by viewModel.filteredDoctors.collectAsState()
    val feedback by viewModel.feedback.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var selectedTab by remember { mutableStateOf(DoctorScreenTab.ALL_DOCTORS) }
    
    Column {
        // Search bar
        SearchBar(...)
        
        // Tabs: All Doctors | My Connections | Feedback
        TabRow(selectedTab)
        
        when (selectedTab) {
            DoctorScreenTab.ALL_DOCTORS -> {
                // List of all doctors
                LazyColumn {
                    items(doctors) { doctor ->
                        DoctorCard(doctor, onConnect = {...})
                    }
                }
            }
            DoctorScreenTab.MY_CONNECTIONS -> {
                // Only connected doctors
                val connectedDoctors = doctors.filter { it.isConnected }
                ...
            }
            DoctorScreenTab.FEEDBACK -> {
                // Feedback from doctors
                LazyColumn {
                    items(feedback) { feedback ->
                        FeedbackCard(feedback)
                    }
                }
            }
        }
    }
}

@Composable
private fun DoctorCard(doctor: DoctorInfo, onConnect: () -> Unit) {
    Card {
        Row {
            // Doctor photo
            AsyncImage(doctor.photoUrl)
            
            Column {
                Text(doctor.fullName)
                Text(doctor.specialization)
                Text(doctor.currentHospital)
                Text(doctor.medicalExpertise)
            }
            
            // Connection button
            when {
                doctor.isConnected -> {
                    Button("Connected", enabled = false)
                }
                doctor.connectionStatus == ConnectionStatus.PENDING -> {
                    Button("Pending", enabled = false)
                }
                doctor.connectionStatus == ConnectionStatus.REJECTED -> {
                    Button("Request Declined", enabled = false)
                }
                else -> {
                    Button("Connect", onClick = onConnect)
                }
            }
        }
    }
}
```

### 2. Modify HomeScreen.kt (Patient Side)

**Changes Needed**:

```kotlin
// Change enum from:
private enum class BottomTab { Dashboard, Insights, Analysis, Profile }

// To:
private enum class BottomTab { Dashboard, Doctors, Analysis, Profile }

// In NavigationBar, change:
NavigationBarItem(
    selected = selectedTab == BottomTab.Doctors, // Changed from Insights
    onClick = { selectedTab = BottomTab.Doctors },
    icon = { 
        // Show badge if unread feedback
        if (unreadFeedbackCount > 0) {
            BadgedBox(badge = { Badge { Text(unreadFeedbackCount.toString()) } }) {
                Icon(Icons.Default.Person, "Doctors") // Changed icon
            }
        } else {
            Icon(Icons.Default.Person, "Doctors")
        }
    },
    label = { Text("Doctors") } // Changed from "Insights"
)

// In content section:
when (selectedTab) {
    BottomTab.Dashboard -> { /* existing dashboard */ }
    BottomTab.Doctors -> {
        DoctorsListScreen() // New screen
    }
    BottomTab.Analysis -> { /* existing analysis */ }
    BottomTab.Profile -> { /* existing profile */ }
}
```

### 3. ConnectionRequestsCard.kt (Doctor Side)

**Location**:
`app/src/main/java/com/example/chronicdiseaseapp/screens/doctorScreen/ConnectionRequestsCard.kt`

**Purpose**: Show pending connection requests in doctor dashboard

```kotlin
@Composable
fun ConnectionRequestsCard(
    requests: List<ConnectionRequest>,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit
) {
    Card {
        Column {
            Text("Connection Requests (${requests.size})")
            
            LazyColumn {
                items(requests) { request ->
                    ConnectionRequestItem(
                        request = request,
                        onAccept = { onAccept(request.id) },
                        onReject = { onReject(request.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionRequestItem(
    request: ConnectionRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card {
        Row {
            Column {
                Text(request.patientName)
                Text("Age: ${request.patientAge}")
                Text(request.patientEmail)
                if (request.message.isNotEmpty()) {
                    Text("Message: ${request.message}")
                }
                Text(request.timeAgo)
            }
            
            Row {
                Button("Accept", onClick = onAccept)
                Button("Reject", onClick = onReject, colors = ButtonDefaults.outlinedButtonColors())
            }
        }
    }
}
```

### 4. Modify DocHomeScreen.kt (Doctor Side)

**Changes Needed**:

```kotlin
// In DoctorHomeViewModel, add:
private val connectionRepository = DoctorConnectionRepository()

private val _connectionRequests = MutableStateFlow<List<ConnectionRequest>>(emptyList())
val connectionRequests: StateFlow<List<ConnectionRequest>> = _connectionRequests.asStateFlow()

init {
    loadDoctorDashboardData()
    observeConnectionRequests()
}

private fun observeConnectionRequests() {
    viewModelScope.launch {
        connectionRepository.getPendingConnectionRequests().collect { requests ->
            _connectionRequests.value = requests
        }
    }
}

fun acceptConnectionRequest(requestId: String) {
    viewModelScope.launch {
        connectionRepository.acceptConnectionRequest(requestId)
    }
}

fun rejectConnectionRequest(requestId: String) {
    viewModelScope.launch {
        connectionRepository.rejectConnectionRequest(requestId)
    }
}

// In DocHomeScreen composable:
val connectionRequests by viewModel.connectionRequests.collectAsState()

// Add to dashboard:
if (connectionRequests.isNotEmpty()) {
    ConnectionRequestsCard(
        requests = connectionRequests,
        onAccept = { viewModel.acceptConnectionRequest(it) },
        onReject = { viewModel.rejectConnectionRequest(it) }
    )
}
```

### 5. Modify PatientsVitalsScreen.kt (Doctor Side)

**Changes Needed**:

Filter to show only connected patients:

```kotlin
// In PatientVitalsViewModel, modify:
private val connectionRepository = DoctorConnectionRepository()

suspend fun loadAllPatientsVitals() {
    _isLoading.value = true
    _errorMessage.value = null
    
    try {
        // Get connected patient IDs first
        val connectedPatientsResult = connectionRepository.getConnectedPatients()
        
        connectedPatientsResult.onSuccess { connectedPatientIds ->
            // Only fetch vitals for connected patients
            val result = repository.getAllPatientsWithVitals()
            
            result.onSuccess { allPatients ->
                // Filter to show only connected patients
                val connectedPatients = allPatients.filter { patient ->
                    connectedPatientIds.contains(patient.patientId)
                }
                _patientsVitals.value = connectedPatients
                applyFiltersAndSort()
            }.onFailure { exception ->
                _errorMessage.value = "Failed to load patients: ${exception.message}"
            }
        }.onFailure { exception ->
            _errorMessage.value = "Failed to load connections: ${exception.message}"
        }
    } catch (e: Exception) {
        _errorMessage.value = "Error loading patients: ${e.message}"
    } finally {
        _isLoading.value = false
    }
}
```

### 6. Add Feedback Feature for Doctors

**New file**: `ProvideFeedbackDialog.kt`

```kotlin
@Composable
fun ProvideFeedbackDialog(
    patientId: String,
    patientName: String,
    currentVitals: PatientVitalsInfo,
    onDismiss: () -> Unit,
    onSubmit: (String, FeedbackType, List<String>, FeedbackSeverity) -> Unit
) {
    var feedbackText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(FeedbackType.GENERAL) }
    var selectedSeverity by remember { mutableStateOf(FeedbackSeverity.NORMAL) }
    var recommendations by remember { mutableStateOf(listOf<String>()) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column {
                Text("Provide Feedback to $patientName")
                
                // Current vitals snapshot
                VitalsSnapshotView(currentVitals)
                
                // Feedback type dropdown
                DropdownMenu(...)
                
                // Severity selector
                SeveritySelector(...)
                
                // Feedback text
                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { feedbackText = it },
                    label = { Text("Feedback") },
                    minLines = 3
                )
                
                // Recommendations (editable list)
                RecommendationsList(...)
                
                Row {
                    Button("Cancel", onClick = onDismiss)
                    Button("Submit", onClick = {
                        onSubmit(feedbackText, selectedType, recommendations, selectedSeverity)
                        onDismiss()
                    })
                }
            }
        }
    }
}
```

## 🗄️ Firebase Setup

### Firestore Security Rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Connections collection
    match /connections/{connectionId} {
      // Patients can read their own connection requests
      allow read: if request.auth != null && 
                     (resource.data.patientId == request.auth.uid || 
                      resource.data.doctorId == request.auth.uid);
      // Patients can create connection requests
      allow create: if request.auth != null && 
                       request.resource.data.patientId == request.auth.uid;
      // Patients can delete their own pending requests
      allow delete: if request.auth != null && 
                       resource.data.patientId == request.auth.uid &&
                       resource.data.status == 'PENDING';
      // Doctors can update status
      allow update: if request.auth != null && 
                       resource.data.doctorId == request.auth.uid;
    }
    
    // Feedback collection
    match /feedback/{feedbackId} {
      // Patients can read their own feedback
      allow read: if request.auth != null && 
                     resource.data.patientId == request.auth.uid;
      // Doctors can create feedback for their connected patients
      allow create: if request.auth != null && 
                       request.resource.data.doctorId == request.auth.uid;
      // Patients can update isRead status
      allow update: if request.auth != null && 
                       resource.data.patientId == request.auth.uid &&
                       request.resource.data.diff(resource.data).affectedKeys().hasOnly(['isRead']);
    }
  }
}
```

### Realtime Database Rules (Update):

```json
{
  "rules": {
    "users": {
      "$userId": {
        ".read": "$userId === auth.uid || isConnectedDoctor($userId)",
        ".write": "$userId === auth.uid",
        "vitals": {
          ".read": "$userId === auth.uid || isConnectedDoctor($userId)",
          ".indexOn": ["timestamp"]
        }
      }
    }
  },
  "functions": {
    "isConnectedDoctor": "root.child('doctorConnections').child(auth.uid).child($userId).val() === true"
  }
}
```

Note: Firebase Realtime Database doesn't support custom functions like shown above. You'll need to
maintain a separate node:

```json
{
  "rules": {
    "users": {
      "$userId": {
        ".read": "$userId === auth.uid",
        ".write": "$userId === auth.uid",
        "vitals": {
          ".read": "$userId === auth.uid || root.child('connections').child(auth.uid).child($userId).child('status').val() === 'ACCEPTED'",
          ".indexOn": ["timestamp"]
        }
      }
    },
    "connections": {
      "$doctorId": {
        "$patientId": {
          ".read": "$doctorId === auth.uid || $patientId === auth.uid",
          ".write": "false"
        }
      }
    }
  }
}
```

However, since we're using Firestore for connections, the easier approach is to keep the current
Realtime Database rules and handle access control in the repository layer (which we've already done
by filtering connected patients).

## 📱 UI/UX Flow

### Patient Flow:

```
1. Open App
2. Go to "Doctors" tab (Bottom Navigation)
3. See list of all doctors
4. Search/Filter by specialization
5. Tap on doctor card
6. View doctor profile
7. Tap "Connect" button
8. Optional: Enter message
9. Confirm connection request
10. See "Pending" status
11. Wait for doctor's response
12. Receive notification when accepted
13. Access "My Connections" tab
14. View feedback from connected doctors
```

### Doctor Flow:

```
1. Open App
2. See notification badge (new connection requests)
3. Dashboard shows connection requests card
4. View patient details (name, age, email, message)
5. Tap "Accept" or "Reject"
6. If accepted: Patient added to "Patients" tab
7. Go to "Patients" tab
8. See ONLY connected patients
9. Tap on patient
10. View patient vitals
11. Tap "Provide Feedback" button
12. Enter feedback, recommendations, severity
13. Submit feedback
14. Patient receives notification
```

## 🧪 Testing Steps

### Test Connection Flow:

1. Create patient account (if not exists)
2. Create doctor account (if not exists)
3. Login as patient
4. Go to Doctors tab
5. Verify all doctors are listed
6. Send connection request to a doctor
7. Verify status shows "Pending"
8. Logout
9. Login as doctor
10. Verify notification badge appears
11. Verify connection request card shows
12. Accept the request
13. Verify request disappears from pending
14. Go to Patients tab
15. Verify the patient appears in list
16. Tap on patient
17. Verify vitals are visible
18. Provide feedback
19. Logout
20. Login as patient
21. Go to Doctors → Feedback tab
22. Verify feedback is visible
23. Tap on feedback
24. Verify it marks as read

## ⚠️ Important Notes

1. **Delete Old Implementation**: Remove or disable the previous `PatientsVitalsScreen`
   implementation that showed ALL patients. The new version should only show connected patients.

2. **Update Navigation**: Remove the old "View All Patients Vitals" card from doctor dashboard since
   now it's access-controlled.

3. **Notifications**: Consider adding Firebase Cloud Messaging (FCM) for push notifications when:
    - Patient receives doctor's response to connection request
    - Patient receives new feedback
    - Doctor receives new connection request

4. **Indexing**: Create Firestore indexes for efficient queries:
    - `connections` collection: Index on `doctorId` + `status` + `requestedAt`
    - `feedback` collection: Index on `patientId` + `createdAt`

5. **Error Handling**: All network operations should have proper error handling and user-friendly
   messages.

6. **Loading States**: Show appropriate loading indicators during async operations.

7. **Offline Support**: Consider implementing local caching for better offline experience.

## 📊 Database Indexes (Create in Firebase Console)

### Firestore Composite Indexes:

1. **connections** collection:
   ```
   doctorId (Ascending) + status (Ascending) + requestedAt (Descending)
   patientId (Ascending) + status (Ascending) + requestedAt (Descending)
   ```

2. **feedback** collection:
   ```
   patientId (Ascending) + createdAt (Descending)
   doctorId (Ascending) + createdAt (Descending)
   patientId (Ascending) + isRead (Ascending) + createdAt (Descending)
   ```

## 🚀 Next Steps

1. **Create DoctorsListScreen.kt** - Main UI for patients to browse doctors
2. **Modify HomeScreen.kt** - Change Insights tab to Doctors tab
3. **Create ConnectionRequestsCard.kt** - UI component for doctor dashboard
4. **Modify DocHomeScreen.kt** - Add connection requests management
5. **Modify PatientsVitalsScreen.kt** - Filter to connected patients only
6. **Create ProvideFeedbackDialog.kt** - Feedback UI for doctors
7. **Update Firebase Rules** - Apply security rules from this document
8. **Test thoroughly** - Follow testing steps above
9. **Add notifications** - Implement FCM (optional but recommended)
10. **Polish UI** - Add animations, better error messages, loading states

---
**Status**: Ready to Implement
**Estimated Time**: 6-8 hours for complete implementation
**Priority**: High
**Dependencies**: All prerequisite files already created
