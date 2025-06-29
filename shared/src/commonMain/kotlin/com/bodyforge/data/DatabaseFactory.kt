package com.bodyforge.data

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.bodyforge.database.BodyForgeDatabase

object DatabaseFactory {
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun create(): BodyForgeDatabase {
        val driver = AndroidSqliteDriver(
            schema = BodyForgeDatabase.Schema,
            context = appContext,
            name = "BodyForge.db"
        )
        return BodyForgeDatabase(driver)
    }
}