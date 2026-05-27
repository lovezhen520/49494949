# WebIDE - Android Web Development IDE

一个功能完整的Android原生Web开发IDE应用，使用Kotlin和Jetpack Compose构建。

## 功能特性

### 1. 项目管理
- 创建新项目
- 打开现有项目
- 删除项目
- 项目列表展示

### 2. 代码编辑器
- 语法高亮（HTML、CSS、JavaScript、JSON）
- 多文件管理
- 文件树导航
- 实时保存

### 3. 实时预览
- WebView预览
- 控制台日志显示
- 热更新支持

### 4. 原生API集成
- 设备信息获取
- 震动功能
- Toast通知
- 位置服务
- 文件系统访问

### 5. 跨域解决方案
- 内置CORS处理
- 本地文件服务

### 6. 导出功能
- 项目导出为ZIP文件
- 分享功能

## 技术栈

- **语言**: Kotlin
- **UI框架**: Jetpack Compose (Material Design 3)
- **架构**: MVVM
- **数据库**: Room
- **异步处理**: Kotlin Coroutines + Flow
- **最低SDK**: API 26 (Android 8.0)
- **目标SDK**: API 34 (Android 14)

## 项目结构

```
app/
├── src/main/java/com/webide/app/
│   ├── MainActivity.kt                 # 主Activity
│   ├── data/database/                  # 数据库层
│   │   ├── AppDatabase.kt
│   │   ├── ProjectDao.kt
│   │   ├── FileDao.kt
│   │   └── Converters.kt
│   ├── domain/
│   │   ├── model/                      # 数据模型
│   │   │   ├── Project.kt
│   │   │   └── ProjectFile.kt
│   │   └── repository/                 # 仓储层
│   │       └── ProjectRepository.kt
│   └── ui/
│       ├── components/                 # UI组件
│       │   ├── CodeEditor.kt
│       │   └── WebViewWithBridge.kt
│       └── screens/                    # 页面
│           ├── HomeScreen.kt
│           ├── EditorScreen.kt
│           └── PreviewScreen.kt
└── src/main/res/
    ├── values/                         # 资源文件
    └── xml/                            # XML配置
```

## 快速开始

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK API 34

### 构建步骤

#### 方法一：使用 Android Studio 构建

1. 克隆或下载项目
2. 使用Android Studio打开项目
3. 同步Gradle依赖
4. 连接Android设备或启动模拟器
5. 点击运行按钮构建并安装应用

#### 方法二：使用命令行构建

```bash
# 克隆项目
git clone <repository-url>
cd WebIDE

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease

# 构建 App Bundle
./gradlew bundleRelease
```

#### 方法三：使用 GitHub Actions 自动构建

1. 将代码推送到 GitHub 仓库
2. GitHub Actions 会自动触发构建
3. 在 Actions 页面查看构建进度
4. 下载构建好的 APK 文件

### 使用说明

1. **创建项目**: 点击首页右下角的"+"按钮，输入项目名称创建新项目
2. **编辑代码**: 在编辑器中修改HTML、CSS、JavaScript文件
3. **预览**: 点击顶部的"运行"按钮查看实时预览
4. **导出**: 点击顶部的"导出"按钮将项目导出为ZIP文件
5. **查看日志**: 在预览页面可以查看控制台日志

## JavaScript API

应用提供了以下原生API供网页调用：

```javascript
// 日志
AndroidAPI.log('Hello from JS');

// 震动
AndroidAPI.vibrate(100); // 震动100毫秒

// 获取设备信息
const deviceInfo = AndroidAPI.getDeviceInfo();
console.log(deviceInfo);

// 显示Toast
AndroidAPI.showToast('Hello World!');

// 获取位置
AndroidAPI.getLocation((location) => {
    console.log('Lat:', location.latitude);
    console.log('Lng:', location.longitude);
});
```

## GitHub Actions 自动构建

项目已配置 GitHub Actions 自动构建工作流，每次推送代码或创建 Pull Request 时会自动构建。

### 触发方式

- **Push**: 推送到 main 或 master 分支时自动构建
- **Pull Request**: 提交 PR 时自动构建
- **手动触发**: 在 Actions 页面手动触发构建

### 构建产物

每次构建成功后会自动上传以下文件：

- `app-debug.apk` - Debug 版本 APK
- `app-release-unsigned.apk` - Release 版本 APK（未签名）
- `app-debug.aab` - Debug 版本 App Bundle
- `app-release.aab` - Release 版本 App Bundle

### 下载构建产物

1. 打开 GitHub 仓库的 Actions 页面
2. 点击最新的构建记录
3. 滚动到 "Artifacts" 区域
4. 点击需要的文件下载

## 上传到 GitHub

### 首次上传步骤

```bash
# 初始化 Git 仓库
git init
git add .
git commit -m "Initial commit"

# 添加远程仓库
git remote add origin https://github.com/<你的用户名>/WebIDE.git

# 推送代码
git branch -M main
git push -u origin main
```

### 后续更新

```bash
git add .
git commit -m "Update changes"
git push
```

## Termux 环境构建

项目针对 Termux 环境进行了优化，如果在 Termux 中构建：

```bash
# 使用系统 Gradle
gradle :app:assembleDebug --no-daemon

# 或使用项目 Gradle Wrapper
./gradlew :app:assembleDebug --no-daemon
```

## 许可

MIT License
