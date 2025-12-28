package com.informatique.mtcit.business.transactions.shared

import android.content.Context
import androidx.core.net.toUri
import com.informatique.mtcit.data.api.InspectionApiService
import com.informatique.mtcit.data.model.CreateInspectionRequestDto
import com.informatique.mtcit.data.model.InspectionDocumentDto
import com.informatique.mtcit.data.model.InspectionFileUpload
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for handling inspection request submission with documents
 * Converts form data to DTO and sends multipart request
 */
@Singleton
class InspectionRequestManager @Inject constructor(
    private val inspectionApiService: InspectionApiService
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
            val crNumber = formData["crNumber"]
                ?: formData["commercialNumber"]
                ?: formData["commercialRegistrationNumber"]
                ?: ""  // Empty string for individual users

            println("✅ Using crNumber: ${if (crNumber.isEmpty()) "\"\" (individual user)" else crNumber}")

            // ✅ DEBUG: Print all formData keys to see what's available
            println("🔍 DEBUG - All formData keys:")
            formData.keys.forEach { key ->
                println("   - $key = ${formData[key]}")
            }

            // ✅ Extract inspection purpose ID (send ID only, not "id|name")
            val inspectionPurposeValue = formData["inspectionPurpose"]
            println("🔍 DEBUG - inspectionPurposeValue: $inspectionPurposeValue")

            var purposeId = extractIdFromLookup(inspectionPurposeValue)
            println("🔍 DEBUG - Extracted purposeId: $purposeId")

            if (purposeId == null) {
                println("⚠️ WARNING: Could not extract inspection purpose ID from: $inspectionPurposeValue")
                println("⚠️ The dropdown is storing display text only, not 'id|name' format")
                println("⚠️ Using default purposeId = 1 as fallback")
                purposeId = 1  // ✅ Use default value
            }
            println("   📋 Using purposeId: $purposeId (from: $inspectionPurposeValue)")

            // ✅ Extract recording port ID (send ID only, not "id|name")
            val inspectionRecordingPortValue = formData["inspectionRecordingPort"]
            var portIdInt = extractIdFromLookup(inspectionRecordingPortValue)
            if (portIdInt == null) {
                println("⚠️ WARNING: Could not extract port ID from: $inspectionRecordingPortValue")
                println("⚠️ The dropdown is storing display text only, not 'id|name' format")
                println("⚠️ Using default portId = 1 as fallback")
                portIdInt = 1  // ✅ Use default value
            }
            val portId = portIdInt.toString()
            println("   📋 Using portId: $portId (from: $inspectionRecordingPortValue)")

            // ✅ Extract authority ID from combined field (send ID only)
            // Format: "authority_id|entity_id" (e.g., "5|12") OR just text
            val authorityAndEntityValue = formData["inspectionAuthorityAndEntity"]
            if (authorityAndEntityValue.isNullOrBlank()) {
                println("❌ No authority and entity selected")
                return InspectionSubmitResult.Error("يرجى اختيار الجهة والهيئة المعتمدة")
            }

            val authorityParts = authorityAndEntityValue.split("|")
            var authorityId = authorityParts[0].toIntOrNull()

            if (authorityId == null) {
                println("⚠️ WARNING: Could not extract authority ID from: $authorityAndEntityValue")
                println("⚠️ The dropdown is storing display text only, not 'id|name' format")
                println("⚠️ Using default authorityId = 1 as fallback")
                authorityId = 1  // ✅ Use default value
            }
            println("   📋 Using authorityId: $authorityId (from: $authorityAndEntityValue)")

            println("✅ Extracted inspection details:")
            println("   ID (requestId): ${requestId ?: "null (adding new ship)"}")
            println("   Ship Info ID: $shipInfoId")
            println("   Purpose ID: $purposeId")
            println("   Authority ID: $authorityId")
            println("   Port ID: $portId")
            println("   crNumber: $crNumber")

            // ✅ Collect uploaded documents and their files
            val documents = mutableListOf<InspectionDocumentDto>()
            val files = mutableListOf<InspectionFileUpload>()

            formData.entries
                .filter { it.key.startsWith("document_") }
                .forEach { (key, value) ->
                    // Extract document ID from key (e.g., "document_123" -> 123)
                    val docId = key.removePrefix("document_").toIntOrNull()
                    if (docId != null && value.isNotBlank() && value != "[]") {
                        println("   📎 Processing document: $key = $value")

                        try {
                            // Parse file URI
                            val fileUri = value.toUri()
                            val fileName = fileUri.lastPathSegment ?: "document_$docId.pdf"

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

            // ✅ Create DTO with correct structure
            val dto = CreateInspectionRequestDto(
                id = requestId,             // Use actual requestId (from proceed-request or create request)
                shipInfoId = shipInfoId,    // Ship info ID
                purposeId = purposeId,      // Inspection purpose ID
                authorityId = authorityId,  // Authority ID
                portId = portId,            // Port ID as string
                crNumber = crNumber,        // Commercial registration number (or default)
                documents = documents       // Documents metadata
            )

            // ✅ Call API
            println("📤 Calling InspectionApiService...")
            val result = inspectionApiService.createInspectionRequest(dto, files)

            result.fold(
                onSuccess = { response ->
                    println("✅ Inspection request submitted successfully!")
                    println("   Message: ${response.message}")
                    println("=".repeat(80))

                    InspectionSubmitResult.Success(
                        message = response.message,
                        requestId = response.data.id
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

