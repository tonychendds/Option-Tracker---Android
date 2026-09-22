# Option Tracker

Option Tracker is an offline Android app for recording option positions you enter by hand. It shows what you still have open, and the realized profit or loss after you close a trade.

It is a position tracker, not a spending app, not a broker, and not financial advice. Version 1 does not sync a brokerage account, does not download quotes, and does not place orders.

## What you can do

Bottom navigation: **Home**, **Positions**, **History**, **Settings**.

- Add an open position: ticker, buy or sell, call or put, strike, expiration, contracts, premium per share, optional fees, notes, and the date you opened it.
- Edit an open position, or delete it.
- Close a position with an exit date, exit premium, and optional fees. The app computes realized P/L and moves the trade to History.
- Home summarizes open premium cash flow, contract counts, realized P/L for the current month, and recent activity.
- History lists closed trades by month and can filter by ticker. Closed trades are read-only.
- Settings: light, dark, or system theme. Currency is US dollars. Remove ads and export are placeholders.

Home and History show a banner **advertisement placeholder**. No AdMob app id or ad unit id is in this project.

## Profit and loss

Premiums are the quoted price **per share**, in US dollars. Standard equity options use a **100** multiplier:

```text
notional = premium per share × contracts × 100
```

The add and close screens label this. One contract at a $1.50 premium is $150 before fees.

Fees are the total commissions for that leg. They always reduce the result.

### Opening cash flow

Credits are positive. Debits are negative. Open positions are not marked to market, because there are no live prices.

```text
Buy:  −(entry notional + entry fees)
Sell: entry notional − entry fees
```

Home calls the sum of those amounts **Open premium**.

### Realized P/L

Closing a buy means you sell. Closing a sell means you buy. The close screen does not ask for a second side.

```text
Buy to open:  (exit notional − entry notional) − entry fees − exit fees
Sell to open: (entry notional − exit notional) − entry fees − exit fees
```

Examples:

| Trade | Result |
| --- | --- |
| Buy 1 contract at $2.50 with a $1.00 fee. Close at $4.00 with a $1.00 fee. | $148.00 profit |
| Sell 2 contracts at $3.00 with a $1.30 fee. Close at $1.20 with a $1.30 fee. | $357.40 profit |

A $0 exit premium is allowed, for a contract that expired worthless. Realized P/L for the current month uses the exit date.

## Open in Android Studio

Use Android Studio with Android Gradle Plugin 9.4 (Quail 4 or newer) and JDK 17 or newer.

1. Install the Android SDK platform **Android API 37.2** and build-tools **37.0.0**.
2. **File → Open** and choose this repository (the folder that contains `settings.gradle.kts`).
3. Let Gradle sync. Android Studio writes `local.properties` with your SDK path.
4. Run the **app** configuration on a device or emulator.

Minimum SDK is 26. Target SDK is 37.

## Build a debug APK

From the repository root, with `JAVA_HOME` pointing at JDK 17+ and the Android SDK installed:

```bash
./gradlew assembleDebug
```

The APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Unit tests for P/L math and position create, read, update, close, and delete:

```bash
./gradlew testDebugUnitTest
```

## Data

Positions live in a Room database on the device (`option_tracker.db`). There is no account and no network permission. Theme choice is stored in DataStore.

## Ads and Play Billing later

This version does not ship the AdMob SDK or Google Play Billing.

- Banner slots on Home and History are a labeled placeholder composable. Do not drop a real ad unit id into that composable without a privacy policy and the Play Console ad setup.
- Settings has a disabled **Remove ads** action. A future build can add Play Billing as a one-time product and hide the placeholder when the purchase is owned. No product id is configured now.
- **Export trades** explains that export is not available and does not write a file.

## Out of scope for v1

Broker sync, live market data, watchlists, alerts, wheel-strategy templates, analytics SDKs, and real AdMob or Play Billing.

## Stack

Kotlin, Jetpack Compose, Material 3, Navigation, Room, DataStore. UI, ViewModel, repository, and Room are separate. Money is stored as integer cents.
