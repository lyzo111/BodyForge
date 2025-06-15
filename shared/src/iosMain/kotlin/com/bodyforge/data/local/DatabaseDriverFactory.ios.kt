package com.bodyforge.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.bodyforge.database.BodyForgeDatabase

actual class DatabaseDriverFactory {
    actual fun create(): SqlDriver {
        return NativeSqliteDriver(
            schema = BodyForgeDatabase.Schema,
            name = "BodyForgeDatabase.db"
        )
    }
}