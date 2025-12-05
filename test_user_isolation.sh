#!/bin/bash

# Test script to verify user data isolation
# Run this after installing the app to check logs

echo "========================================="
echo "User Data Isolation Test"
echo "========================================="
echo ""
echo "Instructions:"
echo "1. Sign up with a new user (e.g., Johnny)"
echo "2. Watch the logs below"
echo "3. Verify you see 'USER CHANGE DETECTED!'"
echo "4. Verify ViewModel instances have new hashCodes"
echo "5. Verify dashboard shows no previous user data"
echo ""
echo "========================================="
echo "Starting logcat filter..."
echo "========================================="
echo ""

# Check if adb is available
if ! command -v adb &> /dev/null; then
    echo "❌ adb not found!"
    echo "Please install Android SDK platform-tools:"
    echo "  brew install --cask android-platform-tools"
    echo ""
    echo "Or add to PATH:"
    echo "  export PATH=\$PATH:~/Library/Android/sdk/platform-tools"
    exit 1
fi

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected!"
    echo "Please connect your device and enable USB debugging"
    exit 1
fi

echo "✅ Device connected"
echo ""

# Clear logcat
adb logcat -c

echo "📱 Watching for user isolation events..."
echo "   (Press Ctrl+C to stop)"
echo ""

# Filter logs for user isolation events
adb logcat | grep -E "HomeScreen|USER CHANGE|ViewModel instance|FirebaseAuth|VitalsRepository|getCurrentUserId" | while read -r line; do
    # Highlight important lines
    if echo "$line" | grep -q "USER CHANGE DETECTED"; then
        echo "🔄 $line"
    elif echo "$line" | grep -q "ViewModel instance"; then
        echo "🏗️  $line"
    elif echo "$line" | grep -q "Previous user:"; then
        echo "👤 $line"
    elif echo "$line" | grep -q "New user:"; then
        echo "👤 $line"
    elif echo "$line" | grep -q "Clearing all"; then
        echo "🧹 $line"
    elif echo "$line" | grep -q "Loading health data from Firebase"; then
        echo "📥 $line"
    else
        echo "$line"
    fi
done
