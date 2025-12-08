# Quick Setup Guide: Doctor Access to Patient Vitals

## ⚠️ IMPORTANT: Update Firebase Security Rules

Before the doctor can access patient vitals, you **MUST** update your Firebase Realtime Database
security rules.

## Step-by-Step Instructions

### 1. Go to Firebase Console

1. Open https://console.firebase.google.com/
2. Select your project: **ChronicDiseaseApp**
3. In the left sidebar, click **Realtime Database**
4. Click the **Rules** tab at the top

### 2. Update the Security Rules

Replace the current rules with the following:

```json
{
  "rules": {
    "users": {
      "$userId": {
        ".read": "$userId === auth.uid || auth != null",
        ".write": "$userId === auth.uid",
        "vitals": {
          ".read": "auth != null",
          ".indexOn": [
            "timestamp"
          ],
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

### 3. Publish the Rules

1. Click the **Publish** button at the top right
2. Confirm the changes
3. Wait for "Rules published successfully" message

### 4. Verify the Changes

1. Go back to the **Data** tab in Realtime Database
2. The rules should now allow authenticated doctors to read patient vitals

## What Changed?

### Old Rules:

```json
".read": "$userId === auth.uid"
```

- Only the user themselves could read their own data

### New Rules:

```json
".read": "$userId === auth.uid || auth != null"
```

- User can read their own data **OR**
- Any authenticated user (including doctors) can read the data

### Vitals-Specific Access:

```json
"vitals": {
  ".read": "auth != null"
}
```

- All authenticated users can read vitals data
- This allows doctors to view patient vitals

## Testing the Feature

### 1. Test as a Patient

1. Login as a patient
2. Go to home screen and sync vitals data
3. Verify data appears in Firebase Realtime Database

### 2. Test as a Doctor

1. Login as a doctor
2. On the dashboard, tap **"View All Patients Vitals"** card
3. You should see a list of all registered patients
4. Tap on any patient to expand and view their vitals:
    - Heart Rate
    - Blood Pressure
    - SpO2
    - Steps

### 3. Expected Behavior

✅ Doctor sees all patients with their latest vitals
✅ Doctor can search patients by name or email
✅ Doctor can sort and filter the patient list
✅ Patients with no vitals show "No vitals data"
✅ Data refreshes when refresh button is tapped

## Security Considerations

### Current Implementation:

- ✅ Requires authentication (no anonymous access)
- ✅ Doctors can only READ patient data (no write access)
- ✅ All data is encrypted in transit (Firebase default)

### ⚠️ For Production Use:

Consider implementing these additional security measures:

1. **Role-Based Access Control (RBAC)**
   ```json
   ".read": "auth != null && root.child('users').child(auth.uid).child('userType').val() === 'DOCTOR'"
   ```

2. **Doctor-Patient Assignment**
   ```json
   ".read": "auth != null && root.child('patientAssignments').child($userId).child(auth.uid).val() === true"
   ```

3. **Audit Logging**
    - Log all data access for compliance
    - Track who accessed which patient's data and when

4. **Data Anonymization**
    - Consider showing only necessary patient info
    - Remove sensitive fields from doctor view

## Troubleshooting

### Problem: "Permission denied" error

**Solution:**

- Double-check that you updated the Firebase rules
- Click "Publish" after updating the rules
- Make sure the user is authenticated (logged in)
- Check the Firebase Console → Realtime Database → Rules

### Problem: Rules not updating

**Solution:**

- Clear browser cache
- Wait 5-10 seconds after publishing
- Try publishing again
- Check for JSON syntax errors in the rules

### Problem: Cannot see the new rules in console

**Solution:**

- Make sure you're in the correct Firebase project
- Check you're on the **Realtime Database** (not Firestore)
- Try refreshing the Firebase Console page

## Alternative: Using Firebase CLI

If you prefer using the command line:

```bash
# Install Firebase CLI (if not already installed)
npm install -g firebase-tools

# Login to Firebase
firebase login

# Navigate to project directory
cd /Users/debojyotidey/AndroidStudioProjects/ChronicDiseaseApp

# Deploy the rules
firebase deploy --only database
```

Create a `database.rules.json` file in your project root with the updated rules, then run the deploy
command.

## Rollback Instructions

If you need to revert to the old rules:

```json
{
  "rules": {
    "users": {
      "$userId": {
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

This will prevent doctors from accessing patient vitals.

## Questions?

If you encounter any issues:

1. Check the Logcat output for detailed error messages
2. Verify Firebase project configuration
3. Ensure you're logged in with the correct Firebase account
4. Review the main documentation: `DOCTOR_PATIENT_VITALS_FEATURE.md`

---
**Status**: Ready to Deploy ✅
**Time Required**: 2-3 minutes
**Difficulty**: Easy
