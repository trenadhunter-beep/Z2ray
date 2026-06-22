# Z2ray

A modern Android client interface for managing V2Ray-style proxy configurations.

[![Android CI Build](https://github.com/trenadhunter-beep/Z2ray/actions/workflows/android_build.yml/badge.svg)](https://github.com/trenadhunter-beep/Z2ray/actions/workflows/android_build.yml)
![API](https://img.shields.io/badge/API-24%2B-yellow.svg?style=flat)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-blue.svg?style=flat&logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg?style=flat&logo=jetpackcompose&logoColor=white)
![Platform](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat&logo=android&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green.svg?style=flat)

---

## About

**Z2ray** is an Android project built with **Kotlin** and **Jetpack Compose**.

It provides a simple and modern interface for importing, managing, and testing V2Ray-compatible server configurations.

The project is designed as a lightweight foundation for an Android V2Ray/Xray client.

---

## Features

- Modern Android UI with Jetpack Compose
- Import V2Ray-style configuration links
- Manage local proxy nodes
- Subscription support
- QR scanner support
- Multi-language UI
- Persian RTL support
- Server latency test UI
- Diagnostic logs
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

### Requirements

- Android Studio
- JDK 17
- Android SDK

### Clone

```bash
git clone https://github.com/trenadhunter-beep/Z2ray.git
cd Z2ray
```

### Build debug APK

```bash
gradle assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## Project Structure

```text
Z2ray/
├── app/
├── gradle/
├── .github/workflows/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## Roadmap

- Android VPN Service integration
- Xray/V2Ray core integration
- Signed release APK
- Improved subscription updater
- Real-time traffic statistics

---

## Disclaimer

This project is provided for educational and development purposes.  
Users are responsible for complying with local laws and network policies.

---

## License

[MIT License](LICENSE)
