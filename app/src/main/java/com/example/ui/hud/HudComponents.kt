package com.example.ui.hud

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.*
import com.example.domain.*
import com.example.viewmodel.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import kotlin.math.*

// Styling Palette Constants
object ShardsDesign {
    // Базовые поверхности (4 уровня глубины)
    val Surface0 = Color(0xFF06080E)   // самый тёмный фон приложения
    val Surface1 = Color(0xFF0D1119)   // фон экранов
    val Surface2 = Color(0xFF141B27)   // карточки, панели
    val Surface3 = Color(0xFF1C2535)   // поднятые элементы, выделение

    // Акцент — тёплое золото (один акцент, не два)
    val Gold = Color(0xFFD4A843)        // чуть темнее, менее кричащий
    val GoldSoft = Color(0x30D4A843) // 19% alpha для фонов

    // Текст
    val TextPrimary = Color(0xFFF0EBE0)    // тёплый белый
    val TextSecondary = Color(0xFF8A8FA8)  // серо-голубой
    val TextMuted = Color(0xFF4A5166)      // приглушённый

    // Стихии — насыщеннее, контрастнее
    val Ice = Color(0xFF38BFFF)            // ярче стал
    val Bloom = Color(0xFF4DD47A)
    val Blaze = Color(0xFFFF6A35)
    val Mist = Color(0xFFB86EFF)           // более фиолетовый
    val Aether = Color(0xFFFFBE30)

    // Редкость предметов
    val Common = Color(0xFF8FA3B8)
    val Uncommon = Color(0xFF52C97A)
    val Rare = Color(0xFF4A9FFF)
    val Epic = Color(0xFFB060F0)
    val Legendary = Color(0xFFFF9500)
    val Mythic = Color(0xFFFF3D6E)

    // Семантические
    val Danger = Color(0xFFFF4444)
    val Success = Color(0xFF52C97A)
    val Warning = Color(0xFFFFBE30)
    
    // Специально для карты
    val MapOverlay = Color(0x2006080E)     // ЛЁГКИЙ оверлей (только 12%)
    val MapVignette = Color(0x5006080E)    // виньетка только 31%
}

val FantasyGold = ShardsDesign.Gold
val DarkVoidBg = ShardsDesign.Surface1
val CardLighterBg = ShardsDesign.Surface2
val IceBlue = ShardsDesign.Ice
val BloomGreen = ShardsDesign.Bloom
val BlazeOrange = ShardsDesign.Blaze
val MistPurple = ShardsDesign.Mist
val AetherGold = ShardsDesign.Aether

@Composable
fun MainGameScreen(
    currentTab: String,
    uiState: GameUiState,
    viewModel: GameViewModel,
    onTabSelected: (String) -> Unit
) {
    val activeOverlay by viewModel.activeOverlayTab.collectAsState()

    when (uiState) {
        is GameUiState.LaunchSelection -> {
            CovenantSelectionScreen(onSelect = { viewModel.selectStartingCovenant(it) })
        }
        is GameUiState.Loaded -> {
            val isNight = uiState.isNightMode
            val themeBgColor = if (isNight) Color(0xFF0C0D11) else Color(0xFFF4ECD8)

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    GameBottomNavigationBar(
                        activeTab = currentTab,
                        onTabSelected = onTabSelected,
                        battleActive = uiState.activeBattle != null
                    )
                },
                containerColor = themeBgColor
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (uiState.activeBattle != null) {
                        CombatArenaLayout(
                            battle = uiState.activeBattle,
                            viewModel = viewModel
                        )
                    } else {
                        // Regular Tab Routing
                        when (currentTab) {
                            "MAP" -> MapDashboardScreen(state = uiState, viewModel = viewModel)
                            "PARTY" -> PartyScreen(state = uiState, viewModel = viewModel)
                            "SETTINGS" -> SettingsSyncScreen(state = uiState, viewModel = viewModel)
                            else -> MapDashboardScreen(state = uiState, viewModel = viewModel)
                        }
                    }

                    // Floating diagnostic and feedback alerts
                    uiState.message?.let { msg ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFF2E1C0C), Color(0xFF120B04))))
                                .border(1.dp, FantasyGold, RoundedCornerShape(12.dp))
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, "Spark", tint = FantasyGold)
                                Text(
                                    text = msg,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Overlay Popup Drawer
                    if (activeOverlay != null && uiState.activeBattle == null) {
                        RetroFeatureOverlay(
                            tab = activeOverlay!!,
                            uiState = uiState,
                            viewModel = viewModel,
                            onClose = { viewModel.openOverlayTab(null) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RetroFeatureOverlay(
    tab: String,
    uiState: GameUiState.Loaded,
    viewModel: GameViewModel,
    onClose: () -> Unit
) {
    val isNight = uiState.isNightMode
    val panelBg = if (isNight) Color(0xFF131722) else Color(0xFFFFFDF8)
    val textPrimary = if (isNight) Color(0xFFECEFF4) else Color(0xFF1E150F)
    val borderCol = if (isNight) FantasyGold else Color(0xFF8A7E6C)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f)
                .background(panelBg)
                .border(2.dp, Color.Black)
                .padding(2.dp)
                .border(1.dp, borderCol)
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when(tab) {
                        "FORGE" -> "⚔️ ДРЕВНЯЯ КУЗНИЦА ЭФИРА"
                        "SUMMON" -> "🌀 ПОРТАЛ ПРИЗЫВА БЕЗДНЫ"
                        "GUILD" -> "⛲ СВЯТИЛИЩЕ ОБЕТОВ"
                        else -> "ОБЪЕКТ СИЛЫ"
                    },
                    color = textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    shape = RoundedCornerShape(0.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("ЗАКРЫТЬ [X]", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            Divider(color = Color.Black.copy(alpha = 0.15f), thickness = 2.dp, modifier = Modifier.padding(bottom = 8.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (tab) {
                    "FORGE" -> ForgeScreen(state = uiState, viewModel = viewModel)
                    "SUMMON" -> SummonPortalScreen(state = uiState, viewModel = viewModel)
                    "GUILD" -> GuildCovenantsScreen(state = uiState, viewModel = viewModel)
                }
            }
        }
    }
}

// --- SCREEN: COVENANT CREATION CHANGER ---
@Composable
fun CovenantSelectionScreen(onSelect: (Element) -> Unit) {
    var selectedElement by remember { mutableStateOf(Element.AETHER) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkVoidBg)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "SHARDS OF THE ABYSS",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FantasyGold,
            fontFamily = FontFamily.Serif,
            letterSpacing = 2.sp
        )
        Text(
            text = "ПОСВЯЩЕНИЕ В ПРОБУЖДЁННЫЕ",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.7f),
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Text(
            text = "Мир расколот вторжением Ворона Хаоса. Выберите Стихийную присягу бога, чтобы овладеть Осколком Силы:",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        // Selectable elements grid
        Element.entries.forEach { element ->
            val isSelected = selectedElement == element
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) Color(0xFF1E1E28) else CardLighterBg)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Color(element.colorHex) else Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { selectedElement = element }
                    .padding(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(element.colorHex).copy(alpha = 0.15f))
                            .border(1.dp, Color(element.colorHex), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when (element) {
                            Element.ICE -> Icons.Default.AcUnit
                            Element.BLOOM -> Icons.Default.Eco
                            Element.BLAZE -> Icons.Default.LocalFireDepartment
                            Element.MIST -> Icons.Default.Air
                            Element.AETHER -> Icons.Default.ElectricBolt
                        }
                        Icon(icon, element.title, tint = Color(element.colorHex), modifier = Modifier.size(24.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = element.title,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "(${element.god})",
                                fontSize = 12.sp,
                                color = FantasyGold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = element.description,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onSelect(selectedElement) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("covenant_confirm_button"),
            colors = ButtonDefaults.buttonColors(containerColor = FantasyGold),
            shape = RoundedCornerShape(0.dp)
        ) {
            Text(
                text = "ПРИНЯТЬ ОБЕТ: ${selectedElement.god.uppercase()}",
                color = Color.Black,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// --- SUB-SCREEN: THE FANTASY OSM MAP & CONTROLLER ---
@Composable
fun MapDashboardScreen(state: GameUiState.Loaded, viewModel: GameViewModel) {
    val context = LocalContext.current
    var zoomFactor by remember { mutableStateOf(1.2f) }
    var landmarksExpanded by remember { mutableStateOf(false) }
    var selectedPoi by remember { mutableStateOf<PointOfInterest?>(null) }
    val isRealGps by viewModel.isRealGpsEnabled.collectAsState()
    val activeRoutePoiId by viewModel.activeRoutePoiId.collectAsState()

    // Interactive Camera offset states for panning
    var panX by remember { mutableStateOf(0f) }
    var panY by remember { mutableStateOf(0f) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.toggleRealGpsTracker(true)
        } else {
            viewModel.showToast("Разрешение GPS отклонено! Используйте ручной контроллер.")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // THE FULL-SCREEN IMMERSIVE RPG CANVAS MAP (Occupies whole space in background)
        Fantasy2DMapView(
            playerLat = state.profile.currentLatitude,
            playerLon = state.profile.currentLongitude,
            pois = state.pois,
            viewModel = viewModel,
            zoomFactor = zoomFactor,
            panX = panX,
            panY = panY,
            onPanChanged = { px, py ->
                panX = px
                panY = py
            },
            onZoomChanged = { zoomFactor = it },
            onPoiTapped = { selectedPoi = it },
            isNight = state.isNightMode
        )

        // FLOATING TOP BAR: CHRONICLES & ECOSYSTEM TELEMETRY
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Squeezed and glowing Telemetry Bar with transparent glass background
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, Color.Black)
                        .padding(2.dp)
                        .border(1.dp, if (state.isNightMode) FantasyGold.copy(alpha = 0.4f) else Color(0xFF8A7E6C)),
                    colors = CardDefaults.cardColors(containerColor = if (state.isNightMode) Color(0xEB0C0D11) else Color(0xEBFFFDF6)),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(Color.Black)
                                        .border(1.dp, FantasyGold),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = state.profile.level.toString(),
                                        fontWeight = FontWeight.Black,
                                        color = FantasyGold,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Орден Пробуждения",
                                        color = if (state.isNightMode) Color.White else Color(0xFF1E150F),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    val maxXP = state.profile.level * 100
                                    val prg = if (maxXP > 0) state.profile.xp.toFloat() / maxXP else 0f
                                    LinearProgressIndicator(
                                        progress = { prg },
                                        modifier = Modifier
                                            .width(70.dp)
                                            .height(3.dp),
                                        color = FantasyGold,
                                        trackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(state.profile.selectedElement.colorHex).copy(alpha = 0.15f))
                                        .border(1.dp, Color(state.profile.selectedElement.colorHex))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = state.profile.selectedElement.god.uppercase(),
                                        color = Color(state.profile.selectedElement.colorHex),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(if (state.isNightMode) Color(0xFF1E1035) else Color(0xFFEFE8D3))
                                        .border(1.dp, if (state.isNightMode) MistPurple else Color(0xFF7E715D))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (state.isNightMode) "🌙 Ночь Бездны" else "☀️ День Света",
                                        color = if (state.isNightMode) MistPurple else Color(0xFF5A4D3B),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Divider(color = Color.Black.copy(alpha = 0.1f), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Icons.Default.MonetizationOn, "золото", tint = AetherGold, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "${state.profile.gold}",
                                    color = if (state.isNightMode) Color.White else Color(0xFF2C1E14),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Icons.Default.OfflineBolt, "ядра", tint = IceBlue, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "${state.profile.abyssalShards}",
                                    color = if (state.isNightMode) Color.White else Color(0xFF2C1E14),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Grain, "погода", tint = Color(state.weather.associatedElement.colorHex), modifier = Modifier.size(13.dp))
                                Text(
                                    text = state.weather.title,
                                    color = Color(state.weather.associatedElement.colorHex),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // COMPACT COMPASS D-PAD CRUISE CONTROLLER (Bottom Left)
        if (!isRealGps) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 12.dp)
                    .alpha(0.85f),
                colors = CardDefaults.cardColors(containerColor = if (state.isNightMode) Color(0xF207080B) else Color(0xF2FFFDF6)),
                border = BorderStroke(1.5.dp, Color.Black),
                shape = RoundedCornerShape(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "НАВИГАТОР БЕЗДНЫ",
                        color = if (state.isNightMode) FantasyGold else Color(0xFF2C1E14),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.2.sp
                    )

                    // D-Pad Grid
                    Column(
                        modifier = Modifier.padding(2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(
                            onClick = { viewModel.triggerVirtualMove("NORTH") },
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (state.isNightMode) Color(0xFF141620) else Color(0xFFF0E7D5), RoundedCornerShape(0.dp))
                                .border(1.5.dp, Color.Black)
                        ) {
                            Icon(Icons.Default.ArrowUpward, "North", tint = if (state.isNightMode) IceBlue else Color(0xFF2C1E14), modifier = Modifier.size(18.dp))
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.triggerVirtualMove("WEST") },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (state.isNightMode) Color(0xFF141620) else Color(0xFFF0E7D5), RoundedCornerShape(0.dp))
                                    .border(1.5.dp, Color.Black)
                            ) {
                                Icon(Icons.Default.ArrowBack, "West", tint = if (state.isNightMode) IceBlue else Color(0xFF2C1E14), modifier = Modifier.size(18.dp))
                            }

                            // Central glowing crystal sphere
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Brush.radialGradient(listOf(Color(0xFF2A92E0), Color.Transparent)), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.BlurCircular, "Core", tint = Color.White, modifier = Modifier.size(14.dp))
                            }

                            IconButton(
                                onClick = { viewModel.triggerVirtualMove("EAST") },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (state.isNightMode) Color(0xFF141620) else Color(0xFFF0E7D5), RoundedCornerShape(0.dp))
                                    .border(1.5.dp, Color.Black)
                            ) {
                                Icon(Icons.Default.ArrowForward, "East", tint = if (state.isNightMode) IceBlue else Color(0xFF2C1E14), modifier = Modifier.size(18.dp))
                            }
                        }

                        IconButton(
                            onClick = { viewModel.triggerVirtualMove("SOUTH") },
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (state.isNightMode) Color(0xFF141620) else Color(0xFFF0E7D5), RoundedCornerShape(0.dp))
                                .border(1.5.dp, Color.Black)
                        ) {
                            Icon(Icons.Default.ArrowDownward, "South", tint = if (state.isNightMode) IceBlue else Color(0xFF2C1E14), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // GPS & SENSOR PANEL (Floating Bottom Right)
        Card(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = if (state.isNightMode) Color(0xF207080B) else Color(0xF2FFFDF6)),
            border = BorderStroke(1.5.dp, Color.Black),
            shape = RoundedCornerShape(0.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = "GPS Portal",
                    tint = if (state.isNightMode) IceBlue else Color(0xFF7E715D),
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = "GPS-СВЯЗЬ",
                    color = if (state.isNightMode) Color.White else Color(0xFF2C1E14),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Switch(
                    checked = isRealGps,
                    onCheckedChange = { checked ->
                        if (checked) {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                viewModel.toggleRealGpsTracker(true)
                            } else {
                                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        } else {
                            viewModel.toggleRealGpsTracker(false)
                        }
                    },
                    modifier = Modifier.scale(0.55f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = FantasyGold,
                        checkedTrackColor = IceBlue.copy(0.6f),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.White.copy(0.1f)
                    )
                )
            }
        }

        // FLOATING ACTION MENU / ADVENTURE BAR (Right-Side alignment Column)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // COMPASS BUTTON WITH NUMERICAL BADGE
            Box(contentAlignment = Alignment.TopEnd) {
                IconButton(
                    onClick = { landmarksExpanded = !landmarksExpanded },
                    modifier = Modifier
                        .size(46.dp)
                        .background(if (state.isNightMode) Color(0xFF141622) else Color(0xFFFFFDF5), RoundedCornerShape(0.dp))
                        .border(BorderStroke(2.dp, Color.Black))
                        .padding(2.dp)
                        .border(BorderStroke(1.dp, if (state.isNightMode) FantasyGold else Color(0xFF8A7E6C)))
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "Compass Toggle",
                        tint = if (state.isNightMode) FantasyGold else Color(0xFF2C1E14),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Numerical Badge
                Box(
                    modifier = Modifier
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(20.dp)
                        .background(Color.Red, CircleShape)
                        .border(1.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.pois.size.toString(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // SNAP RECENTER BUTTON
            IconButton(
                onClick = {
                    panX = 0f
                    panY = 0f
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(if (state.isNightMode) Color(0xFF141622) else Color(0xFFFFFDF5), RoundedCornerShape(0.dp))
                    .border(BorderStroke(2.dp, Color.Black))
                    .padding(2.dp)
                    .border(BorderStroke(1.dp, if (state.isNightMode) FantasyGold else Color(0xFF2C1E14)))
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Recenter Map",
                    tint = if (state.isNightMode) FantasyGold else Color(0xFF2C1E14),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Zoom In button
            IconButton(
                onClick = { if (zoomFactor < 2.5f) zoomFactor += 0.2f },
                modifier = Modifier
                    .size(44.dp)
                    .background(if (state.isNightMode) Color(0xFF141622) else Color(0xFFFFFDF5), RoundedCornerShape(0.dp))
                    .border(BorderStroke(2.dp, Color.Black))
                    .padding(2.dp)
                    .border(BorderStroke(1.dp, if (state.isNightMode) FantasyGold else Color(0xFF8A7E6C)))
            ) {
                Icon(Icons.Default.ZoomIn, "Zoom In", tint = if (state.isNightMode) FantasyGold else Color(0xFF2C1E14), modifier = Modifier.size(20.dp))
            }

            // Zoom Out button
            IconButton(
                onClick = { if (zoomFactor > 0.5f) zoomFactor -= 0.2f },
                modifier = Modifier
                    .size(44.dp)
                    .background(if (state.isNightMode) Color(0xFF141622) else Color(0xFFFFFDF5), RoundedCornerShape(0.dp))
                    .border(BorderStroke(2.dp, Color.Black))
                    .padding(2.dp)
                    .border(BorderStroke(1.dp, if (state.isNightMode) FantasyGold else Color(0xFF8A7E6C)))
            ) {
                Icon(Icons.Default.ZoomOut, "Zoom Out", tint = if (state.isNightMode) FantasyGold else Color(0xFF2C1E14), modifier = Modifier.size(20.dp))
            }
        }

        // COMPACT RETRO PIXELS LANDS DIRECTORY DIALOG (Toggled by clicking the compass circular button above)
        if (landmarksExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val isNight = state.isNightMode
                val dialogBg = if (isNight) Color(0xFF131722) else Color(0xFFFFFDF8)
                val textPrimary = if (isNight) Color(0xFFECEFF4) else Color(0xFF1E150F)
                val borderCol = if (isNight) FantasyGold else Color(0xFF8A7E6C)

                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .fillMaxHeight(0.65f)
                        .background(dialogBg)
                        .border(2.dp, Color.Black)
                        .padding(2.dp)
                        .border(1.dp, borderCol)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                Icons.Default.Explore, "Compass",
                                tint = if (isNight) FantasyGold else Color(0xFF8A7E6C),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "РАЗЛОМЫ БЕЗДНЫ (${state.pois.size})",
                                color = textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        IconButton(
                            onClick = { landmarksExpanded = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, "Close", tint = textPrimary, modifier = Modifier.size(16.dp))
                        }
                    }

                    Divider(color = Color.Black.copy(alpha = 0.1f), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val listSorted = state.pois.map { poi ->
                            val dist = viewModel.calculateDistance(
                                state.profile.currentLatitude, state.profile.currentLongitude,
                                poi.latitude, poi.longitude
                            )
                            Pair(poi, dist)
                        }.sortedBy { it.second }

                        items(listSorted) { (poi, distance) ->
                            val canEnter = distance <= 50f
                            val rowColor = if (isNight) Color(0xFF1A1F2C) else Color(0xFFF7F2EB)
                            val rowText = if (isNight) Color.White else Color(0xFF2C1E14)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(rowColor)
                                    .border(1.dp, if (canEnter) Color(poi.element.colorHex) else Color.Transparent)
                                    .clickable {
                                        selectedPoi = poi
                                        landmarksExpanded = false // Close compass menu
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(Color(poi.type.markerColorHex).copy(alpha = 0.15f), CircleShape)
                                        .border(1.dp, Color(poi.type.markerColorHex), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val ico = when (poi.type) {
                                        PoiType.RIFT -> Icons.Default.LocalFireDepartment
                                        PoiType.ABYSSAL_GATE -> Icons.Default.AllInclusive
                                        PoiType.SANCTUM -> Icons.Default.AccountBalance
                                        PoiType.CHAOS_SPIKE -> Icons.Default.FlashOn
                                        PoiType.NEXUS_POINT -> Icons.Default.Token
                                        PoiType.MERCHANT_CARAVAN -> Icons.Default.Storefront
                                        PoiType.TAVERN -> Icons.Default.Restaurant
                                        PoiType.GUILD_VAULT -> Icons.Default.Key
                                        PoiType.SACRED_GROVE -> Icons.Default.Spa
                                        PoiType.RANDOM_ENCOUNTER -> Icons.Default.Help
                                    }
                                    Icon(ico, null, tint = Color(poi.type.markerColorHex), modifier = Modifier.size(12.dp))
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = poi.name,
                                        color = rowText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "${poi.type.label} • ${distance.toInt()}м",
                                        color = if (canEnter) BloomGreen else rowText.copy(alpha = 0.6f),
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (canEnter) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(poi.element.colorHex), RoundedCornerShape(2.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("В ЗОНЕ", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        }
                                    } else {
                                        Icon(Icons.Default.ChevronRight, "Inspect", tint = rowText.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // RENDERING SELECTED POI DETAIL BOTTOM SHEET
        selectedPoi?.let { poi ->
            val distance = viewModel.calculateDistance(
                state.profile.currentLatitude, state.profile.currentLongitude,
                poi.latitude, poi.longitude
            )
            PoiDetailBottomSheet(
                poi = poi,
                distance = distance,
                playerLevel = state.profile.level,
                activeRoutePoiId = activeRoutePoiId,
                onToggleRoute = { viewModel.toggleRouteToPoi(poi.id) },
                onOpenFeature = { feat ->
                    selectedPoi = null
                    viewModel.openOverlayTab(feat)
                },
                onEnterCombat = {
                    selectedPoi = null
                    if (poi.type == PoiType.SANCTUM) {
                        viewModel.showToast("Вы исцелились в Святилище бога и получили +10 Осколков!")
                        viewModel.claimBattleRewardsExit()
                    } else {
                        viewModel.initiatePOICombat(poi)
                    }
                },
                onDismiss = { selectedPoi = null }
            )
        }
    }
}

// Web Mercator Slippy Map Tile utilities for real-world coordinate projections
fun getTileX(lon: Double, zoom: Int): Double {
    return (lon + 180.0) / 360.0 * (1 shl zoom)
}

fun getTileY(lat: Double, zoom: Int): Double {
    val latRad = Math.toRadians(lat)
    val coercedLatRad = latRad.coerceIn(-1.484, 1.484) // standard Mercator limit to avoid infinity
    return (1.0 - Math.log(Math.tan(coercedLatRad) + 1.0 / Math.cos(coercedLatRad)) / PI) / 2.0 * (1 shl zoom)
}

data class MapPoiRenderItem(
    val poi: PointOfInterest,
    val x: Float,
    val y: Float,
    val distance: Float,
    val canEnter: Boolean,
    val isNearby: Boolean
)

// 2D CANVAS PERSPECTIVE OF OSM COORDINATES
@Composable
fun Fantasy2DMapView(
    playerLat: Double,
    playerLon: Double,
    pois: List<PointOfInterest>,
    viewModel: GameViewModel,
    zoomFactor: Float,
    panX: Float,
    panY: Float,
    onPanChanged: (Float, Float) -> Unit,
    onZoomChanged: (Float) -> Unit,
    onPoiTapped: (PointOfInterest) -> Unit,
    isNight: Boolean
) {
    val activeRoutePoiId by viewModel.activeRoutePoiId.collectAsState()
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    
    // Sonar Radar sweep pulse ring
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 350f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    // Flowing energy points along guide lines
    val guidanceFlow by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GuidanceFlow"
    )

    val zoom = when {
        zoomFactor < 0.8f -> 14
        zoomFactor < 1.3f -> 15
        zoomFactor < 1.7f -> 16
        else -> 17
    }
    val centerX = getTileX(playerLon, zoom)
    val centerY = getTileY(playerLat, zoom)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(if (isNight) 0xFF07080B else 0xFFF7F2EB))
            .pointerInput(zoomFactor, panX, panY) {
                detectTransformGestures { _, pan, zoomChange, _ ->
                    onPanChanged(panX + pan.x, panY + pan.y)
                    val newZoom = (zoomFactor * zoomChange).coerceIn(0.5f, 2.5f)
                    onZoomChanged(newZoom)
                }
            }
    ) {
        val density = LocalDensity.current
        val widthDp = maxWidth
        val heightDp = maxHeight

        val widthPx = with(density) { widthDp.toPx() }
        val heightPx = with(density) { heightDp.toPx() }

        val centerW = widthPx / 2f
        val centerH = heightPx / 2f

        val baseTileSizeDp = 256f
        val tileSizePx = baseTileSizeDp * density.density * zoomFactor
        val tileSizeDp = (baseTileSizeDp * zoomFactor).dp

        val currentX = centerX.toInt()
        val currentY = centerY.toInt()

        val heroX = centerW + panX
        val heroY = centerH + panY

        // Precompute POI Screen coordinates so we can draw and tap reliably
        val poisWithScreenCoords = remember(pois, centerX, centerY, zoom, zoomFactor, centerW, centerH, tileSizePx, playerLat, playerLon, panX, panY) {
            pois.map { poi ->
                val poiX_tile = getTileX(poi.longitude, zoom)
                val poiY_tile = getTileY(poi.latitude, zoom)
                
                val dx = (poiX_tile - centerX).toFloat()
                val dy = (poiY_tile - centerY).toFloat()

                val poiX = centerW + (dx * tileSizePx) + panX
                val poiY = centerH + (dy * tileSizePx) + panY
                
                val distance = viewModel.calculateDistance(playerLat, playerLon, poi.latitude, poi.longitude)
                MapPoiRenderItem(
                    poi = poi,
                    x = poiX,
                    y = poiY,
                    distance = distance,
                    canEnter = distance <= 50f,
                    isNearby = distance <= 500f
                )
            }
        }

        // 1. RENDER BACKGROUND REAL MAP TILES FROM CARTODB (Dark or Light depending on environment)
        Box(modifier = Modifier.fillMaxSize()) {
            for (tileX in (currentX - 2)..(currentX + 2)) {
                for (tileY in (currentY - 2)..(currentY + 2)) {
                    val dx = (tileX - centerX).toFloat()
                    val dy = (tileY - centerY).toFloat()
                    val leftPx = centerW + (dx * tileSizePx) + panX
                    val topPx = centerH + (dy * tileSizePx) + panY
                    val leftDp = with(density) { leftPx.toDp() }
                    val topDp = with(density) { topPx.toDp() }

                    val tileUrl = if (isNight) {
                        "https://basemaps.cartocdn.com/rastertiles/dark_all/$zoom/$tileX/$tileY.png"
                    } else {
                        "https://basemaps.cartocdn.com/rastertiles/light_all/$zoom/$tileX/$tileY.png"
                    }

                    coil.compose.AsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                            .data(tileUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Map Tile",
                        modifier = Modifier
                            .size(tileSizeDp)
                            .offset(x = leftDp, y = topDp),
                        contentScale = ContentScale.FillBounds,
                        colorFilter = if (!isNight) androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFFFFFAF0), androidx.compose.ui.graphics.BlendMode.Multiply) else null
                    )
                }
            }
        }

        // Overlay a refined vignette gradient to bleed real cartography background with the abyssal interface
        val radialGlow = Brush.radialGradient(
            colors = if (isNight) {
                listOf(
                    Color.Transparent,
                    Color.Transparent,
                    Color(0x1506080E),   // 8%
                    Color(0x4006080E)    // 25% only at the edges
                )
            } else {
                listOf(
                    Color.Transparent,
                    Color.Transparent,
                    Color(0x108A7E6C),
                    Color(0x258A7E6C)
                )
            },
            center = Offset(heroX, heroY),
            radius = max(centerW, centerH) * 1.3f
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(radialGlow)
        )

        // 2. RADICAL SYSTEM CANVAS DRAWING OVER REAL WORLD CARTO-GRID
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val compassGold = if (isNight) ShardsDesign.Gold.copy(alpha = 0.22f) else Color(0x608A7E6C)
            val indexRingBg = if (isNight) ShardsDesign.Gold.copy(alpha = 0.08f) else Color(0x208A7E6C)

            // Compass Dial index ring
            drawCircle(
                color = indexRingBg,
                radius = 150f,
                center = Offset(heroX, heroY),
                style = Stroke(width = 1f)
            )

            // Compass marks ticks
            for (ang in 0..11) {
                val theta = (ang * 30 * Math.PI / 180).toFloat()
                val edgeStart = Offset(
                    heroX + cos(theta) * 142f,
                    heroY + sin(theta) * 142f
                )
                val edgeEnd = Offset(
                    heroX + cos(theta) * 150f,
                    heroY + sin(theta) * 150f
                )
                drawLine(
                    color = compassGold,
                    start = edgeStart,
                    end = edgeEnd,
                    strokeWidth = 1.5f
                )
            }

            // Radar Sweep sonar ring
            drawCircle(
                color = IceBlue.copy(alpha = pulseAlpha),
                radius = pulseRadius,
                center = Offset(heroX, heroY),
                style = Stroke(width = 1.6f)
            )

            // 3. DRAW LANDMARKS, PORTALS & ALIGNED PROJECTION VECTOR LINES
            val nativeCanvas = drawContext.canvas.nativeCanvas

            for (item in poisWithScreenCoords) {
                val poi = item.poi
                val poiX = item.x
                val poiY = item.y
                val canEnter = item.canEnter
                val isNearby = item.isNearby

                if (poiX in -50f..(size.width + 50f) && poiY in -50f..(size.height + 50f)) {
                    val elementColor = Color(poi.element.colorHex)
                    val typeColor = Color(poi.type.markerColorHex)

                    // DRAW PROJECTION VECTOR LINE TO LANDMARKS IN SCREEN RANGE
                    if (isNearby) {
                        drawLine(
                            color = typeColor.copy(alpha = if (canEnter) 0.65f else 0.35f),
                            start = Offset(heroX, heroY),
                            end = Offset(poiX, poiY),
                            strokeWidth = if (canEnter) 2.5f else 1.2f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                floatArrayOf(8f, 10f), 0f
                            )
                        )

                        // Flowing guiding spark dot towards portal
                        val flowX = (heroX + (poiX - heroX) * guidanceFlow)
                        val flowY = (heroY + (poiY - heroY) * guidanceFlow)
                        drawCircle(
                            color = typeColor,
                            radius = 4.5f,
                            center = Offset(flowX, flowY)
                        )
                    }

                    // DRAW DIVERSIFIED LANDMARK VECTOR GRAPHICS
                    drawCircle(
                        color = typeColor.copy(alpha = 0.16f),
                        radius = 26f * zoomFactor,
                        center = Offset(poiX, poiY)
                    )

                    when (poi.type) {
                        PoiType.RIFT -> {
                            val riftStroke = 2.5f * zoomFactor
                            drawLine(elementColor, Offset(poiX - 12f * zoomFactor, poiY), Offset(poiX + 12f * zoomFactor, poiY), strokeWidth = riftStroke)
                            drawLine(elementColor, Offset(poiX, poiY - 12f * zoomFactor), Offset(poiX, poiY + 12f * zoomFactor), strokeWidth = riftStroke)
                            drawCircle(typeColor, radius = 4f * zoomFactor, center = Offset(poiX, poiY))
                        }
                        PoiType.ABYSSAL_GATE -> {
                            drawCircle(typeColor, radius = 9f * zoomFactor, center = Offset(poiX, poiY), style = Stroke(width = 2.5f * zoomFactor))
                            drawCircle(elementColor, radius = 15f * zoomFactor, center = Offset(poiX, poiY), style = Stroke(width = 1.8f * zoomFactor))
                            drawLine(elementColor, Offset(poiX - 18f, poiY - 18f), Offset(poiX + 18f, poiY + 18f), strokeWidth = 1.2f)
                        }
                        PoiType.SANCTUM -> {
                            val archPath = Path().apply {
                                moveTo(poiX - 10f * zoomFactor, poiY + 10f * zoomFactor)
                                lineTo(poiX - 10f * zoomFactor, poiY - 2f * zoomFactor)
                                quadraticTo(poiX, poiY - 12f * zoomFactor, poiX + 10f * zoomFactor, poiY - 2f * zoomFactor)
                                lineTo(poiX + 10f * zoomFactor, poiY + 10f * zoomFactor)
                                close()
                            }
                            drawPath(archPath, color = elementColor.copy(alpha = 0.4f))
                            drawPath(archPath, color = typeColor, style = Stroke(width = 1.8f * zoomFactor))
                            drawCircle(typeColor, radius = 3.5f * zoomFactor, center = Offset(poiX, poiY + 2f * zoomFactor))
                        }
                        PoiType.CHAOS_SPIKE -> {
                            val spike = Path().apply {
                                moveTo(poiX, poiY - 14f * zoomFactor)
                                lineTo(poiX - 7f * zoomFactor, poiY)
                                lineTo(poiX, poiY + 14f * zoomFactor)
                                lineTo(poiX + 7f * zoomFactor, poiY)
                                close()
                            }
                            drawPath(spike, color = elementColor.copy(alpha = 0.45f))
                            drawPath(spike, color = typeColor, style = Stroke(width = 1.8f * zoomFactor))
                        }
                        PoiType.NEXUS_POINT -> {
                            val star = Path().apply {
                                moveTo(poiX, poiY - 12f * zoomFactor)
                                lineTo(poiX + 12f * zoomFactor, poiY)
                                lineTo(poiX, poiY + 12f * zoomFactor)
                                lineTo(poiX - 12f * zoomFactor, poiY)
                                close()
                            }
                            drawPath(star, color = typeColor.copy(alpha = 0.5f))
                            drawRect(
                                color = elementColor,
                                topLeft = Offset(poiX - 5f * zoomFactor, poiY - 5f * zoomFactor),
                                size = androidx.compose.ui.geometry.Size(10f * zoomFactor, 10f * zoomFactor),
                                style = Stroke(width = 1.2f * zoomFactor)
                            )
                        }
                        PoiType.MERCHANT_CARAVAN -> {
                            drawCircle(typeColor, radius = 9f * zoomFactor, center = Offset(poiX, poiY), style = Stroke(width = 2f * zoomFactor))
                            drawRect(
                                color = elementColor,
                                topLeft = Offset(poiX - 4f * zoomFactor, poiY - 4f * zoomFactor),
                                size = androidx.compose.ui.geometry.Size(8f * zoomFactor, 8f * zoomFactor)
                            )
                        }
                        PoiType.TAVERN -> {
                            drawCircle(typeColor, radius = 10f * zoomFactor, center = Offset(poiX, poiY), style = Stroke(width = 2.2f * zoomFactor))
                            drawLine(elementColor, Offset(poiX - 6f * zoomFactor, poiY), Offset(poiX + 6f * zoomFactor, poiY), strokeWidth = 2f * zoomFactor)
                        }
                        PoiType.GUILD_VAULT -> {
                            drawCircle(typeColor, radius = 11f * zoomFactor, center = Offset(poiX, poiY), style = Stroke(width = 1.8f * zoomFactor))
                            drawCircle(elementColor, radius = 4f * zoomFactor, center = Offset(poiX, poiY))
                        }
                        PoiType.SACRED_GROVE -> {
                            drawCircle(typeColor, radius = 12f * zoomFactor, center = Offset(poiX, poiY), style = Stroke(width = 1.5f * zoomFactor))
                            drawLine(elementColor, Offset(poiX, poiY - 9f * zoomFactor), Offset(poiX, poiY + 9f * zoomFactor), strokeWidth = 2.5f * zoomFactor)
                        }
                        PoiType.RANDOM_ENCOUNTER -> {
                            drawCircle(typeColor, radius = 8f * zoomFactor, center = Offset(poiX, poiY), style = Stroke(width = 1.6f * zoomFactor))
                        }
                    }

                    // Outer active activation ring
                    drawCircle(
                        color = elementColor.copy(alpha = if (canEnter) 0.95f else 0.45f),
                        radius = 18f * zoomFactor,
                        center = Offset(poiX, poiY),
                        style = Stroke(width = if (canEnter) 2f else 1.2f)
                    )

                    // DRAW LANDMARK NAMEPLATE LABEL
                    if (poiX in 30f..(size.width - 30f) && poiY in -10f..(size.height - 10f)) {
                        val namePaint = android.graphics.Paint().apply {
                            color = if (isNight) android.graphics.Color.argb(220, 240, 235, 224) else android.graphics.Color.argb(220, 30, 21, 15)
                            textSize = 21f * zoomFactor.coerceIn(0.8f, 1.4f)
                            typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                            isAntiAlias = true
                            setShadowLayer(4f, 0f, 1f, if (isNight) android.graphics.Color.argb(185, 0, 0, 0) else android.graphics.Color.argb(100, 255, 255, 255))
                        }
                        val displayName = if ((poi.realName ?: poi.name).length > 16) {
                            (poi.realName ?: poi.name).take(14) + "…"
                        } else {
                            poi.realName ?: poi.name
                        }
                        nativeCanvas.drawText(
                            displayName,
                            poiX - namePaint.measureText(displayName) / 2f,
                            poiY + 30f * zoomFactor + 16f,
                            namePaint
                        )
                    }
                }
            }

            // --- DRAW ACTIVE ROUTE LINE TO SELECTED TARGET ---
            if (activeRoutePoiId != null) {
                val destPoi = poisWithScreenCoords.find { it.poi.id == activeRoutePoiId }
                if (destPoi != null) {
                    // Solid outer highlight path (beautiful epic fantasy neon gold)
                    drawLine(
                        color = Color(0xFFD32F2F).copy(alpha = 0.85f), // Red alert path
                        start = Offset(heroX, heroY),
                        end = Offset(destPoi.x, destPoi.y),
                        strokeWidth = 7f * zoomFactor.coerceAtLeast(1f),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(24f, 16f),
                            guidanceFlow * -40f
                        )
                    )
                    drawLine(
                        color = FantasyGold,
                        start = Offset(heroX, heroY),
                        end = Offset(destPoi.x, destPoi.y),
                        strokeWidth = 3f * zoomFactor.coerceAtLeast(1f),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(24f, 16f),
                            guidanceFlow * -40f
                        )
                    )
                    // Inner glowing focus core pulse
                    drawLine(
                        color = Color.White,
                        start = Offset(heroX, heroY),
                        end = Offset(destPoi.x, destPoi.y),
                        strokeWidth = 1f,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(24f, 16f),
                            guidanceFlow * -40f
                        )
                    )
                }
            }

            // 4. HERO BEACON COMPASS LABELS & CENTRAL RIPPLE CORE
            val paint = android.graphics.Paint().apply {
                color = if (isNight) android.graphics.Color.argb((0.7f * 255).toInt(), 212, 168, 67) else android.graphics.Color.argb((0.7f * 255).toInt(), 138, 126, 108)
                textSize = 21f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                isAntiAlias = true
            }
            nativeCanvas.drawText("С", heroX - 6f, heroY - 158f, paint)
            nativeCanvas.drawText("Ю", heroX - 6f, heroY + 176f, paint)
            nativeCanvas.drawText("В", heroX + 160f, heroY + 6f, paint)
            nativeCanvas.drawText("З", heroX - 176f, heroY + 6f, paint)

            // Hero position pulse
            drawCircle(
                color = ShardsDesign.Ice.copy(alpha = pulseAlpha * 0.5f),
                radius = 15f + pulseRadius * 0.05f,
                center = Offset(heroX, heroY),
                style = Stroke(width = 2f)
            )

            // Inner focus hub
            drawCircle(
                color = if (isNight) ShardsDesign.Gold else Color(0xFF8A7E6C),
                radius = 6f,
                center = Offset(heroX, heroY)
            )
            drawCircle(
                color = Color.White,
                radius = 3f,
                center = Offset(heroX, heroY)
            )
        }

        // 5. TRANSPARENT TAP DETECT OVERLAY
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(poisWithScreenCoords) {
                    detectTapGestures { tapOffset ->
                        val tappedPoi = poisWithScreenCoords.find { item ->
                            val dx = tapOffset.x - item.x
                            val dy = tapOffset.y - item.y
                            sqrt(dx * dx + dy * dy) < 45f  // tapping radius
                        }?.poi
                        if (tappedPoi != null) {
                            onPoiTapped(tappedPoi)
                        }
                    }
                }
        )

        // FLOATING TELEMETRY MINI BADGE (Top Left coordinates info window)
        Card(
            modifier = Modifier
                .padding(top = 138.dp, start = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(if (isNight) 0xBE000000 else 0xBEFFFDF6)),
            border = BorderStroke(0.5.dp, (if (isNight) FantasyGold else Color(0xFF8A7E6C)).copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = "🌐 ЭХО-СПЕКТР РАДАРА",
                    color = if (isNight) FantasyGold else Color(0xFF8A7E6C),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Шир: ${String.format("%.5f", playerLat)}",
                    color = if (isNight) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Дол: ${String.format("%.5f", playerLon)}",
                    color = if (isNight) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Масштаб: ${String.format("%.1fx", zoomFactor)}",
                    color = if (isNight) IceBlue else Color(0xFF5A4D3B),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// Новый Composable: PoiDetailBottomSheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoiDetailBottomSheet(
    poi: PointOfInterest,
    distance: Float,
    playerLevel: Int,
    activeRoutePoiId: String? = null,
    onToggleRoute: () -> Unit = {},
    onOpenFeature: (String) -> Unit = {},
    onEnterCombat: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val elementColor = Color(poi.element.colorHex)
    val canEnter = distance <= 50f
    val isOnCooldown = poi.cooldownUntil > System.currentTimeMillis()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF111520),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Реальное название объекта (если есть)
            poi.realName?.let { realName ->
                Text(
                    text = realName,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Игровое название
            Text(
                text = poi.name,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            // Строка бейджей
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(elementColor.copy(alpha = 0.15f))
                        .border(1.dp, elementColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = poi.element.god.uppercase(),
                        color = elementColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(poi.type.markerColorHex).copy(alpha = 0.15f))
                        .border(1.dp, Color(poi.type.markerColorHex), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = poi.type.label,
                        color = Color(poi.type.markerColorHex),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "Ур. ${poi.minLevel}–${poi.maxLevel}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(100))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            // Реальные теги из OSM
            val osmDescription = buildOsmDescription(poi.osmTags)
            if (osmDescription.isNotEmpty()) {
                Text(
                    text = osmDescription,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }

            // Статус захвата
            if (poi.isCapturedByGuild) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Flag, null, tint = FantasyGold, modifier = Modifier.size(16.dp))
                    Text(
                        "Захвачено гильдией: ${poi.capturedGuildName ?: "Анонимные"}",
                        color = FantasyGold,
                        fontSize = 13.sp
                    )
                }
            }

            // Расстояние и дистанция до входа
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.MyLocation, null,
                    tint = if (canEnter) BloomGreen else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (canEnter) "В зоне входа" else "${distance.toInt()}м (нужно 50м)",
                    color = if (canEnter) BloomGreen else Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Кнопка входа
            Button(
                onClick = onEnterCombat,
                enabled = canEnter && !isOnCooldown,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = elementColor,
                    disabledContainerColor = Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                when {
                    isOnCooldown -> {
                        Text(
                            "Восстановление: ${poi.cooldownRemainingText()}",
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    !canEnter -> {
                        Text(
                            "Подойдите ближе",
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    poi.type == PoiType.SANCTUM -> {
                        Icon(Icons.Default.Healing, null, tint = Color.Black)
                        Spacer(Modifier.width(8.dp))
                        Text("ВОЙТИ В СВЯТИЛИЩЕ", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                    }
                    else -> {
                        Icon(Icons.Default.FlashOn, null, tint = Color.Black)
                        Spacer(Modifier.width(8.dp))
                        Text("НАЧАТЬ БОЙ", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ИНТЕГРАЦИЯ ТЕРРИТОРИАЛЬНЫХ ФУНКЦИЙ (Кузница, Призыв, Обеты)
            val associatedFeature = when (poi.type) {
                PoiType.GUILD_VAULT, PoiType.NEXUS_POINT -> "FORGE"
                PoiType.ABYSSAL_GATE, PoiType.RIFT -> "SUMMON"
                PoiType.SANCTUM, PoiType.SACRED_GROVE -> "GUILD"
                else -> null
            }

            associatedFeature?.let { feat ->
                Button(
                    onClick = { onOpenFeature(feat) },
                    enabled = canEnter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .border(1.5.dp, Color.Black)
                        .padding(2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canEnter) Color(0xFFD4A843) else Color(0xFF1E293B),
                        disabledContainerColor = Color(0x301E293B)
                    ),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    val label = when (feat) {
                        "FORGE" -> "⚡ ВОЙТИ В КУЗНИЦУ АРСЕНАЛА [FORGE]"
                        "SUMMON" -> "🌀 АКТИВИРОВАТЬ ПОРТАЛ ПРИЗЫВА [SUMMON]"
                        "GUILD" -> "⛲ ИЗБРАТЬ ОБЕТ КОВЕНАНТА [COVENANT]"
                        else -> "ОТКРЫТЬ ФУНКЦИЮ"
                    }
                    Text(
                        text = if (canEnter) label else "🔒 $label (НУЖНО <= 50М)",
                        color = if (canEnter) Color.Black else Color.White.copy(0.4f),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Кнопка переключения маршрута (сброс / построение четкого маршрута)
            val isRoutingThis = activeRoutePoiId == poi.id
            Button(
                onClick = onToggleRoute,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("poi_route_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRoutingThis) Color(0xFFC62828) else Color(0xFF1E293B)
                ),
                border = BorderStroke(1.dp, if (isRoutingThis) Color(0xFFE53935) else Color(0xFF334155)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (isRoutingThis) Icons.Default.Cancel else Icons.Default.Navigation,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isRoutingThis) "ОТМЕНИТЬ МАРШРУТ" else "ПОСТРОИТЬ МАРШРУТ",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

fun buildOsmDescription(tags: Map<String, String>): String {
    val parts = mutableListOf<String>()
    tags["historic"]?.let { parts.add("Исторический объект: $it") }
    tags["tourism"]?.let { parts.add("Туристический объект: $it") }
    tags["amenity"]?.let { if (it == "place_of_worship") parts.add("Место поклонения") }
    tags["denomination"]?.let { parts.add("Конфессия: $it") }
    tags["website"]?.let { parts.add("Сайт: $it") }
    return parts.joinToString(" • ")
}

fun PointOfInterest.cooldownRemainingText(): String {
    val remaining = cooldownUntil - System.currentTimeMillis()
    if (remaining <= 0) return ""
    val minutes = remaining / (60000)
    val hours = minutes / 60
    return if (hours > 0) "${hours}ч ${minutes % 60}м" else "${minutes}м"
}

// --- SCREEN: UNLOCKED SQUAD & GEAR INSPECTION ---
@Composable
fun PartyScreen(state: GameUiState.Loaded, viewModel: GameViewModel) {
    var selectedHero by remember { mutableStateOf<AwakenedHero?>(null) }
    if (selectedHero == null && state.heroes.isNotEmpty()) {
        selectedHero = state.heroes.first()
    }

    var itemToDismantle by remember { mutableStateOf<GearItem?>(null) }

    if (itemToDismantle != null) {
        AlertDialog(
            onDismissRequest = { itemToDismantle = null },
            title = {
                Text(
                    text = "Разобрать снаряжение?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "Вы действительно хотите утилизировать предмет «${itemToDismantle?.name}» [${itemToDismantle?.rarity?.title}]?\n\nВы получите часть астральных осколков, но этот предмет будет навсегда уничтожен!",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            },
            containerColor = CardLighterBg,
            confirmButton = {
                Button(
                    onClick = {
                        itemToDismantle?.let { viewModel.dismantleItem(it) }
                        itemToDismantle = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Разобрать", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { itemToDismantle = null },
                    colors = ButtonDefaults.buttonColors(containerColor = CardLighterBg)
                ) {
                    Text("Отмена", color = Color.White)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "ОРДЕН ПРОБУЖДЁННЫХ",
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            color = FantasyGold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        // Horizontal list of recruited heroes
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.heroes.forEach { hero ->
                val isSel = selectedHero?.id == hero.id
                Card(
                    modifier = Modifier
                        .width(135.dp)
                        .border(
                            1.dp,
                            if (isSel) Color(hero.element.colorHex) else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedHero = hero },
                    colors = CardDefaults.cardColors(containerColor = if (isSel) Color(0xFF1B1B26) else CardLighterBg)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = hero.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        Text(
                            text = "${hero.currentLevel} УР.",
                            fontSize = 11.sp,
                            color = Color(hero.element.colorHex),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Row {
                            repeat(hero.starRating) {
                                Icon(Icons.Default.Star, "Star", tint = FantasyGold, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }

        selectedHero?.let { hero ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardLighterBg),
                border = BorderStroke(1.dp, Color(hero.element.colorHex).copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(hero.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(
                                "Покровитель: ${hero.element.god}",
                                fontSize = 12.sp,
                                color = Color(hero.element.colorHex),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Star ascension trigger button
                        Button(
                            onClick = { viewModel.ascendHeroStars(hero) },
                            colors = ButtonDefaults.buttonColors(containerColor = FantasyGold),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("ВОЗВЫСИТЬ ★", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Stat values block
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatLabel(title = "❤️ ОЗ (Макс)", value = hero.maxHp.toString())
                        StatLabel(title = "⚔️ Атака", value = hero.attack.toString())
                        StatLabel(title = "🛡️ Броня", value = hero.defense.toString())
                        StatLabel(title = "⚡ Скор", value = hero.speed.toString())
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.levelUpHeroThroughShards(hero) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(hero.element.colorHex))
                        ) {
                            Text(
                                "Повысить уровень [${hero.currentLevel * 100}з]",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            // Equipped items list
            Text(
                text = "ЭКИПИРОВКА ГЕРОЯ",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = FantasyGold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            val heroGears = state.inventory.filter { it.equippedHeroId == hero.id }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardLighterBg)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (heroGears.isEmpty()) {
                        Text(
                            "Нет экипированных предметов. Войдите во вкладку АРСЕНАЛ ниже, чтобы оснастить воина.",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        )
                    } else {
                        heroGears.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.3f))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Color(item.rarity.colorHex).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .border(1.dp, Color(item.rarity.colorHex), RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            item.slot.name.take(1),
                                            color = Color(item.rarity.colorHex),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Column {
                                        Text(item.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            item.slot.title + " • Мощь +" + item.basePower,
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Button(
                                    onClick = { viewModel.equipItemToHero(item, null) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("Снять", color = Color.White, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // BACKPACK MANAGEMENT LIST
        Text(
            text = "СВОБОДНЫЙ АРСЕНАЛ И ИНВЕНТАРЬ",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = FantasyGold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        val unequippedGears = state.inventory.filter { it.equippedHeroId == null }
        if (unequippedGears.isEmpty()) {
            Text(
                "Инвентарь пуст. Запечатывайте Разломы или куйте новые предметы в кузнице!",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
            )
        } else {
            unequippedGears.forEach { gear ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardLighterBg)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(gear.rarity.colorHex).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .border(1.dp, Color(gear.rarity.colorHex), RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (gear.slot) {
                                            GearSlot.WEAPON -> Icons.Default.FlashOn
                                            GearSlot.HELMET -> Icons.Default.DirectionsRun
                                            GearSlot.ARMOR -> Icons.Default.Shield
                                            else -> Icons.Default.Grade
                                        },
                                        contentDescription = "GearIcon",
                                        tint = Color(gear.rarity.colorHex),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(gear.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            "[${gear.rarity.title}]",
                                            color = Color(gear.rarity.colorHex),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        "${gear.slot.title} • Сила: +${gear.basePower}",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Equip trigger selection dropdown
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                selectedHero?.let { h ->
                                    Button(
                                        onClick = { viewModel.equipItemToHero(gear, h.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(h.element.colorHex)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("Надеть на ${h.name.take(5)}", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                IconButton(
                                    onClick = { itemToDismantle = gear },
                                    modifier = Modifier.size(28.dp).background(Color.Red.copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Icon(Icons.Default.DeleteForever, "Dismantle", tint = Color.Red, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        // Affinity list
                        if (gear.affixes.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                gear.affixes.forEach { affix ->
                                    Text(
                                        text = "⚡ " + affix.format(),
                                        color = FantasyGold,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatLabel(title: String, value: String) {
    Column {
        Text(title, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}


// --- SCREEN: BLACKSMITH & FORGING ---
@Composable
fun ForgeScreen(state: GameUiState.Loaded, viewModel: GameViewModel) {
    var selectedCraftSlot by remember { mutableStateOf(GearSlot.WEAPON) }
    var itemToDismantle by remember { mutableStateOf<GearItem?>(null) }

    if (itemToDismantle != null) {
        AlertDialog(
            onDismissRequest = { itemToDismantle = null },
            title = {
                Text(
                    text = "Разобрать снаряжение?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "Вы действительно хотите утилизировать предмет «${itemToDismantle?.name}» [${itemToDismantle?.rarity?.title}]?\n\nВы получите часть астральных осколков, но этот предмет будет навсегда уничтожен!",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            },
            containerColor = CardLighterBg,
            confirmButton = {
                Button(
                    onClick = {
                        itemToDismantle?.let { viewModel.dismantleItem(it) }
                        itemToDismantle = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Разобрать", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { itemToDismantle = null },
                    colors = ButtonDefaults.buttonColors(containerColor = CardLighterBg)
                ) {
                    Text("Отмена", color = Color.White)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "АСТРАЛЬНАЯ КУЗНИЦА",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = FantasyGold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            // Forge pricing badge
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, FantasyGold.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "Ковка: 15 💎, 150 🪙",
                    color = FantasyGold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Text(
            text = "Выберите тип брони или оружия для плавки Осколков Силы Бездны:",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f)
        )

        // Slot choosing selector grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.height(180.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(GearSlot.entries) { slot ->
                val isSel = selectedCraftSlot == slot
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) Color(0xFF221D12) else CardLighterBg)
                        .border(1.dp, if (isSel) FantasyGold else Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .clickable { selectedCraftSlot = slot }
                        .padding(10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isSel) FantasyGold else Color.White.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Hardware,
                                "Hammer",
                                tint = if (isSel) Color.Black else Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            slot.title,
                            color = if (isSel) FantasyGold else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Huge glowing Forge Button!
        Button(
            onClick = { viewModel.craftNewItem(selectedCraftSlot) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("craft_gear_button"),
            colors = ButtonDefaults.buttonColors(containerColor = FantasyGold),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Build, "anvil", tint = Color.Black)
                Text(
                    "ВЫКОВАТЬ НОВЫЙ ${selectedCraftSlot.title.uppercase()}",
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
        }

        Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 4.dp))

        // ASTRAL REFORGING MECHANICS
        Text(
            text = "АСТРАЛЬНАЯ ПЕРЕКОВКА СТИХИЙ",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = FantasyGold,
            fontFamily = FontFamily.Monospace
        )

        Text(
            text = "За 5 Осколков Силы Бездны вы можете разрушить и пересоздать все характеристики (аффиксы) на любом предмете в вашем инвентаре:",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.6f)
        )

        val unequippedList = state.inventory.filter { it.equippedHeroId == null }
        if (unequippedList.isEmpty()) {
            Text(
                "У вас нет доступных предметов для перековки в сумке.",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            )
        } else {
            unequippedList.forEach { gear ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardLighterBg)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(gear.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "[${gear.rarity.title}]",
                                color = Color(gear.rarity.colorHex),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "${gear.slot.title} • Ур.${gear.levelReq}",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                    }

                    Button(
                        onClick = { viewModel.reforgeEquipmentAffixes(gear) },
                        colors = ButtonDefaults.buttonColors(containerColor = MistPurple),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Перековать (5 💎)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


// --- SCREEN: SUMMON PORTAL GACHA-LITE ---
@Composable
fun SummonPortalScreen(state: GameUiState.Loaded, viewModel: GameViewModel) {
    val infiniteTransition = rememberInfiniteTransition("SummonRipple")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SummonScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "ПОРТАЛ БЕЗДНЫ ХАОСА",
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            color = FantasyGold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Text(
            text = "Призовите спящих легендарных богов и адептов Бездны для вступления в ваш отряд.",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Visually jaw-dropping portal graphic pulse!
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MistPurple.copy(alpha = 0.8f * pulseScale),
                            Color.Black
                        )
                    )
                )
                .border(2.dp, FantasyGold, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(2.dp, IceBlue.copy(alpha = 0.6f), CircleShape)
                    .background(Color.Black)
            ) {
                Icon(
                    Icons.Default.AllInclusive,
                    "DimensionalRift",
                    tint = FantasyGold,
                    modifier = Modifier.size(54.dp).align(Alignment.Center)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardLighterBg)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "СТОИМОСТЬ ПРИЗЫВА",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "15 Осколков Силы Бездны (ядер)",
                    color = FantasyGold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "У вас в распоряжении: ${state.profile.abyssalShards} шт.",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Button(
            onClick = { viewModel.executeVoidPortalSummon() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("summon_hero_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MistPurple),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, "sum", tint = Color.White)
                Text("ОТКРЫТЬ ВРАТА ПРИЗЫВА", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}


// --- SCREEN: COVENANTS LEADERS / TAXATION HALLS ---
@Composable
fun GuildCovenantsScreen(state: GameUiState.Loaded, viewModel: GameViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "ТАКТИЧЕСКИЙ ЗАЛ",
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            color = FantasyGold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B0F0B)),
            border = BorderStroke(1.dp, BlazeOrange.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "🔮 Эфирные Данжи (Оффлайн Тренировка)",
                    color = BlazeOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    "Полноценная тренировочная кампания в метро без GPS. Автогенерация виртуальных кристальных полей. Награда за победу: 70% золота и опыта.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                val leftD = viewModel.remainingEtherDungeons.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Лимит сегодня: ${leftD.value} / 10",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Button(
                        onClick = { viewModel.triggerOfflineEtherDungeon() },
                        colors = ButtonDefaults.buttonColors(containerColor = BlazeOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("НАЧАТЬ ТРЕНИРОВКУ", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        Text(
            text = "АКТИВНЫЕ СТИХИЙНЫЕ КОВЕНАНТЫ",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = FantasyGold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        state.guilds.forEach { guild ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardLighterBg)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(guild.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("Ур. ${guild.level}", color = FantasyGold, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                            Text(
                                "Участников: ${guild.totalContributors}",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = { viewModel.donateToCovenantGuild(guild) },
                            colors = ButtonDefaults.buttonColors(containerColor = FantasyGold),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Взнос (200 🪙)", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Казна: ${guild.treasuryGold} 🪙  |  ${guild.treasuryShards} 💎",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "Узлы силы: ${guild.territoryCount} шт.",
                            color = BloomGreen,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}


// --- SCREEN: STORAGE BACKUP SYNC ---
@Composable
fun SettingsSyncScreen(state: GameUiState.Loaded, viewModel: GameViewModel) {
    val clipboard = LocalClipboardManager.current
    var importText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "СИНХРОНИЗАЦИЯ БЭКАПА",
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            color = FantasyGold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Text(
            text = "Shards of the Abyss полностью поддерживает оффлайн-сохранения во внутреннюю зашифрованную базу данных Android. Выгрузите прогресс в JSON или импортируйте на новое устройство:",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f)
        )

        Button(
            onClick = {
                scope.launch {
                    val json = viewModel.exportBackupJson()
                    if (json.isNotEmpty()) {
                        clipboard.setText(AnnotatedString(json))
                        viewModel.showToast("Архив локального сохранения скопирован в буфер обмена! 💾")
                    } else {
                        viewModel.showToast("Ошибка при записи бэкапа! ❌")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = FantasyGold)
        ) {
            Icon(Icons.Default.CloudUpload, "export", tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("СКОПИРОВАТЬ СЕЙВ ДАННЫХ (ЭКСПОРТ)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 4.dp))

        Text(
            text = "ВОССТАНОВЛЕНИЕ ИЗ JSON АРХИВА",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = FantasyGold,
            fontFamily = FontFamily.Monospace
        )

        OutlinedTextField(
            value = importText,
            onValueChange = { newValue -> importText = newValue },
            placeholder = { Text("Вставьте JSON бэкап код сюда...", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
            shape = RoundedCornerShape(10.dp)
        )

        Button(
            onClick = {
                if (importText.isEmpty()) {
                    viewModel.showToast("Бэкап поле пустое!")
                } else {
                    viewModel.importBackupJson(importText)
                    importText = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MistPurple)
        ) {
            Icon(Icons.Default.SaveAlt, "import", tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("ИМПОРТИРОВАТЬ ДАННЫЕ ПРОГРЕССА", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}


// --- THE BOTTOM NAVIGATION ROW COMPONENT ---
@Composable
fun GameBottomNavigationBar(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    battleActive: Boolean
) {
    if (battleActive) return // Hide during intensive tactical battle QTE screens

    NavigationBar(
        containerColor = Color.Black,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        val tabs = listOf(
            Triple("MAP", "Радар", Icons.Default.Explore),
            Triple("PARTY", "Хранители", Icons.Default.Shield),
            Triple("SETTINGS", "Хроники", Icons.Default.Settings)
        )

        tabs.forEach { (tab, label, icon) ->
            val isSel = activeTab == tab
            NavigationBarItem(
                selected = isSel,
                onClick = { onTabSelected(tab) },
                icon = { Icon(icon, label, tint = if (isSel) FantasyGold else Color.White.copy(alpha = 0.4f)) },
                label = { Text(label, fontSize = 10.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.White.copy(alpha = 0.05f),
                    selectedTextColor = FantasyGold,
                    unselectedTextColor = Color.White.copy(alpha = 0.4f)
                ),
                modifier = Modifier.testTag("nav_tab_$tab")
            )
        }
    }
}


fun getElementSymbol(element: Element) = when (element) {
    Element.ICE -> "❄️"
    Element.BLOOM -> "🌿"
    Element.BLAZE -> "🔥"
    Element.MIST -> "🌀"
    Element.AETHER -> "🌌"
}

fun getCombatantAvatar(name: String, element: Element, isHero: Boolean): String {
    if (isHero) {
        return when (element) {
            Element.ICE -> "🏹" // Frost Ranger
            Element.BLOOM -> "🛡️" // Bloom Guardian
            Element.BLAZE -> "🔥" // Blaze Berserker
            Element.MIST -> "🥷" // Mist Rogue
            Element.AETHER -> "🧙" // Celestial Sorcerer
        }
    } else {
        if (name.contains("Вожак")) {
            return "🐉" // Boss Dragon
        }
        return when (element) {
            Element.ICE -> "🐺" // Glacial Wolf
            Element.BLOOM -> "🗿" // Ancient Treant / Golem
            Element.BLAZE -> "👹" // Pyre Demon
            Element.MIST -> "👻" // Mist Wraith
            Element.AETHER -> "👾" // Abyssal Leviathan
        }
    }
}

// --- SCREEN: COMBAT IN-GAME ARENA ---
@Composable
fun CombatArenaLayout(battle: ActiveBattleState, viewModel: GameViewModel) {
    val qteState = battle.qteState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07080A))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP BATTLE STATS HEADER (Enenemies / Foes of the Rift)
        Column {
            Text(
                text = "⚔ РАЗЛОМ БЕЗДНЫ: " + battle.poiName.uppercase(),
                color = FantasyGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Enemies (Taller, highly designed visual monster cards)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                battle.enemyUnits.forEach { enemy ->
                    val isDead = enemy.currentHp <= 0
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = if (isDead) 0.5.dp else 1.2.dp,
                                color = if (isDead) Color.White.copy(alpha = 0.1f) else Color(enemy.element.colorHex),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDead) Color.Transparent else CardLighterBg
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Element Icon Symbol Header Pill
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = getElementSymbol(enemy.element),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = enemy.element.title,
                                    color = Color(enemy.element.colorHex),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // STYLISH CIRCULAR AVATAR BOX (Icons for enemies!)
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isDead) Color.White.copy(alpha = 0.05f) 
                                        else Color(enemy.element.colorHex).copy(alpha = 0.12f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isDead) Color.White.copy(alpha = 0.2f) else Color(enemy.element.colorHex).copy(alpha = 0.5f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isDead) "💀" else getCombatantAvatar(enemy.name, enemy.element, false),
                                    fontSize = 20.sp
                                )
                            }

                            // Enemy Name Text
                            Text(
                                text = enemy.name,
                                color = if (isDead) Color.White.copy(alpha = 0.3f) else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )

                            // HP Progress
                            val hpRatio = if (enemy.maxHp > 0) enemy.currentHp.toFloat() / enemy.maxHp else 0f
                            LinearProgressIndicator(
                                progress = { hpRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (isDead) Color.Gray else Color.Red,
                                trackColor = Color.White.copy(alpha = 0.05f)
                            )

                            Text(
                                text = "ОЗ: ${enemy.currentHp}/${enemy.maxHp}",
                                color = if (isDead) Color.White.copy(alpha = 0.3f) else Color.Red,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // QTE DYNAMIC INTERACTION ZONE
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
        ) {
            if (battle.isFinished) {
                // VICTORY / DEFEAT CONCLUSION STATE SCREEN
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (battle.isVictory) "🏆 ПОБЕДА ОРДЕНА!" else "💀 ОТСТУПЛЕНИЕ",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = if (battle.isVictory) BloomGreen else Color.Red,
                        fontFamily = FontFamily.Serif
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (battle.isVictory) {
                        Text(
                            text = "Разлом запечатан изначальным огнем. Боты повержены. Получены ценные фрагменты Силы божества.",
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🏅 Опыт", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                                Text("+${battle.xpReward} XP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🪙 Золото", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                                Text("+${battle.goldReward}", color = AetherGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("💎 Осколки", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                                Text("+${battle.shardReward}", color = IceBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    } else {
                        Text(
                            "Отряд Пробужденных потерял связь с Эфиром. Все герои возвращены на базы Cвятилища для восстановления.",
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.claimBattleRewardsExit() },
                        colors = ButtonDefaults.buttonColors(containerColor = FantasyGold),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("battle_rewards_claim")
                    ) {
                        Text("ВЕРНУТЬСЯ НА КАРТУ", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (qteState.active) {
                // INTENSIVE BATTLE INTERACTIVE QTE CONTROLS
                when (qteState.type) {
                    QteType.ATTACK_RING -> {
                        AttackRingQteWidget(
                            prompt = qteState.promptText,
                            onQteFinish = { score -> viewModel.playerFinishQTEHit(score) }
                        )
                    }
                    QteType.BLOCK_SWIPE -> {
                        BlockSwipeQteWidget(
                            targetArrow = qteState.targetDirection,
                            prompt = qteState.promptText,
                            durationMs = qteState.timeLeftMs,
                            onSwipeResult = { success -> viewModel.feedSwipeResult(success) }
                        )
                    }
                    else -> {}
                }
            } else {
                // LOG OF TACTICAL FIGHT HUSTLES
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    reverseLayout = true
                ) {
                    val logsRev = battle.combatLog.reversed()
                    items(logsRev) { log ->
                        Text(
                            text = "● $log",
                            color = if (log.contains("🛡️") || log.contains("УСПЕШНО")) BloomGreen else if (log.contains("💀") || log.contains("ПОРАЖЕНИЕ")) Color.Red else Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // PLAYER PARTY CONTROLS
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val isMyTurn = battle.activeUnitUid?.startsWith("player_") == true

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                battle.playerUnits.forEach { hero ->
                    val isActiveTurn = battle.activeUnitUid == hero.uid
                    val isDead = hero.currentHp <= 0
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = if (isActiveTurn) 2.2.dp else 1.dp,
                                color = if (isActiveTurn) FantasyGold else if (isDead) Color.Transparent else Color(hero.element.colorHex),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDead) Color.Transparent else CardLighterBg
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = hero.name.take(12),
                                color = if (isDead) Color.White.copy(alpha = 0.3f) else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )

                            // TACTICAL HERO AVATARS WITH ELEMENT BADGES
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isDead) Color.White.copy(alpha = 0.05f)
                                        else Color(hero.element.colorHex).copy(alpha = 0.15f)
                                    )
                                    .border(
                                        width = 0.8.dp,
                                        color = Color(hero.element.colorHex).copy(alpha = 0.5f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isDead) "💀" else getCombatantAvatar(hero.name, hero.element, true),
                                    fontSize = 16.sp
                                )
                            }

                            // HP bar
                            val hpRatio = if (hero.maxHp > 0) hero.currentHp.toFloat() / hero.maxHp else 0f
                            LinearProgressIndicator(
                                progress = { hpRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = Color(hero.element.colorHex),
                                trackColor = Color.White.copy(alpha = 0.05f)
                            )
                            Text(
                                text = "${hero.currentHp}/${hero.maxHp}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            // AP bar (Action Points)
                            if (!isDead) {
                                val apRatio = (hero.currentActionPoints / 100f).coerceIn(0f, 1f)
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                    verticalArrangement = Arrangement.spacedBy(1.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    LinearProgressIndicator(
                                        progress = { apRatio },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(1.5.dp)),
                                        color = IceBlue,
                                        trackColor = Color.White.copy(alpha = 0.05f)
                                    )
                                    Text(
                                        text = "AP: ${hero.currentActionPoints.toInt()}/100",
                                        color = IceBlue.copy(alpha = 0.9f),
                                        fontSize = 7.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // TURN ACTIONS BAR CONTROLS
            if (!battle.isFinished) {
                if (isMyTurn) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { viewModel.playerTriggerStrikeAttack() },
                                modifier = Modifier.weight(1.2f).fillMaxHeight().testTag("combat_strike_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = FantasyGold),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.FlashOn, "strike", tint = Color.Black, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("УДАР СТИХИЙ", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                            }

                            Button(
                                onClick = { viewModel.playerTriggerUltimateAttack() },
                                modifier = Modifier.weight(1.2f).fillMaxHeight().testTag("combat_ultimate_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.LocalFireDepartment, "ultimate", tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("СУПЕРПРИЕМ", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                            }

                            Button(
                                onClick = { viewModel.playerTriggerHeal() },
                                modifier = Modifier.weight(1f).fillMaxHeight().testTag("combat_heal_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Spa, "heal", tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("ИСПРАВИТЬ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { viewModel.playerDefenseGuard() },
                                modifier = Modifier.weight(1.5f).fillMaxHeight().testTag("combat_guard_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = CardLighterBg),
                                border = BorderStroke(1.dp, FantasyGold.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Shield, "shield", tint = FantasyGold, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ГЛУХАЯ ОБОРОНА 🛡️", color = FantasyGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Button(
                                onClick = { viewModel.runFromBattle() },
                                modifier = Modifier.weight(1f).fillMaxHeight().testTag("combat_exit_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("ОТСТУПИТЬ", color = Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    // Enemy's turn banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Red.copy(alpha = 0.15f))
                            .border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Red)
                            Text(
                                "Атака порождения Бездны... Будьте наготове защищаться!",
                                color = Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

// QTE WIDGET 1: SHRINKING RIVAL RING
@Composable
fun AttackRingQteWidget(
    prompt: String,
    onQteFinish: (Float) -> Unit
) {
    // Shrinking radius animation from 1.0f factor down to 0.0f
    val animState = remember { Animatable(1.0f) }
    var hasFinished by remember { mutableStateOf(false) }

    LaunchedEffect(prompt) {
        hasFinished = false
        animState.snapTo(1.0f)
        animState.animateTo(
            targetValue = 0.0f,
            animationSpec = tween(1500, easing = LinearEasing)
        )
        // Hit zero trigger auto failure
        if (!hasFinished) {
            hasFinished = true
            onQteFinish(0.0f)
        }
    }

    val pct = animState.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(prompt, color = FantasyGold, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center)

        // Draw circles representation using canvas
        Box(
            modifier = Modifier
                .size(160.dp)
                .clickable {
                    if (!hasFinished) {
                        hasFinished = true
                        // Calculate score which is closeness to target ring zone (ideal around 0.25 to 0.35 factor)
                        val score = if (pct in 0.22f..0.38f) {
                            1.0f // Perfect
                        } else if (pct in 0.12f..0.50f) {
                            0.7f // Good
                        } else {
                            0.3f // Bad timing
                        }
                        onQteFinish(score)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val fullRadius = size.minDimension / 2f

                // 1. Target ideal zone ring (drawn in Gold)
                drawCircle(
                    color = FantasyGold,
                    radius = fullRadius * 0.3f,
                    style = Stroke(width = 6f)
                )

                // 2. Shrinking interactive ring (Cyan)
                drawCircle(
                    color = IceBlue,
                    radius = fullRadius * pct,
                    style = Stroke(width = 4f)
                )
            }

            Text(
                "ТАП!",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                fontFamily = FontFamily.SansSerif
            )
        }

        Text(
            "Старайтесь тапнуть, когда синий круг врежется в золотую мишень!",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

// QTE WIDGET 2: DIRECT SWIPE BLOCK WIDGET (TAP-DIRECTION CONTROL fallback)
@Composable
fun BlockSwipeQteWidget(
    targetArrow: String,
    prompt: String,
    durationMs: Long,
    onSwipeResult: (Boolean) -> Unit
) {
    val animState = remember { Animatable(1.0f) }

    LaunchedEffect(prompt) {
        animState.snapTo(1.0f)
        animState.animateTo(
            targetValue = 0.0f,
            animationSpec = tween(durationMs.toInt(), easing = LinearEasing)
        )
    }

    val progressValue = animState.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(prompt, color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center)
            
            LinearProgressIndicator(
                progress = { progressValue },
                modifier = Modifier
                    .fillMaxWidth(0.81f)
                    .height(6.dp),
                color = if (progressValue > 0.4f) FantasyGold else Color.Red,
                trackColor = Color.White.copy(alpha = 0.12f)
            )
        }

        // Active directional triggers
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onSwipeResult(targetArrow == "UP") },
                    colors = ButtonDefaults.buttonColors(containerColor = if (targetArrow == "UP") FantasyGold else CardLighterBg)
                ) {
                    Text("ВВЕРХ ⬆️", color = if (targetArrow == "UP") Color.Black else Color.White)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onSwipeResult(targetArrow == "LEFT") },
                        colors = ButtonDefaults.buttonColors(containerColor = if (targetArrow == "LEFT") FantasyGold else CardLighterBg)
                    ) {
                        Text("ЛЕВО ⬅️", color = if (targetArrow == "LEFT") Color.Black else Color.White)
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.Black, CircleShape)
                            .border(1.dp, Color.Red, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Shield, "shield", tint = Color.Red)
                    }

                    Button(
                        onClick = { onSwipeResult(targetArrow == "RIGHT") },
                        colors = ButtonDefaults.buttonColors(containerColor = if (targetArrow == "RIGHT") FantasyGold else CardLighterBg)
                    ) {
                        Text("ПРАВО ➡️", color = if (targetArrow == "RIGHT") Color.Black else Color.White)
                    }
                }

                Button(
                    onClick = { onSwipeResult(targetArrow == "DOWN") },
                    colors = ButtonDefaults.buttonColors(containerColor = if (targetArrow == "DOWN") FantasyGold else CardLighterBg)
                ) {
                    Text("ВНИЗ ⬇️", color = if (targetArrow == "DOWN") Color.Black else Color.White)
                }
            }
        }

        Text(
            "Быстро нажмите подсвеченную золотую стрелку направления для блокирования!",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}
