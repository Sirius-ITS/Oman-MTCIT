package com.informatique.mtcit.business.transactions.shared

import com.informatique.mtcit.R
import com.informatique.mtcit.common.FormField
import com.informatique.mtcit.data.model.RequiredDocumentItem
import com.informatique.mtcit.ui.viewmodels.StepData

/**
 * Helper object to create dynamic document upload steps
 * based on required documents from API
 */
object DynamicDocumentSteps {

    /**
     * Create a dynamic document upload step based on required documents
     *
     * @param requiredDocuments List of required documents from API
     * @param stepTitleRes Resource ID for step title (default: رفع المستندات)
     * @param stepDescriptionRes Resource ID for step description
     * @param allowedTypes Allowed file types (default: pdf, jpg, jpeg, png, doc, docx)
     * @param maxSizeMB Maximum file size in MB (default: 5)
     * @return StepData with dynamic file upload fields
     */
    fun createDocumentsStep(
        requiredDocuments: List<RequiredDocumentItem>,
        stepTitleRes: Int = R.string.upload_documents,
        stepDescriptionRes: Int = R.string.upload_documents_description,
        allowedTypes: List<String> = listOf("pdf", "jpg", "jpeg", "png", "doc", "docx"),
        maxSizeMB: Int = 5
    ): StepData {
        val fields = mutableListOf<FormField>()

        // Sort documents by order
        val sortedDocuments = requiredDocuments
            .filter { it.document.isActive == 1 } // Only active documents
            .sortedBy { it.document.docOrder }

        // Create file upload field for each document
        sortedDocuments.forEach { docItem ->
            val document = docItem.document

            fields.add(
                FormField.FileUpload(
                    id = "document_${document.id}",
                    label = document.nameAr, // Use Arabic name as label (String, not resource ID)
                    // Alternative: use labelRes if you have mapped resources
                    // labelRes = getDocumentLabelResource(document.id),
                    allowedTypes = allowedTypes,
                    maxSizeMB = maxSizeMB,
                    mandatory = document.isMandatory == 1
                )
            )
        }

        return StepData(
            titleRes = stepTitleRes,
            descriptionRes = stepDescriptionRes,
            fields = fields
        )
    }

    /**
     * Create multiple document steps grouped by category
     * Useful if you have many documents and want to split them
     *
     * @param requiredDocuments List of required documents from API
     * @param documentsPerStep Maximum documents per step (default: 5)
     * @return List of StepData, one for each group
     */
    fun createGroupedDocumentSteps(
        requiredDocuments: List<RequiredDocumentItem>,
        documentsPerStep: Int = 5
    ): List<StepData> {
        val steps = mutableListOf<StepData>()

        val sortedDocuments = requiredDocuments
            .filter { it.document.isActive == 1 }
            .sortedBy { it.document.docOrder }

        // Split into groups
        val groups = sortedDocuments.chunked(documentsPerStep)

        groups.forEachIndexed { _, group ->
            val fields = group.map { docItem ->
                FormField.FileUpload(
                    id = "document_${docItem.document.id}",
                    label = docItem.document.nameAr, // Use String label
                    allowedTypes = listOf("pdf", "jpg", "jpeg", "png", "doc", "docx"),
                    maxSizeMB = 5,
                    mandatory = docItem.document.isMandatory == 1
                )
            }

            steps.add(
                StepData(
                    titleRes = R.string.upload_documents, // Or use "Upload Documents (${index + 1}/${groups.size})"
                    descriptionRes = R.string.upload_documents_description,
                    fields = fields
                )
            )
        }

        return steps
    }

    /**
     * Separate mandatory and optional documents into different steps
     *
     * @param requiredDocuments List of required documents from API
     * @return Pair of StepData (mandatory step, optional step)
     */
    fun createSeparatedDocumentSteps(
        requiredDocuments: List<RequiredDocumentItem>
    ): Pair<StepData?, StepData?> {
        val activeDocuments = requiredDocuments
            .filter { it.document.isActive == 1 }
            .sortedBy { it.document.docOrder }

        val mandatoryDocs = activeDocuments.filter { it.document.isMandatory == 1 }
        val optionalDocs = activeDocuments.filter { it.document.isMandatory == 0 }

        val mandatoryStep = if (mandatoryDocs.isNotEmpty()) {
            createDocumentsStep(
                requiredDocuments = mandatoryDocs,
                stepTitleRes = R.string.upload_mandatory_documents,
                stepDescriptionRes = R.string.upload_mandatory_documents_description
            )
        } else null

        val optionalStep = if (optionalDocs.isNotEmpty()) {
            createDocumentsStep(
                requiredDocuments = optionalDocs,
                stepTitleRes = R.string.upload_optional_documents,
                stepDescriptionRes = R.string.upload_optional_documents_description
            )
        } else null

        return Pair(mandatoryStep, optionalStep)
    }

    /**
     * Validate that all mandatory documents are uploaded
     *
     * @param requiredDocuments List of required documents from API
     * @param formData Current form data with uploaded documents
     * @return Pair(isValid, errorMessages)
     */
    fun validateRequiredDocuments(
        requiredDocuments: List<RequiredDocumentItem>,
        formData: Map<String, String>
    ): Pair<Boolean, Map<String, String>> {
        val errors = mutableMapOf<String, String>()

        // Check each mandatory document
        requiredDocuments
            .filter { it.document.isActive == 1 && it.document.isMandatory == 1 }
            .forEach { docItem ->
                val fieldId = "document_${docItem.document.id}"
                val uploadedValue = formData[fieldId]

                if (uploadedValue.isNullOrBlank()) {
                    errors[fieldId] = "يجب رفع ${docItem.document.nameAr}"
                }
            }

        return Pair(errors.isEmpty(), errors)
    }

    /**
     * Get summary of uploaded documents for review step
     *
     * @param requiredDocuments List of required documents from API
     * @param formData Current form data with uploaded documents
     * @return Map of document names to upload status
     */
    fun getDocumentsSummary(
        requiredDocuments: List<RequiredDocumentItem>,
        formData: Map<String, String>
    ): Map<String, DocumentUploadStatus> {
        val summary = mutableMapOf<String, DocumentUploadStatus>()

        requiredDocuments
            .filter { it.document.isActive == 1 }
            .sortedBy { it.document.docOrder }
            .forEach { docItem ->
                val fieldId = "document_${docItem.document.id}"
                val uploadedValue = formData[fieldId]

                summary[docItem.document.nameAr] = DocumentUploadStatus(
                    documentId = docItem.document.id,
                    documentName = docItem.document.nameAr,
                    isMandatory = docItem.document.isMandatory == 1,
                    isUploaded = !uploadedValue.isNullOrBlank(),
                    fileName = extractFileName(uploadedValue)
                )
            }

        return summary
    }

    private fun extractFileName(fileUri: String?): String? {
        if (fileUri.isNullOrBlank()) return null
        // Extract filename from content:// URI or regular path
        return fileUri.substringAfterLast("/")
    }
}

/**
 * Status of a document upload
 */
data class DocumentUploadStatus(
    val documentId: Int,
    val documentName: String,
    val isMandatory: Boolean,
    val isUploaded: Boolean,
    val fileName: String? = null
)

