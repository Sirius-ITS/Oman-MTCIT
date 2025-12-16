package com.informatique.mtcit.data.model.cancelRegistration

data class DeletionFileUpload(
    val fileName: String,
    val fileUri: String,
    val fileBytes: ByteArray,
    val mimeType: String = "application/octet-stream"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DeletionFileUpload

        if (fileName != other.fileName) return false
        if (fileUri != other.fileUri) return false
        if (!fileBytes.contentEquals(other.fileBytes)) return false
        if (mimeType != other.mimeType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + fileUri.hashCode()
        result = 31 * result + fileBytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
}