package app.lumadocs.kmp.services

import app.lumadocs.kmp.utils.SecurityUtils
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private const val FILES_URL = "https://www.googleapis.com/drive/v3/files"
private const val UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
private const val FOLDER_MIME = "application/vnd.google-apps.folder"
private const val BOUNDARY = "lumadocs-boundary-7MA4YWxkTrZu0gW"
private const val FILE_FIELDS =
    "id, name, mimeType, description, createdTime, modifiedTime, size, parents, " +
        "thumbnailLink, webViewLink, webContentLink, appProperties"

/**
 * Drive v3 over REST. The Android side uses the google-api-client SDK, which has no
 * Kotlin/Native equivalent, so the same operations are issued directly with Ktor.
 * Field names and appProperties keys must match the Android impl — both platforms read
 * the same files from the same account.
 */
internal class IosGoogleDriveRepositoryImpl(
    private val tokenProvider: DriveTokenProvider,
) : GoogleDriveRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(Darwin)

    /** Runs [block] with a bearer token, or returns [fallback] when the user has no Drive access. */
    private suspend fun <T> authorized(fallback: T, block: suspend (token: String) -> T): T =
        withContext(Dispatchers.Default) {
            val token = tokenProvider.driveAccessToken() ?: return@withContext fallback
            try {
                block(token)
            } catch (e: Exception) {
                println("GoogleDrive: request failed: ${e.message}")
                fallback
            }
        }

    private fun JsonPrimitive.orNull(): String? = if (this is JsonNull) null else content

    private fun JsonObject.toDriveFile(): DriveFile {
        val props = this["appProperties"]?.jsonObject
        fun str(key: String) = this[key]?.jsonPrimitive?.orNull()
        fun prop(key: String) = props?.get(key)?.jsonPrimitive?.orNull()
        return DriveFile(
            id = str("id").orEmpty(),
            name = str("name").orEmpty(),
            mimeType = str("mimeType").orEmpty(),
            description = str("description"),
            createdTime = str("createdTime"),
            modifiedTime = str("modifiedTime"),
            // Drive returns size as a string and omits it entirely for folders.
            size = str("size")?.toLongOrNull() ?: 0L,
            parentId = this["parents"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.orNull(),
            thumbnailLink = str("thumbnailLink"),
            webViewLink = str("webViewLink"),
            webContentLink = str("webContentLink"),
            category = prop("category"),
            encrypted = prop("encrypted") == "true",
            expiryDate = prop("expiryDate"),
        )
    }

    override suspend fun uploadFile(
        fileName: String,
        mimeType: String,
        fileContent: ByteArray,
        parentFolderId: String?,
        description: String?,
        category: String?,
        makeEncrypted: Boolean,
        expiryDate: String?,
    ): UploadResult {
        val signedOut = UploadResult(
            success = false,
            errorMessage = "Failed to initialize Google Drive service. Please sign in again.",
        )
        return authorized(signedOut) { token ->
            val metadata = buildJsonObject {
                put("name", fileName)
                put("mimeType", mimeType)
                if (parentFolderId != null) {
                    put("parents", buildJsonArray { add(JsonPrimitive(parentFolderId)) })
                }
                if (description != null) put("description", description)
                val props = buildJsonObject {
                    if (category != null) put("category", category)
                    if (makeEncrypted) put("encrypted", "true")
                    if (expiryDate != null) put("expiryDate", expiryDate)
                }
                if (props.isNotEmpty()) put("appProperties", props)
            }

            val response = client.post(UPLOAD_URL) {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("uploadType", "multipart")
                parameter("fields", "id")
                contentType(ContentType.parse("multipart/related; boundary=$BOUNDARY"))
                setBody(multipartRelatedBody(json.encodeToString(JsonObject.serializer(), metadata), mimeType, fileContent))
            }

            if (!response.status.isSuccess()) {
                return@authorized UploadResult(success = false, errorMessage = response.driveError("Upload failed"))
            }
            UploadResult(
                success = true,
                fileId = json.parseToJsonElement(response.bodyAsText())
                    .jsonObject["id"]?.jsonPrimitive?.orNull(),
            )
        }
    }

    override suspend fun createFolder(folderName: String, parentFolderId: String?): String? =
        authorized(null) { token ->
            val body = buildJsonObject {
                put("name", folderName)
                put("mimeType", FOLDER_MIME)
                if (parentFolderId != null) {
                    put("parents", buildJsonArray { add(JsonPrimitive(parentFolderId)) })
                }
            }
            val response = client.post(FILES_URL) {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("fields", "id, name")
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(JsonObject.serializer(), body))
            }
            if (!response.status.isSuccess()) return@authorized null
            json.parseToJsonElement(response.bodyAsText()).jsonObject["id"]?.jsonPrimitive?.orNull()
        }

    override suspend fun listFiles(folderId: String?, query: String): List<DriveFile> =
        authorized(emptyList()) { token ->
            val q = buildString {
                append("trashed = false")
                if (folderId != null) append(" and '$folderId' in parents")
                if (query.isNotEmpty()) append(" and $query")
            }
            val response = client.get(FILES_URL) {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("q", q)
                parameter("fields", "files($FILE_FIELDS)")
                parameter("pageSize", 1000)
            }
            if (!response.status.isSuccess()) return@authorized emptyList()
            json.parseToJsonElement(response.bodyAsText())
                .jsonObject["files"]?.jsonArray
                ?.map { it.jsonObject.toDriveFile() }
                .orEmpty()
        }

    override suspend fun deleteFile(fileId: String): Boolean = authorized(false) { token ->
        client.delete("$FILES_URL/$fileId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.status.isSuccess()
    }

    override suspend fun getFileContent(fileId: String, isEncrypted: Boolean): ByteArray? =
        authorized(null) { token ->
            val response = client.get("$FILES_URL/$fileId") {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("alt", "media")
            }
            if (!response.status.isSuccess()) return@authorized null
            val bytes = response.readRawBytes()
            if (!isEncrypted) return@authorized bytes
            try {
                SecurityUtils.decrypt(bytes)
            } catch (e: Exception) {
                println("GoogleDrive: failed to decrypt $fileId: ${e.message}")
                null
            }
        }

    override suspend fun updateFileMetadata(
        fileId: String,
        name: String,
        description: String?,
        expiryDate: String?,
    ): Boolean = authorized(false) { token ->
        val body = buildJsonObject {
            put("name", name)
            // Empty string clears the description on Drive when the user removed it.
            put("description", description ?: "")
            // Drive merges appProperties: a null value deletes just that key and leaves
            // category/encrypted intact.
            put("appProperties", buildJsonObject {
                put("expiryDate", expiryDate?.let { JsonPrimitive(it) } ?: JsonNull)
            })
        }
        client.patch("$FILES_URL/$fileId") {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("fields", "id, name, description, appProperties")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(JsonObject.serializer(), body))
        }.status.isSuccess()
    }

    private suspend fun HttpResponse.driveError(prefix: String): String {
        val detail = runCatching {
            json.parseToJsonElement(bodyAsText())
                .jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.orNull()
        }.getOrNull()
        return "$prefix: ${detail ?: status.description}"
    }

    /**
     * Drive's multipart/related upload: a JSON metadata part followed by the raw bytes.
     * Built by hand because the body mixes text and binary parts under one boundary.
     */
    private fun multipartRelatedBody(metadataJson: String, mimeType: String, content: ByteArray): ByteArray {
        val header = (
            "--$BOUNDARY\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n\r\n" +
                metadataJson + "\r\n" +
                "--$BOUNDARY\r\n" +
                "Content-Type: $mimeType\r\n\r\n"
            ).encodeToByteArray()
        val footer = "\r\n--$BOUNDARY--".encodeToByteArray()
        return header + content + footer
    }
}
