# Health Trends Feature Documentation

## 🎯 Overview

The Health Trends feature provides users with visual representations of their health vitals over
time, allowing them to track patterns and monitor their health progress.

---

## ✨ Features Added

### 1. **Multiple Trend Cards**

The dashboard now displays **4 trend cards** instead of just heart rate:

1. **Heart Rate Trends** (Red - #FF6B6B)
2. **SpO2 Trends** (Teal - #4ECDC4)
3. **Blood Pressure Trends** (Mint - #95E1D3)
4. **Steps Trends** (Orange - #FFBE76)

### 2. **Interactive Trend Cards**

- Each trend card is **clickable**
- Displays mini chart with last 7 data points
- Shows current value and trend percentage
- Color-coded for easy identification

### 3. **Detailed Trend View**

When clicking on any trend card, a **detailed dialog** opens showing:

- Complete history of all readings (oldest to latest)
- Date and time of each reading
- Source of data (Health Connect, Samsung Health, etc.)
- Numbered readings for easy reference
- Color-coded values matching the trend card

---

## 📊 Trend Card Components

### **Card Information:**

Each trend card displays:

```
┌─────────────────────────────────────┐
│ Heart Rate Trends          +2%      │
│ 72 bpm                 Last 7 Days  │
│                                     │
│     📈 Mini Chart                   │
│                                     │
│                  Tap to view details│
└─────────────────────────────────────┘
```

**Fields:**

- **Title**: Type of vital (e.g., "Heart Rate Trends")
- **Current Value**: Latest reading with unit
- **Trend Percentage**: Change over last 7 days
- **Mini Chart**: Visual representation of last 7 readings
- **Call to Action**: "Tap to view details"

---

## 📱 User Interaction Flow

### **Step 1: View Dashboard**

User sees all 4 trend cards on the dashboard:

```
Dashboard
├── Health Metrics (2x2 Grid)
└── Trends (Scrollable)
    ├── Heart Rate Trends
    ├── SpO2 Trends
    ├── Blood Pressure Trends
    └── Steps Trends
```

### **Step 2: Tap on Trend Card**

User taps on any trend card (e.g., "Heart Rate Trends")

### **Step 3: View Detailed History**

Dialog opens showing complete history:

```
┌─────────────────────────────────────┐
│ Heart Rate History            ✕     │
│ Showing 25 readings from old to new │
├─────────────────────────────────────┤
│ #1  Dec 01, 2024 08:30 AM     72 bpm│
│     Health Connect                   │
├─────────────────────────────────────┤
│ #2  Dec 01, 2024 09:45 AM     75 bpm│
│     Health Connect                   │
├─────────────────────────────────────┤
│ ... (scrollable list)                │
├─────────────────────────────────────┤
│ #25 Dec 07, 2024 07:15 PM     71 bpm│
│     Health Connect                   │
├─────────────────────────────────────┤
│           [Close Button]             │
└─────────────────────────────────────┘
```

---

## 🎨 Visual Design

### **Color Scheme:**

Each vital type has a unique color:

| Vital Type | Color | Hex Code |
|------------|-------|----------|
| Heart Rate | Red | #FF6B6B |
| SpO2 | Teal | #4ECDC4 |
| Blood Pressure | Mint | #95E1D3 |
| Steps | Orange | #FFBE76 |

### **Chart Visualization:**

- **Line chart** showing data trends
- **Auto-normalized** to fit card height
- **Smooth curves** for better visualization
- **4px stroke width** for clarity

---

## 💾 Data Handling

### **Data Source:**

All trend data comes from:

1. **Health Connect API** (primary source)
2. **Firebase Realtime Database** (persistent storage)
3. **Sample data** (fallback when no real data)

### **Data Sorting:**

```kotlin
// Data is sorted by timestamp (oldest to latest)
val sortedData = healthDataViewModel.heartRateData
    .sortedBy { it.timestamp }
```

### **Display Logic:**

- **Mini Chart**: Shows last 7 readings
- **Detail View**: Shows ALL available readings
- **Empty State**: Shows "No data available" message

---

## 🔧 Technical Implementation

### **1. Data Observation**

```kotlin
// Observe all health data types
val heartRateData by healthDataViewModel.heartRateData.observeAsState(emptyList())
val bloodPressureData by healthDataViewModel.bloodPressureData.observeAsState(emptyList())
val spO2Data by healthDataViewModel.spO2Data.observeAsState(emptyList())
val stepsData by healthDataViewModel.stepsData.observeAsState(emptyList())
```

### **2. Trend Card Component**

```kotlin
TrendCard(
    title = "Heart Rate Trends",
    currentValue = healthMetrics?.getDisplayText("heartRate") ?: "—",
    unit = "bpm",
    trend = healthMetrics?.weeklyTrend,
    color = Color(0xFFFF6B6B),
    dataPoints = heartRateData.takeLast(7).mapNotNull { it.heartRate?.toFloat() },
    onClick = { showTrendDetail = TrendType.HEART_RATE }
)
```

### **3. Detail Dialog Component**

```kotlin
TrendDetailDialog(
    trendType = TrendType.HEART_RATE,
    data = heartRateData.sortedBy { it.timestamp },
    onDismiss = { showTrendDetail = null }
)
```

---

## 📋 Code Structure

### **New Components Added:**

#### **1. TrendCard (@Composable)**

**Purpose:** Display individual trend card with mini chart

**Parameters:**

- `title: String` - Trend title (e.g., "Heart Rate Trends")
- `currentValue: String` - Current reading value
- `unit: String` - Unit of measurement
- `trend: HealthTrend?` - Trend direction (IMPROVING/DECLINING/STABLE)
- `color: Color` - Card accent color
- `dataPoints: List<Float>` - Last 7 readings for mini chart
- `onClick: () -> Unit` - Click handler

#### **2. TrendDetailDialog (@Composable)**

**Purpose:** Show detailed history of all readings

**Parameters:**

- `trendType: TrendType` - Which vital type to display
- `data: List<HealthReading>` - All readings sorted by timestamp
- `onDismiss: () -> Unit` - Close dialog handler

#### **3. TrendDataItem (@Composable)**

**Purpose:** Display individual reading in detail view

**Parameters:**

- `reading: HealthReading` - Single health reading
- `trendType: TrendType` - Vital type
- `index: Int` - Reading number (1, 2, 3...)
- `total: Int` - Total number of readings

#### **4. TrendType (Enum)**

**Purpose:** Define different types of trends

```kotlin
enum class TrendType {
    HEART_RATE,
    SPO2,
    BLOOD_PRESSURE,
    STEPS
}
```

---

## 🔍 Data Format Examples

### **Heart Rate Reading:**

```kotlin
HealthReading(
    id = "uuid-123",
    timestamp = 1701234567890,
    heartRate = 72,
    source = "Health Connect"
)
```

### **Blood Pressure Reading:**

```kotlin
HealthReading(
    id = "uuid-456",
    timestamp = 1701234567890,
    bloodPressureSystolic = 120,
    bloodPressureDiastolic = 80,
    source = "Health Connect"
)
```

### **SpO2 Reading:**

```kotlin
HealthReading(
    id = "uuid-789",
    timestamp = 1701234567890,
    oxygenSaturation = 98,
    source = "Health Connect"
)
```

### **Steps Reading:**

```kotlin
HealthReading(
    id = "uuid-012",
    timestamp = 1701234567890,
    stepsCount = 8543,
    source = "Health Connect"
)
```

---

## 🎯 User Experience Features

### **1. Responsive Design**

- Cards adapt to different screen sizes
- Scrollable trend section for smaller screens
- Full-screen detail dialog for better readability

### **2. Visual Feedback**

- **Clickable cards** with subtle shadow
- **Color coding** for easy identification
- **Trend indicators** (+2%, -3%, 0%)
- **Loading states** while fetching data

### **3. Empty States**

- **"No data available"** message when no readings
- **Info icon** to indicate empty state
- Maintains layout consistency

### **4. Date Formatting**

- **User-friendly format**: "Dec 01, 2024 08:30 AM"
- **Consistent across app**
- **Locale-aware** (respects device settings)

---

## 📊 Mini Chart Implementation

### **Chart Features:**

- **Auto-normalization**: Scales data to fit card height
- **Smooth line rendering**: Uses Canvas Path API
- **Performance optimized**: Only last 7 points rendered
- **Graceful fallback**: Shows "No data" if empty

### **Normalization Logic:**

```kotlin
// Find min/max values
val minValue = dataPoints.minOrNull() ?: 0f
val maxValue = dataPoints.maxOrNull() ?: 1f
val range = maxValue - minValue

// Normalize to 0-1 range
val normalizedPoints = if (range > 0) {
    dataPoints.map { (it - minValue) / range }
} else {
    dataPoints.map { 0.5f } // Center line if all values same
}

// Draw on canvas
normalizedPoints.forEachIndexed { index, v ->
    val x = width * (index.toFloat() / (size - 1).coerceAtLeast(1))
    val y = height * (1f - v) // Invert Y axis
    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
}
```

---

## 🧪 Testing the Feature

### **Test Scenario 1: View All Trends**

1. Open app and login
2. Navigate to Dashboard
3. Scroll down to "Trends" section
4. Verify all 4 trend cards are visible:
    - ✅ Heart Rate Trends (Red)
    - ✅ SpO2 Trends (Teal)
    - ✅ Blood Pressure Trends (Mint)
    - ✅ Steps Trends (Orange)

### **Test Scenario 2: View Heart Rate Details**

1. Tap on "Heart Rate Trends" card
2. Dialog opens showing heart rate history
3. Verify:
    - ✅ All readings shown (oldest to latest)
    - ✅ Date/time formatted correctly
    - ✅ Values displayed with unit (bpm)
    - ✅ Source shown (Health Connect)
    - ✅ Readings are numbered (#1, #2, etc.)
4. Scroll through list
5. Tap "Close" button or X icon
6. Dialog closes

### **Test Scenario 3: View SpO2 Details**

1. Tap on "SpO2 Trends" card
2. Verify same functionality as heart rate
3. Values shown with % unit
4. Color is teal (#4ECDC4)

### **Test Scenario 4: View Blood Pressure Details**

1. Tap on "Blood Pressure Trends" card
2. Verify values shown as "120/80 mmHg"
3. Both systolic and diastolic displayed
4. Color is mint (#95E1D3)

### **Test Scenario 5: View Steps Details**

1. Tap on "Steps Trends" card
2. Verify values shown as number with "steps" unit
3. Color is orange (#FFBE76)

### **Test Scenario 6: Empty State**

1. Clear app data or use new account with no readings
2. Open Dashboard
3. Verify mini charts show "No data available"
4. Tap on trend card
5. Verify detail dialog shows "No data available" message
6. Close button still works

---

## 🐛 Troubleshooting

### **Issue 1: Trend Cards Not Showing**

**Symptoms:** Blank space where trends should be

**Solutions:**

- Check that `healthDataViewModel` is properly initialized
- Verify data is being fetched from Health Connect
- Check Logcat for errors
- Ensure user has granted health permissions

### **Issue 2: No Data in Detail View**

**Symptoms:** "No data available" shown when data should exist

**Solutions:**

- Verify data is stored in Firebase Realtime Database
- Check that `sortedBy { it.timestamp }` doesn't filter out data
- Ensure `observeAsState()` is working correctly
- Call `loadHealthData()` or `loadHealthDataFromFirebase()`

### **Issue 3: Mini Charts Not Rendering**

**Symptoms:** Charts appear empty or broken

**Solutions:**

- Check that dataPoints list is not empty
- Verify data values are valid floats
- Ensure Canvas has proper dimensions
- Check for null values in data

### **Issue 4: Dialog Not Opening**

**Symptoms:** Clicking trend card does nothing

**Solutions:**

- Verify `showTrendDetail` state is being updated
- Check that `onClick` lambda is properly defined
- Ensure no other UI elements blocking clicks
- Check Logcat for click event logs

---

## 🚀 Future Enhancements

### **Potential Improvements:**

1. **Date Range Selector**
    - Allow users to select custom date ranges
    - Options: Last 7 days, 30 days, 3 months, 1 year

2. **Export Data**
    - Export trends to CSV/PDF
    - Share with doctors
    - Email reports

3. **Advanced Analytics**
    - Average, min, max calculations
    - Trend analysis (improving/declining percentages)
    - Anomaly detection
    - Predictions using ML

4. **Comparison View**
    - Compare multiple vitals side-by-side
    - Overlay charts
    - Correlation analysis

5. **Annotations**
    - Add notes to specific readings
    - Mark important events
    - Tag readings (exercise, medication, etc.)

6. **Better Visualizations**
    - Bar charts for steps
    - Area charts for heart rate
    - Scatter plots for correlations
    - Animated transitions

7. **Goals & Targets**
    - Set health goals
    - Visual indicators for goal progress
    - Achievements/badges

8. **Alerts & Notifications**
    - Alert when vitals are abnormal
    - Remind to check vitals
    - Weekly summary notifications

---

## 📚 Related Files

| File | Purpose |
|------|---------|
| `HomeScreen.kt` | Main dashboard with trend cards |
| `HealthDataViewModel.kt` | Provides health data observables |
| `HealthDataRepository.kt` | Fetches data from Health Connect |
| `VitalsRepository.kt` | Manages Firebase data storage |
| `HealthReading.kt` | Data model for health readings |

---

## 🎓 Code Examples

### **Example 1: Adding a New Trend Type**

To add a new vital (e.g., Body Temperature):

1. **Add to TrendType enum:**

```kotlin
enum class TrendType {
    HEART_RATE,
    SPO2,
    BLOOD_PRESSURE,
    STEPS,
    BODY_TEMPERATURE // NEW
}
```

2. **Add trend card in HomeScreen:**

```kotlin
TrendCard(
    title = "Body Temperature Trends",
    currentValue = healthMetrics?.getDisplayText("bodyTemp") ?: "—",
    unit = "°F",
    trend = healthMetrics?.weeklyTrend,
    color = Color(0xFFE74C3C), // Red color
    dataPoints = bodyTempData.takeLast(7).mapNotNull { it.bodyTemperature?.toFloat() },
    onClick = { showTrendDetail = TrendType.BODY_TEMPERATURE }
)
```

3. **Update detail dialog switch:**

```kotlin
showTrendDetail?.let { trendType ->
    TrendDetailDialog(
        trendType = trendType,
        data = when (trendType) {
            TrendType.HEART_RATE -> heartRateData
            TrendType.SPO2 -> spO2Data
            TrendType.BLOOD_PRESSURE -> bloodPressureData
            TrendType.STEPS -> stepsData
            TrendType.BODY_TEMPERATURE -> bodyTempData // NEW
        }.sortedBy { it.timestamp },
        onDismiss = { showTrendDetail = null }
    )
}
```

---

## ✅ Summary

### **What's Working:**

- ✅ 4 trend cards displayed on dashboard
- ✅ Each card is clickable
- ✅ Mini charts show last 7 readings
- ✅ Detail dialog shows complete history
- ✅ Data sorted from oldest to latest
- ✅ Color-coded for easy identification
- ✅ Responsive and scrollable
- ✅ Empty states handled gracefully

### **Data Flow:**

```
Health Connect → HealthDataRepository → Firebase
                        ↓
                HealthDataViewModel
                        ↓
                  HomeScreen
                        ↓
              TrendCard (clickable)
                        ↓
              TrendDetailDialog
                        ↓
              TrendDataItem (each reading)
```

---

**Last Updated:** December 2024  
**Feature Status:** ✅ Complete and Ready to Use  
**Build Status:** ✅ BUILD SUCCESSFUL
