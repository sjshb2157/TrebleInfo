# Treble Info

检测设备对 Project Treble 的支持情况，并据此判断应当刷入哪一类通用系统映像（GSI）。

应用会读取设备的 VINTF 清单、兼容性矩阵与系统属性，报告以下信息：

- Project Treble 支持状态与 VNDK 版本
- 链接器命名空间隔离（VNDK 是否处于 Lite 模式）
- 清单位置（现代 / 旧式）
- System-as-Root、无缝系统更新（A/B）、动态分区
- CPU 架构与 Binder 架构

外观可在应用内调整：主题模式（跟随系统 / 浅色 / 深色）、是否使用动态取色、
深色模式下是否使用纯黑背景。设置保存在 `SharedPreferences` 中。

## 关于本仓库

这是 [gitlab.com/TrebleInfo/TrebleInfo](https://gitlab.com/TrebleInfo/TrebleInfo) 的一个 fork，
原作者为 Hackintosh Five（penn5）。

与上游的主要差异：

**构建体系**

- 构建工具链升级到 AGP 9 / Gradle 9 / Kotlin 2.2（AGP 内置编译器）
- compileSdk 与 targetSdk 提升到 37，minSdk 提升到 26
- 依赖迁移到 `gradle/libs.versions.toml` 版本目录
- 原本作为 git 子模块引用的构建插件已内置到 `build-logic/`
- 原生层以 C++23 编译（`android-17` 的 libbase 需要 `std::string::resize_and_overwrite`）
- GitHub Actions 流水线：构建、lint、单元测试、16 KB 页对齐校验，以及在模拟器上运行的
  仪器化测试
- 上游的 fastlane 发布流水线与 PoEditor 翻译工具链已移除，它们依赖只有上游持有的凭据
- 移除了 `nonfree` 产品风味、Google Play 结算依赖与捐赠入口
- 仅保留默认（英文）、简体中文与繁体中文，其余语言通过 `localeFilters` 从依赖库中剔除

**AOSP 数据**

- `app/src/main/cpp/` 下 7 个 AOSP 子模块指向 `android-17.0.0_r1`
- 打包 13 份框架兼容性矩阵：`legacy`、`1`–`8`、`202404`、`202504`、`202604`、`202704`。
  每份取自 AOSP 中最后一个仍然可用的版本，文件首行记录来源 commit

**检测逻辑**

- 兼容性矩阵改为按设备声明的 shipping FCM version 精确选取，而不是遍历全部矩阵取第一个匹配的。
  这是 [AOSP 匹配规则](https://source.android.com/docs/core/architecture/vintf/match-rules)
  的要求；没有声明 `target-level` 的设备落到 `legacy` 矩阵
- 修正了 sepolicy 版本比对：Android 15 起厂商清单使用 `202504` 这样不带次版本号的写法，
  而 libvintf 把次版本建模为 `optional` 并将其排在 `0` 之前，导致注入的区间匹配不上

> **关于 HAL 检查**：AOSP 在
> [`c2de8e5`](https://android.googlesource.com/platform/system/libvintf/+/c2de8e5c3d9a91a2657fc6381e504e62fa45a6e2)
> 中移除了兼容性矩阵的 `optional` 属性及其背后的 HAL 必需性检查 —— 所有 HAL 一律视为可选，
> 相关要求转由 VTS 与 VSR 承担。因此本应用不再能识别"声称支持 Treble 但 HAL 不合规"的设备，
> 这是上游行为变更的结果，没有可用的替代 API。

## 构建

需要 JDK 21 与 Android SDK。

```
git clone --recurse-submodules https://github.com/sjshb57/TrebleInfo.git
cd TrebleInfo
./gradlew assembleDebug
```

如果已经克隆过但没有拉子模块：

```
git submodule update --init --recursive --force
```

首次构建会自动下载 NDK 与 CMake，耗时较长。原生层会把 libvintf、libbase、system_core、
fmtlib、tinyxml2 一并编译进 `libtrebledetector.so`，这是 APK 中最大的一块。

### 常用任务

```
./gradlew assembleDebug        # 日常开发
./gradlew assembleRelease      # 走 R8 与资源压缩
./gradlew testDebugUnitTest    # 单元测试
```

`app/build.gradle.kts` 中 `abiFilters` 只保留了 `arm64-v8a`。如需其他架构，删除该配置即可。

仪器化测试需要目标架构与设备一致。在 x86_64 模拟器上运行时用
`-PemulatorAbi=x86_64` 让 debug 变体按该架构构建：

```
./gradlew connectedDebugAndroidTest -PemulatorAbi=x86_64
```

### 签名

在 `app/signing.properties` 中提供以下键即可对 release 包签名，该文件不纳入版本控制：

```
storeFile=...
storePassword=...
keyAlias=...
keyPassword=...
```

不提供时 release 变体会构建出未签名的 APK。

## 测试

- **单元测试**（`app/src/test`）：纯 JVM，覆盖清单解析、挂载点解析、版本解析等
- **仪器化测试**（`app/src/androidTest`）：需要设备或模拟器，验证打包的兼容性矩阵能被
  libvintf 正确解析并按等级选中
- **测试数据**（`app/src/sharedTest/resources`）：从真实设备上抓取的 VINTF 快照，
  `app/src/sharedTest/pull.sh` 是抓取脚本

## 翻译

上游通过 PoEditor 管理翻译，本 fork 无法使用该项目，因此改为直接维护资源文件。
在 `app/src/main/res/values-<语言>/strings.xml` 中增改条目即可，以
`app/src/main/res/values/strings.xml` 为基准。

新增语言时需要同步修改 `app/build.gradle.kts` 中的 `localeFilters`，否则该语言的资源
会在打包时被剔除。

lint 的 `MissingTranslation` 检查处于开启状态，缺失的条目会在构建日志中列出。

## 许可证

GPL-3.0-or-later，与上游一致。完整条款见 [LICENSE](LICENSE)。

`build-logic/materialdesignicons-android` 源自 penn5 的
[materialdesignicons-android](https://github.com/penn5/materialdesignicons-android)，
目录中的 `.upstream-commit` 记录了导入时的版本。

`app/src/main/res/drawable/` 下的图标来自 Material Design Icons（Apache-2.0），
由 `build-logic` 中的插件生成。

`app/src/main/cpp/` 下的子模块与 `app/src/main/resources/` 下的兼容性矩阵为 AOSP 代码与数据，
遵循各自的许可证。

