package com.apix.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apix.app.ui.theme.Gold

@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                text = "الإعدادات", // من الأفضل تعريبها بما أن الواجهة RTL
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(Modifier.height(24.dp))

            // Dark Mode toggle (تم جعل الصف بالكامل قابلاً للنقر)
            SettingsRow(
                title = "الوضع الداكن",
                onClick = { onToggleDarkMode(!isDarkMode) },
                trailing = {
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = null, // ألغينا النقر المباشر على الزر ليتحكم به الصف
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Gold,
                            checkedTrackColor = Gold.copy(alpha = 0.5f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF333333)
                        )
                    )
                }
            )

            Spacer(Modifier.height(12.dp))

            // Telegram Channel
            SettingsRow(
                title = "قناة التليجرام",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/apix_tv"))
                    context.startActivity(intent)
                },
                trailing = {
                    // استخدمنا ArrowBack العادي لأنه في وضع RTL سيشير لليسار تلقائياً
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = null,
                        tint = Gold
                    )
                }
            )

            Spacer(Modifier.height(12.dp))

            // Contact Us
            SettingsRow(
                title = "تواصل معنا",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/apix_support"))
                    context.startActivity(intent)
                },
                trailing = {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = null,
                        tint = Gold
                    )
                }
            )
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) Gold else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ) else Modifier
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // تم تعديل الترتيب: النص أولاً، ثم مسافة مرنة، ثم الزر/الأيقونة
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.weight(1f)) // هذا السطر يدفع الأيقونة إلى أقصى اليسار
        trailing()
    }
}
