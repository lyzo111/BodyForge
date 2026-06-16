# BodyForge – Projekt-Briefing für Claude Code

Konsolidierter Stand aus drei Chat-Summaries (Dezember 2024, Januar 2025) plus eingesehenem Code. Wo Quellen sich widersprechen, gilt der jüngste Stand (Summary V2, Januar 2025).

---

## 1. Zweck und Ziel

BodyForge ist eine Gym-App zum Tracken von Workouts, Progress, Sleep und Nutrition. Kotlin Multiplatform.

Die ursprüngliche Vision (Planungsphase) war kein reiner Tracker, sondern ein persönliches Trainings- und Analysewerkzeug mit datengestützter Trainingsoptimierung, langfristiger Fortschrittsanalyse, automatischer Gewohnheitsauswertung, minimalem manuellem Aufwand und wissenschaftlich begründeten Empfehlungen. Vier Ebenen: Datenerfassung (Training, Ernährung, Schlaf, Körper) → Datenintegration (APIs, Smart Scales, Samsung Notes, Excel) → Analyse (Graphen, Trends, Periodisierung, Habit-Erkennung) → Entscheidungsunterstützung (AdviceEngine).

Plattformziel war Android und iOS mit geteiltem Code via Kotlin Multiplatform und Compose Multiplatform.

Korrektur zum aktuellen Code: Das ist bisher nur Vision. Der vorhandene Code ist faktisch Android-only — `DatabaseFactory` nutzt `android.content.Context` und `AndroidSqliteDriver`, `AnalyticsScreen` nutzt `java.text.SimpleDateFormat` und `java.util`. Diese Abhängigkeiten liegen in `commonMain` und würden einen iOS-Target blockieren. Für echtes Multiplatform müssen DB-Driver und Datums-Formatierung per `expect`/`actual` plattformspezifisch ausgelagert werden.

---

## 2. Technologie-Stack

- Kotlin Multiplatform (Android-fokussiert)
- Jetpack Compose (UI)
- SQLDelight 2.0.0 (Datenbank)
- Repository Pattern + MVVM
- StateFlow / Coroutines
- kotlinx-datetime 0.6.0, kotlinx-serialization (JSON in TEXT-Spalten)

Kern-Dependencies:
```
app.cash.sqldelight:android-driver:2.0.0
androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4
org.jetbrains.kotlinx:kotlinx-datetime:0.6.0
```

---

## 3. Architektur (aktueller Stand, "Big Bang Refactor")

Single Source of Truth: `SharedWorkoutState` (object) hält alle Repos und StateFlows. ViewModel ist verschlankt und liest aus SharedState. 4-Tab-Navigation per HorizontalPager mit Swipe-Gesten.

```
shared/
├── domain/models/       Exercise, Workout, WorkoutSet, ExerciseInWorkout,
│                        WorkoutTemplate, TrainingPhase, TemplateFolder,
│                        ProgressMetric, PhaseAnalytics
├── domain/repository/   Interfaces (Workout, Exercise, WorkoutTemplate)
├── data/repository/     SQLDelight-Implementierungen
├── data/mappers/        Entity <-> Domain (WorkoutMapper)
└── data/DatabaseFactory.kt

composeApp/
├── presentation/state/SharedWorkoutState.kt
├── presentation/viewmodel/WorkoutViewModel.kt
└── ui/
    ├── screens/   App.kt (4-Tab + Pager), WorkoutScreen, TemplatesScreen,
    │              AnalyticsScreen, HistoryScreen
    └── components/inputs/BodyweightInput.kt

server/   leer (für später)
```

Tab-Layout: Workout | Templates | Analytics | History

---

## 4. Datenbank-Schema (SQLDelight)

```sql
Exercise(id, name, muscle_groups, instructions, equipment_needed,
         is_custom, is_bodyweight, default_rest_time_seconds, deleted)
Workout(id, name, started_at, finished_at, notes)
WorkoutSet(id, workout_id, exercise_id, order_in_workout, set_number,
           reps, weight_kg, rest_time_seconds, completed, completed_at, notes)
WorkoutTemplate(id, name, exercise_ids, created_at, description)
NutritionLog(id, date, food_name, calories_per_100g, protein_per_100g,
             carbs_per_100g, fat_per_100g, amount_grams, meal_type, created_at)
SleepLog(id, date, bedtime, wake_time, sleep_quality, notes, created_at)
```

Hinweis: `muscle_groups` und `exercise_ids` sind JSON-Arrays als TEXT. `deleted` existiert, wird aber noch nicht genutzt (Hard Delete aktiv).

---

## 5. Tatsächlicher Implementierungsstand

Die Summaries bezeichnen die App als "App Store-Ready" und "KOMPLETT funktioniert". Das ist überzogen — dieselben Summaries listen offene Kernfunktionen als TODO. Realistische Einordnung für die Übergabe:

Funktioniert / vorhanden:
- 4-Tab-Navigation, HorizontalPager + Swipe
- SQLDelight-DB inkl. Periodization-Modelle und Nutrition/Sleep-Tabellen
- Repository Pattern für Workout, Exercise, Template (Template-Repo ist DB-backed über `selectAllTemplates`, nicht mehr In-Memory)
- SharedWorkoutState als zentrale State-Verwaltung
- Exercise Search + Multi-Filter (Muscle Groups, Exercise Type)
- Bodyweight + Additional Weight (BW+10kg-Anzeige)
- Analytics-Tab: Quick Stats, Volume-Chart, Muscle Group Balance, Achievements; Training-Frequency-Heatmap nur Placeholder
- Workout History
- Orphaned-Workout-Cleanup beim Start (nur ein aktives Workout erlaubt)

Nur Gerüst, nicht funktional:
- Nutrition und Sleep: Tabellen existieren, aber kein Repository und kein UI
- Soft Delete: Spalte vorhanden, Logik fehlt (es wird hart gelöscht)
- Periodization: Modelle und Create-Phase-Dialog da, Edit/Delete fehlt
- Template Preview Dialog: nicht fertig
- Custom Exercise Creation: Dialog da, laut V2 noch nicht voll funktional

---

## 6. Bekannte Bugs / offene TODOs

Sofort (Phase 1):
1. `selectedTypeFilters` unresolved reference in WorkoutScreen.kt, Funktion `SimpleNoExercisesCard` — Variable aus dem QuickWorkoutFlow-Scope korrekt durchreichen
2. Active Workout View mit Set Management implementieren
3. Template Preview Dialog fertigstellen
4. Custom Exercise Creation funktional machen

Kurzfristig (Phase 2):
1. Exercise Details Dialog mit Usage Stats
2. Periodization Management (Edit/Delete Phases)
3. Template-Zuordnung zu Training Phases
4. Progress Tracking (1RM, Volume über Zeit)

Fehlende Composables (in App.kt als Platzhalter): WorkoutHeaderCard, ActiveExerciseCard, EmptyHistoryCard, HistoryWorkoutCard, EditWorkoutDialog

---

## 7. Geplante Features (vollständig)

Periodization / Template-Organisation:
- Template-Ordner-System (Full Body, Upper/Lower, PPL) — DB-Schema-Erweiterung nötig
- TrainingPhase mit PhaseType (Strength, Hypertrophy, Cut, Bulk)
- TemplateFolder mit SchedulingType (Manual, Frequency, Rotation, Rest-Based)
- Vorgefertigte Folder-Factories für PPL, Upper/Lower, Full Body bereits im Code
- PhaseAnalytics: Volume pro Muskelgruppe je Phase

Smartwatch-Integration (Galaxy Watch 6, Wear OS 5):
- Cross-Device Sync (WhatsApp-Style)
- Offline Mode mit lokaler Watch-Datenbank
- Conflict Resolution bei abweichenden Workout-Daten
- Real-time Sync Phone <-> Watch
- Battery-Resilient Architecture

Advanced Analytics:
- Plateau Detection
- Exercise Recommendations
- GitHub-Style Heatmap (aktuell Placeholder) echt machen

Weitere:
- Nutrition Tracking (UI + Repo auf bestehender Tabelle)
- Sleep Tracking (UI + Repo auf bestehender Tabelle)
- Soft Delete für Custom Exercises aktivieren
- Exercise Instructions & Images

---

## 7b. Frühere Vision-Features (aus der Planungsphase, vor dem aktuellen Code)

Diese Features stammen aus der ursprünglichen Konzeptphase und fehlten im bisherigen Briefing. Keines davon ist im aktuellen Code umgesetzt; teils fehlen die nötigen DB-Tabellen ganz. Einordnung als geplant, nicht implementiert.

Training:
- Rest-Timer: startet automatisch nach einem Satz, individuelle Pausenzeit pro Übung, minimale manuelle Eingaben. (`rest_time_seconds` existiert im Schema, der Timer selbst nicht.)
- RPE/RIR pro Satz: war angedacht, nie final entschieden. Nicht im Schema.
- Temporärer Übungsaustausch während eines laufenden Workouts: Übung ersetzen (z. B. Barbell Bench Press → Machine Chest Press), nur temporär, ohne die Vorlage zu zerstören, aber korrekt im Verlauf gespeichert. Nicht umgesetzt.

Datenintegration / Import (komplett fehlend):
- Nutrition über externe API: Lebensmittel suchen, Makros (Kalorien, Protein, Carbs, Fett) übernehmen. Aktuell nur leere `NutritionLog`-Tabelle.
- Sleep über API: Schlafdauer, -qualität, evtl. -phasen automatisch importieren. Aktuell nur leere `SleepLog`-Tabelle.
- Smart-Scale-Import: Daten von Körperfettwaagen (Körperfett, Muskelmasse, Wasseranteil) importieren und in ein einheitliches Format umrechnen. Keine Tabelle, kein Code.
- Historischer Datenimport aus Samsung Notes: Trainingsnotizen einlesen und parsen.
- Historischer Datenimport aus Excel: Dateien einlesen und verarbeiten.
- Parser-Algorithmus, der Rohnotizen in strukturierte Trainingseinheiten umwandelt. Beispiel: `Bench Press 80x8 / 80x7 / 75x9` → Übung + Sätze + Reps + Gewichte + Datum.

Körperdaten (fehlende Tabelle):
- Basisdaten: Körpergröße, Alter, Gewicht.
- Fortschrittsdaten: Gewichtsverlauf, Körperzusammensetzung.

Fortschrittsanalyse / Graphen:
- Verlaufsgraphen für Körpergewicht, Kraftentwicklung, Volumen, Körperfett.

Periodisierung (ergänzend zu den bereits im Code vorhandenen Modellen):
- Frei benennbare Zyklen mit Zeiträumen (z. B. Hypertrophy/Strength/Cutting/Peak/Deload, 01.01.–31.03.).
- Pro Zeitraum den verwendeten Split speichern (Full Body, Upper/Lower, PPL), um vergleichende Analysen zu erlauben (z. B. „Bench Press stieg unter Upper/Lower schneller als unter Full Body“).

AdviceEngine (ambitioniertestes Feature, komplett geplant):
- Kombiniert Schlaf, Ernährung, Trainingsdaten, Restzeiten, Körperdaten und Gewohnheiten.
- Zwei Empfehlungstypen:
  1. Allgemein/edukativ, nicht personalisiert (z. B. Protein-Richtwerte, Schlaf-Regenerations-Hinweise).
  2. Gewohnheitsbasiert/personalisiert aus den eigenen Daten (z. B. „Du schläfst vor leistungsstarken Einheiten im Schnitt unter 6 h“, „Kraftwerte stagnieren seit 8 Wochen“, „Proteinzufuhr an 70 % der Trainingstage unter Ziel“, „Du verkürzt regelmäßig die Satzpausen“).
- Bewusst als Abgrenzung zu generischen Tipps konzipiert: echte Muster aus den eigenen Daten.

---

## 7c. Weitere gewünschte Features (UI, Variationen, Tracking-Genauigkeit)

Themes:
- Auswählbare Themes in den Einstellungen, vergleichbar mit IDEs (VS Code, JetBrains). Setzt eine zentrale Theme-/Color-Verwaltung voraus; aktuell sind Farben als private Konstanten direkt in den Screens hartcodiert (z. B. in `AnalyticsScreen`). Das muss zuerst in ein gemeinsames Theme-System ausgelagert werden, bevor Theme-Switching möglich ist.

Templates und Progress-Vergleich:
- Template-Speicherung (Upper/Lower, PPL etc.) existiert bereits. Nicht neu bauen.
- Neu: Workouts, die dasselbe Template verwenden, gegeneinander vergleichbar machen, um Progress auf Template-Ebene zu tracken. Voraussetzung: Jedes Workout muss seine Template-Herkunft speichern (`Workout` hat aktuell keine `template_id`).

Workout-Variationen:
- Variationen innerhalb einer Routine, z. B. Upper A und Upper B als Varianten von "Upper".
- Datenmodell-Implikation: `WorkoutTemplate` braucht ein Gruppierungskonzept (übergeordnete Routine + Variations-Bezeichner). Aktuell gibt es kein solches Feld.
- Zwei getrennte Graph-Ansichten für den Progress, jeweils als Linie:
  1. Workout-zu-Workout: chronologische Abfolge über Varianten hinweg (Upper A → Upper B → Upper A → ...).
  2. Variation-zu-Variation: nur gleiche Variante (Upper A → nächste Upper A → ...).

Temporärer Skip / Replace im Workout (erweitert den Eintrag aus 7b):
- Während eines Workouts pro Übung markieren können: einmalig geskippt oder einmalig durch andere Übung ersetzt.
- Diese Markierung muss persistiert werden, nicht nur zur Laufzeit gelten.
- Datenmodell-Implikation: Set-/Übungs-Ebene braucht einen Status (z. B. completed / skipped / substituted) und bei Ersatz eine Referenz auf die Original-Übung. `WorkoutSet` und `ExerciseInWorkout` haben das aktuell nicht.
- Darstellung im Progress-Graph: bei einem Skip an dem Datum kein Datenpunkt (Lücke in der Linie); bei einem Ersatz der Punkt als gekennzeichneter Knoten (Marker), der anzeigt, dass dort getauscht wurde. Der Graph liest dafür den Status aus den persistierten Daten.

---

## 8. Konventionen für Claude Code

- Kommentare ausschließlich auf Englisch. Deutsche Kommentare beim Bearbeiten der Datei auf Englisch umschreiben.
- Keine Marker-Kommentare wie NEW, UPDATED, FIXED. Code self-documenting halten.
- Klärende Fragen stellen, bevor größere Änderungen erfolgen.
- Flaches Design: `elevation = 0.dp` durchgängig.
- `BasicTextField` statt `TextField` bei Parameter-Konflikten.

---

## 9. Build

```bash
# Standard
./gradlew :composeApp:installDebug

# Bei Problemen
./gradlew clean
./gradlew :shared:generateSqlDelightInterface
./gradlew :composeApp:installDebug
```

SQLDelight-Interface nach Schema-Änderungen immer neu generieren, sonst fehlen Methoden in den Queries.

---

## 10. Empfohlene Reihenfolge für Claude Code

1. `selectedTypeFilters`-Bug fixen (Compile-Blocker)
2. Fehlende Platzhalter-Composables in App.kt implementieren
3. Active Workout View + Set Management
4. Custom Exercise Creation und Template Preview abschließen
5. Soft Delete aktivieren (Filter auf `deleted`, Repos umstellen)
6. Nutrition/Sleep Repo + UI auf bestehenden Tabellen
7. Periodization Edit/Delete + Template-Folder-System
8. Smartwatch-Integration als eigenes Modul
9. Körperdaten-Tabelle + Tracking (Größe, Alter, Gewicht, Körperzusammensetzung) als Basis für Graphen
10. Rest-Timer und temporärer Übungsaustausch im Active-Workout-Flow
11. Import-Pipeline (Excel, Samsung Notes) mit Parser für Rohnotizen
12. AdviceEngine zuletzt — sie setzt voraus, dass Schlaf, Ernährung, Körper- und Trainingsdaten bereits sauber erfasst werden
