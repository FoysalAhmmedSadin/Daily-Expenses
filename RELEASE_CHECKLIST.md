# Production Release Checklist — v2.0.0

This repository separates CI debug builds from production-signed releases.

## 1. One-time GitHub Actions setup

In **Settings → Secrets and variables → Actions**, add these repository secrets:

| Secret | Value |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | Base64-encoded contents of the production Java keystore |
| `RELEASE_STORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_PASSWORD` | Password for the `upload` key alias |

The release Gradle configuration currently uses the key alias `upload`. The keystore must contain that alias and must be the same signing identity used for any previous production distribution of this package. Keep a secure offline backup of the keystore and passwords. Never commit the keystore or passwords to this repository.

To create the Base64 value locally:
- Linux: `base64 -w 0 my-upload-key.jks`
- macOS: `base64 < my-upload-key.jks | tr -d '\n'`
- PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes('my-upload-key.jks'))`

Do not paste signing passwords or keystore contents into issues, chat, or source files.

## 2. Build signed release artifacts

1. Open **Actions → Production Android Release**.
2. Select **Run workflow**.
3. Set `source_ref` to `v2.0.0` and `release_tag` to `v2.0.0`.
4. Run the workflow and wait for all signature-verification steps to pass.
5. Confirm that the existing GitHub Release contains:
   - `daily-expenses-v2.0.0-release.apk`
   - `daily-expenses-v2.0.0.aab`
   - `SHA256SUMS.txt`

The workflow intentionally does not publish if the signing secrets are missing or signature verification fails.

## 3. Huawei AppGallery submission

The GitHub workflow prepares the signed APK and AAB; it does not log in to AppGallery Connect or submit an app on the developer's behalf.

1. Download the signed APK/AAB from the GitHub Release.
2. Confirm the package name is `com.track.dailyexpenses.app` and the version is `2.0.0 (2)`.
3. Confirm the signing certificate is the same certificate registered for this app in AppGallery Connect. If the app was previously released with a different certificate, stop and resolve the signing identity before uploading.
4. In AppGallery Connect, open the existing app and upload the package under the intended formal-release track. Select the appropriate countries/regions and complete the store listing, content rating, privacy, and compliance declarations.
5. Review Huawei's package validation results, test the uploaded build on supported devices, and submit the draft for review.
6. The app is publicly released only after Huawei approves and publishes it.

Official Huawei guidance:
- [Release an app package](https://developer.huawei.com/consumer/en/doc/app/agc-help-release-overview-0000001272395372/)
- [Uploading an app package](https://developer.huawei.com/consumer/en/doc/app/agc-help-release-app-upload-pkg-0000002277983368/)
- [App Bundle testing guide](https://developer.huawei.com/consumer/en/doc/appgallery-connect-guides/agc-appsigning-aabtest-0000001059321484/)

## 4. Release gates

- [ ] Production signing secrets configured in GitHub Actions.
- [ ] Release workflow completed successfully.
- [ ] APK signature verified and certificate fingerprint checked.
- [ ] AAB signature verified.
- [ ] APK and AAB install/launch and core flows tested on real devices.
- [ ] Huawei package validation passed.
- [ ] Store listing, privacy policy, content rating, and declarations reviewed.
- [ ] Huawei review approved and formal release published.

**Important:** The existing v2.0.0 GitHub release initially contained an APK produced by the debug CI workflow. Do not treat that earlier APK as a production-signed store package. Use the newly generated `*-release.apk` and `.aab` assets after the production workflow succeeds.
