package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.LocalServerHost
import com.example.data.local.LocalSession
import com.example.data.model.Member
import com.example.data.model.MemberFeeStatus
import com.example.ui.auth.AuthScreen
import com.example.ui.screens.BodybuilderProfileScreen
import com.example.ui.screens.CollectFeeDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DigitalMembershipCardDialog
import com.example.ui.screens.FinanceAndReportsScreen
import com.example.ui.screens.MemberAlertScreen
import com.example.ui.screens.MemberRegistrationScreen
import com.example.ui.screens.MembersScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OperationsScreen
import com.example.ui.screens.PinLockScreen
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CoolSilver
import com.example.ui.theme.CrimsonBright
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.GymFeeManagerTheme
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StatusDueSoonAmber
import com.example.ui.viewmodel.GymUiState
import com.example.ui.viewmodel.GymViewModel
import com.example.util.LocalizationStrings
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GymFeeManagerTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val localHost = remember(context) { LocalServerHost.getInstance(context) }
    val session: LocalSession? by localHost.authStore.sessionFlow.collectAsStateWithLifecycle(initialValue = null)
    var devTestMode by rememberSaveable { mutableStateOf(false) }

    val activeSession = session
    when {
        activeSession != null -> {
            LaunchedEffect(activeSession.appId) {
                devTestMode = false
            }
            key(activeSession.appId) {
                GymAuthenticatedApp(
                    currentUserId = activeSession.appId,
                    userEmail = localHost.httpServer.baseUrl,
                    userDisplayName = activeSession.appId.replaceFirstChar { it.uppercase() },
                    localHost = localHost,
                    isDevSandbox = false,
                    onSignOut = {
                        scope.launch { localHost.authStore.clearSession() }
                    },
                    onExitDevMode = {}
                )
            }
        }
        devTestMode -> {
            key("dev_sandbox") {
                GymAuthenticatedApp(
                    currentUserId = "dev_sandbox",
                    userEmail = "dev-sandbox@local.server",
                    userDisplayName = "Dev Tester",
                    localHost = localHost,
                    isDevSandbox = true,
                    onSignOut = { devTestMode = false },
                    onExitDevMode = { devTestMode = false }
                )
            }
        }
        else -> {
            AuthScreen(
                serverEndpoint = localHost.httpServer.baseUrl,
                onLogin = { appId, password ->
                    localHost.httpServer.loginViaServer(appId, password).map { Unit }
                },
                onEnterDevMode = { devTestMode = true }
            )
        }
    }
}

private sealed interface FullScreenOverlay {
    data object Notifications : FullScreenOverlay
    data class MemberAlert(val memberId: String) : FullScreenOverlay
    data class MemberForm(val member: Member?) : FullScreenOverlay
    data class Bodybuilder(val memberId: String?) : FullScreenOverlay
}

@Composable
fun GymAuthenticatedApp(
    currentUserId: String,
    userEmail: String?,
    userDisplayName: String?,
    localHost: LocalServerHost,
    isDevSandbox: Boolean = false,
    onSignOut: () -> Unit = {},
    onExitDevMode: () -> Unit = {}
) {
    val viewModel: GymViewModel = viewModel(
        key = if (isDevSandbox) "dev_sandbox" else currentUserId,
        factory = viewModelFactory {
            initializer {
                GymViewModel(
                    repository = if (isDevSandbox) null else localHost.repository,
                    currentUserId = currentUserId,
                    userEmail = userEmail,
                    userDisplayName = userDisplayName,
                    isDevSandbox = isDevSandbox
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val pinUnlocked by viewModel.pinUnlocked.collectAsStateWithLifecycle()
    val timeOffsetDays by viewModel.timeOffsetDays.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusBanner) {
        statusBanner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    var selectedTab by remember { mutableStateOf(0) }
    var opsInitialTab by remember { mutableStateOf("ATTENDANCE") }
    var overlay by remember { mutableStateOf<FullScreenOverlay?>(null) }
    var collectFeeTarget by remember { mutableStateOf<MemberFeeStatus?>(null) }
    var digitalCardMember by remember { mutableStateOf<Member?>(null) }

    when (val state = uiState) {
        is GymUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize().background(ObsidianBlack),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = CrimsonPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Connecting to Local Gym Server...", color = CoolSilver, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        is GymUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize().background(ObsidianBlack).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Local Server Error", style = MaterialTheme.typography.headlineMedium, color = CrimsonBright)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(state.message, color = CoolSilver, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onSignOut,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                    ) {
                        Text("Return to Login")
                    }
                }
            }
        }
        is GymUiState.Ready -> {
            val data = state.data
            val strings = LocalizationStrings.forLang(data.ownerProfile.language)

            if (!pinUnlocked) {
                Box(
                    modifier = Modifier.fillMaxSize().background(ObsidianBlack),
                    contentAlignment = Alignment.Center
                ) {
                    PinLockScreen(
                        gymName = data.ownerProfile.gymName,
                        onUnlock = { viewModel.unlockWithPin(it) }
                    )
                }
                return
            }

            when (val activeOverlay = overlay) {
                is FullScreenOverlay.Notifications -> {
                    NotificationsScreen(
                        data = data,
                        onBack = { overlay = null },
                        onOpenMemberAlert = { member -> overlay = FullScreenOverlay.MemberAlert(member.id) }
                    )
                }
                is FullScreenOverlay.MemberAlert -> {
                    val member = data.members.firstOrNull { it.id == activeOverlay.memberId }
                    if (member != null) {
                        MemberAlertScreen(
                            member = member,
                            data = data,
                            onBack = { overlay = null },
                            onCollectFee = { feeStatus -> collectFeeTarget = feeStatus },
                            onWhatsAppSent = { mem, amt -> viewModel.logWhatsAppReminder(mem, amt) },
                            onOpenDigitalCard = { mem -> digitalCardMember = mem },
                            onOpenBodybuilderProfile = { mem -> overlay = FullScreenOverlay.Bodybuilder(mem.id) },
                            onEditMember = { mem -> overlay = FullScreenOverlay.MemberForm(mem) }
                        )
                    } else {
                        overlay = null
                    }
                }
                is FullScreenOverlay.MemberForm -> {
                    MemberRegistrationScreen(
                        data = data,
                        existingMember = activeOverlay.member,
                        onBack = { overlay = null },
                        onSave = { member ->
                            viewModel.registerOrUpdateMember(member) { ok ->
                                if (ok) overlay = null
                            }
                        }
                    )
                }
                is FullScreenOverlay.Bodybuilder -> {
                    BodybuilderProfileScreen(
                        data = data,
                        initialMemberId = activeOverlay.memberId,
                        onBack = { overlay = null },
                        onAddProgressLog = { log -> viewModel.addBodyProgressLog(log) }
                    )
                }
                null -> {
                    val navItems = listOf(
                        Triple(0, strings.dashboard, Icons.Default.Dashboard),
                        Triple(1, strings.members, Icons.Default.Group),
                        Triple(2, strings.operations, Icons.Default.FitnessCenter),
                        Triple(3, strings.reports, Icons.Default.Assessment)
                    )

                    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(ObsidianBlack)) {
                        val useRail = maxWidth >= 680.dp

                        Scaffold(
                            containerColor = ObsidianBlack,
                            contentWindowInsets = WindowInsets.navigationBars,
                            snackbarHost = {
                                SnackbarHost(snackbarHostState) { snackbarData ->
                                    Snackbar(
                                        snackbarData = snackbarData,
                                        containerColor = CarbonElevated,
                                        contentColor = PureWhite
                                    )
                                }
                            },
                            bottomBar = {
                                if (!useRail) {
                                    NavigationBar(
                                        containerColor = CarbonSurface,
                                        contentColor = PureWhite
                                    ) {
                                        navItems.forEach { (idx, label, icon) ->
                                            NavigationBarItem(
                                                selected = selectedTab == idx,
                                                onClick = { selectedTab = idx },
                                                icon = { Icon(icon, contentDescription = label) },
                                                label = { Text(label, maxLines = 1) },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = CrimsonBright,
                                                    selectedTextColor = PureWhite,
                                                    indicatorColor = CrimsonContainer,
                                                    unselectedIconColor = CoolSilver,
                                                    unselectedTextColor = CoolSilver
                                                ),
                                                modifier = Modifier.testTag("nav_tab_$idx")
                                            )
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                                    .padding(innerPadding)
                            ) {
                                if (useRail) {
                                    NavigationRail(
                                        containerColor = CarbonSurface,
                                        contentColor = PureWhite
                                    ) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        navItems.forEach { (idx, label, icon) ->
                                            NavigationRailItem(
                                                selected = selectedTab == idx,
                                                onClick = { selectedTab = idx },
                                                icon = { Icon(icon, contentDescription = label) },
                                                label = { Text(label) },
                                                colors = NavigationRailItemDefaults.colors(
                                                    selectedIconColor = CrimsonBright,
                                                    selectedTextColor = PureWhite,
                                                    indicatorColor = CrimsonContainer,
                                                    unselectedIconColor = CoolSilver,
                                                    unselectedTextColor = CoolSilver
                                                )
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Column(modifier = Modifier.widthIn(max = 960.dp).fillMaxSize()) {
                                        if (isDevSandbox || timeOffsetDays != 0L) {
                                            DevSandboxTopBar(
                                                isDevSandbox = isDevSandbox,
                                                timeOffsetDays = timeOffsetDays,
                                                onStepPlusOne = { viewModel.shiftTestDays(1L) },
                                                onResetTime = { viewModel.resetTestTime() },
                                                onExitDevMode = onExitDevMode
                                            )
                                        }
                                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                            when (selectedTab) {
                                                0 -> DashboardScreen(
                                                    data = data,
                                                    onOpenNotifications = { overlay = FullScreenOverlay.Notifications },
                                                    onOpenMemberAlert = { member -> overlay = FullScreenOverlay.MemberAlert(member.id) },
                                                    onCollectFeeClick = { feeStatus -> collectFeeTarget = feeStatus },
                                                    onWhatsAppSent = { mem, amt -> viewModel.logWhatsAppReminder(mem, amt) },
                                                    onOpenRegisterMember = { overlay = FullScreenOverlay.MemberForm(null) },
                                                    onOpenAttendance = {
                                                        opsInitialTab = "ATTENDANCE"
                                                        selectedTab = 2
                                                    },
                                                    onOpenBodybuilder = { overlay = FullScreenOverlay.Bodybuilder(null) },
                                                    onSwitchLanguage = { newLang ->
                                                        val p = data.ownerProfile
                                                        viewModel.updateOwnerProfile(
                                                            p.gymName, p.ownerName, p.ownerPhone, newLang, p.activeBranchId, p.twoFactorPin
                                                        )
                                                    },
                                                    onSwitchBranch = { newBranchId ->
                                                        val p = data.ownerProfile
                                                        viewModel.updateOwnerProfile(
                                                            p.gymName, p.ownerName, p.ownerPhone, p.language, newBranchId, p.twoFactorPin
                                                        )
                                                    }
                                                )
                                                1 -> MembersScreen(
                                                    data = data,
                                                    onOpenMemberAlert = { member -> overlay = FullScreenOverlay.MemberAlert(member.id) },
                                                    onOpenRegistration = { overlay = FullScreenOverlay.MemberForm(null) }
                                                )
                                                2 -> OperationsScreen(
                                                    data = data,
                                                    initialTab = opsInitialTab,
                                                    onCheckIn = { member, method -> viewModel.checkInMember(member, method) },
                                                    onCheckOut = { attId -> viewModel.checkOutAttendance(attId) },
                                                    onSaveTrainer = { tr -> viewModel.saveTrainer(tr) },
                                                    onSaveWorkout = { wp -> viewModel.saveWorkoutPlan(wp) },
                                                    onSaveDiet = { dp -> viewModel.saveDietPlan(dp) },
                                                    onSaveSupplement = { sup -> viewModel.saveSupplement(sup) },
                                                    onSellSupplement = { sup, qty -> viewModel.recordSupplementSale(sup, qty) },
                                                    onOpenBodybuilderLab = { overlay = FullScreenOverlay.Bodybuilder(null) }
                                                )
                                                3 -> FinanceAndReportsScreen(
                                                    data = data,
                                                    onSaveExpense = { exp -> viewModel.saveExpense(exp) },
                                                    onSaveEquipment = { eq -> viewModel.saveEquipment(eq) },
                                                    onSaveBranch = { br -> viewModel.saveBranch(br) },
                                                    onUpdateSettings = { gName, oName, oPhone, lang, brId, pin ->
                                                        viewModel.updateOwnerProfile(gName, oName, oPhone, lang, brId, pin)
                                                    },
                                                    onBuildBackupJson = { viewModel.buildFullBackupJson(data) },
                                                    onSignOut = {
                                                        if (isDevSandbox) onExitDevMode() else onSignOut()
                                                    },
                                                    isDevSandbox = isDevSandbox,
                                                    timeOffsetDays = timeOffsetDays,
                                                    onShiftTestDays = { viewModel.shiftTestDays(it) },
                                                    onResetTestTime = { viewModel.resetTestTime() },
                                                    onResetSandboxData = { viewModel.resetSandboxData() },
                                                    onSeedSampleLocalData = { viewModel.seedSampleDataToLocalDb() },
                                                    serverEndpoint = localHost.httpServer.baseUrl
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

            collectFeeTarget?.let { target ->
                CollectFeeDialog(
                    feeStatus = target,
                    onDismiss = { collectFeeTarget = null },
                    onConfirm = { amount, discount, method, notes ->
                        viewModel.collectFee(
                            member = target.member,
                            amount = amount,
                            discount = discount,
                            paymentMethod = method,
                            forMonthLabel = target.cycleLabel,
                            dueDateMillis = target.dueDateMillis,
                            notes = notes
                        )
                        collectFeeTarget = null
                    }
                )
            }

            digitalCardMember?.let { mem ->
                DigitalMembershipCardDialog(
                    member = mem,
                    ownerProfile = data.ownerProfile,
                    onDismiss = { digitalCardMember = null }
                )
            }
        }
    }
}

@Composable
private fun DevSandboxTopBar(
    isDevSandbox: Boolean,
    timeOffsetDays: Long,
    onStepPlusOne: () -> Unit,
    onResetTime: () -> Unit,
    onExitDevMode: () -> Unit
) {
    val offsetTag = when {
        timeOffsetDays == 0L -> "Today"
        timeOffsetDays > 0L -> "+${timeOffsetDays}d"
        else -> "${timeOffsetDays}d"
    }
    Surface(
        color = StatusDueSoonAmber.copy(alpha = 0.14f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .border(1.dp, StatusDueSoonAmber.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .testTag("dev_sandbox_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Science,
                    contentDescription = "Developer Test Mode",
                    tint = StatusDueSoonAmber,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isDevSandbox) "DEV TEST MODE • SANDBOX ($offsetTag)" else "DEV TIME SHIFT ($offsetTag)",
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusDueSoonAmber,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onStepPlusOne,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("dev_banner_plus_1d")
                ) {
                    Text("+1d", style = MaterialTheme.typography.labelSmall, color = PureWhite)
                }
                if (timeOffsetDays != 0L) {
                    TextButton(
                        onClick = onResetTime,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("dev_banner_reset_time")
                    ) {
                        Text("Reset", style = MaterialTheme.typography.labelSmall, color = StatusDueSoonAmber)
                    }
                }
                if (isDevSandbox) {
                    TextButton(
                        onClick = onExitDevMode,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("dev_banner_exit")
                    ) {
                        Text("Exit", style = MaterialTheme.typography.labelSmall, color = CrimsonBright)
                    }
                }
            }
        }
    }
}
