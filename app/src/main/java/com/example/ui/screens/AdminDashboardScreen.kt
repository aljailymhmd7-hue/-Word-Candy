package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.analytics.AdminAnalyticsReport
import com.example.analytics.AdminDateRange
import com.example.analytics.FirebaseAnalyticsDataClient
import com.example.analytics.Ga4PropertySummary
import com.example.ui.theme.CandyBlue
import com.example.ui.theme.CandyGreen
import com.example.ui.theme.CandyOrange
import com.example.ui.theme.CandyPinkRed
import com.example.ui.theme.CandyPurple
import com.example.ui.theme.NunitoFontFamily
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import java.text.NumberFormat
import java.util.Locale

data class AdminDashboardUiState(
    val isAuthorizing: Boolean = false,
    val isLoading: Boolean = false,
    val isAuthorized: Boolean = false,
    val propertyIdInput: String = "",
    val availableProperties: List<Ga4PropertySummary> = emptyList(),
    val selectedDateRange: AdminDateRange = AdminDateRange.LAST_30_DAYS,
    val report: AdminAnalyticsReport? = null,
    val errorMessage: String? = null
)

@Composable
fun AdminDashboardScreen(
    state: AdminDashboardUiState,
    onAuthorizingChanged: (Boolean) -> Unit,
    onAccessTokenReceived: (String) -> Unit,
    onAuthError: (String) -> Unit,
    onPropertyIdChanged: (String) -> Unit,
    onSelectProperty: (Ga4PropertySummary) -> Unit,
    onSelectDateRange: (AdminDateRange) -> Unit,
    onFetchReport: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)

    val context = LocalContext.current
    val activity = context as? Activity
    val numberFmt = remember { NumberFormat.getIntegerInstance(Locale.US) }
    var lastManualError by remember { mutableStateOf<String?>(null) }

    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        onAuthorizingChanged(false)
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            runCatching {
                val authResult = Identity.getAuthorizationClient(context)
                    .getAuthorizationResultFromIntent(result.data)
                val token = authResult.accessToken
                if (!token.isNullOrBlank()) {
                    lastManualError = null
                    onAccessTokenReceived(token)
                } else {
                    onAuthError("No Google Analytics OAuth access token returned.")
                }
            }.onFailure { err ->
                onAuthError(err.message ?: "Failed to read Google Analytics authorization.")
            }
        } else {
            onAuthError("Google Analytics sign-in was cancelled.")
        }
    }

    val startGoogleAnalyticsAuth: () -> Unit = {
        if (activity == null) {
            onAuthError("Activity context unavailable for Google OAuth.")
        } else {
            onAuthorizingChanged(true)
            lastManualError = null
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(FirebaseAnalyticsDataClient.ANALYTICS_READONLY_SCOPE)))
                .build()
            Identity.getAuthorizationClient(activity)
                .authorize(request)
                .addOnSuccessListener { authResult ->
                    if (authResult.hasResolution()) {
                        val pendingIntent = authResult.pendingIntent
                        if (pendingIntent != null) {
                            authLauncher.launch(
                                IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                            )
                        } else {
                            onAuthorizingChanged(false)
                            onAuthError("Missing Google OAuth resolution prompt.")
                        }
                    } else {
                        onAuthorizingChanged(false)
                        val token = authResult.accessToken
                        if (!token.isNullOrBlank()) {
                            onAccessTokenReceived(token)
                        } else {
                            onAuthError("Google Analytics access token unavailable.")
                        }
                    }
                }
                .addOnFailureListener { err ->
                    onAuthorizingChanged(false)
                    onAuthError(err.message ?: "Google Play Services OAuth authorization failed.")
                }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF14092B),
                        Color(0xFF1E103F),
                        Color(0xFF120726)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .testTag("admin_back_button")
                        ) {
                            @Suppress("DEPRECATION")
                            Icon(
                                imageVector = Icons.Rounded.ArrowBack,
                                contentDescription = "Back to Settings",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = CandyOrange,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "ADMIN CONSOLE",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.2.sp,
                                    color = CandyOrange
                                )
                            }
                            Text(
                                text = "Firebase Analytics",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = Color.White
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (state.isAuthorized) onFetchReport() else startGoogleAnalyticsAuth()
                        },
                        enabled = !state.isLoading && !state.isAuthorizing,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(CircleShape)
                            .background(CandyPurple.copy(alpha = 0.25f))
                            .testTag("admin_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh Firebase Analytics",
                            tint = Color.White
                        )
                    }
                }

                // Connection & Property Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF231347))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (state.isAuthorized) {
                                    "Google Analytics API Connected"
                                } else {
                                    "Analytics API Authorization Required"
                                },
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (state.isAuthorized) CandyGreen else Color.White
                            )
                            Text(
                                text = "Project: gen-lang-client-0270070827",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }

                        Button(
                            onClick = startGoogleAnalyticsAuth,
                            enabled = !state.isAuthorizing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.isAuthorized) Color(0xFF332063) else CandyBlue
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("admin_connect_analytics_button")
                        ) {
                            if (state.isAuthorizing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text(
                                    text = if (state.isAuthorized) "Reconnect" else "Connect",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    if (state.availableProperties.isNotEmpty()) {
                        Text(
                            text = "Discovered GA4 Properties",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.availableProperties.forEach { prop ->
                                val selected = state.propertyIdInput == prop.propertyId
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (selected) CandyPurple.copy(alpha = 0.35f)
                                            else Color.White.copy(alpha = 0.08f)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (selected) CandyPurple else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onSelectProperty(prop) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "${prop.displayName} (${prop.propertyId})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = state.propertyIdInput,
                            onValueChange = onPropertyIdChanged,
                            label = { Text("GA4 Property ID") },
                            placeholder = { Text("e.g. 123456789") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CandyPurple,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                focusedLabelColor = CandyPurple,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.65f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_property_id_input")
                        )

                        Button(
                            onClick = {
                                if (!state.isAuthorized) {
                                    startGoogleAnalyticsAuth()
                                } else if (state.propertyIdInput.isBlank()) {
                                    lastManualError = "Enter your GA4 Property ID to fetch analytics."
                                } else {
                                    lastManualError = null
                                    onFetchReport()
                                }
                            },
                            enabled = !state.isLoading && !state.isAuthorizing,
                            colors = ButtonDefaults.buttonColors(containerColor = CandyOrange),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("admin_fetch_button")
                        ) {
                            Text(
                                text = "Fetch",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF210E3D)
                            )
                        }
                    }

                    // Date Range Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AdminDateRange.entries.forEach { range ->
                            val selected = state.selectedDateRange == range
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selected) Color.White else Color.White.copy(alpha = 0.08f)
                                    )
                                    .clickable { onSelectDateRange(range) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = range.label,
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = if (selected) Color(0xFF1A0D35) else Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }

                val activeError = lastManualError ?: state.errorMessage
                if (!activeError.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CandyPinkRed.copy(alpha = 0.16f))
                            .border(1.dp, CandyPinkRed.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeError,
                            fontSize = 13.sp,
                            color = Color(0xFFFFD6DD),
                            modifier = Modifier.testTag("admin_error_text")
                        )
                    }
                }

                if (state.isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = CandyOrange,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Fetching live Firebase Analytics data...",
                            color = Color.White.copy(alpha = 0.85f),
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                val report = state.report

                // 2x2 KPI Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdminMetricCard(
                        title = "TOTAL INSTALLS",
                        value = report?.let { numberFmt.format(it.totalInstalls) } ?: "—",
                        subtitle = report?.let {
                            "first_open • app_installed: ${numberFmt.format(it.customAppInstalledCount)}"
                        } ?: "Source: first_open event",
                        accentColor = CandyGreen,
                        icon = Icons.Rounded.DownloadDone,
                        testTag = "admin_metric_installs",
                        modifier = Modifier.weight(1f)
                    )
                    AdminMetricCard(
                        title = "TOTAL UNINSTALLS",
                        value = report?.let { numberFmt.format(it.totalUninstalls) } ?: "—",
                        subtitle = "Source: app_remove event",
                        accentColor = CandyPinkRed,
                        icon = Icons.Rounded.DeleteSweep,
                        testTag = "admin_metric_uninstalls",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdminMetricCard(
                        title = "RETENTION RATE",
                        value = report?.retentionRatePercent?.let { String.format(Locale.US, "%.1f%%", it) }
                            ?: "—",
                        subtitle = report?.dauPerMauPercent?.let {
                            String.format(
                                Locale.US,
                                "DAU/MAU %.1f%% • Active %s",
                                it,
                                numberFmt.format(report.activeUsers)
                            )
                        } ?: "Net retained installs",
                        accentColor = CandyBlue,
                        icon = Icons.Rounded.Repeat,
                        testTag = "admin_metric_retention",
                        modifier = Modifier.weight(1f)
                    )
                    AdminMetricCard(
                        title = "MOST DOWNLOADED",
                        value = report?.mostDownloadedLevel?.levelName ?: "—",
                        subtitle = report?.mostDownloadedLevel?.let {
                            "${numberFmt.format(it.downloadCount)} downloads (total ${numberFmt.format(report.totalLevelDownloads)})"
                        } ?: if (report != null) {
                            "${numberFmt.format(report.totalLevelDownloads)} level_download events"
                        } else {
                            "Source: level_download"
                        },
                        accentColor = CandyOrange,
                        icon = Icons.Rounded.CloudDownload,
                        testTag = "admin_metric_top_level",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Ranked Level Downloads Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF231347))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = CandyOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TOP DOWNLOADED LEVELS",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }

                    if (report == null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Analytics,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.45f),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Connect Google Analytics and select your GA4 Property ID to load live level download rankings.",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    } else if (report.topDownloadedLevels.isEmpty()) {
                        Text(
                            text = report.customDimensionNote
                                ?: "No level_download custom dimension data returned for this date range yet.",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    } else {
                        val maxCount = report.topDownloadedLevels.maxOf { it.downloadCount }.coerceAtLeast(1L)
                        report.topDownloadedLevels.forEachIndexed { index, entry ->
                            val fraction = (entry.downloadCount.toFloat() / maxCount.toFloat()).coerceIn(0.08f, 1f)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "#${index + 1}  ${entry.levelName}",
                                        fontFamily = NunitoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${numberFmt.format(entry.downloadCount)} downloads",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CandyOrange
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White.copy(alpha = 0.1f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(CandyOrange, CandyPinkRed)
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AdminMetricCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    icon: ImageVector,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF231347))
            .border(1.5.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                letterSpacing = 0.6.sp,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Text(
            text = value,
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag(testTag)
        )

        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.68f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
