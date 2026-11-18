package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.FeeUrgency
import com.example.data.model.Member
import com.example.data.model.SmartNotificationItem
import com.example.ui.components.GlassCard
import com.example.ui.components.MemberAvatar
import com.example.ui.components.UrgencyBadge
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CoolSilver
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StatusDueSoonAmber
import com.example.ui.theme.StatusDueTodayRed
import com.example.ui.theme.StatusOverdueText
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.viewmodel.GymAppData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    data: GymAppData,
    onBack: () -> Unit,
    onOpenMemberAlert: (Member) -> Unit
) {
    BackHandler { onBack() }
    var filter by remember { mutableStateOf<FeeUrgency?>(null) }

    val filtered = remember(data.dashboard.notifications, filter) {
        if (filter == null) data.dashboard.notifications
        else data.dashboard.notifications.filter { it.urgency == filter }
    }

    Scaffold(
        containerColor = ObsidianBlack,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBlack, titleContentColor = PureWhite),
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("notif_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
                    }
                },
                title = {
                    Column {
                        Text("Smart Notifications (${data.dashboard.alertCount})", style = MaterialTheme.typography.headlineMedium)
                        Text("Tap any member alert for full-screen profile", style = MaterialTheme.typography.bodySmall, color = CoolSilver)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    null to "All",
                    FeeUrgency.DUE_TODAY to "Due Today",
                    FeeUrgency.OVERDUE to "Overdue",
                    FeeUrgency.DUE_SOON to "Due Soon",
                    FeeUrgency.PAID to "Paid"
                ).forEach { (urg, label) ->
                    FilterChip(
                        selected = filter == urg,
                        onClick = { filter = urg },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CrimsonPrimary,
                            selectedLabelColor = PureWhite,
                            containerColor = CarbonElevated,
                            labelColor = CoolSilver
                        )
                    )
                }
            }

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = CoolSilver, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No notifications in this category", color = CoolSilver)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { item ->
                        NotificationRowCard(
                            item = item,
                            onClick = {
                                item.member?.let(onOpenMemberAlert)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRowCard(
    item: SmartNotificationItem,
    onClick: () -> Unit
) {
    val accent = when (item.urgency) {
        FeeUrgency.PAID -> StatusPaidGreen
        FeeUrgency.DUE_SOON -> StatusDueSoonAmber
        FeeUrgency.DUE_TODAY -> StatusDueTodayRed
        FeeUrgency.OVERDUE -> StatusOverdueText
    }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notif_item_${item.id}"),
        highlightAccent = accent,
        onClick = if (item.member != null) onClick else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.member != null) {
                MemberAvatar(
                    photoWebpBase64 = item.member.photoWebpBase64,
                    fullName = item.member.fullName,
                    size = 52.dp,
                    borderColor = accent
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Build, contentDescription = "Equipment Alert", tint = accent)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                Spacer(modifier = Modifier.height(2.dp))
                Text(item.subtitle, style = MaterialTheme.typography.bodyMedium, color = accent)
            }
            UrgencyBadge(urgency = item.urgency)
        }
    }
}
