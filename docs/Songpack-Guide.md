# Reactive Music — Songpack Authoring Guide

> Applies to **Reactive Music 1.3.1 for Minecraft 26.3 (Fabric)** as ported in this repository.
> 中文版：[音乐包制作指南.md](音乐包制作指南.md)

Reactive Music does not play a fixed playlist. It first decides **what situation you are in** (day/night, rain, underground, village, boss fight…), then picks a song from the matching *entry*. All you have to do is **define situations → bind songs**.

---

## 1. Quick start (5 minutes)

A songpack is just a folder (or a zip) inside `.minecraft/resourcepacks/`:

```
resourcepacks/
└─ MyPack/                     ← folder name is free (could also be MyPack.zip)
   ├─ MyPack.yaml              ← config, at the pack root, .yaml extension
   └─ music/
      ├─ CalmMorning.mp3       ← song name = file name (without .mp3)
      └─ BattleTheme.mp3
```

Minimal `MyPack.yaml`:

```yaml
name: "My Pack"
description: "Personal use"
version: "1.0"
credits: "By me"

musicDelayLength: NORMAL      # silence between songs (preset)
musicSwitchSpeed: NORMAL      # how long before fading out when the situation goes away

entries:
  - events: [ "MAIN_MENU" ]   # title screen
    forceStartMusicOnValid: true
    songs:
      - "CalmMorning"

  - events: [ "DAY" ]         # daytime (generic)
    songs:
      - "CalmMorning"
      - "BattleTheme"
```

Then in game: open the config screen (`/reactivemusic` or Mod Menu) → *Songpacks* → **Load**.
If loading fails or produced warnings, the reason is shown right there.

---

## 2. Matching logic (the most important section)

`events` is an **array of strings**:

| Form | Meaning |
| --- | --- |
| `events: [ "DAY", "RAIN" ]` | **AND** — daytime **and** raining |
| `events: [ "DAY\|\|RAIN" ]` | **OR** — daytime **or** raining |
| `events: [ "DAY", "BIOME=mountain" ]` | daytime and mountain |

- Each array element is one *condition group*; **every group must pass** (AND).
- Inside a group, different kinds of conditions (events / dimension / biome / block) are **OR**-ed;
  `||` simply puts several conditions into the same group (useful for “any of these biomes”).

When several entries match at once, **entries earlier in the YAML win**. Put specific situations first
and generic ones (like `DAY`) last.

---

## 3. Top-level fields

| Field | Type | Description |
| --- | --- | --- |
| `name` | string | Display name (also the identifier saved as “currently loaded songpack”) |
| `version` | string | Arbitrary version string |
| `author` | string | Author |
| `description` | string | Shown in the config screen |
| `credits` | string | Shown in the config screen |
| `musicDelayLength` | enum | Delay preset: `NONE` / `SHORT` (12.5 s) / `NORMAL` (45 s) / `LONG` (120 s) |
| `musicSwitchSpeed` | enum | Wait before fading out after the situation goes away: `INSTANT` (5 s) / `SHORT` (12.5 s) / `NORMAL` (45 s) / `LONG` (120 s) |
| `entries` | list | The entries, see below |

> Players can override these presets with *Custom Song Delay / Custom Switch Wait* in the config screen;
> your values are only used while the player keeps “songpack default”.

---

## 4. Entry fields

| Field | Type | Default | Description |
| --- | --- | --- | --- |
| `events` | string array | required | Conditions, see section 5 |
| `songs` | string array | required | Song names (no `.mp3`; maps to `music/<song>.mp3`) |
| `allowFallback` | bool | `false` | Allow borrowing songs from other entries when all of this entry's songs played recently |
| `forceStartMusicOnValid` | bool | `false` | Start playing immediately when the condition becomes true |
| `forceStopMusicOnChanged` | bool | `false` | Force-stop current music when the condition toggles (= both flags below) |
| `forceStopMusicOnValid` | bool | `false` | Stop current music when the condition becomes true |
| `forceStopMusicOnInvalid` | bool | `false` | Stop current music when the condition becomes false |
| `forceChance` | 0.0–1.0 | `1.0` | Probability of the `force*` behaviours (rolled once per situation change) |
| `startMusicOnEventValid` | bool | `false` | Deprecated, do not use |
| `stackable` / `alwaysPlay` / `alwaysStop` | — | — | Legacy fields; triggers the v0.5 compatibility warning |

Common patterns:

```yaml
# Title screen: switch immediately
- events: [ "MAIN_MENU" ]
  forceStopMusicOnChanged: true
  forceStartMusicOnValid: true
  songs: [ "Title" ]

# Boss: switch as soon as the boss bar appears
- events: [ "BOSS" ]
  forceStopMusicOnChanged: true
  forceStartMusicOnValid: true
  songs: [ "LastStand", "Ruthless" ]

# Sunset: only force-switch 50% of the time
- events: [ "DIM=overworld", "SUNSET" ]
  forceStopMusicOnChanged: true
  forceStartMusicOnValid: true
  forceChance: 0.5
  songs: [ "Sunset1", "Sunset2" ]
```

---

## 5. Event reference

### 5.1 Built-in events (case-insensitive)

| Event | Condition |
| --- | --- |
| `MAIN_MENU` | Title screen / not in a world |
| `CREDITS` | End credits are playing |
| `HOME` | Within 45 blocks of the position recorded on last sleep |
| `DAY` / `NIGHT` / `SUNRISE` / `SUNSET` | Time of day (day 23000–13000, night 13000–23000) |
| `RAIN` / `SNOW` / `STORM` | Rain / snow (cold biomes) / thunderstorm |
| `UNDERWATER` | Player is underwater |
| `UNDERGROUND` | Overworld, cannot see the sky, Y < 55 |
| `DEEP_UNDERGROUND` | Same, Y < 15 |
| `HIGH_UP` | Overworld, can see the sky, Y > 128 |
| `MINECART` / `BOAT` / `HORSE` / `PIG` | Riding the matching vehicle/mob |
| `FISHING` | Bobber is cast |
| `DYING` | Health below 35% |
| `OVERWORLD` / `NETHER` / `END` | Dimension (or use `DIM=`, below) |
| `BOSS` | A boss bar is on screen |
| `VILLAGE` | At least one villager within 30 blocks |
| `NEARBY_MOBS` | At least one hostile mob within 12 blocks |
| `GENERIC` | Always true (fallback) |

### 5.2 Parameterised conditions

| Form | Description |
| --- | --- |
| `DIM=overworld` | Dimension id *contains* the string (`overworld` / `the_nether` / `the_end`; use an id fragment for modded dimensions) |
| `BIOME=mountain` | Current biome id **contains** the string (e.g. `minecraft:mountain_*`); `plains`, `desert` work too |
| `BIOMETAG=forest` | Fabric's `ConventionalBiomeTags`. Use the tag name without `IS_`/`IN_`/`CLIMATE_` prefix, lowercase |
| `BLOCK=nether_bricks,1000` | At least 1000 blocks whose id contains `nether_bricks` within a **25-block radius cube** around the player |

Common `BIOMETAG` values (not exhaustive — see Fabric API `ConventionalBiomeTags`):

```
forest  birch_forest  dark_forest  flower_forest  taiga  jungle  savanna  swamp
desert  badlands  beach  stony_shores  mushroom  river  ocean  deep_ocean  shallow_ocean
mountain  mountain_peak  mountain_slope  hill  windswept  plateau  plains  snowy_plains
cave  underground  lush  magical  rare  dead  wasteland
cold  hot  temperate  wet  dry  vegetation_sparse  vegetation_dense
coniferous_tree  deciduous_tree  jungle_tree  savanna_tree  old_growth
```

> `BLOCK` scanning is incremental: one column per tick, a full 51×51×51 cube takes ~2.5 s.
> Keep the threshold reasonably high or ordinary terrain will trigger it.

---

## 6. Audio requirements

- **MP3 only** (the mod ships its own MP3 decoder and does not go through vanilla audio).
- File name = song name: `music/CalmMorning.mp3` ↔ `songs: [ "CalmMorning" ]`.
  **Case-sensitive**; stick to letters, digits, `-` and `_`.
- 44.1 kHz stereo, 128–192 kbps recommended; larger files slow down loading.
- Keep the whole pack under ~300 MB (the built-in pack is ~280 MB).
- Tracks should start and end cleanly (no long silence): switching is situation-driven, a song is not guaranteed to finish.

---

## 7. Debugging & troubleshooting

1. **Check the config screen first**: failed packs show the exact reason
   (missing song, YAML syntax error, misspelled event, …).
2. **Enable Debug Mode**: Config → *Debug* → *Debug Mode Enabled*. Effects:
   - switch songs instantly whenever the situation changes (handy to hear whether an event fires)
   - always show all songpack warnings in the config screen
3. **Chat logging** (requires cheats/OP):
   - `/reactivemusic toggleLogging` — log entry/song changes to chat
   - `/reactivemusic toggleSoundEventLogging` — print every played sound id (useful to find sound names)
   - `/reactivemusic logBlockCounter` — dump surrounding block counts (for tuning `BLOCK=`)
   - `/reactivemusic blacklistDimension` / `unblacklistDimension` — add/remove the current dimension to the blacklist
4. **Read the log**: search `reactive_music` in `logs/latest.log`; it prints parse time and load errors.

FAQ:

| Symptom | Cause |
| --- | --- |
| No song at all loaded | Song name ≠ file name (case!), or file not under `music/` |
| “Failed to load” in the UI | YAML syntax error; missing `name`/`entries`; tabs used for indentation |
| An event never fires | Misspelled event name (shows up as a warning), or a higher-priority entry shadows it |
| Always the same song | Too few songs in the entry, or all played recently without `allowFallback` |
| Abrupt song changes | Entry lacks `forceStopMusicOnChanged`; also lower *Music Switch Speed* |
| No sound at all | The mod lowers master volume to 50% on first launch; check music/master sliders |

---

## 8. Publishing

Zip the folder (**yaml at the zip root**, no extra nesting):

```
MyPack.zip
├─ MyPack.yaml
└─ music/*.mp3
```

Drop it into `resourcepacks/`. Players can switch between multiple packs in the config screen.

---

## 9. References

- Built-in songpack: [`src/main/resources/musicpack/ReactiveMusic.yaml`](../src/main/resources/musicpack/ReactiveMusic.yaml)
- Template: [`docs/音乐包模板/`](音乐包模板/) (copy, rename, edit)
- Upstream project: <https://github.com/CircuitLord/ReactiveMusic>
