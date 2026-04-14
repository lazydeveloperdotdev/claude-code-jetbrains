#!/bin/bash

echo "Building Claude Code JetBrains Plugin..."

# Build the plugin
./gradlew buildPlugin

if [ $? -eq 0 ]; then
    echo ""
    echo "Build successful!"
    echo ""
    echo "Plugin location: build/distributions/claude-code-jetbrains-plugin-1.0.0.zip"
    echo ""
    echo "To install:"
    echo "1. Open your JetBrains IDE"
    echo "2. Go to Settings/Preferences → Plugins"
    echo "3. Click the gear icon → 'Install Plugin from Disk...'"
    echo "4. Select: $(pwd)/build/distributions/claude-code-jetbrains-plugin-1.0.0.zip"
    echo ""
else
    echo "Build failed. Please check the error messages above."
    exit 1
fi
