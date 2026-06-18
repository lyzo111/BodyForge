package com.bodyforge.data

import android.content.Intent
import android.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Portable, self-contained representation of a template so a recipient can rebuild it even if
// they don't have the same exercises (each exercise carries enough info to be recreated).
@Serializable
data class SharedExercise(
    val id: String,
    val name: String,
    val muscleGroups: List<String>,
    val equipment: String = "None",
    val isBodyweight: Boolean = false
)

@Serializable
data class SharedTemplate(
    val name: String,
    val description: String = "",
    val exercises: List<SharedExercise>
)

object TemplateSharing {
    private const val PREFIX = "BFTPL1:"
    private val json = Json { ignoreUnknownKeys = true }

    // Encodes a template into a shareable message containing a base64 code other apps can carry.
    fun encode(template: SharedTemplate): String {
        val payload = json.encodeToString(template)
        val code = PREFIX + Base64.encodeToString(payload.encodeToByteArray(), Base64.NO_WRAP)
        return "BodyForge template \"${template.name}\".\n" +
            "Open BodyForge → Templates → Import and paste this:\n\n$code"
    }

    // Extracts and decodes a template from arbitrary shared text (the code may be surrounded by
    // other text from the messaging app).
    fun decode(text: String): SharedTemplate? {
        val start = text.indexOf(PREFIX)
        if (start < 0) return null
        val token = text.substring(start + PREFIX.length).trim().substringBefore('\n').trim()
        return try {
            val bytes = Base64.decode(token, Base64.NO_WRAP)
            json.decodeFromString<SharedTemplate>(bytes.decodeToString())
        } catch (e: Exception) {
            null
        }
    }

    // Opens the system share sheet with the given text.
    fun share(text: String, subject: String) {
        val context = DatabaseFactory.context()
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        val chooser = Intent.createChooser(send, subject).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
