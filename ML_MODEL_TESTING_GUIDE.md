# 🧪 ML Model Testing Guide

## ✅ Build Status: SUCCESSFUL

Your app has been built successfully with ML model integration. Now let's verify it's working!

---

## 🚀 How to Test the ML Model

### Method 1: In-App Test Button (Easiest!)

1. **Launch the App**
    - Install and run on your device/emulator
    - Login to your account

2. **Navigate to Analysis Tab**
    - Tap the **"Analysis"** tab (⭐ Star icon in bottom navigation)

3. **Check Model Status**
    - You should see: **"✅ ML Model Ready"** (green card)
    - If you see "⏳ Initializing ML Model..." wait a few seconds

4. **Run Tests**
    - Look for the **"🧪 Run Model Tests"** button (blue)
    - Tap the button
    - Wait 2-5 seconds

5. **View Results**
    - You'll see either:
        - ✅ **"ALL TESTS PASSED (5/5)"** (green card) → Model is working perfectly!
        - ⚠️ **"X/5 TESTS PASSED"** (yellow card) → Check Logcat for details

---

### Method 2: Logcat Monitoring (Detailed Verification)

#### Step 1: Open Logcat

In Android Studio:

- View → Tool Windows → Logcat
- Or press `Alt+6` (Windows/Linux) / `Cmd+6` (Mac)

#### Step 2: Filter Logs

In the filter box, type: `ModelTester`

#### Step 3: Run the Tests

- Tap "🧪 Run Model Tests" in the app

#### Step 4: Check Test Results

You should see:

```
========================================
Starting ML Model Integration Tests
========================================

--- Test 1: Model Initialization ---
TFLiteModelHelper: Metadata loaded - Min: 92.214, Max: 100.0, Threshold: 0.0189
TFLiteModelHelper: ✅ TFLite model initialized successfully
✅ PASS: Model initialized successfully

--- Test 2: Single Window Inference ---
Input: 98.0, 97.0, 98.0, 99.0, 98.0, 97.0, 98.0, 99.0, 97.0, 98.0
MSE: 0.0012, 0.0015, 0.0011, ...
Mask: false, false, false, ...
Anomalies detected: 0 / 10
✅ PASS: Inference completed with correct output shape

--- Test 3: Normal Series Detection ---
Testing with 30 normal SpO2 readings (97-99%)
Anomalies detected: 0 / 30
✅ PASS: Normal data correctly identified (0 anomalies)

--- Test 4: Anomaly Series Detection ---
Testing with SpO2 data containing anomalies
Values: 98.0, 97.0, 98.0, 99.0, 98.0, 85.0, 87.0, 88.0, ...
Anomalies detected: 5 / 20
  - CRITICAL: Value=85.0, Votes=4, Deviation=13.2
  - CRITICAL: Value=87.0, Votes=3, Deviation=11.1
  - HIGH: Value=88.0, Votes=2, Deviation=10.3
✅ PASS: Anomalies detected (5 total, 3 critical)

--- Test 5: NaN Handling ---
Testing with NaN values
Input: 98.0, NaN, 97.0, 98.0, NaN, 99.0, 98.0, 97.0, 98.0, 99.0
MSE: 0.0013, 0.0014, 0.0012, ...
✅ PASS: NaN values handled correctly

========================================
ML Model Test Summary
========================================
Test 1 - Model Initialization:     ✅ PASS
Test 2 - Single Window Inference:  ✅ PASS
Test 3 - Normal Series Detection:  ✅ PASS
Test 4 - Anomaly Series Detection: ✅ PASS
Test 5 - NaN Handling:              ✅ PASS
----------------------------------------
Total: 5 / 5 tests passed
🎉 ALL TESTS PASSED! Model is working correctly.
========================================
```

---

## 📊 What Each Test Verifies

### Test 1: Model Initialization

- ✅ Model file exists and loads
- ✅ Metadata JSON is parsed correctly
- ✅ TensorFlow Lite interpreter creates successfully
- ✅ Model configuration is valid

### Test 2: Single Window Inference

- ✅ Model accepts input shape [1, 10, 1]
- ✅ Model produces output shape [1, 10, 1]
- ✅ MSE calculation works
- ✅ Anomaly masking works
- ✅ No crashes during inference

### Test 3: Normal Series Detection

- ✅ Sliding window mechanism works
- ✅ Normal data (97-99% SpO2) is recognized
- ✅ Low false positive rate
- ✅ Voting mechanism works

### Test 4: Anomaly Series Detection

- ✅ Detects critical drops (85-88% SpO2)
- ✅ Severity classification works
- ✅ Deviation calculation works
- ✅ Critical thresholds enforced

### Test 5: NaN Handling

- ✅ Handles missing data gracefully
- ✅ Median imputation works
- ✅ No NaN propagation to output
- ✅ No crashes with incomplete data

---

## ✅ Success Criteria

### All Tests Should Pass:

```
✅ Test 1 - Model Initialization
✅ Test 2 - Single Window Inference
✅ Test 3 - Normal Series Detection
✅ Test 4 - Anomaly Series Detection
✅ Test 5 - NaN Handling
```

### If 5/5 Tests Pass:

🎉 **Your ML model is fully functional and ready for production use!**

---

## 🐛 Troubleshooting

### Test 1 Fails (Model Initialization)

**Symptoms:** ❌ FAIL: Model initialization failed

**Possible Causes:**

- Model file not found
- Model file corrupted
- Metadata JSON invalid

**Solutions:**

1. Verify files exist:
   ```bash
   ls -lh app/src/main/assets/*.tflite
   ls -lh app/src/main/assets/model_meta.json
   ```
2. Check file names match exactly:
    - `conv_model_float32.tflite` (no spaces!)
3. Rebuild project:
    - Build → Clean Project
    - Build → Rebuild Project

### Test 2 Fails (Single Window Inference)

**Symptoms:** ❌ FAIL: Output shape mismatch

**Possible Causes:**

- Model input/output shape mismatch
- Model not compatible with TFLite

**Solutions:**

1. Check Logcat for detailed error
2. Verify model was exported correctly from Python
3. Try INT8 model instead:
    - Edit `TFLiteModelHelper.kt` line 56
    - Change `conv_model_float32.tflite` → `conv_model_int8.tflite`

### Test 3 Fails (Normal Series)

**Symptoms:** Too many anomalies for normal data

**Possible Causes:**

- Threshold too low
- Model too sensitive

**Solutions:**

1. Increase threshold in `model_meta.json`:
   ```json
   "threshold": 0.025  (increase from 0.0189)
   ```
2. Increase k_votes:
   ```json
   "k_votes": 3  (increase from 2)
   ```

### Test 4 Fails (Anomaly Detection)

**Symptoms:** Expected anomalies not detected

**Possible Causes:**

- Threshold too high
- Critical thresholds incorrect

**Solutions:**

1. Lower threshold:
   ```json
   "threshold": 0.012  (decrease from 0.0189)
   ```
2. Check critical thresholds:
   ```json
   "abs_critical_low_spo2": 90.0
   ```

### Test 5 Fails (NaN Handling)

**Symptoms:** NaN propagated to output or crash

**Possible Causes:**

- Median calculation error
- Imputation logic issue

**Solutions:**

1. Check Logcat for stack trace
2. Verify data preprocessing in `TFLiteModelHelper.kt`

---

## 🎯 Real Data Testing

After tests pass, test with real health data:

### 1. Load Real SpO2 Data

- Go to **Dashboard** tab
- Tap **Refresh** button
- Wait for Health Connect data to load
- Verify you see SpO2 readings

### 2. Analyze Real Data

- Go to **Analysis** tab
- Tap **"SpO2 Readings"** card
- Or tap **"🔍 Analyze All Vitals"**
- Wait for analysis

### 3. Verify Results Make Sense

- Normal SpO2 (97-100%): Few or no anomalies
- Low SpO2 (< 90%): Should be flagged as CRITICAL
- Sudden drops: Should be detected

---

## 📈 Performance Metrics

### Expected Performance:

| Metric | Value | Notes |
|--------|-------|-------|
| Model Load Time | < 1 sec | One-time on app launch |
| Single Window Inference | < 10 ms | Per 10-point window |
| Series Analysis (30 points) | < 200 ms | Includes all windows |
| Memory Usage | < 50 MB | Model + inference |

### Check Performance in Logcat:

Filter by `TFLiteModelHelper` and look for timing info.

---

## 🔄 Testing with Different Scenarios

### Scenario 1: All Normal Data

**Input:** 30 readings of 97-99%
**Expected:** 0-1 anomalies detected
**Pass Criteria:** Low false positive rate

### Scenario 2: Single Anomaly

**Input:** 98, 98, 98, 85, 98, 98, 98...
**Expected:** 1 critical anomaly at the 85
**Pass Criteria:** Anomaly detected with high confidence

### Scenario 3: Gradual Decline

**Input:** 99, 98, 97, 96, 95, 94, 93, 92...
**Expected:** Some anomalies at lower values
**Pass Criteria:** Detects trend below normal range

### Scenario 4: Noisy Data

**Input:** 98, 97, 99, 96, 98, 100, 97...
**Expected:** 0-1 anomalies
**Pass Criteria:** Robust to normal variation

---

## 📝 Test Report Template

After running tests, document results:

```
═══════════════════════════════════════
ML MODEL TEST REPORT
═══════════════════════════════════════
Date: [Date]
Device: [Device Name/Emulator]
App Version: 1.0.0
Model Version: 1.0.0

TEST RESULTS:
✅/❌ Test 1 - Model Initialization
✅/❌ Test 2 - Single Window Inference
✅/❌ Test 3 - Normal Series Detection
✅/❌ Test 4 - Anomaly Series Detection
✅/❌ Test 5 - NaN Handling

TOTAL: X / 5 PASSED

REAL DATA TEST:
- SpO2 readings analyzed: X
- Anomalies detected: X
- Critical anomalies: X
- Results accuracy: Good/Fair/Poor

NOTES:
[Any observations or issues]

STATUS: ✅ PASS / ❌ FAIL
═══════════════════════════════════════
```

---

## 🎉 Next Steps After Tests Pass

### 1. **Production Testing**

- Test with multiple users
- Monitor for false positives
- Collect user feedback

### 2. **Parameter Tuning**

- Adjust threshold based on real data
- Fine-tune sensitivity
- Update metadata JSON as needed

### 3. **Performance Optimization**

- Switch to INT8 if needed
- Monitor battery usage
- Optimize for specific devices

### 4. **Feature Enhancement**

- Add historical tracking
- Implement alerts
- Create doctor dashboard
- Add trend visualization

---

## 📚 Additional Resources

- **Code:** `app/src/main/java/.../ml/ModelTester.kt`
- **Documentation:** `ML_MODEL_VERIFIED_READY.md`
- **Quick Start:** `QUICK_ML_SETUP.md`
- **Full Guide:** `ML_MODEL_INTEGRATION_GUIDE.md`

---

## ✅ Final Checklist

- [ ] App builds successfully
- [ ] Model file verified (268 KB)
- [ ] Metadata updated for SpO2
- [ ] Test button appears in Analysis tab
- [ ] All 5 tests pass
- [ ] Real data analysis works
- [ ] Results make sense
- [ ] No crashes or errors

---

**Ready to test?** Launch the app and tap "🧪 Run Model Tests"! 🚀

**Expected Result:** ✅ ALL TESTS PASSED (5/5)
