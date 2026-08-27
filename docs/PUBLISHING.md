# 发布到 GitHub Releases

## 为什么看不到最新 Release？

本仓库在 Cursor Cloud 中推送到的是 **Origin**（`origin.cursor.com`），而 GitHub Releases 由 **GitHub**（`github.com/SimonSchubert/Kai`）上的 tag 触发。

**仅推送到 Origin 不会自动在 GitHub 创建 Release。** 当前 GitHub 上最新 Release 仍为 **v3.0.0**（v3.1.x / v3.2.0 / v3.3.0 的 tag 若只存在于 Origin，GitHub Actions 不会运行）。

## 发布 v3.3.0 到 GitHub（需在本机执行）

在已登录 GitHub 的机器上：

```bash
# 1. 添加 GitHub 远程（若尚未添加）
git remote add github https://github.com/SimonSchubert/Kai.git

# 2. 拉取 Cursor / Origin 上的最新 main 与 tag（或合并协作功能分支）
git fetch origin main
git checkout main
git pull origin main

# 3. 推送代码与 tag 到 GitHub（触发 release.yml）
git push github main
git push github v3.3.0

# 4. 若 tag 已存在但 Release 未生成，可手动触发 Actions：
# GitHub → Actions → Build and Release → Run workflow → tag: v3.3.0
```

或使用 GitHub CLI：

```bash
gh auth login
git push https://github.com/SimonSchubert/Kai.git main v3.3.0
gh workflow run release.yml -f tag=v3.3.0
```

若你 fork 到自己的仓库（例如 `cuixinyuan666/kai`），将上述 URL 换成你的 fork 地址即可；Release 会出现在 **你的 fork** 的 Releases 页面，而不是上游 `SimonSchubert/Kai`。

## Release 产物（v3.3.0）

| 平台 | 文件名 |
|------|--------|
| Android | `Kai-3.3.0-android.apk` |
| Windows | `Kai-3.3.0-windows.zip`（便携压缩包） |
| Windows | `Kai-3.3.0-windows.msi` |
| macOS | `Kai-3.3.0-macos.dmg` |
| Linux | `Kai-3.3.0-linux.deb` / `.rpm` / `.AppImage` / `.flatpak` / `.tar.gz` |

## 验证 Release 是否成功

1. 打开 https://github.com/SimonSchubert/Kai/actions — 确认 **Build and Release** 工作流对 `v3.3.0` 为绿色。
2. 打开 https://github.com/SimonSchubert/Kai/releases — 应出现 **Release v3.3.0** 及上述安装包。
