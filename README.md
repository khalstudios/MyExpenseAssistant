# Kahan Gaya Paisa

Kahan Gaya Paisa ("where did the money go?") is an Android app that auto-records UPI/card expenses by reading payment notifications (Google Pay, PhonePe, Paytm, BHIM) and bank SMS alerts. Transactions are categorised automatically and everything stays on-device.

## How it works

```
NotificationListenerService ─> PaymentTextParser ─> Categorizer ─> TransactionRepository ─> Room ─> Compose UI
                               (BankSmsParser for     (rules +          (dedupe)
                                SMS and bank apps)  learned rules)
```

| Layer | Location | Responsibility |
| --- | --- | --- |
| Capture | [PaymentNotificationListener.kt](app/src/main/java/com/khaltech/expenseassistant/service/PaymentNotificationListener.kt) | Reads notifications from whitelisted payment packages only |
| Parse | [PaymentTextParser.kt](app/src/main/java/com/khaltech/expenseassistant/parser/PaymentTextParser.kt) | Extracts amount, direction, merchant, UPI reference; rejects failed/pending/collect-request/promo text |
| Parse (bank alerts) | [BankSmsParser.kt](app/src/main/java/com/khaltech/expenseassistant/parser/bank/BankSmsParser.kt), [Banks.kt](app/src/main/java/com/khaltech/expenseassistant/parser/bank/Banks.kt) | SMS and bank-app notifications: identifies the bank from the sender header or its name, classifies the type, extracts account/card last 4 digits, counterparty, reference and balance |
| Categorise | [Categorizer.kt](app/src/main/java/com/khaltech/expenseassistant/categorize/Categorizer.kt) | User-taught rules → keyword knowledge base → structural heuristics, each with a confidence score |
| Store | [TransactionRepository.kt](app/src/main/java/com/khaltech/expenseassistant/data/repo/TransactionRepository.kt) | Deduplicates (UPI ref, or amount+direction+merchant within 3 min) and persists |
| UI | [HomeScreen.kt](app/src/main/java/com/khaltech/expenseassistant/ui/HomeScreen.kt) | Monthly totals, category breakdown, per-transaction category override |

### Bank SMS

Alerts reach the app as SMS-app notifications (Google Messages, Samsung Messages, the stock `com.android.mms`, Truecaller) or bank-app notifications. The app does not request the SMS permission. The notification title is the sender header, such as `VM-HDFCBK-S`, which identifies the bank; headers ending in `-P` (promotional) are ignored.

About 45 banks and card issuers are recognised, and the transaction type is classified as UPI, ATM withdrawal, card swipe, online card payment, NEFT, IMPS, RTGS, auto-debit (NACH/ECS/mandate), salary, interest, bank charges, refund/reversal, cash deposit or cheque. The bank, account or card last 4 digits, type and balance are stored with the transaction and shown under **Metadata** on its detail screen.

Not recorded: OTPs, failed, declined or pending transactions, future debits ("will be debited"), statements and due reminders, collect requests, mandate setup, and promotions. Credit card bill payments are recognised and skipped on both sides (the bank debit and the card's "payment received"), because the card spends were already recorded one by one.

A message with no bank, account or UPI detail falls back to the general parser, so wallet and merchant SMS still work.

### The "intelligence"

Categorisation is a layered classifier rather than a single lookup:

1. **Learned rules** — every time you correct a category or rename a captured merchant, the normalised merchant key (`Swiggy Private Limited` → `swiggy`) is stored in `merchant_rules` and wins next time. A saved rename becomes the display name for future matching payments, and the tags you save are carried forward too; category rules have confidence `0.99`. Notes are never copied to later payments, and income transactions neither create nor use learned rules.
2. **Knowledge base** — ~250 merchant/keyword patterns across 15 categories, longest match first, checked against the merchant field before the raw text. Confidence `0.6–0.95`.
3. **Heuristics** — credits default to income; payments to a personal VPA or a 1–3 word personal name become `Transfer to People`. Confidence `0.55–0.6`.

Anything under `0.6` confidence is surfaced as "needs a category check" on the home screen, so corrections feed straight back into layer 1.

## Build

Prerequisites: Android Studio (Narwhal or newer), JDK 17, Android SDK 36.

```powershell
# Generate the Gradle wrapper once (Android Studio also does this on first open)
gradle wrapper --gradle-version 8.13

./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

## Enabling capture on device

1. Install and open the app.
2. Tap **Enable** next to *Notification access* → toggle "Kahan Gaya Paisa" in the system list.
3. Make a UPI payment. It appears within a second or two.

Notification access covers GPay, PhonePe, Paytm and bank SMS. There is no screen reading or accessibility service; a payment that posts neither an app notification nor a bank SMS can be added with **+**.

Automatic capture stores every completed payment notification it can parse, even when the merchant or category is unavailable or inaccurate. In those cases, the source app is used as the merchant name and the transaction can be corrected later.

### Contact names

In **Account** under **Capture**, enable *Contact names* to let the app match the merchant name parsed from newly captured payments to a similar phone-contact name. The app does not inspect phone numbers in notifications. A contact name is used only when it is the clear match; it becomes the transaction merchant while the original parsed counterparty is retained in Notes. Each result, including no match, is cached locally by merchant name so the phone contacts provider is not queried again for repeat payments. Contact access is optional and contact data is only read on-device during the first lookup.

### Backups

In **Account** under **Your data**, select **Back up your data** and choose Google Drive (or another storage provider) in Android's file picker. The backup contains your transactions, budgets, learned merchant categories, profile, and category icon choices. **Restore from backup** replaces those items currently stored on the device, so make a current backup first when needed.

You can also enable **Automatic backups** once, choose either every 15 days or monthly, then select a Google Drive folder. The app retains access only to that selected folder and creates future dated backup files there without asking again. Android may delay scheduled work for battery, storage, or connectivity reasons, so backups run approximately at the selected interval. Turn off automatic backups at any time from Account. The app does not store Google account credentials.

## Privacy

The full privacy policy is [docs/privacy-policy/index.md](docs/privacy-policy/index.md), published by GitHub Pages (source: the `docs/` folder) at https://khalstudios.github.io/MyExpenseAssistant/privacy-policy/. That URL is the one to give Play Console, and the in-app Privacy dialog links to it.

- No internet permission is declared. Data leaves the device only when you explicitly save a backup or CSV through Android's document picker, such as to Google Drive.
- Only packages listed in [PaymentApps.kt](app/src/main/java/com/khaltech/expenseassistant/parser/PaymentApps.kt) are read; every other notification is discarded before parsing.
- No accessibility service and no SMS permission: bank SMS are read only from the notifications messaging apps post.
- Contacts are accessed only after the user enables the optional *Contact names* permission in Account.

## Play Store note

Notification listeners are policy-sensitive. If you publish this, declare the notification access use in Play Console, and keep the in-app disclosure shown before the system settings screen opens. The same build is uploaded to Play and shared as an APK; `scripts/release.sh` produces both.

## Extending

- **More apps**: add the package to `PaymentApps.known`, and to `bankAlertPackages` if it is an SMS or bank app.
- **More merchants**: add keywords to `MerchantKeywords.rules`.
- **New text formats**: add a regex to `PaymentTextParser.MERCHANT_PATTERNS` and a case to [PaymentTextParserTest.kt](app/src/test/java/com/khaltech/expenseassistant/parser/PaymentTextParserTest.kt).
- **More banks or sender headers**: add a `Bank` to `Banks.all` with its headers and the names it signs messages with.
- **A bank alert parsed wrongly**: paste the real message, with account numbers masked, as a case in [BankSmsParserTest.kt](app/src/test/java/com/khaltech/expenseassistant/parser/bank/BankSmsParserTest.kt), then adjust the pattern in `BankSmsParser`.
