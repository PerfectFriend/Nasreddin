package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import com.example.model.AppLanguage
import com.example.model.ComicBook
import com.example.model.ComicPage
import java.util.UUID

/**
 * Storage Access Framework (SAF) importer for user-provided comic folders and multi-image selections.
 * Automatically retains persistable read URI permissions across app restarts, sorts pages
 * using [AlphanumericComparator] so "page2" precedes "page10", and generates localized metadata
 * via [AppStrings] matching the active [AppLanguage].
 */
object FolderImporter {
    private const val TAG = "FolderImporter"

    private val SUPPORTED_MIME_TYPES = setOf(
        "image/jpeg",
        "image/png",
        "image/webp",
        "image/jpg",
        "image/gif",
        "image/bmp"
    )

    private val SUPPORTED_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "webp", "gif", "bmp"
    )

    fun importFromFolderTreeUri(
        context: Context,
        treeUri: Uri,
        language: AppLanguage = AppLanguage.RUSSIAN
    ): ComicBook? {
        try {
            // Persist read access across app restarts
            try {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                Log.w(TAG, "Could not take persistable permission: ${e.message}")
            }

            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)

            val rawItems = mutableListOf<RawImageItem>()
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )

            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)

                while (cursor.moveToNext()) {
                    val childDocId = if (idIdx >= 0) cursor.getString(idIdx) else null
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                    val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx) else null

                    if (childDocId != null && name != null) {
                        val isImageMime = mime != null && (mime.startsWith("image/") || SUPPORTED_MIME_TYPES.contains(mime))
                        val ext = name.substringAfterLast('.', "").lowercase()
                        val isImageExt = SUPPORTED_EXTENSIONS.contains(ext)

                        if (isImageMime || isImageExt) {
                            val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId)
                            rawItems.add(RawImageItem(name, fileUri))
                        }
                    }
                }
            }

            if (rawItems.isEmpty()) {
                return null
            }

            // Natural sort pages
            val comparator = AlphanumericComparator()
            rawItems.sortWith { a, b -> comparator.compare(a.displayName, b.displayName) }

            val folderName = getTreeDisplayName(context, treeUri)
                ?: AppStrings.importedComicFallbackTitle(language)

            val pages = rawItems.mapIndexed { index, item ->
                ComicPage(
                    id = UUID.randomUUID().toString(),
                    title = AppStrings.importedPageTitle(
                        lang = language,
                        pageNumber = index + 1,
                        fileName = item.displayName.substringBeforeLast('.')
                    ),
                    uriString = item.uri.toString(),
                    sequenceNumber = index + 1,
                    language = language
                )
            }

            return ComicBook(
                id = UUID.randomUUID().toString(),
                title = folderName,
                subtitle = AppStrings.importedFolderSubtitle(language, pages.size),
                description = AppStrings.importedFolderDescription(language, folderName),
                coverUriString = pages.firstOrNull()?.uriString,
                pages = pages,
                isBuiltIn = false,
                language = language,
                sourceFolderUri = treeUri.toString()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error importing folder", e)
            return null
        }
    }

    fun importFromSelectedUris(
        context: Context,
        uris: List<Uri>,
        language: AppLanguage = AppLanguage.RUSSIAN
    ): ComicBook? {
        if (uris.isEmpty()) return null

        val rawItems = mutableListOf<RawImageItem>()
        for (uri in uris) {
            try {
                // Try taking persistable permission if possible
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Persistable URI permission unavailable for $uri: ${e.message}")
                }

                val displayName = queryDisplayName(context, uri) ?: ("page_" + (rawItems.size + 1))
                rawItems.add(RawImageItem(displayName, uri))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to resolve uri: $uri", e)
            }
        }

        if (rawItems.isEmpty()) return null

        val comparator = AlphanumericComparator()
        rawItems.sortWith { a, b -> comparator.compare(a.displayName, b.displayName) }

        val pages = rawItems.mapIndexed { index, item ->
            ComicPage(
                id = UUID.randomUUID().toString(),
                title = AppStrings.importedPageTitle(
                    lang = language,
                    pageNumber = index + 1,
                    fileName = item.displayName.substringBeforeLast('.')
                ),
                uriString = item.uri.toString(),
                sequenceNumber = index + 1,
                language = language
            )
        }

        return ComicBook(
            id = UUID.randomUUID().toString(),
            title = AppStrings.newImportedComicTitle(language, pages.size),
            subtitle = AppStrings.customSelectionSubtitle(language),
            description = AppStrings.customSelectionDescription(language, pages.size),
            coverUriString = pages.firstOrNull()?.uriString,
            pages = pages,
            isBuiltIn = false,
            language = language
        )
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        var name: String? = null
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        name = cursor.getString(idx)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query display name for $uri: ${e.message}")
        }
        return name ?: uri.lastPathSegment
    }

    private fun getTreeDisplayName(context: Context, treeUri: Uri): String? {
        try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            return queryDisplayName(context, docUri)
        } catch (e: Exception) {
            Log.w(TAG, "Fallback to tree URI segment for $treeUri: ${e.message}")
            return treeUri.lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/')
        }
    }

    private data class RawImageItem(val displayName: String, val uri: Uri)
}
