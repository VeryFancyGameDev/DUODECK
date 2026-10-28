# DuoDeck

A playful, 3DS-style dual-screen game launcher for Android, in the spirit of iiSU, Pocket Pass and Socket.

- **Top screen**: big cover art, title, system, RetroAchievements progress bar and a PLAY button.
- **Bottom screen**: system tabs, search, a **grid** or **carousel** game browser, and a bubble-icon action bar.
- **Dual screen**: on a device with a second physical display the top screen goes there; on a single screen the two stack (portrait) or sit side by side (landscape).
- Light / dark / system theme, five accent colours, dotted 3DS wallpaper, glossy gradient icons, squishy press animations.
- **RetroAchievements**: points, rank and per-game progress badges.
- **SteamGridDB**: automatic cover art, cached on disk. Works without a key (colourful placeholder tiles).
- Gamepad/D-pad: D-pad moves, A launches, Y swaps grid/carousel, L1/R1 change system, Start opens settings.
- Can be set as your Android home app.

## Get the APK with no setup (GitHub does the build)

1. Create a new empty GitHub repository.
2. Upload the contents of this folder (keep the `.github` folder).
3. Open the **Actions** tab, wait for **Build APK** to turn green (about 5 minutes).
4. Open the run, download the **DuoDeck-apk** artifact, unzip, and install `app-release.apk` on your device.

Or open the folder in Android Studio and press Run.

## First launch

Tap **Choose ROMs folder** and pick your ROMs folder. That's the only required step.
Put games in subfolders named after the system (`GBA`, `SNES`, `PSP`, `Dreamcast`…); DuoDeck also recognises common file types.

## Emulators

DuoDeck launches whichever supported emulator is installed (RetroArch, Dolphin, PPSSPP, Azahar/Citra, melonDS, DuckStation, NetherSX2, Flycast, Redream…).
Long-press a game to choose a different app. Launching is best-effort because every emulator accepts files a little differently; RetroArch uses a built-in core per system.
