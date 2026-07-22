#!/bin/bash

# Clean iOS build and Xcode cache

echo "🧹 Cleaning Xcode build cache..."
rm -rf ~/Library/Developer/Xcode/DerivedData/*

echo "🧹 Cleaning iOS project build files..."
rm -rf iosApp/iosApp.xcworkspace
rm -rf iosApp/Pods
rm -rf iosApp/Podfile.lock
rm -rf iosApp/.xcode.env.local
rm -rf composeApp/build

echo "✅ Xcode cache cleaned!"

