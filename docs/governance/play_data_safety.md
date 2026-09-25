# Google Play Console — Data Safety Declaration

**App Name:** StockPro — Manufacturing Plant Inventory System  
**Package:** `com.company.stockpro`  
**Governing Standard:** `docs/architecture.md` §10.5, Google Play Developer Program Policies

---

## 1. Data Collection & Sharing Overview

- **Does the app collect or share any required user data types?** Yes.
- **Is all of the user data collected by the app encrypted in transit?** Yes (TLS 1.3 / HTTPS across all network requests).
- **Does the app provide a way for users to request that their data be deleted?** Yes (in-app deactivation workflow + web deletion portal).

---

## 2. Detailed Data Types Declaration

### 2.1 Personal Info
| Data Type | Collected | Shared | Purpose | Ephemeral? | Required / Optional? |
|---|---|---|---|---|---|
| **Name** | Yes | No | App functionality, Account management | No | Required |
| **Email address** | Yes | No | App functionality, Account management, Developer communications | No | Required |
| **User IDs** | Yes | No | App functionality, Account management | No | Required |

### 2.2 Photos & Videos
| Data Type | Collected | Shared | Purpose | Ephemeral? | Required / Optional? |
|---|---|---|---|---|---|
| **Photos** | Yes | No | App functionality (Item catalog photos, Tool custody condition photos) | No | Optional |

### 2.3 App Info & Performance
| Data Type | Collected | Shared | Purpose | Ephemeral? | Required / Optional? |
|---|---|---|---|---|---|
| **Crash logs** | Yes | No | Analytics, App functionality | No | Required |
| **Diagnostics** | Yes | No | Analytics, Performance monitoring | No | Required |

### 2.4 Device or Other IDs
| Data Type | Collected | Shared | Purpose | Ephemeral? | Required / Optional? |
|---|---|---|---|---|---|
| **Device or other IDs** | Yes | No | App functionality, Account management, Push notifications (FCM tokens) | No | Required |

---

## 3. Device Permissions Justification

| Permission | Technical Name | Operational Purpose |
|---|---|---|
| **Camera** | `android.permission.CAMERA` | Used exclusively within `BarcodeScannerDialog` for scanning QR code and Code-128 physical stock and bin labels. Frames are processed in memory and never saved or transmitted off-device. |
| **Internet** | `android.permission.INTERNET` | Communicating with Firebase Cloud Functions and Firestore replication backend. |
| **Notifications** | `android.permission.POST_NOTIFICATIONS` | Receiving critical low-stock, out-of-stock, and sync-failure push notifications. |
