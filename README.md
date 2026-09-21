# 自在浏览 · TV Browser

面向 Android 电视的轻量浏览器。Kotlin + Jetpack Compose + 系统 WebView，
使用遥控器方向键、确认键和返回键完成搜索与网页浏览。

## 当前功能

- 深色首页、三列网站卡片、焦点缩放与高亮。
- 网址直达与百度关键词搜索，预置七个网站；首位固定“回忆录”（http://Abyss.local:8765/）。
- 网页标题、地址、加载进度、刷新、网页历史后退和返回首页。
- 真实 WebView 错误码、失败地址、Android/内核版本、DNS/TCP 连接诊断；区分名称解析、连接拒绝、超时、HTTP 与证书错误。
- 工具栏和错误页可修改访问地址，选择同时保存首页收藏；修正后的回忆录地址在重启后保留。
- 原生全屏容器及返回退出；阻止不受支持的外部应用协议，不绕过证书错误。
- TV 桌面入口、原创应用图标与横幅。
- 页面重建时恢复导航及 WebView 会话；返回首页恢复书签焦点。

## 操作

| 按键 | 行为 |
| --- | --- |
| 方向键 | 空间导航首页、错误页、网页控件；输入框上下换项、左右移动光标 |
| 确认键 | 打开卡片、按钮或搜索输入框 |
| 工具栏“浏览网页” / 向下 | 将焦点移入网页 |
| 菜单键 / 网页顶部无上方控件时向上 | 将焦点移回工具栏 |
| 返回键 | 优先退出全屏、关闭网页弹窗，再网页历史后退；首页显示退出确认 |
| 播放、暂停、快进、快退 | 控制当前页面的视频；memoir-tv 前后跳转 10 秒 |

网页注入 ES5 导航脚本，对普通可聚焦 HTML 控件提供空间导航；memoir-tv 通过 `window.MemoirTV.handleKey` 与浏览器协作，避免网页和原生层重复处理同一次方向键。没有开放 Android JavaScript 特权接口。跨域 iframe、封闭 Shadow DOM、特殊网页播放器和厂商输入法仍需目标环境验证。

## 回忆录连接问题

MuMu Android 12 / WebView 110 实测 `abyss.local` 返回 `ERROR_HOST_LOOKUP (-2) / net::ERR_NAME_NOT_RESOLVED`，同一电脑的 IP 地址返回 HTTP 200。公共网站正常与局域网 mDNS 可用是两回事；这不是服务器 HTTP 明文被禁止，应用已允许局域网 HTTP。

错误页可「连接诊断」查看 DNS 和端口结果，然后用「修改地址」填写 `http://素材电脑IP:8765/`。勾选更新收藏后，下次从首页直接进入。要长期固定 IP，需要路由器 DHCP 地址保留；不自动扫描局域网、不硬编码用户电脑 IP，也不修改路由器。目标电视的根因以该设备显示的错误码为准。

## 架构

单 Activity，首页与浏览页两个 Compose 页面。使用轻量 MVVM 和单向数据流，
先保留一个 app 模块：

- `ui/home/HomeViewModel`：搜索、书签展示状态和可恢复导航状态。
- `data/BookmarkRepository`：书签数据接口；SharedPreferences + JSON 本地实现。
- `browser/AddressResolver`：独立、可测试的网址和搜索策略。
- `browser/BrowserSession`：界面生命周期内的 WebView 控制器，负责历史、回调和释放。
- `browser/NavigationFailure`、`NetworkDiagnostics`：错误解释、脱敏地址和有超时限制的 DNS/TCP 诊断。
- `assets/remote-navigation.js`：网页控件导航与 memoir-tv 协作；不接触原片或用户凭证。
- `ui/browser/BrowserErrorPanel`、`AddressDialog`：错误页焦点、地址编辑和收藏保存。
- `ui/components`、`ui/theme`：可复用电视控件与视觉规范。
- `MainActivity`：窗口配置与依赖组装。

WebView、焦点和键盘属于界面层，不放入 ViewModel。网页回调只更新界面状态，
不会在 Compose 重组中再次调用 loadUrl。

## 构建与验证

JDK 17，Android SDK 36；minSdk 24，targetSdk 35。当前发布目标为 armeabi-v7a 电视。
依赖版本与 Kotlin 2.0 编译器保持兼容，暂不追随最新版本。

```shell
./gradlew assembleDebug testDebugUnitTest lintDebug
./gradlew connectedDebugAndroidTest
./gradlew assembleRelease
```

Windows 使用 `gradlew.bat`。仪器测试需要可用的 Android 模拟器或设备，
覆盖遥控器焦点、输入验证、真实 WebView 历史导航及 Activity 重建。
网页测试使用设备内的临时 HTTP 服务，不依赖外部网站。

本轮在 Android 8 / WebView 69 的模拟器完成真实按键、错误页、收藏保存、历史和重建测试；在 MuMu Android 12 / WebView 110 完成 memoir-tv 登录、播放暂停、前后跳转、全屏和返回联调。模拟器结果不代替目标电视验收。

跨项目测试需要在 memoir-tv 仓库启动 `python tests/tv_fixture.py --port 18765`，再对测试设备执行 `adb reverse tcp:18765 tcp:18765`：

```shell
./gradlew -PtestAbi=x86_64 -Pandroid.testInstrumentationRunnerArguments.memoirUrl=http://127.0.0.1:18765/ connectedDebugAndroidTest
```

`testAbi` 仅用于模拟器；正式构建省略该参数，保持 armeabi-v7a。指定 `ANDROID_SERIAL` 可避免误操作其他连接设备。联调用临时合成素材，账号只在该临时库中创建。Android 8 手机型模拟器的首次沉浸式引导需要先确认，再开始遥控自动化；电视系统不一定存在这个提示。

Debug APK：`app/build/outputs/apk/debug/app-debug.apk`。

Release 开启 R8 与资源压缩。正式签名从本地 `keystore.properties` 读取：

```properties
storeFile=/absolute/path/to/release.jks
storePassword=YOUR_LOCAL_PASSWORD
keyAlias=YOUR_KEY_ALIAS
keyPassword=YOUR_LOCAL_PASSWORD
```

未提供本地签名配置时只生成未签名 release APK，不自动创建或提交签名密钥。

## 开发约定

- 日常开发分支为 `develop`，`master` 保留稳定基线。
- 按完整职责提交，使用 `feat: 中文描述` 或 `refactor: 中文描述`。
- 新增代码文件包含 `Author: imdlxiao` 作者头。
- `doc/`、`agent/` 用于本地文档和自动化，已被 Git 忽略。
- 外部位图统一放到 `app/src/main/res/drawable-xhdpi/`，使用前记录来源和许可。
- 当前图标和焦点动画由项目自行绘制，无外部图标包或 Lottie 依赖。

MVP 暂不提供多标签、下载、广告拦截、账户同步、历史记录列表和编辑收藏。
应用商店发布还需要正式签名、隐私政策以及目标电视的安装与遥控器验收。

## 许可

[MIT](LICENSE)
