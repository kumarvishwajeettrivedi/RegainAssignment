package com.example.regainassignment.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap

@Composable
fun AppIcon(
    packageName: String,
    appName: String,
    size: Dp = 40.dp
) {
    val context = LocalContext.current
    val appIcon: Drawable? = remember(packageName) {
        try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }
    
    if (appIcon != null) {
        // Convert to bitmap with proper size to avoid blurriness
        val bitmapSize = (size.value * 2).toInt() // 2x for better quality
        Image(
            bitmap = appIcon.toBitmap(bitmapSize, bitmapSize).asImageBitmap(),
            contentDescription = appName,
            modifier = Modifier
                .size(size)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
        )
    } else {
        // Fallback: Show first letter in a circle
        Box(
            modifier = Modifier
                .size(size)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = appName.take(1).uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = (size.value / 2).sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
