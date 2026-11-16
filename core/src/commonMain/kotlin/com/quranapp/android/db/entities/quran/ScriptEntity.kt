package com.quranapp.android.db.entities.quran

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "scripts",
    indices = [
        Index(value = ["code"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = ScriptEntity::class,
            parentColumns = ["script_id"],
            childColumns = ["parent"],
        )
    ]
)
data class ScriptEntity(
    @PrimaryKey
    @ColumnInfo(name = "script_id")
    val scriptId: Int,

    @ColumnInfo(name = "code")
    val code: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "parent")
    val parent: Int?
)