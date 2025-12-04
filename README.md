# Chronic Disease Management App

An Android health monitoring application with ML-powered anomaly detection for chronic disease
patients.

---

## 📱 Features

### ✅ Health Data Tracking

- **Heart Rate** monitoring (from Samsung Galaxy Watch 4 via Health Connect)
- **Blood Pressure** tracking (Systolic/Diastolic)
- **SpO2** (Oxygen Saturation) monitoring
- **Steps** counting and activity tracking

### 🤖 ML-Powered Anomaly Detection

- TensorFlow Lite model for real-time health anomaly detection
- Custom thresholds for each vital sign
- Color-coded severity alerts (CRITICAL, HIGH, MEDIUM, LOW)

### 📊 Health Analytics

- Interactive trend charts (7-day history)
- Dashboard with real-time health metrics
- Detailed vital history with timestamps

### 🔐 User Management

- Firebase Authentication (Email/Password, Google Sign-In)
- User profiles with photo upload
- Role-based access (Patient/Doctor)

### ☁️ Cloud Sync

- Firebase Realtime Database for data persistence
- Cross-device synchronization
- Per-user data isolation

---

## 🚀 Quick Start

### Prerequisites

- Android Studio (latest version)
- Android device with **Health Connect** installed
- Samsung Galaxy Watch 4 (or compatible wearable)
- Firebase project setup

### Installation

1. **Clone the repository**
   ```bash
   git clone <your-repo-url>
   cd ChronicDiseaseApp
   ```

2. **Open in Android Studio**
    - File → Open → Select project folder
    - Wait for Gradle sync

3. **Add Firebase Configuration**
    - Place your `google-services.json` in `app/` directory
    - Configure Firebase Authentication and Realtime Database

4. **Add ML Model**
    - Place `conv_model_float32.tflite` in `app/src/main/assets/`
    - Model metadata is already configured in `model_meta.json`

5. **Build & Run**
   ```bash
   ./gradlew assembleDebug
   ```

---

## 📖 Documentation

| Document | Description |
|----------|-------------|
| [START_HERE.md](START_HERE.md) | Complete setup guide |
| [QUICK_START_GUIDE.md](QUICK_START_GUIDE.md) | Quick setup instructions |
| [ML_MODEL_INTEGRATION_GUIDE.md](ML_MODEL_INTEGRATION_GUIDE.md) | ML model setup & configuration |
| [QUICK_ML_SETUP.md](QUICK_ML_SETUP.md) | Quick ML setup (3 steps) |
| [QUICK_FIREBASE_SETUP.md](QUICK_FIREBASE_SETUP.md) | Firebase configuration guide |
| [HEART_RATE_THRESHOLD_UPDATE.md](HEART_RATE_THRESHOLD_UPDATE.md) | Threshold configuration details |
| [FIREBASE_REALTIME_DB_INTEGRATION.md](FIREBASE_REALTIME_DB_INTEGRATION.md) | Database structure & integration |
| [HEALTH_CONNECT_TROUBLESHOOTING.md](HEALTH_CONNECT_TROUBLESHOOTING.md) | Health Connect setup & debugging |
| [README_SAMSUNG_HEALTH_INTEGRATION.md](README_SAMSUNG_HEALTH_INTEGRATION.md) | Samsung Health integration |
| [SYNC_SAMSUNG_HEALTH_TO_HEALTH_CONNECT.md](SYNC_SAMSUNG_HEALTH_TO_HEALTH_CONNECT.md) | Data sync guide |
| [TRENDS_FEATURE_DOCUMENTATION.md](TRENDS_FEATURE_DOCUMENTATION.md) | Trend charts documentation |

---

## ⚙️ ML Model Configuration

### Current Thresholds

| Vital Type | Critical Low | Critical High | Normal Range |
|------------|--------------|---------------|--------------|
| **Heart Rate** | < 40 bpm | **> 83 bpm** | 40-83 bpm |
| **Diastolic BP** | < 40 mmHg | **> 85 mmHg** | 40-85 mmHg |
| **Systolic BP** | < 90 mmHg | > 180 mmHg | 90-180 mmHg |
| **SpO2** | **< 90%** | N/A | ≥ 90% |

### Detection Parameters

- **k_votes:** 1 (minimum windows to flag anomaly)
- **delta:** 3.0 (deviation threshold in units)
- **window_len:** 10 (sliding window size)

**Configuration file:** `app/src/main/assets/model_meta.json`

---

## 🏗️ Architecture

```
app/
├── src/main/
│   ├── java/com/example/chronicdiseaseapp/
│   │   ├── datamodels/          # Data classes
│   │   ├── ml/                   # TensorFlow Lite ML model
│   │   ├── repository/           # Data repositories
│   │   ├── screens/              # UI screens (Compose)
│   │   ├── utils/                # Utility classes
│   │   └── viewModel/            # ViewModels
│   │
│   └── assets/
│       ├── conv_model_float32.tflite  # ML model
│       └── model_meta.json             # Model config
```

---

## 🔧 Technologies Used

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Architecture:** MVVM (ViewModel + Repository)
- **ML Framework:** TensorFlow Lite
- **Backend:** Firebase (Auth + Realtime Database)
- **Health Data:** Android Health Connect API
- **Wearable:** Samsung Galaxy Watch 4 integration

---

## 📊 Key Features Breakdown

### 1. Health Dashboard

- Real-time vital signs display
- Sync status indicator
- Last sync timestamp
- Data source info (Health Connect)

### 2. Trends View

- 7-day historical charts
- Tap cards for detailed history
- Color-coded trend indicators
- All vitals in one place

### 3. Analysis Tab (ML)

- Tap "Analyze All Vitals" for ML analysis
- Color-coded anomaly cards:
    - 🔴 **CRITICAL** - Immediate attention needed
    - 🟠 **HIGH** - Significant deviation
    - 🟡 **MEDIUM** - May be abnormal
    - 🟢 **LOW** - Minor irregularity
- Shows deviation, confidence, and expected values

### 4. Profile Management

- Upload profile photo
- Age and personal info
- Privacy settings
- Data permissions management

---

## 🛡️ Privacy & Security

- ✅ Health data stays on device (local processing)
- ✅ Firebase Authentication for secure login
- ✅ Per-user data isolation
- ✅ Health Connect permission system
- ✅ No third-party data sharing
- ✅ GDPR-compliant privacy notices

---

## 🧪 Testing

### Run ML Model Tests

1. Go to Analysis tab
2. Tap "🧪 Run Model Tests"
3. Verify all 5 tests pass

### Manual Testing

1. Load health data from Dashboard (tap refresh)
2. Go to Analysis tab
3. Tap "Analyze All Vitals"
4. Check for anomaly detection

---

## 📝 Known Issues & Limitations

- ML model trained on SpO2 data (uses reconstruction error for other vitals)
- Requires minimum 10 readings per vital for analysis
- Health Connect requires Android 14+ or backported APK
- Galaxy Watch 4 sync requires Samsung Health app

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

---

## 📄 License

[Add your license here]

---

## 👨‍💻 Author

[Your name/team]

---

## 🆘 Support

For issues and questions:

- Check documentation files (see table above)
- Review `HEALTH_CONNECT_TROUBLESHOOTING.md` for common issues
- Check Logcat for error messages

---

**Version:** 1.0.0  
**Last Updated:** December 2024
