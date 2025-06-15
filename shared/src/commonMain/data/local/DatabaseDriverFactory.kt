package com.bodyforge.data.local

import app.cash.sqldelight.db.SqlDriver
import com.bodyforge.database.BodyForgeDatabase

expect class DatabaseDriverFactory {
    fun create(): SqlDriver
}

object DatabaseHelper {
    fun createDatabase(driverFactory: DatabaseDriverFactory): BodyForgeDatabase {
        val driver = driverFactory.create()
        return BodyForgeDatabase(driver)
    }
}

// shared/src/androidMain/kotlin/com/bodyforge/data/local/DatabaseDriverFactory.android.kt
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

// shared/src/iosMain/kotlin/com/bodyforge/data/local/DatabaseDriverFactory.ios.kt
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

// shared/src/jvmMain/kotlin/com/bodyforge/data/local/DatabaseDriverFactory.jvm.kt
package com.bodyforge.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.bodyforge.database.BodyForgeDatabase

actual class DatabaseDriverFactory {
    actual fun create(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BodyForgeDatabase.Schema.create(driver)
        return driver
    }
}