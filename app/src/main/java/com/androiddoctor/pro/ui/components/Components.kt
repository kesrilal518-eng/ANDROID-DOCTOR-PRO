package com.androiddoctor.pro.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androiddoctor.pro.data.DiagnosticResult
import com.androiddoctor.pro.data.TestStatus
import com.androiddoctor.pro.ui.theme.CardDark
import com.androiddoctor.pro.ui.theme.FailedRed
import com.androiddoctor.pro.ui.theme.NeonBlue
import com.androiddoctor.pro.ui.theme.NeonGreen
import com.androiddoctor.pro.ui.theme.SurfaceDark
import com.androiddoctor.pro.ui.theme.TextSecondary
import com.androiddoctor.pro.ui.theme.WarningOrange

@Composable
fun StatusBadge(status: TestStatus) {
    val (bg, text) = when (status) {
        TestStatus.PASS -> NeonGreen to Color.Black
        TestStatus.WARNING -> WarningOrange to Color.Black
        TestStatus.FAILED -> FailedRed to Color.White
        TestStatus.UNAVAILABLE -> Color.Gray to Color.White
        TestStatus.INCONCLUSIVE -> NeonBlue.copy(alpha = 0.7f) to Color.Black
        TestStatus.PENDING, TestStatus.RUNNING -> NeonBlue to Color.Black
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = status.name, color = text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HealthScoreRing(score: Int, modifier: Modifier = Modifier) {
    val color = when {
        score >= 80 -> NeonGreen
        score >= 60 -> WarningOrange
        else -> FailedRed
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(120.dp).border(6.dp, color, CircleShape).padding(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "$score", style = MaterialTheme.typography.displayLarge, color = color, fontWeight = FontWeight.Bold)
            Text(text = "HEALTH", color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
fun DeviceInfoCard(
    manufacturer: String, model: String, androidVersion: String,
    ram: String, storage: String, battery: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "DEVICE", color = NeonBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Text(text = "$manufacturer $model", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text("Android $androidVersion", color = TextSecondary)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoChip("RAM", ram)
                InfoChip("Storage", storage)
                InfoChip("Battery", battery)
            }
        }
    }
}

@Composable
fun InfoChip(label: String, value: String) {
    Column {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Text(value, color = NeonGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
fun DiagnosticResultCard(result: DiagnosticResult) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = result.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                StatusBadge(result.status)
            }
            Spacer(Modifier.height(6.dp))
            Text(result.message, color = TextSecondary, fontSize = 13.sp)
            if (result.recommendation.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(text = "\u2192 ${result.recommendation}", color = NeonBlue.copy(alpha = 0.9f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ScanProgressBar(progress: Float, currentTest: String, isRunning: Boolean) {
    val infinite = rememberInfiniteTransition(label = "scan")
    val pulse by infinite.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(800, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "pulse"
    )
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = if (isRunning) "Scanning: $currentTest" else "Scan complete",
                color = if (isRunning) NeonGreen.copy(alpha = pulse) else NeonGreen,
                fontWeight = FontWeight.Medium
            )
            Text(text = "${(progress * 100).toInt()}%", color = NeonBlue)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = NeonGreen,
            trackColor = CardDark
        )
    }
}

@Composable
fun NeonButton(
    text: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = NeonGreen,
            contentColor = Color.Black,
            disabledContainerColor = NeonGreen.copy(alpha = 0.3f)
        )
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
