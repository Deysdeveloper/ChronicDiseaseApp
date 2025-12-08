# Doctor-Patient Connection System

## Overview

A comprehensive connection system that allows patients to discover and connect with doctors, and
enables doctors to view vitals and provide feedback only for connected patients.

## Key Features

### For Patients:

1. **Discover Doctors** - Browse all registered doctors
2. **View Doctor Profiles** - See specialization, hospital, expertise
3. **Send Connection Requests** - Connect with chosen doctors
4. **Receive Feedback** - Get medical feedback from connected doctors
5. **Track Connection Status** - See pending, accepted, or rejected requests

### For Doctors:

1. **Connection Requests** - Receive and manage patient connection requests
2. **Accept/Reject Requests** - Control which patients to connect with
3. **View Connected Patients Only** - Access vitals only for connected patients
4. **Provide Feedback** - Give medical advice and recommendations
5. **Monitor Patient Health** - Track connected patients' vital signs

## Architecture

```
Patient App
    ├── Doctors Tab (replaces Insights)
    │   ├── Browse all doctors
    │   ├── View doctor profiles
    │   ├── Send connection requests
    │   └── View feedback from doctors
    │
    └── Dashboard
        └── Vitals tracking

Doctor App
    ├── Dashboard
    │   ├── Connection request notifications
    │   ├── Accept/Reject patients
    │   └── View connected patients vitals
    │
    └── Patients Tab
        ├── Connected patients list
        ├── View patient vitals
        └── Provide feedback
```

## Data Models

### ConnectionRequest

- Patient ID, Name, Email, Age
- Doctor ID, Name
- Status: PENDING/ACCEPTED/REJECTED
- Timestamps
- Optional message from patient

### DoctorInfo

- Doctor profile information
- Specialization, Hospital, Expertise
- Connection status with current patient
- Connection request ID if exists

### DoctorFeedback

- Feedback from doctor to patient
- Type: General, Vitals Review, Medication, etc.
- Severity: Normal, Caution, Warning, Urgent
- Vitals snapshot at time of feedback
- Recommendations list

## Firebase Structure

### Firestore Collections:

#### `connections` Collection

```
Document ID: auto-generated
Fields:
  - patientId: string
  - patientName: string
  - patientEmail: string
  - patientAge: number
  - doctorId: string
  - doctorName: string
  - status: "PENDING" | "ACCEPTED" | "REJECTED"
  - requestedAt: timestamp
  - respondedAt: timestamp (nullable)
  - message: string
```

#### `feedback` Collection

```
Document ID: auto-generated
Fields:
  - doctorId: string
  - doctorName: string
  - patientId: string
  - patientName: string
  - feedbackText: string
  - feedbackType: string
  - severity: string
  - vitalsSnapshot: object
  - recommendations: array
  - createdAt: timestamp
  - isRead: boolean
```

## Implementation Files

### New Files Created:

1. `DoctorConnection.kt` - Data models
2. `DoctorConnectionRepository.kt` - Firebase operations
3. `DoctorConnectionViewModel.kt` - Business logic for patients
4. `DoctorsListScreen.kt` - UI for browsing doctors
5. `FeedbackViewModel.kt` - Feedback management
6. Connection request handling in Doctor Dashboard

### Modified Files:

1. `HomeScreen.kt` - Change Insights tab to Doctors tab
2. `DocHomeScreen.kt` - Add connection request notifications
3. `PatientsVitalsScreen.kt` - Filter to show only connected patients
4. `Navigation.kt` - Add new routes

## User Flow

### Patient Connecting to Doctor:

1. Patient opens app → Goes to "Doctors" tab
2. Sees list of all registered doctors
3. Views doctor profile (expertise, hospital, specialization)
4. Clicks "Connect" button
5. Optional: Adds a message
6. Request sent → Status shows "Pending"
7. Waits for doctor's response
8. Receives notification when accepted/rejected

### Doctor Accepting Patient:

1. Doctor opens app → Sees notification badge
2. Goes to Dashboard → Views pending requests
3. Sees patient info (name, age, email, message)
4. Clicks "Accept" or "Reject"
5. If accepted: Patient added to connected patients list
6. Doctor can now view patient's vitals
7. Doctor can provide feedback to patient

### Doctor Viewing Patient Vitals:

1. Doctor goes to "Patients" tab
2. Sees only connected patients (not all patients)
3. Clicks on patient card
4. Views patient's vitals (Heart Rate, BP, SpO2, Steps)
5. Can provide feedback with recommendations

### Patient Receiving Feedback:

1. Patient receives notification
2. Goes to "Doctors" tab → "Feedback" section
3. Sees feedback from doctor
4. Views recommendations
5. Can see vitals snapshot at time of feedback

## Security & Privacy

### Access Control:

- ✅ Doctors can ONLY see vitals of CONNECTED patients
- ✅ Patients must explicitly connect with doctors
- ✅ Connection requests can be rejected
- ✅ All data requires authentication

### Firebase Rules Update:

```json
{
  "rules": {
    "users": {
      "$userId": {
        ".read": "$userId === auth.uid",
        ".write": "$userId === auth.uid",
        "vitals": {
          ".read": "$userId === auth.uid || root.child('connections').child(auth.uid).child($userId).val() === 'ACCEPTED'",
          ".indexOn": ["timestamp"]
        }
      }
    }
  }
}
```

## Benefits

1. **Privacy**: Patients control who sees their data
2. **Consent-Based**: Explicit connection approval required
3. **Bidirectional**: Both patient and doctor must agree
4. **Traceable**: All connections are logged
5. **Feedback Loop**: Doctors can provide guidance
6. **Scalable**: Supports multiple doctor-patient relationships

## Future Enhancements

1. **Appointments**: Book appointments with connected doctors
2. **Chat**: Real-time messaging between connected users
3. **Video Consultation**: Telemedicine support
4. **Prescription Management**: Digital prescriptions
5. **Emergency Contacts**: Quick access for urgent cases
6. **Doctor Ratings**: Patient reviews and ratings
7. **Insurance Integration**: Link insurance information
8. **Payment Gateway**: Handle consultation fees

## Testing Checklist

- [ ] Patient can see all doctors
- [ ] Patient can send connection request
- [ ] Doctor receives notification
- [ ] Doctor can accept request
- [ ] Doctor can reject request
- [ ] Patient sees updated status
- [ ] Doctor can view connected patient vitals
- [ ] Doctor cannot view unconnected patient vitals
- [ ] Doctor can provide feedback
- [ ] Patient receives feedback notification
- [ ] Patient can view feedback
- [ ] Connection status updates in real-time

---
**Status**: Implementation in Progress
**Version**: 2.0
**Last Updated**: December 2025
