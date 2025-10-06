package com.informatique.mtcit.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class StepState {
    COMPLETED, CURRENT, UPCOMING
}

/**
 * Dynamic, reusable horizontal stepper for Material 3.
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top
    ) {
        steps.forEachIndexed { index, stepTitle ->
            val stepState = when (index) {
                in completedSteps -> StepState.COMPLETED
                currentStep -> StepState.CURRENT
                else -> StepState.UPCOMING
            }

            // Each step item with proper weight distribution
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StepItem(
                    stepNumber = index + 1,
                    title = stepTitle,
                    stepState = stepState,
                    isClickable = index <= currentStep || index in completedSteps,
                    onClick = { onStepClick(index) }
                )
            }

            // Connector between steps (except for the last step)
            if (index < steps.size - 1) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StepConnector(
                        isCompleted = index in completedSteps && (index + 1) in completedSteps,
                        isActive = index == currentStep - 1,
                        modifier = Modifier.width(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StepItem(
    stepNumber: Int,
    title: String,
    stepState: StepState,
    isClickable: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(enabled = isClickable) { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Step Circle
        val backgroundColor by animateColorAsState(
            targetValue = when (stepState) {
                StepState.COMPLETED -> MaterialTheme.colorScheme.primary
                StepState.CURRENT -> MaterialTheme.colorScheme.primary
                StepState.UPCOMING -> MaterialTheme.colorScheme.outline
            },
            label = "StepBackgroundColor"
        )

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            if (stepState == StepState.COMPLETED) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "مكتمل",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(
                    text = stepNumber.toString(),
                    color = when (stepState) {
                        StepState.CURRENT -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Step Title with better text handling
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = when (stepState) {
                StepState.CURRENT -> MaterialTheme.colorScheme.onPrimaryContainer
                StepState.COMPLETED -> MaterialTheme.colorScheme.onSurface
                StepState.UPCOMING -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = if (stepState == StepState.CURRENT) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 3,
            lineHeight = 14.sp,
            modifier = Modifier.width(60.dp)
        )
    }
}

@Composable
private fun StepConnector(
    isCompleted: Boolean,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(2.dp)
            .background(
                when {
                    isCompleted -> MaterialTheme.colorScheme.primary
                    isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.outline
                }
            )
    )
}
