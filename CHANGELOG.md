# Changelog

All notable changes to SimpleLoot will be documented in this file.

## [1.1.1] - 2025-12-10

### Changed
- Simplified Cloth Config compatibility check for better forward compatibility
- Removed hardcoded API checks in favor of runtime fallback detection
- When official Cloth Config updates for 1.21.11, it will work automatically

---

## [1.1.0] - 2025-12-09

### Added
- Built-in fallback config screen for when Cloth Config is unavailable or incompatible
- Automatic Cloth Config compatibility detection
- Keybinds shortcut button in config screen

### Changed
- Updated to Minecraft 1.21.11
- Cloth Config is now optional (recommended but not required)
- ModMenu is now optional (recommended but not required)
- Default hotbar protection changed to `false`
- Transfer delay controls now include --, -, +, ++ buttons for finer adjustment

### Technical
- Minecraft 1.21.11
- Fabric Loader 0.18.2+
- Fabric API 0.139.4+1.21.11
- Yarn Mappings 1.21.11+build.1
- Cloth Config 20.0.149 (optional)
- ModMenu 17.0.0 (optional)
- Java 21

---

## [1.0.0] - 2025-12-07

### Added
- Hover Loot functionality — hold a key and swipe to transfer items
- Bidirectional transfers (container ↔ inventory)
- Hotbar protection option
- Queue-based transfer system with mouse path interpolation
- Support for all vanilla storage containers:
  - Chests (single and double)
  - Barrels
  - Shulker Boxes
  - Ender Chests
  - Dispensers
  - Droppers
  - Hoppers
- Per-container-type enable/disable options
- Configurable transfer delay
- Configuration screen via Cloth Config
- ModMenu integration
- All keybindings unbound by default

### Technical
- Minecraft 1.21.10
- Fabric Loader 0.16.9+
- Fabric API 0.138.3+
- Cloth Config 20.0.149
- Java 21
