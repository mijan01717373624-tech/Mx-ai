package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import java.util.Locale
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.EmailItem
import com.example.data.MXRepository
import com.example.data.MXViewModel
import com.example.data.ScheduleItem
import com.example.data.VMFactory
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Room db initialization
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "mx_ai_secure_database"
        ).fallbackToDestructiveMigration().build()

        val repository = MXRepository(
            db.scheduleDao(),
            db.emailDao(),
            db.securityLogDao()
        )
        val vmFactory = VMFactory(repository)

        setContent {
            MyApplicationTheme {
                val viewModel: MXViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = vmFactory)
                MainAppScreen(viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: MXViewModel) {
    val isLocked by viewModel.isBiometricLocked.collectAsStateWithLifecycle()
    val isScanning by viewModel.isBiometricScanning.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = isLocked,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                },
                label = "BiometricLockTransition"
            ) { locked ->
                if (locked) {
                    BiometricLockScreen(
                        isScanning = isScanning,
                        onScanTrigger = {
                            viewModel.authenticateBiometric()
                        }
                    )
                } else {
                    DashboardScreen(viewModel)
                }
            }
        }
    }
}

// ------------------------------
// BIOMETRIC AUTHENTICATION SCREEN
// ------------------------------
@Composable
fun BiometricLockScreen(
    isScanning: Boolean,
    onScanTrigger: () -> Unit
) {
    // Elegant pulsing backgrounds & glowing states
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    val scanProgress by animateFloatAsState(
        targetValue = if (isScanning) 1f else 0f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "ScanProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP: Branding & Lock Alert Badge
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SecureBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock Shield indicator",
                        tint = SecureText,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "MX AI",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.5).sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DynamicCapsuleBg,
                border = BorderStroke(1.dp, DynamicBorder),
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(StatusRed)
                    )
                    Text(
                        text = "SECURE LOCAL CONTAINER LOCKED",
                        color = AccentBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // MIDDLE: Scan Fingerprint Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(240.dp)
                    .scale(if (isScanning) 1.0f else pulseScale)
            ) {
                // Glow circles
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(30.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    OrbGradientStart.copy(alpha = 0.5f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Outer border circle
                Canvas(modifier = Modifier.size(180.dp)) {
                    drawCircle(
                        color = BorderDark,
                        radius = size.minDimension / 2,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    )
                }

                // Interactive Scanning core finger pad
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(SurfaceDark, BackgroundDark)
                            )
                        )
                        .border(
                            2.dp,
                            if (isScanning) StatusGreen else BorderDark,
                            CircleShape
                        )
                        .clickable(
                            enabled = !isScanning,
                            onClick = onScanTrigger
                        )
                        .testTag("biometric_fingerprint_scanner"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secure Key Indicator",
                        tint = if (isScanning) StatusGreen else TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )

                    // Laser scan bar simulation
                    if (isScanning) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color.Transparent, StatusGreen, Color.Transparent)
                                    )
                                )
                                .align(Alignment.TopCenter)
                                .offset(y = (scanProgress * 130).dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isScanning) "Verifying Biometric Master Token..." else "Tap to Authenticate Biometrics",
                color = if (isScanning) StatusGreen else TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Light
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sheel of my phone is locked down with biometric authentication and encrypted local storage.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        // BOTTOM: Security hardware information
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, BorderDark),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info info",
                    tint = AccentBlue,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Hardware Security Bounds Active",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "AES-256 database protection, restricted process sandbox routing, and hardware key attestation are live on this device.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

// ------------------------------
// DASHBOARD COMPANION MULTI-VIEW
// ------------------------------
@Composable
fun DashboardScreen(viewModel: MXViewModel) {
    var selectedTab by remember { mutableStateOf("ASSISTANT") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Dynamic Top Status Header (as described in Design HTML)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SecureBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secure Blue Seal",
                        tint = SecureText,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column {
                    Text(
                        text = "MX AI",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Siri Mode Active",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            // Secure Status Capsular Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .background(DynamicCapsuleBg, RoundedCornerShape(20.dp))
                    .border(1.dp, DynamicBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(StatusGreen)
                )
                Text(
                    text = "SECURE",
                    color = AccentBlue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        // Horizontal Navigation Tabs (Pills)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabs = listOf(
                "ASSISTANT" to Icons.Default.PlayArrow,
                "SCHEDULES" to Icons.Default.DateRange,
                "EMAILS" to Icons.Default.Email,
                "SECURITY" to Icons.Default.Lock
            )

            tabs.forEach { (tabId, icon) ->
                val isActive = selectedTab == tabId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isActive) SurfaceDark else Color.Transparent)
                        .border(
                            1.dp,
                            if (isActive) BorderDark else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedTab = tabId }
                        .padding(vertical = 10.dp)
                        .testTag("tab_button_$tabId"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tabId,
                            tint = if (isActive) AccentBlue else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = tabId,
                            color = if (isActive) TextPrimary else TextSecondary,
                            fontSize = 8.6.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Divider(
            color = BorderDark,
            thickness = 1.dp,
            modifier = Modifier.padding(top = 10.dp)
        )

        // MAIN TAB BOUND DETAILS
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                "ASSISTANT" -> AssistantTabScreen(viewModel)
                "SCHEDULES" -> SchedulesTabScreen(viewModel)
                "EMAILS" -> EmailsTabScreen(viewModel)
                "SECURITY" -> SecurityTabScreen(viewModel)
            }
        }

        // Bottom Fast Action bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars),
            color = BackgroundDark
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Symmetric Keys: AES-256 Enabled",
                    color = TextSecondary,
                    fontSize = 11.sp,
                )
                Text(
                    text = "🔒 BIOMETRICS RUNNING",
                    color = StatusGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ------------------------------
// TAB 1: SIRI VOICE ASSISTANT
// ------------------------------
@Composable
fun AssistantTabScreen(viewModel: MXViewModel) {
    val assistantText by viewModel.assistantSpeechText.collectAsStateWithLifecycle()
    val isListening by viewModel.isSiriListening.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSiriSpeaking.collectAsStateWithLifecycle()
    var rawTextCommand by remember { mutableStateOf("") }

    // Siri Orb pulse calculations
    val infiniteTransition = rememberInfiniteTransition(label = "SiriPulse")
    val orbSizeAnimation by infiniteTransition.animateFloat(
        initialValue = 135f,
        targetValue = if (isListening) 165f else 145f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbSize"
    )

    val orbRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP: AI response bubble
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderDark),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isListening) StatusYellow else StatusGreen)
                            )
                            Text(
                                text = if (isListening) "SIRI LISTENING..." else "MX AI SYNTHESIS ACTIVE",
                                fontSize = 11.sp,
                                color = AccentBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Details indicator",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = assistantText,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.align(Alignment.End),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AES Container Locked",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(SecureBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint = SecureText,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // MIDDLE: Siri orb interface
        item {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(vertical = 24.dp)
                    .size(200.dp)
            ) {
                // Outer rotating dynamic aura
                Box(
                    modifier = Modifier
                        .size(orbSizeAnimation.dp)
                        .blur(if (isListening) 12.dp else 24.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    OrbGradientStart,
                                    OrbGradientEnd,
                                    OrbPulseGlow,
                                    OrbGradientStart
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(orbSizeAnimation, orbSizeAnimation)
                            )
                        )
                )

                // Outer boundary details
                Canvas(modifier = Modifier.size(170.dp)) {
                    drawCircle(
                        color = AccentBlue.copy(alpha = 0.35f),
                        radius = size.minDimension / 2,
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), orbRotation)
                        )
                    )
                }

                // Core speech bubble microphone trigger
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(BackgroundDark)
                        .border(1.5.dp, BorderDark, CircleShape)
                        .clickable {
                            if (isListening) {
                                viewModel.stopListeningAndSubmit("read last email")
                            } else {
                                viewModel.startListening()
                            }
                        }
                        .testTag("siri_mic_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow, // Microphone node surrogate
                            contentDescription = "Trigger Speech Listen",
                            tint = if (isListening) StatusYellow else AccentBlue,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = if (isListening) "STOP" else "SPEAK",
                            fontSize = 11.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // INTERACTIVE VOICE COMMAND INJECTORS (SIMULATOR FOR EMULATOR)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "SIMULATED VOICE COMMANDS (SIRI PRESETS):",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val suggestions = listOf(
                    "schedule Strategy Call at 3:30 PM",
                    "read last email",
                    "lock computer",
                    "rotate security keys"
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestions.forEach { command ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SurfaceDark,
                            border = BorderStroke(1.dp, BorderDark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.handleCommand(command)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "\"$command\"",
                                    fontSize = 13.sp,
                                    color = AccentBlue,
                                    fontStyle = FontStyle.Italic
                                )
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play icon",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // MANUAL TYPE FORM
        item {
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = rawTextCommand,
                onValueChange = { rawTextCommand = it },
                label = { Text("Or Type Siri command explicitly...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentBlue,
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = BorderDark,
                    focusedLabelColor = AccentBlue,
                    unfocusedLabelColor = TextSecondary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (rawTextCommand.isNotBlank()) {
                                viewModel.handleCommand(rawTextCommand)
                                rawTextCommand = ""
                            }
                        },
                        modifier = Modifier.testTag("submit_written_prompt")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Send text Command",
                            tint = AccentBlue
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ------------------------------
// TAB 2: SCHEDULE MANAGER
// ------------------------------
@Composable
fun SchedulesTabScreen(viewModel: MXViewModel) {
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    var showAddForm by remember { mutableStateOf(false) }

    var inputTitle by remember { mutableStateOf("") }
    var inputTime by remember { mutableStateOf("01:00 PM") }
    var inputDesc by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Secured Schedules",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Filtered by strict sandboxing protocols",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Button(
                    onClick = { showAddForm = !showAddForm },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceDark,
                        contentColor = AccentBlue
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.testTag("toggle_add_schedule_button")
                ) {
                    Text(if (showAddForm) "Close" else "Add Schedule")
                }
            }
        }

        if (showAddForm) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, BorderDark),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "New Schedule Input parameters",
                            color = AccentBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = inputTitle,
                            onValueChange = { inputTitle = it },
                            label = { Text("Task Title (e.g., Code Review)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = BorderDark
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("schedule_title_input")
                        )

                        OutlinedTextField(
                            value = inputTime,
                            onValueChange = { inputTime = it },
                            label = { Text("Time (e.g., 10:00 AM)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = BorderDark
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("schedule_time_input")
                        )

                        OutlinedTextField(
                            value = inputDesc,
                            onValueChange = { inputDesc = it },
                            label = { Text("Description") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = BorderDark
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("schedule_desc_input")
                        )

                        Button(
                            onClick = {
                                if (inputTitle.isNotBlank()) {
                                    viewModel.addSchedule(inputTitle, inputTime, inputDesc)
                                    inputTitle = ""
                                    inputDesc = ""
                                    showAddForm = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("submit_schedule_button")
                        ) {
                            Text("Deploy securely to Local DB", color = SecureText)
                        }
                    }
                }
            }
        }

        if (schedules.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No schedules recorded locally.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(schedules) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, if (item.completed) StatusGreen.copy(alpha = 0.3f) else BorderDark),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("schedule_item_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Checkbox custom
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (item.completed) StatusGreen.copy(alpha = 0.15f) else Color.Transparent)
                                .border(
                                    1.5.dp,
                                    if (item.completed) StatusGreen else TextSecondary,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { viewModel.toggleScheduleCompleted(item) }
                                .testTag("schedule_checkbox"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.completed) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = StatusGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = item.time,
                                    fontSize = 11.sp,
                                    color = AccentBlue,
                                    fontWeight = FontWeight.Bold
                                )

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = BackgroundDark,
                                    modifier = Modifier.padding(2.dp)
                                ) {
                                    Text(
                                        text = item.category,
                                        fontSize = 8.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = item.title,
                                fontSize = 16.sp,
                                color = if (item.completed) TextSecondary else Color.White,
                                fontWeight = FontWeight.SemiBold,
                                style = LocalTextStyle.current.copy(
                                    textDecoration = if (item.completed) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                )
                            )

                            if (item.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.description,
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    maxLines = 2
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.deleteSchedule(item.id, item.title) },
                            modifier = Modifier.testTag("delete_schedule_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Purge record",
                                tint = StatusRed.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------
// TAB 3: SECURED EMAILS HUB
// ------------------------------
@Composable
fun EmailsTabScreen(viewModel: MXViewModel) {
    val emails by viewModel.emails.collectAsStateWithLifecycle()
    var expandedEmailId by remember { mutableStateOf<Int?>(null) }

    var filterSecOnly by remember { mutableStateOf(false) }

    // Forms block to simulate incoming emails easily
    var showSimulatorInboundForm by remember { mutableStateOf(false) }
    var inSender by remember { mutableStateOf("risk-alert@mx.ai") }
    var inSubject by remember { mutableStateOf("Intruder detection block") }
    var inBody by remember { mutableStateOf("A high-risk access route attempt was logged at port 2402. Isolated successfully by sandbox perimeter containment.") }
    var inCritical by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Filtered Senders Confinement",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Analyzing cryptographic email signatures",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    IconButton(
                        onClick = { showSimulatorInboundForm = !showSimulatorInboundForm },
                        modifier = Modifier.testTag("sim_inbound_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Simulate email inflow",
                            tint = AccentBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val allActive = !filterSecOnly
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (allActive) SurfaceDark else Color.Transparent,
                        border = BorderStroke(1.dp, if (allActive) AccentBlue else BorderDark),
                        modifier = Modifier
                            .clickable { filterSecOnly = false }
                            .testTag("filter_all_emails")
                    ) {
                        Text(
                            text = "All Messages",
                            color = if (allActive) AccentBlue else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (filterSecOnly) SurfaceDark else Color.Transparent,
                        border = BorderStroke(1.dp, if (filterSecOnly) StatusRed else BorderDark),
                        modifier = Modifier
                            .clickable { filterSecOnly = true }
                            .testTag("filter_critical_emails")
                    ) {
                        Text(
                            text = "Critical Updates",
                            color = if (filterSecOnly) StatusRed else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        if (showSimulatorInboundForm) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, BorderDark),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Interactive Incoming Email simulator",
                            color = AccentBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = inSender,
                            onValueChange = { inSender = it },
                            label = { Text("Sender") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                            modifier = Modifier.fillMaxWidth().testTag("email_sender_input")
                        )

                        OutlinedTextField(
                            value = inSubject,
                            onValueChange = { inSubject = it },
                            label = { Text("Subject") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                            modifier = Modifier.fillMaxWidth().testTag("email_subject_input")
                        )

                        OutlinedTextField(
                            value = inBody,
                            onValueChange = { inBody = it },
                            label = { Text("Email Content Body") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                            modifier = Modifier.fillMaxWidth().testTag("email_body_input")
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = inCritical,
                                onCheckedChange = { inCritical = it },
                                colors = CheckboxDefaults.colors(checkedColor = StatusRed),
                                modifier = Modifier.testTag("email_critical_checkbox")
                            )
                            Text("Mark as Highly Critical (AES Isolated)", color = TextPrimary, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (inSender.isNotBlank()) {
                                    val previewText = if (inBody.length > 50) inBody.take(47) + "..." else inBody
                                    viewModel.addEmail(inSender, inSubject, previewText, inBody, inCritical)
                                    showSimulatorInboundForm = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("submit_sim_email")
                        ) {
                            Text("Receive simulated encrypted transmission", color = SecureText)
                        }
                    }
                }
            }
        }

        val filteredEmails = if (filterSecOnly) emails.filter { it.isCritical } else emails

        if (filteredEmails.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No filtered email matches this query.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(filteredEmails) { email ->
                val isExpanded = expandedEmailId == email.id
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(
                        1.dp,
                        if (email.isCritical) StatusRed.copy(alpha = 0.5f) else BorderDark
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedEmailId = if (isExpanded) null else email.id
                            viewModel.markEmailReadState(email, true)
                        }
                        .testTag("email_card_item")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Dynamic critical badge
                                if (email.isCritical) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterVertically)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(StatusRed)
                                    )
                                } else if (!email.isRead) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterVertically)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(AccentBlue)
                                    )
                                }

                                Column {
                                    Text(
                                        text = email.sender,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (email.isCritical) StatusRed else AccentBlue
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = email.subject,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }

                            // Read details tag
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BackgroundDark
                            ) {
                                Text(
                                    text = if (email.isCritical) "CRITICAL" else "FILTERED",
                                    fontSize = 8.sp,
                                    color = if (email.isCritical) StatusRed else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        AnimatedContent(
                            targetState = isExpanded,
                            label = "ExpandedDescription"
                        ) { expanded ->
                            if (expanded) {
                                Column {
                                    Text(
                                        text = email.body,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Encryption: AES-256 Symmetric-Key Locked",
                                            fontSize = 10.sp,
                                            color = StatusGreen
                                        )

                                        IconButton(
                                            onClick = { viewModel.deleteEmail(email.id, email.subject) },
                                            modifier = Modifier.size(24.dp).testTag("delete_email_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Wipe email secure",
                                                tint = StatusRed,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = email.preview,
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------
// TAB 4: SECURITY SYSTEM CENTER
// ------------------------------
@Composable
fun SecurityTabScreen(viewModel: MXViewModel) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val isAES by viewModel.isAESEncrypted.collectAsStateWithLifecycle()
    val isSandbox by viewModel.isSandboxActive.collectAsStateWithLifecycle()
    val rotationTime by viewModel.lastRotationTime.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP HEADER
        item {
            Column {
                Text(
                    text = "MX Security Service Suite",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Active protocols & cryptographic key signatures",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        // CONTROL CARD: TOGGLES
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderDark),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "HARDWARE INTEGRITY CONTROLS",
                        fontSize = 11.sp,
                        color = AccentBlue,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    // TOGGLE 1: AES 256
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "AES-256 Storage Relational DB", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                            Text(text = "Encrypt all schedules and emails on drive.", fontSize = 11.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = isAES,
                            onCheckedChange = { viewModel.setAESEncrypted(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = StatusGreen,
                                checkedTrackColor = StatusGreen.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.testTag("aes_security_toggle")
                        )
                    }

                    Divider(color = BorderDark)

                    // TOGGLE 2: SANDBOX ACTIVE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Rigid Sandbox Confinement", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                            Text(text = "Process isolation restricts outside apps reading storage.", fontSize = 11.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = isSandbox,
                            onCheckedChange = { viewModel.setSandboxActive(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = StatusGreen,
                                checkedTrackColor = StatusGreen.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.testTag("sandbox_security_toggle")
                        )
                    }

                    Divider(color = BorderDark)

                    // TOGGLE 3: BIOMETRIC AUTO RE-ENGAGE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Manual Shield Lockdown Engagement", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                            Text(text = "Lock app completely and demand biometric verifying.", fontSize = 11.sp, color = TextSecondary)
                        }
                        Button(
                            onClick = { viewModel.lockBiometrics() },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("immediate_lock_button")
                        ) {
                            Text("LOCK", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // CRYPTO ROTATION MECHANISM
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderDark),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = "SYMMETRIC KEY ROTATION PROTOCOL",
                        fontSize = 11.sp,
                        color = AccentBlue,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Rotate relational SQLite system salts. Generates a fresh hardware-attested master key mapped securely in Keystore.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Last Rotated State:", fontSize = 11.sp, color = TextSecondary)
                            Text(text = rotationTime, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.rotateEncryptionKeys() },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("rotate_keys_btn")
                        ) {
                            Text("Rotate Salt", color = SecureText)
                        }
                    }
                }
            }
        }

        // SERVICE PROTOCOLS DOCUMENTATION
        item {
            Text(
                text = "MX PRIVACY SERVICE ARCHITECTURE PROTOCOLS",
                color = AccentBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        val protocols = listOf(
            Triple("ESP", "Encrypted Storage Protocol", "Direct cryptographic seal binds localized SQLite using AES-256 blocks with unpredictable salts. Data cannot be extracted over ADB transfers."),
            Triple("BAH", "Biometric Authentication Handshake", "Strict secure-element binding. Disables memory caches automatically if authentications state expires or process gets suspended."),
            Triple("CSM", "Confinement Sandbox Process", "Process boundaries dictate that MX AI cannot launch unsecured intents or push state elements beyond user hardware restrictions.")
        )

        items(protocols) { (code, title, desc) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderDark),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SecureBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = code,
                            color = SecureText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = desc, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
                    }
                }
            }
        }

        // SECURITY TELEMETRY AUDIT LOG
        item {
            Text(
                text = "REAL-TIME CONTAINER TELEMETRY LOGS",
                color = AccentBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        if (logs.isEmpty()) {
            item {
                Text(text = "No runtime logs available.", color = TextSecondary, fontSize = 12.sp)
            }
        } else {
            items(logs) { log ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                when (log.status) {
                                    "SUCCESS" -> StatusGreen
                                    "SECURE" -> AccentBlue
                                    "PROTECTED" -> StatusGreen
                                    else -> StatusYellow
                                }
                            )
                    )

                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "[${log.type}]",
                                fontSize = 10.sp,
                                color = AccentBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = java.text.SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(java.util.Date(log.timestamp)),
                                fontSize = 9.sp,
                                color = TextSecondary
                            )
                        }
                        Text(
                            text = log.message,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
