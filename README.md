# Treble Info

检测设备对 Project Treble 的支持情况，并据此判断应当刷入哪一类通用系统映像（GSI）。

应用会读取设备的 VINTF 清单、兼容性矩阵与系统属性，报告以下信息：

- Project Treble 支持状态与 VNDK 版本
- 链接器命名空间隔离（VNDK 是否处于 Lite 模式）
- 清单位置（现代 / 旧式）
- System-as-Root、无缝系统更新（A/B）、动态分区
- CPU 架构与 Binder 架构

## 关于本仓库

这是 [gitlab.com/TrebleInfo/TrebleInfo](https://gitlab.com/TrebleInfo/TrebleInfo) 的一个 fork，
原作者为 Hackintosh Five（penn5）。本 fork 的工作集中在构建体系的现代化，应用本身的功能逻辑
基本保持原样。

与上游的主要差异：

- 构建工具链升级到 AGP 9 / Gradle 9 / Kotlin 2.2（AGP 内置编译器）
- compileSdk 与 targetSdk 提升到 37，minSdk 提升到 26
- 依赖迁移到 `gradle/libs.versions.toml` 版本目录
- 原本作为 git 子模块引用的两个构建插件已内置到 `build-logic/`
- 7 个 AOSP 原生子模块更新到上游最新
- 补充了 FCM level 8 / 202404 / 202504 / 202604 的兼容性矩阵
- 新增 GitHub Actions 构建流水线（含 Windows 构建与 16 KB 页对齐校验）
- 上游的 fastlane 发布流水线与 PoEditor 翻译工具链已移除，它们依赖只有上游持有的凭据

## 构建

需要 JDK 21 与 Android SDK。

```
git clone --recurse-submodules https://github.com/sjshb2157/TrebleInfo.git
cd TrebleInfo
./gradlew assembleFreeDebug
```

如果已经克隆过但没有拉子模块：

```
git submodule update --init --recursive --force
```

> **`--force` 不能省。** 原生代码依赖 7 个 AOSP 子模块，如果本地工作区停留在旧的
> commit，之后任何一次 `git add -A`（包括 Android Studio 的「提交全部改动」）都会
> 把仓库里的子模块指针改回旧版本。

首次构建会自动下载 NDK 与 CMake，耗时较长。

### 构建变体

存在 `free` 与 `nonfree` 两个 flavor，区别仅在于捐赠方式：`free` 跳转 PayPal 网页，
`nonfree` 使用 Google Play 结算。

```
./gradlew assembleFreeDebug        # 日常开发
./gradlew assembleFreeRelease      # 走 R8 与资源压缩
./gradlew testFreeDebugUnitTest    # 单元测试
```

`app/build.gradle.kts` 中 `abiFilters` 只保留了 `arm64-v8a`。如需其他架构，删除该配置即可。

### 签名

在 `app/signing.properties` 中提供以下键即可对 release 包签名，该文件不纳入版本控制：

```
storeFile=...
storePassword=...
keyAlias=...
keyPassword=...
```

不提供时 release 变体会构建出未签名的 APK。

## 翻译

上游通过 PoEditor 管理翻译，本 fork 无法使用该项目，因此改为直接维护资源文件。
在 `app/src/main/res/values-<语言>/strings.xml` 中增改条目即可，以
`app/src/main/res/values/strings.xml` 为基准。

lint 的 `MissingTranslation` 检查处于开启状态，缺失的条目会在构建日志中列出。

## 许可证

GPL-3.0-or-later，与上游一致。完整条款见 [LICENSE](LICENSE)。

`build-logic/` 下的两个 Gradle 插件源自 penn5 的
[poeditor-android](https://github.com/penn5/poeditor-android) 与
[materialdesignicons-android](https://github.com/penn5/materialdesignicons-android)，
各目录中的 `.upstream-commit` 记录了导入时的版本。

`app/src/main/cpp/` 下的子模块为 AOSP 代码，遵循各自的许可证。
