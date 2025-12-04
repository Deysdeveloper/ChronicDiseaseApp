# ML Model Integration Guide

## 📋 Overview

This guide explains how to integrate your trained TensorFlow Lite anomaly detection model into the
Chronic Disease Android App.

## ✅ What's Already Done

### 1. **Dependencies Added** (`app/build.gradle.kts`)

```kotlin
// TensorFlow Lite for ML model inference
implementation("org.tensorflow:tensorflow-lite:2.14.0")
implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
```

### 2. **Code Structure Created**

- ✅ `TFLiteModelHelper.kt` - Handles model loading and inference
- ✅ `AnomalyDetectionViewModel.kt` - ViewModel for ML operations
- ✅ `HomeScreen.kt` - Updated with Analysis tab
- ✅ `model_meta.json` - Model metadata configuration

### 3. **UI Components**

- ✅ Analysis tab in bottom navigation
- ✅ Anomaly detection results display
- ✅ Individual vital analysis buttons
- ✅ "Analyze All Vitals" button

---

## 🎯 What You Need to Do

### Step 1: Generate the TFLite Model

Run the provided Python/Colab script to generate:

1. `conv_model_float32.tflite` - Float32 TFLite model
2. `conv_model_int8.tflite` - INT8 quantized model (optional, for better performance)

**Script Output Location:**

```
/content/chronic_model/
├── conv_model_float32.tflite
├── conv_model_int8.tflite (optional)
└── model_meta.json
```

### Step 2: Copy Model Files to Android Project

```bash
# Copy the TFLite model to assets folder
cp conv_model_float32.tflite /path/to/ChronicDiseaseApp/app/src/main/assets/

# Optional: Copy INT8 model for better performance
cp conv_model_int8.tflite /path/to/ChronicDiseaseApp/app/src/main/assets/

# Model metadata is already in assets folder, update if needed
cp model_meta.json /path/to/ChronicDiseaseApp/app/src/main/assets/
```

**Expected File Structure:**

```
app/src/main/assets/
├── conv_model_float32.tflite  ← ADD THIS FILE
├── conv_model_int8.tflite     ← OPTIONAL
└── model_meta.json            ← ALREADY EXISTS (update if needed)
```

### Step 3: Update Model Metadata (if needed)

Edit `app/src/main/assets/model_meta.json`:

```json
{
  "min": 60.0,                      // Training data min value
  "max": 220.0,                     // Training data max value
  "threshold": 0.015,               // Anomaly detection threshold
  "window_len": 10,                 // Sliding window length
  "target": "systolic",             // Target vital type
  "k_votes": 2,                     // Minimum votes for anomaly
  "delta": 8.0,                     // Deviation threshold
  "abs_critical_low_spo2": 90.0,    // Critical SpO2 threshold
  "abs_critical_high_systolic": 180.0,
  "abs_critical_low_systolic": 90.0,
  "abs_critical_high_diastolic": 120.0,
  "model_version": "1.0.0",
  "description": "Conv1D Autoencoder for health vital anomaly detection"
}
```

### Step 4: Sync Gradle and Build

1. Open project in Android Studio
2. Click **"Sync Project with Gradle Files"**
3. Wait for sync to complete
4. Build and run the app

---

## 🚀 How to Use the ML Model in the App

### User Flow:

1. **Launch App** → ML model initializes automatically
2. **Navigate to "Analysis" Tab** (bottom navigation, Star icon)
3. **View Data Summary** → See how many readings are available
4. **Analyze Individual Vitals**:
    - Tap on any data card (Heart Rate, SpO2, Blood Pressure)
    - Model will analyze that specific vital
5. **Analyze All Vitals**:
    - Tap "🔍 Analyze All Vitals" button
    - Model analyzes all available health data
6. **View Results**:
    - Anomalies displayed with severity colors:
        - 🔴 CRITICAL (red)
        - 🟠 HIGH (orange)
        - 🟡 MEDIUM (yellow)
        - 🟢 LOW (green)

---

## 🧪 Testing the Integration

### Test Case 1: Model Initialization

```kotlin
// Check Logcat for:
TFLiteModelHelper: ✅ TFLite model initialized successfully
AnomalyDetectionVM: ✅ ML model initialized successfully
```

### Test Case 2: Analyze Heart Rate

```kotlin
// Should see in Logcat:
AnomalyDetectionVM: Heart Rate Analysis: X anomalies found
```

### Test Case 3: Full Analysis

```kotlin
// Should see in Logcat:
AnomalyDetectionVM: Complete Analysis: X total anomalies found
```

---

## 🔧 Model Parameters (Tuning)

You can tune these in `model_meta.json`:

| Parameter | Description | Default | Recommendation |
|-----------|-------------|---------|----------------|
| `threshold` | MSE threshold for anomaly | 0.015 | Start with 99th percentile from validation |
| `k_votes` | Min windows flagging a point | 2 | Higher = fewer false positives |
| `delta` | Deviation from local median | 8.0 | Adjust based on vital type |
| `window_len` | Sliding window size | 10 | Must match training |

### Adjusting Sensitivity:

**More Sensitive (catch more anomalies):**

- Lower `threshold` (e.g., 0.01)
- Lower `k_votes` (e.g., 1)
- Lower `delta` (e.g., 5.0)

**Less Sensitive (fewer false alarms):**

- Higher `threshold` (e.g., 0.02)
- Higher `k_votes` (e.g., 3)
- Higher `delta` (e.g., 12.0)

---

## 📊 Model Architecture Summary

**Input:**

- Shape: `[1, window_len, 1]` (batch, time steps, features)
- Type: Float32
- Range: [0, 1] (min-max normalized)

**Output:**

- Shape: `[1, window_len, 1]` (reconstructed input)
- Type: Float32
- Range: [0, 1]

**Detection Logic:**

1. Slide window across time series
2. For each window, run inference
3. Calculate MSE between input and reconstruction
4. Flag points where MSE > threshold
5. Count votes across overlapping windows
6. Apply deviation check (local median ± delta)
7. Final anomaly = (votes ≥ k_votes) AND (deviation check)

---

## 🐛 Troubleshooting

### Issue 1: Model file not found

**Error:** `Model not initialized`
**Solution:**

1. Verify `conv_model_float32.tflite` is in `app/src/main/assets/`
2. Clean and rebuild project
3. Check file name exactly matches (case-sensitive)

### Issue 2: Model initialized but no anomalies detected

**Possible Causes:**

- Not enough data points (need ≥ 10 readings)
- Threshold too high
- Data is genuinely normal
  **Solution:**
- Lower threshold in `model_meta.json`
- Increase `delta` for more sensitive deviation detection

### Issue 3: Too many false positives

**Solution:**

- Increase `threshold`
- Increase `k_votes` to 3 or 4
- Increase `delta` for deviation tolerance

### Issue 4: App crashes on analysis

**Check Logcat for:**

- Memory issues → Use INT8 model instead
- Shape mismatch → Verify window_len matches training
- NullPointerException → Ensure model file exists

---

## 📱 Performance Optimization

### Use INT8 Quantized Model:

1. Generate INT8 model from Colab script
2. Copy to assets: `conv_model_int8.tflite`
3. Update initialization:

```kotlin
modelHelper.initialize("conv_model_int8.tflite")
```

**Benefits:**

- 4x smaller model size
- 2-4x faster inference
- Lower memory usage

**Trade-off:**

- Slightly lower accuracy (~1-2%)

---

## 🔐 Firebase Integration (Future Enhancement)

To store anomaly results in Firebase:

```kotlin
// In AnomalyDetectionViewModel.kt
private fun saveAnomalyToFirebase(anomaly: AnomalyResult) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val anomalyData = mapOf(
        "timestamp" to anomaly.timestamp,
        "value" to anomaly.value,
        "vitalType" to anomaly.vitalType.name,
        "severity" to anomaly.severity.name,
        "votes" to anomaly.votes,
        "deviation" to anomaly.deviation
    )
    
    FirebaseDatabase.getInstance()
        .reference
        .child("users")
        .child(userId)
        .child("anomalies")
        .push()
        .setValue(anomalyData)
}
```

---

## 📚 Code Reference

### Key Files:

| File | Purpose | Location |
|------|---------|----------|
| `TFLiteModelHelper.kt` | Model loading & inference | `ml/` |
| `AnomalyDetectionViewModel.kt` | ML operations | `viewModel/` |
| `HomeScreen.kt` | UI integration | `screens/patientScreen/` |
| `model_meta.json` | Model configuration | `assets/` |
| `conv_model_float32.tflite` | TFLite model | `assets/` (YOU ADD THIS) |

### API Usage:

```kotlin
// Initialize (automatic in ViewModel)
val modelHelper = TFLiteModelHelper(context)
modelHelper.initialize()

// Analyze single vital
anomalyViewModel.analyzeHeartRate(heartRateReadings)

// Analyze all vitals
anomalyViewModel.analyzeAllVitals(
    heartRateData = heartRateReadings,
    spO2Data = spO2Readings,
    bloodPressureData = bpReadings
)

// Observe results
anomalyViewModel.anomalies.observe(lifecycleOwner) { anomalies ->
    // Display anomalies
}
```

---

## ✅ Integration Checklist

- [ ] Run Colab script to generate TFLite model
- [ ] Copy `conv_model_float32.tflite` to `app/src/main/assets/`
- [ ] (Optional) Copy `conv_model_int8.tflite` for better performance
- [ ] Update `model_meta.json` with correct parameters
- [ ] Sync Gradle files in Android Studio
- [ ] Build and run app
- [ ] Navigate to Analysis tab
- [ ] Test "Analyze All Vitals" button
- [ ] Verify anomalies are detected and displayed
- [ ] Check Logcat for successful initialization
- [ ] Test with real Health Connect data
- [ ] Tune parameters if needed

---

## 🎉 You're Done!

Once you copy the `.tflite` file to the assets folder, the app will:

- ✅ Automatically load the model on startup
- ✅ Provide anomaly detection UI in Analysis tab
- ✅ Display results with severity levels
- ✅ Log all operations for debugging

**Need Help?** Check Logcat for detailed logs with tags:

- `TFLiteModelHelper`
- `AnomalyDetectionVM`

---

## 📞 Support

For issues or questions:

1. Check Logcat logs
2. Review this guide
3. Verify model file exists in assets
4. Test with sample data first
