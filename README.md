# 🍳 CookSense

[![CurseForge](https://cf.way2muchnoise.eu/full_cooksense_downloads.svg)](https://www.curseforge.com/minecraft/mc-mods/cooksense)
[![Modrinth](https://img.shields.io/modrinth/dt/cooksense?logo=modrinth&label=Modrinth)](https://modrinth.com/mod/cooksense)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21--1.21.1-brightgreen.svg)]()
[![Fabric](https://img.shields.io/badge/Loader-Fabric-blue.svg)](https://fabricmc.net/)

A lightweight, client-side Minecraft quality-of-life mod that displays floating countdown timers and cooking progress indicators directly above campfires and soul campfires.

---

## 📥 Downloads

Official releases of CookSense are exclusively distributed on:
- [**CurseForge**](https://www.curseforge.com/minecraft/mc-mods/cooksense)
- [**Modrinth**](https://modrinth.com/mod/cooksense)

> **Notice:** We do not host or distribute compiled `.jar` binaries on GitHub to maintain official version verification and download analytics. Please only download CookSense from the official platforms above.

---

## ✨ Features

- **Live Countdown Timers**: Real-time remaining cooking seconds displayed above each campfire slot.
- **Visual Progress Bars**: Animated progress bars smoothly track cooking progress and flash emerald green when ready.
- **Stacked Item Grouping**: Multiple identical items placed at the same time are cleanly grouped into a single compact row.
- **Soul Campfire Theme**: Automatically adapts to a glowing cyan and dark navy palette when cooking over soul fire.
- **Adaptive Height & Occlusion**: Adjusts height under low ceilings and rotates smoothly with camera viewing angles.
- **100% Client-Side**: Compatible with singleplayer, vanilla servers, Spigot/Paper/Purpur networks, and Minecraft Realms.

---

## ⚙️ Configuration

CookSense can be customized by editing `.minecraft/config/cooksense.json`:

- **`enabled`** *(default: `true`)* — Master toggle to enable or disable the overlay.
- **`renderDistance`** *(default: `8.0`)* — Maximum distance (in blocks) at which timers will render.
- **`textScale`** *(default: `1.0`)* — Scale multiplier for the floating plate and text.
- **`soulCampfireBlue`** *(default: `true`)* — Toggles the mystical cyan color theme for soul campfires.
- **`seeThroughBlocks`** *(default: `false`)* — When enabled, timers render through walls and solid blocks.
- **`textShadow`** *(default: `true`)* — Renders a drop shadow behind text for enhanced readability.

---

## 🛠️ Building from Source

To build CookSense locally:

```bash
git clone https://github.com/Sattik03/CookSense.git
cd CookSense
./gradlew build
```

Compiled `.jar` files will be generated in `build/libs/`.

---

## 📄 License

CookSense is licensed under the [MIT License](LICENSE). You are free to include CookSense in any public or private modpack.
