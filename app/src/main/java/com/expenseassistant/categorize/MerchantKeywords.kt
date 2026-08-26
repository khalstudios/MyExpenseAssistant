package com.expenseassistant.categorize

import com.expenseassistant.data.model.Category

/**
 * Built-in merchant/keyword knowledge base. Longest match wins, so keep specific
 * brands ahead of generic words by giving them longer keys.
 */
object MerchantKeywords {

    val rules: Map<Category, List<String>> = mapOf(
        Category.FOOD_AND_DRINK to listOf(
            "swiggy", "zomato", "eatsure", "faasos", "behrouz", "ovenstory", "dominos", "pizza",
            "mcdonald", "kfc", "burger king", "subway", "starbucks", "cafe", "coffee", "chai",
            "restaurant", "hotel ", "dhaba", "biryani", "bakery", "juice", "sweets", "tiffin",
            "canteen", "food", "kitchen", "eats", "bar & ", "brewery", "dunkin", "barbeque",
        ),
        Category.GROCERIES to listOf(
            "blinkit", "zepto", "instamart", "bigbasket", "dmart", "d-mart", "jiomart",
            "grofers", "reliance fresh", "more supermarket", "spencer", "kirana", "grocery",
            "supermarket", "provision", "vegetable", "fruits", "dairy", "milk", "amul",
        ),
        Category.TRANSPORT to listOf(
            "uber", "ola ", "olacabs", "rapido", "namma yatri", "bmtc", "metro", "dmrc",
            "auto", "cab", "taxi", "yulu", "bounce", "redbus", "bus ", "toll", "fastag", "parking",
        ),
        Category.FUEL to listOf(
            "petrol", "diesel", "fuel", "hpcl", "bpcl", "indian oil", "indianoil", "iocl",
            "shell", "nayara", "reliance petro", "filling station", "gas station",
        ),
        Category.SHOPPING to listOf(
            "amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa", "tatacliq", "snapdeal",
            "decathlon", "lifestyle", "shoppers stop", "pantaloons", "zara", "h&m", "uniqlo",
            "croma", "reliance digital", "vijay sales", "ikea", "store", "mart", "retail",
        ),
        Category.BILLS_AND_UTILITIES to listOf(
            "electricity", "bescom", "mseb", "tneb", "kseb", "adani electricity", "torrent power",
            "water bill", "gas bill", "indane", "hp gas", "bharatgas", "broadband", "airtel",
            "jio", "vodafone", "vi ", "bsnl", "act fibernet", "hathway", "tata play", "dth",
            "recharge", "postpaid", "prepaid", "bill payment", "municipal", "society maintenance",
        ),
        Category.RENT to listOf("rent", "landlord", "nobroker", "housing.com", "lease", "pg rent"),
        Category.ENTERTAINMENT to listOf(
            "netflix", "prime video", "hotstar", "jiocinema", "sonyliv", "zee5", "spotify",
            "youtube premium", "gaana", "wynk", "bookmyshow", "pvr", "inox", "cinepolis",
            "cinema", "gaming", "steam", "playstation", "xbox",
        ),
        Category.HEALTH to listOf(
            "pharmacy", "apollo", "medplus", "netmeds", "1mg", "pharmeasy", "tata 1mg",
            "hospital", "clinic", "diagnostic", "lab", "doctor", "dental", "medical",
            "practo", "cult.fit", "cultfit", "gym", "fitness", "insurance premium",
        ),
        Category.EDUCATION to listOf(
            "school", "college", "university", "tuition", "coaching", "udemy", "coursera",
            "byju", "unacademy", "vedantu", "upgrad", "exam fee", "course", "library",
        ),
        Category.TRAVEL to listOf(
            "makemytrip", "goibibo", "cleartrip", "yatra", "ixigo", "irctc", "indigo",
            "air india", "vistara", "spicejet", "akasa", "airbnb", "oyo", "treebo", "fabhotels",
            "booking.com", "agoda", "railway", "flight", "hostel", "resort",
        ),
        Category.INVESTMENTS to listOf(
            "zerodha", "groww", "upstox", "angel one", "angelone", "kuvera", "coin",
            "mutual fund", "sip ", "nps", "ppf", "smallcase", "icici direct", "hdfc securities",
            "stock", "demat", "gold bond", "recurring deposit", "fixed deposit",
        ),
        Category.INCOME to listOf("salary", "payroll", "interest credited", "dividend", "reimbursement"),
    )

    private val flattened: List<Pair<String, Category>> = rules
        .flatMap { (category, keywords) -> keywords.map { it.trim() to category } }
        .sortedByDescending { it.first.length }

    /** @return the best matching category plus the length of the matched keyword. */
    fun match(haystack: String): Pair<Category, Int>? {
        val text = haystack.lowercase()
        val hit = flattened.firstOrNull { (keyword, _) -> text.contains(keyword) } ?: return null
        return hit.second to hit.first.length
    }
}
