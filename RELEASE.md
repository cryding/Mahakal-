# MAHAKAL Android Production Release & Signing Runbook

This guide contains the operational specifications, cryptographic standards, and deployment runbook for producing signed, verified production Android release artifacts (`.apk` and `.aab`) for the MAHAKAL application.

---

## 1. Required GitHub Actions Secrets

To execute the production release pipeline in `.github/workflows/android-release.yml`, configure the following repository secrets under **Settings > Secrets and variables > Actions**:

| Secret Name | Description | Example / Format |
|---|---|---|
| `PROD_RELEASE_KEYSTORE_BASE64` | Base64-encoded production PKCS12 keystore file (`.jks` / `.keystore`) | Single-line base64 string (`base64 -w 0 upload.keystore`) |
| `PROD_STORE_PASSWORD` | Password protecting the keystore file | Alphanumeric passphrase |
| `PROD_KEY_PASSWORD` | Password protecting the key alias `upload` | Alphanumeric passphrase |

> **Security Note:** Never log or print the values of these secrets in workflow steps. These secrets are securely injected into Gradle via environment variables (`ORG_GRADLE_PROJECT_mahakal_release_*` / `MAHAKAL_RELEASE_*`).

---

## 2. Production Keystore Specifications

The production release signing configuration strictly requires:

- **Format:** PKCS12
- **Key Alias:** `upload`
- **Canonical Certificate SHA-256 Fingerprint:**  
  `52:E9:B3:41:EC:66:62:A4:60:5B:0A:DE:2F:E8:51:54:64:D6:12:A3:52:F4:CB:38:B3:F3:9C:AF:AD:97:BD:3A`
- **Normalized SHA-256 Fingerprint:**  
  `52E9B341EC6662A4605B0ADE2FE8515464D612A352F4CB38B3F39CAFAD97BD3A`

Any build attempt where the keystore alias is not `upload`, the format is invalid, or the certificate SHA-256 does not match this canonical fingerprint will be immediately terminated by the pipeline.

---

## 3. Generating the Base64 Keystore Secret

To encode an existing release keystore into the format expected by `PROD_RELEASE_KEYSTORE_BASE64`:

### Linux
```bash
base64 -w 0 upload.keystore > upload.keystore.base64.txt
```

### macOS
```bash
base64 -i upload.keystore -o upload.keystore.base64.txt
```

### Verification Before Uploading
Verify that your keystore matches the canonical SHA-256 fingerprint before setting the secret:

```bash
keytool -list -v -keystore upload.keystore -alias upload
```

Look for the line:
```text
SHA256: 52:E9:B3:41:EC:66:62:A4:60:5B:0A:DE:2F:E8:51:54:64:D6:12:A3:52:F4:CB:38:B3:F3:9C:AF:AD:97:BD:3A
```

---

## 4. Running the Release Pipeline

The production pipeline is located in `.github/workflows/android-release.yml`.

### Trigger via GitHub Actions UI (workflow_dispatch)
1. Navigate to **Actions** in GitHub.
2. Select **MAHAKAL Android Release & GitHub Release Pipeline**.
3. Click **Run workflow**.
4. Configure inputs:
   - **Branch:** `main`
   - **Release Git Tag:** `v1.0.0` (or target semver)
   - **Publish GitHub Release:** `false` (set to `true` only when publishing a public release)
5. Click **Run workflow**.

---

## 5. Artifact Verification & Inspection Checklist

The release workflow automatically runs strict automated gates before releasing artifacts:

- [x] **No Unsigned Artifacts:** Release builds consume the real `release` signing configuration. AGP tasks enforce that keystore files and passwords are non-empty.
- [x] **APK Signature Verification:** Verified using Android SDK `apksigner verify --verbose --print-certs`.
- [x] **AAB Signature Verification:** Verified using `jarsigner -verify -verbose -certs` and `keytool -printcert -jarfile`.
- [x] **Zero Debug Signatures:** Both APK and AAB are rejected if `CN=Android Debug` is detected.
- [x] **Fingerprint Match:** The certificate SHA-256 is compared directly against the canonical hash `52:E9:B3:41:...`.
- [x] **Leaked Secret Scan:** Artifacts are inspected to guarantee no `.jks`, `.keystore`, `.signing.properties`, or password files were bundled.
- [x] **Prohibited Endpoint Scan:** DEX bytecode is scanned to verify none of the following development endpoints exist:
  - `127.0.0.1`
  - `10.0.2.2`
  - `10.0.3.2`
  - `localhost`
  - `dev-api.mahakal.internal`
- [x] **Production API Endpoint:** Verified as `https://mahakal-qo14.onrender.com`.
- [x] **Checksum Generation:** `SHA256SUMS.txt` is produced for all staged artifacts:
  - `release-artifacts/MAHAKAL-release.apk`
  - `release-artifacts/MAHAKAL-release.aab`
  - `release-artifacts/SHA256SUMS.txt`
- [x] **Keystore Cleanup:** The decoded temporary keystore is securely removed using `shred -u` in an `always()` post-action step.

---

## 6. Troubleshooting Guide

### A. Secret Decoding Failure
- **Error:** `Decoded keystore is empty or missing`
- **Cause:** `PROD_RELEASE_KEYSTORE_BASE64` secret was created with extra newlines, missing characters, or copied incorrectly.
- **Remedy:** Re-encode using `base64 -w 0 upload.keystore` (Linux) or `base64 -i upload.keystore` (macOS), and update the GitHub secret.

### B. Fingerprint Mismatch
- **Error:** `APK signature certificate does not match the canonical production fingerprint!`
- **Cause:** An incorrect keystore or alias was provided, or a debug keystore was supplied.
- **Remedy:** Verify your keystore with `keytool -list -v -keystore upload.keystore -alias upload` and confirm the SHA-256 fingerprint matches `52:E9:B3:41:EC:66:62:A4:60:5B:0A:DE:2F:E8:51:54:64:D6:12:A3:52:F4:CB:38:B3:F3:9C:AF:AD:97:BD:3A`.

### C. Unsigned Artifact Failure
- **Error:** `Production release build failed: Keystore file is missing or not configured.`
- **Cause:** The release build task was invoked without injecting keystore parameters or files.
- **Remedy:** Ensure the `signingConfigs.release` parameters are supplied via Gradle project properties (`ORG_GRADLE_PROJECT_mahakal_release_*` or command-line `-Pmahakal.release.*`).

### D. Endpoint Scan Failure
- **Error:** `Prohibited development endpoint found in release DEX: ...`
- **Cause:** Localhost or test endpoints (`10.0.2.2`, `127.0.0.1`, `localhost`) were hardcoded in application sources.
- **Remedy:** Ensure `NetworkModule.kt` and `ApiConstants.kt` point exclusively to the production endpoint `https://mahakal-qo14.onrender.com`.
