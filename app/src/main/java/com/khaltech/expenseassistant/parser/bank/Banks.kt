package com.khaltech.expenseassistant.parser.bank

/**
 * @param senderCodes the six-character DLT headers the bank sends from ("HDFCBK" in "VM-HDFCBK-S").
 * @param aliases lower-case names the bank signs its messages with, used when the SMS app shows a
 *   saved contact name instead of the header, or the bank sends from a header not listed here.
 */
data class Bank(val name: String, val senderCodes: Set<String>, val aliases: List<String>)

/**
 * Indian banks and card issuers whose alerts the parser recognises. Banks add and rotate headers,
 * so a miss here is not fatal: the aliases, and then the message structure itself, still identify
 * a bank alert.
 */
object Banks {

    val all: List<Bank> = listOf(
        // Public sector
        Bank("State Bank of India", setOf("SBIINB", "SBIPSG", "SBIUPI", "CBSSBI", "ATMSBI", "SBYONO", "SBIBNK", "SBISMS"), listOf("state bank of india", "sbi")),
        Bank("Punjab National Bank", setOf("PNBSMS", "PNBBNK", "PUNBNK"), listOf("punjab national bank", "pnb")),
        Bank("Bank of Baroda", setOf("BOBTXN", "BOBSMS", "BARODA", "BOBBNK"), listOf("bank of baroda")),
        Bank("Canara Bank", setOf("CANBNK", "CNRBNK"), listOf("canara bank")),
        Bank("Union Bank of India", setOf("UNIONB", "UBOIND", "UBISMS"), listOf("union bank of india", "union bank")),
        Bank("Bank of India", setOf("BOIIND", "BOISMS"), listOf("bank of india", "boi")),
        Bank("Indian Bank", setOf("INDBNK", "INDBMK"), listOf("indian bank")),
        Bank("Central Bank of India", setOf("CENTBK", "CBOISM"), listOf("central bank of india")),
        Bank("Indian Overseas Bank", setOf("IOBCHN", "IOBBNK", "IOBSMS"), listOf("indian overseas bank", "iob")),
        Bank("UCO Bank", setOf("UCOBNK", "UCOBKS"), listOf("uco bank")),
        Bank("Bank of Maharashtra", setOf("MAHABK", "MAHABN"), listOf("bank of maharashtra")),
        Bank("Punjab & Sind Bank", setOf("PSBANK", "PSBSMS"), listOf("punjab & sind bank", "punjab and sind bank")),
        Bank("IDBI Bank", setOf("IDBIBK", "IDBIBN"), listOf("idbi bank", "idbi")),

        // Private sector
        Bank("HDFC Bank", setOf("HDFCBK", "HDFCBN", "HDFCCC"), listOf("hdfc bank", "hdfc")),
        Bank("ICICI Bank", setOf("ICICIB", "ICICIT", "ICICIO"), listOf("icici bank", "icici")),
        Bank("Axis Bank", setOf("AXISBK", "AXISMR", "AXISCC"), listOf("axis bank", "axis")),
        Bank("Kotak Mahindra Bank", setOf("KOTAKB", "KMBLTD", "KOTAKM"), listOf("kotak mahindra bank", "kotak bank", "kotak")),
        Bank("Yes Bank", setOf("YESBNK", "YESBKS"), listOf("yes bank")),
        Bank("IndusInd Bank", setOf("INDUSB", "INDUSL", "IBLBNK"), listOf("indusind bank", "indusind")),
        Bank("IDFC FIRST Bank", setOf("IDFCFB", "IDFCBK"), listOf("idfc first bank", "idfc first", "idfc")),
        Bank("Federal Bank", setOf("FEDBNK", "FEDFIB"), listOf("federal bank")),
        Bank("South Indian Bank", setOf("SIBSMS", "SIBANK"), listOf("south indian bank")),
        Bank("Karur Vysya Bank", setOf("KVBANK", "KVBSMS"), listOf("karur vysya bank", "kvb")),
        Bank("Karnataka Bank", setOf("KBLBNK", "KTKBNK"), listOf("karnataka bank")),
        Bank("City Union Bank", setOf("CUBLTD", "CUBANK"), listOf("city union bank")),
        Bank("Tamilnad Mercantile Bank", setOf("TMBANK", "TMBSMS"), listOf("tamilnad mercantile bank", "tmb")),
        Bank("RBL Bank", setOf("RBLBNK", "RBLCRD"), listOf("rbl bank", "rbl")),
        Bank("DCB Bank", setOf("DCBBNK", "DCBANK"), listOf("dcb bank")),
        Bank("Bandhan Bank", setOf("BANDHN", "BDNSMS"), listOf("bandhan bank")),
        Bank("CSB Bank", setOf("CSBBNK", "CSBANK"), listOf("csb bank")),
        Bank("Jammu & Kashmir Bank", setOf("JKBANK", "JKBSMS"), listOf("j&k bank", "jammu & kashmir bank", "jammu and kashmir bank")),
        Bank("Dhanlaxmi Bank", setOf("DHANBK", "DLXBNK"), listOf("dhanlaxmi bank")),

        // Small finance and payments banks
        Bank("AU Small Finance Bank", setOf("AUBANK", "AUSFBL"), listOf("au small finance bank", "au bank")),
        Bank("Equitas Small Finance Bank", setOf("EQUTAS", "EQUITS"), listOf("equitas small finance bank", "equitas")),
        Bank("Ujjivan Small Finance Bank", setOf("UJJIVN", "UJJSFB"), listOf("ujjivan small finance bank", "ujjivan")),
        Bank("India Post Payments Bank", setOf("IPBMSG", "IPPBNK"), listOf("india post payments bank", "ippb")),
        Bank("Airtel Payments Bank", setOf("AIRBNK", "AIRPBL"), listOf("airtel payments bank")),
        Bank("Fino Payments Bank", setOf("FINOBK", "FINOPB"), listOf("fino payments bank")),
        Bank("Jio Payments Bank", setOf("JIOPBL", "JIOPAY"), listOf("jio payments bank")),

        // Foreign banks
        Bank("Standard Chartered", setOf("SCBANK", "STANCB"), listOf("standard chartered")),
        Bank("HSBC", setOf("HSBCIN", "HSBCBK"), listOf("hsbc")),
        Bank("DBS Bank", setOf("DBSBNK", "DIGIBK"), listOf("dbs bank", "digibank")),
        Bank("Citibank", setOf("CITIBK", "CITIBN"), listOf("citibank")),

        // Card issuers that send their own alerts
        Bank("SBI Card", setOf("SBICRD", "SBMCRD"), listOf("sbi card", "sbi credit card")),
        Bank("American Express", setOf("AMEXIN", "AMEXSM"), listOf("american express", "amex")),
        Bank("OneCard", setOf("ONECRD"), listOf("onecard")),
    )

    private val byCode: Map<String, Bank> = all
        .flatMap { bank -> bank.senderCodes.map { it to bank } }
        .toMap()

    // Longest alias first, so "Central Bank of India" is never read as "Bank of India".
    private val aliasPatterns: List<Pair<Regex, Bank>> = all
        .flatMap { bank -> bank.aliases.map { it to bank } }
        .sortedByDescending { it.first.length }
        .map { (alias, bank) -> Regex("""(?<![a-z0-9])${Regex.escape(alias)}(?![a-z0-9])""") to bank }

    private val SEPARATORS = Regex("[^A-Z0-9]+")

    /** Reads "VM-HDFCBK-S", "AD-HDFCBK", "HDFCBK", or a saved contact name such as "HDFC Bank". */
    fun fromSender(sender: String?): Bank? {
        if (sender.isNullOrBlank()) return null
        return sender.uppercase().split(SEPARATORS).firstNotNullOfOrNull { byCode[it] }
            ?: fromText(sender)
    }

    fun fromText(text: String): Bank? {
        val lower = text.lowercase()
        return aliasPatterns.firstOrNull { (pattern, _) -> pattern.containsMatchIn(lower) }?.second
    }

    /**
     * TRAI headers end in -P for promotional traffic, -S for service, -T for transactional and -G
     * for government. A promotional header never carries a transaction alert.
     */
    fun isPromotionalSender(sender: String?): Boolean =
        sender != null && Regex("""^[A-Z]{2}-[A-Z0-9]{6}-P$""").matches(sender.trim().uppercase())
}
