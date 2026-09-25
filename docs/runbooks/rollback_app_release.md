# Runbook: Roll Back a Bad App Release

**Incident Code:** INC-APP-ROLLBACK  
**Severity:** P0/P1  
**Trigger:** Crashlytics crash rate > 0.5%, widespread ANRs, or corrupted local Room database migrations detected during staged rollout.

---

## 1. Immediate Mitigation: Halt Staged Rollout (< 5 Minutes)

1. Open **Google Play Console** → **StockPro** → **Production** (or Open/Closed Testing track).
2. Click **Releases overview** → select current rolling release.
3. Click **Halt rollout**.
   - *Effect: Halts distribution to new devices. Users who have not yet updated will remain on the previous stable version.*

---

## 2. Server-Side Feature Kill Switch (Remote Config)

If the bug is caused by a specific feature (e.g. camera scanner or tool custody dialog):

1. Open **Firebase Remote Config** console.
2. Toggle the relevant kill switch:
   - `enable_barcode_scanner`: `false`
   - `enable_tool_custody`: `false`
   - `min_supported_version_code`: Set to previous stable version code.
3. Click **Publish changes**.
   - *Effect: Client apps fetch updated Remote Config within 15 minutes and disable the faulty feature, falling back to manual entry or stable workflows.*

---

## 3. Emergency Hotfix Release

Android applications cannot be technically "rolled back" on Google Play to a lower version code. To replace a bad build for users who already installed it:

1. Revert the problematic change in git.
2. Increment `versionCode` by +1 (e.g., from 42 to 43).
3. Assemble and sign the release bundle:
   ```bash
   $env:JAVA_HOME="C:\Program Files\Android\Android Studio1\jbr"; .\gradlew.bat bundleRelease
   ```
4. Upload hotfix bundle to Google Play Console as an **Expedited Review Release** with 100% rollout.
