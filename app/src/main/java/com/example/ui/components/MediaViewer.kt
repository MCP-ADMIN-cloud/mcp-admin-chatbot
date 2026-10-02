package com.example.ui.components

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.*

@Composable
fun MediaAttachmentCard(
    mediaUri: String?,
    mediaType: String?,
    mediaName: String?,
    modifier: Modifier = Modifier
) {
    if (mediaType == null) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("media_attachment_card"),
        shape = RoundedCornerShape(12.dp),
        color = Slate50,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
    ) {
        when (mediaType) {
            "IMAGE" -> {
                Column(modifier = Modifier.padding(6.dp)) {
                    AsyncImage(
                        model = mediaUri,
                        contentDescription = mediaName ?: "Attached Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    if (!mediaName.isNullOrBlank()) {
                        Text(
                            text = mediaName,
                            style = MaterialTheme.typography.labelSmall.copy(color = Slate600),
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }
                }
            }
            "VIDEO" -> {
                VideoPlayerCard(mediaUri = mediaUri, mediaName = mediaName)
            }
            "AUDIO" -> {
                AudioPlayerCard(mediaUri = mediaUri, mediaName = mediaName)
            }
            "DOCUMENT" -> {
                DocumentCard(mediaName = mediaName ?: "Parsed Document.pdf")
            }
        }
    }
}

@Composable
fun VideoPlayerCard(mediaUri: String?, mediaName: String?) {
    var isPlaying by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Slate900),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Indigo600)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause Video" else "Play Video",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isPlaying) "Playing Video Stream..." else "Tap to Play Video",
                    style = MaterialTheme.typography.labelMedium.copy(color = Color.White)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Videocam, contentDescription = null, tint = Indigo600, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = mediaName ?: "Video Clip.mp4",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Slate800)
                )
            }
            Text(
                text = "00:45 / 02:15",
                style = MaterialTheme.typography.labelSmall.copy(color = Slate600)
            )
        }
    }
}

@Composable
fun AudioPlayerCard(mediaUri: String?, mediaName: String?) {
    var isPlaying by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { isPlaying = !isPlaying },
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Indigo600)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause Audio" else "Play Audio",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mediaName ?: "Voice Note.m4a",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
            )
            
            Spacer(modifier = Modifier.height(4.dp))

            // Simulated Audio Waveform Bar Visualizer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val heights = listOf(8, 14, 22, 10, 18, 24, 12, 20, 16, 22, 10, 14, 20, 8, 16)
                heights.forEach { h ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(h.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isPlaying) Indigo600 else Slate300)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "0:14",
            style = MaterialTheme.typography.labelSmall.copy(color = Slate600, fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
fun DocumentCard(mediaName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE0E7FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Description, contentDescription = "Document", tint = Indigo600)
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mediaName,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
            )
            Text(
                text = "Parsed & Ingested to Context • 1.2 MB",
                style = MaterialTheme.typography.labelSmall.copy(color = Slate600, fontSize = 11.sp)
            )
        }

        Icon(Icons.Default.CheckCircle, contentDescription = "Parsed", tint = Emerald600, modifier = Modifier.size(20.dp))
    }
}
