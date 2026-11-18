package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.DietPlan
import com.example.data.model.FeeEngine
import com.example.data.model.Member
import com.example.data.model.Supplement
import com.example.data.model.Trainer
import com.example.data.model.WorkoutExercise
import com.example.data.model.WorkoutPlan
import com.example.ui.components.GlassCard
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CoolSilver
import com.example.ui.theme.CrimsonBright
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StatusDueSoonAmber
import com.example.ui.theme.StatusDueTodayRed
import com.example.ui.theme.StatusGold
import com.example.ui.theme.StatusInfoBlue
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.viewmodel.GymAppData
import com.example.util.CommunicationIntents
import com.example.util.QrAndBarcodeUtils
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun OperationsScreen(
    data: GymAppData,
    initialTab: String = "ATTENDANCE",
    onCheckIn: (Member, String) -> Unit,
    onCheckOut: (String) -> Unit,
    onSaveTrainer: (Trainer) -> Unit,
    onSaveWorkout: (WorkoutPlan) -> Unit,
    onSaveDiet: (DietPlan) -> Unit,
    onSaveSupplement: (Supplement) -> Unit,
    onSellSupplement: (Supplement, Long) -> Unit,
    onOpenBodybuilderLab: () -> Unit
) {
    var activeTab by remember(initialTab) { mutableStateOf(initialTab) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Club Operations", style = MaterialTheme.typography.headlineLarge, color = PureWhite)
                OutlinedButton(onClick = onOpenBodybuilderLab, shape = RoundedCornerShape(50)) {
                    Text("Bodybuilder Lab", color = StatusGold, style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ATTENDANCE" to "Attendance",
                    "TRAINERS" to "Trainers",
                    "WORKOUTS" to "Workouts",
                    "DIETS" to "Diet Plans",
                    "SUPPLEMENTS" to "Supplements"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = activeTab == key,
                        onClick = { activeTab = key },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CrimsonPrimary,
                            selectedLabelColor = PureWhite,
                            containerColor = CarbonElevated,
                            labelColor = CoolSilver
                        ),
                        modifier = Modifier.testTag("ops_tab_$key")
                    )
                }
            }
        }

        when (activeTab) {
            "ATTENDANCE" -> AttendanceTab(data, onCheckIn, onCheckOut)
            "TRAINERS" -> TrainersTab(data, onSaveTrainer)
            "WORKOUTS" -> WorkoutsTab(data, onSaveWorkout)
            "DIETS" -> DietsTab(data, onSaveDiet)
            "SUPPLEMENTS" -> SupplementsTab(data, onSaveSupplement, onSellSupplement)
        }
    }
}

@Composable
private fun AttendanceTab(
    data: GymAppData,
    onCheckIn: (Member, String) -> Unit,
    onCheckOut: (String) -> Unit
) {
    var scanInput by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("QR") }
    val timeFmt = remember { DateTimeFormatter.ofPattern("hh:mm a") }
    val todayKey = remember { FeeEngine.todayDateKey() }

    val matchedMember = remember(scanInput, data.members) {
        if (scanInput.isBlank()) null
        else data.members.firstOrNull {
            it.membershipId.equals(scanInput.trim(), ignoreCase = true) ||
                it.fullName.contains(scanInput.trim(), ignoreCase = true)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), highlightAccent = StatusInfoBlue) {
                Text("Check-In Terminal (QR / Barcode / Manual)", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("QR", "BARCODE", "MANUAL", "BIOMETRIC").forEach { m ->
                        FilterChip(
                            selected = method == m,
                            onClick = { method = m },
                            label = { Text(m, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CrimsonPrimary,
                                selectedLabelColor = PureWhite
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = scanInput,
                    onValueChange = { scanInput = it },
                    label = { Text("Enter Membership ID (e.g. GYM-1001) or Name") },
                    leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = CrimsonBright) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("attendance_scan_input")
                )

                if (matchedMember != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val pattern = remember(matchedMember.membershipId) {
                        QrAndBarcodeUtils.generateCode39Pattern(matchedMember.membershipId)
                    }
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Canvas(modifier = Modifier.fillMaxWidth().height(34.dp)) {
                                val barW = size.width / pattern.size.coerceAtLeast(1)
                                pattern.forEachIndexed { idx, black ->
                                    if (black) {
                                        drawRect(Color.Black, topLeft = Offset(idx * barW, 0f), size = Size(barW + 0.5f, size.height))
                                    }
                                }
                            }
                            Text(matchedMember.membershipId, color = Color.Black, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onCheckIn(matchedMember, method)
                            scanInput = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusPaidGreen, contentColor = ObsidianBlack),
                        modifier = Modifier.fillMaxWidth().testTag("confirm_checkin_btn")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Check In ${matchedMember.fullName} ($method)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Quick One-Tap Member Check-In Pills
        if (data.members.isNotEmpty()) {
            item {
                Text("Quick Manual Check-In", style = MaterialTheme.typography.titleSmall, color = CoolSilver)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    data.members.forEach { m ->
                        OutlinedButton(onClick = { onCheckIn(m, method) }) {
                            Text("${m.fullName} (${m.membershipId})", color = PureWhite, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // Attendance History & Monthly Count
        val todayList = data.attendance.filter { it.dateKey == todayKey }
        item {
            Text("Today's Attendance (${todayList.size}) • Total Logs (${data.attendance.size})", style = MaterialTheme.typography.titleMedium, color = PureWhite)
        }
        items(data.attendance.take(40), key = { it.id }) { att ->
            val inStr = Instant.ofEpochMilli(att.checkInMillis).atZone(ZoneId.systemDefault()).format(timeFmt)
            val outStr = att.checkOutMillis?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(timeFmt)
            }
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${att.memberName} (${att.membershipId})", style = MaterialTheme.typography.titleSmall, color = PureWhite)
                        Text("${att.dateKey} • In: $inStr • Method: ${att.method}", style = MaterialTheme.typography.bodySmall, color = CoolSilver)
                        if (outStr != null) {
                            Text("Out: $outStr", style = MaterialTheme.typography.labelSmall, color = StatusPaidGreen)
                        }
                    }
                    if (att.checkOutMillis == null) {
                        OutlinedButton(onClick = { onCheckOut(att.id) }) {
                            Icon(Icons.Default.Logout, contentDescription = null, tint = CrimsonBright, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Check Out", color = PureWhite, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainersTab(data: GymAppData, onSaveTrainer: (Trainer) -> Unit) {
    var showAdd by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(
                onClick = { showAdd = true },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                modifier = Modifier.fillMaxWidth().testTag("add_trainer_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add New Trainer")
            }
        }
        items(data.trainers, key = { it.id }) { tr ->
            val assignedCount = data.members.count { it.trainerId == tr.id || it.trainerName == tr.name }
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tr.name, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                        Text("${tr.specialization} • ${tr.mobile}", style = MaterialTheme.typography.bodySmall, color = CoolSilver)
                        Text(
                            text = "Salary: Rs. ${tr.salary} • Assigned: $assignedCount members • Days: ${tr.attendanceDaysThisMonth}",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusGold
                        )
                    }
                    OutlinedButton(onClick = {
                        onSaveTrainer(tr.copy(attendanceDaysThisMonth = (tr.attendanceDaysThisMonth + 1).coerceAtMost(31L)))
                    }) {
                        Text("+1 Day", color = StatusPaidGreen, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var mobile by remember { mutableStateOf("") }
        var salary by remember { mutableStateOf("40000") }
        var spec by remember { mutableStateOf("Hypertrophy & Strength") }

        AlertDialog(
            onDismissRequest = { showAdd = false },
            containerColor = CarbonElevated,
            titleContentColor = PureWhite,
            title = { Text("Add Trainer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Trainer Name") }, singleLine = true)
                    OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile") }, singleLine = true)
                    OutlinedTextField(value = salary, onValueChange = { salary = it.filter { c -> c.isDigit() } }, label = { Text("Monthly Salary (Rs.)") }, singleLine = true)
                    OutlinedTextField(value = spec, onValueChange = { spec = it }, label = { Text("Specialization") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && mobile.length >= 3) {
                            val br = if (data.ownerProfile.activeBranchId == "ALL") "main" else data.ownerProfile.activeBranchId
                            onSaveTrainer(
                                Trainer(
                                    name = name.trim(),
                                    mobile = mobile.trim(),
                                    salary = salary.toLongOrNull() ?: 0L,
                                    specialization = spec.trim(),
                                    branchId = br
                                )
                            )
                            showAdd = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel", color = CoolSilver) } }
        )
    }
}

@Composable
private fun WorkoutsTab(data: GymAppData, onSaveWorkout: (WorkoutPlan) -> Unit) {
    val context = LocalContext.current
    var selectedDay by remember { mutableStateOf("CHEST") }
    var showAdd by remember { mutableStateOf(false) }

    val filtered = remember(data.workoutPlans, selectedDay) {
        data.workoutPlans.filter { it.dayCategory == selectedDay }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("CHEST" to "Chest Day", "BACK" to "Back Day", "LEG" to "Leg Day", "SHOULDER" to "Shoulder", "ARM" to "Arm Day", "ABS" to "Abs Day").forEach { (key, label) ->
                    FilterChip(
                        selected = selectedDay == key,
                        onClick = { selectedDay = key },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CrimsonPrimary, selectedLabelColor = PureWhite)
                    )
                }
            }
        }
        item {
            Button(
                onClick = { showAdd = true },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                modifier = Modifier.fillMaxWidth().testTag("add_workout_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create $selectedDay Workout Plan")
            }
        }
        items(filtered, key = { it.id }) { plan ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(plan.title, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                if (!plan.assignedMemberName.isNullOrBlank()) {
                    Text("Assigned: ${plan.assignedMemberName}", style = MaterialTheme.typography.labelSmall, color = StatusGold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                plan.parsedExercises().forEach { ex ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${ex.name} • ${ex.sets}x${ex.reps} (${ex.weightKg}kg)", style = MaterialTheme.typography.bodyMedium, color = PureWhite)
                            if (ex.instructions.isNotBlank()) {
                                Text(ex.instructions, style = MaterialTheme.typography.bodySmall, color = CoolSilver)
                            }
                        }
                        if (ex.videoUrl.isNotBlank()) {
                            OutlinedButton(onClick = { CommunicationIntents.openVideoLink(context, ex.videoUrl) }) {
                                Icon(Icons.Default.PlayCircle, contentDescription = null, tint = CrimsonBright, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Video", color = PureWhite, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var title by remember { mutableStateOf("$selectedDay Hypertrophy Split") }
        var exName by remember { mutableStateOf("") }
        var sets by remember { mutableStateOf("4") }
        var reps by remember { mutableStateOf("10-12") }
        var weight by remember { mutableStateOf("60") }
        var videoUrl by remember { mutableStateOf("") }
        var instructions by remember { mutableStateOf("Controlled 3s eccentric") }

        AlertDialog(
            onDismissRequest = { showAdd = false },
            containerColor = CarbonElevated,
            titleContentColor = PureWhite,
            title = { Text("New $selectedDay Workout Plan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Plan Title") }, singleLine = true)
                    OutlinedTextField(value = exName, onValueChange = { exName = it }, label = { Text("Primary Exercise Name") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = sets, onValueChange = { sets = it.filter { c -> c.isDigit() } }, label = { Text("Sets") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = reps, onValueChange = { reps = it }, label = { Text("Reps") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = weight, onValueChange = { weight = it.filter { c -> c.isDigit() } }, label = { Text("kg") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = videoUrl, onValueChange = { videoUrl = it }, label = { Text("Video Link (Optional)") }, singleLine = true)
                    OutlinedTextField(value = instructions, onValueChange = { instructions = it }, label = { Text("Instructions") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank() && exName.isNotBlank()) {
                            val ex = WorkoutExercise(
                                name = exName.trim(),
                                sets = sets.toLongOrNull() ?: 4L,
                                reps = reps.trim(),
                                weightKg = weight.toLongOrNull() ?: 0L,
                                videoUrl = videoUrl.trim(),
                                instructions = instructions.trim()
                            )
                            onSaveWorkout(
                                WorkoutPlan(
                                    dayCategory = selectedDay,
                                    title = title.trim(),
                                    exercises = listOf(ex.toMap())
                                )
                            )
                            showAdd = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) { Text("Save Plan") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel", color = CoolSilver) } }
        )
    }
}

@Composable
private fun DietsTab(data: GymAppData, onSaveDiet: (DietPlan) -> Unit) {
    var showAdd by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(
                onClick = { showAdd = true },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                modifier = Modifier.fillMaxWidth().testTag("add_diet_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create Diet Plan")
            }
        }
        items(data.dietPlans, key = { it.id }) { diet ->
            GlassCard(modifier = Modifier.fillMaxWidth(), highlightAccent = StatusPaidGreen) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(diet.title, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                    Text("${diet.totalCalories} kcal • ${diet.totalProteinGrams}g P", style = MaterialTheme.typography.labelMedium, color = StatusPaidGreen)
                }
                Text("Goal: ${diet.goal}", style = MaterialTheme.typography.labelSmall, color = StatusGold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Breakfast: ${diet.breakfast}", style = MaterialTheme.typography.bodySmall, color = CoolSilver)
                Text("Lunch: ${diet.lunch}", style = MaterialTheme.typography.bodySmall, color = CoolSilver)
                Text("Snacks: ${diet.snacks}", style = MaterialTheme.typography.bodySmall, color = CoolSilver)
                Text("Dinner: ${diet.dinner}", style = MaterialTheme.typography.bodySmall, color = CoolSilver)
            }
        }
    }

    if (showAdd) {
        var title by remember { mutableStateOf("") }
        var goal by remember { mutableStateOf("Lean Muscle Gain") }
        var breakfast by remember { mutableStateOf("6 Egg Whites, 80g Oats, Banana") }
        var lunch by remember { mutableStateOf("250g Chicken Breast, 200g Rice") }
        var snacks by remember { mutableStateOf("1 Scoop Whey + Almonds") }
        var dinner by remember { mutableStateOf("200g Fish/Steak + Salad") }
        var calories by remember { mutableStateOf("2800") }
        var protein by remember { mutableStateOf("210") }

        AlertDialog(
            onDismissRequest = { showAdd = false },
            containerColor = CarbonElevated,
            titleContentColor = PureWhite,
            title = { Text("Create Diet Plan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Plan Title") }, singleLine = true)
                    OutlinedTextField(value = goal, onValueChange = { goal = it }, label = { Text("Goal") }, singleLine = true)
                    OutlinedTextField(value = breakfast, onValueChange = { breakfast = it }, label = { Text("Breakfast") })
                    OutlinedTextField(value = lunch, onValueChange = { lunch = it }, label = { Text("Lunch") })
                    OutlinedTextField(value = snacks, onValueChange = { snacks = it }, label = { Text("Snacks") })
                    OutlinedTextField(value = dinner, onValueChange = { dinner = it }, label = { Text("Dinner") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = calories, onValueChange = { calories = it.filter { c -> c.isDigit() } }, label = { Text("Calories") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = protein, onValueChange = { protein = it.filter { c -> c.isDigit() } }, label = { Text("Protein (g)") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSaveDiet(
                                DietPlan(
                                    title = title.trim(),
                                    goal = goal.trim(),
                                    breakfast = breakfast.trim(),
                                    lunch = lunch.trim(),
                                    snacks = snacks.trim(),
                                    dinner = dinner.trim(),
                                    totalCalories = calories.toLongOrNull() ?: 2500L,
                                    totalProteinGrams = protein.toLongOrNull() ?: 180L
                                )
                            )
                            showAdd = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) { Text("Save Diet") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel", color = CoolSilver) } }
        )
    }
}

@Composable
private fun SupplementsTab(
    data: GymAppData,
    onSaveSupplement: (Supplement) -> Unit,
    onSellSupplement: (Supplement, Long) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(
                onClick = { showAdd = true },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                modifier = Modifier.fillMaxWidth().testTag("add_supplement_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Supplement Inventory")
            }
        }
        items(data.supplements, key = { it.id }) { sup ->
            GlassCard(modifier = Modifier.fillMaxWidth(), highlightAccent = if (sup.stockQty <= 3) StatusDueTodayRed else StatusPaidGreen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(sup.name, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                        Text("${sup.brand} • ${sup.category} • Rs. ${sup.unitPrice}", style = MaterialTheme.typography.bodySmall, color = CoolSilver)
                        Text(
                            text = "Stock: ${sup.stockQty} • Sold: ${sup.soldQty} • Exp: ${FeeEngine.formatDate(sup.expiryDateMillis)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (sup.stockQty <= 3) StatusDueSoonAmber else StatusPaidGreen
                        )
                    }
                    Button(
                        onClick = { onSellSupplement(sup, 1L) },
                        enabled = sup.stockQty > 0L,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sell 1", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("WHEY_PROTEIN") }
        var brand by remember { mutableStateOf("") }
        var stock by remember { mutableStateOf("10") }
        var price by remember { mutableStateOf("18500") }

        AlertDialog(
            onDismissRequest = { showAdd = false },
            containerColor = CarbonElevated,
            titleContentColor = PureWhite,
            title = { Text("Add Supplement Product") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name") }, singleLine = true)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("WHEY_PROTEIN", "MASS_GAINER", "CREATINE", "PRE_WORKOUT").forEach { c ->
                            FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                    OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = stock, onValueChange = { stock = it.filter { c -> c.isDigit() } }, label = { Text("Stock Qty") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = price, onValueChange = { price = it.filter { c -> c.isDigit() } }, label = { Text("Price (Rs.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val br = if (data.ownerProfile.activeBranchId == "ALL") "main" else data.ownerProfile.activeBranchId
                            onSaveSupplement(
                                Supplement(
                                    name = name.trim(),
                                    category = category,
                                    brand = brand.trim(),
                                    stockQty = stock.toLongOrNull() ?: 0L,
                                    unitPrice = price.toLongOrNull() ?: 0L,
                                    expiryDateMillis = System.currentTimeMillis() + 86400000L * 365L,
                                    branchId = br
                                )
                            )
                            showAdd = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel", color = CoolSilver) } }
        )
    }
}
