package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.FeeEngine
import com.example.data.model.FeePayment
import com.example.data.model.Member
import com.example.data.model.MemberFeeStatus
import com.example.ui.components.GlassCard
import com.example.ui.components.UrgencyBadge
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.CoolSilver
import com.example.ui.theme.CrimsonBright
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StatusGold
import com.example.ui.theme.StatusInfoBlue
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.viewmodel.GymAppData
import com.example.util.CommunicationIntents
import com.example.util.ExportAndPrintUtils
import com.example.util.ImageWebpUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberAlertScreen(
    member: Member,
    data: GymAppData,
    onBack: () -> Unit,
    onCollectFee: (MemberFeeStatus) -> Unit,
    onWhatsAppSent: (Member, Long) -> Unit,
    onOpenDigitalCard: (Member) -> Unit,
    onOpenBodybuilderProfile: (Member) -> Unit,
    onEditMember: (Member) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val feeStatus = remember(member, data.dashboard.memberFeeStatuses) {
        data.dashboard.memberFeeStatuses.firstOrNull { it.member.id == member.id }
            ?: FeeEngine.computeMemberFeeStatus(member)
    }
    val memberPayments = remember(data.payments, member.id) {
        data.payments.filter { it.memberId == member.id }
    }
    var showHistoryDialog by remember { mutableStateOf(false) }

    val profileImg = remember(member.photoWebpBase64) {
        ImageWebpUtils.decodeBase64ToImageBitmap(member.photoWebpBase64)
    }

    val reminderMessage = remember(member, feeStatus, data.ownerProfile) {
        CommunicationIntents.buildWhatsAppFeeMessage(
            memberName = member.fullName,
            amount = feeStatus.dueAmount,
            dueDate = FeeEngine.formatLocalDate(feeStatus.dueDate),
            gymName = data.ownerProfile.gymName,
            ownerPhone = data.ownerProfile.ownerPhone
        )
    }

    Scaffold(
        containerColor = ObsidianBlack,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBlack, titleContentColor = PureWhite),
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("member_alert_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
                    }
                },
                title = {
                    Text("Member Fee Alert", style = MaterialTheme.typography.headlineMedium)
                },
                actions = {
                    IconButton(onClick = { onEditMember(member) }, modifier = Modifier.testTag("member_alert_edit")) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Member", tint = PureWhite)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Large WEBP Photo & Identity Hero
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    highlightAccent = CrimsonPrimary
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(CarbonCard)
                                .border(2.dp, CrimsonPrimary, RoundedCornerShape(18.dp))
                        ) {
                            if (profileImg != null) {
                                Image(
                                    bitmap = profileImg,
                                    contentDescription = member.fullName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.img_athlete_placeholder_1794983436146),
                                    contentDescription = member.fullName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(member.fullName, style = MaterialTheme.typography.headlineLarge, color = PureWhite)
                            Text(
                                text = "ID: ${member.membershipId}",
                                style = MaterialTheme.typography.labelMedium,
                                color = CrimsonBright
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    color = CrimsonContainer,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.border(1.dp, CrimsonPrimary.copy(alpha = 0.5f), RoundedCornerShape(50))
                                ) {
                                    Text(
                                        text = member.status,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PureWhite,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                UrgencyBadge(urgency = feeStatus.urgency)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Auto Due: Every month on day ${member.dueDayOfMonth}",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusGold
                            )
                        }
                    }
                }
            }

            // All Required Member Fields
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Membership & Fee Details", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                    Spacer(modifier = Modifier.height(10.dp))
                    DetailRow("Full Name", member.fullName)
                    DetailRow("Membership ID", member.membershipId)
                    DetailRow("Phone Number", member.mobile)
                    DetailRow("WhatsApp", member.whatsapp)
                    DetailRow("Address", member.address ?: "Not provided")
                    DetailRow("Package", member.packageName)
                    DetailRow("Joining Date", FeeEngine.formatDate(member.joiningDateMillis))
                    DetailRow("Start Date", FeeEngine.formatDate(member.startDateMillis))
                    DetailRow("Expiry Date", FeeEngine.formatDate(member.expiryDateMillis))
                    DetailRow("Monthly Fee Amount", "Rs. ${member.monthlyFee}")
                    DetailRow("Pending / Due Amount", "Rs. ${feeStatus.dueAmount}", valueColor = CrimsonBright)
                    DetailRow(
                        "Last Payment Date",
                        member.lastPaymentDateMillis?.let { FeeEngine.formatDate(it) } ?: "No payments recorded"
                    )
                    if (!member.trainerName.isNullOrBlank()) {
                        DetailRow("Assigned Trainer", member.trainerName)
                    }
                }
            }

            // 6 Required Primary Action Buttons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onCollectFee(feeStatus) },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusPaidGreen, contentColor = ObsidianBlack),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("alert_collect_fee_btn")
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Collect Fee", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                CommunicationIntents.openWhatsAppReminder(
                                    context,
                                    member.whatsapp.ifBlank { member.mobile },
                                    reminderMessage
                                )
                                onWhatsAppSent(member, feeStatus.dueAmount)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary, contentColor = PureWhite),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("alert_whatsapp_btn")
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WhatsApp", fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AlertOutlineButton(
                            label = "Call Member",
                            icon = Icons.Default.Call,
                            tint = StatusInfoBlue,
                            modifier = Modifier.weight(1f),
                            tag = "alert_call_btn"
                        ) {
                            CommunicationIntents.dialMember(context, member.mobile)
                        }
                        AlertOutlineButton(
                            label = "Send SMS",
                            icon = Icons.Default.Sms,
                            tint = StatusGold,
                            modifier = Modifier.weight(1f),
                            tag = "alert_sms_btn"
                        ) {
                            CommunicationIntents.sendSms(context, member.mobile, reminderMessage)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AlertOutlineButton(
                            label = "Payment History (${memberPayments.size})",
                            icon = Icons.Default.History,
                            tint = PureWhite,
                            modifier = Modifier.weight(1f),
                            tag = "alert_history_btn"
                        ) {
                            showHistoryDialog = true
                        }
                        AlertOutlineButton(
                            label = "Print Receipt",
                            icon = Icons.Default.Print,
                            tint = CrimsonBright,
                            modifier = Modifier.weight(1f),
                            tag = "alert_print_btn"
                        ) {
                            ExportAndPrintUtils.printFeeReceipt(
                                context = context,
                                ownerProfile = data.ownerProfile,
                                member = member,
                                payment = memberPayments.firstOrNull()
                            )
                        }
                    }

                    // Digital ID Card & Bodybuilder Profile
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AlertOutlineButton(
                            label = "Digital ID Card",
                            icon = Icons.Default.QrCode2,
                            tint = CrimsonBright,
                            modifier = Modifier.weight(1f),
                            tag = "alert_digital_card_btn"
                        ) {
                            onOpenDigitalCard(member)
                        }
                        AlertOutlineButton(
                            label = "Bodybuilder Profile",
                            icon = Icons.Default.FitnessCenter,
                            tint = StatusGold,
                            modifier = Modifier.weight(1f),
                            tag = "alert_bodybuilder_btn"
                        ) {
                            onOpenBodybuilderProfile(member)
                        }
                    }
                }
            }

            // Documents Preview (CNIC Front / Back / Medical Cert)
            if (!member.cnicFrontBase64.isNullOrBlank() || !member.cnicBackBase64.isNullOrBlank() || !member.medicalCertBase64.isNullOrBlank()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Uploaded KYC & Medical Documents", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DocThumbnail("CNIC Front", member.cnicFrontBase64, Modifier.weight(1f))
                            DocThumbnail("CNIC Back", member.cnicBackBase64, Modifier.weight(1f))
                            DocThumbnail("Medical", member.medicalCertBase64, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    if (showHistoryDialog) {
        PaymentHistoryDialog(
            member = member,
            payments = memberPayments,
            onPrintReceipt = { pay ->
                ExportAndPrintUtils.printFeeReceipt(context, data.ownerProfile, member, pay)
            },
            onDismiss = { showHistoryDialog = false }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color = PureWhite) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = CoolSilver)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AlertOutlineButton(
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    tag: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(48.dp)
            .testTag(tag)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = PureWhite, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
private fun DocThumbnail(title: String, base64: String?, modifier: Modifier = Modifier) {
    val bmp = remember(base64) { ImageWebpUtils.decodeBase64ToImageBitmap(base64) }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CarbonCard)
                .border(1.dp, GlassBorder, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (bmp != null) {
                Image(bitmap = bmp, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Default.Badge, contentDescription = title, tint = CoolSilver)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.labelSmall, color = CoolSilver)
    }
}
