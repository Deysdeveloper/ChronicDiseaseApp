# Chronic Disease Management App - Complete Guide

An Android health monitoring application with ML-powered anomaly detection, doctor-patient connectivity, and comprehensive health tracking for chronic disease patients.

---

## 📱 Core Features

### ✅ Health Data Tracking
- **Heart Rate** monitoring (Samsung Galaxy Watch 4 via Health Connect)
- **Blood Pressure** tracking (Systolic/Diastolic)
- **SpO2** (Oxygen Saturation) monitoring
- **Steps** counting and activity tracking
- **7-day trend visualization** with interactive charts
- Real-time data sync from Health Connect

### 🤖 ML-Powered Anomaly Detection
- TensorFlow Lite model for real-time health anomaly detection
- Custom thresholds for each vital sign
- Color-coded severity alerts (CRITICAL, HIGH, MEDIUM, LOW)
- Confidence scoring and expected value predictions

### 👨‍⚕️ Doctor-Patient Features
- **Doctor Dashboard**: View all registered patients
- **Patient Vitals Access**: Monitor patient health data in real-time
- **Connection System**: Doctor-patient relationship management
- **Searchable Patient List**: Find and filter patients easily

### 📚 Disease Knowledge Base
- Educational articles about chronic diseases
- Always-fresh content (no cache issues)
- Pull-to-refresh for latest articles
- Category-based organization

### 🔐 User Management
- Firebase Authentication (Email/Password, Google Sign-In)
- Role-based access (Patient/Doctor)
- User profiles with photo upload
- Secure per-user data isolation

### ☁️ Cloud Sync & Performance
- Firebase Realtime Database for data persistence
- Firebase Performance Monitoring enabled
- Cross-device synchronization
- Automatic trace logging for ML operations

---

## 🚀 Quick Start

### Prerequisites
- Android Studio (latest version)
- Android device with **Health Connect** installed
- Samsung Galaxy Watch 4 (or compatible wearable)
- Firebase project setup

### Installation Steps

#### 1. Clone & Open Project
```bash
git clone <your-repo-url>
cd ChronicDiseaseApp
# Open in Android Studio
```

#### 2. Firebase Setup

**A. Create Firebase Project**
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create project: `chronicdiseaseapp`
3. Add Android app with package: `com.example.chronicdiseaseapp`
4. Download `google-services.json` and place in `app/` directory

**B. Enable Firebase Services**
- **Authentication**: Enable Email/Password and Google Sign-In
- **Firestore**: Create database in production mode
- **Realtime Database**: Create database and apply these security rules:

```json
{
  "rules": {
    "users": {
      "$userId": {
        ".read": "$userId === auth.uid || auth != null",
        ".write": "$userId === auth.uid",
        "vitals": {
          ".read": "auth != null",
          ".indexOn": ["timestamp"]
        }
      }
    }
  }
}
```

**C. Setup Firestore Collections**

Create `diseaseArticles` collection with documents containing:
```json
{
  "title": "Understanding Hypertension",
  "content": "Full article content here...",
  "category": "Heart Disease",
  "publishedDate": 1701234567890,
  "imageUrl": "https://example.com/image.jpg",
  "author": "Dr. Smith",
  "tags": ["hypertension", "heart", "blood pressure"]
}
```

Create `users` collection (auto-populated on user registration):
```json
{
  "uid": "user-firebase-uid",
  "fullName": "John Doe",
  "email": "john@example.com",
  "age": 45,
  "userType": "PATIENT",
  "photoUrl": "https://...",
  "createdAt": 1701234567890
}
```

#### 3. Add ML Model
```bash
# Place your TensorFlow Lite model in assets folder
cp conv_model_float32.tflite app/src/main/assets/
```

**Model Configuration** (`app/src/main/assets/model_meta.json`):
```json
{
  "min": 60.0,
  "max": 220.0,
  "threshold": 0.015,
  "window_len": 10,
  "k_votes": 1,
  "delta": 3.0,
  "abs_critical_low_spo2": 90.0,
  "abs_critical_high_systolic": 180.0,
  "abs_critical_low_systolic": 90.0,
  "abs_critical_high_diastolic": 85.0,
  "abs_critical_high_heart_rate": 83.0,
  "abs_critical_low_heart_rate": 40.0
}
```

#### 4. Health Connect Setup

**For Patients (Data Sync):**
1. Open Samsung Health app
2. Go to Settings → Connected Services → Health Connect
3. Enable data sharing for:
   - Blood Pressure
   - Heart Rate
   - Oxygen Saturation (SpO2)
   - Steps
4. Tap "Sync now" and wait for completion
5. Verify in Health Connect app: Browse data → Vitals

**App Permissions:**
- The app will request Health Connect permissions on first launch
- Required: Heart Rate, Blood Pressure, SpO2, Steps
- Optional: Active Calories

#### 5. Build & Run
```bash
./gradlew assembleDebug
# Or click Run in Android Studio
```

---

## 📖 Detailed Feature Guides

### 🏥 Patient Features

#### Dashboard
- **Real-time vitals display** (Heart Rate, BP, SpO2, Steps)
- **Sync status indicator** (Connected/Sample Data)
- **Last sync timestamp**
- **Quick refresh button**

#### Trends View
- **Interactive trend charts** for all vitals
- **7-day historical data** visualization
- **Tap cards** to view detailed history
- **Color-coded indicators**:
  - Heart Rate: Red (#FF6B6B)
  - SpO2: Teal (#4ECDC4)
  - Blood Pressure: Mint (#95E1D3)
  - Steps: Orange (#FFBE76)

#### Analysis Tab (ML-Powered)
- **Tap "Analyze All Vitals"** for comprehensive health analysis
- **Anomaly severity levels**:
  - 🔴 CRITICAL: Immediate attention needed
  - 🟠 HIGH: Significant deviation
  - 🟡 MEDIUM: May be abnormal
  - 🟢 LOW: Minor irregularity
- **Detailed metrics**: Deviation, confidence, expected values
- **Individual vital analysis**: Analyze each metric separately

#### Disease Knowledge
- **Educational articles** about chronic diseases
- **Pull-to-refresh** for latest content
- **Category filtering**
- **Full article view** with images and tags
- **Always fresh data** (no stale cache)

### 👨‍⚕️ Doctor Features

#### Doctor Dashboard
- **"View All Patients Vitals"** card for quick access
- **Patient count summary**
- **Quick navigation** to patient monitoring

#### Patients Vitals Screen
- **Complete patient list** with latest vitals
- **Search functionality**: Find patients by name or email
- **Filter options**:
  - All Patients
  - With Vitals Data
  - Without Vitals Data
- **Sort options**:
  - Name (Alphabetical)
  - Recent Update
  - Age
  - Heart Rate
- **Expandable patient cards** showing:
  - Heart Rate (bpm)
  - Blood Pressure (systolic/diastolic mmHg)
  - SpO2 (%)
  - Steps (daily count)
- **Real-time updates** on refresh

#### Connection Requests
- **Manage patient connections**
- **Accept/Decline requests**
- **View connected patients**

---

## ⚙️ Configuration & Thresholds

### ML Model Thresholds

| Vital Type | Critical Low | Critical High | Normal Range |
|------------|--------------|---------------|--------------|
| **Heart Rate** | < 40 bpm | **> 83 bpm** | 40-83 bpm |
| **Diastolic BP** | < 40 mmHg | **> 85 mmHg** | 40-85 mmHg |
| **Systolic BP** | < 90 mmHg | > 180 mmHg | 90-180 mmHg |
| **SpO2** | **< 90%** | N/A | ≥ 90% |

### Detection Parameters
- **k_votes**: 1 (minimum windows to flag anomaly)
- **delta**: 3.0 (deviation threshold in units)
- **window_len**: 10 (sliding window size)
- **threshold**: 0.015 (MSE threshold for anomaly detection)

### Tuning Sensitivity

**More Sensitive (catch more anomalies):**
- Lower `threshold` (e.g., 0.01)
- Lower `k_votes` (e.g., 1)
- Lower `delta` (e.g., 2.0)

**Less Sensitive (fewer false alarms):**
- Higher `threshold` (e.g., 0.02)
- Higher `k_votes` (e.g., 3)
- Higher `delta` (e.g., 5.0)

---

## 🏗️ Architecture

### Project Structure
```
app/
├── src/main/
│   ├── java/com/example/chronicdiseaseapp/
│   │   ├── datamodels/          # Data classes
│   │   │   ├── HealthReading.kt
│   │   │   ├── DiseaseArticle.kt
│   │   │   └── PatientVitals.kt
│   │   ├── ml/                   # TensorFlow Lite
│   │   │   └── TFLiteModelHelper.kt
│   │   ├── repository/           # Data repositories
│   │   │   ├── HealthDataRepository.kt
│   │   │   ├── VitalsRepository.kt
│   │   │   ├── DiseaseKnowledgeRepository.kt
│   │   │   └── PatientVitalsRepository.kt
│   │   ├── screens/              # UI screens (Compose)
│   │   │   ├── patientScreen/
│   │   │   └── doctorScreen/
│   │   ├── utils/                # Utilities
│   │   └── viewModel/            # ViewModels
│   │
│   ├── assets/
│   │   ├── conv_model_float32.tflite  # ML model
│   │   └── model_meta.json             # Model config
│   │
│   └── AndroidManifest.xml
│       # Performance monitoring enabled
│       # Health Connect permissions
```

### Data Flow
```
Samsung Watch → Samsung Health → Health Connect
                                      ↓
                              HealthDataRepository
                                      ↓
                    ┌─────────────────┴─────────────────┐
                    ↓                                    ↓
          VitalsRepository                    TFLiteModelHelper
          (Firebase RTDB)                     (ML Analysis)
                    ↓                                    ↓
          HealthDataViewModel               AnomalyDetectionViewModel
                    ↓                                    ↓
              HomeScreen                           Analysis Tab
```

### Technologies Used
- **Language**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **Architecture**: MVVM (ViewModel + Repository)
- **ML Framework**: TensorFlow Lite
- **Backend**: Firebase (Auth, Firestore, Realtime Database)
- **Health Data**: Android Health Connect API
- **Image Loading**: Coil
- **Async**: Kotlin Coroutines + StateFlow
- **Performance**: Firebase Performance Monitoring

---

## 🐛 Troubleshooting

### Health Connect Issues

**Problem: No blood pressure data showing**
- **Solution**: 
  1. Ensure readings are less than 30 days old (query limit)
  2. Sync Samsung Health to Health Connect
  3. Verify data exists in Health Connect app (Browse data → Vitals)
  4. Check app has granted Blood Pressure permission

**Problem: "Using Sample Data" banner appears**
- **Solution**:
  1. Grant all required Health Connect permissions
  2. Sync data from Samsung Health
  3. Check Logcat: `adb logcat | grep HealthDataRepository`
  4. Verify Health Connect is installed and updated

**Problem: Permission denied errors**
- **Solution**:
  1. Uninstall and reinstall the app
  2. Grant permissions when prompted
  3. Check Settings → Apps → Chronic Disease App → Permissions
  4. Verify Health Connect permissions are enabled

### Firebase Issues

**Problem: "Permission denied" in Realtime Database**
- **Solution**:
  1. Update Firebase Realtime Database rules (see Quick Start)
  2. Ensure user is authenticated
  3. Check Firebase Console → Realtime Database → Rules
  4. Verify `$userId === auth.uid` is present

**Problem: No patients showing in doctor view**
- **Solution**:
  1. Ensure patients are registered with userType="PATIENT"
  2. Check Firestore `users` collection
  3. Verify patients have synced vitals data
  4. Check Firebase Realtime Database for vitals data

**Problem: Disease articles not loading**
- **Solution**:
  1. Create `diseaseArticles` collection in Firestore
  2. Add at least one article document
  3. Check network connectivity
  4. Pull-to-refresh in the app
  5. Check Logcat for specific errors

### ML Model Issues

**Problem: "Model not initialized" error**
- **Solution**:
  1. Verify `conv_model_float32.tflite` is in `app/src/main/assets/`
  2. Clean and rebuild project
  3. Check file name matches exactly (case-sensitive)
  4. Verify model file isn't corrupted

**Problem: No anomalies detected (but vitals seem abnormal)**
- **Solution**:
  1. Lower `threshold` in `model_meta.json` (try 0.01)
  2. Decrease `delta` value (try 2.0)
  3. Set `k_votes` to 1
  4. Ensure at least 10 readings exist for analysis

**Problem: Too many false positives**
- **Solution**:
  1. Increase `threshold` (try 0.02)
  2. Increase `k_votes` (try 3)
  3. Increase `delta` (try 5.0)

### Performance Issues

**Problem: Slow app startup**
- **Solution**:
  1. Use INT8 quantized model instead of float32
  2. Check Firebase Performance console for bottlenecks
  3. Enable ProGuard/R8 for release builds

**Problem: High memory usage**
- **Solution**:
  1. Clear old vitals data periodically
  2. Use pagination for large patient lists
  3. Limit trend history to last 30 days

---

## 🧪 Testing

### Manual Testing Checklist

#### Patient Flow
- [ ] User registration and login works
- [ ] Health Connect permissions requested and granted
- [ ] Dashboard displays real health data
- [ ] Sync button refreshes data from Health Connect
- [ ] Trend cards display mini charts
- [ ] Tapping trend cards opens detailed history
- [ ] Analysis tab shows ML anomaly detection results
- [ ] Disease Knowledge loads articles
- [ ] Pull-to-refresh updates disease articles
- [ ] Profile photo upload works

#### Doctor Flow
- [ ] Doctor login works
- [ ] Doctor dashboard displays "View All Patients" card
- [ ] Patients list loads all registered patients
- [ ] Search filters patients by name/email
- [ ] Sort options work correctly
- [ ] Filter options (All/With/Without data) work
- [ ] Patient cards expand to show vitals
- [ ] Real-time vitals data updates

#### ML Testing
- [ ] Go to Analysis tab
- [ ] Tap "🧪 Run Model Tests"
- [ ] Verify all 5 tests pass
- [ ] Analyze individual vitals (Heart Rate, BP, SpO2)
- [ ] Check anomaly severity colors
- [ ] Verify confidence scores and expected values

### Logcat Monitoring
```bash
# Health Connect data fetching
adb logcat | grep "HealthDataRepository"

# Firebase operations
adb logcat | grep "VitalsRepository"

# ML model operations
adb logcat | grep "TFLiteModelHelper"

# Firebase Performance
adb logcat | grep "FirebasePerformance"
```

---

## 📊 Firebase Performance Monitoring

### Automatic Metrics
Firebase automatically tracks:
- App startup time
- Screen rendering and frame rates
- HTTP/HTTPS network requests
- App foreground/background duration

### Custom Traces Added
- **app_startup_trace**: App initialization time
- **test_performance_trace**: Validation trace
- **ml_anomaly_detection**: ML inference performance
  - Attributes: vital_type, window_size, status
  - Metrics: inference_time_ms, anomalies_detected

### Viewing Performance Data
1. Go to [Firebase Console](https://console.firebase.google.com/project/chronicdiseaseapp/performance)
2. Navigate to Performance → Dashboard
3. Wait 1-3 hours for data aggregation
4. View traces, network requests, and custom metrics

---

## 🔐 Privacy & Security

### Data Protection
- ✅ Health data processed locally on device
- ✅ Per-user data isolation in Firebase
- ✅ Encrypted data transmission (Firebase)
- ✅ Health Connect permission system
- ✅ No third-party data sharing

### Compliance Considerations
⚠️ **For Production Use:**
1. Implement role-based access control (RBAC)
2. Add doctor-patient relationship constraints
3. Implement audit logging for all data access
4. Add patient consent mechanisms
5. Ensure HIPAA compliance (if applicable)
6. Add data retention policies
7. Implement secure data export

### Current Security Rules
- Users can only read/write their own data
- Doctors can read all patient vitals (authenticated users only)
- Firebase Authentication required for all operations

---

## 📈 Data Structure

### Firebase Realtime Database
```
users/
  {userId}/
    vitals/
      heartRate/
        {timestamp}/
          value: Int
          timestamp: Long
          source: String
          id: String
      bloodPressure/
        {timestamp}/
          systolic: Int
          diastolic: Int
          timestamp: Long
          source: String
          id: String
      spO2/
        {timestamp}/
          value: Int
          timestamp: Long
          source: String
          id: String
      steps/
        {timestamp}/
          value: Int
          timestamp: Long
          source: String
          id: String
```

### Firestore Collections
```
users/
  {userId}/
    uid: String
    fullName: String
    email: String
    age: Int
    userType: "PATIENT" | "DOCTOR"
    photoUrl: String (optional)
    createdAt: Timestamp

diseaseArticles/
  {articleId}/
    title: String
    content: String
    category: String
    publishedDate: Number
    imageUrl: String (optional)
    author: String (optional)
    tags: Array<String> (optional)
```

---

## 🚀 Future Enhancements

### Potential Features
1. **Advanced Analytics**
   - Comparative analysis against normal ranges
   - Correlation between different vitals
   - Predictive health trends

2. **Appointment System**
   - Schedule doctor appointments
   - Link vitals to appointments
   - Telemedicine integration

3. **Medication Tracking**
   - Medication reminders
   - Adherence monitoring
   - Drug interaction warnings

4. **Export & Reporting**
   - PDF health reports
   - CSV data export
   - Share reports with doctors

5. **Real-time Alerts**
   - Push notifications for abnormal vitals
   - Doctor alerts for critical patient conditions
   - Reminder notifications

6. **Offline Support**
   - Local database caching
   - Offline mode with sync queue
   - Better handling of network issues

7. **Multi-language Support**
   - Internationalization
   - Locale-specific health guidelines

---

## 🤝 Contributing

### Development Workflow
1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Make your changes
4. Test thoroughly (manual + unit tests)
5. Commit: `git commit -m 'Add amazing feature'`
6. Push: `git push origin feature/amazing-feature`
7. Submit a pull request

### Code Quality Standards
- Follow MVVM architecture pattern
- Use Kotlin coroutines for async operations
- Implement proper error handling
- Use StateFlow for reactive UI
- Follow Material 3 design guidelines
- Add comments for complex logic
- Write meaningful variable/function names

---

## 📄 License

[Add your license here - e.g., MIT, Apache 2.0]

---

## 👨‍💻 Support & Contact

For issues, questions, or feature requests:
- Check this documentation first
- Review Logcat logs for error details
- Verify Firebase configuration
- Check Health Connect setup

---

## 📝 Known Limitations

1. ML model trained primarily on SpO2 data (uses reconstruction error for other vitals)
2. Requires minimum 10 readings per vital for ML analysis
3. Health Connect requires Android 14+ or backported APK
4. Blood pressure query limited to last 30 days
5. Firebase Realtime Database rules allow any authenticated user to read patient vitals (needs RBAC for production)

---

## ✅ Version Information

**Version**: 1.0.0  
**Last Updated**: January 2026  
**Android Target SDK**: 34  
**Minimum SDK**: 26  
**Kotlin Version**: 1.9.0  
**Compose BOM**: 2024.04.01  
**Firebase BOM**: 33.8.0  
**TensorFlow Lite**: 2.14.0

---

## 🎯 Quick Reference Commands

### Build
```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew clean                  # Clean build
```

### Testing
```bash
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Run instrumentation tests
```

### Logs
```bash
adb logcat | grep ChronicDisease  # App logs
adb logcat *:E                    # Error logs only
adb logcat -c                     # Clear logs
```

### Firebase
```bash
# Check Firebase configuration
cat app/google-services.json

# Enable debug logging
adb shell setprop log.tag.FirebasePerformance DEBUG
```

---

**🎉 You're all set! Start monitoring health and detecting anomalies today!**
