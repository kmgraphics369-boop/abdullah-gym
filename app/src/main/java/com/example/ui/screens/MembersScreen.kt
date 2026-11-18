package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.FeeEngine
import com.example.data.model.FeeUrgency
import com.example.data.model.Member
import com.example.ui.components.GlassCard
import com.example.ui.components.MemberAvatar
import com.example.ui.components.UrgencyBadge
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CoolSilver
import com.example.ui.theme.CrimsonBright
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StatusDueTodayRed
import com.example.ui.theme.StatusGold
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.viewmodel.GymAppData
import com.example.util.ImageWebpUtils
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun MembersScreen(
    data: GymAppData,
    onOpenMemberAlert: (Member) -> Unit,
    onOpenRegistration: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val statuses = remember(data.dashboard.memberFeeStatuses, query, selectedFilter) {
        data.dashboard.memberFeeStatuses.filter { st ->
            val matchesQuery = query.isBlank() ||
                st.member.fullName.contains(query, ignoreCase = true) ||
                st.member.membershipId.contains(query, ignoreCase = true) ||
                st.member.mobile.contains(query, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "DUE_TODAY" -> st.urgency == FeeUrgency.DUE_TODAY
                "OVERDUE" -> st.urgency == FeeUrgency.OVERDUE
                "DUE_SOON" -> st.urgency == FeeUrgency.DUE_SOON
                "PAID" -> st.urgency == FeeUrgency.PAID
                "EXPIRED" -> st.member.status == "EXPIRED"
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        containerColor = ObsidianBlack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenRegistration,
                containerColor = CrimsonPrimary,
                contentColor = PureWhite,
                modifier = Modifier.testTag("fab_register_member")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Register Member")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("Members Directory (${data.members.size})", style = MaterialTheme.typography.headlineLarge, color = PureWhite)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search name, ID, or phone...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CoolSilver) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("members_search_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "ALL" to "All",
                        "DUE_TODAY" to "Due Today",
                        "OVERDUE" to "Overdue",
                        "DUE_SOON" to "Due Soon",
                        "PAID" to "Paid",
                        "EXPIRED" to "Expired"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { selectedFilter = key },
                            label = { Text(label) },
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

            if (statuses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Text("No matching members", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Tap + to register a member with auto-calculated monthly due dates.", color = CoolSilver, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onOpenRegistration,
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Register Member")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(statuses, key = { it.member.id }) { st ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("member_row_${st.member.membershipId}"),
                            onClick = { onOpenMemberAlert(st.member) }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MemberAvatar(
                                    photoWebpBase64 = st.member.photoWebpBase64,
                                    fullName = st.member.fullName,
                                    size = 54.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(st.member.fullName, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                                    Text("${st.member.membershipId} • ${st.member.packageName}", style = MaterialTheme.typography.bodySmall, color = CoolSilver)
                                    Text("Due every ${st.member.dueDayOfMonth}th • Rs. ${st.member.monthlyFee}/mo", style = MaterialTheme.typography.labelSmall, color = StatusGold)
                                }
                                UrgencyBadge(urgency = st.urgency)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberRegistrationScreen(
    data: GymAppData,
    existingMember: Member?,
    onBack: () -> Unit,
    onSave: (Member) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    val generatedId = remember(existingMember, data.members.size) {
        existingMember?.membershipId ?: "GYM-${1001 + data.members.size}"
    }

    var fullName by remember { mutableStateOf(existingMember?.fullName ?: "") }
    var fatherName by remember { mutableStateOf(existingMember?.fatherName ?: "") }
    var cnic by remember { mutableStateOf(existingMember?.cnic ?: "") }
    var mobile by remember { mutableStateOf(existingMember?.mobile ?: "") }
    var whatsapp by remember { mutableStateOf(existingMember?.whatsapp ?: "") }
    var emergencyContact by remember { mutableStateOf(existingMember?.emergencyContact ?: "") }
    var address by remember { mutableStateOf(existingMember?.address ?: "") }
    var gender by remember { mutableStateOf(existingMember?.gender ?: "Male") }
    var dob by remember { mutableStateOf(existingMember?.dateOfBirth ?: "") }
    var bloodGroup by remember { mutableStateOf(existingMember?.bloodGroup ?: "O+") }

    var packageName by remember { mutableStateOf(existingMember?.packageName ?: "Monthly Standard") }
    var monthlyFeeText by remember { mutableStateOf((existingMember?.monthlyFee ?: 3000L).toString()) }
    var joiningDate by remember {
        mutableStateOf(
            existingMember?.let { FeeEngine.millisToLocalDate(it.joiningDateMillis) } ?: LocalDate.now()
        )
    }
    var startDate by remember {
        mutableStateOf(
            existingMember?.let { FeeEngine.millisToLocalDate(it.startDateMillis) } ?: LocalDate.now()
        )
    }
    var expiryDate by remember {
        mutableStateOf(
            existingMember?.let { FeeEngine.millisToLocalDate(it.expiryDateMillis) } ?: LocalDate.now().plusMonths(1)
        )
    }
    var initialPaidToday by remember { mutableStateOf(existingMember == null) }
    var selectedTrainerId by remember { mutableStateOf(existingMember?.trainerId ?: "") }
    var selectedTrainerName by remember { mutableStateOf(existingMember?.trainerName ?: "") }
    var trainerMenuOpen by remember { mutableStateOf(false) }

    var heightCmText by remember { mutableStateOf(existingMember?.heightCm?.toString() ?: "175") }
    var weightKgText by remember { mutableStateOf(existingMember?.weightKg?.toString() ?: "75") }
    var bodyFatText by remember { mutableStateOf(existingMember?.bodyFatPercent?.toString() ?: "15") }
    var fitnessGoal by remember { mutableStateOf(existingMember?.fitnessGoal ?: "Muscle Gain") }
    var competitionPrep by remember { mutableStateOf(existingMember?.competitionPrep ?: false) }

    var photoBase64 by remember { mutableStateOf(existingMember?.photoWebpBase64) }
    var cnicFrontBase64 by remember { mutableStateOf(existingMember?.cnicFrontBase64) }
    var cnicBackBase64 by remember { mutableStateOf(existingMember?.cnicBackBase64) }
    var medicalCertBase64 by remember { mutableStateOf(existingMember?.medicalCertBase64) }
    var activeUploadTarget by remember { mutableStateOf("PHOTO") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val webp = ImageWebpUtils.uriToWebpBase64(context, uri, maxDimension = 256, quality = 65)
            when (activeUploadTarget) {
                "PHOTO" -> photoBase64 = webp
                "CNIC_FRONT" -> cnicFrontBase64 = webp
                "CNIC_BACK" -> cnicBackBase64 = webp
                "MEDICAL" -> medicalCertBase64 = webp
            }
        }
    }

    fun pickDate(initial: LocalDate, onPicked: (LocalDate) -> Unit) {
        DatePickerDialog(
            context,
            { _, year, month, day -> onPicked(LocalDate.of(year, month + 1, day)) },
            initial.year,
            initial.monthValue - 1,
            initial.dayOfMonth
        ).show()
    }

    Scaffold(
        containerColor = ObsidianBlack,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBlack, titleContentColor = PureWhite),
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("reg_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
                    }
                },
                title = {
                    Text(
                        if (existingMember == null) "Member Registration" else "Edit Member",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Uploads Section (WEBP Profile Photo + CNIC + Medical)
            GlassCard(modifier = Modifier.fillMaxWidth(), highlightAccent = CrimsonPrimary) {
                Text("Photo & Documents (WEBP)", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MemberAvatar(photoWebpBase64 = photoBase64, fullName = fullName.ifBlank { "Member" }, size = 68.dp)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                activeUploadTarget = "PHOTO"
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            modifier = Modifier.fillMaxWidth().testTag("upload_profile_photo_btn")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (photoBase64 != null) "Change Profile WEBP" else "Upload Profile Photo")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UploadDocButton("CNIC Front", cnicFrontBase64 != null, Modifier.weight(1f)) {
                        activeUploadTarget = "CNIC_FRONT"
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    UploadDocButton("CNIC Back", cnicBackBase64 != null, Modifier.weight(1f)) {
                        activeUploadTarget = "CNIC_BACK"
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    UploadDocButton("Medical", medicalCertBase64 != null, Modifier.weight(1f)) {
                        activeUploadTarget = "MEDICAL"
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                }
            }

            // Personal Details
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Personal Details", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("reg_full_name")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = fatherName,
                    onValueChange = { fatherName = it },
                    label = { Text("Father Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = cnic,
                    onValueChange = { cnic = it },
                    label = { Text("CNIC Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = {
                            mobile = it
                            if (whatsapp.isBlank()) whatsapp = it
                        },
                        label = { Text("Mobile Number *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("reg_mobile")
                    )
                    OutlinedTextField(
                        value = whatsapp,
                        onValueChange = { whatsapp = it },
                        label = { Text("WhatsApp Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = emergencyContact,
                    onValueChange = { emergencyContact = it },
                    label = { Text("Emergency Contact") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = gender,
                        onValueChange = { gender = it },
                        label = { Text("Gender") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dob,
                        onValueChange = { dob = it },
                        label = { Text("Date of Birth") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = bloodGroup,
                        onValueChange = { bloodGroup = it },
                        label = { Text("Blood") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }
            }

            // Gym & Automatic Fee Details
            GlassCard(modifier = Modifier.fillMaxWidth(), highlightAccent = StatusGold) {
                Text("Gym Details & Automatic Fee Cycle", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ID: $generatedId • Due Date auto-locks to Day ${joiningDate.dayOfMonth} every month",
                    style = MaterialTheme.typography.labelMedium,
                    color = StatusGold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Monthly Standard", "Pro Bodybuilding", "Strength + Cardio", "Competition Prep", "VIP Coaching").forEach { pkg ->
                        FilterChip(
                            selected = packageName == pkg,
                            onClick = { packageName = pkg },
                            label = { Text(pkg, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CrimsonPrimary,
                                selectedLabelColor = PureWhite
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = monthlyFeeText,
                    onValueChange = { monthlyFeeText = it.filter { c -> c.isDigit() } },
                    label = { Text("Monthly Fee (Rs.) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("reg_monthly_fee")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DatePickButton("Joining: ${FeeEngine.formatLocalDate(joiningDate)}", Modifier.weight(1f)) {
                        pickDate(joiningDate) { joiningDate = it; startDate = it; expiryDate = it.plusMonths(1) }
                    }
                    DatePickButton("Start: ${FeeEngine.formatLocalDate(startDate)}", Modifier.weight(1f)) {
                        pickDate(startDate) { startDate = it }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                DatePickButton("Expiry Date: ${FeeEngine.formatLocalDate(expiryDate)}", Modifier.fillMaxWidth()) {
                    pickDate(expiryDate) { expiryDate = it }
                }

                if (existingMember == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("First Month Paid on Registration?", style = MaterialTheme.typography.bodyMedium, color = PureWhite)
                            Text(
                                text = if (initialPaidToday) "Next due date set to next month on ${joiningDate.dayOfMonth}th" else "Marked due immediately on ${joiningDate.dayOfMonth}th",
                                style = MaterialTheme.typography.bodySmall,
                                color = CoolSilver
                            )
                        }
                        Switch(checked = initialPaidToday, onCheckedChange = { initialPaidToday = it })
                    }
                }

                if (data.trainers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box {
                        OutlinedButton(
                            onClick = { trainerMenuOpen = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (selectedTrainerName.isBlank()) "Assign Trainer (Optional)" else "Trainer: $selectedTrainerName",
                                color = PureWhite
                            )
                        }
                        DropdownMenu(
                            expanded = trainerMenuOpen,
                            onDismissRequest = { trainerMenuOpen = false },
                            modifier = Modifier.background(CarbonElevated)
                        ) {
                            data.trainers.forEach { tr ->
                                DropdownMenuItem(
                                    text = { Text("${tr.name} (${tr.specialization})", color = PureWhite) },
                                    onClick = {
                                        selectedTrainerId = tr.id
                                        selectedTrainerName = tr.name
                                        trainerMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Bodybuilder Initial Profile
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Bodybuilder Baseline", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = heightCmText,
                        onValueChange = { heightCmText = it },
                        label = { Text("Height (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = weightKgText,
                        onValueChange = { weightKgText = it },
                        label = { Text("Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = bodyFatText,
                        onValueChange = { bodyFatText = it },
                        label = { Text("Fat %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(0.9f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = fitnessGoal,
                    onValueChange = { fitnessGoal = it },
                    label = { Text("Fitness Goal (e.g. Classic Physique, Fat Loss)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Competition Preparation", color = PureWhite, style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = competitionPrep, onCheckedChange = { competitionPrep = it })
                }
            }

            if (validationError != null) {
                Text(validationError!!, color = StatusDueTodayRed, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = {
                    val cleanName = fullName.trim()
                    val cleanMobile = mobile.trim()
                    val fee = monthlyFeeText.toLongOrNull() ?: 0L
                    if (cleanName.isBlank() || cleanMobile.length < 3 || fee <= 0L) {
                        validationError = "Please enter Full Name, Mobile Number, and Monthly Fee."
                        return@Button
                    }
                    val dueDay = joiningDate.dayOfMonth
                    val paidThrough = if (existingMember != null) {
                        existingMember.paidThroughMillis
                    } else if (initialPaidToday) {
                        val nextMonth = YearMonth.from(joiningDate).plusMonths(1)
                        FeeEngine.localDateToMillis(FeeEngine.anchoredDueDate(nextMonth, dueDay))
                    } else {
                        FeeEngine.localDateToMillis(joiningDate)
                    }
                    val branchId = if (data.ownerProfile.activeBranchId == "ALL") {
                        data.branches.firstOrNull()?.id ?: "main"
                    } else {
                        data.ownerProfile.activeBranchId
                    }

                    val member = Member(
                        id = existingMember?.id ?: "",
                        userId = existingMember?.userId ?: "",
                        membershipId = generatedId,
                        branchId = existingMember?.branchId ?: branchId,
                        fullName = cleanName,
                        fatherName = fatherName.trim().takeIf { it.isNotBlank() },
                        cnic = cnic.trim().takeIf { it.isNotBlank() },
                        mobile = cleanMobile,
                        whatsapp = whatsapp.trim().ifBlank { cleanMobile },
                        emergencyContact = emergencyContact.trim().takeIf { it.isNotBlank() },
                        address = address.trim().takeIf { it.isNotBlank() },
                        gender = gender.trim().takeIf { it.isNotBlank() },
                        dateOfBirth = dob.trim().takeIf { it.isNotBlank() },
                        bloodGroup = bloodGroup.trim().takeIf { it.isNotBlank() },
                        packageName = packageName,
                        monthlyFee = fee,
                        pendingAmount = existingMember?.pendingAmount ?: 0L,
                        joiningDateMillis = FeeEngine.localDateToMillis(joiningDate),
                        startDateMillis = FeeEngine.localDateToMillis(startDate),
                        expiryDateMillis = FeeEngine.localDateToMillis(expiryDate),
                        paidThroughMillis = paidThrough,
                        lastPaymentDateMillis = if (existingMember == null && initialPaidToday) System.currentTimeMillis() else existingMember?.lastPaymentDateMillis,
                        dueDayOfMonth = dueDay.toLong(),
                        trainerId = selectedTrainerId.takeIf { it.isNotBlank() },
                        trainerName = selectedTrainerName.takeIf { it.isNotBlank() },
                        status = existingMember?.status ?: "ACTIVE",
                        heightCm = heightCmText.toDoubleOrNull(),
                        weightKg = weightKgText.toDoubleOrNull(),
                        bodyFatPercent = bodyFatText.toDoubleOrNull(),
                        fitnessGoal = fitnessGoal.trim().takeIf { it.isNotBlank() },
                        competitionPrep = competitionPrep,
                        photoWebpBase64 = photoBase64,
                        cnicFrontBase64 = cnicFrontBase64,
                        cnicBackBase64 = cnicBackBase64,
                        medicalCertBase64 = medicalCertBase64
                    )
                    onSave(member)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_member_btn")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (existingMember == null) "Save & Register Member" else "Update Member Profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun UploadDocButton(
    label: String,
    hasFile: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
        modifier = modifier.height(44.dp)
    ) {
        Text(
            text = if (hasFile) "✓ $label" else "+ $label",
            color = if (hasFile) StatusPaidGreen else CoolSilver,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

@Composable
private fun DatePickButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.height(46.dp)
    ) {
        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = CrimsonBright, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = PureWhite, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}
