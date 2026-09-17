package com.khaltech.expenseassistant.data.local

import androidx.room.TypeConverter
import com.khaltech.expenseassistant.data.model.AccountType
import com.khaltech.expenseassistant.data.model.BudgetPeriod
import com.khaltech.expenseassistant.data.model.CaptureSource
import com.khaltech.expenseassistant.data.model.Category
import com.khaltech.expenseassistant.data.model.Direction
import com.khaltech.expenseassistant.data.model.PaymentMode
import com.khaltech.expenseassistant.data.model.TransactionType

class Converters {
    @TypeConverter fun categoryToString(value: Category): String = value.name
    @TypeConverter fun stringToCategory(value: String): Category = Category.fromName(value)

    @TypeConverter fun budgetPeriodToString(value: BudgetPeriod): String = value.name
    @TypeConverter fun stringToBudgetPeriod(value: String): BudgetPeriod = BudgetPeriod.fromName(value)

    @TypeConverter fun directionToString(value: Direction): String = value.name
    @TypeConverter fun stringToDirection(value: String): Direction = Direction.valueOf(value)

    @TypeConverter fun sourceToString(value: CaptureSource): String = value.name
    @TypeConverter fun stringToSource(value: String): CaptureSource = CaptureSource.valueOf(value)

    @TypeConverter fun paymentModeToString(value: PaymentMode): String = value.name
    @TypeConverter fun stringToPaymentMode(value: String): PaymentMode = PaymentMode.fromName(value)

    @TypeConverter fun transactionTypeToString(value: TransactionType?): String? = value?.name
    @TypeConverter fun stringToTransactionType(value: String?): TransactionType? = TransactionType.fromName(value)

    @TypeConverter fun accountTypeToString(value: AccountType?): String? = value?.name
    @TypeConverter fun stringToAccountType(value: String?): AccountType? = AccountType.fromName(value)

    @TypeConverter fun tagsToString(value: List<String>): String = value.joinToString("\u001F")
    @TypeConverter fun stringToTags(value: String): List<String> =
        value.split('\u001F').map { it.trim() }.filter { it.isNotEmpty() }
}
