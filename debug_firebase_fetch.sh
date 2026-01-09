#!/bin/bash

# Firebase Fetch Debug Script
# This script helps diagnose why old data isn't fetching from Firebase RTDB

echo "======================================"
echo "Firebase RTDB Fetch Debug Script"
echo "======================================"
echo ""

# Color codes for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if adb is available
if ! command -v adb &> /dev/null; then
    echo -e "${RED}❌ adb not found. Please make sure Android SDK is installed and adb is in your PATH.${NC}"
    echo ""
    echo "On macOS, install Android Studio or add to PATH:"
    echo "export PATH=\$PATH:\$HOME/Library/Android/sdk/platform-tools"
    echo ""
    exit 1
fi

echo -e "${GREEN}✅ adb found${NC}"
echo ""

# Prompt for information
echo "Please provide the following information:"
echo ""
read -p "1. Your Firebase Auth UID (if known, or press Enter to skip): " USER_UID
read -p "2. What type of user? (patient/doctor): " USER_TYPE
echo ""

echo "======================================"
echo "Collecting Diagnostic Information..."
echo "======================================"
echo ""

# Create output directory
mkdir -p firebase_debug_logs
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="firebase_debug_logs/debug_${TIMESTAMP}.txt"

# Function to extract and display key logs
extract_logs() {
    local pattern=$1
    local description=$2

    echo -e "\n${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${YELLOW}$description${NC}"
    echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

    adb logcat -d | grep -iE "$pattern" | head -30
}

echo "Collecting logs (this may take a moment)..."

# 1. Authentication status
extract_logs "FirebaseAuth.*User|auth.*uid|sign.*in|sign.*out" "1. Authentication Status" | tee -a "$LOG_FILE"

# 2. Firebase RTDB read operations
extract_logs "Fetched.*readings from Firebase|Fetching heart rate|Fetching blood pressure|Fetching SpO2|Fetching steps" "2. Firebase RTDB Read Operations" | tee -a "$LOG_FILE"

# 3. Permission errors
extract_logs "permission.*denied|PermissionDenied|access.*denied" "3. Permission Errors" | tee -a "$LOG_FILE"

# 4. VitalsRepository errors
extract_logs "VitalsRepository.*Error|VitalsRepository.*❌|VitalsRepository.*failed" "4. VitalsRepository Errors" | tee -a "$LOG_FILE"

# 5. HealthDataViewModel logs
extract_logs "HealthDataViewModel.*Loaded|HealthDataViewModel.*Error|HealthDataViewModel.*❌" "5. HealthDataViewModel Logs" | tee -a "$LOG_FILE"

# 6. Database error codes
extract_logs "DatabaseError.*code|DatabaseError.*message" "6. Firebase Database Error Codes" | tee -a "$LOG_FILE"

# 7. Snapshot information
extract_logs "Snapshot exists|Snapshot children|Total readings fetched" "7. Firebase Snapshot Information" | tee -a "$LOG_FILE"

echo ""
echo -e "${GREEN}======================================${NC}"
echo -e "${GREEN}✅ Log Collection Complete!${NC}"
echo -e "${GREEN}======================================${NC}"
echo ""

# Save user info to log file
echo "=======================================" >> "$LOG_FILE"
echo "User Information" >> "$LOG_FILE"
echo "=======================================" >> "$LOG_FILE"
echo "User Type: $USER_TYPE" >> "$LOG_FILE"
echo "User UID: $USER_UID" >> "$LOG_FILE"
echo "Timestamp: $(date)" >> "$LOG_FILE"
echo "" >> "$LOG_FILE"

# Analysis
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}PRELIMINARY ANALYSIS${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Check for permission denied
if adb logcat -d | grep -iq "permission.*denied\|PermissionDenied"; then
    echo -e "${RED}❌ FOUND: Permission Denied Errors${NC}"
    echo "   → Firebase security rules are blocking read access"
    echo "   → Action: Update Firebase security rules"
    echo ""
fi

# Check for authentication issues
if adb logcat -d | grep -iq "user not logged in\|Cannot fetch.*- user\|unauthenticated"; then
    echo -e "${RED}❌ FOUND: User Not Authenticated${NC}"
    echo "   → User is not logged in or session expired"
    echo "   → Action: Check authentication flow"
    echo ""
fi

# Check for successful fetches
if adb logcat -d | grep -iq "Fetched.*readings from Firebase"; then
    echo -e "${GREEN}✅ FOUND: Successful Firebase Fetches${NC}"
    # Count how many
    FETCHES=$(adb logcat -d | grep -c "Fetched.*readings from Firebase")
    echo "   → Total successful fetches: $FETCHES"
    echo ""
fi

# Check for empty data
if adb logcat -d | grep -iq "Total readings fetched: 0\|No readings found"; then
    echo -e "${YELLOW}⚠️  FOUND: Empty Data Results${NC}"
    echo "   → Data query succeeded but returned 0 records"
    echo "   → Possible causes:"
    echo "     - Query limit (limitToLast) excluding old data"
    echo "     - Data path mismatch"
    echo "     - Data doesn't exist in Firebase"
    echo "     - Timestamp filter excluding old records"
    echo ""
fi

# Check for snapshot existence
if adb logcat -d | grep -iq "Snapshot does not exist\|Snapshot exists: false"; then
    echo -e "${RED}❌ FOUND: Data Path Not Found${NC}"
    echo "   → Querying a path that doesn't exist in Firebase"
    echo "   → Action: Verify user UID and data path in Firebase Console"
    echo ""
elif adb logcat -d | grep -iq "Snapshot exists: true"; then
    echo -e "${GREEN}✅ FOUND: Data Path Exists${NC}"
    # Get children count
    CHILDREN=$(adb logcat -d | grep "Snapshot children" | tail -1 | grep -oE "[0-9]+")
    echo "   → Records found: $CHILDREN"
    echo ""
fi

# Check for missing fields
if adb logcat -d | grep -iq "Missing.*field"; then
    echo -e "${YELLOW}⚠️  FOUND: Missing Data Fields${NC}"
    echo "   → Old data might be missing 'source' or 'id' fields"
    echo "   → Action: Validate old data structure in Firebase Console"
    echo ""
fi

# Recommendations
echo "======================================"
echo "RECOMMENDATIONS"
echo "======================================"
echo ""
echo "1. Verify Data in Firebase Console:"
echo "   https://console.firebase.google.com/project/chronicdiseaseapp/database/data"
echo "   → Navigate to: users/${USER_UID}/vitals/"
echo ""
echo "2. Test Security Rules:"
echo "   https://console.firebase.google.com/project/chronicdiseaseapp/database/rules"
echo "   �� Click 'Rules Simulator'"
echo "   → Test read operation on your data"
echo ""
echo "3. Update Security Rules (if needed):"
echo "   → Use FIREBASE_SECURITY_RULES_FINAL.json"
echo "   → Publish updated rules"
echo ""
echo "4. Clear App Cache and Retry:"
echo "   → Uninstall app (or clear data)"
echo "   → Reinstall and re-authenticate"
echo "   → Try fetching data again"
echo ""
echo "5. Add Debug Logging:"
echo "   → Review COMPREHENSIVE_DEBUG_GUIDE.md"
echo "   → Add comprehensive logging to VitalsRepository.kt"
echo "   → Reproduce the issue and collect logs"
echo ""
echo "======================================"
echo "Next Steps"
echo "======================================"
echo ""
echo "Full logs saved to: $LOG_FILE"
echo ""
echo "Please share:"
echo "1. This log file"
echo "2. Firebase Console screenshot of data"
echo "3. Rules Simulator test result"
echo "4. Your answers to questions in COMPREHENSIVE_DEBUG_GUIDE.md"
echo ""
echo -e "${GREEN}Good luck debugging! 🚀${NC}"