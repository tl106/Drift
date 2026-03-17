package com.drift.sleep.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drift.sleep.ui.theme.Amber
import com.drift.sleep.ui.theme.DeepSea
import com.drift.sleep.ui.theme.Mist
import com.drift.sleep.ui.theme.Moonlight
import com.drift.sleep.ui.theme.Slate
import com.drift.sleep.ui.theme.Void

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
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
    batteryTip: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 48.dp)
    ) {
        Text(
            text = "设置",
            color = Moonlight,
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // --- Timer Settings ---
        SectionLabel("等待时间")
        Spacer(modifier = Modifier.height(6.dp))
        SectionHint("开始监听后，等待此时间无活动则开始降低音量")
        Spacer(modifier = Modifier.height(14.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(15L, 20L, 25L, 30L, 40L).forEach { minutes ->
                ChipButton(
                    text = "${minutes} 分钟",
                    selected = waitMinutes == minutes,
                    onClick = { onWaitChange(minutes) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        SectionLabel("渐弱时间")
        Spacer(modifier = Modifier.height(6.dp))
        SectionHint("音量从当前值降到零所需的时间")
        Spacer(modifier = Modifier.height(14.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(5L, 10L, 15L, 20L).forEach { minutes ->
                ChipButton(
                    text = "${minutes} 分钟",
                    selected = fadeMinutes == minutes,
                    onClick = { onFadeChange(minutes) }
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // --- Auto Start ---
        SectionLabel("自动启动")
        Spacer(modifier = Modifier.height(6.dp))
        SectionHint("每天定时自动开始检测，无需手动打开App")
        Spacer(modifier = Modifier.height(14.dp))

        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (autoStartEnabled) "每晚 ${autoStartHour}:${autoStartMinute.toString().padStart(2, '0')} 自动启动" else "已关闭",
                        color = Moonlight,
                        fontSize = 14.sp
                    )
                }
                Switch(
                    checked = autoStartEnabled,
                    onCheckedChange = onAutoStartToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Amber,
                        checkedTrackColor = Amber.copy(alpha = 0.3f),
                        uncheckedThumbColor = Mist,
                        uncheckedTrackColor = Slate
                    )
                )
            }

            if (autoStartEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(21 to 30, 22 to 0, 22 to 30, 23 to 0, 23 to 30).forEach { (h, m) ->
                        val timeStr = "${h}:${m.toString().padStart(2, '0')}"
                        ChipButton(
                            text = timeStr,
                            selected = autoStartHour == h && autoStartMinute == m,
                            onClick = { onAutoStartTimeChange(h, m) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- Battery Optimization ---
        SectionLabel("后台运行")
        Spacer(modifier = Modifier.height(6.dp))
        SectionHint("关闭电池优化，防止系统杀掉Drift后台服务")
        Spacer(modifier = Modifier.height(14.dp))

        SettingsCard(onClick = if (!isBatteryOptimized) onRequestBatteryOptimization else null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBatteryOptimized) "已关闭电池优化 ✓" else "需要关闭电池优化",
                        color = if (isBatteryOptimized) Amber else Moonlight,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = batteryTip,
                        color = Mist,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
                if (!isBatteryOptimized) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "去设置",
                        color = Amber,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(56.dp))

        // App info
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Drift v1.0", color = Mist.copy(alpha = 0.5f), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "纯本地运算 · 不上传数据 · 不录音",
                color = Mist.copy(alpha = 0.4f),
                fontSize = 11.sp
            )
        }

        // Bottom nav space
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Moonlight,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun SectionHint(text: String) {
    Text(
        text = text,
        color = Mist,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
}

@Composable
private fun SettingsCard(
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Slate.copy(alpha = 0.25f))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Column { content() }
    }
}

@Composable
private fun ChipButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Amber else Slate.copy(alpha = 0.4f))
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Void else Moonlight,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}
