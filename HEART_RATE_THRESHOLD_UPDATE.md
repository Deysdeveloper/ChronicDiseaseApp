# Heart Rate & Diastolic BP Threshold Updates

## 🎯 **What You Requested:**

1. **Diastolic BP:** Threshold at **85 mmHg** (above = abnormal)
2. **Heart Rate:** Threshold at **85 bpm** (above = abnormal)

---

## ✅ **All Changes Applied:**

### **Updated Thresholds:**

| Vital Type | Low Threshold | High Threshold | Previous High |
|------------|---------------|----------------|---------------|
| **Heart Rate** | 40 bpm | **85 bpm** ✅ | 200 bpm |
| **Diastolic BP** | 40 mmHg | **85 mmHg** ✅ | 120 mmHg |
| Systolic BP | 90 mmHg | 180 mmHg | 180 mmHg |
| SpO2 | 90% | No limit | No limit |

---

## 📊 **Detection Ranges:**

### **Heart Rate:**

- 🔴 **< 40 bpm** → Too Low (CRITICAL)
- ✅ **40-85 bpm** → Normal ✅
- 🔴 **> 85 bpm** → Too High (CRITICAL) ✅

### **Diastolic Blood Pressure:**

- 🔴 **< 40 mmHg** → Too Low (CRITICAL)
- ✅ **40-85 mmHg** → Normal ✅
- 🔴 **> 85 mmHg** → Too High (CRITICAL) ✅

---

## 🎯 **Examples:**

### **Heart Rate Detection:**

| Reading | Status | Alert |
|---------|--------|-------|
| 38 bpm | Too Low | 🔴 CRITICAL |
| 60 bpm | Normal | ✅ None |
| 75 bpm | Normal | ✅ None |
| 80 bpm | Normal | ✅ None |
| **86 bpm** | **High** | **🔴 CRITICAL** ✅ |
| **90 bpm** | **High** | **🔴 CRITICAL** ✅ |
| **100 bpm** | **High** | **🔴 CRITICAL** ✅ |

### **Diastolic BP Detection:**

| Reading | Status | Alert |
|---------|--------|-------|
| 35 mmHg | Too Low | 🔴 CRITICAL |
| 70 mmHg | Normal | ✅ None |
| 80 mmHg | Normal | ✅ None |
| **88 mmHg** | **High** | **🔴 CRITICAL** ✅ |
| **92 mmHg** | **High** | **🔴 CRITICAL** ✅ |

---

## 🔍 **What You'll See in the App:**

### **Heart Rate > 85:**

```
┌─────────────────────────────────────────────┐
│ ⚠️ CRITICAL: Heart Rate at 92 is           │
│ critically abnormal                         │
│                                             │
│ Time: Dec 4, 2025 12:15 pm                 │
│ Deviation: ±10.0                           │
│ Confidence: 3 votes                        │
│ Expected: 78.0                             │
└─────────────────────────────────────────────┘
```

### **Diastolic BP > 85:**

```
┌─────────────────────────────────────────────┐
│ ⚠️ CRITICAL: Diastolic BP at 90 is         │
│ critically abnormal                         │
│                                             │
│ Time: Dec 4, 2025 12:15 pm                 │
│ Deviation: ±8.0                            │
│ Confidence: 3 votes                        │
│ Expected: 78.0                             │
└─────────────────────────────────────────────┘
```

---

## 📝 **Configuration Details:**

### **model_meta.json:**

```json
{
  "abs_critical_high_heart_rate": 85.0,
  "abs_critical_low_heart_rate": 40.0,
  "abs_critical_high_diastolic": 85.0,
  "abs_critical_low_diastolic": 40.0
}
```

### **Detection Logic:**

```kotlin
VitalType.HEART_RATE -> Pair(40f, 85f)     // Low=40, High=85
VitalType.DIASTOLIC -> Pair(40f, 85f)      // Low=40, High=85
```

---

## 💡 **Medical Context:**

### **Heart Rate:**

- **Resting Normal:** 60-100 bpm (general population)
- **Athletic/Fit:** 40-60 bpm (lower is better)
- **Your Threshold:** 85 bpm (catches elevated resting HR)

**Why 85?** Helps identify:

- Stress/anxiety
- Cardiovascular issues
- Dehydration
- Early infection/fever
- Poor fitness

### **Diastolic Blood Pressure:**

- **Optimal:** < 80 mmHg
- **Elevated:** 80-89 mmHg (Stage 1 Hypertension)
- **High:** ≥ 90 mmHg (Stage 2 Hypertension)

**Your 85 threshold catches Stage 1 hypertension early!**

---

## 🧪 **Test Scenarios:**

### **Scenario 1: Normal Day**

```
Heart Rate: 70 bpm → ✅ Normal (< 85)
Diastolic BP: 75 mmHg → ✅ Normal (< 85)
Result: No alerts
```

### **Scenario 2: After Exercise**

```
Heart Rate: 95 bpm → 🔴 CRITICAL (> 85)
Diastolic BP: 78 mmHg → ✅ Normal (< 85)
Result: 1 heart rate anomaly detected
```

### **Scenario 3: Hypertension**

```
Heart Rate: 82 bpm → ✅ Normal (< 85)
Diastolic BP: 88 mmHg → 🔴 CRITICAL (> 85)
Result: 1 blood pressure anomaly detected
```

### **Scenario 4: Both Elevated**

```
Heart Rate: 92 bpm → 🔴 CRITICAL (> 85)
Diastolic BP: 90 mmHg → 🔴 CRITICAL (> 85)
Result: 2 anomalies detected (HR + BP)
```

---

## 📊 **Complete Threshold Summary:**

| Vital | Normal Range | Abnormal (CRITICAL) |
|-------|--------------|---------------------|
| **Heart Rate** | 40-85 bpm ✅ | < 40 or > 85 |
| **Diastolic BP** | 40-85 mmHg ✅ | < 40 or > 85 |
| Systolic BP | 90-180 mmHg | < 90 or > 180 |
| SpO2 | ≥ 90% | < 90% |

---

## 🔄 **What Changed:**

| File | Change |
|------|--------|
| **model_meta.json** | Added HR thresholds (40-85) |
| **TFLiteModelHelper.kt** | Load and apply HR thresholds |
| **Detection Logic** | HR > 85 → CRITICAL |
| **Logging** | Added HR threshold logs |
| **Build** | ✅ Successful |

---

## 🚀 **Ready to Use:**

1. ✅ **Heart Rate threshold:** 85 bpm
2. ✅ **Diastolic BP threshold:** 85 mmHg
3. ✅ **Build successful**
4. ✅ **Ready for testing**

---

## 🧪 **How to Test:**

### **Step 1: Check Current Data**

1. Go to Dashboard
2. Check your current heart rate
3. If > 85 bpm, it should now be flagged

### **Step 2: Run Analysis**

1. Go to Analysis tab
2. Tap "Analyze All Vitals"
3. Check for CRITICAL alerts:
    - Heart Rate > 85
    - Diastolic BP > 85

### **Step 3: Verify Logcat**

```
Heart Rate thresholds: Low=40.0, High=85.0
Diastolic BP thresholds: Low=40.0, High=85.0
```

---

## 📈 **Expected Results:**

### **If You Have Normal Vitals:**

```
Heart Rate: 65-80 bpm
Diastolic BP: 70-80 mmHg
Result: ✅ All Clear! No anomalies detected
```

### **If You Have Elevated Vitals:**

```
Heart Rate: 88-95 bpm
Diastolic BP: 88-92 mmHg
Result: 🔴 2 CRITICAL anomalies detected
  - Heart Rate at 88 is critically abnormal
  - Diastolic BP at 88 is critically abnormal
```

---

## ⚠️ **Important Notes:**

### **These are RESTING thresholds:**

- **Not during exercise** (exercise HR can be 120-180 bpm normally)
- **Not immediately after activity**
- **Best measured at rest** (sitting/lying down for 5+ min)

### **If you get frequent alerts:**

1. Verify readings are taken at rest
2. Check if watch is properly positioned
3. Consult with healthcare provider if consistently elevated

---

## 🎉 **Summary:**

**Thresholds Set:**

- ✅ Heart Rate: **85 bpm** (was 200 bpm)
- ✅ Diastolic BP: **85 mmHg** (was 120 mmHg)

**Detection:**

- ✅ Values > 85 → Flagged as CRITICAL
- ✅ Color-coded red alerts
- ✅ Detailed anomaly cards

**Status:**

- ✅ Configuration updated
- ✅ Code implemented
- ✅ Build successful
- ✅ Ready for production use

---

**All Done! Heart Rate > 85 bpm and Diastolic BP > 85 mmHg will now show as abnormal!** 🎉
