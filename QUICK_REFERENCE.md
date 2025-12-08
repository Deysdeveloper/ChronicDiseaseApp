# Quick Reference: Doctor-Patient Connection System

## 🚀 Quick Start

### For Testing:

```bash
# Build and run
./gradlew installDebug

# Or use Android Studio
# Click Run ▶️
```

## 📱 What Changed

### Patient App:

- **"Insights" tab** → **"Doctors" tab**
- New screen to browse and connect with doctors
- 3 tabs: All Doctors | My Connections | Feedback

### Doctor App:

- Dashboard shows connection requests
- Can accept/reject patient requests
- **Patients tab now shows ONLY connected patients**

## 🔑 Key Features

### Patient Can:

✅ Browse all doctors
✅ Search by name/specialization/hospital
✅ Send connection requests
✅ View connection status
✅ Receive feedback from doctors

### Doctor Can:

✅ Receive connection requests
✅ Accept or reject patients
✅ View ONLY connected patients' vitals
✅ Provide feedback (UI to be added)

## ⚡ Important Files

### New Files:

```
datamodels/DoctorConnection.kt
repository/DoctorConnectionRepository.kt
viewModel/DoctorConnectionViewModel.kt
screens/patientScreen/DoctorsListScreen.kt
```

### Modified Files:

```
screens/patientScreen/HomeScreen.kt
viewModel/DoctorHomeViewModel.kt
repository/PatientVitalsRepository.kt
```

## 🔐 Firebase Setup (REQUIRED)

### Firestore Rules:

Add these to your Firestore rules:

```javascript
match /connections/{connectionId} {
  allow read: if request.auth != null && 
                 (resource.data.patientId == request.auth.uid || 
                  resource.data.doctorId == request.auth.uid);
  allow create: if request.auth != null && 
                   request.resource.data.patientId == request.auth.uid;
  allow update: if request.auth != null && 
                   resource.data.doctorId == request.auth.uid;
}

match /feedback/{feedbackId} {
  allow read: if request.auth != null && 
                 resource.data.patientId == request.auth.uid;
  allow create: if request.auth != null && 
                   request.resource.data.doctorId == request.auth.uid;
}
```

### Where to Update:

1. Go to https://console.firebase.google.com/
2. Select your project
3. Firestore Database → Rules tab
4. Add the above rules
5. Click "Publish"

## 🧪 Testing Flow

### Test as Patient:

1. Login as patient
2. Tap "Doctors" tab (bottom navigation)
3. See list of doctors
4. Search for a doctor (optional)
5. Tap "Connect" on a doctor card
6. Enter message (optional)
7. Tap "Send Request"
8. Status shows "Pending"

### Test as Doctor:

1. Login as doctor
2. See notification on dashboard
3. View pending connection requests
4. Tap "Accept" on a request
5. Go to "Patients" tab
6. See the connected patient
7. Tap on patient to view vitals

### Verify Privacy:

1. Doctor should ONLY see connected patients
2. Unconnected patients should NOT appear
3. After connection, patient's vitals should be visible

## 📊 Data Structure

### Firestore Collections:

**connections:**

```json
{
  "patientId": "user123",
  "patientName": "John Doe",
  "patientEmail": "john@example.com",
  "patientAge": 45,
  "doctorId": "doc456",
  "doctorName": "Dr. Smith",
  "status": "PENDING" | "ACCEPTED" | "REJECTED",
  "requestedAt": 1234567890,
  "message": "Optional patient message"
}
```

**feedback:**

```json
{
  "doctorId": "doc456",
  "doctorName": "Dr. Smith",
  "patientId": "user123",
  "patientName": "John Doe",
  "feedbackText": "Your BP is a bit high...",
  "feedbackType": "VITALS_REVIEW",
  "severity": "CAUTION",
  "vitalsSnapshot": {
    "heartRate": 72,
    "bloodPressureSystolic": 140,
    "bloodPressureDiastolic": 90,
    "spO2": 98
  },
  "recommendations": [
    "Reduce salt intake",
    "Exercise 30 min daily"
  ],
  "createdAt": 1234567890,
  "isRead": false
}
```

## 🐛 Troubleshooting

### Issue: "Permission denied" in Firestore

**Solution:** Update Firestore security rules (see above)

### Issue: Doctor sees all patients

**Solution:** This shouldn't happen. Check `PatientVitalsRepository.kt` line 43-44

### Issue: Patient can't send request

**Solution:**

- Check Firebase Auth is working
- Check Firestore rules allow creation
- Check Logcat for errors

### Issue: Connection request not showing for doctor

**Solution:**

- Check DoctorHomeViewModel is observing requests
- Check Firestore listener is active
- Verify doctor UID matches in connection document

### Issue: No doctors showing for patient

**Solution:**

- Ensure doctors have userType = "DOCTOR" in Firestore
- Check getAllDoctors() in repository
- Verify patient is authenticated

## 💻 Code Examples

### Send Connection Request:

```kotlin
viewModel.sendConnectionRequest(
    doctorId = "doc123",
    doctorName = "Dr. Smith",
    message = "I need help with diabetes"
)
```

### Accept Request (Doctor):

```kotlin
viewModel.acceptConnectionRequest(requestId)
```

### Get Connected Patients (Doctor):

```kotlin
val result = repository.getConnectedPatients()
result.onSuccess { patientIds ->
    // patientIds contains UIDs of connected patients
}
```

## 📚 Full Documentation

- **Overview:** `DOCTOR_CONNECTION_SYSTEM.md`
- **Implementation Plan:** `IMPLEMENTATION_PLAN_DOCTOR_CONNECTION.md`
- **Completion Summary:** `IMPLEMENTATION_COMPLETE.md`
- **This Guide:** `QUICK_REFERENCE.md`

## ✅ Checklist

Before deploying:

- [ ] Build succeeds (`./gradlew assembleDebug`)
- [ ] Firebase rules updated
- [ ] Tested patient flow
- [ ] Tested doctor flow
- [ ] Verified privacy (only connected patients visible)
- [ ] Error messages are user-friendly
- [ ] UI looks good on different screen sizes

## 🎯 Key Points

1. **Privacy First:** Doctors only see connected patients
2. **Consent Required:** Both parties must agree
3. **Real-Time:** Updates happen instantly
4. **Secure:** Proper authentication & authorization
5. **User-Friendly:** Beautiful, intuitive UI

## 🚀 What's Next

### Optional Enhancements:

1. Add feedback UI for doctors
2. Implement push notifications
3. Add doctor profile pages
4. Create chat system
5. Enable appointment booking

### Production Checklist:

1. Enable Firebase Analytics
2. Set up Crashlytics
3. Add performance monitoring
4. Create user documentation
5. Plan backup strategy

---

**Need Help?**

- Check full documentation files
- Review code comments
- Check Logcat for errors
- Verify Firebase configuration

**Status:** ✅ Ready to Use
**Build:** ✅ Success
**Version:** 2.0

🎉 **Happy Testing!**
