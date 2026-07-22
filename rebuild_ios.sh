#!/bin/bash

# Complete iOS rebuild script
# Usage: chmod +x rebuild_ios.sh && ./rebuild_ios.sh

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🚀 Complete iOS Project Rebuild"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Step 1: Clean Gradle
echo "📋 Step 1: Cleaning Gradle build..."
cd "$PROJECT_ROOT"
./gradlew clean --no-build-cache

# Step 2: Build iOS frameworks
echo ""
echo "📋 Step 2: Building iOS frameworks..."

echo "  → Building iOS Simulator ARM64 framework..."
./gradlew :composeApp:linkPodDebugFrameworkIosSimulatorArm64 -x test

echo "  → Building iOS ARM64 framework (device)..."
./gradlew :composeApp:linkPodDebugFrameworkIosArm64 -x test

# Step 3: Clean and reinstall CocoaPods
echo ""
echo "📋 Step 3: Reinstalling CocoaPods..."
cd "$PROJECT_ROOT/iosApp"

if [ -d "Pods" ]; then
    echo "  → Removing old Pods directory..."
    rm -rf Pods
fi

if [ -f "Podfile.lock" ]; then
    echo "  → Removing old Podfile.lock..."
    rm Podfile.lock
fi

echo "  → Installing CocoaPods dependencies..."
pod install

cd "$PROJECT_ROOT"

# Step 4: Summary
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ iOS Rebuild Completed Successfully!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📝 Next Steps:"
echo "  1. Open iosApp/iosApp.xcworkspace in Xcode"
echo "  2. Select 'iosApp' scheme"
echo "  3. Select target device (Simulator or Physical Device)"
echo "  4. Press Cmd+B to build or Cmd+R to run"
echo ""
echo "⚠️  IMPORTANT:"
echo "  • Always open .xcworkspace, NOT .xcodeproj"
echo "  • If build fails, try:"
echo "    - Product → Clean Build Folder (Cmd+Shift+K)"
echo "    - Delete DerivedData: rm -rf ~/Library/Developer/Xcode/DerivedData/*"
echo ""

