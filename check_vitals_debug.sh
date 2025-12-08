#!/bin/bash

# 🔍 Check Vitals Debug Script
# This script helps monitor vitals-related logs in real-time

echo "🔍 Starting Vitals Debug Monitor..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📱 Instructions:"
echo "1. Make sure your device/emulator is connected"
echo "2. Login as doctor 'amandeep' in the app"
echo "3. Navigate to 'View All Patients Vitals'"
echo "4. Watch the logs below..."
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Clear previous logs
adb logcat -c 2>/dev/null

# Check if adb is available
if ! command -v adb &> /dev/null; then
    echo "❌ Error: adb command not found"
    echo "   Please ensure Android SDK platform-tools is in your PATH"
    exit 1
fi

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ Error: No device connected"
    echo "   Please connect your Android device or start an emulator"
    exit 1
fi

echo "✅ Device connected! Monitoring logs..."
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 VITALS DEBUG LOGS:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Monitor logs with color coding
adb logcat | grep --line-buffered -E "(PatientVitalsRepository|DoctorConnectionRepository|DEBUG|ERROR)" | while IFS= read -r line; do
    # Color code based on content
    if echo "$line" | grep -q "ERROR\|❌"; then
        echo -e "${RED}$line${NC}"
    elif echo "$line" | grep -q "DEBUG.*doctor UID\|Getting connected patients"; then
        echo -e "${CYAN}$line${NC}"
    elif echo "$line" | grep -q "Found.*connected patients\|✅"; then
        echo -e "${GREEN}$line${NC}"
    elif echo "$line" | grep -q "No connected patients\|⚠️"; then
        echo -e "${YELLOW}$line${NC}"
    elif echo "$line" | grep -q "Fetching vitals\|📊"; then
        echo -e "${PURPLE}$line${NC}"
    elif echo "$line" | grep -q "Heart Rate\|Blood Pressure\|SpO2\|Steps\|🔍"; then
        echo -e "${BLUE}$line${NC}"
    else
        echo "$line"
    fi
done
