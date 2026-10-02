package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun ContextGaugeBar(
    tokensUsed: Int,
    maxContextTokens: Int,
    contextTurnsLimit: Int,
    onOpenAdjustDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (tokensUsed.toFloat() / maxContextTokens.toFloat()).coerceIn(0f, 1f)
    val percentage = (progress * 100).toInt()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenAdjustDialog() }
            .testTag("context_window_gauge_bar"),
        color = Slate100,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Context:",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Slate300)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (progress > 0.85f) Rose600
                                else if (progress > 0.65f) Amber600
                                else Indigo600
                            )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "$tokensUsed / ${maxContextTokens / 1000}k tokens ($contextTurnsLimit turns)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = Slate600,
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Adjust Context Window",
                tint = Indigo600,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
