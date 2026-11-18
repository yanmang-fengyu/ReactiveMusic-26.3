# Reactive Music — Minecraft 26.3 (Fabric) Port

[English](#english) | [简体中文](#简体中文)

Unofficial port of [CircuitLord/ReactiveMusic](https://github.com/CircuitLord/ReactiveMusic) from **Minecraft 1.21.9 / Yarn mappings** to **Minecraft 26.3 (Fabric)**.

- Download: [Releases](https://github.com/yanmang-fengyu/ReactiveMusic-26.3/releases) — `reactivemusic-1.3.1+26.3.jar` (281 MB, 61 built-in tracks)
- Required: `yet_another_config_lib_v3-3.9.7+26.3-fabric.jar` (YACL, config screen + config I/O)
- Runtime: Fabric Loader ≥ 0.20.1, Fabric API 0.166.2+26.3, Java 25

> The repository contains source code, build scripts and docs only.
> The 61 music tracks (~282 MB) are **not** committed (size + copyright); grab the ready-to-play jar from [Releases](https://github.com/yanmang-fengyu/ReactiveMusic-26.3/releases),
> or drop upstream assets into `src/main/resources/musicpack/music/` and build yourself.

---

## English

### Installation

Put both jars into the `mods` folder of a Minecraft 26.3 Fabric instance:

```
reactivemusic-1.3.1+26.3.jar
yet_another_config_lib_v3-3.9.7+26.3-fabric.jar
```

Fabric API is usually already installed (`fabric-api-0.166.2+26.3.jar` matches).
Music starts on the title screen; `/reactivemusic` opens the config screen (requires YACL).

### What this port adds (1.3.1)

**Tunable timings** — `/reactivemusic` → *General*:

| Option | Description |
| --- | --- |
| Music Delay | Preset: none / short (12.5 s) / normal (45 s) / long (120 s) / songpack default |
| Music Switch Speed | Preset: instant (5 s) / short / normal / long / songpack default |
| **Custom Song Delay (s)** | `-1` = use preset; 0–300, 1-second precision |
| **Custom Switch Wait (s)** | Same, how long before fading out after the situation changes |
| **Fade Out Duration (s)** | 0.5–30, default 7.5 s |
| **Volume While Paused (%)** | 0–100, default 70 |

Config file: `.minecraft/config/ReactiveMusic.json5`.

**Simplified Chinese** — new `assets/reactivemusic/lang/zh_cn.json`; every category, option, tooltip, button and command message is translated. Hard-coded English strings were replaced with translation keys, so adding another language is just another json file. Follows the in-game language.

**Songpack authoring guide** — [`docs/音乐包制作指南.md`](docs/音乐包制作指南.md) (Chinese) and a ready-to-copy template in [`docs/音乐包模板/`](docs/音乐包模板/).

### What was changed for 26.3

1. **Mapping migration** — Minecraft 26.1+ ships unobfuscated, and Fabric dropped Yarn:

   | 1.21.9 / Yarn | 26.3 / official |
   | --- | --- |
   | `MinecraftClient` | `Minecraft` |
   | `ClientPlayerEntity` | `LocalPlayer` |
   | `Text` | `Component` |
   | `Vec3d` | `Vec3` |
   | `SoundCategory` | `SoundSource` |
   | `ResourceLocation` | `Identifier` |
   | `MusicTracker` | `MusicManager` |
   | `BossBarHud` | `BossHealthOverlay` |
   | `MinecraftClient.inGameHud` | `Minecraft.gui` |

   Done with Loom's `migrateMappings` on a 1.21.9 project, then manual fixes for 26.3.

2. **Build system** — no-remap Loom plugin (`net.fabricmc.fabric-loom`), Gradle 9.8.2, Java 25, Loom 1.19.4, Loader 0.20.1, Fabric API 0.166.2+26.3; mixin `compatibilityLevel` `JAVA_21` → `JAVA_25`.

3. **Minecraft 26.3 API changes** — entity packages reshuffled (`animal.pig.Pig`, `animal.equine.Horse`, `npc.villager.Villager`, `vehicle.boat.*`, `vehicle.minecart.*`), screens moved into `Gui` (`mc.gui.screen()`, `setScreenAndShow`), `displayClientMessage` → `sendSystemMessage`, `SoundInstance.getIdentifier()`, `SoundManager.isActive`, `SoundEngine.PlayResult`, `getOverworldClockTime()`, `dimension()`, `ResourceKey.identifier()`, `getSoundSourceVolume`, `gui.hud.getBossOverlay()` (field `events`), Fabric `ClientCommandManager` → `ClientCommands`, and more.

4. **Robustness** — create `resourcepacks/` if missing; compare biome tags with `.equals()` instead of `==` (no longer interned in 26.x) plus null guards.

### Verified

- ✅ Loads on 26.3 (Loader 0.20.1 + Fabric API 0.166.2 + YACL 3.9.7)
- ✅ All three mixins apply (`Minecraft.tick`, `MusicManager.tick`, `SoundManager.play`, boss-bar accessor)
- ✅ Title-screen event fires and an MP3 plays (`ReactiveMusic Player Thread`)
- ✅ Config read/write and `/reactivemusic` registration work

Known gap: in-world situation detection (biomes, mobs, weather) was reviewed against the 26.3 API but not automated-tested in a save.

### Repository layout

```
src/main/…                ported sources & resources (java + assets + musicpack)
docs/                     songpack guide and template
build.gradle              no-remap Loom setup and dependencies
gradle.properties         26.3 / loader / fabric api / yacl / modmenu
```

### Building

Requires JDK 25. Music assets are not in the repo; put the 61 mp3 files into
`src/main/resources/musicpack/music/` first (they can be extracted from an upstream release).

```bash
./gradlew build      # -> build/libs/reactivemusic-1.3.1+26.3.jar
```


### License

GPLv3, inherited from the upstream project. Music assets remain the property of their respective authors and are not distributed here.

---


## 简体中文

### 安装

把这两个 jar 丢进 26.3 实例的 `mods` 文件夹：

```
reactivemusic-1.3.1+26.3.jar
yet_another_config_lib_v3-3.9.7+26.3-fabric.jar
```

Fabric API 一般已经装了（`fabric-api-0.166.2+26.3.jar` 正好匹配）。
进游戏后主菜单就会开始播放；`/reactivemusic` 打开配置界面（需要 YACL）。

### 本次新增（1.3.1）

**可调参数**（`/reactivemusic` →「常规」）：

| 选项 | 说明 |
| --- | --- |
| 音乐间隔 | 预设：无 / 短（12.5 秒）/ 中（45 秒）/ 长（120 秒）/ 音乐包默认 |
| 切歌速度 | 预设：立即（5 秒）/ 短 / 中 / 长 / 音乐包默认 |
| **自定义音乐间隔（秒）** | `-1` = 使用预设；0–300，精确到 1 秒 |
| **自定义切歌等待（秒）** | 同上，控制情境失效后多久开始淡出 |
| **淡出时长（秒）** | 0.5–30，默认 7.5 秒 |
| **暂停时音量（%）** | 0–100，默认 70 |

配置文件：`.minecraft/config/ReactiveMusic.json5`。

**简体中文** —— 新增 `assets/reactivemusic/lang/zh_cn.json`，配置界面的分类、选项、按钮、悬浮说明、命令提示全部中文化；源码里硬编码的英文文本换成了可翻译键，以后加语言只需加 json。跟随游戏语言生效。

**音乐包指南** —— [`docs/音乐包制作指南.md`](docs/音乐包制作指南.md)，以及可直接复制的 [`docs/音乐包模板/`](docs/音乐包模板/)。

### 这次移植做了什么

1. **映射体系迁移**：Minecraft 26.1 起官方直接发布未混淆代码，Fabric 停止维护 Yarn，所以整套名字要换成官方（Mojang）名字：

   | 1.21.9 / Yarn | 26.3 / 官方 |
   | --- | --- |
   | `MinecraftClient` | `Minecraft` |
   | `ClientPlayerEntity` | `LocalPlayer` |
   | `Text` | `Component` |
   | `Vec3d` | `Vec3` |
   | `SoundCategory` | `SoundSource` |
   | `ResourceLocation` | `Identifier` |
   | `MusicTracker` | `MusicManager` |
   | `BossBarHud` | `BossHealthOverlay` |
   | `MinecraftClient.inGameHud` | `Minecraft.gui` |

   先在 1.21.9 工程上用 Loom 的 `migrateMappings` 半自动迁移，再针对 26.3 差异逐项修正。

2. **构建体系**：换成无重映射模式（`net.fabricmc.fabric-loom`，26.1+ 不再需要 intermediary，也不接受 `mappings` 依赖）；Gradle 9.8.2 + Java 25；Loom 1.19.4、Loader 0.20.1、Fabric API 0.166.2+26.3；mixin `compatibilityLevel` 改为 `JAVA_25`。

3. **Minecraft 26.3 API 变更**：实体包结构重排（`animal.pig.Pig`、`animal.equine.Horse`、`npc.villager.Villager`、`vehicle.boat.*`、`vehicle.minecart.*`）；屏幕系统移入 `Gui`（`mc.gui.screen()`、`setScreenAndShow`）；`displayClientMessage` → `sendSystemMessage`；`SoundInstance.getIdentifier()`、`SoundManager.isActive`、`SoundEngine.PlayResult`；`getOverworldClockTime()`、`dimension()`、`ResourceKey.identifier()`；`getSoundSourceVolume`；`gui.hud.getBossOverlay()`（字段变 `events`）；Fabric 的 `ClientCommandManager` → `ClientCommands` 等。

4. **健壮性**：`resourcepacks` 目录不存在时自动创建；生物群系标签改用 `.equals()` 比较（26.x 下 `Identifier` 不再保证 intern），并加空值保护。

### 已验证

- ✅ 26.3 客户端能加载（Fabric Loader 0.20.1 + Fabric API 0.166.2 + YACL 3.9.7）
- ✅ 三个 mixin 全部注入成功（`Minecraft.tick`、`MusicManager.tick`、`SoundManager.play`、BossBar Accessor）
- ✅ 主菜单事件触发并成功播放 MP3
- ✅ 配置读写（YACL）正常，`/reactivemusic` 命令注册成功

未覆盖：自动化环境里没法自动进入存档，所以“进世界后的生物群系/怪物/天气事件判定”只做了代码级核对，建议首次进游戏时留意（尤其不同维度、村庄/怪物附近、下雨天）。

### 工程结构

```
src/main/…                已移植的源码与资源（java + assets + 音乐包）
docs/                     音乐包制作指南与模板
build.gradle              Loom 无重映射 + 依赖
gradle.properties         26.3 / loader / fabric api / yacl / modmenu
```

### 重新构建

需要 JDK 25。仓库不含音乐素材，请先把 61 个 mp3 放进 `src/main/resources/musicpack/music/`（可从上游发布版提取）。

```bash
./gradlew build      # -> build/libs/reactivemusic-1.3.1+26.3.jar
```


### 版权

沿用上游 GPLv3。音乐素材版权归各自作者所有，不随本仓库分发。

### 重要提示

本模组几乎全由 deepseek 更改而来，若出了问题记得发运行日志，作者能力不好



