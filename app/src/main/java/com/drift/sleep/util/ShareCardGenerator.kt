package com.drift.sleep.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.drift.sleep.data.SleepRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ShareCardGenerator {

    private const val CARD_WIDTH = 1080
    private const val CARD_HEIGHT = 1440

    fun generateAndShare(context: Context, record: SleepRecord) {
        val bitmap = generateCard(record)
        val file = saveBitmap(context, bitmap)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        shareImage(context, uri)
    }

    private fun generateCard(record: SleepRecord): Bitmap {
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("M月d日 EEEE", Locale.CHINESE)
        val startStr = timeFormat.format(Date(record.startTime))
        val pauseStr = timeFormat.format(Date(record.pauseTime))
        val dateStr = dateFormat.format(Date(record.pauseTime))
        val durationMin = (record.pauseTime - record.startTime) / 60000

        // Background gradient
        val bgPaint = Paint()
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, CARD_HEIGHT.toFloat(),
            intArrayOf(Color.parseColor("#060A10"), Color.parseColor("#0A1628"), Color.parseColor("#060A10")),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(RectF(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat()), 48f, 48f, bgPaint)

        // Title "drift"
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E8ECF2")
            textSize = 64f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            letterSpacing = 0.3f
        }
        canvas.drawText("drift", 80f, 140f, titlePaint)

        // Date
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4A5568")
            textSize = 40f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        }
        canvas.drawText(dateStr, 80f, 200f, datePaint)

        // Divider line
        val divPaint = Paint().apply { color = Color.parseColor("#1A2540"); strokeWidth = 2f }
        canvas.drawLine(80f, 260f, CARD_WIDTH - 80f, 260f, divPaint)

        // Moon emoji circle
        val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4A55A")
            textSize = 120f
        }
        canvas.drawText("\uD83C\uDF19", (CARD_WIDTH / 2 - 60).toFloat(), 440f, moonPaint)

        // Time range - big
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E8ECF2")
            textSize = 96f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("$startStr → $pauseStr", CARD_WIDTH / 2f, 600f, timePaint)

        // Duration
        val durPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4A5568")
            textSize = 44f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("守护了 ${durationMin} 分钟", CARD_WIDTH / 2f, 680f, durPaint)

        // Paused app
        if (record.pausedApp != null) {
            val appPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#D4A55A")
                textSize = 48f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("暂停了 ${record.pausedApp}", CARD_WIDTH / 2f, 800f, appPaint)
        }

        // Bottom branding
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2A3550")
            textSize = 36f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Drift · 睡着自动暂停", CARD_WIDTH / 2f, (CARD_HEIGHT - 80).toFloat(), brandPaint)

        return bitmap
    }

    private fun saveBitmap(context: Context, bitmap: Bitmap): File {
        val dir = File(context.cacheDir, "share")
        dir.mkdirs()
        val file = File(dir, "drift_sleep_report.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }

    private fun shareImage(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "分享睡眠报告").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
