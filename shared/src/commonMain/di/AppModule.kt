package com.bodyforge.di

import com.bodyforge.data.local.DatabaseDriverFactory
import com.bodyforge.data.local.DatabaseHelper
import com.bodyforge.data.repository.ExerciseRepositoryImpl
import com.bodyforge.data.repository.WorkoutRepositoryImpl
import com.bodyforge.domain.repository.ExerciseRepository
import com.bodyforge.domain.repository.WorkoutRepository
import com.bodyforge.domain.usecases.CreateWorkout
import com.bodyforge.domain.usecases.UpdateWorkoutSet
import com.bodyforge.domain.usecases.FinishWorkout

class AppModule(databaseDriverFactory: DatabaseDriverFactory) {

    // Database
    private val database = DatabaseHelper.createDatabase(databaseDriverFactory)

    // Repositories
    val workoutRepository: WorkoutRepository = WorkoutRepositoryImpl(database)
    val exerciseRepository: ExerciseRepository = ExerciseRepositoryImpl(database)

    // Use Cases
    val createWorkout = CreateWorkout(workoutRepository)
    val updateWorkoutSet = UpdateWorkoutSet(workoutRepository)
    val finishWorkout = FinishWorkout(workoutRepository)
}

// composeApp/src/commonMain/di/PlatformModule.kt
package com.bodyforge.di

import com.bodyforge.data.local.DatabaseDriverFactory

expect object PlatformModule {
    fun provideDatabaseDriverFactory(): DatabaseDriverFactory
}

// composeApp/src/androidMain/kotlin/com/bodyforge/di/PlatformModule.android.kt
package com.bodyforge.di

import android.content.Context
import com.bodyforge.data.local.DatabaseDriverFactory

actual object PlatformModule {
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    actual fun provideDatabaseDriverFactory(): DatabaseDriverFactory {
        return DatabaseDriverFactory(appContext)
    }
}

// composeApp/src/commonMain/di/AppContainer.kt
package com.bodyforge.di

object AppContainer {

    private var _appModule: AppModule? = null

    val appModule: AppModule
        get() = _appModule ?: throw IllegalStateException("AppModule not initialized. Call init() first.")

    fun init() {
        val databaseDriverFactory = PlatformModule.provideDatabaseDriverFactory()
        _appModule = AppModule(databaseDriverFactory)
    }
}