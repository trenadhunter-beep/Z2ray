<p align="center">
  <img src="docs/z2ray-logo.svg" alt="Z2ray" width="760" />
</p>

# Z2ray

A modern Android Xray/V2Ray client for importing, managing, testing, and running proxy configurations.

[![Android CI Build](https://github.com/trenadhunter-beep/Z2ray/actions/workflows/android_build.yml/badge.svg)](https://github.com/trenadhunter-beep/Z2ray/actions/workflows/android_build.yml)
![API](https://img.shields.io/badge/API-23%2B-yellow.svg?style=flat)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-blue.svg?style=flat&logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg?style=flat&logo=jetpackcompose&logoColor=white)
![Xray](https://img.shields.io/badge/Core-Xray-06B6D4.svg?style=flat)
![License](https://img.shields.io/badge/License-MIT-green.svg?style=flat)

---

## About

**Z2ray** is an Android client built with **Kotlin**, **Jetpack Compose**, Android **VpnService**, and **Xray Core**.

It supports importing V2Ray-compatible links, managing subscriptions, measuring latency, running a real Android VPN tunnel, and displaying live traffic statistics.

---

## Features

- Real Android VPN Service
- Xray Core integration through `libv2ray.aar`
- GeoIP / GeoSite routing assets with in-app status/update support
- Reality advanced fields support
- TUN-based full-device routing
- Real traffic speed from Xray stats
- VLESS, VMess, Trojan, and Shadowsocks parser
- Subscription import and refresh
- Import/export configs backup
- QR scanner support
- Per-app proxy support
- Bypass Iran / Direct / Global routing modes
- API 23+ / Android 6.0+ support
- Multi-language UI with Persian RTL support
- GitHub Actions APK build

---

## Supported Config Formats

| Protocol | Link |
| --- | --- |
| VLESS | `vless://` |
| VMess | `vmess://` |
| Trojan | `trojan://` |
| Shadowsocks | `ss://` |

---

## Download APK

The APK is built automatically with GitHub Actions.

1. Open:

   <https://github.com/trenadhunter-beep/Z2ray/actions>

2. Open the latest successful build.
3. Scroll down to **Artifacts**.
4. Download:

```text
z2ray
```

5. Extract the ZIP file.
6. Install:

```text
z2ray.apk
```

---

## Build

```bash
git clone https://github.com/trenadhunter-beep/Z2ray.git
cd Z2ray
gradle assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## Release

For signed release builds, see [RELEASE.md](RELEASE.md).

## License

[MIT License](LICENSE)
