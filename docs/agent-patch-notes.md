# Z2ray Agent Patch Notes

This patch turns the repository from a basic MVP importer into a broader Xray-oriented client foundation.

## Added

- Extended import/parser support:
  - VLESS advanced fields: REALITY, gRPC serviceName, authority, packetEncoding, pinnedPeerCertSha256, verifyPeerCertByName, ECH config list.
  - VMess extra fields: alterId, scy/encryption, header type, serviceName/authority.
  - Trojan extra TLS/transport fields.
  - Shadowsocks SIP008 JSON subscriptions.
  - Hysteria2/hy2 share links.
  - SOCKS/SOCKS5 share links.
  - Raw Xray JSON profile passthrough.
  - Basic Clash/Mihomo YAML proxy parsing.
- Extended Xray JSON generation:
  - Transport settings for TCP headers, WebSocket, gRPC, HTTP/2, HTTPUpgrade, XHTTP/SplitHTTP, mKCP, QUIC.
  - TLS pinning fields and ECH fields where available.
  - Hysteria2 outbound using Xray's `hysteria` protocol and `hysteriaSettings`.
  - Raw JSON configs are no longer rewritten.
- VPN/core bridge hardening:
  - Callback-based socket protection support to reduce VPN recursion risk when libv2ray exposes protect callbacks.
- Project polish:
  - Renamed namespace/package from `com.example` to `com.z2ray.android`.
  - Renamed Gradle root project to `Z2ray`.
  - Bumped Room database version for new config fields.
- Routing and diagnostics:
  - Custom routing rule model with JSON import/export.
  - Xray routing generation for custom DOMAIN/IP/PROTOCOL/NETWORK rules.
  - Ad-block toggle using `geosite:category-ads-all`.
  - URL connectivity test, download speed test, and TLS certificate handshake diagnostic helpers.
- Core/assets/release hardening:
  - Detailed geoip/geosite asset status with SHA-256 display and runtime download fallback.
  - Core runtime info provider.
  - Replaced the misleading in-app v2fly runtime selector with build-time core flavors: default Xray and optional v2fly APKs through CI/release workflow inputs.
  - Release workflow now requires signing secrets and publishes checksums.
  - CI no longer bundles GeoIP/GeoSite into the APK; assets are downloaded in-app to reduce APK size.
  - Added privacy and security policy documents.
- UI additions:
  - Protocol/group/latency filtering and sorting.
  - Config detail dialog with copy/delete/TLS test actions.
  - Basic routing rule import/toggle/delete UI.
- Tests:
  - Parser tests for VLESS REALITY, Hysteria2, SIP008 JSON, and basic Clash YAML.

## Still not fully solved

- TUIC is parsed, but Xray-core does not provide a normal TUIC outbound in this build path; it is intentionally blocked by config generation unless a raw compatible JSON/core is supplied.
- Full Mihomo YAML parsing is intentionally pragmatic, not a complete YAML parser.
- Real post-connect URL testing still needs a dedicated local-proxy test path or Xray observatory integration.
- Release signing workflow is present, but the repository owner must add the required GitHub secrets and verify the first release artifact before production distribution.
