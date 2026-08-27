# 发布到 GitHub Releases

本仓库的 GitHub 地址是 **https://github.com/cuixinyuan666/kai**（不是上游 `SimonSchubert/Kai`）。

Cursor Cloud 推送到 Origin 后，GitHub 会镜像 `main` 与 tag。`v*` tag 会触发 `.github/workflows/release.yml`。

## 已知失败原因（v3.2.0 / v3.3.0）

各平台安装包已成功构建，但 **Rename assets** 在 Windows zip 已名为 `Kai-<version>-windows.zip` 时执行 `mv` 到同名文件失败，因此没有创建 GitHub Release。v3.3.1 已改为幂等重命名，WinGet 失败也不再阻断 Release。

## 发布流程

```bash
# 版本号写在 gradle/libs.versions.toml 的 appVersion
git tag v3.3.1
git push origin main
git push origin v3.3.1
```

然后打开：

- Actions：https://github.com/cuixinyuan666/kai/actions
- Releases：https://github.com/cuixinyuan666/kai/releases

## Release 产物（v3.3.1）

| 平台 | 文件名 |
|------|--------|
| Android | `Kai-3.3.1-android.apk` |
| Windows | `Kai-3.3.1-windows.zip`（便携压缩包） |
| Windows | `Kai-3.3.1-windows.msi` |
| macOS | `Kai-3.3.1-macos.dmg` |
| Linux | `Kai-3.3.1-linux.deb` / `.rpm` / `.AppImage` / `.flatpak` / `.tar.gz` |
