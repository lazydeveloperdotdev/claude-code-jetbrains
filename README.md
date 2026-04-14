# Claude Code JetBrains Plugin

A JetBrains IDE plugin that provides multi-tab Claude Code terminal sessions directly in your IDE.

## Features

- **Multiple Claude Sessions** - Run multiple Claude Code instances in separate tabs
- **Full TUI Experience** - Complete Claude Code terminal UI with all features
- **Easy Tab Management** - Create new tabs with one click
- **Independent Contexts** - Each tab maintains its own conversation
- **Works with All IDEs** - IntelliJ IDEA, PyCharm, WebStorm, GoLand, etc.

## Prerequisites

- JetBrains IDE (IntelliJ IDEA, PyCharm, WebStorm, GoLand, etc.) version 2023.3+
- Java 17 or higher
- Claude Code CLI installed and available in your PATH

## Installation

1. **Install the plugin**:
   - Open your JetBrains IDE
   - Go to `Settings/Preferences` → `Plugins`
   - Click the gear icon → `Install Plugin from Disk...`
   - Select: `/Users/raj/claude-code-jetbrains-plugin/build/distributions/claude-code-jetbrains-plugin-1.0.0.zip`
   - Restart your IDE

## Usage

1. **Open the Claude Code tool window**:
   - View → Tool Windows → Claude Code
   - Or find it in the right sidebar

2. **The first tab opens automatically** with Claude running

3. **Create additional tabs**:
   - Click the "+" button in the tab bar (browser-style)
   - Each tab runs an independent Claude session

4. **Use Claude normally**:
   - Type directly in the terminal
   - Full Claude Code TUI and features
   - Each tab has its own conversation history

## Building from Source

### Requirements

- JDK 17+
- Gradle (included via wrapper)

### Build Commands

```bash
cd ~/claude-code-jetbrains-plugin

# Build the plugin
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
gradle buildPlugin

# Output: build/distributions/claude-code-jetbrains-plugin-1.0.0.zip
```

## Configuration

The plugin uses the `claude` CLI from your system PATH. Make sure:

1. Claude Code is installed
2. You're authenticated
3. The CLI is accessible in your PATH

## Troubleshooting

### "claude command not found"

Make sure Claude Code CLI is in your PATH:
```bash
which claude
```

### Plugin doesn't load

Check the IDE logs:
- Help → Show Log in Finder/Explorer
- Look for errors related to the Claude Code plugin

## License

MIT

## Support

For issues and questions:
- GitHub Issues: [Report an issue](https://github.com/anthropics/claude-code/issues)
- Documentation: [Claude Code Docs](https://docs.anthropic.com/claude-code)
