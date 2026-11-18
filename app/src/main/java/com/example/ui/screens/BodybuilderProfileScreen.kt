package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BodyProgressLog
import com.example.data.model.FeeEngine
import com.example.data.model.Member
import com.example.ui.components.GlassCard
import com.example.ui.components.MemberAvatar
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CoolSilver
import com.example.ui.theme.CrimsonBright
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StatusDueSoonAmber
import com.example.ui.theme.StatusGold
import com.example.ui.theme.StatusInfoBlue
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.viewmodel.GymAppData
import com.example.util.ImageWebpUtils
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodybuilderProfileScreen(
    data: GymAppData,
    initialMemberId: String?,
    onBack: () -> Unit,
    onAddProgressLog: (BodyProgressLog) -> Unit
) {
    BackHandler { onBack() }
    var selectedMemberId by remember(initialMemberId, data.members) {
        mutableStateOf(initialMemberId ?: data.members.firstOrNull()?.id)
    }
    var showLogDialog by remember { mutableStateOf(false) }

    val member = remember(data.members, selectedMemberId) {
        data.members.firstOrNull { it.id == selectedMemberId } ?: data.members.firstOrNull()
    }

    val logs = remember(data.bodyLogs, member?.id) {
        if (member == null) emptyList()
        else data.bodyLogs.filter { it.memberId == member.id }.sortedBy { it.recordedAtMillis }
    }

    Scaffold(
        containerColor = ObsidianBlack,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBlack, titleContentColor = PureWhite),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
                    }
                },
                title = { Text("Bodybuilder Lab", style = MaterialTheme.typography.headlineMedium) }
            )
        },
        floatingActionButton = {
            if (member != null) {
                FloatingActionButton(
                    onClick = { showLogDialog = true },
                    containerColor = CrimsonPrimary,
                    contentColor = PureWhite,
                    modifier = Modifier.testTag("fab_log_body_progress")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Log Progress")
                }
            }
        }
    ) { padding ->
        if (member == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Register a member first to track bodybuilder stats & graphs.", color = CoolSilver)
            }
        } else {
            val latestWeight = logs.lastOrNull()?.weightKg ?: member.weightKg ?: 0.0
            val heightCm = member.heightCm ?: 175.0
            val bmi = if (heightCm > 0.0 && latestWeight > 0.0) {
                val hMeters = heightCm / 100.0
                ((latestWeight / (hMeters * hMeters)) * 10.0).roundToInt() / 10.0
            } else 0.0
            val bodyFat = logs.lastOrNull()?.bodyFatPercent ?: member.bodyFatPercent ?: 0.0

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Member Switcher
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        data.members.forEach { m ->
                            FilterChip(
                                selected = m.id == member.id,
                                onClick = { selectedMemberId = m.id },
                                label = { Text(m.fullName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CrimsonPrimary,
                                    selectedLabelColor = PureWhite,
                                    containerColor = CarbonElevated,
                                    labelColor = CoolSilver
                                )
                            )
                        }
                    }
                }

                // Physique Summary Card
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), highlightAccent = StatusGold) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MemberAvatar(member.photoWebpBase64, member.fullName, size = 62.dp, borderColor = StatusGold)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(member.fullName, style = MaterialTheme.typography.titleLarge, color = PureWhite)
                                Text("Goal: ${member.fitnessGoal ?: "Hypertrophy"}", style = MaterialTheme.typography.bodySmall, color = CoolSilver)
                                if (member.competitionPrep == true) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(color = CrimsonPrimary.copy(alpha = 0.25f), shape = RoundedCornerShape(50)) {
                                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = StatusGold, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Competition Prep Active", style = MaterialTheme.typography.labelSmall, color = StatusGold)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MetricPill("Height", "${heightCm.toInt()} cm", StatusInfoBlue)
                            MetricPill("Weight", "$latestWeight kg", PureWhite)
                            MetricPill("BMI", "$bmi", StatusPaidGreen)
                            MetricPill("Body Fat", "$bodyFat%", StatusDueSoonAmber)
                        }
                    }
                }

                // 3 Progress Graphs
                item {
                    ProgressLineChartCard(
                        title = "Weight Progress (kg)",
                        values = logs.map { it.weightKg.toFloat() },
                        labels = logs.map { FeeEngine.formatDate(it.recordedAtMillis).take(6) },
                        lineColor = StatusInfoBlue
                    )
                }
                item {
                    ProgressLineChartCard(
                        title = "Muscle Gain Progress (kg)",
                        values = logs.map { it.muscleMassKg.toFloat() },
                        labels = logs.map { FeeEngine.formatDate(it.recordedAtMillis).take(6) },
                        lineColor = StatusPaidGreen
                    )
                }
                item {
                    ProgressLineChartCard(
                        title = "Fat Loss Progress (%)",
                        values = logs.map { it.bodyFatPercent.toFloat() },
                        labels = logs.map { FeeEngine.formatDate(it.recordedAtMillis).take(6) },
                        lineColor = CrimsonBright
                    )
                }

                // Before & After + Progress Photos
                val photoLogs = logs.filter { !it.photoWebpBase64.isNullOrBlank() }
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Before & After / Progress Photos", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (photoLogs.isEmpty()) {
                            Text("Tap + to add Before, After, or Monthly Progress photos.", style = MaterialTheme.typography.bodySmall, color = CoolSilver)
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(photoLogs, key = { it.id }) { pl ->
                                    val bmp = remember(pl.photoWebpBase64) { ImageWebpUtils.decodeBase64ToImageBitmap(pl.photoWebpBase64) }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(110.dp, 136.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(CarbonCard)
                                                .border(1.dp, CrimsonPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        ) {
                                            if (bmp != null) {
                                                Image(bitmap = bmp, contentDescription = pl.photoTag, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("${pl.photoTag} • ${pl.weightKg}kg", style = MaterialTheme.typography.labelSmall, color = StatusGold)
                                        Text(FeeEngine.formatDate(pl.recordedAtMillis), style = MaterialTheme.typography.labelSmall, color = CoolSilver)
                                    }
                                }
                            }
                        }
                    }
                }

                // Monthly Measurements Log
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Monthly Measurements History", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (logs.isEmpty()) {
                            Text("No measurement check-ins recorded yet.", style = MaterialTheme.typography.bodySmall, color = CoolSilver)
                        } else {
                            logs.reversed().forEach { log ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(FeeEngine.formatDate(log.recordedAtMillis), style = MaterialTheme.typography.labelMedium, color = PureWhite)
                                        Text("Wt ${log.weightKg}kg • Muscle ${log.muscleMassKg}kg • Fat ${log.bodyFatPercent}%", style = MaterialTheme.typography.bodySmall, color = CoolSilver)
                                    }
                                    Text(
                                        text = "C:${log.chestCm ?: "-"} W:${log.waistCm ?: "-"} A:${log.armsCm ?: "-"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusGold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLogDialog && member != null) {
        LogBodyProgressDialog(
            member = member,
            onDismiss = { showLogDialog = false },
            onSave = { log ->
                onAddProgressLog(log)
                showLogDialog = false
            }
        )
    }
}

@Composable
private fun MetricPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = CoolSilver)
    }
}

@Composable
private fun ProgressLineChartCard(
    title: String,
    values: List<Float>,
    labels: List<String>,
    lineColor: Color
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), highlightAccent = lineColor) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = PureWhite)
            if (values.isNotEmpty()) {
                Text("Latest: ${values.last()}", style = MaterialTheme.typography.labelMedium, color = lineColor)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (values.size < 2) {
            Text(
                text = if (values.isEmpty()) "Log at least 2 check-ins to plot $title." else "1 check-in recorded (${values.first()}). Add one more to see trend curve.",
                style = MaterialTheme.typography.bodySmall,
                color = CoolSilver
            )
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val minVal = values.minOrNull() ?: 0f
                val maxVal = values.maxOrNull() ?: 1f
                val range = (maxVal - minVal).coerceAtLeast(1f)
                val padX = 16f
                val padY = 16f
                val usableW = size.width - padX * 2
                val usableH = size.height - padY * 2

                val points = values.mapIndexed { idx, v ->
                    val x = padX + (idx.toFloat() / (values.size - 1)) * usableW
                    val y = padY + usableH - ((v - minVal) / range) * usableH
                    Offset(x, y)
                }

                // Baseline grid
                drawLine(
                    color = GlassBorder,
                    start = Offset(padX, size.height - padY),
                    end = Offset(size.width - padX, size.height - padY),
                    strokeWidth = 2f
                )

                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
                drawPath(path, color = lineColor, style = Stroke(width = 5f, cap = StrokeCap.Round))
                points.forEach { pt ->
                    drawCircle(color = ObsidianBlack, radius = 9f, center = pt)
                    drawCircle(color = lineColor, radius = 6f, center = pt)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(labels.firstOrNull() ?: "", style = MaterialTheme.typography.labelSmall, color = CoolSilver)
                Text(labels.lastOrNull() ?: "", style = MaterialTheme.typography.labelSmall, color = CoolSilver)
            }
        }
    }
}

@Composable
private fun LogBodyProgressDialog(
    member: Member,
    onDismiss: () -> Unit,
    onSave: (BodyProgressLog) -> Unit
) {
    val context = LocalContext.current
    var weightText by remember { mutableStateOf((member.weightKg ?: 75.0).toString()) }
    var fatText by remember { mutableStateOf((member.bodyFatPercent ?: 14.0).toString()) }
    var muscleText by remember { mutableStateOf("38.0") }
    var chestText by remember { mutableStateOf("104") }
    var waistText by remember { mutableStateOf("82") }
    var armsText by remember { mutableStateOf("39") }
    var thighsText by remember { mutableStateOf("58") }
    var photoTag by remember { mutableStateOf("PROGRESS") }
    var photoBase64 by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            photoBase64 = ImageWebpUtils.uriToWebpBase64(context, uri, maxDimension = 320, quality = 60)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CarbonElevated,
        titleContentColor = PureWhite,
        title = { Text("Log Body Progress • ${member.fullName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text("Weight kg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fatText,
                        onValueChange = { fatText = it },
                        label = { Text("Fat %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = muscleText,
                    onValueChange = { muscleText = it },
                    label = { Text("Muscle Mass (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = chestText, onValueChange = { chestText = it }, label = { Text("Chest") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = waistText, onValueChange = { waistText = it }, label = { Text("Waist") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = armsText, onValueChange = { armsText = it }, label = { Text("Arms") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("BEFORE", "PROGRESS", "AFTER").forEach { tag ->
                        FilterChip(
                            selected = photoTag == tag,
                            onClick = { photoTag = tag },
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (photoBase64 != null) "Photo Attached ✓" else "Attach Progress Photo", color = PureWhite)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        BodyProgressLog(
                            memberId = member.id,
                            memberName = member.fullName,
                            recordedAtMillis = System.currentTimeMillis(),
                            weightKg = weightText.toDoubleOrNull() ?: 75.0,
                            bodyFatPercent = fatText.toDoubleOrNull() ?: 14.0,
                            muscleMassKg = muscleText.toDoubleOrNull() ?: 38.0,
                            chestCm = chestText.toDoubleOrNull(),
                            waistCm = waistText.toDoubleOrNull(),
                            armsCm = armsText.toDoubleOrNull(),
                            thighsCm = thighsText.toDoubleOrNull(),
                            photoTag = photoTag,
                            photoWebpBase64 = photoBase64
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
            ) {
                Text("Save Log")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = CoolSilver) }
        }
    )
}
