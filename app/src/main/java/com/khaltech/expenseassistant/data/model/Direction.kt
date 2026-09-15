package com.khaltech.expenseassistant.data.model

enum class Direction { DEBIT, CREDIT }

/** SCREEN is no longer produced; it stays so transactions and backups from earlier builds still load. */
enum class CaptureSource { NOTIFICATION, SCREEN, MANUAL }
