# SimpleLoot

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.9--1.21.11-green)](https://minecraft.net)
[![Fabric](https://img.shields.io/badge/Mod%20Loader-Fabric-blue)](https://fabricmc.net)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)
[![Version](https://img.shields.io/badge/Version-1.3.0-orange)](https://modrinth.com/mod/simpleloot)

A lightweight Fabric mod for Minecraft that adds **Rust-style Hover Loot** - quickly transfer items by holding a key and swiping your mouse over slots.

**Author:** Partacus-SPQR  
**Source:** [GitHub](https://github.com/Partacus-SPQR/SimpleLoot)  
**Download:** [Modrinth](https://modrinth.com/mod/simpleloot)

## Features

- **Hover Loot** - Hold a key and swipe over items to transfer them instantly
- **Hover Drop** - Hold Ctrl + hover loot key (or dedicated drop key) to drop items on the ground
- **Armor Equip** - Hover over armor in your inventory to instantly equip it, or hover armor slots to unequip
- **Crafting Grid** - Hover over items in your inventory to send them to the 2x2 or 3x3 crafting grid
- **Creative Support** - Hover drop works in creative mode's survival inventory tab
- **Bidirectional** - Works both ways: container to inventory and inventory to container
- **Hotbar Protection** - Optionally prevent hotbar items from being transferred
- **Container Filters** - Enable/disable specific container types
- **No Default Keybinds** - You choose your preferred keys
- **Fallback Config** - Full-featured config screen works without Cloth Config (sliders, tooltips, reset buttons, scrollable)

## Supported Screens

**Containers:** Chests, Double Chests, Barrels, Shulker Boxes, Ender Chests, Dispensers, Droppers, Hoppers

**Inventory:** Player inventory slots, Crafting Table (3x3), Survival inventory crafting (2x2)

## Usage

1. Open **Options > Controls > Key Binds > SimpleLoot**
2. Bind a key to **Hover Loot (Hold)**
3. Open any container, hold your key, and swipe over items

### Drop Mode
- Hold **Ctrl + Hover Loot key** to drop items instead of transferring
- Or bind a dedicated **Hover Drop** key

### Armor Equip
- In your inventory, hover over armor pieces to equip them
- Hover over equipped armor slots to unequip
- Hover armor in inventory while already wearing same type to instantly swap

### Crafting Grid
- In your inventory or crafting table, hover over items to send a full stack to the crafting grid
- Hover over crafting grid slots to move items back to inventory
- Hover over the crafting output to craft the maximum amount possible

## Configuration

Access via **ModMenu** or edit `config/simpleloot.json`

| Setting | Description | Default |
|---------|-------------|---------|
| Enabled | Enable/disable the mod | `true` |
| Hotbar Protection | Protect hotbar slots | `false` |
| Transfer Delay | Delay between transfers (ms) | `20` |
| Debug Mode | Enable debug logging | `false` |
| Allow Hover Drop | Enable drop mode (Ctrl+hover) | `true` |
| Allow Crafting Grid | Enable crafting grid transfers | `true` |
| Allow Armor Equip | Enable armor equip via hover | `true` |
| Armor Swap Delay | Delay between armor swaps (ms) | `70` |
| Container Types | Per-container enable/disable | All enabled |

## Keybindings

All keybindings support both keyboard keys and mouse buttons.

| Keybind | Description | Default |
|---------|-------------|---------|
| Hover Loot (Hold) | Hold to transfer items you hover over | Unbound |
| Hover Drop (Hold) | Hold to drop items you hover over | Unbound |
| Enable/Disable | Toggle the mod on/off | Unbound |
| Open Config | Open config screen | Unbound |

## Requirements

- Minecraft 1.21.9, 1.21.10, or 1.21.11
- Fabric Loader 0.16.0+
- Fabric API

### Optional (Recommended)

- [Cloth Config](https://modrinth.com/mod/cloth-config) - Enhanced config screen
- [ModMenu](https://modrinth.com/mod/modmenu) - In-game mod configuration access

> **Note:** SimpleLoot includes a full-featured fallback config screen with sliders, tooltips, reset buttons, and scrolling. This screen is used when Cloth Config is unavailable or incompatible with your Minecraft version.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/)
2. Install [Fabric API](https://modrinth.com/mod/fabric-api)
3. (Optional) Install [Cloth Config](https://modrinth.com/mod/cloth-config) and [ModMenu](https://modrinth.com/mod/modmenu)
4. Drop SimpleLoot into your `mods` folder

## Building

```bash
git clone https://github.com/Partacus-SPQR/SimpleLoot.git
cd SimpleLoot
./gradlew build
```

## License

MIT License - see [LICENSE](LICENSE)

## Author

Partacus-SPQR

## Author's Note

Built this because I wanted my loot to transfer just like Rust. Simple and efficient - no more shift clicking!
