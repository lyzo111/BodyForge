package com.bodyforge.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.bodyforge.database.BodyForgeDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun create(): SqlDriver {
        return AndroidSqliteDriver(
            schema = BodyForgeDatabase.Schema,
            context = context,
            name = "BodyForgeDatabase.db"
        )
    }
}