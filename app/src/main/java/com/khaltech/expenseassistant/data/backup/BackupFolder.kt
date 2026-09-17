package com.khaltech.expenseassistant.data.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.IOException

/**
 * The app's own folder inside whichever location the user picked, so backups are grouped together
 * instead of loose in the chosen directory.
 */
object BackupFolder {

    /**
     * Deliberately a constant rather than the `app_name` resource: a translated or renamed app would
     * otherwise stop finding the folder its earlier backups are already in.
     */
    const val NAME = "Kahan Gaya Paisa"

    private const val JSON = "application/json"

    /** The [NAME] folder inside [treeUri], created on first use and reused on every backup after. */
    fun resolve(context: Context, treeUri: Uri): DocumentFile {
        val tree = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IOException("That location is unavailable")
        // Picking the app's own folder in the system picker is the obvious thing to do on a second
        // backup; without this it would nest another copy of the folder inside itself.
        if (tree.name == NAME && tree.isDirectory) return tree
        val existing = tree.listFiles().firstOrNull { it.isDirectory && it.name == NAME }
        if (existing != null) return existing
        return tree.createDirectory(NAME) ?: throw IOException("Could not create the $NAME folder")
    }

    /** Whether the app still holds the persisted grant for [treeUri], which the user can revoke. */
    fun hasAccess(context: Context, treeUri: Uri): Boolean =
        context.contentResolver.persistedUriPermissions.any {
            it.uri == treeUri && it.isWritePermission
        }

    /** One backup file already sitting in the app's folder. */
    data class Entry(val uri: Uri, val name: String, val savedAt: Long, val sizeBytes: Long)

    /**
     * Backups in the app's folder under [treeUri], newest first. Empty when the location is gone or
     * access to it was revoked, which the caller shows as "no backups here" rather than an error.
     */
    fun list(context: Context, treeUri: Uri): List<Entry> =
        runCatching { resolve(context, treeUri) }.getOrNull()
            ?.listFiles()
            ?.filter { it.isFile && it.name?.endsWith(".json", ignoreCase = true) == true }
            ?.map { Entry(it.uri, it.name.orEmpty(), it.lastModified(), it.length()) }
            ?.sortedByDescending { it.savedAt }
            ?: emptyList()

    /** Writes [contents] to a new dated file inside the app's folder under [treeUri]. */
    fun write(context: Context, treeUri: Uri, fileName: String, contents: String) {
        val destination = resolve(context, treeUri).createFile(JSON, fileName)
            ?: throw IOException("Could not create the backup file")
        context.contentResolver.openOutputStream(destination.uri)?.bufferedWriter()?.use { writer ->
            writer.write(contents)
        } ?: throw IOException("Could not write the backup file")
    }
}
