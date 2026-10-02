# Kahan Gaya Paisa

Kahan Gaya Paisa ("where did the money go?") is an Android app that auto-records UPI/card expenses by reading payment notifications (Google Pay, PhonePe, Paytm, BHIM) and bank SMS alerts. Transactions are categorised automatically and everything stays on-device.

## How it works

```
NotificationListenerService -> PaymentTextParser -> Categorizer -> TransactionRepository -> Room -> Compose UI
                               (BankSmsParser for     (rules +          (dedupe)
                                SMS and bank apps)  learned rules)
```

| Layer | Location | Responsibility |
| --- | --- | --- |
| Capture | [PaymentNotificationListener.kt](app/src/main/java/com/khaltech/expenseassistant/service/PaymentNotificationListener.kt) | Reads notifications from whitelisted payment packages only |
| Parse | [PaymentTextParser.kt](app/src/main/java/com/khaltech/expenseassistant/parser/PaymentTextParser.kt) | Extracts amount, direction, merchant, UPI reference; rejects failed/pending/collect-request/promo text |
| Parse (bank alerts) | [BankSmsParser.kt](app/src/main/java/com/khaltech/expenseassistant/parser/bank/BankSmsParser.kt), [Banks.kt](app/src/main/java/com/khaltech/expenseassistant/parser/bank/Banks.kt) | SMS and bank-app notifications: identifies the bank from the sender header or its name, classifies the type, extracts account/card last 4 digits, counterparty, reference and balance |
| Categorise | [Categorizer.kt](app/src/main/java/com/khaltech/expenseassistant/categorize/Categorizer.kt) | User-taught rules → keyword knowledge base → structural heuristics, each with a confidence score |
| Store | [TransactionRepository.kt](app/src/main/java/com/khaltech/expenseassistant/data/repo/TransactionRepository.kt), [CaptureDedupe.kt](app/src/main/java/com/khaltech/expenseassistant/data/repo/CaptureDedupe.kt) | Deduplicates (see below) and persists |
| UI | [HomeScreen.kt](app/src/main/java/com/khaltech/expenseassistant/ui/HomeScreen.kt), [InsightsScreen.kt](app/src/main/java/com/khaltech/expenseassistant/ui/insights/InsightsScreen.kt) | Today card and latest activity, insights, budgets, per-transaction corrections |

### Deduplication

One payment usually reaches the app twice: from the UPI app, which states no account, and as a bank alert, which does. A capture is treated as a second sighting when it shares a UPI reference with a stored payment, or has the same amount and direction within 3 minutes of when it happened or 5 minutes of when it arrived, unless the two name a different account, bank or reference. So the UPI app's copy and the bank alert merge, with the alert's bank, account and reference filled into the stored payment, while ₹1 sent from two accounts a minute apart is recorded twice. Two payments seen only as UPI app notifications, with no reference or account, cannot be told apart and are recorded once.

### Bank SMS

Alerts reach the app as SMS-app notifications (Google Messages, Samsung Messages, the stock `com.android.mms`, Truecaller) or bank-app notifications. The app does not request the SMS permission. The notification title is the sender header, such as `VM-HDFCBK-S`, which identifies the bank; headers ending in `-P` (promotional) are ignored.

About 45 banks and card issuers are recognised, and the transaction type is classified as UPI, ATM withdrawal, card swipe, online card payment, NEFT, IMPS, RTGS, auto-debit (NACH/ECS/mandate), salary, interest, bank charges, refund/reversal, cash deposit or cheque. The bank, account or card last 4 digits, type and balance are stored with the transaction and shown under **Metadata** on its detail screen.

Not recorded: OTPs, failed, declined or pending transactions, future debits ("will be debited"), statements and due reminders, collect requests, mandate setup, and promotions. Credit card bill payments are recognised and skipped on both sides (the bank debit and the card's "payment received"), because the card spends were already recorded one by one.

A message with no bank, account or UPI detail falls back to the general parser, so wallet and merchant SMS still work.

### The "intelligence"

Categorisation is a layered classifier rather than a single lookup:

1. **Learned rules**: every time you correct a category or rename a captured merchant, the normalised merchant key (`Swiggy Private Limited` → `swiggy`) is stored in `merchant_rules` and wins next time. A saved rename becomes the display name for future matching payments, and the tags you save are carried forward too; category rules have confidence `0.99`. Notes are never copied to later payments, choosing one of your own categories teaches nothing, and income transactions neither create nor use learned rules.
2. **Knowledge base**: ~290 merchant/keyword patterns across 15 categories, longest match first, checked against the merchant field before the raw text. Confidence `0.6–0.95`. The House Expense and Vehicle Expense keywords (plumbers, garages) apply only with Pro; without it those payments go to Bills & Utilities and Transport.
3. **Heuristics**: credits default to income; payments to a personal VPA or a 1–3 word personal name become `People`. Confidence `0.55–0.6`.

Anything under `0.6` confidence that you have not corrected is collected under **Needs review**, so corrections feed straight back into layer 1.

Renaming a captured payment keeps the name it arrived with: its learned rules keep matching the original merchant, and the detail screen shows that name as **Original name** under **Metadata**. A manual entry's name was typed in the first place, so renaming one replaces it.

**Profile > Organise > Merchants** lists everyone you have paid, most-paid first, grouped by the same merchant key, and opens every payment made to each.

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

Notification access covers GPay, PhonePe, Paytm and bank SMS. WhatsApp notifications are never read, so chat messages can't be mistaken for payments; a WhatsApp Pay payment is still recorded from your bank's SMS. There is no screen reading or accessibility service; a payment that posts neither an app notification nor a bank SMS can be added with **+**.

Automatic capture stores every completed payment notification it can parse, even when the merchant or category is unavailable or inaccurate. In those cases, the source app is used as the merchant name and the transaction can be corrected later.

### Contact names

In **Profile** under **Capture**, enable *Contact names* to let the app match the merchant name parsed from newly captured payments to a similar phone-contact name. The app does not inspect phone numbers in notifications. A contact name is used only when it is the clear match; it becomes the transaction merchant while the original parsed counterparty is kept as the payment's original name. Each result, including no match, is cached locally by merchant name so the phone contacts provider is not queried again for repeat payments. Contact access is optional and contact data is only read on-device during the first lookup.

### Backups

In **Profile** under **Backup**, select **Back up your data**. A dialog explains what happens next, then Android's picker asks only for a location: Google Drive, this device, or any other storage provider. The app creates a folder named `Kahan Gaya Paisa` there and saves the dated backup file inside it. That location is remembered, so later backups go straight into the same folder without opening the picker again; **Change location** in the dialog picks a different one. The backup contains your transactions, learned merchant rules, budgets, recurring payments you added or removed, profile, and category icon and colour choices. Files are named `18-09-2026-kahan-gaya-paisa-backup-1430.json`: date, app name, then the time, so two backups on the same day do not collide.

**Restore from backup** lists the backups already in that folder, newest first, so restoring is a tap; **Pick a file instead** falls back to Android's file picker for a backup kept somewhere else. Restoring replaces those items currently stored on the device, so make a current backup first when needed.

With Pro, you can also enable **Automatic backups** once, choose daily, weekly, every 2 weeks, or monthly, then pick a location the same way. The app retains access only to the location you selected, and writes future dated backups into its own `Kahan Gaya Paisa` folder there without asking again. Android may delay scheduled work for battery, storage, or connectivity reasons, so backups run approximately at the selected interval. The first one runs one interval after you turn them on, not straight away. Turn off automatic backups at any time from Profile; they also stop if Pro lapses. Backing up and restoring by hand always stay free. The app does not store Google account credentials.

## Pro

Free covers everything you record and everything you can read about it: capture, categorising, every transaction behind a category, tag or merchant, insights, budgets, tagging with up to five tags of your own and the tag analytics card, two categories of your own, manual backup and restore, and CSV export. Pro, a one-time unlock or a yearly plan, is room to organise plus the analysis on top:

- **Unlimited categories of your own**, named, coloured and given an icon how you like. The first two are free.
- **Ten more built-in categories**: House Expense, Vehicle Expense, Taxes, Friends / Relatives, Family, Personal Care, Hobbies, Insurance, Gifts and Donations. With Pro, house and vehicle payments are also filed automatically.
- **More than five tags of your own**, all of them charted.
- **Recurring payments**: the ones found from three payments at a steady interval, and your own entries for rent, subscriptions and anything paid outside the phone.
- **Automatic backups** on a schedule.
- **Momentum comparisons**: the Momentum card on Insights can lay each of the six weeks, months or years before the one on screen over it, each as its own line and bars. Comparing with the one just before stays free.

Nothing is taken away by this split. The categoriser never files a free user's payment under a Pro category: most have no keyword rules at all, and the House Expense and Vehicle Expense rules fall back to a free category without Pro, so the precise filing is part of what Pro sells rather than a category that would appear on its own. Any category already on your transactions or budgets remains available to pick whatever your entitlement, and custom categories you created keep working if Pro lapses; only creating a new one past the free two is gated.

Everything gated lives behind [ProLock.kt](app/src/main/java/com/khaltech/expenseassistant/ui/pro/ProLock.kt) and [ProState.kt](app/src/main/java/com/khaltech/expenseassistant/ui/pro/ProState.kt), so the pitch and the limits are defined once.

## Privacy

The full privacy policy is [docs/privacy-policy/index.md](docs/privacy-policy/index.md), published by GitHub Pages (source: the `docs/` folder) at https://khalstudios.github.io/MyExpenseAssistant/privacy-policy/. That URL is the one to give Play Console, and the in-app Privacy dialog links to it.

- The `INTERNET` permission is merged in by the Play Billing library (via its `transport-backend-cct` dependency), not declared by the app, and no app code opens a network connection. Data leaves the device only when you explicitly save a backup or CSV through Android's document picker, such as to Google Drive.
- Only packages listed in [PaymentApps.kt](app/src/main/java/com/khaltech/expenseassistant/parser/PaymentApps.kt) are read; every other notification is discarded before parsing.
- No accessibility service and no SMS permission: bank SMS are read only from the notifications messaging apps post.
- Contacts are accessed only after the user enables the optional *Contact names* permission in Profile.

## Play Store note

Notification listeners are policy-sensitive. If you publish this, declare the notification access use in Play Console, and keep the in-app disclosure shown before the system settings screen opens. The same build is uploaded to Play and shared as an APK; `scripts/release.sh` produces both.

## Extending

- **More apps**: add the package to `PaymentApps.known`, and to `bankAlertPackages` if it is an SMS or bank app.
- **More merchants**: add keywords to `MerchantKeywords.rules`. A keyword for a Pro category also needs a free stand-in in `MerchantKeywords.freeFallback`; `MerchantKeywordsTest` fails otherwise.
- **New text formats**: add a regex to `PaymentTextParser.MERCHANT_PATTERNS` and a case to [PaymentTextParserTest.kt](app/src/test/java/com/khaltech/expenseassistant/parser/PaymentTextParserTest.kt).
- **More banks or sender headers**: add a `Bank` to `Banks.all` with its headers and the names it signs messages with.
- **A bank alert parsed wrongly**: paste the real message, with account numbers masked, as a case in [BankSmsParserTest.kt](app/src/test/java/com/khaltech/expenseassistant/parser/bank/BankSmsParserTest.kt), then adjust the pattern in `BankSmsParser`.
