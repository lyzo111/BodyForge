package com.bodyforge.ui.components.inputs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BodyweightInput(
    bodyweight: Double,
    onBodyweightChange: (Double) -> Unit
) {
    Card(
        backgroundColor = Color(0xFF0F766E),
        elevation = 0.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "💪",
                    fontSize = 20.sp
                )
                Text(
                    text = "Your Bodyweight:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        if (bodyweight > 30.0) {
                            val newWeight = (bodyweight - 0.5).coerceAtLeast(30.0)
                            onBodyweightChange(newWeight)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (bodyweight > 30.0) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(48.dp),
                    contentPadding = PaddingValues(0.dp),
                    elevation = ButtonDefaults.elevation(0.dp)
                ) {
                    Text(
                        text = "−",
                        fontSize = 24.sp,
                        color = if (bodyweight > 30.0) Color.White else Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                }

                var textValue by remember(bodyweight) { mutableStateOf(formatWeight(bodyweight)) }
                var isEditing by remember { mutableStateOf(false) }

                if (isEditing) {
                    BasicTextField(
                        value = textValue,
                        onValueChange = { newText ->
                            val filtered = newText.filter { it.isDigit() || it == '.' }
                            if (filtered.count { it == '.' } <= 1 && filtered.length <= 8) {
                                textValue = filtered
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        textStyle = TextStyle(
                            fontSize = 18.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val newValue = parseWeightInput(textValue).coerceIn(30.0, 999.0)
                                onBodyweightChange(newValue)
                                textValue = formatWeight(newValue)
                                isEditing = false
                            }
                        )
                    )
                } else {
                    Text(
                        text = "${formatWeight(bodyweight)} kg",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                isEditing = true
                                textValue = formatWeight(bodyweight)
                            }
                            .background(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = {
                        if (bodyweight < 999.0) {
                            val newWeight = (bodyweight + 0.5).coerceAtMost(999.0)
                            onBodyweightChange(newWeight)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (bodyweight < 999.0) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(48.dp),
                    contentPadding = PaddingValues(0.dp),
                    elevation = ButtonDefaults.elevation(0.dp)
                ) {
                    Text(
                        text = "+",
                        fontSize = 24.sp,
                        color = if (bodyweight < 999.0) Color.White else Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Helper functions
private fun formatWeight(weight: Double): String {
    return if (weight % 1.0 == 0.0) {
        weight.toInt().toString()
    } else {
        String.format("%.3f", weight).trimEnd('0').trimEnd('.')
    }
}

private fun parseWeightInput(input: String): Double {
    if (input.isEmpty() || input == ".") return 0.0

    val cleanInput = when {
        input.startsWith(".") -> "0$input"
        input.endsWith(".") -> input.dropLast(1)
        else -> input
    }

    cleanInput.toDoubleOrNull()?.let { parsed ->
        return parsed.coerceIn(0.0, 9999.0)
    }

    val withoutLeadingZeros = cleanInput.trimStart('0').ifEmpty { "0" }
    withoutLeadingZeros.toDoubleOrNull()?.let { parsed ->
        return parsed.coerceIn(0.0, 9999.0)
    }

    val numbersOnly = cleanInput.filter { it.isDigit() }
    return if (numbersOnly.isEmpty()) {
        0.0
    } else {
        numbersOnly.toDoubleOrNull()?.coerceIn(0.0, 9999.0) ?: 0.0
    }
}