# 📦 TensorFlow Lite Model Files

## ⚠️ IMPORTANT: Place Your TFLite Model Here

This folder is ready for your ML model files. Currently **missing**:

### Required File:

```
✗ conv_model_float32.tflite  ← ADD THIS FILE HERE
```

### Optional File (for better performance):

```
✗ conv_model_int8.tflite     ← RECOMMENDED: Faster & smaller
```

---

## 🚀 Quick Setup

### Step 1: Generate Model

Run the provided Python/Colab script to generate the TFLite model files.

### Step 2: Copy to This Folder

```bash
# Copy the model file here:
cp /path/to/conv_model_float32.tflite app/src/main/assets/

# Optional: Copy INT8 version
cp /path/to/conv_model_int8.tflite app/src/main/assets/
```

### Step 3: Build & Run

- Sync Gradle
- Build the app
- Navigate to "Analysis" tab
- Tap "Analyze All Vitals"

---

## 📁 Expected File Structure

```
app/src/main/assets/
├── conv_model_float32.tflite  ← YOUR MODEL (REQUIRED)
├── conv_model_int8.tflite     ← QUANTIZED MODEL (OPTIONAL)
├── model_meta.json            ← CONFIGURATION (ALREADY EXISTS)
└── README_PLACE_MODEL_HERE.md ← THIS FILE
```

---

## ✅ Verification

After adding the model, check:

1. File size: ~50-200 KB (float32) or ~10-50 KB (int8)
2. File extension: `.tflite`
3. File name matches exactly (case-sensitive)

---

## 🐛 Troubleshooting

**Model not loading?**

- Ensure file name is exactly `conv_model_float32.tflite`
- Check file is in `app/src/main/assets/` (not in subdirectory)
- Clean and rebuild project
- Check Logcat for "TFLiteModelHelper" logs

**Need detailed instructions?**
→ See `ML_MODEL_INTEGRATION_GUIDE.md` in project root

---

## 📊 Model Info

- **Type:** Conv1D Autoencoder
- **Purpose:** Anomaly detection in health vitals
- **Input:** 10-point sliding window
- **Output:** Reconstructed window + anomaly flags
- **Framework:** TensorFlow Lite
- **Version:** 2.14.0
