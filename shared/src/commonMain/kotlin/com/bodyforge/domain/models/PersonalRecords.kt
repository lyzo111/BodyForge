package com.bodyforge.domain.models

// Estimated one-rep max (Epley). Mirrors the formula used by the analytics progress chart.
fun estimatedOneRepMax(weightKg: Double, reps: Int): Double =
    if (reps <= 0) 0.0 else weightKg * (1.0 + reps / 30.0)

// Best lifts recorded for one exercise: top estimated 1RM and the heaviest weight moved.
data class ExerciseRecords(val bestE1RM: Double, val heaviestKg: Double) {
    val hasAny: Boolean get() = bestE1RM > 0.0
}

// Best estimated-1RM and heaviest weight for one exercise across the given workouts. Only weighted
// sets with at least one rep count; pass completed workouts to get all-time records.
fun List<Workout>.recordsFor(exerciseId: String): ExerciseRecords {
    var bestE1RM = 0.0
    var heaviest = 0.0
    for (w in this) {
        for (eiw in w.exercises) {
            if (eiw.exercise.id != exerciseId) continue
            for (s in eiw.sets) {
                if (s.weightKg <= 0.0 || s.reps <= 0) continue
                val e1 = estimatedOneRepMax(s.weightKg, s.reps)
                if (e1 > bestE1RM) bestE1RM = e1
                if (s.weightKg > heaviest) heaviest = s.weightKg
            }
        }
    }
    return ExerciseRecords(bestE1RM, heaviest)
}

// The best estimated-1RM reached for an exercise within a single workout, counting only completed
// sets — used to detect a new personal record live during a workout.
fun ExerciseInWorkout.bestCompletedE1RM(): Double =
    sets.filter { it.completed && it.weightKg > 0.0 && it.reps > 0 }
        .maxOfOrNull { estimatedOneRepMax(it.weightKg, it.reps) } ?: 0.0
