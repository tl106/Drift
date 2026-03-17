package com.drift.sleep.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drift.sleep.data.SleepRecord
import com.drift.sleep.ui.theme.Amber
import com.drift.sleep.ui.theme.Mist
import com.drift.sleep.ui.theme.Moonlight
import com.drift.sleep.ui.theme.Slate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    records: List<SleepRecord>,
    onShareRecord: ((SleepRecord) -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 48.dp)
    ) {
        Text(
            text = "记录",
            color = Moonlight,
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "还没有记录",
                        color = Mist,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Light
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "完成一次检测后这里会显示记录",
                        color = Mist.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            val grouped = remember(records) {
                records.groupBy { record ->
                    SimpleDateFormat("M月d日", Locale.CHINESE).format(Date(record.pauseTime))
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                grouped.forEach { (date, dayRecords) ->
                    item {
                        Text(
                            text = date,
                            color = Mist,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    items(dayRecords) { record ->
                        RecordCard(record = record, onShare = onShareRecord)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun RecordCard(record: SleepRecord, onShare: ((SleepRecord) -> Unit)? = null) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val startStr = timeFormat.format(Date(record.startTime))
    val pauseStr = timeFormat.format(Date(record.pauseTime))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Slate.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Amber dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Amber, CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$startStr → $pauseStr",
                    color = Moonlight,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
                if (record.pausedApp != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "暂停了 ${record.pausedApp}",
                        color = Mist,
                        fontSize = 13.sp
                    )
                }
            }

            if (onShare != null) {
                IconButton(onClick = { onShare(record) }) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "分享",
                        tint = Mist,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
