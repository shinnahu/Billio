package app.billio.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Everything the app knows, kept in one small JSON file on the phone.
 * There is no account and no server: this file is the only copy.
 */
class Store(private val file: File) {
    private val lock = Any()
    private val state = MutableStateFlow(read())

    val data: StateFlow<AppData> = state

    fun update(change: (AppData) -> AppData) {
        synchronized(lock) {
            val next = change(state.value)
            if (next == state.value) return
            write(next)
            state.value = next
        }
    }

    fun exportJson(): String = pretty.encodeToString(state.value)

    /** Replaces everything with a backup made by [exportJson]. Returns false when the text is not a backup. */
    fun importJson(text: String): Boolean {
        val parsed = runCatching { json.decodeFromString<AppData>(text) }.getOrNull() ?: return false
        update { parsed }
        return true
    }

    private fun read(): AppData {
        if (!file.exists()) return AppData()
        return runCatching { json.decodeFromString<AppData>(file.readText()) }.getOrElse {
            // Keep the unreadable file aside instead of overwriting it with an empty one.
            runCatching { file.copyTo(File(file.parentFile, file.name + ".broken"), overwrite = true) }
            AppData()
        }
    }

    private fun write(data: AppData) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(json.encodeToString(data))
        try {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: Exception) {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val pretty = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }
    }
}
