# Contributing to SimpleLoot

First off, thank you for considering contributing to SimpleLoot! It's people like you that make SimpleLoot such a great tool.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [How Can I Contribute?](#how-can-i-contribute)
- [Development Setup](#development-setup)
- [Coding Guidelines](#coding-guidelines)
- [Commit Messages](#commit-messages)
- [Pull Request Process](#pull-request-process)

## Code of Conduct

This project and everyone participating in it is governed by our Code of Conduct. By participating, you are expected to uphold this code. Please be respectful and constructive in all interactions.

## Getting Started

### Prerequisites

- Java 21 or higher
- Git
- An IDE (IntelliJ IDEA recommended, but VS Code works too)
- Basic understanding of Minecraft modding with Fabric

### Issues

- Check if the issue already exists before creating a new one
- Use the issue templates when available
- Provide as much detail as possible
- Include Minecraft version, mod version, and relevant logs

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check the existing issues. When creating a bug report, include:

- **Clear title** describing the issue
- **Steps to reproduce** the behavior
- **Expected behavior** vs **actual behavior**
- **Screenshots or videos** if applicable
- **Minecraft version** and **mod version**
- **Other mods installed** (to check for conflicts)
- **Crash logs** if applicable (from `logs/` or `crash-reports/`)

### Suggesting Features

Feature suggestions are welcome! Please:

- Check if the feature has already been suggested
- Provide a clear description of the feature
- Explain why it would be useful
- Consider how it fits with the mod's philosophy (lightweight, simple)

### Code Contributions

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Test thoroughly
5. Commit your changes (see [Commit Messages](#commit-messages))
6. Push to your branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

## Development Setup

### Clone and Build

```bash
# Clone your fork
git clone https://github.com/YOUR_USERNAME/SimpleLoot.git
cd SimpleLoot

# Build the project
./gradlew build

# Generate IDE run configurations (IntelliJ)
./gradlew genSources

# Run the Minecraft client with the mod
./gradlew runClient
```

### Project Structure

```
SimpleLoot/
├── src/main/java/com/simpleloot/
│   ├── SimpleLootClient.java      # Main mod entry point
│   ├── config/                     # Configuration classes
│   │   ├── SimpleLootConfig.java   # Config data and persistence
│   │   ├── ModConfigScreen.java    # Cloth Config screen
│   │   └── ModMenuIntegration.java # ModMenu integration
│   ├── loot/                       # Core functionality
│   │   ├── HoverLootHandler.java   # Main hover loot logic
│   │   └── HandledScreenAccessor.java
│   └── mixin/                      # Mixins for MC access
│       └── HandledScreenMixin.java
├── src/main/resources/
│   ├── fabric.mod.json             # Mod metadata
│   ├── simpleloot.mixins.json      # Mixin configuration
│   └── assets/simpleloot/
│       └── lang/en_us.json         # Translations
├── build.gradle                    # Build configuration
├── gradle.properties               # Version properties
└── README.md
```

### Testing

- Test all supported container types
- Test with various other mods installed
- Test keybind conflicts
- Test edge cases (full inventory, empty containers, etc.)
- Test multiplayer compatibility

## Coding Guidelines

### Style

- Use 4 spaces for indentation (not tabs)
- Follow standard Java naming conventions
- Add JavaDoc comments to public methods and classes
- Keep methods focused and reasonably sized
- Use meaningful variable and method names

### Best Practices

- Prefer composition over inheritance
- Handle edge cases gracefully
- Log important events at appropriate levels
- Don't break existing functionality
- Maintain backwards compatibility for configs

### Fabric/Minecraft Specific

- Use Yarn mappings for deobfuscated names
- Minimize mixin usage when Fabric API alternatives exist
- Register events properly to avoid memory leaks
- Be mindful of performance, especially in render callbacks
- Test on both client and server environments

## Commit Messages

Follow conventional commit format:

```
type(scope): description

[optional body]

[optional footer]
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

### Examples

```
feat(loot): add support for trapped chests

fix(config): prevent crash when config file is corrupted

docs(readme): update installation instructions
```

## Pull Request Process

1. **Ensure** your code follows the coding guidelines
2. **Update** documentation if needed
3. **Test** your changes thoroughly
4. **Fill out** the PR template completely
5. **Link** related issues
6. **Request** a review
7. **Address** any feedback
8. **Wait** for approval and merge

### PR Checklist

- [ ] Code compiles without errors
- [ ] No new warnings introduced
- [ ] Tested in-game
- [ ] Documentation updated (if applicable)
- [ ] Changelog updated (for features/fixes)
- [ ] Commit messages follow guidelines

## Questions?

Feel free to open an issue for questions about contributing. We're here to help!

---

Thank you for contributing to SimpleLoot! 🎮
