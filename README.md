# Option Tracker

Option Tracker is an offline Android app for recording option positions you enter by hand. It shows what you still have open, and the realized profit or loss after you close a trade.

It is a position tracker, not a spending app, not a broker, and not financial advice. Version 1 does not sync a brokerage account, does not download quotes, and does not place orders.

## What you can do

Bottom navigation: **Home**, **Positions**, **History**, **Settings**.

- Add an open position: ticker, buy or sell, call or put, strike, expiration, contracts, premium per share, optional fees, notes, and the date you opened it.
- Edit an open position, or delete it.
- Close a position with an exit date, exit premium, and optional fees. The app computes realized P/L and moves the trade to History.
- Home summarizes open premium cash flow, contract counts, realized P/L for the current month, realized P/L for the selected calendar year, and recent activity.
- History lists closed trades by month, shows that year's total and a January–December report, and can filter by ticker. Closed trades are read-only. Home and History share the year. Chips appear when closed trades span more than the current year.
- Settings: light, dark, or system theme. Currency is US dollars. **Import CSV** replaces the trades on the phone with a spreadsheet export. Remove ads and export are placeholders.

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

**YTD** is the sum of realized P/L for closed trades whose close date falls in the selected calendar year, from January 1 through December 31. The default year is the current year. A prior year can be chosen when the log spans more than one year. Open positions are not included. The same figure is the **Year total** at the top of History for that year, and the ticker filter narrows it. An imported close date is the expiration date, so a closed trade dated later this year is included in that year's total.

**Monthly report** on History lists January through December for that same year. Each row shows realized P/L, how many trades closed, and the hit rate. Hit rate is the share of those closes whose realized P/L is greater than zero. A breakeven result is not a hit. A month with no closes shows zero profit and no hit rate. The sheet's max-profit column is not stored, so the report does not sum it. Tap a month that has trades to jump to that month's list. The ticker filter applies to the report and the lists.

### Spreadsheet realized P/L

A closed CSV row may include `realizedOverride`, a dollar profit or loss from the sheet. When that cell has a number, including zero or a loss, the app stores it and **shows that amount** everywhere realized P/L appears: the trade, the monthly report, History month totals, the year total, and Home, including YTD. Entry premium, exit premium, and fees are still saved, but they are not used for that displayed result.

If `realizedOverride` is blank, realized P/L is calculated from premiums and fees as above. The sheet's single `fees` column is stored as the opening fee, and the exit fee is zero, so the fee is subtracted once.

The sheet has no exit date. A closed import uses the expiration date as the close date, which is what History groups by.

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

## Import CSV

Settings → **Import CSV** opens a file picker. The file stays on the device; nothing is uploaded. Confirming the import **replaces all local trades**. Rows that fail validation are skipped and counted. If the file has no usable rows, existing trades are left in place.

The header row is required. Names are matched without regard to case:

```text
status,account,ticker,side,right,strike,openDate,expDate,contracts,entryPremium,exitPremium,fees,notes,realizedOverride
```

| Column | Values |
| --- | --- |
| status | `Open` or `Closed` |
| account | Free text such as IRA, CASH, HSA, or ROTH. Shown on the position. |
| ticker | Such as `AAPL` or `BRK.B` |
| side | `Buy` or `Sell` |
| right | `Call` or `Put` |
| strike | Strike price in dollars |
| openDate, expDate | `yyyy-MM-dd` |
| contracts | Whole number of contracts |
| entryPremium | Premium per share. Blank is not allowed. |
| exitPremium | Premium per share. Blank is fine for an open trade. A closed trade needs this or `realizedOverride`. |
| fees | Dollars, blank means 0 |
| notes | Optional text. Quotes are allowed when the note contains a comma. |
| realizedOverride | Optional dollar P/L for a closed trade. See above. |

Example:

```csv
status,account,ticker,side,right,strike,openDate,expDate,contracts,entryPremium,exitPremium,fees,notes,realizedOverride
Open,CASH,SPY,Sell,Put,500,2026-09-01,2026-10-16,2,3.00,,0.65,hedge,
Closed,IRA,AAPL,Buy,Call,200,2026-09-01,2026-09-18,1,2.50,4.00,1.00,"rolled, earnings",148.00
```

## Data

Positions live in a Room database on the device (`option_tracker.db`). There is no login and no network permission. The account label from a CSV is stored with the trade. Theme choice is stored in DataStore.

## Ads and Play Billing later

This version does not ship the AdMob SDK or Google Play Billing.

- Banner slots on Home and History are a labeled placeholder composable. Do not drop a real ad unit id into that composable without a privacy policy and the Play Console ad setup.
- Settings has a disabled **Remove ads** action. A future build can add Play Billing as a one-time product and hide the placeholder when the purchase is owned. No product id is configured now.
- **Import CSV** reads a local file through the system file picker. It does not use the network.
- **Export trades** explains that export is not available and does not write a file.

## Out of scope for v1

Broker sync, live market data, watchlists, alerts, wheel-strategy templates, analytics SDKs, and real AdMob or Play Billing.

## Stack

Kotlin, Jetpack Compose, Material 3, Navigation, Room, DataStore. UI, ViewModel, repository, and Room are separate. Money is stored as integer cents.
