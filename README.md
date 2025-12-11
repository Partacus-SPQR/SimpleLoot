# SimpleLoot

A lightweight Fabric mod for Minecraft 1.21.11 that adds **Rust-style Hover Loot** - quickly transfer items by holding a key and swiping your mouse over slots.

## Features

- **Hover Loot** - Hold a key and swipe over items to transfer them instantly
- **Bidirectional** - Works both ways: container to inventory and inventory to container
- **Hotbar Protection** - Optionally prevent hotbar items from being transferred
- **Container Filters** - Enable/disable specific container types
- **No Default Keybinds** - You choose your preferred keys
- **Fallback Config** - Full-featured config screen works without Cloth Config (sliders, tooltips, reset buttons, scrollable)

## Supported Containers

Chests, Double Chests, Barrels, Shulker Boxes, Ender Chests, Dispensers, Droppers, Hoppers

## Usage

1. Open **Options > Controls > Key Binds > SimpleLoot**
2. Bind a key to **Hover Loot (Hold)**
3. Open any container, hold your key, and swipe over items

## Configuration

Access via **ModMenu** or edit `config/simpleloot.json`

| Setting | Description | Default |
|---------|-------------|---------|
| Enabled | Enable/disable the mod | `true` |
| Hotbar Protection | Protect hotbar slots | `false` |
| Transfer Delay | Delay between transfers (ms) | `0` |
| Debug Mode | Enable debug logging | `false` |
| Container Types | Per-container enable/disable | All enabled |

## Keybindings

All keybindings support both keyboard keys and mouse buttons.

| Keybind | Description | Default |
|---------|-------------|---------|
| Hover Loot (Hold) | Hold to transfer items you hover over | Unbound |
| Enable/Disable | Toggle the mod on/off | Unbound |
| Open Config | Open config screen | Unbound |

## Requirements

- Minecraft 1.21.11
- Fabric Loader 0.18.2+
- Fabric API 0.139.4+

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
