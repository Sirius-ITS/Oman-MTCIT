package com.informatique.mtcit.data.model

import kotlinx.serialization.Serializable

/**
 * Checklist Settings Response
 * API: /api/v1/checklist-settings/by-purpose/{purposeId}
 */
@Serializable
data class ChecklistSettingsResponse(
    val message: String,
    val statusCode: Int,
    val success: Boolean,
    val timestamp: String,
    val data: ChecklistSettingsData
)

/**
 * Checklist Settings Data wrapper
 */
@Serializable
data class ChecklistSettingsData(
    val id: Int,
    val nameAr: String,
    val nameEn: String,
    val name: String,
    val isActive: Boolean,
    val purpose: ChecklistPurpose,
    val notesAr: String? = null,
    val notesEn: String? = null,
    val items: List<ChecklistItem>
)

/**
 * Checklist Purpose info
 */
@Serializable
data class ChecklistPurpose(
    val id: Int,
    val nameAr: String,
    val nameEn: String,
    val name: String
)

/**
 * Single checklist item representing a field/question
 */
@Serializable
data class ChecklistItem(
    val id: Int,
    val question: String,
    val checklistType: ChecklistType,
    val isMandatory: Boolean = false,
    val itemOrder: Int = 0,
    val isActive: Boolean = true,
    val note: String? = null,
    val choices: List<ChecklistChoice> = emptyList()
)

/**
 * Checklist type (defines field type)
 */
@Serializable
data class ChecklistType(
    val id: Int,
    val nameAr: String,
    val nameEn: String,
    val name: String
)

/**
 * Checklist choice (for dropdown/list fields)
 */
@Serializable
data class ChecklistChoice(
    val id: Int,
    val answer: String,
    val isActive: Boolean = true
)

/**
 * Work Order Result containing checklist answers
 * This comes from request detail API when inspection is completed
 */
@Serializable
data class WorkOrderResult(
    val id: Int? = null,
    val checklistAnswers: List<ChecklistAnswer>,
    val checklistItems: List<ChecklistItem> = emptyList()  // ✅ Full checklist structure for form display
)

/**
 * Single checklist answer
 */
@Serializable
data class ChecklistAnswer(
    val checklistItemId: Int,
    val fieldNameAr: String,
    val fieldNameEn: String,
    val answer: String
)

