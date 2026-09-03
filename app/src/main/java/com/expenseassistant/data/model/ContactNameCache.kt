package com.expenseassistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contact_name_cache")
data class ContactNameCache(
    @PrimaryKey val merchantKey: String,
    val contactName: String?,
    val lookedUpAt: Long = System.currentTimeMillis(),
)