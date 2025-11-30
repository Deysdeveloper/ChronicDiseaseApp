#!/bin/bash

# Health Connect Data Fetch Monitor
# This script monitors logcat to check if the app is fetching data from Health Connect

echo "=============================================="
echo "Health Connect Data Fetch Monitor"
echo "=============================================="
echo ""
echo "This script will:"
echo "1. Clear existing logs"
echo "2. Start your app"
echo "3. Monitor data fetching in real-time"
echo "4. Show summary of results"
echo ""
echo "Press Ctrl+C to stop monitoring"
echo "=============================================="
echo ""

# Set ADB path (modify if needed)
ADB="/Users/debojyotidey/Library/Android/sdk/platform-tools/adb"

# Check if device is connected
echo "Checking for connected devices..."
DEVICE_COUNT=$($ADB devices | grep -v "List" | grep "device" | wc -l)

if [ $DEVICE_COUNT -eq 0 ]; then
    echo "❌ No devices connected!"
    echo "Please connect your device via USB and enable USB debugging"
    exit 1
fi

echo "✅ Device connected"
echo ""

# Clear logcat
echo "Clearing old logs..."
$ADB logcat -c
echo "✅ Logs cleared"
echo ""

# Start the app
echo "Starting Chronic Disease App..."
$ADB shell am start -n com.example.chronicdiseaseapp/.MainActivity
echo "✅ App started"
echo ""

# Wait for app to initialize
echo "Waiting for app to initialize (3 seconds)..."
sleep 3
echo ""

echo "=============================================="
echo "MONITORING HEALTH DATA FETCH - LIVE LOGS"
echo "=============================================="
echo ""

# Start monitoring with timeout
timeout 30s $ADB logcat -v time | grep --line-buffered -E "(HealthDataRepository|HealthDataViewModel)" &
LOGCAT_PID=$!

# Wait for logcat to capture data
sleep 30

# Kill logcat if still running
kill $LOGCAT_PID 2>/dev/null

echo ""
echo "=============================================="
echo "FETCH SUMMARY REPORT"
echo "=============================================="
echo ""

# Capture recent logs for analysis
LOGS=$($ADB logcat -d | grep -E "(HealthDataRepository|HealthDataViewModel)")

# Check Health Connect initialization
if echo "$LOGS" | grep -q "Health Connect initialized successfully"; then
    echo "✅ Health Connect initialized successfully"
else
    echo "❌ Health Connect failed to initialize"
fi

echo ""
echo "--- Permission Status ---"
# Check permission status
if echo "$LOGS" | grep -q "Required permissions granted: true"; then
    echo "✅ All required permissions granted"
    PERMS_GRANTED=true
elif echo "$LOGS" | grep -q "Required permissions granted: false"; then
    echo "❌ Required permissions NOT granted"
    PERMS_GRANTED=false
else
    echo "⚠️  Permission status unknown"
    PERMS_GRANTED=false
fi

echo ""
echo "--- Data Fetch Results ---"

# Check Blood Pressure data
echo ""
echo "1. Blood Pressure Data:"
if echo "$LOGS" | grep "getBloodPressureData" | grep -q "Received [1-9]"; then
    BP_COUNT=$(echo "$LOGS" | grep "getBloodPressureData: Received" | tail -1 | grep -oE "[0-9]+" | head -1)
    echo "   ✅ SUCCESS - Received $BP_COUNT blood pressure records"
    echo "   📊 Real data from Health Connect"
elif echo "$LOGS" | grep "getBloodPressureData" | grep -q "Received 0"; then
    echo "   ❌ NO DATA - Received 0 records from Health Connect"
    echo "   ⚠️  Using sample data instead"
    echo "   💡 Action: Sync Samsung Health to Health Connect"
else
    echo "   ⚠️  Status unknown - check logs below"
fi

# Check Heart Rate data
echo ""
echo "2. Heart Rate Data:"
if echo "$LOGS" | grep "getHeartRateData" | grep -q "Successfully retrieved [1-9]"; then
    HR_COUNT=$(echo "$LOGS" | grep "getHeartRateData: Successfully retrieved" | tail -1 | grep -oE "[0-9]+" | head -1)
    echo "   ✅ SUCCESS - Retrieved $HR_COUNT heart rate readings"
elif echo "$LOGS" | grep "getHeartRateData" | grep -q "No heart rate records found"; then
    echo "   ❌ NO DATA - No heart rate records in Health Connect"
    echo "   ⚠️  Using sample data"
else
    echo "   ⚠️  Status unknown"
fi

# Check SpO2 data
echo ""
echo "3. SpO2 Data:"
if echo "$LOGS" | grep "getSpO2Data" | grep -q "Successfully retrieved [1-9]"; then
    SPO2_COUNT=$(echo "$LOGS" | grep "getSpO2Data: Successfully retrieved" | tail -1 | grep -oE "[0-9]+" | head -1)
    echo "   ✅ SUCCESS - Retrieved $SPO2_COUNT SpO2 readings"
elif echo "$LOGS" | grep "getSpO2Data" | grep -q "No SpO2 records found"; then
    echo "   ❌ NO DATA - No SpO2 records in Health Connect"
    echo "   ⚠️  Using sample data"
else
    echo "   ⚠️  Status unknown"
fi

# Check Steps data
echo ""
echo "4. Steps Data:"
if echo "$LOGS" | grep "getStepsData" | grep -q "Successfully retrieved"; then
    echo "   ✅ SUCCESS - Retrieved steps data"
elif echo "$LOGS" | grep "getStepsData" | grep -q "No steps data found"; then
    echo "   ❌ NO DATA - No steps data in Health Connect"
    echo "   ⚠️  Using sample data"
else
    echo "   ⚠️  Status unknown"
fi

echo ""
echo "=============================================="
echo "DIAGNOSIS & RECOMMENDATIONS"
echo "=============================================="
echo ""

# Determine overall status and provide recommendations
if [ "$PERMS_GRANTED" = false ]; then
    echo "🔴 PROBLEM: Permissions Not Granted"
    echo ""
    echo "Action Required:"
    echo "1. Open Health Connect app"
    echo "2. Go to 'Apps and devices'"
    echo "3. Find 'Chronic Disease App'"
    echo "4. Grant all required permissions:"
    echo "   - Blood Pressure"
    echo "   - Heart Rate"
    echo "   - Oxygen Saturation"
    echo "   - Steps"
    echo "5. Restart this script to check again"
elif echo "$LOGS" | grep -q "Received 0 records"; then
    echo "🟡 PROBLEM: No Data in Health Connect"
    echo ""
    echo "Permissions are OK, but Health Connect has no data."
    echo ""
    echo "Action Required - Sync Samsung Health to Health Connect:"
    echo "1. Open Samsung Health app"
    echo "2. Go to Settings → Connected Services → Health Connect"
    echo "3. Enable these data types:"
    echo "   ✓ Blood Pressure"
    echo "   ✓ Heart Rate"
    echo "   ✓ Oxygen Saturation (SpO2)"
    echo "   ✓ Steps"
    echo "4. Tap 'Sync now' button"
    echo "5. Wait 1-2 minutes for sync to complete"
    echo "6. Verify data in Health Connect app:"
    echo "   - Open Health Connect"
    echo "   - Browse Data → Vitals → Blood Pressure"
    echo "   - Check if your readings appear"
    echo "7. Refresh your Chronic Disease App"
    echo ""
    echo "📖 See: SYNC_SAMSUNG_HEALTH_TO_HEALTH_CONNECT.md"
elif echo "$LOGS" | grep -q "Successfully retrieved [1-9]"; then
    echo "🟢 SUCCESS: Data is Being Fetched!"
    echo ""
    echo "✅ App is successfully reading data from Health Connect"
    echo "✅ Your real health data is being displayed"
    echo "✅ No action required"
else
    echo "🟡 UNCLEAR: Check Detailed Logs"
    echo ""
    echo "The status is unclear. Check the detailed logs below."
fi

echo ""
echo "=============================================="
echo "DETAILED LOGS (Last 50 lines)"
echo "=============================================="
echo ""
echo "$LOGS" | tail -50

echo ""
echo "=============================================="
echo "Full logs saved to: health_data_logs.txt"
echo "=============================================="

# Save full logs to file
echo "$LOGS" > health_data_logs.txt

echo ""
echo "To view full logs: cat health_data_logs.txt"
echo "To monitor live: $ADB logcat | grep -E '(HealthDataRepository|HealthDataViewModel)'"
echo ""
