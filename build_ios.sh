#!/bin/bash

set -e

echo "🧹 Cleaning Gradle build..."
./gradlew clean

echo "🔨 Building iOS Simulator ARM64 Framework..."
./gradlew :composeApp:linkPodDebugFrameworkIosSimulatorArm64

echo "🔨 Building iOS ARM64 Framework..."
./gradlew :composeApp:linkPodDebugFrameworkIosArm64

echo "📦 Installing CocoaPods dependencies..."
cd iosApp
rm -rf Pods Podfile.lock
pod install
cd ..

echo "✅ iOS build completed successfully!"
echo ""
echo "Next steps:"
echo "1. Open iosApp.xcworkspace in Xcode"
echo "2. Select 'iosApp' scheme"
echo "3. Build for iOS Simulator or Device"

