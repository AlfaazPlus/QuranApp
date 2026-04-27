package com.quranapp.android.db.entities.quran

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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