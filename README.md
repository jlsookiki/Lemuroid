# Lemuroid, handheld-console edition

A fork of [Lemuroid](https://github.com/Swordfish90/Lemuroid) that turns an Android gaming handheld into something closer to a Steam Deck: one library, covers for everything, automatic resume, and no Android home screen in the way. Built and used on an Anbernic RG406V, should work on any Android 6+ device with a gamepad.

## What's different from upstream

- **One library.** Every game in a single cover grid with filter chips for All, Favorites and each system. A "Continue playing" banner at the top for the last game played. Dark, fixed Steam-style palette. D-pad focus scales tiles and draws a ring, so it reads right on a controller.
- **Local cover art.** Drop `Game Name.png` (or jpg/webp) next to the ROM, or in a `media`, `covers` or `images` subfolder, and it overrides the Libretro thumbnail. Works for ROM hacks and anything the thumbnail servers don't know.
- **Game page.** Select a tile to get a page with the cover, description, developer, year and genre, read from an EmulationStation-style `gamelist.xml` or a `Game Name.txt` beside the ROM. Resume, Restart and Favorite live there too.
- **Saves you can see.** The game page has a Saves tab: export and import the battery save to `saves/` beside the ROM, see the automatic resume point and the four state slots with screenshots, and pick any slot as the resume point.
- **Named save profiles.** Several battery saves per game, stored as plain files next to the ROM (`saves/Game.srm`, `saves/Game.<profile>.srm`). The active profile is mirrored out at every launch.
- **Can be the home screen.** Lemuroid registers as an Android launcher. Set it as home and the device boots straight into the library. An Apps drawer in the top bar keeps every other app reachable. Games run in their own task, so the Home button backgrounds a game with an automatic save, and starting a different game saves and quits the first one.

Not included: RetroAchievements. Upstream Lemuroid has no support for it and adding it means work in the native emulation layer, so the tab is a placeholder.

## Install

1. Download the APK from the [Releases](../../releases) page and install it. It is signed with a different key than the F-Droid and Play builds, so uninstall those first.
2. Open it, choose your ROMs folder. Pick the folder even if you have used Lemuroid before: this build needs write access to it for save export.
3. Optional, to make it the home screen: Android Settings → Apps → Default apps → Home app → Lemuroid. On a device without a lock screen it will boot straight into the library.

Covers and descriptions are plain files beside your ROMs, for example:

```
ROMs/gba/Pokemon Unbound.gba
ROMs/gba/media/Pokemon Unbound.png
ROMs/gba/gamelist.xml
ROMs/gba/saves/Pokemon Unbound.srm
```

## Building

Standard Android build. Release signing expects `release.jks` at the repo root with alias and passwords `lemuroid`; generate your own with `keytool`.

```
./gradlew :lemuroid-app:assembleFreeDynamicRelease
```

## Credits and license

All the emulation is Lemuroid and LibretroDroid by Swordfish90 and the Libretro cores. This fork only changes the front end. GPL-3.0, same as upstream. The original README follows.

---

# Lemuroid

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/com.swordfish.lemuroid/)
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=com.swordfish.lemuroid)

## Description

Lemuroid is an open-source emulation project for Android based on Libretro. Its main goal is ease of use, good Android integration and a great user experience.

It originated from a rib of [Retrograde](https://github.com/retrograde/retrograde-android), but graduated to a standalone project integrating [LibretroDroid](https://github.com/Swordfish90/LibretroDroid).

|Screen 1|Screen 2|Screen 3|
|---|---|---|
|![Screen1](https://github.com/Swordfish90/Lemuroid/blob/master/fastlane/metadata/android/en-US/images/phoneScreenshots/1.jpg)|![Screen2](https://github.com/Swordfish90/Lemuroid/blob/master/fastlane/metadata/android/en-US/images/phoneScreenshots/2.jpg)|![Screen3](https://github.com/Swordfish90/Lemuroid/blob/master/fastlane/metadata/android/en-US/images/phoneScreenshots/3.jpg)|

### Supported Systems:
- Atari 2600 (A26) ([stella](https://docs.libretro.com/library/stella/))
- Atari 7800 (A78) ([prosystem](https://docs.libretro.com/library/prosystem/))
- Atari Lynx (Lynx) ([handy](https://docs.libretro.com/library/handy/))
- Nintendo (NES) ([fceumm](https://docs.libretro.com/library/fceumm/))
- Super Nintendo (SNES) ([snes9x](https://docs.libretro.com/library/snes9x/))
- Game Boy (GB) ([gambatte](https://docs.libretro.com/library/gambatte/))
- Game Boy Color (GBC) ([gambatte](https://docs.libretro.com/library/gambatte/))
- Game Boy Advance (GBA) ([mgba](https://docs.libretro.com/library/mgba/))
- Sega Genesis (aka Megadrive) ([genesis_plus_gx](https://docs.libretro.com/library/genesis_plus_gx/))
- Sega CD (aka Mega CD) ([genesis_plus_gx](https://docs.libretro.com/library/genesis_plus_gx/))
- Sega Master System (SMS) ([genesis_plus_gx](https://docs.libretro.com/library/genesis_plus_gx/))
- Sega Game Gear (GG) ([genesis_plus_gx](https://docs.libretro.com/library/genesis_plus_gx/))
- Nintendo 64 (N64) ([mupen64plus](https://docs.libretro.com/library/mupen64plus/))
- PlayStation (PSX) ([PCSX-ReARMed](https://docs.libretro.com/library/pcsx_rearmed/))
- PlayStation Portable (PSP) ([ppsspp](https://docs.libretro.com/library/ppsspp/))
- FinalBurn Neo (Arcade) ([fbneo](https://github.com/libretro/FBNeo/))
- Nintendo DS (NDS) ([desmume](https://docs.libretro.com/library/desmume/)/[MelonDS](https://docs.libretro.com/library/melonds/))
- NEC PC Engine (PCE) ([beetle_pce_fast](https://docs.libretro.com/library/beetle_pce_fast/))
- Neo Geo Pocket (NGP) ([mednafen_ngp](https://docs.libretro.com/library/beetle_neopop/))
- Neo Geo Pocket Color (NGC) ([mednafen_ngp](https://docs.libretro.com/library/beetle_neopop/))
- WonderSwan (WS) ([beetle_cygne](https://docs.libretro.com/library/beetle_cygne/))
- WonderSwan Color (WSC) ([beetle_cygne](https://docs.libretro.com/library/beetle_cygne/))
- Nintendo 3DS (3DS) ([citra](https://docs.libretro.com/library/citra/))

### Features:
- Android TV support
- Automatically save and restore game states.
- ROMs scanning and indexing
- Optimized touch controls
- Quick save/load
- Support for Zipped ROMs
- Display simulation (LCD/CRT)
- Gamepad support
- Local multiplayer
- Tilt input
- Customizable touch controls (size and position)
- Cloud save sync
- HD mode

### Languages:
You can help translate Lemuroid in your native language by going here: https://crowdin.com/project/lemuroid
