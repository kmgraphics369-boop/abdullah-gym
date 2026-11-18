package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Branch
import com.example.data.model.Equipment
import com.example.data.model.Expense
import com.example.data.model.FeeEngine
import com.example.data.model.FeeUrgency
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
import com.example.util.ExportAndPrintUtils
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun FinanceAndReportsScreen(
    data: GymAppData,
    onSaveExpense: (Expense) -> Unit,
    onSaveEquipment: (Equipment) -> Unit,
    onSaveBranch: (Branch) -> Unit,
    onUpdateSettings: (gymName: String, ownerName: String, ownerPhone: String, lang: String, branchId: String, pin: String?) -> Unit,
    onBuildBackupJson: () -> String,
    onSignOut: () -> Unit,
    isDevSandbox: Boolean = false,
    timeOffsetDays: Long = 0L,
    onShiftTestDays: (Long) -> Unit = {},
    onResetTestTime: () -> Unit = {},
    onResetSandboxData: () -> Unit = {},
    onSeedSampleLocalData: () -> Unit = {},
    serverEndpoint: String = "http://127.0.0.1:8765"
) {
    var activeTab by remember { mutableStateOf("REPORTS") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("Finance, Reports & Admin", style = MaterialTheme.typography.headlineLarge, color = PureWhite)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "REPORTS" to "Reports",
                    "EXPENSES" to "Expenses",
                    "EQUIPMENT" to "Equipment",
                    "SETTINGS" to "Security & Settings"
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
                        modifier = Modifier.testTag("fin_tab_$key")
                    )
                }
            }
        }

        when (activeTab) {
            "REPORTS" -> ReportsTab(data)
            "EXPENSES" -> ExpensesTab(data, onSaveExpense)
            "EQUIPMENT" -> EquipmentTab(data, onSaveEquipment)
            "SETTINGS" -> SettingsAndSecurityTab(
                data = data,
                onSaveBranch = onSaveBranch,
                onUpdateSettings = onUpdateSettings,
                onBuildBackupJson = onBuildBackupJson,
                onSignOut = onSignOut,
                isDevSandbox = isDevSandbox,
                timeOffsetDays = timeOffsetDays,
                onShiftTestDays = onShiftTestDays,
                onResetTestTime = onResetTestTime,
                onResetSandboxData = onResetSandboxData,
                onSeedSampleLocalData = onSeedSampleLocalData,
                serverEndpoint = serverEndpoint
            )
        }
    }
}

@Composable
private fun ReportsTab(data: GymAppData) {
    val context = LocalContext.current
    var selectedReport by remember { mutableStateOf("DAILY_COLLECTION") }

    val reportTypes = listOf(
        "DAILY_COLLECTION" to "Daily Collection",
        "WEEKLY" to "Weekly Report",
        "MONTHLY" to "Monthly Report",
        "ANNUAL" to "Annual Report",
        "DUE_FEE" to "Due Fee Report",
        "OVERDUE_FEE" to "Overdue Fee Report",
        "MEMBER" to "Member Report",
        "ATTENDANCE" to "Attendance Report",
        "EXPENSE" to "Expense Report",
        "PROFIT" to "Profit Report"
    )

    val (headers, rows) = remember(selectedReport, data) {
        val today = LocalDate.now()
        val currentMonth = YearMonth.from(today)
        when (selectedReport) {
            "DAILY_COLLECTION" -> listOf("Receipt", "Member", "Method", "Amount") to
                data.payments.filter { FeeEngine.millisToLocalDate(it.paidAtMillis) == today }
                    .map { listOf(it.receiptNumber, it.memberName, it.paymentMethod, "Rs.${it.amount}") }
            "WEEKLY" -> listOf("Date", "Member", "Cycle", "Amount") to
                data.payments.filter { !FeeEngine.millisToLocalDate(it.paidAtMillis).isBefore(today.minusDays(7)) }
                    .map { listOf(FeeEngine.formatDate(it.paidAtMillis), it.memberName, it.forMonthLabel, "Rs.${it.amount}") }
            "MONTHLY" -> listOf("Date", "Member", "Cycle", "Amount") to
                data.payments.filter { YearMonth.from(FeeEngine.millisToLocalDate(it.paidAtMillis)) == currentMonth }
                    .map { listOf(FeeEngine.formatDate(it.paidAtMillis), it.memberName, it.forMonthLabel, "Rs.${it.amount}") }
            "ANNUAL" -> listOf("Date", "Member", "Method", "Amount") to
                data.payments.filter { FeeEngine.millisToLocalDate(it.paidAtMillis).year == today.year }
                    .map { listOf(FeeEngine.formatDate(it.paidAtMillis), it.memberName, it.paymentMethod, "Rs.${it.amount}") }
            "DUE_FEE" -> listOf("ID", "Member", "Due Date", "Amount") to
                data.dashboard.memberFeeStatuses.filter { it.urgency == FeeUrgency.DUE_TODAY || it.urgency == FeeUrgency.DUE_SOON }
                    .map { listOf(it.member.membershipId, it.member.fullName, FeeEngine.formatLocalDate(it.dueDate), "Rs.${it.dueAmount}") }
            "OVERDUE_FEE" -> listOf("ID", "Member", "Days Over", "Amount") to
                data.dashboard.memberFeeStatuses.filter { it.urgency == FeeUrgency.OVERDUE }
                    .map { listOf(it.member.membershipId, it.member.fullName, "${-it.daysRemaining}d", "Rs.${it.dueAmount}") }
            "MEMBER" -> listOf("ID", "Name", "Package", "Status") to
                data.members.map { listOf(it.membershipId, it.fullName, it.packageName, it.status) }
            "ATTENDANCE" -> listOf("Date", "Member", "ID", "Method") to
                data.attendance.map { listOf(it.dateKey, it.memberName, it.membershipId, it.method) }
            "EXPENSE" -> listOf("Date", "Category", "Title", "Amount") to
                data.expenses.map { listOf(FeeEngine.formatDate(it.expenseDateMillis), it.category, it.title, "Rs.${it.amount}") }
            else -> listOf("Metric", "Period", "Category", "Value") to listOf(
                listOf("Monthly Revenue", currentMonth.toString(), "Income", "Rs.${data.dashboard.monthlyRevenue}"),
                listOf("Monthly Expenses", currentMonth.toString(), "Expense", "Rs.${data.dashboard.monthlyExpenses}"),
                listOf("Net Profit", currentMonth.toString(), "Net", "Rs.${data.dashboard.totalProfit}")
            )
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                reportTypes.forEach { (key, label) ->
                    FilterChip(
                        selected = selectedReport == key,
                        onClick = { selectedReport = key },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CrimsonPrimary, selectedLabelColor = PureWhite)
                    )
                }
            }
        }

        // Export PDF / Excel / CSV Buttons
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        ExportAndPrintUtils.exportPdfReport(
                            context = context,
                            fileName = "gym_${selectedReport.lowercase()}",
                            title = "${data.ownerProfile.gymName} - ${reportTypes.first { it.first == selectedReport }.second}",
                            subtitle = "Generated ${FeeEngine.todayDateKey()} • Rows: ${rows.size}",
                            headers = headers,
                            rows = rows
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier.weight(1f).testTag("export_pdf_btn")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF")
                }
                OutlinedButton(
                    onClick = {
                        ExportAndPrintUtils.exportExcelXml(
                            context = context,
                            fileName = "gym_${selectedReport.lowercase()}",
                            sheetTitle = selectedReport,
                            headers = headers,
                            rows = rows
                        )
                    },
                    modifier = Modifier.weight(1f).testTag("export_excel_btn")
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, tint = StatusPaidGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Excel", color = PureWhite)
                }
                OutlinedButton(
                    onClick = {
                        ExportAndPrintUtils.exportCsv(
                            context = context,
                            fileName = "gym_${selectedReport.lowercase()}",
                            headers = headers,
                            rows = rows
                        )
                    },
                    modifier = Modifier.weight(1f).testTag("export_csv_btn")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = StatusInfoBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CSV", color = PureWhite)
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    headers.forEach { h ->
                        Text(h, style = MaterialTheme.typography.labelMedium, color = CrimsonBright, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                if (rows.isEmpty()) {
                    Text("No records in this report period.", style = MaterialTheme.typography.bodySmall, color = CoolSilver)
                }
            }
        }
        items(rows) { row ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEach { cell ->
                        Text(cell, style = MaterialTheme.typography.bodySmall, color = PureWhite, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpensesTab(data: GymAppData, onSaveExpense: (Expense) -> Unit) {
    var showAdd by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(
                onClick = { showAdd = true },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                modifier = Modifier.fillMaxWidth().testTag("add_expense_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Record Expense")
            }
        }
        items(data.expenses, key = { it.id }) { exp ->
            GlassCard(modifier = Modifier.fillMaxWidth(), highlightAccent = StatusDueTodayRed) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(exp.title, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                        Text("${exp.category} • ${FeeEngine.formatDate(exp.expenseDateMillis)}", style = MaterialTheme.typography.bodySmall, color = CoolSilver)
                    }
                    Text("Rs. ${exp.amount}", style = MaterialTheme.typography.titleMedium, color = CrimsonBright, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showAdd) {
        var category by remember { mutableStateOf("RENT") }
        var title by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAdd = false },
            containerColor = CarbonElevated,
            titleContentColor = PureWhite,
            title = { Text("Record Gym Expense") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("RENT", "ELECTRICITY", "WATER", "SALARIES", "EQUIPMENT", "MAINTENANCE").forEach { c ->
                            FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Expense Title") }, singleLine = true)
                    OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() } }, label = { Text("Amount (Rs.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amount.toLongOrNull() ?: 0L
                        if (title.isNotBlank() && amt > 0) {
                            val br = if (data.ownerProfile.activeBranchId == "ALL") "main" else data.ownerProfile.activeBranchId
                            onSaveExpense(Expense(category = category, title = title.trim(), amount = amt, branchId = br))
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
private fun EquipmentTab(data: GymAppData, onSaveEquipment: (Equipment) -> Unit) {
    var showAdd by remember { mutableStateOf(false) }
    val now = System.currentTimeMillis()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(
                onClick = { showAdd = true },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                modifier = Modifier.fillMaxWidth().testTag("add_equipment_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Gym Equipment")
            }
        }
        items(data.equipment, key = { it.id }) { eq ->
            val alert = eq.nextMaintenanceMillis <= now + 86400000L * 7L || eq.condition != "EXCELLENT" && eq.condition != "GOOD"
            GlassCard(modifier = Modifier.fillMaxWidth(), highlightAccent = if (alert) StatusDueSoonAmber else StatusPaidGreen) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(eq.name, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                        Text("Cost: Rs. ${eq.cost} • Condition: ${eq.condition}", style = MaterialTheme.typography.bodySmall, color = CoolSilver)
                        Text("Next Maintenance: ${FeeEngine.formatDate(eq.nextMaintenanceMillis)}", style = MaterialTheme.typography.labelSmall, color = if (alert) StatusDueSoonAmber else StatusPaidGreen)
                    }
                    if (alert) {
                        Icon(Icons.Default.Warning, contentDescription = "Maintenance Alert", tint = StatusDueSoonAmber)
                    }
                }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var cost by remember { mutableStateOf("150000") }
        var condition by remember { mutableStateOf("GOOD") }

        AlertDialog(
            onDismissRequest = { showAdd = false },
            containerColor = CarbonElevated,
            titleContentColor = PureWhite,
            title = { Text("Add Equipment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Equipment Name") }, singleLine = true)
                    OutlinedTextField(value = cost, onValueChange = { cost = it.filter { c -> c.isDigit() } }, label = { Text("Cost (Rs.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("EXCELLENT", "GOOD", "NEEDS_SERVICE", "OUT_OF_ORDER").forEach { c ->
                            FilterChip(selected = condition == c, onClick = { condition = c }, label = { Text(c, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val br = if (data.ownerProfile.activeBranchId == "ALL") "main" else data.ownerProfile.activeBranchId
                            onSaveEquipment(
                                Equipment(
                                    name = name.trim(),
                                    cost = cost.toLongOrNull() ?: 0L,
                                    condition = condition,
                                    nextMaintenanceMillis = System.currentTimeMillis() + 86400000L * 30L,
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
private fun SettingsAndSecurityTab(
    data: GymAppData,
    onSaveBranch: (Branch) -> Unit,
    onUpdateSettings: (gymName: String, ownerName: String, ownerPhone: String, lang: String, branchId: String, pin: String?) -> Unit,
    onBuildBackupJson: () -> String,
    onSignOut: () -> Unit,
    isDevSandbox: Boolean,
    timeOffsetDays: Long,
    onShiftTestDays: (Long) -> Unit,
    onResetTestTime: () -> Unit,
    onResetSandboxData: () -> Unit,
    onSeedSampleLocalData: () -> Unit,
    serverEndpoint: String
) {
    val context = LocalContext.current
    var gymName by remember(data.ownerProfile.gymName) { mutableStateOf(data.ownerProfile.gymName) }
    var ownerName by remember(data.ownerProfile.ownerName) { mutableStateOf(data.ownerProfile.ownerName) }
    var ownerPhone by remember(data.ownerProfile.ownerPhone) { mutableStateOf(data.ownerProfile.ownerPhone) }
    var twoFaPin by remember(data.ownerProfile.twoFactorPin) { mutableStateOf(data.ownerProfile.twoFactorPin ?: "") }
    var showBranchDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), highlightAccent = CrimsonPrimary) {
                Text("Gym Profile & WhatsApp Signature", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = gymName, onValueChange = { gymName = it }, label = { Text("Gym Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("Owner Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = ownerPhone, onValueChange = { ownerPhone = it }, label = { Text("Owner Phone (Shown in WhatsApp)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = twoFaPin, onValueChange = { twoFaPin = it.filter { c -> c.isDigit() }.take(6) }, label = { Text("2FA Owner PIN Lock (Optional 4-6 digits)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        onUpdateSettings(
                            gymName,
                            ownerName,
                            ownerPhone,
                            data.ownerProfile.language,
                            data.ownerProfile.activeBranchId,
                            twoFaPin.takeIf { it.isNotBlank() }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Club Settings")
                }
            }
        }

        // Multi-Branch Support
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Multi-Branch Locations (${data.branches.size})", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                    OutlinedButton(onClick = { showBranchDialog = true }) {
                        Text("+ Add Branch", color = CrimsonBright)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                data.branches.forEach { b ->
                    Text("• ${b.name} (${b.city}) — ${b.address}", style = MaterialTheme.typography.bodySmall, color = CoolSilver)
                }
            }
        }

        // Developer Test Mode (Time Travel & Sandbox / Sample Seed)
        item {
            val simDate = FeeEngine.formatLocalDate(LocalDate.now().plusDays(timeOffsetDays))
            val offsetLabel = when {
                timeOffsetDays == 0L -> "Today (0d offset)"
                timeOffsetDays > 0L -> "+${timeOffsetDays}d simulated"
                else -> "${timeOffsetDays}d simulated"
            }
            GlassCard(modifier = Modifier.fillMaxWidth(), highlightAccent = StatusDueSoonAmber) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Science, contentDescription = "Developer Test Mode", tint = StatusDueSoonAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Developer Test Mode", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isDevSandbox) {
                        "Mode: Local Offline Sandbox • Simulated Date: $simDate ($offsetLabel)"
                    } else {
                        "Mode: Local Server SQLite • Simulated Date: $simDate ($offsetLabel)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusGold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Shift the simulated calendar day to test automatic monthly fee due transitions (Paid → Due Soon → Due Today → Overdue).",
                    style = MaterialTheme.typography.bodySmall,
                    color = CoolSilver
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(onClick = { onShiftTestDays(-5L) }, modifier = Modifier.testTag("dev_shift_minus5")) {
                        Text("-5d", color = PureWhite)
                    }
                    OutlinedButton(onClick = onResetTestTime, modifier = Modifier.testTag("dev_shift_reset")) {
                        Text("Today", color = StatusPaidGreen)
                    }
                    OutlinedButton(onClick = { onShiftTestDays(1L) }, modifier = Modifier.testTag("dev_shift_plus1")) {
                        Text("+1d", color = PureWhite)
                    }
                    OutlinedButton(onClick = { onShiftTestDays(5L) }, modifier = Modifier.testTag("dev_shift_plus5")) {
                        Text("+5d", color = StatusDueSoonAmber)
                    }
                    OutlinedButton(onClick = { onShiftTestDays(15L) }, modifier = Modifier.testTag("dev_shift_plus15")) {
                        Text("+15d", color = CrimsonBright)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (isDevSandbox) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onResetSandboxData,
                            colors = ButtonDefaults.buttonColors(containerColor = StatusDueSoonAmber, contentColor = ObsidianBlack),
                            modifier = Modifier.weight(1f).testTag("dev_reset_sandbox_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset Sample Data", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier.weight(1f).testTag("dev_exit_sandbox_btn")
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, tint = CrimsonBright, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Exit Test Mode", color = PureWhite)
                        }
                    }
                } else {
                    Button(
                        onClick = onSeedSampleLocalData,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusDueSoonAmber, contentColor = ObsidianBlack),
                        modifier = Modifier.fillMaxWidth().testTag("dev_seed_local_btn")
                    ) {
                        Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Load Sample Gym Test Data", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Local Server Backup & Security
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), highlightAccent = StatusInfoBlue) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = StatusInfoBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Local Server Backup & Security", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isDevSandbox) {
                        "Local Developer Sandbox (In-memory — Local Server DB paused)"
                    } else {
                        "Local Server Active at $serverEndpoint • App ID: ${data.ownerProfile.userId.ifBlank { "imran" }}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDevSandbox) StatusDueSoonAmber else StatusPaidGreen
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val json = onBuildBackupJson()
                            ExportAndPrintUtils.exportJsonBackup(context, json)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusInfoBlue, contentColor = ObsidianBlack),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Backup", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier.weight(1f).testTag("sign_out_btn")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = CrimsonBright, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isDevSandbox) "Exit Dev Mode" else "Sign Out", color = PureWhite)
                    }
                }
            }
        }

        // Audit Logs
        item {
            Text("Security Audit Logs (${data.auditLogs.size})", style = MaterialTheme.typography.titleMedium, color = PureWhite)
        }
        items(data.auditLogs.take(20), key = { it.id }) { log ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("${log.action} • ${FeeEngine.formatDate(log.timestampMillis)}", style = MaterialTheme.typography.labelMedium, color = StatusGold)
                Text(log.details, style = MaterialTheme.typography.bodySmall, color = PureWhite)
            }
        }
    }

    if (showBranchDialog) {
        var name by remember { mutableStateOf("") }
        var city by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBranchDialog = false },
            containerColor = CarbonElevated,
            titleContentColor = PureWhite,
            title = { Text("Add Gym Branch") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Branch Name") }, singleLine = true)
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, singleLine = true)
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSaveBranch(Branch(name = name.trim(), city = city.trim(), address = address.trim()))
                            showBranchDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showBranchDialog = false }) { Text("Cancel", color = CoolSilver) } }
        )
    }
}
