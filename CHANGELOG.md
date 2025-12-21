# Changelog

All notable changes to SimpleLoot will be documented in this file.

## [1.3.1] - 2025-12-21

### Added
- **Item validation for Enchanting Tables**: Only enchantable items (tools, weapons, armor, books) and lapis lazuli can be transferred to the enchanting table
- **Item validation for Beacons**: Only valid payment items (iron/gold/emerald/diamond/netherite ingots) can be transferred to beacon slots
- Smart slot detection for processing and workstation screens

### Changed
- Furnace slot handling now relies on Minecraft's built-in quick-move logic for proper item placement (smeltable items to input, fuel to fuel slot)

---

## [1.3.0] - 2025-12-16

### Added
- **Hover Drop feature**: Hold Ctrl + hover loot key (or dedicated hover drop key) to drop items on the ground while hovering
- **Dedicated hover drop keybind**: Can be bound separately from the main hover loot key
- **Crafting Grid support**: Hover over inventory items while in player inventory to send entire stacks to the 2x2 crafting grid
- **Crafting Table support**: Works with 3x3 crafting grid in crafting tables
- **Bidirectional crafting transfers**: Hover over crafting grid slots to move items back to inventory
- **Crafting output collection**: Hover over the crafting output slot to take the crafted result
- **Armor Equip feature**: Hover over armor pieces in inventory to equip them, or hover over worn armor slots to unequip
- **Processing block support**: Furnaces, Blast Furnaces, Smokers, Brewing Stands
- **Workstation support**: Anvils, Smithing Tables, Grindstones, Stonecutters, Looms, Enchanting Tables, Beacons, Crafters, Cartography Tables
- New config option `allowHoverDrop` to enable/disable drop mode
- New config option `allowCraftingGrid` to enable/disable crafting grid transfers
- New config option `allowArmorEquip` to enable/disable armor equip via hover loot
- New config option `armorSwapDelayMs` to control delay between armor swaps (default: 70ms)
- Per-block-type enable/disable options for all new supported screens
- Creative inventory support for hover drop (survival tab)
- **Multi-version support**: Now supports Minecraft 1.21.9, 1.21.10, and 1.21.11
- **Stonecutter build system**: Single codebase builds for multiple Minecraft versions

### Changed
- Transfer delay default changed from 0ms to 20ms for stability
- Fabric Loader minimum requirement lowered to 0.16.0 for better compatibility
- Config tooltips now show default values

### Fixed
- Items now transfer as full stacks to crafting grid (was incorrectly splitting stacks)
- Fixed incorrect default values in Cloth Config screen (hotbarProtection, armorSwapDelayMs)

---

## [1.2.1] - 2025-12-11

### Added
- Mouse button support for all keybindings (can now bind to mouse buttons in addition to keyboard keys)

### Fixed
- Cloth Config screen now displays proper labels instead of raw translation keys

---

## [1.2.0] - 2025-12-11

### Added
- Sliders for numeric config values in fallback config screen
- Tooltips for all config options explaining their purpose
- Reset buttons for every config option to restore defaults
- Scrollable config screen with interactive scrollbar (click and drag)
- Scroll indicators showing when more content is available
- Footer buttons: Save & Close, Key Binds, Cancel

### Changed
- Fallback config screen now fully matches Cloth Config UX quality
- Footer buttons are now always visible (fixed at bottom of screen)
- Improved layout with proper spacing and centered widgets

### Fixed
- Footer buttons no longer clipped when scrolling
- Tooltips render correctly above scroll area

---

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
