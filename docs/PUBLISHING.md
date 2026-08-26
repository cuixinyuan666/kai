# 发布到 GitHub Releases

## 为什么看不到 Release？

本仓库在 Cursor Cloud 中推送到的是 **Origin**（`origin.cursor.com`），而 GitHub Releases 由 **GitHub**（`github.com/SimonSchubert/Kai`）上的 tag 触发。

仅推送到 Origin **不会**自动在 GitHub 创建 Release。当前 GitHub 上最新 Release 仍为 **v3.0.0**。

## 发布 v3.2.0 到 GitHub（需在本机执行）

在已登录 GitHub 的机器上：

```bash
# 1. 添加 GitHub 远程（若尚未添加）
git remote add github https://github.com/SimonSchubert/Kai.git

# 2. 推送代码与 tag
git push github main
git push github v3.2.0

# 3. 等待 Actions 完成，或手动触发
# GitHub → Actions → Build and Release → Run workflow → tag: v3.2.0
```

或使用 GitHub CLI：

```bash
gh auth login
git push https://github.com/SimonSchubert/Kai.git main v3.2.0
gh workflow run release.yml -f tag=v3.2.0
```

## Release 产物

| 平台 | 文件名 |
|------|--------|
| Android | `Kai-3.2.0-android.apk` |
| Windows | `Kai-3.2.0-windows.zip`（便携压缩包） |
| Windows | `Kai-3.2.0-windows.msi` |

## 本地已构建（Cloud Agent）

若 GitHub Actions 尚未运行，可在 Cloud Agent 产物中下载已构建的 Android APK：

- `/opt/cursor/artifacts/releases/Kai-3.2.0-android.apk`
