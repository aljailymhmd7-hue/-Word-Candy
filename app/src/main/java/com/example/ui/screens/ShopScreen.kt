package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.UnityAdsStatus
import com.example.audio.LocalSoundEffects
import com.example.model.AppLanguage
import com.example.model.BoardTheme
import com.example.model.GameProgress
import com.example.model.LocalizedStrings
import com.example.ui.components.TopResourceBar
import com.example.ui.theme.CandyBlue
import com.example.ui.theme.CandyGold
import com.example.ui.theme.CandyGreenDark
import com.example.ui.theme.CandyOrange
import com.example.ui.theme.CandyPinkRed
import com.example.ui.theme.CandyPurple
import com.example.ui.theme.CandyYellow
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.SurfaceDarkPurple
import com.example.ui.theme.style

@Composable
fun ShopScreen(
    progress: GameProgress,
    adsStatus: UnityAdsStatus,
    onWatchRewardedAd: () -> Unit,
    onBuyHints: (amount: Int, cost: Int) -> Unit,
    onBuyOrEquipTheme: (BoardTheme) -> Unit,
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = progress.language
    val sfx = LocalSoundEffects.current
    var pendingPurchaseTheme by remember { mutableStateOf<BoardTheme?>(null) }
    val ownedThemeCount = BoardTheme.entries.count { it.id in progress.ownedBoardThemes }

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
            // Candy Shop Header Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(SurfaceDarkPurple, Color(0xFF53259B))
                        )
                    )
                    .border(2.dp, CandyYellow.copy(alpha = 0.85f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag("candy_shop_banner"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(CandyOrange, CandyPinkRed))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Storefront,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = LocalizedStrings.shopTitle(lang),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = LocalizedStrings.themesUnlockedSummary(
                                lang = lang,
                                owned = ownedThemeCount,
                                total = BoardTheme.entries.size
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = CandyYellow
                        )
                    }
                }
                MiniGridPreview(
                    theme = progress.activeBoardTheme,
                    size = 52.dp
                )
            }

            // Unity Rewarded Video Ad Card
            RewardedAdShopCard(
                lang = lang,
                adsStatus = adsStatus,
                onClick = onWatchRewardedAd
            )

            // Hint Packs
            ShopPackCard(
                title = LocalizedStrings.hintPack3(lang),
                subtitle = if (lang == AppLanguage.EN) "Reveal starting letters fast" else "Revelez les premieres lettres",
                priceCoins = 75,
                badgeColor = listOf(CandyGold, CandyOrange),
                icon = Icons.Rounded.Lightbulb,
                onClick = { onBuyHints(3, 75) },
                testTag = "shop_buy_3_hints"
            )

            ShopPackCard(
                title = LocalizedStrings.hintPack10(lang),
                subtitle = if (lang == AppLanguage.EN) "Best value bundle (save 50 coins!)" else "Meilleure offre (economisez 50 !)",
                priceCoins = 200,
                badgeColor = listOf(CandyOrange, CandyPinkRed),
                icon = Icons.Rounded.AutoAwesome,
                onClick = { onBuyHints(10, 200) },
                testTag = "shop_buy_10_hints"
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = LocalizedStrings.boardThemesTitle(lang),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )

            BoardTheme.entries.forEach { theme ->
                val isOwned = theme.id in progress.ownedBoardThemes
                val isEquipped = progress.activeBoardTheme == theme
                ThemeShopCard(
                    theme = theme,
                    lang = lang,
                    isOwned = isOwned,
                    isEquipped = isEquipped,
                    canAfford = progress.coins >= theme.priceCoins,
                    onClick = {
                        if (isOwned) {
                            onBuyOrEquipTheme(theme)
                        } else if (progress.coins >= theme.priceCoins) {
                            sfx?.playClick()
                            pendingPurchaseTheme = theme
                        } else {
                            onBuyOrEquipTheme(theme)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    val confirmTheme = pendingPurchaseTheme
    if (confirmTheme != null) {
        val themeTitle = if (lang == AppLanguage.EN) confirmTheme.titleEn else confirmTheme.titleFr
        AlertDialog(
            onDismissRequest = {
                sfx?.playClick()
                pendingPurchaseTheme = null
            },
            modifier = Modifier.testTag("shop_unlock_theme_dialog"),
            containerColor = Color.White,
            title = {
                Text(
                    text = LocalizedStrings.unlockThemeTitle(lang, themeTitle),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SurfaceDarkPurple
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MiniGridPreview(theme = confirmTheme, size = 104.dp)
                    Text(
                        text = LocalizedStrings.unlockThemeBody(lang, confirmTheme.priceCoins),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SurfaceDarkPurple,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingPurchaseTheme = null
                        onBuyOrEquipTheme(confirmTheme)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = CandyPinkRed),
                    modifier = Modifier.testTag("shop_confirm_unlock_button")
                ) {
                    Text(
                        text = "${LocalizedStrings.unlockButton(lang)} (${confirmTheme.priceCoins})",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        sfx?.playClick()
                        pendingPurchaseTheme = null
                    }
                ) {
                    Text(LocalizedStrings.cancel(lang))
                }
            }
        )
    }
}

@Composable
private fun MiniGridPreview(
    theme: BoardTheme,
    size: Dp
) {
    val themeStyle = remember(theme) { theme.style() }
    val letters = remember { listOf('W', 'O', 'R', 'D', 'S', 'C', 'A', 'N', 'D') }
    val cornerDp = (themeStyle.tileCornerRadiusDp / 2.2f).coerceAtLeast(3f).dp

    Box(
        modifier = Modifier
            .size(size)
            .shadow(4.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(themeStyle.backgroundStops))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
                .background(themeStyle.boardFrameColor)
                .border(1.5.dp, themeStyle.boardBorderColor, RoundedCornerShape(10.dp))
                .padding(3.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            for (r in 0 until 3) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (c in 0 until 3) {
                        val idx = r * 3 + c
                        val tileColor = themeStyle.tilePalette[idx % themeStyle.tilePalette.size]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(cornerDp))
                                .background(tileColor)
                                .border(0.8.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(cornerDp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (size >= 70.dp) {
                                Text(
                                    text = letters[idx].toString(),
                                    fontFamily = FredokaFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopPackCard(
    title: String,
    subtitle: String,
    priceCoins: Int,
    badgeColor: List<Color>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .border(2.dp, CandyYellow, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(badgeColor)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SurfaceDarkPurple
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B5A8E)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(listOf(CandyPurple, CandyBlue)))
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.MonetizationOn,
                contentDescription = null,
                tint = CandyGold,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = priceCoins.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ThemeShopCard(
    theme: BoardTheme,
    lang: AppLanguage,
    isOwned: Boolean,
    isEquipped: Boolean,
    canAfford: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .border(
                width = if (isEquipped) 2.5.dp else 1.5.dp,
                color = when {
                    isEquipped -> CandyGreenDark
                    isOwned -> CandyBlue.copy(alpha = 0.65f)
                    else -> CandyYellow.copy(alpha = 0.7f)
                },
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("shop_theme_${theme.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MiniGridPreview(
                theme = theme,
                size = 58.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (lang == AppLanguage.EN) theme.titleEn else theme.titleFr,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SurfaceDarkPurple
                )
                Text(
                    text = if (lang == AppLanguage.EN) theme.subtitleEn else theme.subtitleFr,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B5A8E)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        val actionBg = when {
            isEquipped -> listOf(CandyGreenDark, Color(0xFF0E9F4A))
            isOwned -> listOf(CandyBlue, CandyPurple)
            canAfford -> listOf(CandyOrange, CandyPinkRed)
            else -> listOf(Color(0xFF9E98B3), Color(0xFF7D7694))
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(actionBg))
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                isEquipped -> {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = LocalizedStrings.equipped(lang),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        color = Color.White
                    )
                }
                isOwned -> {
                    Text(
                        text = LocalizedStrings.equip(lang),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        color = Color.White
                    )
                }
                else -> {
                    Icon(
                        imageVector = if (canAfford) Icons.Rounded.MonetizationOn else Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = if (canAfford) CandyGold else Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = theme.priceCoins.toString(),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun RewardedAdShopCard(
    lang: AppLanguage,
    adsStatus: UnityAdsStatus,
    onClick: () -> Unit
) {
    val enabled = adsStatus.isConfigured && adsStatus.isRewardedReady
    val pillLabel = when {
        !adsStatus.isConfigured -> LocalizedStrings.adConfigureHint(lang)
        adsStatus.isPlatformMismatch -> LocalizedStrings.adAndroidIdHint(lang)
        adsStatus.isRewardedReady -> LocalizedStrings.watchAdButton(lang)
        adsStatus.isRewardedFailed || (adsStatus.lastError != null && !adsStatus.isInitialized) ->
            LocalizedStrings.adUnavailable(lang)
        else -> LocalizedStrings.adLoadingButton(lang)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .border(2.dp, CandyGreenDark, RoundedCornerShape(22.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp)
            .testTag("shop_watch_rewarded_ad"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(CandyGreenDark, CandyBlue))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = LocalizedStrings.watchAdCoinsTitle(lang),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SurfaceDarkPurple
                )
                Text(
                    text = LocalizedStrings.watchAdCoinsSubtitle(lang),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B5A8E)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (enabled) Brush.horizontalGradient(listOf(CandyGreenDark, CandyBlue))
                    else Brush.horizontalGradient(listOf(Color(0xFF9E98B3), Color(0xFF7D7694)))
                )
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Text(
                text = pillLabel,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                color = Color.White
            )
        }
    }
}
