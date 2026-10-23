package com.personaljarvis.memory

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class MemoryEvent(
    val kind: String,
    val source: String,
    val summary: String,
    val occurredAt: Long
)

class MemoryStore(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(database: SQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE memory_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                kind TEXT NOT NULL,
                source TEXT NOT NULL,
                entity TEXT,
                summary TEXT NOT NULL,
                occurred_at INTEGER NOT NULL,
                expires_at INTEGER
            )
            """.trimIndent()
        )
        database.execSQL(
            """
            CREATE INDEX memory_events_time
            ON memory_events(occurred_at DESC)
            """.trimIndent()
        )
        database.execSQL(
            """
            CREATE TABLE contact_aliases (
                alias TEXT PRIMARY KEY,
                canonical_name TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL(
            """
            CREATE TABLE preferences (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(
        database: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) = Unit

    fun recordEvent(
        kind: String,
        source: String,
        summary: String,
        entity: String? = null,
        occurredAt: Long = System.currentTimeMillis(),
        expiresAt: Long? = null
    ) {
        writableDatabase.insert(
            "memory_events",
            null,
            ContentValues().apply {
                put("kind", kind)
                put("source", source)
                put("entity", entity)
                put("summary", summary.take(1_000))
                put("occurred_at", occurredAt)
                put("expires_at", expiresAt)
            }
        )
    }

    fun recentEvents(
        sinceMillis: Long,
        kinds: Set<String> = emptySet(),
        limit: Int = 20
    ): List<MemoryEvent> {
        val now = System.currentTimeMillis()
        val kindClause = if (kinds.isEmpty()) {
            ""
        } else {
            " AND kind IN (${kinds.joinToString(",") { "?" }})"
        }
        val args = buildList {
            add(sinceMillis.toString())
            add(now.toString())
            addAll(kinds)
            add(limit.toString())
        }.toTypedArray()
        return readableDatabase.query(
            "memory_events",
            arrayOf("kind", "source", "summary", "occurred_at"),
            "occurred_at >= ? AND (expires_at IS NULL OR expires_at > ?)$kindClause",
            args.dropLast(1).toTypedArray(),
            null,
            null,
            "occurred_at DESC",
            limit.toString()
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        MemoryEvent(
                            kind = cursor.getString(0),
                            source = cursor.getString(1),
                            summary = cursor.getString(2),
                            occurredAt = cursor.getLong(3)
                        )
                    )
                }
            }
        }
    }

    fun rememberContact(alias: String, canonicalName: String) {
        writableDatabase.insertWithOnConflict(
            "contact_aliases",
            null,
            ContentValues().apply {
                put("alias", alias)
                put("canonical_name", canonicalName)
                put("updated_at", System.currentTimeMillis())
            },
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun contacts(limit: Int = 20): List<ContactContext> {
        return readableDatabase.query(
            "contact_aliases",
            arrayOf("alias", "canonical_name"),
            null,
            null,
            null,
            null,
            "updated_at DESC",
            limit.toString()
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        ContactContext(
                            alias = cursor.getString(0),
                            canonicalName = cursor.getString(1)
                        )
                    )
                }
            }
        }
    }

    fun setPreference(key: String, value: String) {
        writableDatabase.insertWithOnConflict(
            "preferences",
            null,
            ContentValues().apply {
                put("key", key)
                put("value", value)
                put("updated_at", System.currentTimeMillis())
            },
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun preferences(limit: Int = 20): List<PreferenceContext> {
        return readableDatabase.query(
            "preferences",
            arrayOf("key", "value"),
            null,
            null,
            null,
            null,
            "updated_at DESC",
            limit.toString()
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        PreferenceContext(
                            key = cursor.getString(0),
                            value = cursor.getString(1)
                        )
                    )
                }
            }
        }
    }

    private companion object {
        const val DATABASE_NAME = "xiaozhu-memory.db"
        const val DATABASE_VERSION = 1
    }
}
