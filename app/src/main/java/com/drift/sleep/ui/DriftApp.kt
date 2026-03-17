package com.drift.sleep.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drift.sleep.data.SleepRecord
import com.drift.sleep.ui.theme.Amber
import com.drift.sleep.ui.theme.AmberGlow
import com.drift.sleep.ui.theme.DeepSea
import com.drift.sleep.ui.theme.Mist
import com.drift.sleep.ui.theme.Moonlight
import com.drift.sleep.ui.theme.Slate
import com.drift.sleep.ui.theme.SoftRed
import com.drift.sleep.ui.theme.Void

enum class Page { Home, History, Settings }

@Composable
fun DriftApp(
    currentPage: Page,
    onPageChange: (Page) -> Unit,
    isRunning: Boolean,
    hasNotificationAccess: Boolean,
    hasNotificationPermission: Boolean,
    onToggle: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    waitMinutes: Long,
    fadeMinutes: Long,
    onWaitChange: (Long) -> Unit,
    onFadeChange: (Long) -> Unit,
    autoStartEnabled: Boolean,
    autoStartHour: Int,
    autoStartMinute: Int,
    onAutoStartToggle: (Boolean) -> Unit,
    onAutoStartTimeChange: (Int, Int) -> Unit,
    isBatteryOptimized: Boolean,
    onRequestBatteryOptimization: () -> Unit,
    batteryTip: String,
    records: List<SleepRecord>,
    onShareRecord: (SleepRecord) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Void)
    ) {
        // Shared ambient background
        AmbientBackground(isRunning = isRunning && currentPage == Page.Home)

        // Page content
        when (currentPage) {
            Page.Home -> HomeScreen(
                isRunning = isRunning,
                hasNotificationAccess = hasNotificationAccess,
                hasNotificationPermission = hasNotificationPermission,
                onToggle = onToggle,
                onRequestNotificationAccess = onRequestNotificationAccess,
                onRequestNotificationPermission = onRequestNotificationPermission
            )
            Page.History -> HistoryScreen(records = records, onShareRecord = onShareRecord)
            Page.Settings -> SettingsScreen(
                waitMinutes = waitMinutes,
                fadeMinutes = fadeMinutes,
                onWaitChange = onWaitChange,
                onFadeChange = onFadeChange,
                autoStartEnabled = autoStartEnabled,
                autoStartHour = autoStartHour,
                autoStartMinute = autoStartMinute,
                onAutoStartToggle = onAutoStartToggle,
                onAutoStartTimeChange = onAutoStartTimeChange,
                isBatteryOptimized = isBatteryOptimized,
                onRequestBatteryOptimization = onRequestBatteryOptimization,
                batteryTip = batteryTip
            )
        }

        // Bottom navigation
        BottomNavBar(
            currentPage = currentPage,
            onPageChange = onPageChange,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// --- Home Screen ---

@Composable
private fun HomeScreen(
    isRunning: Boolean,
    hasNotificationAccess: Boolean,
    hasNotificationPermission: Boolean,
    onToggle: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Permission cards
        if (!hasNotificationPermission) {
            PermissionCard(
                label = "通知权限",
                description = "显示运行状态",
                actionText = "授权",
                onClick = onRequestNotificationPermission
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (!hasNotificationAccess) {
            PermissionCard(
                label = "通知访问",
                description = "暂停其他App的播放",
                actionText = "去设置",
                onClick = onRequestNotificationAccess
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Title
        Text(
            text = "drift",
            color = if (isRunning) Moonlight.copy(alpha = 0.5f) else Moonlight,
            fontSize = 20.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 8.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        val statusText = when {
            !hasNotificationAccess -> "需要权限设置"
            isRunning -> "正在守护你的睡眠"
            else -> "安心入睡"
        }
        Text(
            text = statusText,
            color = Mist,
            fontSize = 13.sp,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.weight(0.8f))

        // Main button
        DriftButton(
            isRunning = isRunning,
            enabled = hasNotificationAccess,
            onClick = onToggle
        )

        Spacer(modifier = Modifier.weight(0.8f))

        // Hint
        val hintText = when {
            isRunning -> "切到你想听的App，安心入睡\n摇一摇可以恢复音量"
            !hasNotificationAccess -> ""
            else -> "点击开始，然后去听你喜欢的内容\n睡着后会自动帮你暂停"
        }
        if (hintText.isNotEmpty()) {
            Text(
                text = hintText,
                color = Mist.copy(alpha = 0.7f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Light
            )
        }

        Spacer(modifier = Modifier.weight(0.5f))
        // Space for bottom nav
        Spacer(modifier = Modifier.height(72.dp))
    }
}

// --- Ambient Background ---

@Composable
private fun AmbientBackground(isRunning: Boolean) {
    val ambientAlpha by animateFloatAsState(
        targetValue = if (isRunning) 0.15f else 0.06f,
        animationSpec = tween(2000),
        label = "ambientAlpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    if (isRunning) Amber.copy(alpha = ambientAlpha) else Color(0xFF1E3A5F).copy(alpha = ambientAlpha),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h * (0.38f + drift * 0.04f)),
                radius = w * 0.8f
            ),
            center = Offset(w * 0.5f, h * (0.38f + drift * 0.04f)),
            radius = w * 0.8f
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(DeepSea.copy(alpha = 0.3f), Color.Transparent),
                center = Offset(w * 0.3f, h * 0.85f),
                radius = w * 0.5f
            ),
            center = Offset(w * 0.3f, h * 0.85f),
            radius = w * 0.5f
        )
    }
}

// --- Drift Button ---

@Composable
private fun DriftButton(isRunning: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
    val breathe by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    val glowAlpha = if (isRunning) 0.2f + breathe * 0.3f else 0f
    val glowScale = if (isRunning) 1.15f + breathe * 0.15f else 1f

    val buttonColor by animateColorAsState(
        targetValue = when {
            !enabled -> Slate.copy(alpha = 0.5f)
            isRunning -> SoftRed
            else -> Amber
        },
        animationSpec = tween(600),
        label = "buttonColor"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isRunning) 0.92f else 1f,
        animationSpec = tween(600),
        label = "buttonScale"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp)) {
        // Breathing glow — use Canvas radialGradient for smooth circular falloff
        if (isRunning) {
            Canvas(
                modifier = Modifier
                    .size(280.dp)
                    .scale(glowScale)
                    .alpha(glowAlpha)
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Amber.copy(alpha = 0.6f),
                            Amber.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension / 2f
                    ),
                    radius = size.minDimension / 2f
                )
            }
        }

        // Subtle outer ring
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .scale(buttonScale)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        buttonColor.copy(alpha = 0.08f),
                        buttonColor.copy(alpha = 0.02f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension / 2f
                ),
                radius = size.minDimension / 2f
            )
        }

        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(buttonScale)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(buttonColor, buttonColor.copy(alpha = 0.75f))
                    )
                )
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isRunning) "停止" else "开始",
                color = if (isRunning) Color.White.copy(alpha = 0.9f) else Void,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 4.sp
            )
        }
    }
}

// --- Bottom Nav ---

@Composable
private fun BottomNavBar(
    currentPage: Page,
    onPageChange: (Page) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Void.copy(alpha = 0.95f), Void),
                    startY = 0f,
                    endY = 80f
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 40.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                icon = Icons.Rounded.NightsStay,
                label = "首页",
                selected = currentPage == Page.Home,
                onClick = { onPageChange(Page.Home) }
            )
            NavItem(
                icon = Icons.Rounded.History,
                label = "记录",
                selected = currentPage == Page.History,
                onClick = { onPageChange(Page.History) }
            )
            NavItem(
                icon = Icons.Rounded.Settings,
                label = "设置",
                selected = currentPage == Page.Settings,
                onClick = { onPageChange(Page.Settings) }
            )
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color by animateColorAsState(
        targetValue = if (selected) Amber else Mist.copy(alpha = 0.6f),
        animationSpec = tween(200),
        label = "navColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

// --- Permission Card ---

@Composable
private fun PermissionCard(
    label: String,
    description: String,
    actionText: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = Slate.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = label, color = Moonlight, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = description, color = Mist, fontSize = 12.sp, fontWeight = FontWeight.Light)
            }
            Text(text = actionText, color = Amber, fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
        }
    }
}
