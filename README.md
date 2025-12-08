# SimpleLoot

A lightweight Fabric mod for Minecraft 1.21.10 that adds **Rust-style Hover Loot** — quickly transfer items by holding a key and swiping your mouse over slots.

## Features

- **Hover Loot** — Hold a key and swipe over items to transfer them instantly
- **Bidirectional** — Works both ways: container → inventory and inventory → container
- **Hotbar Protection** — Optionally prevent hotbar items from being transferred
- **Container Filters** — Enable/disable specific container types
- **No Default Keybinds** — You choose your preferred keys

## Supported Containers

Chests, Double Chests, Barrels, Shulker Boxes, Ender Chests, Dispensers, Droppers, Hoppers

## Usage

1. Open **Options → Controls → Key Binds → SimpleLoot**
2. Bind a key to **Hover Loot (Hold)**
3. Open any container, hold your key, and swipe over items

## Configuration

Access via **ModMenu** or edit `config/simpleloot.json`

| Setting | Description | Default |
|---------|-------------|---------|
| Enabled | Enable/disable the mod | `true` |
| Hotbar Protection | Protect hotbar slots | `true` |
| Transfer Delay | Delay between transfers (ms) | `0` |
| Debug Mode | Enable debug logging | `false` |
| Container Types | Per-container enable/disable | All enabled |

## Keybindings

| Keybind | Description | Default |
|---------|-------------|---------|
| Hover Loot (Hold) | Hold to transfer items you hover over | Unbound |
| Enable/Disable | Toggle the mod on/off | Unbound |
| Open Config | Open config screen | Unbound |

## Requirements

- Minecraft 1.21.10
- Fabric Loader 0.16.9+
- Fabric API
- Cloth Config 20.0.0+
- ModMenu (recommended)

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/)
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) and [Cloth Config](https://modrinth.com/mod/cloth-config)
3. Drop SimpleLoot into your `mods` folder

## Building

```bash
git clone https://github.com/Partacus-SPQR/SimpleLoot.git
cd SimpleLoot
./gradlew build
```

## License

MIT License — see [LICENSE](LICENSE)


## Author

Partacus-SPQR

## Author's Note

Built this because I wanted my goop to transer just like Rust. It is simple and effecient, no more shift clicking!!! :D