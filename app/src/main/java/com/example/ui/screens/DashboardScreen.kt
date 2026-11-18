package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.FeeEngine
import com.example.data.model.FeeUrgency
import com.example.data.model.Member
import com.example.data.model.MemberFeeStatus
import com.example.ui.components.GlassCard
import com.example.ui.components.MemberAvatar
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatMetricTile
import com.example.ui.components.UrgencyBadge
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CoolSilver
import com.example.ui.theme.CrimsonBright
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StatusDueSoonAmber
import com.example.ui.theme.StatusDueTodayRed
import com.example.ui.theme.StatusGold
import com.example.ui.theme.StatusInfoBlue
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.viewmodel.GymAppData
import com.example.util.CommunicationIntents
import com.example.util.LocalizationStrings

@Composable
fun DashboardScreen(
    data: GymAppData,
    onOpenNotifications: () -> Unit,
    onOpenMemberAlert: (Member) -> Unit,
    onCollectFeeClick: (MemberFeeStatus) -> Unit,
    onWhatsAppSent: (Member, Long) -> Unit,
    onOpenRegisterMember: () -> Unit,
    onOpenAttendance: () -> Unit,
    onOpenBodybuilder: () -> Unit,
    onSwitchLanguage: (String) -> Unit,
    onSwitchBranch: (String) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalizationStrings.forLang(data.ownerProfile.language)
    val metrics = data.dashboard
    var branchMenuOpen by remember { mutableStateOf(false) }

    val activeBranchLabel = remember(data.branches, data.ownerProfile.activeBranchId) {
        if (data.ownerProfile.activeBranchId == "ALL") {
            "All Branches"
        } else {
            data.branches.firstOrNull { it.id == data.ownerProfile.activeBranchId }?.name ?: "Main Branch"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Header with Notification Bell, Branch Switcher & Language Toggle
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_gym_hero_1794983438058),
                    contentDescription = "Club Hero Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    ObsidianBlack.copy(alpha = 0.55f),
                                    ObsidianBlack.copy(alpha = 0.85f),
                                    ObsidianBlack
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = data.ownerProfile.gymName.uppercase(),
                                style = MaterialTheme.typography.headlineLarge,
                                color = PureWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${strings.appTitle} • ${data.ownerProfile.ownerName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = CoolSilver
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Language EN / UR Pill
                            Surface(
                                color = CarbonElevated.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .border(1.dp, GlassBorder, RoundedCornerShape(50))
                                    .clickable {
                                        val nextLang = if (data.ownerProfile.language == "en") "ur" else "en"
                                        onSwitchLanguage(nextLang)
                                    }
                                    .testTag("language_toggle_button")
                            ) {
                                Text(
                                    text = if (data.ownerProfile.language == "ur") "URDU / EN" else "EN / اردو",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PureWhite,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }

                            // Notification Bell with Live Badge Count
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(CrimsonContainer.copy(alpha = 0.9f))
                                    .border(1.dp, CrimsonPrimary.copy(alpha = 0.6f), CircleShape)
                                    .clickable(onClick = onOpenNotifications)
                                    .testTag("notification_bell_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = strings.smartNotifications,
                                    tint = PureWhite,
                                    modifier = Modifier.size(22.dp)
                                )
                                if (metrics.alertCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .clip(CircleShape)
                                            .background(CrimsonBright)
                                            .padding(horizontal = 5.dp, vertical = 1.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = metrics.alertCount.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PureWhite,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Branch selector + Quick Action chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            Surface(
                                color = CarbonElevated.copy(alpha = 0.92f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                    .clickable { branchMenuOpen = true }
                                    .testTag("branch_selector_chip")
                            ) {
                                Text(
                                    text = "Branch: $activeBranchLabel ▾",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = PureWhite,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = branchMenuOpen,
                                onDismissRequest = { branchMenuOpen = false },
                                modifier = Modifier.background(CarbonElevated)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Branches", color = PureWhite) },
                                    onClick = {
                                        branchMenuOpen = false
                                        onSwitchBranch("ALL")
                                    }
                                )
                                data.branches.forEach { b ->
                                    DropdownMenuItem(
                                        text = { Text("${b.name} (${b.city})", color = PureWhite) },
                                        onClick = {
                                            branchMenuOpen = false
                                            onSwitchBranch(b.id)
                                        }
                                    )
                                }
                            }
                        }

                        QuickHeroPill(
                            label = "+ ${strings.registerMember}",
                            accent = CrimsonPrimary,
                            onClick = onOpenRegisterMember,
                            tag = "quick_register_member"
                        )
                        QuickHeroPill(
                            label = "QR Check-In",
                            accent = StatusInfoBlue,
                            onClick = onOpenAttendance,
                            tag = "quick_qr_checkin"
                        )
                        QuickHeroPill(
                            label = "Bodybuilder Lab",
                            accent = StatusGold,
                            onClick = onOpenBodybuilder,
                            tag = "quick_bodybuilder"
                        )
                    }
                }
            }
        }

        // 9 Owner Dashboard KPI Cards (3x3 clean responsive rows)
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricTile(
                        title = strings.todaysCollection,
                        value = "Rs.${metrics.todaysCollection}",
                        icon = Icons.Default.Payments,
                        accentColor = StatusPaidGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricTile(
                        title = strings.todaysDueFees,
                        value = "Rs.${metrics.todaysDueFees}",
                        icon = Icons.Default.Schedule,
                        accentColor = StatusDueTodayRed,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricTile(
                        title = strings.overdueFees,
                        value = "Rs.${metrics.overdueFees}",
                        icon = Icons.Default.Warning,
                        accentColor = StatusDueSoonAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricTile(
                        title = strings.totalMembers,
                        value = metrics.totalMembers.toString(),
                        icon = Icons.Default.Group,
                        accentColor = StatusInfoBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricTile(
                        title = strings.activeMembers,
                        value = metrics.activeMembers.toString(),
                        icon = Icons.Default.CheckCircle,
                        accentColor = StatusPaidGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricTile(
                        title = strings.expiredMembers,
                        value = metrics.expiredMembers.toString(),
                        icon = Icons.Default.Cancel,
                        accentColor = CrimsonBright,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricTile(
                        title = strings.monthlyRevenue,
                        value = "Rs.${metrics.monthlyRevenue}",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        accentColor = StatusPaidGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricTile(
                        title = strings.monthlyExpenses,
                        value = "Rs.${metrics.monthlyExpenses}",
                        icon = Icons.AutoMirrored.Filled.TrendingDown,
                        accentColor = StatusDueTodayRed,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricTile(
                        title = strings.totalProfit,
                        value = "Rs.${metrics.totalProfit}",
                        icon = Icons.Default.EmojiEvents,
                        accentColor = if (metrics.totalProfit >= 0) StatusGold else StatusDueTodayRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Top Priority Section: Today's Fee Due Members
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                SectionHeader(
                    title = strings.todaysFeeDueMembers,
                    actionLabel = "${metrics.priorityDueMembers.size} Due",
                    onAction = onOpenNotifications
                )
            }
        }

        if (metrics.priorityDueMembers.isEmpty()) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        highlightAccent = if (data.members.isEmpty()) CrimsonPrimary else StatusPaidGreen
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (data.members.isEmpty()) Icons.Default.FitnessCenter else Icons.Default.CheckCircle,
                                contentDescription = "Status",
                                tint = if (data.members.isEmpty()) CrimsonBright else StatusPaidGreen,
                                modifier = Modifier.size(34.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (data.members.isEmpty()) {
                                        "No Members Registered Yet"
                                    } else {
                                        "All Member Fees Are Up To Date"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PureWhite
                                )
                                Text(
                                    text = if (data.members.isEmpty()) {
                                        "Register your first gym member to automate monthly due dates and WhatsApp reminders."
                                    } else {
                                        "Zero members due today or overdue. Tap the Bell icon to inspect upcoming cycles."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CoolSilver
                                )
                            }
                        }
                        if (data.members.isEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onOpenRegisterMember,
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("empty_dashboard_register_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.registerMember)
                            }
                        }
                    }
                }
            }
        } else {
            items(metrics.priorityDueMembers, key = { it.member.id }) { status ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PriorityDueMemberCard(
                        status = status,
                        collectLabel = strings.collectFee,
                        whatsappLabel = strings.whatsapp,
                        callLabel = strings.call,
                        onOpenFullAlert = { onOpenMemberAlert(status.member) },
                        onCollectFee = { onCollectFeeClick(status) },
                        onWhatsApp = {
                            val msg = CommunicationIntents.buildWhatsAppFeeMessage(
                                memberName = status.member.fullName,
                                amount = status.dueAmount,
                                dueDate = FeeEngine.formatLocalDate(status.dueDate),
                                gymName = data.ownerProfile.gymName,
                                ownerPhone = data.ownerProfile.ownerPhone
                            )
                            CommunicationIntents.openWhatsAppReminder(
                                context = context,
                                phone = status.member.whatsapp.ifBlank { status.member.mobile },
                                message = msg
                            )
                            onWhatsAppSent(status.member, status.dueAmount)
                        },
                        onCall = {
                            CommunicationIntents.dialMember(context, status.member.mobile)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickHeroPill(
    label: String,
    accent: Color,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        color = accent.copy(alpha = 0.22f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .border(1.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = PureWhite,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun PriorityDueMemberCard(
    status: MemberFeeStatus,
    collectLabel: String,
    whatsappLabel: String,
    callLabel: String,
    onOpenFullAlert: () -> Unit,
    onCollectFee: () -> Unit,
    onWhatsApp: () -> Unit,
    onCall: () -> Unit
) {
    val member = status.member
    val daysText = when {
        status.daysRemaining < 0 -> "${-status.daysRemaining}d Overdue"
        status.daysRemaining == 0L -> "Due Today"
        status.daysRemaining == 1L -> "1 Day Remaining"
        else -> "${status.daysRemaining} Days Remaining"
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("due_member_card_${member.membershipId}"),
        highlightAccent = when (status.urgency) {
            FeeUrgency.DUE_TODAY -> StatusDueTodayRed
            FeeUrgency.OVERDUE -> CrimsonPrimary
            FeeUrgency.DUE_SOON -> StatusDueSoonAmber
            FeeUrgency.PAID -> StatusPaidGreen
        },
        onClick = onOpenFullAlert
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MemberAvatar(
                photoWebpBase64 = member.photoWebpBase64,
                fullName = member.fullName,
                size = 56.dp,
                borderColor = when (status.urgency) {
                    FeeUrgency.DUE_TODAY -> StatusDueTodayRed
                    FeeUrgency.OVERDUE -> CrimsonBright
                    FeeUrgency.DUE_SOON -> StatusDueSoonAmber
                    FeeUrgency.PAID -> StatusPaidGreen
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = member.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        color = PureWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    UrgencyBadge(urgency = status.urgency)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${member.membershipId} • ${member.mobile}",
                    style = MaterialTheme.typography.labelMedium,
                    color = CoolSilver
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Due: Rs. ${status.dueAmount}",
                        style = MaterialTheme.typography.titleSmall,
                        color = CrimsonBright,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${FeeEngine.formatLocalDate(status.dueDate)} • $daysText",
                        style = MaterialTheme.typography.labelSmall,
                        color = CoolSilver
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onCollectFee,
                colors = ButtonDefaults.buttonColors(containerColor = StatusPaidGreen, contentColor = ObsidianBlack),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier
                    .weight(1.25f)
                    .height(44.dp)
                    .testTag("collect_fee_btn_${member.membershipId}")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(collectLabel, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }

            OutlinedButton(
                onClick = onWhatsApp,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("whatsapp_btn_${member.membershipId}")
            ) {
                Icon(Icons.Default.Chat, contentDescription = null, tint = StatusPaidGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(whatsappLabel, color = PureWhite, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }

            OutlinedButton(
                onClick = onCall,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier
                    .weight(0.8f)
                    .height(44.dp)
                    .testTag("call_btn_${member.membershipId}")
            ) {
                Icon(Icons.Default.Call, contentDescription = null, tint = StatusInfoBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(callLabel, color = PureWhite, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
    }
}
