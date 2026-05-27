# GitHub 上传与自动构建教程

本教程将指导你如何将 WebIDE 项目上传到 GitHub 并配置自动构建。

---

## 📋 目录

1. [准备工作](#准备工作)
2. [在 GitHub 创建仓库](#在-github-创建仓库)
3. [上传代码到 GitHub](#上传代码到-github)
4. [启用 GitHub Actions](#启用-github-actions)
5. [查看构建状态](#查看构建状态)
6. [下载构建产物](#下载构建产物)
7. [常见问题](#常见问题)

---

## 🛠️ 准备工作

### 1. 解压项目

首先解压刚才的 `WebIDE-Android.zip` 文件：

```bash
# 解压项目
unzip WebIDE-Android.zip -d WebIDE
cd WebIDE
```

### 2. 安装 Git

确保你的电脑已安装 Git：

```bash
# 检查 Git 版本
git --version
```

如果没有安装，请先安装：
- **Windows/macOS**: 下载 https://git-scm.com/downloads
- **Linux (Debian/Ubuntu)**: `sudo apt-get install git`

### 3. 注册 GitHub 账号

如果还没有 GitHub 账号，访问 https://github.com 注册一个。

---

## 🏗️ 在 GitHub 创建仓库

### 步骤 1: 登录 GitHub

访问 https://github.com 并登录。

### 步骤 2: 创建新仓库

1. 点击右上角的 "+" 按钮
2. 选择 "New repository"

### 步骤 3: 配置仓库

填写以下信息：

| 配置项 | 说明 | 示例值 |
|--------|------|--------|
| Repository name | 仓库名称 | WebIDE |
| Description (可选) | 仓库描述 | Android Web Development IDE |
| Public/Private | 仓库类型 | Public (推荐) |
| Initialize this repository with | 初始化选项 | **不要勾选任何选项** |

⚠️ **重要**：不要勾选 "Add a README file"、"Add .gitignore" 或 "Choose a license"，因为我们的项目里已经有了。

### 步骤 4: 创建仓库

点击 "Create repository" 按钮。

---

## 📤 上传代码到 GitHub

### 方法一：使用命令行（推荐）

在项目目录中执行以下命令：

```bash
# 1. 初始化 Git 仓库
git init

# 2. 添加所有文件
git add .

# 3. 创建第一次提交
git commit -m "Initial commit"

# 4. 设置远程仓库地址（替换为你的用户名）
git remote add origin https://github.com/<你的用户名>/WebIDE.git

# 5. 重命名分支为 main
git branch -M main

# 6. 推送到 GitHub
git push -u origin main
```

> **提示**：将 `<你的用户名>` 替换为你实际的 GitHub 用户名。

### 方法二：使用 GitHub Desktop

如果你更喜欢图形界面：

1. 下载并安装 GitHub Desktop: https://desktop.github.com
2. 打开 GitHub Desktop
3. 选择 "File" → "Add Local Repository"
4. 选择你的项目目录
5. 点击 "Publish repository"
6. 填写仓库信息并发布

---

## 🔧 启用 GitHub Actions

GitHub Actions 通常默认启用，但如果需要确认：

### 步骤 1: 进入仓库设置

1. 打开你的 GitHub 仓库页面
2. 点击顶部的 "Settings" 标签
3. 在左侧菜单中找到 "Actions" → "General"

### 步骤 2: 配置 Actions 权限

确保以下选项已启用：

- ✅ "Allow all actions and reusable workflows"
- ✅ "Read and write permissions" (在 Workflow permissions 中)

### 步骤 3: 查看工作流文件

项目中已经包含了工作流配置文件：
[.github/workflows/build.yml](file:///workspace/.github/workflows/build.yml)

这个文件会告诉 GitHub 如何自动构建项目。

---

## 👀 查看构建状态

### 方法一：通过 Actions 页面查看

1. 打开你的 GitHub 仓库
2. 点击顶部的 "Actions" 标签
3. 你会看到所有的构建记录

### 方法二：通过提交状态查看

每次提交后，GitHub 会在提交旁边显示一个状态图标：

| 图标 | 状态 | 说明 |
|------|------|------|
| 🟡 | 构建中 | GitHub Actions 正在构建 |
| ✅ | 构建成功 | 构建完成，产物已生成 |
| ❌ | 构建失败 | 构建出错，需要检查日志 |

### 查看构建日志

1. 点击 Actions 标签
2. 点击某个构建记录
3. 在左侧选择一个任务（如 "build"）
4. 查看详细的构建日志

---

## 📥 下载构建产物

构建成功后，你可以下载生成的 APK 文件：

### 步骤 1: 打开构建记录

1. 进入 Actions 页面
2. 点击最新的构建记录（状态为 ✅ 的）

### 步骤 2: 找到 Artifacts 区域

1. 滚动到页面底部
2. 找到 "Artifacts" 区域

### 步骤 3: 下载文件

你会看到以下可下载的文件：

| 文件名 | 说明 |
|--------|------|
| `app-debug` | Debug 版本 APK（适合测试） |
| `app-release` | Release 版本 APK（未签名） |
| `app-debug-bundle` | Debug 版本 App Bundle |
| `app-release-bundle` | Release 版本 App Bundle |

点击文件名即可下载。

### 安装 APK 到设备

下载 `app-debug.apk` 后：

1. 将 APK 传输到你的 Android 设备
2. 在设备上打开文件管理器
3. 点击 APK 文件进行安装
4. 如果提示"允许安装未知来源应用"，请允许

---

## 🔄 触发构建的方式

### 1. Push 触发

每次你推送代码到 `main` 或 `master` 分支时，会自动触发构建：

```bash
# 修改代码后
git add .
git commit -m "Update something"
git push
```

### 2. Pull Request 触发

当你创建 Pull Request 时，会自动触发构建，帮助你检查代码是否能正常编译。

### 3. 手动触发

你也可以手动触发构建：

1. 进入 Actions 页面
2. 点击 "Build Android App" 工作流
3. 点击 "Run workflow" 按钮
4. 选择分支（如 main）
5. 点击绿色的 "Run workflow" 按钮

---

## ❓ 常见问题

### Q1: 推送代码时提示需要认证？

**A**: GitHub 需要验证你的身份，推荐使用 Personal Access Token：

1. 访问 https://github.com/settings/tokens
2. 点击 "Generate new token" → "Generate new token (classic)"
3. 勾选 `repo` 权限
4. 生成并复制 token
5. 推送时，用户名填你的 GitHub 用户名，密码填这个 token

### Q2: 构建失败怎么办？

**A**: 查看构建日志：

1. 进入 Actions 页面
2. 点击失败的构建记录
3. 查看日志中的错误信息

常见原因：
- 网络问题导致依赖下载失败 → 重新触发构建
- 代码有语法错误 → 修复后重新推送

### Q3: 如何修改构建配置？

**A**: 编辑 [.github/workflows/build.yml](file:///workspace/.github/workflows/build.yml) 文件：

```yaml
# 修改 JDK 版本
java-version: '17'  # 可以改成 '11', '21' 等

# 修改触发条件
on:
  push:
    branches: [ main, dev ]  # 可以添加更多分支
```

### Q4: 如何签名 Release APK？

**A**: 需要在 GitHub 仓库中配置签名密钥：

1. 生成签名密钥（使用 Android Studio）
2. 在仓库 Settings → Secrets and variables → Actions 中添加密钥
3. 修改工作流文件添加签名步骤

这需要较复杂的配置，建议先使用 Debug APK 测试。

### Q5: 构建速度太慢怎么办？

**A**: GitHub Actions 使用了缓存机制：

- Gradle 依赖会被缓存
- 第二次构建会比第一次快很多
- 通常 2-5 分钟即可完成构建

---

## 📚 更多资源

- [GitHub Actions 官方文档](https://docs.github.com/cn/actions)
- [Android 构建官方文档](https://developer.android.com/studio/build)
- [Git 入门教程](https://git-scm.com/doc)

---

## 🎉 完成！

现在你可以：

✅ 上传代码到 GitHub  
✅ 自动构建 APK  
✅ 下载并安装应用  

有问题？查看上面的"常见问题"部分，或者提交 Issue！
