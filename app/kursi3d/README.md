# Kursi Clash 3D (Kotlin + three.js)

3D toon cartoon fighting game. Original fictional characters (no real people).
Kotlin shell (MainActivity) hosts the game in a full-screen WebView; the game lives in app/src/main/assets/game.

## Run on Android
1. Android Studio (Koala+) -> File > Open -> this folder, wait for Gradle sync.
2. Run on a phone/emulator (minSdk 24, landscape).

## Run in a browser (no build)
Open `kursi-clash-3d.html` (single file) in Chrome. Add `?touch=1` to force touch buttons.

## Controls
Touch: left D-pad, right PUNCH / KICK / GRAB / SUPER.
Keyboard: A/D move, W jump, S crouch, J punch, K kick, I grab, L/Space super, Esc pause, M music, N mute.
Hold back = block. Combos: P,P,P / P,P,K / P,K / K,K. Crouch+K sweep, Crouch+P uppercut, air kick.

## 500 Levels
Title > **500 LEVELS**: pick a fighter, then any level (1-500). Every level is generated and different: its own enemy (new colours/looks), arena, difficulty and twist (low gravity, big heads, turbo, glass cannons, rage/iron/giant enemy, mini fighters, sprint, slow-mo, full super). A boss fight every 10th level. Stars (by HP left) and progress are saved on the device.
