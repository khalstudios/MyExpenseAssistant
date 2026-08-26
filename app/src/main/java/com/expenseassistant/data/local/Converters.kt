package com.expenseassistant.data.local

import androidx.room.TypeConverter
import com.expenseassistant.data.model.CaptureSource
import com.expenseassistant.data.model.Category
import com.expenseassistant.data.model.Direction

class Converters {
    @TypeConverter fun categoryToString(value: Category): String = value.name
    @TypeConverter fun stringToCategory(value: String): Category = Category.fromName(value)

    @TypeConverter fun directionToString(value: Direction): String = value.name
    @TypeConverter fun stringToDirection(value: String): Direction = Direction.valueOf(value)

    @TypeConverter fun sourceToString(value: CaptureSource): String = value.name
    @TypeConverter fun stringToSource(value: String): CaptureSource = CaptureSource.valueOf(value)
}
