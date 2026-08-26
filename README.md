# Expense Assistant

Android app that auto-records UPI/card expenses by reading payment notifications (Google Pay, PhonePe, Paytm, BHIM, bank SMS) and, as a fallback, payment success screens. Transactions are categorised automatically and everything stays on-device.

## How it works

```
NotificationListenerService ─┐
                             ├─> PaymentTextParser ─> Categorizer ─> TransactionRepository ─> Room ─> Compose UI
AccessibilityService ────────┘        (regex)         (rules +          (dedupe)
                                                    learned rules)
```

| Layer | Location | Responsibility |
| --- | --- | --- |
| Capture | [PaymentNotificationListener.kt](app/src/main/java/com/expenseassistant/service/PaymentNotificationListener.kt) | Reads notifications from whitelisted payment packages only |
| Capture (fallback) | [PaymentScreenAccessibilityService.kt](app/src/main/java/com/expenseassistant/service/PaymentScreenAccessibilityService.kt) | Scrapes "Payment successful" screens when no notification is posted |
| Parse | [PaymentTextParser.kt](app/src/main/java/com/expenseassistant/parser/PaymentTextParser.kt) | Extracts amount, direction, merchant, UPI reference; rejects failed/pending/collect-request/promo text |
| Categorise | [Categorizer.kt](app/src/main/java/com/expenseassistant/categorize/Categorizer.kt) | User-taught rules → keyword knowledge base → structural heuristics, each with a confidence score |
| Store | [TransactionRepository.kt](app/src/main/java/com/expenseassistant/data/repo/TransactionRepository.kt) | Deduplicates (UPI ref, or amount+direction+merchant within 3 min) and persists |
| UI | [HomeScreen.kt](app/src/main/java/com/expenseassistant/ui/HomeScreen.kt) | Monthly totals, category breakdown, per-transaction category override |

### The "intelligence"

Categorisation is a layered classifier rather than a single lookup:

1. **Learned rules** — every time you correct a category, the normalised merchant key (`Swiggy Private Limited` → `swiggy`) is stored in `merchant_rules` and wins next time. Confidence `0.99`.
2. **Knowledge base** — ~250 merchant/keyword patterns across 15 categories, longest match first, checked against the merchant field before the raw text. Confidence `0.6–0.95`.
3. **Heuristics** — credits default to income; payments to a personal VPA or a 1–3 word personal name become `Transfer to People`. Confidence `0.55–0.6`.

Anything under `0.6` confidence is surfaced as "needs a category check" on the home screen, so corrections feed straight back into layer 1.

## Build

Prerequisites: Android Studio (Ladybug or newer), JDK 17, Android SDK 35.

```powershell
# Generate the Gradle wrapper once (Android Studio also does this on first open)
gradle wrapper --gradle-version 8.9

./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

## Enabling capture on device

1. Install and open the app.
2. Tap **Enable** next to *Notification access* → toggle "Expense Assistant" in the system list.
3. Optionally tap **Enable** next to *Screen reading* → Settings ▸ Accessibility ▸ Installed apps ▸ Expense Assistant.
4. Make a UPI payment. It appears within a second or two.

The accessibility path is only needed for apps that show a success screen but post no notification. Notification access alone covers GPay, PhonePe, Paytm and bank SMS.

## Privacy

- No internet permission is declared — data cannot leave the device.
- Only packages listed in [PaymentApps.kt](app/src/main/java/com/expenseassistant/parser/PaymentApps.kt) are read; every other notification is discarded before parsing.
- The accessibility service is scoped via `android:packageNames` to four UPI apps and only acts on screens containing success wording.

## Play Store note

Accessibility services and notification listeners are policy-sensitive. If you publish this, you must complete the **Permissions Declaration Form**, and the accessibility service must be presented as an optional, clearly-explained feature (as it is here) rather than a requirement.

## Extending

- **More apps**: add the package to `PaymentApps.known`, and to `accessibility_service_config.xml` if screen scanning is needed.
- **More merchants**: add keywords to `MerchantKeywords.rules`.
- **New text formats**: add a regex to `PaymentTextParser.MERCHANT_PATTERNS` and a case to [PaymentTextParserTest.kt](app/src/test/java/com/expenseassistant/parser/PaymentTextParserTest.kt).
