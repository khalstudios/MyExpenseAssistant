package com.khaltech.expenseassistant.parser.bank

import com.khaltech.expenseassistant.data.model.AccountType
import com.khaltech.expenseassistant.data.model.Direction
import com.khaltech.expenseassistant.data.model.TransactionType
import com.khaltech.expenseassistant.parser.PaymentTextParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

/**
 * Alerts in the shape each bank sends them. Account numbers, references and names are made up;
 * the wording, punctuation and line breaks are what the parser has to cope with.
 */
class BankSmsParserTest {

    private fun parsed(body: String, sender: String? = null): BankTransaction =
        when (val result = BankSmsParser.parse(body, sender)) {
            is BankSmsResult.Transaction -> result.transaction
            else -> fail("Expected a transaction but got $result for: $body") as Nothing
        }

    private fun assertNotATransaction(body: String, sender: String? = null) {
        assertEquals(body, BankSmsResult.NotATransaction, BankSmsParser.parse(body, sender))
    }

    // ---- UPI ----

    @Test
    fun `hdfc multiline upi debit`() {
        val tx = parsed(
            "Sent Rs.500.00\nFrom HDFC Bank A/C *1234\nTo SWIGGY\nOn 12/09/26\nRef 425612345678\nNot You?\n" +
                "Call 18002586161/SMS BLOCK UPI to 7308080808",
            sender = "VM-HDFCBK-S",
        )
        assertEquals(50000L, tx.amountMinor)
        assertEquals(Direction.DEBIT, tx.direction)
        assertEquals(TransactionType.UPI, tx.type)
        assertEquals("HDFC Bank", tx.bank?.name)
        assertEquals(AccountType.BANK_ACCOUNT, tx.accountType)
        assertEquals("1234", tx.accountLast4)
        assertEquals("SWIGGY", tx.counterparty)
        assertEquals("425612345678", tx.referenceId)
    }

    @Test
    fun `hdfc upi credit from a vpa`() {
        val tx = parsed(
            "Credit Alert!\nRs.1,500.00 credited to HDFC Bank A/c XX1234 on 12-09-26 from VPA rahul.s@oksbi (UPI 425612345678)",
            sender = "AD-HDFCBK-S",
        )
        assertEquals(150000L, tx.amountMinor)
        assertEquals(Direction.CREDIT, tx.direction)
        assertEquals(TransactionType.UPI, tx.type)
        assertEquals("rahul.s@oksbi", tx.counterparty)
        assertEquals("425612345678", tx.referenceId)
    }

    @Test
    fun `sbi upi debit with no currency before the amount`() {
        val tx = parsed(
            "Dear UPI user A/C X2719 debited by 5000.00 on date 10Sep26 trf to Indian Clearing " +
                "Refno 625329079506 If not u? call-1800111109 for other services-18001234-SBI",
            sender = "JD-SBIUPI-S",
        )
        assertEquals(500000L, tx.amountMinor)
        assertEquals(Direction.DEBIT, tx.direction)
        assertEquals(TransactionType.UPI, tx.type)
        assertEquals("State Bank of India", tx.bank?.name)
        assertEquals("2719", tx.accountLast4)
        assertEquals("Indian Clearing", tx.counterparty)
        assertEquals("625329079506", tx.referenceId)
    }

    @Test
    fun `sbi upi credit`() {
        val tx = parsed("Dear SBI UPI User, ur A/cX1234 credited by Rs500 on 12Sep26 by (Ref no 425612345678)")
        assertEquals(50000L, tx.amountMinor)
        assertEquals(Direction.CREDIT, tx.direction)
        assertEquals(TransactionType.UPI, tx.type)
        assertEquals("1234", tx.accountLast4)
        assertEquals("425612345678", tx.referenceId)
    }

    @Test
    fun `axis multiline upi debit naming the payee in the upi path`() {
        val tx = parsed(
            "INR 1355.00 debited\nA/c no. XX8840\n13-09-26, 22:14:23\nUPI/P2M/662291936201/Mahi dhaba\n" +
                "Not you? SMS BLOCKUPI Cust ID to 919951860002\nAxis Bank",
            sender = "AX-AXISBK-S",
        )
        assertEquals(135500L, tx.amountMinor)
        assertEquals(TransactionType.UPI, tx.type)
        assertEquals("Axis Bank", tx.bank?.name)
        assertEquals("8840", tx.accountLast4)
        assertEquals("Mahi dhaba", tx.counterparty)
        assertEquals("662291936201", tx.referenceId)
    }

    @Test
    fun `icici upi debit naming the payee as credited`() {
        val tx = parsed(
            "ICICI Bank Acct XX245 debited for Rs 55.00 on 03-Sep-26; BOTTLE LAB TECH credited. " +
                "UPI:061447710011. Call 18002662 for dispute. SMS BLOCK 245 to 9215676766.",
        )
        assertEquals(5500L, tx.amountMinor)
        assertEquals(TransactionType.UPI, tx.type)
        assertEquals("ICICI Bank", tx.bank?.name)
        assertEquals("245", tx.accountLast4)
        assertEquals("BOTTLE LAB TECH", tx.counterparty)
        assertEquals("061447710011", tx.referenceId)
    }

    @Test
    fun `kotak upi debit and credit`() {
        val debit = parsed(
            "Sent Rs.250.00 from Kotak Bank AC X1234 to swiggy@icici on 12-09-26.UPI Ref 425612345678. Not you, kotak.com/fraud",
        )
        assertEquals(Direction.DEBIT, debit.direction)
        assertEquals("Kotak Mahindra Bank", debit.bank?.name)
        assertEquals("swiggy@icici", debit.counterparty)
        assertEquals("425612345678", debit.referenceId)

        val credit = parsed("Received Rs.1,000.00 in your Kotak Bank AC X1234 from rahul@okaxis on 12-09-26.UPI Ref:425612345679.")
        assertEquals(Direction.CREDIT, credit.direction)
        assertEquals(100000L, credit.amountMinor)
        assertEquals("rahul@okaxis", credit.counterparty)
    }

    @Test
    fun `pnb upi debit reads the amount before the balance`() {
        val tx = parsed(
            "A/c XX1234 debited INR 500.00 Dt 12-09-26 14:22:10 thru UPI:425612345678.Bal INR 5,000.00 CR " +
                "Not u?Fwd this SMS to 9264092640 to block UPI.-PNB",
            sender = "BP-PNBSMS-S",
        )
        assertEquals(50000L, tx.amountMinor)
        assertEquals(500000L, tx.availableBalanceMinor)
        assertEquals(Direction.DEBIT, tx.direction)
        assertEquals(TransactionType.UPI, tx.type)
        assertEquals("Punjab National Bank", tx.bank?.name)
        assertNull(tx.counterparty)
        assertEquals("425612345678", tx.referenceId)
    }

    @Test
    fun `bank of baroda dr and cr in one alert is a debit`() {
        val tx = parsed(
            "Rs.500.00 Dr. from A/C XXXXXX1234 and Cr. to swiggy@axisbank. Ref:425612345678. " +
                "AvlBal:Rs10,000.00(2026:09:12 14:22:10). Not you? Call 18005700 -BOB",
            sender = "VM-BOBTXN-S",
        )
        assertEquals(Direction.DEBIT, tx.direction)
        assertEquals(TransactionType.UPI, tx.type)
        assertEquals("Bank of Baroda", tx.bank?.name)
        assertEquals("swiggy@axisbank", tx.counterparty)
        assertEquals(1000000L, tx.availableBalanceMinor)
    }

    @Test
    fun `canara upi debit`() {
        val tx = parsed(
            "An amount of INR 500.00 has been DEBITED to your account XXX1234 on 12/09/2026 towards UPI. " +
                "Total Avail.bal INR 10,000.00. - Canara Bank",
        )
        assertEquals(50000L, tx.amountMinor)
        assertEquals(Direction.DEBIT, tx.direction)
        assertEquals(TransactionType.UPI, tx.type)
        assertEquals("Canara Bank", tx.bank?.name)
        assertEquals(1000000L, tx.availableBalanceMinor)
        assertNull(tx.counterparty)
    }

    @Test
    fun `union bank debit with colon after rs and a never share otp footer`() {
        val tx = parsed(
            "A/c *1234 Debited for Rs:500.00 on 12-09-2026 14:22:10 by Mob Bk ref no 425612345678 " +
                "Avl Bal Rs:10000.00.Never Share OTP/PIN/CVV-Union Bank of India",
        )
        assertEquals(50000L, tx.amountMinor)
        assertEquals("Union Bank of India", tx.bank?.name)
        assertEquals(1000000L, tx.availableBalanceMinor)
        assertEquals("425612345678", tx.referenceId)
    }

    @Test
    fun `yes bank and indusind name the payee in the upi path`() {
        val yes = parsed(
            "INR 500.00 debited from A/c XX1234 on 12-SEP-2026 towards UPI/425612345678/SWIGGY. Avl Bal: INR 10,000.00 - YES BANK",
        )
        assertEquals("Yes Bank", yes.bank?.name)
        assertEquals("SWIGGY", yes.counterparty)

        val indusind = parsed(
            "IndusInd Bank: Your A/C 1234XXXX5678 has been debited with INR 500.00 towards UPI/425612345678/swiggy@icici on 12-09-26",
        )
        assertEquals("IndusInd Bank", indusind.bank?.name)
        assertEquals("5678", indusind.accountLast4)
        assertEquals("swiggy@icici", indusind.counterparty)
    }

    @Test
    fun `federal bank debit to a vpa`() {
        val tx = parsed(
            "Rs 500.00 debited from your A/c XX1234 using UPI on 12-09-2026 14:22:10 to VPA swiggy@icici - " +
                "(UPI Ref No 425612345678)-Federal Bank",
        )
        assertEquals("Federal Bank", tx.bank?.name)
        assertEquals("swiggy@icici", tx.counterparty)
        assertEquals("425612345678", tx.referenceId)
    }

    @Test
    fun `india post payments bank credit`() {
        val tx = parsed(
            "You have received a payment of Rs. 500.00 in a/c X1234 on 12/09/2026 14:22 from RAHUL thru IPPB. " +
                "Info: UPI/CREDIT/425612345678.Avl Bal Rs. 1,500.00-IPPB",
        )
        assertEquals(Direction.CREDIT, tx.direction)
        assertEquals(TransactionType.UPI, tx.type)
        assertEquals("India Post Payments Bank", tx.bank?.name)
        assertEquals("RAHUL", tx.counterparty)
        assertEquals("425612345678", tx.referenceId)
        assertEquals(150000L, tx.availableBalanceMinor)
    }

    // ---- Cards ----

    @Test
    fun `hdfc credit card online spend`() {
        val tx = parsed(
            "Spent Rs.1,234.00 On HDFC Bank Card 5678 At AMAZON PAY INDIA On 2026-09-12:10:12:33\n" +
                "Not You? To Block+Reissue Call 18002586161/SMS BLOCK CC 5678 to 7308080808",
            sender = "VM-HDFCBK-S",
        )
        assertEquals(123400L, tx.amountMinor)
        assertEquals(Direction.DEBIT, tx.direction)
        assertEquals(TransactionType.CARD_ONLINE, tx.type)
        assertEquals(AccountType.CREDIT_CARD, tx.accountType)
        assertEquals("5678", tx.accountLast4)
        assertEquals("AMAZON PAY INDIA", tx.counterparty)
    }

    @Test
    fun `icici credit card spend keeps the limit out of the amount`() {
        val tx = parsed(
            "INR 1,009.00 spent using ICICI Bank Card XX6010 on 12-Sep-26 on BLINK COMMERCE . " +
                "Avl Limit: INR 46,441.48. If not you, call 1800 2662/SMS BLOCK 6010 to 9215676766.",
        )
        assertEquals(100900L, tx.amountMinor)
        assertEquals(4644148L, tx.availableBalanceMinor)
        assertEquals(TransactionType.CARD_POS, tx.type)
        assertEquals(AccountType.CREDIT_CARD, tx.accountType)
        assertEquals("6010", tx.accountLast4)
        assertEquals("BLINK COMMERCE", tx.counterparty)
    }

    @Test
    fun `axis credit card spend with the merchant on its own line`() {
        val tx = parsed(
            "Spent INR 2,500.00\nAxis Bank Card no. XX1234\n12-09-26 18:45:10 IST\nFLIPKART\nAvl Limit: INR 47,500.00\n" +
                "Not you? SMS BLOCK 1234 to 919951860002",
        )
        assertEquals(250000L, tx.amountMinor)
        assertEquals(TransactionType.CARD_ONLINE, tx.type)
        assertEquals(AccountType.CREDIT_CARD, tx.accountType)
        assertEquals("FLIPKART", tx.counterparty)
        assertEquals(4750000L, tx.availableBalanceMinor)
    }

    @Test
    fun `sbi card online spend is not marked online by the dispute link`() {
        val netflix = parsed(
            "Rs.649.00 spent on your SBI Credit Card ending 1234 at NETFLIX on 12/09/26. Trxn. not done by you? " +
                "Report at https://sbicard.com/Dispute",
            sender = "VM-SBICRD-S",
        )
        assertEquals("SBI Card", netflix.bank?.name)
        assertEquals(TransactionType.CARD_ONLINE, netflix.type)
        assertEquals("NETFLIX", netflix.counterparty)

        val shop = parsed(
            "Rs.1,999.00 spent on your SBI Credit Card ending 1234 at RELIANCE TRENDS on 12/09/26. " +
                "Trxn. not done by you? Report at https://sbicard.com/Dispute",
            sender = "VM-SBICRD-S",
        )
        assertEquals(TransactionType.CARD_POS, shop.type)
    }

    @Test
    fun `sbi debit card swipe`() {
        val tx = parsed(
            "Dear Customer, your SBI Debit Card ending 1234 has been used for Rs. 1,500.00 at RELIANCE TRENDS on 12Sep26 14:22. " +
                "If not done by you, call 1800 11 2211",
        )
        assertEquals(Direction.DEBIT, tx.direction)
        assertEquals(TransactionType.CARD_POS, tx.type)
        assertEquals(AccountType.DEBIT_CARD, tx.accountType)
        assertEquals("RELIANCE TRENDS", tx.counterparty)
    }

    @Test
    fun `rbl credit card online spend`() {
        val tx = parsed(
            "Your RBL Bank Credit Card XX1234 has been used for INR 1,200.00 at MYNTRA on 12-09-2026. Available limit: INR 50,000",
        )
        assertEquals("RBL Bank", tx.bank?.name)
        assertEquals(TransactionType.CARD_ONLINE, tx.type)
        assertEquals(AccountType.CREDIT_CARD, tx.accountType)
        assertEquals("MYNTRA", tx.counterparty)
        assertEquals(5000000L, tx.availableBalanceMinor)
    }

    @Test
    fun `account debit at a pos terminal`() {
        val tx = parsed("Rs 850.00 debited from A/c XX1234 via POS at DMART on 12-09-26. Avl Bal Rs 9,150.00 -AU Bank")
        assertEquals(TransactionType.CARD_POS, tx.type)
        assertEquals("AU Small Finance Bank", tx.bank?.name)
        assertEquals("DMART", tx.counterparty)
    }

    // ---- ATM and cash ----

    @Test
    fun `sbi atm withdrawal`() {
        val tx = parsed(
            "Dear Customer, Rs.5,000 withdrawn at SBI ATM S1BW000123 from A/cX1234 on 12Sep26 Transaction Number 1234. " +
                "Available Balance Rs.20,000. If not withdrawn by you, forward this SMS to 7400165218/ call 1800111109 -SBI",
            sender = "VM-ATMSBI-S",
        )
        assertEquals(500000L, tx.amountMinor)
        assertEquals(Direction.DEBIT, tx.direction)
        assertEquals(TransactionType.ATM_WITHDRAWAL, tx.type)
        assertEquals("1234", tx.accountLast4)
        assertEquals(2000000L, tx.availableBalanceMinor)
        assertNull(tx.counterparty)
    }

    @Test
    fun `atm withdrawal with a debit card`() {
        val tx = parsed("Rs.2,000.00 withdrawn from ATM using Debit Card XX1234 at ATM ID S1BW0012 on 12-09-26 -Bank of India")
        assertEquals(TransactionType.ATM_WITHDRAWAL, tx.type)
        assertEquals(AccountType.DEBIT_CARD, tx.accountType)
    }

    @Test
    fun `cash deposit at a cdm`() {
        val tx = parsed("Rs 10,000.00 deposited in cash to your A/c XX1234 at CDM on 12-09-26. -Indian Bank")
        assertEquals(Direction.CREDIT, tx.direction)
        assertEquals(TransactionType.CASH_DEPOSIT, tx.type)
        assertEquals("Indian Bank", tx.bank?.name)
    }

    // ---- NEFT, IMPS, RTGS ----

    @Test
    fun `hdfc neft salary credit names the employer and utr`() {
        val tx = parsed(
            "Update! INR 50,000.00 deposited in HDFC Bank A/c XX1234 on 01-SEP-26 for NEFT Cr-CITI0000001-ACME TECHNOLOGIES PVT LTD-" +
                "SALARY SEP 2026-CITIN52026090112345678. Avl bal INR 75,000.00. Cheque deposits in A/C are subject to clearing",
        )
        assertEquals(5000000L, tx.amountMinor)
        assertEquals(Direction.CREDIT, tx.direction)
        assertEquals(TransactionType.SALARY, tx.type)
        assertEquals("ACME TECHNOLOGIES PVT LTD", tx.counterparty)
        assertEquals("CITIN52026090112345678", tx.referenceId)
        assertEquals(7500000L, tx.availableBalanceMinor)
    }

    @Test
    fun `axis neft credit names the sender in the info path`() {
        val tx = parsed(
            "INR 25,000.00 credited to A/c no. XX1234 on 01-09-26 at 10:15:22 IST. Info- NEFT/AXISN52026090112/ACME CORP. " +
                "Avl Bal- INR 60,000.00 - Axis Bank",
        )
        assertEquals(TransactionType.NEFT, tx.type)
        assertEquals(Direction.CREDIT, tx.direction)
        assertEquals("ACME CORP", tx.counterparty)
        assertEquals(6000000L, tx.availableBalanceMinor)
    }

    @Test
    fun `sbi neft credit`() {
        val tx = parsed("Your A/C XXXXX1234 has credit for NEFT transaction of Rs 25,000.00 on 12/09/26 by ACME CORP. Avl Bal Rs 30,000.00 -SBI")
        assertEquals(Direction.CREDIT, tx.direction)
        assertEquals(TransactionType.NEFT, tx.type)
        assertEquals("ACME CORP", tx.counterparty)
    }

    @Test
    fun `imps debit to a person`() {
        val tx = parsed("Rs 2,000.00 debited from A/c XX1234 on 12-09-26 for IMPS to RAHUL SHARMA, Ref 425612345678 -IDFC FIRST Bank")
        assertEquals(TransactionType.IMPS, tx.type)
        assertEquals(Direction.DEBIT, tx.direction)
        assertEquals("IDFC FIRST Bank", tx.bank?.name)
        assertEquals("RAHUL SHARMA", tx.counterparty)
        assertEquals("425612345678", tx.referenceId)
    }

    @Test
    fun `icici imps debit between accounts`() {
        val tx = parsed(
            "ICICI Bank Acct XX123 debited with INR 10,000.00 on 12-Sep-26 & Acct XX456 credited. IMPS:425612345678. Call 18002662 for dispute",
        )
        assertEquals(TransactionType.IMPS, tx.type)
        assertEquals(Direction.DEBIT, tx.direction)
        assertEquals("123", tx.accountLast4)
        assertEquals("425612345678", tx.referenceId)
    }

    @Test
    fun `rtgs credit with indian digit grouping`() {
        val tx = parsed(
            "INR 5,00,000.00 credited to A/c XX1234 on 12-09-26 by RTGS from ACME CORP UTR HDFCR52026091212345678 -Kotak Bank",
        )
        assertEquals(50000000L, tx.amountMinor)
        assertEquals(TransactionType.RTGS, tx.type)
        assertEquals("ACME CORP", tx.counterparty)
        assertEquals("HDFCR52026091212345678", tx.referenceId)
    }

    // ---- Auto-debits ----

    @Test
    fun `nach debit names the biller`() {
        val tx = parsed("Rs.1,499.00 debited from A/c XX1234 on 05-09-26 towards NACH-DR-HDFC LIFE INSURANCE-12345678. Avl Bal Rs 8,000 -ICICI Bank")
        assertEquals(TransactionType.AUTO_DEBIT, tx.type)
        assertEquals("HDFC LIFE INSURANCE", tx.counterparty)
    }

    @Test
    fun `ecs debit`() {
        val tx = parsed("Your a/c XX1234 debited for Rs 5,000.00 on 05-09-26 via ECS towards BAJAJ FINANCE LTD -Bank of Maharashtra")
        assertEquals(TransactionType.AUTO_DEBIT, tx.type)
        assertEquals("BAJAJ FINANCE LTD", tx.counterparty)
    }

    @Test
    fun `upi mandate execution is an auto debit`() {
        val tx = parsed("Rs.199.00 debited from A/c XX1234 for UPI Mandate to NETFLIX on 12-09-26. UPI Ref 425612345678 -Axis Bank")
        assertEquals(TransactionType.AUTO_DEBIT, tx.type)
        assertEquals("NETFLIX", tx.counterparty)
    }

    // ---- Salary, interest, charges, refunds ----

    @Test
    fun `icici salary credit`() {
        val tx = parsed(
            "Dear Customer, Acct XX123 is credited with Rs 75,000.00 on 01-Sep-26 from ACME TECH PVT LTD. Info:NEFT-SALARY SEP26. " +
                "Avl Bal Rs 80,000.00 -ICICI Bank",
        )
        assertEquals(TransactionType.SALARY, tx.type)
        assertEquals(Direction.CREDIT, tx.direction)
        assertEquals("ACME TECH PVT LTD", tx.counterparty)
    }

    @Test
    fun `interest credits`() {
        val sbi = parsed("Dear Customer, Your A/c XXXXX1234 is credited with INR 1,234.00 towards interest. -SBI")
        assertEquals(TransactionType.INTEREST, sbi.type)
        assertEquals(123400L, sbi.amountMinor)

        val hdfc = parsed("INR 812.00 credited to HDFC Bank A/c XX1234 on 30-09-26 for Int.Pd:01-07-2026 to 30-09-2026")
        assertEquals(TransactionType.INTEREST, hdfc.type)
    }

    @Test
    fun `bank charges`() {
        val sms = parsed("Dear Customer, Rs.17.70 has been debited from your A/c XXXXX1234 towards SMS alert charges for quarter ending 30-06-2026 -SBI")
        assertEquals(TransactionType.BANK_CHARGES, sms.type)
        assertEquals("1234", sms.accountLast4)

        val bounce = parsed("Rs.590.00 debited from A/c XX1234 towards ECS return charges incl GST due to insufficient funds -Canara Bank")
        assertEquals(TransactionType.BANK_CHARGES, bounce.type)

        val fee = parsed("Rs 236.00 debited from A/c XX1234 for Debit Card Annual Fee incl GST -Punjab National Bank")
        assertEquals(TransactionType.BANK_CHARGES, fee.type)
        assertEquals(Direction.DEBIT, fee.direction)
    }

    @Test
    fun `refund credit names the merchant`() {
        val tx = parsed("Rs 500.00 credited to your A/c XX1234 on 12-09-26 towards refund from AMAZON. Avl Bal Rs 10,500 -HDFC Bank")
        assertEquals(TransactionType.REFUND, tx.type)
        assertEquals(Direction.CREDIT, tx.direction)
        assertEquals("AMAZON", tx.counterparty)
    }

    @Test
    fun `upi reversal is a credit even though it names the debit`() {
        val tx = parsed("Your UPI transaction of Rs 500.00 debited on 12-09-26 has been reversed to A/c XX1234. UPI Ref 425612345678 -Kotak Bank")
        assertEquals(TransactionType.REFUND, tx.type)
        assertEquals(Direction.CREDIT, tx.direction)
    }

    @Test
    fun `cheque debit`() {
        val tx = parsed("Chq no. 123456 for Rs 5,000.00 debited from A/c XX1234 on 12-09-26 -Central Bank of India")
        assertEquals(TransactionType.CHEQUE, tx.type)
        assertEquals("Central Bank of India", tx.bank?.name)
    }

    // ---- Credit card bill payments ----

    @Test
    fun `credit card payment confirmations are recognised as bill payments`() {
        listOf(
            "Dear Customer, Payment of INR 25,000.00 has been received on your ICICI Bank Credit Card Account 4xxx on 12-SEP-26. Thank you.",
            "We have received payment of Rs.10,000.00 via BBPS & the same has been credited to your SBI Credit Card ending 1234 on 12/09/26",
            "Payment of INR 10000 received towards Axis Bank Credit Card XX1234",
        ).forEach { body ->
            val tx = parsed(body)
            assertEquals(body, TransactionType.CREDIT_CARD_BILL_PAYMENT, tx.type)
        }
    }

    @Test
    fun `bank debits paying a card bill are recognised as bill payments`() {
        listOf(
            "Rs 10,000.00 debited from A/c XX1234 towards ICICI Bank Credit Card XX5678 payment -HDFC Bank",
            "Sent Rs.10000.00\nFrom HDFC Bank A/C *1234\nTo CRED Club\nOn 12/09/26\nRef 425612345678",
        ).forEach { body ->
            val tx = parsed(body)
            assertEquals(body, TransactionType.CREDIT_CARD_BILL_PAYMENT, tx.type)
            assertEquals(Direction.DEBIT, tx.direction)
        }
    }

    @Test
    fun `card bill payments never reach the transaction list`() {
        assertNull(
            PaymentTextParser.parse(
                "Payment of INR 10000 received towards Axis Bank Credit Card XX1234",
                "com.google.android.apps.messaging",
            )
        )
    }

    // ---- Messages that are not transactions ----

    @Test
    fun `rejects otps failures reminders requests and promotions`() {
        listOf(
            "123456 is the OTP for transaction of INR 5,000.00 at AMAZON on your HDFC Bank Card ending 1234. Valid for 5 mins",
            "Your UPI transaction of Rs 500 to SWIGGY from A/c XX1234 has failed. Amount if debited will be credited in 3 days",
            "Your ICICI Bank Credit Card XX1234 transaction of INR 5,000 at AMAZON has been declined due to insufficient limit",
            "Rs 1,499 will be debited from your A/c XX1234 on 05-09-26 towards NACH -HDFC Bank",
            "Your HDFC Bank Credit Card XX1234 statement is generated. Total Amt Due: Rs 12,345. Min Amt Due: Rs 617. Due date 20-09-26",
            "rahul@okaxis has requested Rs 500 from you via UPI on your A/c XX1234",
            "Your UPI mandate for Rs 199.00 to NETFLIX from A/c XX1234 has been successfully created.",
            "Get a pre-approved personal loan of Rs 5,00,000 from HDFC Bank. Apply now",
            "Avl bal in A/c XX1234 is Rs 10,000.00 as on 12-09-26 -SBI",
        ).forEach { assertNotATransaction(it) }
    }

    @Test
    fun `rejects anything from a promotional header`() {
        assertNotATransaction("Rs 500 credited to your A/c XX1234 as cashback", sender = "VM-HDFCBK-P")
    }

    @Test
    fun `leaves messages with no bank signal to the general parser`() {
        assertEquals(BankSmsResult.Unrecognised, BankSmsParser.parse("Paid Rs.100 to Zomato from Paytm Wallet"))
    }
}
