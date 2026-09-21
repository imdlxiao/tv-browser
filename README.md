# 自在浏览 · TV Browser

面向 Android 电视的轻量浏览器。Kotlin + Jetpack Compose + 系统 WebView，
使用遥控器方向键、确认键和返回键完成搜索与网页浏览。

## 当前功能

- 深色首页、三列网站卡片、焦点缩放与高亮。
- 网址直达与百度关键词搜索，预置七个网站；首位固定“回忆录”（http://Abyss.local:8765/）。
- 网页标题、地址、加载进度、刷新、网页历史后退和返回首页。
- 网络错误重试，阻止不受支持的外部应用协议，不绕过证书错误。
- TV 桌面入口、原创应用图标与横幅。
- 页面重建时恢复导航及 WebView 会话；返回首页恢复书签焦点。

## 操作

| 按键 | 行为 |
| --- | --- |
| 方向键 | 移动首页/工具栏焦点，网页内按网站能力导航和滚动 |
| 确认键 | 打开卡片、按钮或搜索输入框 |
| 工具栏“浏览网页” / 向下 | 将焦点移入网页 |
| 菜单键 / 网页顶部向上 | 将焦点移回工具栏 |
| 返回键 | 网页优先后退，无历史时回首页；首页显示退出确认 |

网页对方向键的支持取决于网站及电视系统 WebView；复杂网站和视频播放需目标电视验证。

## 架构

单 Activity，首页与浏览页两个 Compose 页面。使用轻量 MVVM 和单向数据流，
先保留一个 app 模块：

- `ui/home/HomeViewModel`：搜索、书签展示状态和可恢复导航状态。
- `data/BookmarkRepository`：书签数据接口；SharedPreferences + JSON 本地实现。
- `browser/AddressResolver`：独立、可测试的网址和搜索策略。
- `browser/BrowserSession`：界面生命周期内的 WebView 控制器，负责历史、回调和释放。
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
