# Indian Political Fight – Comedy Battle Arena (fictional parody edition)

A complete, playable 2D fighting game for Android written in Kotlin with a custom Canvas/SurfaceView engine
(fixed 60 FPS loop, no third-party libraries).

**All characters, parties, dialogue and events are fictional parody. No real person or party is depicted.**
Fighters are food-themed caricatures in four invented parties (Spice Samaj, Snack Sena, Cool Dal, Dairy Morcha),
so the humour is balanced and nobody is promoted.

## Build & run
1. Install Android Studio (Koala or newer) with Android SDK 34.
2. File > Open > select this folder. Let Gradle sync (it downloads Gradle 8.7 / AGP 8.5.2 automatically).
3. Pick a device (landscape) or emulator (API 21+), press Run. Or: `./gradlew assembleDebug` after Studio generates the wrapper;
   the APK appears in `app/build/outputs/apk/debug/`.

## Features
- 8 fighters, each with: unique punch/kick animation style, special attack, passive ability, stats, health bar, power meter,
  intro/special/victory/defeat dialogue.
- 7 specials: Election Speech Attack, Manifesto Bomb, Rally Storm, Promise Power, Debate Blast, Vote Wave,
  Campaign Chappal (+ Freebie Flood). Cartoon projectiles, no gore.
- 7 arenas (Parliament Hall, Rally Ground, Bazaar Street, Riverside City, Mustard Field, Old Town, Campaign Stage).
- Modes: Story (10 levels, unlocked sequentially, saved), Quick Fight, Party vs Party, Tournament (4 rounds), Two Player (shared screen).
- Touch controls: Left, Right, Jump, Punch, Kick, Special, Block with multi-touch.
- Real hitbox/hurtbox collision, damage, blocking, knockback, hit-stun, projectiles, timer, score, win/lose/draw.
- AI opponent with difficulty scaling per level (reaction time, blocking, spacing, special use).
- Procedurally generated sound effects and looping background music; SharedPreferences saves progress, best score, settings.
- Letterboxed 1600x900 virtual screen scales to any screen size.

## Code map (`app/src/main/java/com/comedyarena/politicalfight`)
- `Hero.kt` roster, parties, level table (edit here to rebalance or add characters)
- `Fighter.kt` physics, combat state machine, AI, cartoon rendering
- `GameView.kt` game loop, collisions, projectiles, HUD, touch controls, match flow
- `Arenas.kt` backgrounds · `MenuActivity.kt` all menus · `Sfx.kt` audio · `Prefs.kt` saving

## Using sprite sheets later
Characters are drawn from vector shapes. To swap in sprite sheets, replace `Fighter.draw()` with a frame lookup per `state`
(`IDLE/WALK/JUMP/PUNCH/KICK/SPECIAL/BLOCK/HIT/WIN/KO`) using `anim` and `t` as the frame counter.
