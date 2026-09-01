# 发布到 GitHub Releases

本仓库的 GitHub 地址是 **https://github.com/cuixinyuan666/kai**（不是上游 `SimonSchubert/Kai`）。

打 `v*` tag 会触发 `.github/workflows/release.yml`，**只编译并上传 Android APK**。

## 发布流程

版本号写在 `gradle/libs.versions.toml` 的 `appVersion`（Android `versionCode` 同步加一）：

```bash
git tag v3.5.9
git push origin main
git push origin v3.5.9
```

然后打开：

- Actions：https://github.com/cuixinyuan666/kai/actions
- Releases：https://github.com/cuixinyuan666/kai/releases

## Release 产物

| 平台 | 文件名 |
|------|--------|
| Android | `Kai-<version>-android.apk` |

不构建 Windows / macOS / Linux 安装包。若仓库未配置签名密钥，APK 使用 debug 签名，可侧载安装。
