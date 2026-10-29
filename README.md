# DuoDeck

A playful, 3DS / Wii U-style dual-screen game launcher for Android, in the spirit of iiSU, Pocket Pass and Socket.

- **Top screen**: big square art, title, system, RetroAchievements progress, which emulator will run it, PLAY, your avatar and recently played.
- **Bottom screen**: tabs (All, Favourites, Recent, Groups, Apps, one per system) and square tiles in **pages** (Wii U style), a scrolling **grid**, or a **carousel**.
- **Groups**: make folders of games; long-press a game → Add to group.
- **Apps**: add any installed app as a tile.
- **Box art**: SteamGridDB automatic square covers, or long-press → Change box art to browse options or upload your own image.
- **Emulators & cores**: Settings → Emulators & cores (or per game via long-press). Choose app, launch style and RetroArch core. Switch (Eden, Yuzu, Citron, Sudachi…), Dolphin, PPSSPP, Azahar, melonDS, DuckStation, NetherSX2, Flycast, Redream, RetroArch supported.
- **Theme Store**: install themes from `themes/index.json` in this repo, import/share theme files, light and dark for each, custom wallpaper.
- **Home menu music**: original built-in loops, your own audio file, or off.
- **Avatar maker**: original Mii-style character creator.
- Gamepad: D-pad moves, A launches, X opens the game menu, B backs out of a group, Y changes layout, L1/R1 change tab, Start opens settings.

## Build

Push to GitHub; the **Build APK** workflow produces `DuoDeck-apk` under Actions → the run → Artifacts.

## Adding a theme to the store

Add an entry to `themes/index.json` (copy an existing one). Colours are `#RRGGBB` or `#AARRGGBB`.
