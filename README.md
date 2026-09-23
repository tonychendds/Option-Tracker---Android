# Option Tracker

Option Tracker is an offline Android app for recording option positions you enter by hand. It shows what you still have open, and the realized profit or loss after you close a trade.

It is a position tracker, not a spending app, not a broker, and not financial advice. Version 1 does not sync a brokerage account, does not download quotes, and does not place orders.

## What you can do

Bottom navigation: **Home**, **Positions**, **History**, **Settings**.

- Add an open position: ticker, buy or sell, call or put, strike, expiration, contracts, premium per share, optional fees, notes, and the date you opened it. Home → **Add** can also **Add from screenshot**.
- **Add from screenshot** reads a Charles Schwab “Trade Transaction Details” image on the device and opens the add form already filled in. You review the fields and tap Save. Nothing is stored until then.
- Edit an open position, or delete it.
- Close a position with an exit date, exit premium, and optional fees. The app computes realized P/L and moves the trade to History.
- Home summarizes open premium cash flow, contract counts, realized P/L for trades **opened** this month, realized P/L for the selected calendar year, and recent activity.
- History lists closed trades under the month they were **opened**, shows that year's close-date total and a January–December report, and can filter by ticker. Open a closed trade to edit its premiums, fees, dates, or a realized P/L override, or to delete it. Home and History share the year. Chips appear when open or close dates span more than the current year.
- Settings: light, dark, or system theme. Currency is US dollars. **Import CSV** replaces the trades on the phone with a spreadsheet export. Remove ads and export are placeholders.

Home and History show a banner **advertisement placeholder**. No AdMob app id or ad unit id is in this project.

## Profit and loss

Premiums are the quoted price **per share**, in US dollars. Standard equity options use a **100** multiplier:

```text
notional = premium per share × contracts × 100
```

The add and close screens label this. One contract at a $1.50 premium is $150 before fees. Type the premium as a positive number. **Buy** means you pay it (a debit). **Sell** means you receive it (a credit). Closing reverses that cash flow: selling to close receives the exit premium, and buying to close pays it.

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

A $0 exit premium is allowed, for a contract that expired worthless.

**Months use the open date.** If a trade was opened in September and closed in October, its realized P/L counts in September only. Home’s “this month” card uses that same rule for the current month. History’s month lists and the January–December report do too. A position that is still open has no realized P/L and is left out.

**YTD stays on the close date.** It is the sum of realized P/L for closed trades whose close date falls in the selected calendar year, from January 1 through December 31. The default year is the current year. A prior year can be chosen when opens or closes span more than one year. Open positions are not included. The same figure is the **Year total** at the top of History for that year, and the ticker filter narrows it. An imported close date is the expiration date, so a closed trade that expires later this year is included in that year's total even when it was opened the year before. Because of that, the year total can differ from the sum of the open-month rows when a trade crosses a calendar year.

**Monthly report** on History lists January through December of the open year. Each row shows realized P/L, how many closed trades were opened that month, and the hit rate. Hit rate is the share of those closes whose realized P/L is greater than zero. A breakeven result is not a hit. A month with no closes shows zero profit and no hit rate. The sheet's max-profit column is not stored, so the report does not sum it. Tap a month that has trades to jump to that month's list. The ticker filter applies to the report and the lists.

### Spreadsheet realized P/L

A closed CSV row may include `realizedOverride`, a dollar profit or loss from the sheet. When that cell has a number, including zero or a loss, the app stores it and **shows that amount** everywhere realized P/L appears: the trade, the monthly report, History month totals, the year total, and Home, including YTD. Entry premium, exit premium, and fees are still saved, but they are not used for that displayed result.

If `realizedOverride` is blank, realized P/L is calculated from premiums and fees as above. The sheet's single `fees` column is stored as the opening fee, and the exit fee is zero, so the fee is subtracted once.

The sheet has no exit date. A closed import uses the expiration date as the close date. That date is what the year total uses. History month groups use the open date instead.

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

## Add from screenshot

Home → **+** → **Add from screenshot**, or **Add from screenshot** on the add-position screen. Pick a screenshot of a Charles Schwab trade-details page.

The app reads the text **on the device** with Google ML Kit. The image is not uploaded. OCR does not need a connection. A label on the left and its value on the right are paired by their position on the screen, so a two-column ticket still lines up.

A line such as `TSLL 09/25/2026 11.00 C` supplies the ticker, expiration, strike, and call or put. **Sell to Open** and **Buy to Open** set the side. **Price** is the premium per share. **Quantity** is the number of contracts. Commission, industry fee, and similar fee lines are added together. The trade date is the open date. The order id is stored in notes.

**Buy to Close** and **Sell to Close** still open the add form so you can review them. Saving creates a new open position. It does not close an existing trade.

If the symbol line is read but quantity, price, or the trade date is not, the form fills in what was found and names the missing fields. If the symbol line cannot be read, the form stays empty and says so. You can type the trade yourself.

Saving compares the form with trades already stored, both open and closed. A match is the same ticker, side, call or put, strike, expiration, contracts, and open date, with an entry premium within $0.01. The same Schwab order id in the notes also counts. Save then asks you to cancel or save anyway. After a screenshot fills the form, a banner names the possible duplicate before you tap Save.

## Import CSV

Settings → **Import CSV** opens a file picker. The file stays on the device; nothing is uploaded. Confirming the import **replaces all local trades**. Rows that fail validation are skipped and counted. If the file has no usable rows, existing trades are left in place.

The header row is required. Names are matched without regard to case:

```text
status,account,ticker,side,right,strike,openDate,expDate,contracts,entryPremium,exitPremium,fees,notes,realizedOverride
```

| Column | Values |
| --- | --- |
| status | `Open` or `Closed` |
| account | Free text such as IRA, CASH, HSA, or ROTH. Blank is allowed. Shown on the position. |
| ticker | Such as `AAPL` or `BRK.B` |
| side | `Buy` or `Sell` |
| right | `Call` or `Put` |
| strike | Strike price in dollars. `11.0` is $11.00. Extra decimals are rounded half-up to the nearest cent. |
| openDate, expDate | `yyyy-MM-dd` |
| contracts | Whole number of contracts |
| entryPremium | Premium per share. Blank is not allowed. Extra decimals are rounded half-up to the nearest cent. |
| exitPremium | Premium per share. Blank is fine for an open trade. A closed trade needs this or `realizedOverride`. `0.0000` is a zero premium. Extra decimals are rounded half-up to the nearest cent. |
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

Positions live in a Room database on the device (`option_tracker.db`). There is no login. The account label from a CSV is stored with the trade. Theme choice is stored in DataStore.

The Positions list shows a delayed last price for each underlying stock, such as `NVDA  $178.42`, with a space before the Open badge. Tickers are requested once per unique symbol from Yahoo Finance’s public spark feed (`query1.finance.yahoo.com`). No API key is required. Prices are cached for about 90 seconds, and pull-to-refresh loads them again. The list footer says quotes are delayed 15+ minutes. If the phone is offline or the request fails, the position stays and the stock price is an em dash (—). Premium profit and loss is unchanged.

The same row keeps the entry premium and adds a delayed quote for that contract, such as `entry $1.80 · now $1.42`. The contract is the OCC symbol: root, expiration, call or put, and strike. `BRK.B` becomes `BRKB`. When bid and ask are both present the quote is their midpoint; otherwise it is the last price. Contracts are requested once. If that quote fails, the line stays `entry $1.80 · now —`. The stock price and ITM, ATM, and OTM chips are unchanged.

Next to that price, an open call or put is marked ITM, ATM, or OTM. ATM means the delayed stock price is within 0.5% of the strike, including an exact match. A call is ITM above that band and OTM below it. A put is the reverse. If the quote is missing, the moneyness chip is omitted. Chip color follows the side: a short ITM and a long OTM are red, a short OTM and a long ITM are green, and ATM is grey.

## Assigned puts

**Assigned** in the bottom bar lists shares from short puts that were assigned. Each row shows the ticker, cost basis (the put’s strike per share), the same delayed stock quote, the assigned date, the share count (contracts × 100), and unrealized P/L: `(quote − cost basis) × shares`. Pull down to refresh. If the quote is missing, the price and the P/L are an em dash. The footer says the quote is delayed.

On an open short put, **Assigned** closes the option at a $0 exit premium so the opening credit, minus fees, stays on that option. It does not mix stock profit into the option. The new lot is linked to that closed trade. Cash close is still available. A past assignment can be added by hand with a ticker, cost basis, shares or contracts, and date. Editing changes the cost basis, shares, and date. Deleting removes the stock lot only and does not reopen the option.

Call assignment, where shares are called away, is not in this version.

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
