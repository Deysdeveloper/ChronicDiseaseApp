#!/bin/bash

echo "================================================"
echo "Reinstalling Chronic Disease App with HR Fix"
echo "================================================"

# Check if adb is available
if ! command -v adb &> /dev/null; then
    echo "❌ Error: adb not found in PATH"
    echo "Please add Android SDK platform-tools to your PATH"
    exit 1
fi

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ Error: No Android device detected"
    echo "Please connect your device and enable USB debugging"
    exit 1
fi

echo ""
echo "📱 Device detected"
echo ""

# Uninstall old version
echo "🗑️  Uninstalling old version..."
adb uninstall com.example.chronicdiseaseapp 2>/dev/null || echo "   (No previous installation found)"

echo ""
echo "📦 Installing new version with HR fix..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Installation successful!"
    echo ""
    echo "================================================"
    echo "Testing Instructions:"
    echo "================================================"
    echo "1. Open the app on your device"
    echo "2. In a new terminal, run:"
    echo "   adb logcat | grep -E 'First 5 HR values|HR calculation|Updated health metrics'"
    echo ""
    echo "3. Watch for logs like:"
    echo "   First 5 HR values: [82, 76, 91, 73, 85]"
    echo "   HR calculation: sum=1872, count=22, avg=85.0"
    echo "   ✅ Updated health metrics: HR=85, ..."
    echo ""
    echo "4. Tap the refresh button and verify HR changes"
    echo "================================================"
else
    echo ""
    echo "❌ Installation failed!"
    exit 1
fi
