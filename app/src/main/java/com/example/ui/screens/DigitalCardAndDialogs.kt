package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.FeeEngine
import com.example.data.model.FeePayment
import com.example.data.model.Member
import com.example.data.model.MemberFeeStatus
import com.example.data.model.OwnerProfile
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
import com.example.ui.theme.StatusDueTodayRed
import com.example.ui.theme.StatusPaidGreen
import com.example.util.ExportAndPrintUtils
import com.example.util.QrAndBarcodeUtils

@Composable
fun CollectFeeDialog(
    feeStatus: MemberFeeStatus,
    onDismiss: () -> Unit,
    onConfirm: (amount: Long, discount: Long, method: String, notes: String?) -> Unit
) {
    var amountText by remember { mutableStateOf(feeStatus.dueAmount.toString()) }
    var discountText by remember { mutableStateOf("0") }
    var method by remember { mutableStateOf("CASH") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CarbonElevated,
        titleContentColor = PureWhite,
        title = {
            Column {
                Text("Collect Fee • ${feeStatus.member.fullName}", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Cycle: ${feeStatus.cycleLabel} • Due ${FeeEngine.formatLocalDate(feeStatus.dueDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = CoolSilver
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Fee Amount (Rs.)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("collect_amount_input")
                )
                OutlinedTextField(
                    value = discountText,
                    onValueChange = { discountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Discount (Rs.)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Payment Method", style = MaterialTheme.typography.labelLarge, color = CoolSilver)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("CASH", "BANK_TRANSFER", "JAZZCASH", "EASYPAISA").forEach { m ->
                        FilterChip(
                            selected = method == m,
                            onClick = { method = m },
                            label = { Text(m.take(8), style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CrimsonPrimary,
                                selectedLabelColor = PureWhite
                            )
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toLongOrNull() ?: feeStatus.dueAmount
                    val disc = discountText.toLongOrNull() ?: 0L
                    onConfirm(amt, disc, method, notes.takeIf { it.isNotBlank() })
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusPaidGreen, contentColor = ObsidianBlack),
                modifier = Modifier.testTag("confirm_collect_fee_btn")
            ) {
                Text("Confirm & Collect", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = CoolSilver)
            }
        }
    )
}

@Composable
fun PaymentHistoryDialog(
    member: Member,
    payments: List<FeePayment>,
    onPrintReceipt: (FeePayment) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CarbonElevated,
        titleContentColor = PureWhite,
        title = {
            Text("Payment History • ${member.fullName}", style = MaterialTheme.typography.headlineMedium)
        },
        text = {
            if (payments.isEmpty()) {
                Text("No fee payments recorded yet for ${member.fullName}.", color = CoolSilver)
            } else {
                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(payments, key = { it.id }) { pay ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Rs. ${pay.amount} • ${pay.forMonthLabel}", style = MaterialTheme.typography.titleSmall, color = StatusPaidGreen)
                                    Text("${pay.receiptNumber} • ${pay.paymentMethod}", style = MaterialTheme.typography.labelSmall, color = CoolSilver)
                                    Text("Paid: ${FeeEngine.formatDate(pay.paidAtMillis)}", style = MaterialTheme.typography.labelSmall, color = CoolSilver)
                                }
                                IconButton(onClick = { onPrintReceipt(pay) }) {
                                    Icon(Icons.Default.Print, contentDescription = "Print Receipt", tint = PureWhite)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = CrimsonBright)
            }
        }
    )
}

@Composable
fun DigitalMembershipCardDialog(
    member: Member,
    ownerProfile: OwnerProfile,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val qrMatrix = remember(member.membershipId, member.status) {
        QrAndBarcodeUtils.generateQrMatrix("${member.membershipId}|${member.fullName}|${member.status}")
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = CarbonElevated,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CrimsonPrimary.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Visual Card Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(ObsidianBlack, CarbonCard, Color(0xFF280B14))
                            )
                        )
                        .border(1.5.dp, CrimsonPrimary, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ownerProfile.gymName.uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = PureWhite
                            )
                            Surface(
                                color = CrimsonPrimary,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = member.status,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PureWhite,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                MemberAvatar(
                                    photoWebpBase64 = member.photoWebpBase64,
                                    fullName = member.fullName,
                                    size = 64.dp,
                                    borderColor = CrimsonBright
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(member.fullName, style = MaterialTheme.typography.titleLarge, color = PureWhite)
                                    Text(member.membershipId, style = MaterialTheme.typography.labelMedium, color = CrimsonBright)
                                    Text(member.packageName, style = MaterialTheme.typography.bodySmall, color = CoolSilver)
                                    Text(
                                        text = "Exp: ${FeeEngine.formatDate(member.expiryDateMillis)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusPaidGreen
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White)
                                    .padding(6.dp)
                            ) {
                                QrMatrixCanvas(matrix = qrMatrix, modifier = Modifier.size(80.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            ExportAndPrintUtils.printOrShareDigitalCard(context, ownerProfile, member, shareAsImage = true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        modifier = Modifier.weight(1f).testTag("download_card_btn")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download")
                    }
                    OutlinedButton(
                        onClick = {
                            ExportAndPrintUtils.printOrShareDigitalCard(context, ownerProfile, member, shareAsImage = false)
                        },
                        modifier = Modifier.weight(1f).testTag("print_card_btn")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print", color = PureWhite)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close", color = CoolSilver)
                }
            }
        }
    }
}

@Composable
fun QrMatrixCanvas(matrix: Array<BooleanArray>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val n = matrix.size
        if (n == 0) return@Canvas
        val cellW = size.width / n
        val cellH = size.height / n
        drawRect(Color.White)
        for (r in 0 until n) {
            for (c in 0 until n) {
                if (matrix[r][c]) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(c * cellW, r * cellH),
                        size = Size(cellW + 0.5f, cellH + 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun PinLockScreen(
    gymName: String,
    onUnlock: (String) -> Boolean
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ObsidianBlack)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            highlightAccent = CrimsonPrimary
        ) {
            Icon(Icons.Default.Lock, contentDescription = "2FA PIN", tint = CrimsonBright, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text("Two-Factor Security Lock", style = MaterialTheme.typography.headlineMedium, color = PureWhite)
            Text("Enter Owner PIN to access $gymName", style = MaterialTheme.typography.bodySmall, color = CoolSilver)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it; error = null },
                label = { Text("Owner Security PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("pin_lock_input")
            )
            if (error != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(error!!, color = StatusDueTodayRed, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = {
                    if (!onUnlock(pin)) {
                        error = "Incorrect PIN. Try again."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pin_unlock_btn")
            ) {
                Text("Unlock Owner Dashboard")
            }
        }
    }
}
