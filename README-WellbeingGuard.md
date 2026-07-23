# Screen Time Guard (Wellbeing Guard)

An Android app that checks your daily screen time every half hour — the same data that the Digital Wellbeing app is based on — and if you exceed the limit you set, an alert pops up on your device
**and** a message is also sent to other devices you set.

## What's in the app

| Capability | Details |
|---|---|
| Screen time measurement | via `UsageStatsManager` (the source of Digital Wellbeing), from midnight until now |
| Periodic check | `WorkManager` — default 30 minutes (the minimum Android allows is 15) |
| Limit as a parameter | "Daily limit in hours" field in the app, also supports fractions (2.5) |
| Local alert | High priority notification |
| Alert to additional phones | SMS to defined numbers, and/or Telegram bot message |
| Spam prevention | "Notification interval" — no more than one notification in a set period of time |
| Auto-start | Continues to work after device restart |

Everything is saved locally on the device. There is no server and no data collection.

## How to build the APK

### Option A — without installing anything (GitHub Actions)
1. Open a new repo on GitHub and upload this folder to it.
2. **Actions** tab → The "Build APK" workflow runs automatically.
3. When finished, download the artifact named `wellbeing-guard-apk` — inside `app-debug.apk`.
4. Transfer to the phone, open, and confirm "Install from unknown sources"
### Option B — Android Studio
`File → Open` on the folder, then `Build → Build APK(s)` or run directly on a connected device.

### Option C — Command line
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

## First launch on the phone

1. **Allow access to usage data** — The button opens the Android settings screen;
Locate "Screen saver" and turn it on. Without this, it is impossible to measure screen time.
2. **Allow notifications and SMS**.
3. **Disable battery optimization** — Highly recommended, otherwise Android may reject the tests.
4. Fill in the limit, phone numbers, and turn on the **Active monitoring** switch.
5. It is worth clicking **Send test notification** to make sure that the messages reach the destination.

## Telegram instead of SMS (recommended)

Sending SMS requires a permission that Google is very restrictive about in the store, and messages cost money. Free alternative:

1. In Telegram, open a chat with `@BotFather` → `/newbot` → receive a **token**.
2. Paste the token in the `Telegram Bot Token` field.
3. Each recipient sends one message to the bot (required, otherwise the bot cannot contact them),
and finds their chat ID via `@userinfobot`.
4. Paste the IDs in the Chat IDs field, separated by a comma.

## Limitations worth knowing

- Android does not allow you to read the **settings** of Digital Wellbeing itself (the timers set there),
but it does allow you to read the **usage data** that it displays — and that's what the app does.
- `WorkManager` does not guarantee accuracy to the second; Testing may be delayed in Doze mode.
Disabling battery optimization almost always fixes this.
- This is a debug version signed by a developer — great for personal use, not for uploading to the store
