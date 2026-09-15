# Privacy Policy — Kahan Gaya Paisa

**Effective date:** 15 September 2026

Kahan Gaya Paisa records your spending on your own phone. This policy explains what the app
reads, what it stores, and what leaves your device.

## The short version

**No data is collected, transmitted, sold or shared.** The app does not request the Android
`INTERNET` permission, so it is technically incapable of sending your information anywhere. There
are no ads, no analytics, no crash reporting and no third-party tracking SDKs. The developer never
receives any of your data and has no way to see it.

## What the app accesses

Each of the following is disclosed inside the app before you are asked to grant it, and can be
revoked at any time in Android Settings. The app still works without them; you add transactions by
hand instead.

| Access | What is read | Why |
| --- | --- | --- |
| Notification access | Notifications posted by payment apps (such as Google Pay, PhonePe, Paytm, BHIM), banking apps, and messaging apps (such as Google Messages, Samsung Messages and Truecaller) | To record payments automatically from payment alerts and bank SMS, instead of you typing them in |
| Contacts (optional) | Contact names only | So a transfer shows a person's name instead of a UPI ID or account name |
| Notifications (optional) | Nothing is read | To show a brief, silent alert each time a payment is recorded, and to warn you when spending nears or crosses a budget you set. On the lock screen the alert shows only the app name and "Transaction recorded", never the amount or payee |

### How notifications are handled

- Notifications from any app not on the fixed list of payment, banking and messaging apps are
  ignored without being read.
- Notifications from listed apps are checked on your phone, in memory, for a completed payment or
  bank transaction. Anything else, including your personal messages, OTPs, promotions, reminders
  and failed or pending payments, is discarded immediately and never stored.
- When a notification is a completed transaction, the app keeps the details listed in the next
  section.

The app does not use an accessibility service and does not read what is on your screen. It does
not request the SMS permission and cannot open your message inbox: a bank SMS is read only from the
notification your messaging app posts when it arrives.

### How contacts are used

When contact access is on, the app searches your contacts for a name that closely matches the payee
of a newly recorded transaction. Only contact names are read; phone numbers, email addresses and
other contact details are not. The matched name, or the fact that there was no match, is saved
alongside that payee name so the same contact search is not repeated.

## What the app stores, and where

All of it is stored on your device, in the app's private storage:

- **Transactions:** amount, direction (spent or received), merchant or payee, category, payment
  mode, date and time, the text of the notification or SMS it was recorded from, and any note or
  tags you add
- **Bank alert details, when the alert states them:** bank name, account or card type, the last 4
  digits of the account or card number, transaction type (for example UPI, NEFT or ATM), reference
  number, and the balance or available limit the alert reported. Full account and card numbers are
  never read or stored; alerts show them masked.
- **Learning:** categories, names and tags the app has learned from your corrections, and contact
  name matches
- **Budgets** and budget alert history
- **Profile details you enter:** name, email and monthly income
- **Settings**, such as your automatic backup schedule and category icon choices

Android's own cloud backup is switched off for this app, so this data is not copied to your Google
account automatically.

## Deleting your data

- **Profile › Your data › Delete all transactions** erases every recorded transaction. You can
  choose to also clear learned categories, contact name matches and budgets.
- Uninstalling the app deletes everything it stored on the phone.
- Backup and CSV files you saved yourself are not affected by either; delete them where you saved
  them.

## Backups and exports

The app can write a backup file or a CSV export to a folder you choose, either when you ask or on
an automatic schedule you set up. Those files contain your transactions and the details above, and
they leave the app's private storage only by your action.

If you choose a folder that is synchronised to a cloud service — Google Drive, for example — that
copy of your data is then handled by that service under its own terms and privacy policy. The app
itself never uploads anything. For automatic backups, the app keeps access only to the one folder
you picked, and you can turn automatic backups off at any time.

## Other permissions

The app's background-work library, used to run automatic backups on schedule, declares permissions
to keep the device awake briefly, restart scheduled work after a reboot, and check network state.
None of them allows the app to send data anywhere.

## Children

The app is not directed at children and does not knowingly collect information from them.

## Changes to this policy

If this policy changes, the effective date above will be updated and the revised policy will be
published at the same location.

## Contact

Questions about this policy or your data: **shardulmane3@gmail.com**
