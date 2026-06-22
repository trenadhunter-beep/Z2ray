# Z2ray Release Guide

## GitHub Actions secrets for signed release

Add these repository secrets before running **Android Release** workflow:

```text
KEYSTORE_BASE64       Base64 encoded .jks file
STORE_PASSWORD        Keystore password
KEY_ALIAS             Key alias
KEY_PASSWORD          Key password
```

Create base64 keystore value:

```bash
base64 -w 0 release-keystore.jks
```

Run workflow:

```text
Actions → Android Release → Run workflow
```

Optional tag example:

```text
v1.0.0
```

The workflow uploads:

```text
z2ray.apk
```

If signing secrets are missing, workflow builds a debug fallback APK for testing only.
