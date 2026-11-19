<#
.SYNOPSIS
    在 DSH 沙箱之外构建 Reactive Music（Minecraft 26.3 / Fabric）。

.DESCRIPTION
    为什么需要这个脚本：DSH 的 workspace-write 沙箱会让 Java 把工作区文件判定为只读，
    Loom 的 zipfs 因此无法写 jar，构建一定失败。把构建放到普通 PowerShell 里跑就没有这个问题，
    也不需要任何权限提权。

    脚本会：
      1. 自动定位 JDK 25（优先 $env:JAVA_HOME，其次 Minecraft 启动器自带的 runtime）
      2. 定位 Gradle（优先本机 _toolchain 里的发行版，其次仓库自带的 gradlew）
      3. 执行 gradle build
      4. 把产物复制到 dist\
      5. 可选：安装到 26.3 实例的 mods 目录（-Install）

.EXAMPLE
    pwsh -File build.ps1
    只构建，产物在 dist\

.EXAMPLE
    pwsh -File build.ps1 -Install
    构建并安装到自动探测到的 mods 目录

.EXAMPLE
    pwsh -File build.ps1 -Install -ModsDir "D:\some\instance\mods"
    指定 mods 目录
#>
[CmdletBinding()]
param(
    [string]$ModsDir,
    [switch]$Install,
    [switch]$Pause
)

$ErrorActionPreference = 'Continue'
$root = $PSScriptRoot
$toolchain = Join-Path (Split-Path $root) '_toolchain'

function Write-Step([string]$msg) {
    Write-Host ""
    Write-Host "== $msg" -ForegroundColor Cyan
}

function Fail([string]$msg) {
    Write-Host ""
    Write-Host "构建失败：$msg" -ForegroundColor Red
    if ($env:DSH_SHELL -eq '1') {
        Write-Host "提示：当前看起来是在 DSH 沙箱里运行。请改在普通 PowerShell / 双击 build.cmd 运行。" -ForegroundColor Yellow
    }
    if ($Pause) { Read-Host "按回车退出" }
    exit 1
}

# ---------------------------------------------------------------- JDK
Write-Step "定位 JDK 25"

$javaCandidates = @()
if ($env:JAVA_HOME) { $javaCandidates += (Join-Path $env:JAVA_HOME 'bin\java.exe') }
if ($env:APPDATA) {
    $javaCandidates += (Join-Path $env:APPDATA '.minecraft\runtime\java-runtime-epsilon\bin\java.exe')
    $javaCandidates += (Join-Path $env:APPDATA '.minecraft\runtime\java-runtime-delta\bin\java.exe')
}

$java = $javaCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
if (-not $java) {
    $cmd = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($cmd) { $java = $cmd.Source }
}
if (-not $java) { Fail "找不到 java.exe。请安装 JDK 25 并设置 JAVA_HOME。" }

$versionLine = (& $java -version 2>&1 | Select-Object -First 1).ToString()
$major = 0
if ($versionLine -match '"(\d+)') { $major = [int]$Matches[1] }
if ($major -lt 25) {
    Fail "当前 Java 版本是 $major（$java），Minecraft 26.3 需要 JDK 25 或更高。"
}

$env:JAVA_HOME = Split-Path (Split-Path $java)
Write-Host "  $java (Java $major)"

# ---------------------------------------------------------------- Gradle
Write-Step "定位 Gradle"

$gradle = $null
$toolchainGradle = Join-Path $toolchain 'gradle-9.8.2\bin\gradle.bat'
$wrapperGradle = Join-Path $root 'gradlew.bat'

if (Test-Path -LiteralPath $toolchainGradle) {
    $gradle = $toolchainGradle
} elseif (Test-Path -LiteralPath $wrapperGradle) {
    $gradle = $wrapperGradle
} else {
    Fail "既没有 ../_toolchain/gradle-9.8.2，也没有 gradlew.bat。"
}
Write-Host "  $gradle"

# 构建缓存：优先用本机共享的 .gradle-home，省去重复下载
$toolHome = Join-Path (Split-Path $root) '.gradle-home'
if (-not $env:GRADLE_USER_HOME -and (Test-Path -LiteralPath $toolHome)) {
    $env:GRADLE_USER_HOME = $toolHome
    Write-Host "  GRADLE_USER_HOME=$toolHome"
}

# 本机 TLS 被中间人代理拦截，需要额外信任库；没有就跳过
$cacerts = Join-Path $toolchain 'cacerts'
if ((Test-Path -LiteralPath $cacerts) -and -not $env:GRADLE_OPTS) {
    $env:GRADLE_OPTS = "-Djavax.net.ssl.trustStore=$cacerts -Djavax.net.ssl.trustStorePassword=changeit -Djava.net.preferIPv4Stack=true"
}

# ---------------------------------------------------------------- Build
Write-Step "开始构建（首次会下载依赖，可能较慢）"

Push-Location $root
try {
    & $gradle --console=plain --no-daemon build
    $code = $LASTEXITCODE
} finally {
    Pop-Location
}

if ($code -ne 0) { Fail "Gradle 返回 $code" }

$libs = Join-Path $root 'build\libs'
$jar = Get-ChildItem -LiteralPath $libs -Filter 'reactivemusic-*.jar' -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $jar) { Fail "构建成功但没找到 build\libs\reactivemusic-*.jar" }

# ---------------------------------------------------------------- Publish
Write-Step "复制产物到 dist"

$dist = Join-Path $root 'dist'
New-Item -ItemType Directory -Force -Path $dist -ErrorAction Stop | Out-Null
Copy-Item -LiteralPath $jar.FullName -Destination $dist -Force -ErrorAction Stop
Write-Host "  -> $(Join-Path $dist $jar.Name)"

# ---------------------------------------------------------------- Install
if ($Install) {
    Write-Step "安装到 mods"

    if (-not $ModsDir) {
        $candidates = @(
            'D:\Game\Minecraft\.minecraft\versions\26.3-Fabric 0.20.1\mods',
            (Join-Path $env:APPDATA '.minecraft\mods')
        )
        $ModsDir = $candidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
    }

    if (-not $ModsDir -or -not (Test-Path -LiteralPath $ModsDir)) {
        Write-Host "  找不到 mods 目录，跳过安装（可用 -ModsDir 指定）" -ForegroundColor Yellow
    } else {
        # 清掉旧版本，避免同时加载两个
        Get-ChildItem -LiteralPath $ModsDir -Filter 'reactivemusic-*.jar' | Remove-Item -Force -ErrorAction Stop
        Copy-Item -LiteralPath $jar.FullName -Destination $ModsDir -Force -ErrorAction Stop
        Write-Host "  -> $(Join-Path $ModsDir $jar.Name)"

        $yacl = Get-ChildItem -LiteralPath $dist -Filter 'yet_another_config_lib*.jar' -ErrorAction SilentlyContinue |
            Select-Object -First 1
        if ($yacl -and -not (Test-Path -LiteralPath (Join-Path $ModsDir $yacl.Name))) {
            Copy-Item -LiteralPath $yacl.FullName -Destination $ModsDir -Force -ErrorAction Stop
            Write-Host "  -> $(Join-Path $ModsDir $yacl.Name)"
        }
    }
}

# ---------------------------------------------------------------- Done
$sizeMb = [math]::Round($jar.Length / 1MB, 1)
$hash = (Get-FileHash -LiteralPath $jar.FullName -Algorithm SHA256).Hash.Substring(0, 16)
Write-Host ""
Write-Host "完成：$($jar.Name)（$sizeMb MB，SHA256 $hash…）" -ForegroundColor Green
if (-not $Install) { Write-Host "如需直接安装到游戏，加 -Install（或双击 build.cmd）。" }
Write-Host ""

if ($Pause) { Read-Host "按回车退出" }
