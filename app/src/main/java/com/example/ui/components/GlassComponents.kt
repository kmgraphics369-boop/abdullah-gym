package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.FeeUrgency
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CoolSilver
import com.example.ui.theme.CrimsonBright
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StatusDueSoonAmber
import com.example.ui.theme.StatusDueSoonContainer
import com.example.ui.theme.StatusDueTodayContainer
import com.example.ui.theme.StatusDueTodayRed
import com.example.ui.theme.StatusOverdueCharcoal
import com.example.ui.theme.StatusOverdueText
import com.example.ui.theme.StatusPaidContainer
import com.example.ui.theme.StatusPaidGreen
import com.example.util.ImageWebpUtils

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    highlightAccent: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            highlightAccent?.copy(alpha = 0.55f) ?: Color(0x33FFFFFF),
            GlassBorder
        )
    )
    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            CarbonCard.copy(alpha = 0.92f),
            CarbonElevated.copy(alpha = 0.88f)
        )
    )
    Surface(
        modifier = modifier
            .clip(shape)
            .background(bgBrush)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        color = Color.Transparent,
        shape = shape
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            content = content
        )
    }
}

@Composable
fun StatMetricTile(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    GlassCard(
        modifier = modifier,
        highlightAccent = accentColor
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = PureWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = CoolSilver,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MemberAvatar(
    photoWebpBase64: String?,
    fullName: String,
    size: Dp = 52.dp,
    borderColor: Color = CrimsonPrimary
) {
    val imageBitmap = remember(photoWebpBase64) {
        ImageWebpUtils.decodeBase64ToImageBitmap(photoWebpBase64)
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(ObsidianBlack)
            .border(2.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = fullName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.img_athlete_placeholder_1794983436146),
                contentDescription = fullName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBlack.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = fullName.trim().split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("").ifBlank { "GM" },
                    style = MaterialTheme.typography.titleSmall,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun UrgencyBadge(urgency: FeeUrgency, customLabel: String? = null) {
    val (bg, fg, label) = when (urgency) {
        FeeUrgency.PAID -> Triple(StatusPaidContainer, StatusPaidGreen, customLabel ?: "Paid")
        FeeUrgency.DUE_SOON -> Triple(StatusDueSoonContainer, StatusDueSoonAmber, customLabel ?: "Due Soon")
        FeeUrgency.DUE_TODAY -> Triple(StatusDueTodayContainer, StatusDueTodayRed, customLabel ?: "Due Today")
        FeeUrgency.OVERDUE -> Triple(StatusOverdueCharcoal, StatusOverdueText, customLabel ?: "Overdue")
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(fg)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(CrimsonBright)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = PureWhite
            )
        }
        if (actionLabel != null && onAction != null) {
            Surface(
                color = CrimsonContainer,
                shape = RoundedCornerShape(50),
                modifier = Modifier.clickable(onClick = onAction)
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = CrimsonBright,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
