package com.quranapp.android.db.entities.quran

import androidx.room.*

@Entity(
    tableName = "scripts",
    indices = [
        Index(value = ["code"], unique = true)
    ]
)
data class ScriptEntity(
    @PrimaryKey
    @ColumnInfo(name = "script_id")
    val scriptId: Int,

    @ColumnInfo(name = "code")
    val code: String,

    @ColumnInfo(name = "display_name")
    val displayName: String
)