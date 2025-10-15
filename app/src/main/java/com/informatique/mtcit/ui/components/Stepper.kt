//package com.informatique.mtcit.ui.components
//
//import androidx.compose.animation.animateColorAsState
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.horizontalScroll
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Check
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.informatique.mtcit.ui.theme.LocalExtraColors
//
//enum class StepState {
//    COMPLETED, CURRENT, UPCOMING
//}
//
///**
// * Dynamic, reusable horizontal stepper for Material 3.
// * @param steps List of step titles
// * @param currentStep Currently active step (0-indexed)
// * @param completedSteps Set of completed step indices
// * @param onStepClick Callback when a step is clicked
// * @param modifier Modifier for styling
// */
//@Composable
//fun DynamicStepper(
//    modifier: Modifier = Modifier,
//    steps: List<String>,
//    currentStep: Int,
//    completedSteps: Set<Int> = emptySet(),
//    onStepClick: (Int) -> Unit = {},
//) {
//    val scrollState = rememberScrollState()
//
//    // Auto-scroll to current step when it changes - improved calculation
//    LaunchedEffect(currentStep) {
//        if (steps.size > 3) { // Start scrolling when more than 3 steps
//            val stepWidth = 85 // Total width per step (including connectors)
//            val containerWidth = 300 // Approximate visible container width
//            val targetPosition = (currentStep * stepWidth) - (containerWidth / 3)
//            val maxScroll = scrollState.maxValue
//            val targetScroll = targetPosition.coerceIn(0, maxScroll)
//
//            // Smooth animated scroll to center the current step
//            scrollState.animateScrollTo(targetScroll)
//        }
//    }
//
//    Row(
//        modifier = modifier
//            .fillMaxWidth()
//            .horizontalScroll(scrollState)
//            .padding(horizontal = 8.dp, vertical = 12.dp),
//        horizontalArrangement = Arrangement.Center, // Changed from spacedBy(0.dp) to Center
//        verticalAlignment = Alignment.Top
//    ) {
//        steps.forEachIndexed { index, stepTitle ->
//            // Fixed logic: current step should NEVER be COMPLETED, only previous steps
//            val stepState = when {
//                index < currentStep && index in completedSteps -> StepState.COMPLETED
//                index == currentStep -> StepState.CURRENT
//                else -> StepState.UPCOMING
//            }
//
//            // Each step with connector integrated
//            Row(
//                verticalAlignment = Alignment.Top
//            ) {
//                // Step item
//                Column(
//                    modifier = Modifier.width(65.dp), // Reduced from 80dp to 65dp
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//                    StepItem(
//                        stepNumber = index + 1,
//                        title = stepTitle,
//                        stepState = stepState,
//                        isClickable = index <= currentStep || index in completedSteps,
//                        onClick = { onStepClick(index) }
//                    )
//                }
//
//                // Connector line positioned at center of circle (except for last step)
//                if (index < steps.size - 1) {
//                    Box(
//                        modifier = Modifier.padding(top = 18.dp) // 18dp = circle center (36dp/2)
//                    ) {
//                        StepConnector(
//                            isCompleted = index < currentStep && index in completedSteps,
//                            isActive = index == currentStep - 1,
//                            modifier = Modifier.width(20.dp) // Increased width to nearly touch circles
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun StepItem(
//    stepNumber: Int,
//    title: String,
//    stepState: StepState,
//    isClickable: Boolean,
//    onClick: () -> Unit
//) {
//    val extraColors = LocalExtraColors.current
//    Column(
//        modifier = Modifier
//            .clickable(enabled = isClickable) { onClick() }
//            .padding(4.dp),
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        // Step Circle
//        val backgroundColor by animateColorAsState(
//            targetValue = when (stepState) {
//                StepState.COMPLETED -> extraColors.blue3
//                StepState.CURRENT -> extraColors.blue3
//                StepState.UPCOMING -> extraColors.steppunselected
//            },
//            label = "StepBackgroundColor"
//        )
//
//        Box(
//            modifier = Modifier
//                .size(36.dp)
//                .clip(CircleShape)
//                .background(backgroundColor),
//            contentAlignment = Alignment.Center
//        ) {
//            if (stepState == StepState.COMPLETED) {
//                Icon(
//                    imageVector = Icons.Default.Check,
//                    contentDescription = "مكتمل",
//                    tint = MaterialTheme.colorScheme.onPrimary,
//                    modifier = Modifier.size(18.dp)
//                )
//            } else {
//                Text(
//                    text = stepNumber.toString(),
//                    color = when (stepState) {
//                        StepState.CURRENT -> extraColors.white
//                        else -> extraColors.blue2
//                    },
//                    style = MaterialTheme.typography.labelMedium,
//                    fontWeight = FontWeight.Bold
//                )
//            }
//        }
//
//        Spacer(modifier = Modifier.height(4.dp)) // Reduced spacer height
//
//        // Step Title with better text handling
//        Text(
//            text = title,
//            style = MaterialTheme.typography.labelSmall,
//            color = when (stepState) {
//                StepState.CURRENT -> MaterialTheme.colorScheme.onPrimaryContainer
//                StepState.COMPLETED -> MaterialTheme.colorScheme.onSurface
//                StepState.UPCOMING -> MaterialTheme.colorScheme.onSurfaceVariant
//            },
//            fontWeight = if (stepState == StepState.CURRENT) FontWeight.Bold else FontWeight.Normal,
//            textAlign = TextAlign.Center,
//            maxLines = 2, // Increased max lines to 2 for wrapping
//            lineHeight = 14.sp,
//            modifier = Modifier.width(60.dp)
//        )
//    }
//}
//
//@Composable
//private fun StepConnector(
//    isCompleted: Boolean,
//    isActive: Boolean,
//    modifier: Modifier = Modifier
//) {
//    Box(
//        modifier = modifier
//            .height(2.dp)
//            .background(
//                when {
//                    isCompleted -> MaterialTheme.colorScheme.primary
//                    isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
//                    else -> MaterialTheme.colorScheme.outline
//                }
//            )
//    )
//}
package com.informatique.mtcit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.informatique.mtcit.ui.theme.LocalExtraColors
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class StepState {
    COMPLETED, CURRENT, UPCOMING
}

/**
 * Dynamic, reusable linear progress stepper for Material 3.
 * @param steps List of step titles
 * @param currentStep Currently active step (0-indexed)
 * @param completedSteps Set of completed step indices
 * @param onStepClick Callback when a step is clicked
 * @param modifier Modifier for styling
 */
@Composable
fun DynamicStepper(
    modifier: Modifier = Modifier,
    steps: List<String>,
    currentStep: Int,
    completedSteps: Set<Int> = emptySet(),
    onStepClick: (Int) -> Unit = {},
) {
    val extraColors = LocalExtraColors.current
    val totalSteps = steps.size
    val progressPercentage = ((currentStep + 1).toFloat() / totalSteps.toFloat() * 100).toInt()

    // Detect if RTL (Arabic)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    // Format numbers based on language
    fun formatNumber(number: Int): String {
        return if (isRtl) {
            // Arabic numerals
            val arabicSymbols = DecimalFormatSymbols(Locale("ar"))
            DecimalFormat("#", arabicSymbols).format(number)
        } else {
            // English numerals
            val englishSymbols = DecimalFormatSymbols(Locale("en"))
            DecimalFormat("#", englishSymbols).format(number)
        }
    }

    val stepText = if (isRtl) "خطوة" else "Step"
    val ofText = if (isRtl) "من" else "of"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Step indicator text (e.g., "Step 4 of 7" or "خطوة ٤ من ٧")
        Text(
            text = "$stepText ${formatNumber(currentStep + 1)} $ofText ${formatNumber(totalSteps)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Progress bar container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(extraColors.steppunselected)
        ) {
            // Progress indicator
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (currentStep + 1).toFloat() / totalSteps.toFloat())
                    .clip(RoundedCornerShape(4.dp))
                    .background(extraColors.blue3)
            )
        }

        // Percentage text
        Text(
            text = "${formatNumber(progressPercentage)}%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(top = 8.dp)
        )
    }
}