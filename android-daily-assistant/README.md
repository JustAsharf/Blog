# Daily Assistant — offline money & reminders app for Android

A single-purpose Android app for the day-to-day stuff: **what your salary allows you to
spend today**, **what you actually spent**, **what bills are coming**, and **reminders that
fire on time**.

Everything runs **on the phone**. There is no server, no account, no sign-in, and the app
does not even request the `INTERNET` permission — so it cannot phone home even by accident.
Your data lives in a local SQLite (Room) database and is yours to export whenever you want.

---

## Getting the APK

**Option 1 — download the build (no tools needed)**

1. Open the repository's **Actions** tab on GitHub.
2. Pick the most recent **Build Daily Assistant APK** run for this branch.
3. Download the **daily-assistant-apk** artifact and unzip it.
4. Copy `daily-assistant.apk` to your phone and tap it. Android will ask you to allow
   installing from this source — that is normal for an app not coming from Play.

**Option 2 — build it yourself**

```bash
cd android-daily-assistant
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Requirements: JDK 17 and the Android SDK (Android Studio installs both). `minSdk 26`, so it
runs on Android 8.0 and newer.

The budgeting rules, money parsing, pay-cycle dates, reminder scheduling and insight rules are
covered by 32 JVM unit tests that need no device or emulator:

```bash
./gradlew testDebugUnitTest
```

> The CI build is signed with a debug key, which is regenerated on every run. If you install a
> newer build over an older one and Android complains about signatures, uninstall the old copy
> first. For a stable upgrade path, add a release keystore as a repository secret and sign the
> release variant — see *Signed release builds* below.

---

## What it does

### Salary and the daily allowance
Enter your take-home salary and the day of the month you get paid. The app works in **pay
cycles** (payday → day before the next payday), not calendar months, and gives you one number
that matters: **safe to spend today**.

```
spendable  = salary + extra income − savings share − bills not yet paid
allowance  = (spendable − spent earlier in the cycle) ÷ days left in the cycle
today left = allowance − spent today
```

* **Save first, spend after** — reserve a percentage of the salary (default 10%) before any
  daily budget is computed.
* **Carry-over** — a quiet day today makes tomorrow's allowance bigger. Can be switched off
  for a flat daily budget.

### Spending
* Two-tap logging: amount, category, save. Quick `+5 / +10 / +20 / +50 / +100` buttons.
* Money-in entries too (bonus, freelance, refunds) — they raise the cycle's spendable total.
* Payment method (cash, card, transfer, wallet), note, and any date — including backdating.
* Edit or delete anything later; history is grouped by day, month by month.

### Bills and subscriptions
Rent, internet, phone, gym. Each bill has a due day, an amount and a "warn me N days before".
Unpaid bills are **subtracted from your spendable money** so the daily allowance never lies to
you. "Mark paid" books the expense in one tap.

### Reminders
Local reminders on `WorkManager`, so they survive reboots and app kills with no push service:

* one-off, daily, weekly (pick the weekdays) or monthly,
* a built-in nightly nudge to log the day's spending (time adjustable, or off),
* an automatic daily check that warns about bills due or already late.

### Savings goals
Target, optional deadline, progress bar, and the per-day amount needed to make the deadline.
Contributions are logged as spending so the budget stays honest.

### Debts
Who owes you, who you owe, with optional due dates and a settled/reopen switch.

### Reports
* Category donut plus a ranked breakdown for any month.
* 30-day daily-spending bars, 7-day bars on the dashboard.
* Spend-by-payment-method split.
* **CSV export** of every entry, through the system file picker.

### Insights (on-device, rule-based)
Plain arithmetic over your own data — no model, no network:

* how much is still safe to spend today,
* your biggest category and its share of the cycle,
* burn-rate projection: *"at this pace you end the cycle X over budget — cut Y per day"*,
* this week vs last week,
* no-spend-day count,
* biggest single expense,
* bills landing within a week,
* goal pacing, outstanding debts, average day, days to payday.

### Privacy and the data itself
* **App lock**: optional 4–8 digit PIN, stored only as a SHA-256 hash on the device.
* **Backup / restore**: a single JSON file written wherever you choose through the system file
  picker. Restoring replaces the current contents. This is also how you move to a new phone —
  because nothing is kept anywhere but the phone, there is no cloud copy to fall back on.

---

## Project layout

```
android-daily-assistant/
├── app/src/main/java/com/dailyassistant/
│   ├── DailyApp.kt            Application: DB, notification channel, reschedules reminders
│   ├── MainActivity.kt        Single activity, Compose entry point
│   ├── data/                  Room entities, DAOs, repository, settings, budget maths, insights
│   ├── ui/                    Compose screens, theme, charts, shared components
│   ├── util/                  Money (minor units) and date helpers
│   ├── vm/MainViewModel.kt    One ViewModel, Flow-backed state
│   └── work/                  Notifications, WorkManager workers, scheduling
└── app/src/main/res/          Theme, adaptive launcher icon, backup rules
```

**Stack:** Kotlin 2.0, Jetpack Compose (Material 3), Room, WorkManager, Navigation Compose.
No third-party libraries beyond AndroidX.

**Money is never a `Double`** — amounts are `Long` minor units (cents/piastres) so totals never
drift. Dates are stored as epoch-days, timestamps as epoch-millis.

---

## Signed release builds

The CI job builds the debug variant, which installs directly. For a stable signature across
updates:

1. Create a keystore: `keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias daily`
2. Add `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` as repository secrets.
3. Add a `signingConfigs` block to `app/build.gradle.kts` reading those values from the
   environment, and switch the workflow to `assembleRelease`.

---

## Ideas not built yet

Deliberately left out to keep the first version honest and offline:

* Arabic (and RTL) translation — the layouts already support RTL, only the strings are English.
* Home-screen widget showing "safe to spend today".
* A quick-add tile in the notification shade.
* Receipt photos attached to an entry (local storage only).
* Recurring income beyond the monthly salary.
* Biometric unlock in addition to the PIN.
* Multi-currency entries with a manually entered rate.
* Week-based cycles for people paid weekly.
* Category budget notifications ("80% of the Food budget is gone").
* Encrypted backup file (passphrase-protected export).
