package com.informatique.mtcit.business.transactions.shared

import android.content.Context
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.informatique.mtcit.data.api.InspectionApiService
import com.informatique.mtcit.data.model.CreateInspectionRequestDto
import com.informatique.mtcit.data.model.InspectionDocumentDto
import com.informatique.mtcit.data.model.InspectionFileUpload
import com.informatique.mtcit.data.repository.LookupRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for handling inspection request submission with documents
 * Converts form data to DTO and sends multipart request
 */
@Singleton
class InspectionRequestManager @Inject constructor(
    private val inspectionApiService: InspectionApiService,
    private val lookupRepository: LookupRepository
) {

    /**
     * Submit inspection request with documents from review step
     *
     * @param formData Accumulated form data containing all inspection details
     * @param context Android context for file access
     * @return Result indicating success or failure
     */
    suspend fun submitInspectionRequest(
        formData: Map<String, String>,
        context: Context
    ): InspectionSubmitResult {
        return try {
            println("=".repeat(80))
            println("🔍 InspectionRequestManager: Submitting inspection request...")
            println("=".repeat(80))

            // ✅ Extract requestId
            // Case 1: User selected existing ship → comes from proceed-request API
            // Case 2: User added new ship → comes from create registration request API
            // Case 3: User just started (no requestId yet) → null
            val requestId = formData["requestId"]?.toIntOrNull()

            if (requestId == null) {
                println("⚠️ No requestId found - user is adding new ship, will send id = null")
            } else {
                println("✅ Found requestId: $requestId")
            }

            // ✅ Extract shipInfoId (from selected ship or created ship)
            var shipInfoId = formData["shipInfoId"]?.toIntOrNull()

            // ✅ Fallback: If shipInfoId not found, try using requestId or shipId
            if (shipInfoId == null) {
                println("⚠️ shipInfoId not found in formData, trying fallbacks...")

                // Try requestId
                shipInfoId = formData["requestId"]?.toIntOrNull()
                if (shipInfoId != null) {
                    println("   ✅ Using requestId as shipInfoId: $shipInfoId")
                } else {
                    // Try shipId
                    shipInfoId = formData["shipId"]?.toIntOrNull()
                    if (shipInfoId != null) {
                        println("   ✅ Using shipId as shipInfoId: $shipInfoId")
                    } else {
                        println("❌ No shipInfoId, requestId, or shipId found in formData")
                        println("❌ Available keys: ${formData.keys.joinToString(", ")}")
                        return InspectionSubmitResult.Error("لم يتم العثور على معرف السفينة")
                    }
                }
            } else {
                println("✅ Found shipInfoId in formData: $shipInfoId")
            }

            // ✅ Extract crNumber (commercial registration number if company, else empty string)
            val personType = formData["selectionPersonType"]
            val crNumber = if (personType == "شركة") {
                formData["selectionData"]
                    ?: formData["crNumber"]
                    ?: formData["commercialNumber"]
                    ?: formData["commercialRegistrationNumber"]
                    ?: ""
            } else {
                ""  // Empty string for individuals
            }

            println("✅ Person Type: '$personType'")
            println("✅ Using crNumber: ${if (crNumber.isEmpty()) "\"\" (individual or not provided)" else "'$crNumber' (company)"}")

            // ✅ DEBUG: Print all formData keys to see what's available
            println("🔍 DEBUG - All formData keys:")
            formData.keys.forEach { key ->
                println("   - $key = ${formData[key]}")
            }

            // ✅ Extract inspection purpose ID (send ID only, not "id|name")
            val inspectionPurposeValue = formData["inspectionPurpose"]
            println("🔍 DEBUG - inspectionPurposeValue: '$inspectionPurposeValue'")

            // Try extracting ID from "id|name" format first
            val purposeId = extractIdFromLookup(inspectionPurposeValue)
                ?: lookupRepository.getInspectionPurposeId(inspectionPurposeValue ?: "")

            if (purposeId == null) {
                println("❌ ERROR: Could not resolve inspection purpose ID for value: '$inspectionPurposeValue'")
                println("   Available keys: ${formData.keys}")
                return InspectionSubmitResult.Error("تعذر تحديد معرف الغرض من المعاينة")
            }
            println("   ✅ Resolved purposeId: $purposeId (from: $inspectionPurposeValue)")

            // ✅ Extract inspection place ID from "placeId" field (send ID as Int)
            val inspectionPlaceValue = formData["placeId"]  // Changed from "inspectionPlace" to "placeId"
            println("🔍 DEBUG - placeId value: '$inspectionPlaceValue'")

            // Place ID is integer, extract from "id|name" format or lookup
            val placeId = if (inspectionPlaceValue?.contains("|") == true) {
                val parts = inspectionPlaceValue.split("|")
                parts[0].trim().toIntOrNull()  // Take first part as place ID
            } else {
                lookupRepository.getInspectionPlaceId(inspectionPlaceValue ?: "")
            }

            if (placeId == null) {
                println("❌ ERROR: Could not resolve place ID for value: '$inspectionPlaceValue'")
                return InspectionSubmitResult.Error("تعذر تحديد معرف موقع المعاينة")
            }
            println("   ✅ Resolved placeId: $placeId (from: $inspectionPlaceValue)")

            // ✅ Extract authority ID from combined field (send ID only)
            // Format: "id|name" (e.g., "128|authority name")
            val authorityAndEntityValue = formData["inspectionAuthorityAndEntity"]
            println("🔍 DEBUG - authorityAndEntityValue: '$authorityAndEntityValue'")

            if (authorityAndEntityValue.isNullOrBlank()) {
                println("❌ No authority and entity selected")
                return InspectionSubmitResult.Error("يرجى اختيار الجهة والهيئة المعتمدة")
            }

            // Try extracting ID from "id|name" format first
            val authorityId = extractIdFromLookup(authorityAndEntityValue)
                ?: lookupRepository.getInspectionAuthorityId(authorityAndEntityValue)

            if (authorityId == null) {
                println("❌ ERROR: Could not resolve authority ID for value: '$authorityAndEntityValue'")
                return InspectionSubmitResult.Error("تعذر تحديد معرف الجهة المعتمدة للمعاينة")
            }
            println("   ✅ Resolved authorityId: $authorityId (from: $authorityAndEntityValue)")

            println("✅ Extracted inspection details:")
            println("   Ship Info ID: $shipInfoId")
            println("   Purpose ID: $purposeId")
            println("   Authority ID: $authorityId")
            println("   Place ID: $placeId")
            println("   crNumber: $crNumber")

            // ✅ Collect uploaded documents and their files
            // ✅ Filter ONLY inspection-specific documents (prefix: "inspection_document_")
            // ✅ This avoids collecting documents from parent transaction (e.g., permanent registration)
            val documents = mutableListOf<InspectionDocumentDto>()
            val files = mutableListOf<InspectionFileUpload>()

            println("🔍 DEBUG - Collecting inspection documents from formData...")
            println("   Total formData keys: ${formData.keys.size}")

            formData.entries
                .filter { it.key.startsWith("inspection_document_") }  // ✅ Changed filter
                .forEach { (key, value) ->
                    // Extract document ID from key (e.g., "inspection_document_123" -> 123)
                    val docId = key.removePrefix("inspection_document_").toIntOrNull()  // ✅ Changed prefix
                    if (docId != null && value.isNotBlank() && value != "[]") {
                        println("   📎 Processing inspection document: $key = $value")

                        try {
                            // Parse file URI
                            val fileUri = value.toUri()

                            // ✅ Get actual file name using helper
                            val fileName = getFileName(context, fileUri) ?: run {
                                val fallback = fileUri.lastPathSegment ?: "document_$docId.pdf"
                                println("      ⚠️ Could not resolve display name, using: $fallback")
                                fallback
                            }

                            println("      ✅ Resolved fileName: '$fileName' from URI")

                            // Read file bytes
                            val inputStream = context.contentResolver.openInputStream(fileUri)
                            val fileBytes = inputStream?.readBytes()
                            inputStream?.close()

                            if (fileBytes != null && fileBytes.isNotEmpty()) {
                                // Determine MIME type
                                val mimeType = context.contentResolver.getType(fileUri)
                                    ?: guessMimeType(fileName)

                                // Add to documents metadata list
                                documents.add(
                                    InspectionDocumentDto(
                                        fileName = fileName,
                                        documentId = docId
                                    )
                                )

                                // Add to files list
                                files.add(
                                    InspectionFileUpload(
                                        documentId = docId,
                                        fileName = fileName,
                                        fileBytes = fileBytes,
                                        mimeType = mimeType
                                    )
                                )

                                println("      ✅ Added file: $fileName (${fileBytes.size} bytes, $mimeType)")
                            } else {
                                println("      ⚠️ File is empty or unreadable: $value")
                            }
                        } catch (e: Exception) {
                            println("      ❌ Failed to read file: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }

            println("📄 Total documents: ${documents.size}, Total files: ${files.size}")

            // ✅ Extract parent transaction info (if inspection triggered from another transaction)
            // These will be null when creating inspection request directly from inspection transaction
            val needInspectionRequestId = formData["needInspectionRequestId"]?.toIntOrNull()
            val needInspectionRequestTypeId = formData["needInspectionRequestTypeId"]?.toIntOrNull()

            if (needInspectionRequestId != null && needInspectionRequestTypeId != null) {
                println("✅ Inspection triggered from parent transaction:")
                println("   Parent Request ID: $needInspectionRequestId")
                println("   Parent Request Type: $needInspectionRequestTypeId (1=temp, 2=perm, 3=issue nav, 5=renew nav)")
            } else {
                println("ℹ️ Standalone inspection request (no parent transaction)")
            }

            // ✅ Create DTO with correct structure (no 'id' field)
            val dto = CreateInspectionRequestDto(
                shipInfoId = shipInfoId,    // Ship info ID
                purposeId = purposeId,      // Inspection purpose ID
                authorityId = authorityId,  // Authority ID
                placeId = placeId,          // Inspection place ID as string
                crNumber = crNumber,        // Commercial registration number
                documents = documents,      // Documents metadata
                needInspectionRequestId = needInspectionRequestId,        // Parent request ID (null if standalone)
                needInspectionRequestTypeId = needInspectionRequestTypeId // Parent request type (null if standalone)
            )

            // ✅ DEBUG: Print final DTO values before sending
            println("=" .repeat(80))
            println("📤 FINAL REQUEST DTO VALUES:")
            println("   shipInfoId: ${dto.shipInfoId}")
            println("   purposeId: ${dto.purposeId}")
            println("   authorityId: ${dto.authorityId}")
            println("   placeId: '${dto.placeId}'")
            println("   crNumber: '${dto.crNumber}'")
            println("   needInspectionRequestId: ${dto.needInspectionRequestId ?: "null (standalone)"}")
            println("   needInspectionRequestTypeId: ${dto.needInspectionRequestTypeId ?: "null (standalone)"}")
            println("   documents (${dto.documents.size} items):")
            dto.documents.forEachIndexed { index, doc ->
                println("      [$index] fileName='${doc.fileName}', documentId=${doc.documentId}")
            }
            println("=" .repeat(80))

            // ✅ Call API
            println("📤 Calling InspectionApiService...")
            val result = inspectionApiService.createInspectionRequest(dto, files)

            result.fold(
                onSuccess = { response ->
                    println("✅ Inspection request submitted successfully!")
                    println("   Message: ${response.message}")
                    println("   Request ID: ${response.data}")
                    println("=".repeat(80))

                    InspectionSubmitResult.Success(
                        message = response.message,
                        requestId = response.data
                    )
                },
                onFailure = { error ->
                    println("❌ Inspection request submission failed: ${error.message}")
                    println("=".repeat(80))

                    InspectionSubmitResult.Error(
                        message = error.message ?: "فشل في إرسال طلب المعاينة"
                    )
                }
            )
        } catch (e: Exception) {
            println("❌ Exception in submitInspectionRequest: ${e.message}")
            e.printStackTrace()
            println("=".repeat(80))

            InspectionSubmitResult.Error(
                message = "حدث خطأ أثناء إرسال طلب المعاينة: ${e.message}"
            )
        }
    }

    /**
     * Extract ID from lookup string format
     * Format: "id|name" -> id
     * Also handles case where only name is stored
     */
    private fun extractIdFromLookup(value: String?): Int? {
        println("🔍 extractIdFromLookup called with: '$value'")

        if (value.isNullOrBlank()) {
            println("   ❌ Value is null or blank")
            return null
        }

        return try {
            // Try to parse as direct number first
            val directParse = value.toIntOrNull()
            if (directParse != null) {
                println("   ✅ Parsed as direct number: $directParse")
                return directParse
            }

            // Try to extract from "id|name" format
            val parts = value.split("|")
            println("   🔍 Split by '|': parts.size = ${parts.size}, parts = $parts")

            if (parts.size >= 2) {
                // Format is "id|name"
                val id = parts[0].toIntOrNull()
                println("   🔍 First part: '${parts[0]}' → Parsed ID: $id")
                return id
            } else {
                // Only name is stored (e.g., "استثنائية")
                // This is a fallback - the dropdown should store "id|name" but sometimes only stores name
                println("   ⚠️ Only name found, no ID prefix: '$value'")
                println("   ⚠️ Cannot extract ID from name alone")
                return null
            }
        } catch (e: Exception) {
            println("⚠️ Exception extracting ID from: $value - ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Resolve the display name of a content Uri to ensure we send the exact picked filename
     */
    private fun getFileName(context: Context, uri: android.net.Uri): String? {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else null
            } ?: uri.lastPathSegment
        } catch (e: Exception) {
            println("⚠️ Failed to resolve file name from uri: $uri, ${e.message}")
            uri.lastPathSegment
        }
    }

    /**
     * Guess MIME type from file extension
     */
    private fun guessMimeType(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            else -> "application/octet-stream"
        }
    }
}

/**
 * Result of inspection request submission
 */
sealed class InspectionSubmitResult {
    data class Success(
        val message: String,
        val requestId: Int
    ) : InspectionSubmitResult()

    data class Error(
        val message: String
    ) : InspectionSubmitResult()
}

