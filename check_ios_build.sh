#!/bin/bash

# iOS Build Status Checker
# Проверяет, что все необходимое установлено и настроено

echo "🔍 iOS Build Status Check"
echo "========================="
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✅${NC} $2"
        return 0
    else
        echo -e "${RED}❌${NC} $2"
        return 1
    fi
}

check_dir() {
    if [ -d "$1" ]; then
        echo -e "${GREEN}✅${NC} $2"
        return 0
    else
        echo -e "${RED}❌${NC} $2"
        return 1
    fi
}

check_command() {
    if command -v "$1" &> /dev/null; then
        echo -e "${GREEN}✅${NC} $2 installed"
        return 0
    else
        echo -e "${RED}❌${NC} $2 not found"
        return 1
    fi
}

echo "📋 Checking dependencies..."
check_command "java" "Java"
check_command "gradle" "Gradle"
check_command "pod" "CocoaPods"
check_command "xcodebuild" "Xcode"
echo ""

echo "📁 Checking project structure..."
check_file "composeApp/build.gradle.kts" "composeApp/build.gradle.kts"
check_file "iosApp/Podfile" "iosApp/Podfile"
check_dir "composeApp/src/commonMain" "commonMain source"
check_dir "composeApp/src/androidMain" "androidMain source"
check_dir "composeApp/src/iosMain" "iosMain source"
echo ""

echo "🔨 Checking build artifacts..."
check_dir "composeApp/build/cocoapods" "Cocoapods framework"
check_dir "iosApp/Pods" "CocoaPods dependencies"
check_dir "iosApp/iosApp.xcworkspace" "Xcode workspace"
echo ""

echo "📦 Checking critical files..."
check_file "gradle/libs.versions.toml" "Dependency versions"
check_file "iosApp/iosApp.xcodeproj/project.pbxproj" "Xcode project file"
check_file "iosApp/Podfile.lock" "Pod dependencies lock"
echo ""

echo "✨ Status Summary:"
echo "==================="
echo ""
echo "To build iOS project:"
echo "  1. Run: ./rebuild_ios.sh"
echo "  2. Or: open iosApp/iosApp.xcworkspace"
echo "  3. Select 'iosApp' scheme"
echo "  4. Press Cmd+B to build"
echo ""
echo "For issues, see:"
echo "  - FINAL_INSTRUCTIONS.md"
echo "  - SOLUTION_SUMMARY.md"
echo "  - QUICK_REFERENCE.md"
echo ""

