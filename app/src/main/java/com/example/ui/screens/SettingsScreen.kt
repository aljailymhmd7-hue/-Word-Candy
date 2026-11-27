package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.audio.LocalSoundEffects
import com.example.model.AppLanguage
import com.example.model.GameProgress
import com.example.model.LocalizedStrings
import com.example.ui.components.LanguageTogglePill
import com.example.ui.components.TopResourceBar
import com.example.ui.theme.CandyOrange
import com.example.ui.theme.CandyPinkRed
import com.example.ui.theme.CandyPurple
import com.example.ui.theme.SurfaceDarkPurple

@Composable
fun SettingsScreen(
    progress: GameProgress,
    onToggleLanguage: () -> Unit,
    onSetSoundEnabled: (Boolean) -> Unit,
    onSetHapticsEnabled: (Boolean) -> Unit,
    onResetProgress: () -> Unit,
    onOpenAdminDashboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lang = progress.language
    val sfx = LocalSoundEffects.current
    var showResetDialog by remember { mutableStateOf(false) }
    var versionTapCount by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopResourceBar(
            coins = progress.coins,
            hints = progress.hints,
            language = lang,
            onToggleLanguage = onToggleLanguage
        )

        Column(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = LocalizedStrings.settingsTitle(lang),
                style = MaterialTheme.typography.displayMedium,
                color = Color.White
            )

            // Language Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = null,
                        tint = CandyPurple,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = LocalizedStrings.languageLabel(lang),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SurfaceDarkPurple
                    )
                }
                LanguageTogglePill(language = lang, onToggle = onToggleLanguage)
            }

            // Sound Effects Toggle
            SettingSwitchCard(
                icon = Icons.Rounded.VolumeUp,
                iconTint = CandyOrange,
                label = LocalizedStrings.soundLabel(lang),
                checked = progress.soundEnabled,
                onCheckedChange = onSetSoundEnabled,
                testTag = "settings_sound_switch"
            )

            // Haptics Toggle
            SettingSwitchCard(
                icon = Icons.Rounded.Vibration,
                iconTint = CandyPinkRed,
                label = LocalizedStrings.hapticsLabel(lang),
                checked = progress.hapticsEnabled,
                onCheckedChange = onSetHapticsEnabled,
                testTag = "settings_haptics_switch"
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Reset Progress
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(4.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .clickable {
                        sfx?.playClick()
                        showResetDialog = true
                    }
                    .padding(horizontal = 16.dp)
                    .testTag("settings_reset_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteForever,
                    contentDescription = null,
                    tint = CandyPinkRed,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = LocalizedStrings.resetProgress(lang),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = CandyPinkRed
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        val next = versionTapCount + 1
                        if (next >= 5) {
                            versionTapCount = 0
                            sfx?.playClick()
                            onOpenAdminDashboard()
                        } else {
                            versionTapCount = next
                        }
                    }
                    .padding(vertical = 12.dp)
                    .testTag("settings_version_admin_trigger")
            )
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(LocalizedStrings.resetConfirmTitle(lang)) },
            text = { Text(LocalizedStrings.resetConfirmBody(lang)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onResetProgress()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = CandyPinkRed)
                ) {
                    Text(LocalizedStrings.confirm(lang))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    sfx?.playClick()
                    showResetDialog = false
                }) {
                    Text(LocalizedStrings.cancel(lang))
                }
            }
        )
    }
}

@Composable
private fun SettingSwitchCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = SurfaceDarkPurple
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CandyPurple
            )
        )
    }
}
