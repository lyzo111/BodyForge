package com.bodyforge.data

import android.content.Intent
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Portable, self-contained representation of a template so a recipient can rebuild it even if
// they don't have the same exercises (each exercise carries enough info to be recreated).
@Serializable
data class SharedExercise(
    val name: String,
    val muscleGroups: List<String> = emptyList(),
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
    private const val PREFIX = "BFT2:"          // current: deflate + base64 (compact)
    private const val LEGACY_PREFIX = "BFTPL1:" // v2.1: plain base64 JSON
    private val json = Json { ignoreUnknownKeys = true }

    // Encodes a template into a shareable message. The recipient can paste the whole message;
    // the code is extracted automatically on import.
    fun encode(template: SharedTemplate): String {
        val payload = json.encodeToString(template).encodeToByteArray()
        val code = PREFIX + Base64.encodeToString(deflate(payload), Base64.NO_WRAP)
        return "BodyForge template \"${template.name}\" — paste this whole message into Templates → Import:\n\n$code"
    }

    // Extracts and decodes a template from arbitrary shared text (the code may be surrounded by
    // other text from the messaging app). Handles both the compact and the legacy formats.
    fun decode(text: String): SharedTemplate? =
        tryDecode(text, PREFIX, ::inflate) ?: tryDecode(text, LEGACY_PREFIX) { it }

    private fun tryDecode(text: String, prefix: String, transform: (ByteArray) -> ByteArray): SharedTemplate? {
        val start = text.indexOf(prefix)
        if (start < 0) return null
        val token = text.substring(start + prefix.length).trim().takeWhile { !it.isWhitespace() }
        return try {
            val raw = Base64.decode(token, Base64.NO_WRAP)
            json.decodeFromString<SharedTemplate>(transform(raw).decodeToString())
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

    private fun deflate(data: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        deflater.setInput(data)
        deflater.finish()
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (!deflater.finished()) {
            out.write(buffer, 0, deflater.deflate(buffer))
        }
        deflater.end()
        return out.toByteArray()
    }

    private fun inflate(data: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(data)
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (!inflater.finished()) {
            val n = inflater.inflate(buffer)
            if (n == 0 && inflater.needsInput()) break
            out.write(buffer, 0, n)
        }
        inflater.end()
        return out.toByteArray()
    }
}
