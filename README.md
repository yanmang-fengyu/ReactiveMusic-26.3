# Reactive Music → Minecraft 26.3 (Fabric) 移植说明

把 [CircuitLord/ReactiveMusic](https://github.com/CircuitLord/ReactiveMusic) 从 **Minecraft 1.21.9 / Yarn 映射** 移植到 **Minecraft 26.3（Fabric）**。

- 产出 jar：`dist/reactivemusic-1.3.1+26.3.jar`（约 281 MB，内含 61 首背景音乐）
- 必需前置：`dist/yet_another_config_lib_v3-3.9.7+26.3-fabric.jar`（YACL，配置界面/配置读写）
- 游戏前置：Fabric Loader ≥ 0.20.1、Fabric API 0.166.2+26.3、Java 25

> **GitHub**：<https://github.com/yanmang-fengyu/ReactiveMusic-26.3>
>
> 仓库只包含源码、构建脚本和文档；61 首音乐素材（约 282 MB）因版权与体积原因不入库。
> 打包好的完整 jar 见仓库的 [Releases](https://github.com/yanmang-fengyu/ReactiveMusic-26.3/releases)，
> 也可以把上游音乐素材放进 `src/main/resources/musicpack/music/` 后自行 `gradle build`。

## 安装

把这两个 jar 丢进 26.3 实例的 `mods` 文件夹：

```
reactivemusic-1.3.1+26.3.jar
yet_another_config_lib_v3-3.9.7+26.3-fabric.jar
```

Fabric API 一般已经装了（你这个实例里是 `fabric-api-0.166.2+26.3.jar`，正好匹配）。
进游戏后主菜单就会开始播放 Reactive Music 的曲子；`/reactivemusic` 打开配置界面（需要 YACL），
`/reactivemusic blacklistDimension` 之类子命令仍然可用。

## 本次新增（1.3.1）

### 可调参数（游戏内：`/reactivemusic` →「常规」）

| 选项 | 说明 |
| --- | --- |
| 音乐间隔 | 预设：无 / 短（12.5 秒）/ 中（45 秒）/ 长（120 秒）/ 音乐包默认 |
| 切歌速度 | 预设：立即（5 秒）/ 短 / 中 / 长 / 音乐包默认 |
| **自定义音乐间隔（秒）** | -1 = 使用预设；0–300，可精确到 1 秒 |
| **自定义切歌等待（秒）** | 同上，控制情境失效后多久开始淡出 |
| **淡出时长（秒）** | 0.5–30，默认 7.5 秒 |
| **暂停时音量（%）** | 0–100，默认 70（暂停/开菜单时降低音量） |

配置文件：`.minecraft/config/ReactiveMusic.json5`，也可以直接改。

### 简体中文

- 新增 `assets/reactivemusic/lang/zh_cn.json`，配置界面的分类、选项、按钮、悬浮说明、命令提示全部中文化
- 跟随游戏语言：游戏语言为「简体中文」时自动生效
- 同时把源码里硬编码的英文文本换成了可翻译键（以后加语言只需加 json）

### 音乐包指南

- [`docs/音乐包制作指南.md`](docs/音乐包制作指南.md)：字段表、事件总表、匹配逻辑（且/或）、音频要求、调试排错
- [`docs/音乐包模板/`](docs/音乐包模板/)：复制到 `resourcepacks/` 改名即可用，附带示例 YAML
## 这次移植做了什么

### 1. 映射体系迁移（最大的一块）

Minecraft 从 **26.1 起官方直接发布未混淆代码**，Fabric 也随之停止维护 Yarn。
所以原来那套 Yarn 名字全部要换成官方（Mojang）名字，例如：

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

做法：先在 1.21.9 工程上用 Loom 的 `migrateMappings` 把源码从 Yarn 半自动迁移到 Mojang 映射，
再针对 26.3 的差异逐项修正（见下）。

### 2. 构建体系

- 插件从 `fabric-loom` 换成 **无重映射模式**：`apply plugin: 'net.fabricmc.fabric-loom'`
  （26.1+ 不需要 intermediary 重映射，Loom 也不再接受 `mappings` 依赖）
- Gradle 9.8.2 + Java 25（26.3 要求 Java 25）
- Loom 1.19.4，Fabric Loader 0.20.1，Fabric API 0.166.2+26.3
- mixin 配置 `compatibilityLevel` 从 `JAVA_21` 改为 `JAVA_25`

### 3. Minecraft 26.3 API 变更适配

- 实体类包结构重排：`entity.animal.Pig` → `entity.animal.pig.Pig`、`animal.horse.Horse` → `animal.equine.Horse`、
  `npc.Villager` → `npc.villager.Villager`、`vehicle.Boat/Minecart` → `vehicle.boat/vehicle.minecart.*`
- 屏幕系统移入 `Gui`：`mc.screen` → `mc.gui.screen()`，`mc.setScreen(x)` → `mc.setScreenAndShow(x)`
- `displayClientMessage(x, false)` → `sendSystemMessage(x)`
- `SoundInstance.getLocation()` → `getIdentifier()`；`SoundManager.isPlaying` → `isActive`；
  `play()` 返回类型 `SoundSystem.PlayResult` → `SoundEngine.PlayResult`（mixin 描述符同步改了）
- 时间/维度：`world.getTimeOfDay()` → `getOverworldClockTime()`，`getRegistryKey()` → `dimension()`，
  `ResourceKey.location()` → `identifier()`
- `Options.getSoundVolume / getSoundVolumeOption` → `getSoundSourceVolume / getSoundSourceOptionInstance`
- `inGameHud.getBossBarHud()` → `gui.hud.getBossOverlay()`，且 BossBar 的字段名变成 `events`
- Fabric API：`ClientCommandManager` → `ClientCommands`（`ClientCommandRegistrationCallback` 仍存在）
- 其它：`getEntityPos()` → `position()`、`getBlockPos()` → `blockPosition()`、`isSubmergedInWater()` → `isUnderWater()`、
  `getEntitiesByClass` → `getEntitiesOfClass`、`AI 目标`、`streamTags()` → `tags()` 等

### 4. 顺手修的两个健壮性问题

- `resourcepacks` 文件夹不存在时（全新实例）会刷 ERROR 日志 → 现在会自动创建
- 生物群系标签比较原来用 `==`（依赖 `Identifier` 被 intern），26.x 下不可靠 → 改成 `.equals()`
  并对可能为 null 的标签做保护

## 已验证

- ✅ 26.3 客户端能加载（Fabric Loader 0.20.1 + Fabric API 0.166.2 + YACL 3.9.7）
- ✅ 三个 mixin 全部注入成功（`Minecraft.tick`、`MusicManager.tick`、`SoundManager.play`、BossBar Accessor）
- ✅ 主菜单事件触发并成功播放 MP3（`ReactiveMusic Player Thread` 日志）
- ✅ 配置读写（YACL）正常，`/reactivemusic` 命令注册成功

未覆盖：自动化环境里没法自动进入存档，所以“进世界后的生物群系/怪物/天气事件判定”只做了代码级核对，
建议你第一次进游戏时留意一下（尤其不同维度、村庄/怪物附近、下雨天）。

## 工程结构

```
ReactiveMusic/                   ← 本移植工程（git 仓库根目录）
├─ build.gradle                  ← Loom 无重映射 + 依赖
├─ gradle.properties             ← 26.3 / loader / fabric api / yacl / modmenu
├─ src/main/…                    ← 已移植的源码与资源
│  ├─ java/circuitlord/…         ← 模组源码
│  └─ resources/                 ← fabric.mod.json / mixins / lang / 音乐包
├─ docs/                         ← 音乐包制作指南 + 模板
├─ dist/                         ← 成品 jar + YACL（不入库）
├─ vendor/                       ← 上游 1.3.5 官方 jar（音频来源，不入库）
└─ _history/                     ← 上游源码快照 / 1.21.9 迁移工程（不入库）
```

构建工具链放在上一级目录：`../_toolchain`（Gradle 发行版、信任库、辅助脚本），
构建缓存放在 `../.gradle-home`。

### 重新构建

```powershell
cd D:\work\AI_work\minecraft\ReactiveMusic
$env:JAVA_HOME='C:\Users\35837\AppData\Roaming\.minecraft\runtime\java-runtime-epsilon'
$env:GRADLE_USER_HOME='D:\work\AI_work\minecraft\.gradle-home'
$env:GRADLE_OPTS='-Djavax.net.ssl.trustStore=D:\work\AI_work\minecraft\_toolchain\cacerts -Djavax.net.ssl.trustStorePassword=changeit'
D:\work\AI_work\minecraft\_toolchain\gradle-9.8.2\bin\gradle.bat build
```

> 说明：本机构建需要放宽 DSH 沙箱，否则 Java 会把工作区文件判定为只读（`Files.isWritable=false`），
> Loom 的 zipfs 便以只读方式打开 jar，导致合并/打包失败。
> 另外本机 TLS 被 SteamTools 代理中间人拦截，`_toolchain/cacerts` 是加了其根证书的信任库。

## 版权

原模组与音乐素材版权归 CircuitLord 所有，许可证 GPLv3（见 `LICENSE`）。此移植仅为个人使用。
