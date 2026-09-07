package com.expenseassistant.ui

/** Direct-distribution build: on-screen capture ships, so its disclosure does too. */
val ScreenCaptureDisclosure: Disclosure? = Disclosure(
    title = "Read payment success screens?",
    summary = "A fallback for payments that finish without posting any notification.",
    points = listOf(
        DisclosurePoint(
            "What it reads",
            "Visible text on payment confirmation screens, and only in GPay, PhonePe, Paytm and BHIM.",
        ),
        DisclosurePoint(
            "Why",
            "Some UPI payments show a success screen but never post a notification, so they would otherwise be missed.",
        ),
        DisclosurePoint(
            "Where it goes",
            "The text is read on this phone, turned into a transaction, and stored here. Nothing is uploaded.",
        ),
        DisclosurePoint(
            "How it works",
            "It uses an Android accessibility service, which you can switch off at any time in Android Settings.",
        ),
    ),
    confirmLabel = "Continue",
)
