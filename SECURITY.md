# Security Policy

## Supported releases

Only signed APKs published from the official GitHub Release workflow should be treated as production builds.

## Reporting vulnerabilities

Please open a private security advisory or contact the maintainer before publishing details.

## Build and release requirements

Production releases should:

- Be signed with a stable release keystore
- Publish APK SHA-256 checksums
- Publish source commit hash
- Publish checksums of bundled `libv2ray.aar`; `geoip.dat` and `geosite.dat` are downloaded/verified in-app to keep APK size smaller
- Avoid debug fallback APKs in official releases

## Operational notes

- Proxy credentials and subscription URLs are sensitive.
- `allowInsecure` weakens TLS validation and should be avoided where possible.
- Prefer certificate pinning or verified peer names when available.
