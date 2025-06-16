package com.bodyforge.data

import com.bodyforge.database.BodyForgeDatabase

expect object DatabaseFactory {
    fun create(): BodyForgeDatabase
}