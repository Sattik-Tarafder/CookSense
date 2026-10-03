# CookSense

[![CurseForge](https://cf.way2muchnoise.eu/full_cooksense_downloads.svg)](https://www.curseforge.com/minecraft/mc-mods/cooksense)
[![Modrinth](https://img.shields.io/modrinth/dt/cooksense?logo=modrinth&label=Modrinth)](https://modrinth.com/mod/cooksense)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.4-brightgreen.svg)]()
[![Fabric](https://img.shields.io/badge/Loader-Fabric-blue.svg)](https://fabricmc.net/)
[![NeoForge](https://img.shields.io/badge/Loader-NeoForge-orange.svg)](https://neoforged.net/)

A lightweight, client-side Minecraft quality-of-life mod that displays floating countdown timers and cooking progress indicators directly above campfires and soul campfires.

---

## Downloads

Official releases of CookSense are exclusively distributed on:
- [**CurseForge**](https://www.curseforge.com/minecraft/mc-mods/cooksense)
- [**Modrinth**](https://modrinth.com/mod/cooksense)

---

## Features

- **Countdown Timers** — Displays the exact remaining cooking time for each item.
- **Progress Bars** — Smooth color-coded bars that turn green when food is ready.
- **Smart Grouping** — Combines identical items into one row to prevent clutter.
- **Soul Campfire Theme** — Custom glowing cyan styling for soul campfires.
- **100% Client-Side** — Fully client-side; works on singleplayer, vanilla servers, and Realms without server installation.

---

## Configuration

CookSense can be customized by editing `.minecraft/config/cooksense.json`:

- **`enabled`** *(default: `true`)* — Master toggle to enable or disable the overlay.
- **`renderDistance`** *(default: `8.0`)* — Maximum distance (in blocks) at which timers will render.
- **`textScale`** *(default: `1.0`)* — Scale multiplier for the floating plate and text.
- **`soulCampfireBlue`** *(default: `true`)* — Toggles the mystical cyan color theme for soul campfires.
- **`seeThroughBlocks`** *(default: `false`)* — When enabled, timers render through walls and solid blocks.
- **`textShadow`** *(default: `true`)* — Renders a drop shadow behind text for enhanced readability.

---

## Building from Source

To build CookSense locally:

```bash
git clone https://github.com/Sattik-Tarafder/CookSense.git
cd CookSense
./gradlew build
```

Compiled `.jar` files will be generated in `fabric/build/libs/` and `neoforge/build/libs/`.

---

## License

CookSense is licensed under the [MIT License](LICENSE). You are free to include CookSense in any public or private modpack.
