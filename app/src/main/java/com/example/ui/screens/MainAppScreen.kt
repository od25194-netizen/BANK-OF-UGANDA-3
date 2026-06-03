package com.example.ui.screens

import android.widget.Toast
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BusinessAnswers
import com.example.model.CreditAssessmentResult
import com.example.model.Pillar
import com.example.model.RoadmapStep
import com.example.model.LoanMatch
import kotlinx.coroutines.launch
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.CreditViewModel
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import com.example.util.TranslationHelper
import android.util.Log

@Composable
fun MainAppScreen(
    viewModel: CreditViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val answers by viewModel.answers.collectAsState()
    val currentStep by viewModel.currentFormStep.collectAsState()
    val isAnalyzingMessage by viewModel.isAnalyzingMessage.collectAsState()
    val result by viewModel.assessmentResult.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val authLoading by viewModel.authLoading.collectAsState()
    val userEmail by viewModel.currentUserEmail.collectAsState()
    val currentLanguageCode by viewModel.currentLanguageCode.collectAsState()

    val context = LocalContext.current
    var showLanguageDialog by remember { mutableStateOf(false) }
    
    val profileImageUriString by viewModel.profileImageUriString.collectAsState()
    val businessLogoUriString by viewModel.businessLogoUriString.collectAsState()

    val profileImageUri = remember(profileImageUriString) {
        profileImageUriString?.let { android.net.Uri.parse(it) }
    }
    val businessLogoUri = remember(businessLogoUriString) {
        businessLogoUriString?.let { android.net.Uri.parse(it) }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setProfileImageUri(uri.toString())
        }
    }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setBusinessLogoUri(uri.toString())
        }
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguageCode = currentLanguageCode,
            onLanguageSelected = { viewModel.setLanguage(it) },
            onDismiss = { showLanguageDialog = false }
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
            when (screen) {
                AppScreen.Auth -> AuthScreen(
                    isLoading = authLoading,
                    errorMessage = authError,
                    currentLanguageCode = currentLanguageCode,
                    onShowLanguagePicker = { showLanguageDialog = true },
                    onSignIn = { email, password ->
                        viewModel.clearAuthError()
                        viewModel.signIn(email, password) {
                            Toast.makeText(context, "Welcome to SME CreditReady!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onSignUp = { email, password ->
                        viewModel.clearAuthError()
                        viewModel.signUp(email, password) {
                            Toast.makeText(context, "Welcome! Your SME CreditReady account is ready.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onForgotPassword = { email ->
                        viewModel.resetPassword(email) { success, msg ->
                            if (success) {
                                Toast.makeText(context, "A password reset email has been sent to your primary address.", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Error: ${msg ?: "Account reset fail"}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onClearError = { viewModel.clearAuthError() }
                )
                AppScreen.Landing -> LandingScreen(
                    userEmail = userEmail,
                    currentLanguageCode = currentLanguageCode,
                    onShowLanguagePicker = { showLanguageDialog = true },
                    onSignOut = { viewModel.signOut() },
                    onStart = { viewModel.startAssessment() },
                    onQuickDemo = { viewModel.quickDemo() }
                )
                AppScreen.Form -> IntakeFormScreen(
                    step = currentStep,
                    answers = answers,
                    onAnswersUpdated = { viewModel.updateAnswers(it) },
                    onBack = { viewModel.prevStep() },
                    onNext = { viewModel.nextStep() }
                )
                AppScreen.Loading -> LoadingScreen(message = isAnalyzingMessage)
                AppScreen.Results -> result?.let {
                    ResultsDashboardScreen(
                        result = it,
                        answers = answers,
                        viewModel = viewModel,
                        profileImageUri = profileImageUri,
                        businessLogoUri = businessLogoUri,
                        onUploadPhotoClick = { photoPickerLauncher.launch("image/*") },
                        onUploadLogoClick = { logoPickerLauncher.launch("image/*") },
                        onRetake = { viewModel.retake() }
                    )
                } ?: LoadingScreen(message = "Processing response...")
            }
        }
    }
}

// ================= Landing Screen =================

@Composable
fun LandingScreen(
    userEmail: String?,
    currentLanguageCode: String,
    onShowLanguagePicker: () -> Unit,
    onSignOut: () -> Unit,
    onStart: () -> Unit,
    onQuickDemo: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(ForestGreenDark, BackgroundLight),
                    startY = 0f,
                    endY = 800f
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Toolbar Row for User Session
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = TranslationHelper.getString("logged_in_as", currentLanguageCode),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray.copy(alpha = 0.8f)
                )
                Text(
                    text = userEmail ?: "Guest User",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            // Language picker trigger button
            LanguageSelectorButton(
                currentLanguageCode = currentLanguageCode,
                darkTheme = true,
                onClick = onShowLanguagePicker
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSignOut,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Sign Out",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(40.dp))

        // Badge: BoU 60th Context
        Card(
            colors = CardDefaults.cardColors(containerColor = GoldAccent.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(100.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Bank of Uganda Anniversary",
                    tint = GoldAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "BANK OF UGANDA • 60th ANNIVERSARY",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Logo Icon Frame
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(ForestGreenLight.copy(alpha = 0.2f))
                .border(2.dp, GoldAccent, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "SME CreditReady Symbol",
                tint = GoldAccent,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "SME CreditReady",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Your AI-Powered Loan Eligibility & Development Coach",
            style = MaterialTheme.typography.titleMedium,
            color = GoldAccentLight,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Rejection statistic card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "80%+",
                    style = MaterialTheme.typography.displayMedium,
                    color = RatingRed,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "of Ugandan SMEs are rejected for formal credit. We build the exact step-by-step roadmap to make your enterprise loan-ready.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDark,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Main action button
        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("get_started_btn"),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = TranslationHelper.getString("btn_start_assess", currentLanguageCode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Start Assessment",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Demo presenter card for live presentation
        Card(
            colors = CardDefaults.cardColors(containerColor = ForestGreenLight),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onQuickDemo() }
                .testTag("quick_demo_card")
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GoldAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Live Demo Mode",
                        tint = ForestGreenDark
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = TranslationHelper.getString("btn_quick_demo", currentLanguageCode),
                        style = MaterialTheme.typography.titleMedium,
                        color = ForestGreenDark,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Simulate Maria Nakato's clothing shop challenge immediately – perfect for live judges!",
                        style = MaterialTheme.typography.bodySmall,
                        color = ForestGreenDark.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Explainer Process Stepper
        Text(
            text = "HOW IT WORKS",
            style = MaterialTheme.typography.labelLarge,
            color = TextDark,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.3.sp,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StepItem(
                number = "1",
                title = "Conversational Intake Form",
                description = "Answer a few easy, financial-literacy aligned questions in under 3 minutes about daily cash limits and records."
            )
            StepItem(
                number = "2",
                title = "AI Scoring Engine",
                description = "Our analyzer reviews your capacity and formulates solid credit pillars using the Bank of Uganda regulations."
            )
            StepItem(
                number = "3",
                title = "Personalized Coaching Roadmap",
                description = "Get detailed matches to Ugandan microfinance lenders, solid SACCO groups, and the step-by-step steps to improve."
            )
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun StepItem(number: String, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(ForestGreen),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextDark,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextDark.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

// ================= Intake Form Screen =================

@Composable
fun IntakeFormScreen(
    step: Int,
    answers: BusinessAnswers,
    onAnswersUpdated: ((BusinessAnswers) -> BusinessAnswers) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ForestGreen
                        )
                    }
                    Text(
                        text = "Credit Readiness Coach",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen
                    )
                    Text(
                        text = "$step of 4",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccentDark
                    )
                }
                // Custom dynamic progress slider
                LinearProgressIndicator(
                    progress = { step / 4f },
                    modifier = Modifier.fillMaxWidth(),
                    color = ForestGreen,
                    trackColor = ForestGreenLight
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                when (step) {
                    1 -> StepAboutBusiness(answers, onAnswersUpdated)
                    2 -> StepMoneyDetails(answers, onAnswersUpdated)
                    3 -> StepRecordDetails(answers, onAnswersUpdated)
                    4 -> StepLoanGoal(answers, onAnswersUpdated)
                }
            }

            Column(modifier = Modifier.padding(top = 32.dp)) {
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("form_next_submit_btn")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (step == 4) "Calculate Readiness Score" else "Continue",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (step == 4) Icons.Default.ArrowForward else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Step",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

// Form Step 1 Compose
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StepAboutBusiness(
    answers: BusinessAnswers,
    onAnswersUpdated: ((BusinessAnswers) -> BusinessAnswers) -> Unit
) {
    Text(
        text = "Tell us about your business",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = TextDark
    )
    Text(
        text = "This allows our AI to contextualize your industry and operating complexity.",
        style = MaterialTheme.typography.bodyMedium,
        color = TextDark.copy(alpha = 0.6f),
        modifier = Modifier.padding(bottom = 24.dp)
    )

    // Business Name
    Text(
        text = "Registered or Trade Name",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = TextDark,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    OutlinedTextField(
        value = answers.businessName,
        onValueChange = { name -> onAnswersUpdated { it.copy(businessName = name) } },
        placeholder = { Text("e.g. Maria Nakato Clothes, Juma Bodaboda Services") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
            .testTag("business_name_input"),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ForestGreen,
            unfocusedBorderColor = Color.LightGray
        )
    )

    // Business Type Dropdown/Selection Grid
    Text(
        text = "Category of Operation",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = TextDark,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    val categories = listOf(
        "market vendor", "retail shop", "salon/barbershop", "boda boda/transport", "farming/agriculture", 
        "food/restaurant", "clothing boutique", "hardware retail", "mechanic/auto repair", "medical clinic/pharmacy", 
        "carpentry/furniture shop", "brick making/construction", "general merchandise", "other"
    )
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { cat ->
            val isSelected = answers.businessType == cat
            FilterChip(
                selected = isSelected,
                onClick = { onAnswersUpdated { it.copy(businessType = cat) } },
                label = { Text(cat.uppercase()) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ForestGreen,
                    selectedLabelColor = Color.White,
                    containerColor = ForestGreenLight.copy(alpha = 0.3f),
                    labelColor = ForestGreenDark
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }

    // Operating Duration
    Text(
        text = "How long have you been operating?",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = TextDark,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    val times = listOf("less than 6 months", "6–12 months", "1–3 years", "3+ years")
    times.forEach { time ->
        val isSelected = answers.operatingTime == time
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) ForestGreen.copy(alpha = 0.1f) else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (isSelected) ForestGreen else Color.LightGray,
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { onAnswersUpdated { it.copy(operatingTime = time) } }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = { onAnswersUpdated { it.copy(operatingTime = time) } },
                colors = RadioButtonDefaults.colors(selectedColor = ForestGreen)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = time, style = MaterialTheme.typography.bodyMedium, color = TextDark)
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Is Registered Toggle
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GoldAccentLight)
            .border(1.dp, GoldAccent, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Registered with URSB?",
                style = MaterialTheme.typography.titleMedium,
                color = ForestGreenDark,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Ugandan Registration Services Bureau license ensures official matching.",
                style = MaterialTheme.typography.bodySmall,
                color = TextDark.copy(alpha = 0.8f)
            )
        }
        Switch(
            checked = answers.isRegistered,
            onCheckedChange = { reg -> onAnswersUpdated { it.copy(isRegistered = reg) } },
            colors = SwitchDefaults.colors(checkedThumbColor = ForestGreen, checkedTrackColor = ForestGreenLight)
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Operating District (Uganda Region Grouped) - 135 Districts
    Text(
        text = "Operating District (Uganda Region Grouped)",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = TextDark,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    var expandedDistrictMenu by remember { mutableStateOf(false) }
    var districtSearchQuery by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        OutlinedTextField(
            value = answers.district,
            onValueChange = {},
            readOnly = true,
            placeholder = { Text("Select operating district...") },
            trailingIcon = {
                IconButton(onClick = { expandedDistrictMenu = !expandedDistrictMenu }) {
                    Icon(
                        imageVector = if (expandedDistrictMenu) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Dropdown menu icon"
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expandedDistrictMenu = true },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ForestGreen,
                unfocusedBorderColor = Color.LightGray
            )
        )

        DropdownMenu(
            expanded = expandedDistrictMenu,
            onDismissRequest = { expandedDistrictMenu = false },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 350.dp)
                .background(Color.White)
        ) {
            OutlinedTextField(
                value = districtSearchQuery,
                onValueChange = { districtSearchQuery = it },
                placeholder = { Text("Search 135 districts...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray, modifier = Modifier.size(16.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ForestGreen,
                    unfocusedBorderColor = Color.LightGray
                ),
                singleLine = true
            )

            ugandaDistrictsByRegion.forEach { (region, list) ->
                val filtered = list.filter { it.contains(districtSearchQuery, ignoreCase = true) }
                if (filtered.isNotEmpty()) {
                    Text(
                        text = region.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = GoldAccentDark,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                    filtered.forEach { dist ->
                        DropdownMenuItem(
                            text = { Text(dist, fontWeight = if (answers.district == dist) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                onAnswersUpdated { it.copy(district = dist) }
                                expandedDistrictMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

// Form Step 2 Compose
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StepMoneyDetails(
    answers: BusinessAnswers,
    onAnswersUpdated: ((BusinessAnswers) -> BusinessAnswers) -> Unit
) {
    Text(
        text = "Your business cash flow",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = TextDark
    )
    Text(
        text = "Estimated turnovers allow us to assess your repayment capability.",
        style = MaterialTheme.typography.bodyMedium,
        color = TextDark.copy(alpha = 0.6f),
        modifier = Modifier.padding(bottom = 24.dp)
    )

    // Estimated monthly revenue range
    Text(
        text = "Monthly Revenue (UGX Slider)",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = TextDark,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    val ranges = listOf("below 500k", "500k–2M", "2M–5M", "5M–20M", "above 20M")
    ranges.forEach { range ->
        val isSelected = answers.monthlyRevenue == range
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) ForestGreen.copy(alpha = 0.1f) else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (isSelected) ForestGreen else Color.LightGray,
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { onAnswersUpdated { it.copy(monthlyRevenue = range) } }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = { onAnswersUpdated { it.copy(monthlyRevenue = range) } },
                colors = RadioButtonDefaults.colors(selectedColor = ForestGreen)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "$range UGX", style = MaterialTheme.typography.bodyMedium, color = TextDark)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Do you use mobile money
    Text(
        text = "Mobile Money for Collections",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = TextDark,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    val mmoProviders = listOf(
        "mtn" to "MTN Mobile Money",
        "airtel" to "Airtel Money",
        "both" to "Both MTN & Airtel",
        "no" to "No Mobile Money (Cash Only)"
    )
    mmoProviders.forEach { (key, label) ->
        val isSelected = answers.mobileMoneyUsage == key
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) ForestGreen.copy(alpha = 0.1f) else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (isSelected) ForestGreen else Color.LightGray,
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { onAnswersUpdated { it.copy(mobileMoneyUsage = key) } }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = { onAnswersUpdated { it.copy(mobileMoneyUsage = key) } },
                colors = RadioButtonDefaults.colors(selectedColor = ForestGreen)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextDark)
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Corporate Bank Account Toggle
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ForestGreenLight.copy(alpha = 0.4f))
            .border(1.dp, ForestGreen.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Have Business Bank Account?",
                style = MaterialTheme.typography.titleMedium,
                color = ForestGreenDark,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Allows submitting official commercial statement metrics.",
                style = MaterialTheme.typography.bodySmall,
                color = TextDark.copy(alpha = 0.8f)
            )
        }
        Switch(
            checked = answers.hasBankStatement,
            onCheckedChange = { bank -> onAnswersUpdated { it.copy(hasBankStatement = bank) } },
            colors = SwitchDefaults.colors(checkedThumbColor = ForestGreen, checkedTrackColor = ForestGreenLight)
        )
    }
}

// Form Step 3 Compose
@Composable
fun StepRecordDetails(
    answers: BusinessAnswers,
    onAnswersUpdated: ((BusinessAnswers) -> BusinessAnswers) -> Unit
) {
    Text(
        text = "Records and borrowing history",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = TextDark
    )
    Text(
        text = "Banks need verification of sales. Your history will tell us if SACCOs are a better fit.",
        style = MaterialTheme.typography.bodyMedium,
        color = TextDark.copy(alpha = 0.6f),
        modifier = Modifier.padding(bottom = 24.dp)
    )

    // Bookkeeping records
    Text(
        text = "How do you track sales/expenses?",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = TextDark,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    val loggingOptions = listOf(
        "yes always" to "Strict Written Ledgers / Apps (Daily)",
        "sometimes" to "Sometimes Written down (Weekly/Monthly)",
        "no" to "No Written Records / Keep in Memory"
    )
    loggingOptions.forEach { (key, label) ->
        val isSelected = answers.recordKeeping == key
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) ForestGreen.copy(alpha = 0.1f) else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (isSelected) ForestGreen else Color.LightGray,
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { onAnswersUpdated { it.copy(recordKeeping = key) } }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = { onAnswersUpdated { it.copy(recordKeeping = key) } },
                colors = RadioButtonDefaults.colors(selectedColor = ForestGreen)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextDark)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Loan History status
    Text(
        text = "Previous Borrowing History",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = TextDark,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    val historyOptions = listOf(
        "yes and repaid fully" to "Taken loans and REPAID completely on time",
        "yes with difficulties" to "Taken loans with slight delays/difficulties",
        "yes and defaulted" to "Taken loans and defaulted",
        "never" to "I have NEVER borrowed before"
    )
    historyOptions.forEach { (key, label) ->
        val isSelected = answers.loanHistory == key
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) ForestGreen.copy(alpha = 0.1f) else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (isSelected) ForestGreen else Color.LightGray,
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { onAnswersUpdated { it.copy(loanHistory = key) } }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = { onAnswersUpdated { it.copy(loanHistory = key) } },
                colors = RadioButtonDefaults.colors(selectedColor = ForestGreen)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextDark)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Savings habits
    Text(
        text = "Where do you save money?",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = TextDark,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    val savingsOptions = listOf(
        "yes in bank" to "Regular Savings Account in Commercial Bank",
        "yes in SACCO/group" to "Local SACCO / VSLA Group Savings",
        "yes at home" to "Cash at Home / Mobile Money Lock",
        "no" to "Currently unable to save regularly"
    )
    savingsOptions.forEach { (key, label) ->
        val isSelected = answers.savingsHabit == key
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) ForestGreen.copy(alpha = 0.1f) else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (isSelected) ForestGreen else Color.LightGray,
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { onAnswersUpdated { it.copy(savingsHabit = key) } }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = { onAnswersUpdated { it.copy(savingsHabit = key) } },
                colors = RadioButtonDefaults.colors(selectedColor = ForestGreen)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextDark)
        }
    }
}

// Form Step 4 Compose
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StepLoanGoal(
    answers: BusinessAnswers,
    onAnswersUpdated: ((BusinessAnswers) -> BusinessAnswers) -> Unit
) {
    Text(
        text = "Your loan requirements",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = TextDark
    )
    Text(
        text = "Clarifying targets enables matching to custom micro-loans and credit products.",
        style = MaterialTheme.typography.bodyMedium,
        color = TextDark.copy(alpha = 0.6f),
        modifier = Modifier.padding(bottom = 24.dp)
    )

    // Reason for Loan
    Text(
        text = "Purpose of Funding",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = TextDark,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    val goals = listOf(
        "expand business" to "Expand Business Outlets",
        "buy stock/inventory" to "Purchase Wholesale Stock / Inventory",
        "buy equipment" to "Acquire Productive Equipment",
        "school fees" to "Pay Educational / School Fees",
        "emergency" to "Emergency Business Buffer",
        "other" to "Other Commercial Purpose"
    )
    goals.forEach { (key, label) ->
        val isSelected = answers.loanReason == key
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) ForestGreen.copy(alpha = 0.1f) else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (isSelected) ForestGreen else Color.LightGray,
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { onAnswersUpdated { it.copy(loanReason = key) } }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = { onAnswersUpdated { it.copy(loanReason = key) } },
                colors = RadioButtonDefaults.colors(selectedColor = ForestGreen)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextDark)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Loan amount
    Text(
        text = "Target Loan Size (UGX)",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = TextDark,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    val sizes = listOf("below 1M", "1M–5M", "5M–20M", "above 20M")
    sizes.forEach { size ->
        val isSelected = answers.loanAmount == size
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) ForestGreen.copy(alpha = 0.1f) else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (isSelected) ForestGreen else Color.LightGray,
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { onAnswersUpdated { it.copy(loanAmount = size) } }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = { onAnswersUpdated { it.copy(loanAmount = size) } },
                colors = RadioButtonDefaults.colors(selectedColor = ForestGreen)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "$size UGX", style = MaterialTheme.typography.bodyMedium, color = TextDark)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Urgency/Timeline
    Text(
        text = "When do you require the funding?",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = TextDark,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    val tempos = listOf("as soon as possible", "within 3 months", "within 6 months", "just planning ahead")
    tempos.forEach { tempo ->
        val isSelected = answers.targetTimeline == tempo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) ForestGreen.copy(alpha = 0.1f) else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (isSelected) ForestGreen else Color.LightGray,
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { onAnswersUpdated { it.copy(targetTimeline = tempo) } }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = { onAnswersUpdated { it.copy(targetTimeline = tempo) } },
                colors = RadioButtonDefaults.colors(selectedColor = ForestGreen)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = tempo, style = MaterialTheme.typography.bodyMedium, color = TextDark)
        }
    }
}

// ================= Loading Screen =================

@Composable
fun LoadingScreen(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ForestGreenDark)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Rotating Circle Indicator Group
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                strokeWidth = 6.dp,
                color = GoldAccent,
                trackColor = ForestGreen,
                modifier = Modifier.size(100.dp)
            )
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "AI Processing",
                tint = GoldAccent,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "AI COACH ANALYSIS",
            style = MaterialTheme.typography.labelLarge,
            color = GoldAccent,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Large Rotating Tip Frame
        Card(
            colors = CardDefaults.cardColors(containerColor = ForestGreen.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp
                )
            }
        }
    }
}

// ================= Results Dashboard Screen =================

enum class DashboardTab { Score, Roadmap, Lenders, Profile, Settings }

@Composable
fun CreditReadyHeader(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    titleText: String = "Credit Readiness Coach"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PulsingDot")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = ForestGreen
                            )
                        }
                    } else {
                        // "CR" Logo Badge inside square ForestGreen card
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ForestGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "CR",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "SME CreditReady",
                            style = MaterialTheme.typography.titleMedium,
                            color = ForestGreen,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 16.sp
                        )
                        Text(
                            text = "BoU 60th Anniversary Hackathon",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            lineHeight = 10.sp
                        )
                    }
                }

                // Demo Mode Badge with Pulsing Green Dot
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(100.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .drawBehind {
                                drawCircle(
                                    color = Color(0xFF22C55E),
                                    alpha = pulseAlpha
                                )
                            }
                    )
                    Text(
                        text = "Demo Mode",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                }
            }
            HorizontalDivider(
                color = Color(0xFFF1F5F9),
                thickness = 1.dp
            )
        }
    }
}

@Composable
fun CreditReadyBottomNavBar(
    selectedTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column {
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DashboardNavItem(
                    tab = DashboardTab.Score,
                    selected = selectedTab == DashboardTab.Score,
                    label = "Score",
                    onClick = { onTabSelected(DashboardTab.Score) }
                )
                DashboardNavItem(
                    tab = DashboardTab.Roadmap,
                    selected = selectedTab == DashboardTab.Roadmap,
                    label = "Roadmap",
                    onClick = { onTabSelected(DashboardTab.Roadmap) }
                )
                DashboardNavItem(
                    tab = DashboardTab.Lenders,
                    selected = selectedTab == DashboardTab.Lenders,
                    label = "Lenders",
                    onClick = { onTabSelected(DashboardTab.Lenders) }
                )
                DashboardNavItem(
                    tab = DashboardTab.Profile,
                    selected = selectedTab == DashboardTab.Profile,
                    label = "Profile",
                    onClick = { onTabSelected(DashboardTab.Profile) }
                )
                DashboardNavItem(
                    tab = DashboardTab.Settings,
                    selected = selectedTab == DashboardTab.Settings,
                    label = "Settings",
                    onClick = { onTabSelected(DashboardTab.Settings) }
                )
            }
        }
    }
}

@Composable
fun RowScope.DashboardNavItem(
    tab: DashboardTab,
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    val contentColor = if (selected) ForestGreen else Color.LightGray
    val alphaValue = if (selected) 1f else 0.4f

    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(
                onClick = onClick,
                interactionSource = null,
                indication = null
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val symbol = when (tab) {
            DashboardTab.Score -> "●"
            DashboardTab.Roadmap -> "◆"
            DashboardTab.Lenders -> "▲"
            DashboardTab.Profile -> "■"
            DashboardTab.Settings -> "⚙"
        }
        
        Text(
            text = symbol,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.alpha(alphaValue)
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            fontSize = 10.sp,
            modifier = Modifier.alpha(alphaValue)
        )
    }
}

@Composable
fun TopPriorityActionCard(
    title: String,
    description: String,
    onActionClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForestGreen),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(GoldAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "!",
                        color = ForestGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = "TOP PRIORITY ACTION",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Start Improvement Task",
                    color = ForestGreen,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun BestMatchPreviewCard(
    match: LoanMatch
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), // Slate-900 Dark Container
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = match.lender.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase()
                    Text(
                        text = initials,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column {
                    Text(
                        text = "BEST MATCH",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = match.lender,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (match.eligible_now) "ELIGIBLE NOW" else "ELIGIBLE IN",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (match.eligible_now) Color(0xFF4ADE80) else GoldAccent,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (match.eligible_now) "Immediate" else "3 Months",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ProfileOverviewCard(
    answers: BusinessAnswers,
    profileImageUri: android.net.Uri?,
    onUploadPhotoClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ForestGreen.copy(alpha = 0.1f))
                        .border(1.5.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable { onUploadPhotoClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (profileImageUri != null) {
                        AsyncImage(
                            model = profileImageUri,
                            contentDescription = "SME uploaded profile avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "Add business photo avatar",
                                tint = ForestGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "PHOTO", 
                                style = MaterialTheme.typography.labelSmall, 
                                fontSize = 8.sp, 
                                fontWeight = FontWeight.Black, 
                                color = ForestGreen
                            )
                        }
                    }
                }
                Column {
                    Text(
                        text = answers.businessName.ifEmpty { "Maria's Retail Shop" },
                        style = MaterialTheme.typography.titleLarge,
                        color = ForestGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = (answers.businessType.ifEmpty { "Retail Outlet" }).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldAccentDark,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(bottom = 16.dp))

            ProfileDetailRow(label = "URSB Registration", value = if (answers.isRegistered) "Official Registered Bureau Profile" else "Unregistered SME Status", isAccent = answers.isRegistered)
            ProfileDetailRow(label = "Operating Duration", value = answers.operatingTime.ifEmpty { "1–3 Years" })
            ProfileDetailRow(label = "Monthly Turnover Collections", value = "${answers.monthlyRevenue.replace("below 500k", "<500k").replace("above 20M", ">20M").ifEmpty { "500k–2M" }} UGX Range")
            ProfileDetailRow(label = "Mobile Money Service", value = if (answers.mobileMoneyUsage == "both") "MTN & Airtel Business Wallets" else "Cash-Only Collections")
            ProfileDetailRow(label = "Previous Credit Repayments", value = if (answers.loanHistory == "never") "No credit histories / defaults recorded" else "Existing positive reports")
            ProfileDetailRow(label = "VSLA/SACCO Savings Habit", value = "Active Group Contributions / VSLA savings")
            ProfileDetailRow(label = "Funding Reason Target", value = "Purchase Wholesale Stock / Inventory")
            ProfileDetailRow(label = "Target Size Size", value = "${answers.loanAmount.ifEmpty { "1M–5M" }} UGX")
        }
    }
}

@Composable
fun ProfileDetailRow(
    label: String,
    value: String,
    isAccent: Boolean = false
) {
    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isAccent) RatingGreen else TextDark
        )
    }
}

@Composable
fun ResultsDashboardScreen(
    result: CreditAssessmentResult,
    answers: BusinessAnswers,
    viewModel: CreditViewModel,
    profileImageUri: android.net.Uri?,
    businessLogoUri: android.net.Uri?,
    onUploadPhotoClick: () -> Unit,
    onUploadLogoClick: () -> Unit,
    onRetake: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(DashboardTab.Score) }
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val lastSeenTimestamp = viewModel.lastSeen.collectAsState().value
    val memberSinceTimestamp = viewModel.memberSince.collectAsState().value

    var isEditMode by remember { mutableStateOf(false) }
    var editBusinessName by remember(answers) { mutableStateOf(answers.businessName) }
    var editBusinessType by remember(answers) { mutableStateOf(answers.businessType) }
    var editLoanAmount by remember(answers) { mutableStateOf(answers.loanAmount) }
    var editDistrict by remember(answers) { mutableStateOf(answers.district) }

    var selectedRegion by remember { mutableStateOf("Central Region") }
    var showDistrictDropdown by remember { mutableStateOf(false) }
    var showLanguagePickerInSettings by remember { mutableStateOf(false) }

    val currentLanguageCode by viewModel.currentLanguageCode.collectAsState()

    if (showLanguagePickerInSettings) {
        LanguageSelectionDialog(
            currentLanguageCode = currentLanguageCode,
            onLanguageSelected = { viewModel.setLanguage(it) },
            onDismiss = { showLanguagePickerInSettings = false }
        )
    }

    Scaffold(
        topBar = {
            CreditReadyHeader(
                onBack = null,
                titleText = "SME CreditReady Report"
            )
        },
        bottomBar = {
            CreditReadyBottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundLight)
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (selectedTab) {
                DashboardTab.Score -> {
                    // Summary Card Header with double progress canvas drawing
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "YOUR CREDIT READINESS SCORE",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreenDark.copy(alpha = 0.7f),
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            DoubleRingGauge(
                                currentScore = result.overall_score,
                                potentialScore = result.score_if_improved,
                                grade = result.grade,
                                modifier = Modifier.size(200.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = result.improvement_message,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreen,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    WhatIfEligibilitySimulator(answers = answers, currentScore = result.overall_score)

                    // Summary Text block
                    Text(
                        text = "EXECUTIVE SUMMARY",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextDark,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 8.dp)
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = result.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextDark,
                                lineHeight = 22.sp
                            )
                        }
                    }

                    // 5 Pillars scoring grid
                    Text(
                        text = "CREDIT GAP PILLARS",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextDark,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 12.dp)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        result.pillars.forEach { pillar ->
                            PillarScoreCard(pillar)
                        }
                    }
                }

                DashboardTab.Roadmap -> {
                    // Render Top Priority Action card!
                    val firstStep = result.roadmap.firstOrNull()
                    if (firstStep != null) {
                        TopPriorityActionCard(
                            title = firstStep.action,
                            description = "Using ${firstStep.resource} will provide the verifiable transaction logs required and unlock eligibility to score ${result.score_if_improved}% (Grade ${if (result.score_if_improved >= 70) "B" else "C+"}).\nExpected impact: ${firstStep.impact}.",
                            onActionClick = {
                                Toast.makeText(context, "Task started! Let's build your active days history ledger.", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    Text(
                        text = "STEP-BY-STEP ROADS TO ELIGIBILITY",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextDark,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 12.dp)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        result.roadmap.forEach { step ->
                            RoadmapStepItem(step)
                        }
                    }
                }

                DashboardTab.Lenders -> {
                    // Interactive Loan Calculator
                    LoanCalculatorCard()

                    // Render "Best Match" Slate-900 preview card
                    val bestMatch = result.loan_matches.firstOrNull()
                    if (bestMatch != null) {
                        BestMatchPreviewCard(match = bestMatch)
                    }

                    Text(
                        text = "MAPPED FINANCIAL LENDERS",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextDark,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 12.dp)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        result.loan_matches.forEach { match ->
                            LenderMatchCard(match)
                        }
                    }
                }

                DashboardTab.Profile -> {
                    Text(
                        text = "SME ASSESSMENT REGISTER",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextDark,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 12.dp)
                    )

                    // Database Sync status banner
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(0xFF22C55E), shape = CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Firebase RTDB Link Status",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                    Text(
                                        text = "Continuous Live Sync Enabled",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                            Text(
                                text = "ONLINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF16A34A),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Monospace UID Badge
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "SECURE COHORT BADGE UID",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = viewModel.currentUserId,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = ForestGreenDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("SME UID", viewModel.currentUserId)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "SME Badge UID copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Copy UID",
                                    tint = ForestGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Completeness progress bar calculation
                    val totalFieldsCount = 11
                    val setFieldsCount = listOf(
                        answers.businessName, answers.businessType, answers.operatingTime,
                        answers.employeeCount, answers.district, answers.monthlyRevenue,
                        answers.monthlyExpenses, answers.loanReason, answers.loanAmount
                    ).count { it.isNotBlank() } + (if (answers.isRegistered) 1 else 0) + (if (answers.hasBankStatement) 1 else 0)
                    val completenessPercent = (setFieldsCount * 100) / totalFieldsCount
                    val completenessColor = when {
                        completenessPercent < 45 -> RatingRed
                        completenessPercent < 75 -> RatingAmber
                        else -> RatingGreen
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SME Profile Completeness",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = "$completenessPercent%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = completenessColor
                                )
                            }
                            LinearProgressIndicator(
                                progress = { completenessPercent.toFloat() / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(100.dp)),
                                color = completenessColor,
                                trackColor = Color(0xFFF1F5F9)
                            )
                        }
                    }

                    // Profile Navigation and Mode Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selectedTab = DashboardTab.Settings },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, ForestGreen.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = "System Settings", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Preferences", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }

                        if (!isEditMode) {
                            Button(
                                onClick = { isEditMode = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                modifier = Modifier.weight(1f).height(46.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Profile Info", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Profile", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    if (isEditMode) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.5.dp, GoldAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    text = "EDIT PROFILE ADVISORY DETAILS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccentDark,
                                    letterSpacing = 1.sp
                                )

                                OutlinedTextField(
                                    value = editBusinessName,
                                    onValueChange = { editBusinessName = it },
                                    label = { Text("Business Name") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = editBusinessType,
                                    onValueChange = { editBusinessType = it },
                                    label = { Text("Business Operations Category") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = editLoanAmount,
                                    onValueChange = { editLoanAmount = it },
                                    label = { Text("Target Borrowing Capacity (UGX)") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Region / District Picker flow
                                Column {
                                    Text(
                                        text = "Operating District Selection:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    // Region selection horizontal list elements
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ugandaDistrictsByRegion.keys.forEach { region ->
                                            val isRegSelected = region == selectedRegion
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isRegSelected) ForestGreen else Color(0xFFF1F5F9))
                                                    .clickable { 
                                                        selectedRegion = region
                                                        // Auto set to first district in region
                                                        editDistrict = ugandaDistrictsByRegion[region]?.firstOrNull() ?: editDistrict
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = region,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isRegSelected) Color.White else Color.DarkGray,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // District field representation with Dropdown
                                    OutlinedTextField(
                                        value = editDistrict,
                                        onValueChange = { editDistrict = it },
                                        readOnly = true,
                                        label = { Text("District in $selectedRegion") },
                                        shape = RoundedCornerShape(12.dp),
                                        trailingIcon = {
                                            IconButton(onClick = { showDistrictDropdown = !showDistrictDropdown }) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropDown,
                                                    contentDescription = "District List Toggle",
                                                    tint = ForestGreen
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    if (showDistrictDropdown) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, Color.LightGray),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 200.dp)
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            val districts = ugandaDistrictsByRegion[selectedRegion] ?: emptyList()
                                            districts.forEach { distName ->
                                                Text(
                                                    text = distName,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            editDistrict = distName
                                                            showDistrictDropdown = false
                                                        }
                                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = TextDark,
                                                    fontWeight = if (editDistrict == distName) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { isEditMode = false },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.5.dp, Color.Gray),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Text("Cancel")
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.updateAnswers { current ->
                                                current.copy(
                                                    businessName = editBusinessName,
                                                    businessType = editBusinessType,
                                                    loanAmount = editLoanAmount,
                                                    district = editDistrict
                                                )
                                            }
                                            isEditMode = false
                                            Toast.makeText(context, "SME Advisory profile successfully synchronized to Firebase!", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Text("Save Sync")
                                    }
                                }
                            }
                        }
                    } else {
                        ProfileOverviewCard(
                            answers = answers,
                            profileImageUri = profileImageUri,
                            onUploadPhotoClick = onUploadPhotoClick
                        )
                    }
                }

                DashboardTab.Settings -> {
                    Text(
                        text = "SYSTEM PREFERENCES ENGINE",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextDark,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 12.dp)
                    )

                    // Language Settings Card
                    val selectedLangObj = remember(currentLanguageCode) {
                        TranslationHelper.languages.find { it.code == currentLanguageCode }
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Active Translation Language",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                    Text(
                                        text = selectedLangObj?.let { "${it.name} (${it.localName})" } ?: "English",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ForestGreen
                                    )
                                }
                                Button(
                                    onClick = { showLanguagePickerInSettings = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen.copy(alpha = 0.1f), contentColor = ForestGreen),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Change Dialect", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Theme Customizer Setting Card
                    val isDarkThemePref by viewModel.isDarkTheme.collectAsState()
                    val isDark = isDarkThemePref ?: false

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text(
                                        text = "High-Contrast Solar Theme",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                    Text(
                                        text = "Optimize contrast ratios and visual display colors for outdoor marketplace operation.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                                Switch(
                                    checked = isDark,
                                    onCheckedChange = { viewModel.setDarkTheme(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = ForestGreen, checkedTrackColor = ForestGreen.copy(alpha = 0.4f))
                                )
                            }
                        }
                    }

                    // Notification Setting Toggles
                    val notifRemindersActive = viewModel.scoreAlertsEnabled.collectAsState().value
                    val advisoryTipsActive = viewModel.roadmapRemindersEnabled.collectAsState().value

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "LIVE ADVISORY ALERTS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                letterSpacing = 0.5.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text(
                                        text = "Transaction Ledger Prompts",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                    Text(
                                        text = "Receive notifications to log daily Mobile Money billing collections.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                                Switch(
                                    checked = notifRemindersActive,
                                    onCheckedChange = { viewModel.toggleScoreAlerts(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = ForestGreen, checkedTrackColor = ForestGreen.copy(alpha = 0.4f))
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text(
                                        text = "Weekly Financial Roadmap Tips",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                    Text(
                                        text = "Daily micro learning insights regarding URSB and bank loan eligibility criteria.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                                Switch(
                                    checked = advisoryTipsActive,
                                    onCheckedChange = { viewModel.toggleRoadmapReminders(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = ForestGreen, checkedTrackColor = ForestGreen.copy(alpha = 0.4f))
                                )
                            }
                        }
                    }

                    // Connection and Registry Details
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "COHORT REGISTRY LANDSCAPE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "• Member Since: ${memberSinceTimestamp.ifEmpty { "Today" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextDark
                            )
                            Text(
                                text = "• Active Live Session: ${lastSeenTimestamp.ifEmpty { "Just Now" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextDark
                            )
                        }
                    }

                    // Logout operation
                    Button(
                        onClick = { viewModel.signOut() },
                        colors = ButtonDefaults.buttonColors(containerColor = RatingRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Log Out", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out Of Business Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Dashboard Bottom Operations (Shared buttons)
            Button(
                onClick = {
                    Toast.makeText(
                        context,
                        "Opening print dialog to download official PDF copy. Sharing digital certificate with Pride Microfinance and PostBank loan officers!",
                        Toast.LENGTH_LONG
                    ).show()
                    printReport(context, result, answers)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("download_report_btn")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Report",
                        tint = ForestGreenDark
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share Official Certificate",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreenDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onRetake,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen),
                border = BorderStroke(2.dp, ForestGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("retake_btn")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Start Again"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Retake Assessment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// ================= Gauge & Component Helpers =================

@Composable
fun DoubleRingGauge(
    currentScore: Int,
    potentialScore: Int,
    grade: String,
    modifier: Modifier = Modifier
) {
    // High performance gauge load animations
    val animProgressCurrent = remember { Animatable(0f) }
    val animProgressPotential = remember { Animatable(0f) }
    
    LaunchedEffect(currentScore, potentialScore) {
        launch {
            animProgressCurrent.animateTo(
                targetValue = currentScore.toFloat(),
                animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
            )
        }
        launch {
            animProgressPotential.animateTo(
                targetValue = potentialScore.toFloat(),
                animationSpec = tween(durationMillis = 1700, easing = FastOutSlowInEasing)
            )
        }
    }

    val animatedScore = animProgressCurrent.value.toInt()
    val animatedPotential = animProgressPotential.value

    // Determine status color based on score value
    val gaugeColor = when {
        animatedScore < 40 -> RatingRed
        animatedScore < 70 -> RatingAmber
        else -> RatingGreen
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthVal = 14.dp.toPx()
            val sizeDim = size.minDimension - strokeWidthVal
            val offsetVal = strokeWidthVal / 2f
            
            // Draw background track card
            drawArc(
                color = Color.LightGray.copy(alpha = 0.3f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(offsetVal, offsetVal),
                size = Size(sizeDim, sizeDim),
                style = Stroke(width = strokeWidthVal, cap = StrokeCap.Round)
            )

            // Draw potential improved ghost ring in gold
            val improvedSweepAngle = (animatedPotential / 100f) * 270f
            drawArc(
                color = GoldAccent.copy(alpha = 0.4f),
                startAngle = 135f,
                sweepAngle = improvedSweepAngle,
                useCenter = false,
                topLeft = Offset(offsetVal, offsetVal),
                size = Size(sizeDim, sizeDim),
                style = Stroke(width = strokeWidthVal, cap = StrokeCap.Round)
            )

            // Draw active/current score progress ring
            val currentSweepAngle = (animProgressCurrent.value / 100f) * 270f
            drawArc(
                color = gaugeColor,
                startAngle = 135f,
                sweepAngle = currentSweepAngle,
                useCenter = false,
                topLeft = Offset(offsetVal, offsetVal),
                size = Size(sizeDim, sizeDim),
                style = Stroke(width = strokeWidthVal, cap = StrokeCap.Round)
            )
        }

        // Grade label details centered inside the dial gauge
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = animatedScore.toString(),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = TextDark
            )
            Text(
                text = grade,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = gaugeColor
            )
            Text(
                text = "CREDIT READY",
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun PillarScoreCard(pillar: Pillar) {
    val statusColor = when (pillar.status.lowercase()) {
        "good" -> RatingGreen
        "fair" -> RatingAmber
        else -> RatingRed
    }

    val iconVector = when (pillar.status.lowercase()) {
        "good" -> Icons.Default.Check
        "fair" -> Icons.Default.Warning
        else -> Icons.Default.Warning
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pillar.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                // Pillar badge
                Card(
                    colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = pillar.status,
                            tint = statusColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = pillar.status.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Score progress bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                LinearProgressIndicator(
                    progress = { pillar.score / 100f },
                    color = statusColor,
                    trackColor = Color.LightGray.copy(alpha = 0.3f),
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${pillar.score}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = pillar.explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = TextDark.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Coach Quick Win Tip Drawer
            Card(
                colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Quick Win Tip",
                        tint = statusColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "WEEKLY QUICK WIN:",
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = pillar.quick_win,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RoadmapStepItem(step: RoadmapStep) {
    val nodeColor = when (step.timeline.lowercase()) {
        "this week" -> ForestGreen
        "this month" -> GoldAccent
        "in 3 months" -> GoldAccentDark
        else -> ForestGreenDark
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Timeline chronological vertical pin decoration
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(nodeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
            // Vertical connecting line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(80.dp)
                    .background(nodeColor.copy(alpha = 0.3f))
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = step.timeline.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = nodeColor
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = step.action,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Text(
                    text = "IMPACT: ${step.impact}",
                    style = MaterialTheme.typography.labelSmall,
                    color = RatingGreen,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Useful Resource Drawer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(nodeColor.copy(alpha = 0.05f))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Resource link",
                        tint = nodeColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tool: ${step.resource}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDark.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun LenderMatchCard(match: LoanMatch) {
    val badgeColor = if (match.eligible_now) RatingGreen else RatingAmber
    val iconVector = if (match.eligible_now) Icons.Default.CheckCircle else Icons.Default.Warning
    val context = LocalContext.current

    // Helper to get lender initials and secondary bg color
    val (initials, avatarBg) = when {
        match.lender.contains("MTN", ignoreCase = true) -> "MTN" to Color(0xFFFDB913)
        match.lender.contains("Pride", ignoreCase = true) -> "PM" to Color(0xFF1E3A8A)
        match.lender.contains("Post", ignoreCase = true) -> "PB" to Color(0xFFC026D3)
        match.lender.contains("Centenary", ignoreCase = true) -> "CB" to Color(0xFF0284C7)
        else -> {
            val words = match.lender.split(" ")
            val first = words.getOrNull(0)?.firstOrNull()?.toString() ?: ""
            val second = words.getOrNull(1)?.firstOrNull()?.toString() ?: ""
            (first + second).uppercase() to ForestGreen
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        border = if (match.eligible_now) BorderStroke(1.5.dp, RatingGreen.copy(alpha = 0.5f)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1.5f)
                ) {
                    // Circle Logo Avatar
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(avatarBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (initials == "MTN") Color.Black else Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = match.lender,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreenDark
                        )
                        Text(
                            text = match.product,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextDark.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Match Status badge
                Card(
                    colors = CardDefaults.cardColors(containerColor = badgeColor.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (match.eligible_now) "Eligible Now" else "Unlock Soon",
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Amount limit
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AMOUNT LIMIT: ",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextDark.copy(alpha = 0.5f)
                )
                Text(
                    text = match.amount_range,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Black,
                    color = ForestGreen
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))

            Text(
                text = match.reason,
                style = MaterialTheme.typography.bodyMedium,
                color = TextDark
            )

            if (!match.eligible_now) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Unlock requirement",
                        tint = RatingAmber,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "HOW TO QUALIFY:",
                            style = MaterialTheme.typography.labelSmall,
                            color = RatingAmber,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = match.requirement_to_qualify,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDark
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        Toast.makeText(
                            context,
                            "SECURE MATCH TRANSMITTED! SME CreditReady has safely shared your certified readiness scorecard with the loan officer at ${match.lender}.",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Request Access",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Request Warm Introduction",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WhatIfEligibilitySimulator(answers: BusinessAnswers, currentScore: Int) {
    var isRegistered by remember { mutableStateOf(answers.isRegistered) }
    var hasBankStatements by remember { mutableStateOf(answers.hasBankStatement) }
    var keepRecords by remember { mutableStateOf(answers.recordKeeping == "yes" || answers.recordKeeping == "sometimes") }
    var useMoMo by remember { mutableStateOf(answers.mobileMoneyUsage != "none") }

    // Start with current score, add simulated improvements
    val simulatedScore = remember(isRegistered, hasBankStatements, keepRecords, useMoMo) {
        var score = currentScore
        // Simulate changes
        if (isRegistered && !answers.isRegistered) score += 15
        if (hasBankStatements && !answers.hasBankStatement) score += 12
        if (keepRecords && !(answers.recordKeeping == "yes" || answers.recordKeeping == "sometimes")) score += 10
        if (useMoMo && answers.mobileMoneyUsage == "none") score += 10
        score.coerceIn(0, 100)
    }

    val simulatedGrade = when {
        simulatedScore < 40 -> "F"
        simulatedScore < 50 -> "D"
        simulatedScore < 60 -> "C"
        simulatedScore < 70 -> "C+"
        simulatedScore < 80 -> "B"
        simulatedScore < 90 -> "B+"
        else -> "A"
    }

    val statusColor = when {
        simulatedScore < 40 -> RatingRed
        simulatedScore < 70 -> RatingAmber
        else -> RatingGreen
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        border = BorderStroke(1.5.dp, GoldAccent.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WHAT-IF ELIGIBILITY SIMULATOR",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccentDark,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Simulate action plans to see your future score",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, statusColor)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$simulatedScore/100",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = statusColor
                        )
                        Text(
                            text = "Grade $simulatedGrade",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Checkbox options
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isRegistered = !isRegistered }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = isRegistered,
                    onCheckedChange = { isRegistered = it },
                    colors = CheckboxDefaults.colors(checkedColor = ForestGreen)
                )
                Text(
                    text = "URSB Name Registration (+15 pts)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isRegistered) FontWeight.Bold else FontWeight.Normal,
                    color = TextDark,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { hasBankStatements = !hasBankStatements }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = hasBankStatements,
                    onCheckedChange = { hasBankStatements = it },
                    colors = CheckboxDefaults.colors(checkedColor = ForestGreen)
                )
                Text(
                    text = "Submit bank statements logs (+12 pts)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (hasBankStatements) FontWeight.Bold else FontWeight.Normal,
                    color = TextDark,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { keepRecords = !keepRecords }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = keepRecords,
                    onCheckedChange = { keepRecords = it },
                    colors = CheckboxDefaults.colors(checkedColor = ForestGreen)
                )
                Text(
                    text = "Regular sales/expense bookkeeping (+10 pts)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (keepRecords) FontWeight.Bold else FontWeight.Normal,
                    color = TextDark,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { useMoMo = !useMoMo }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = useMoMo,
                    onCheckedChange = { useMoMo = it },
                    colors = CheckboxDefaults.colors(checkedColor = ForestGreen)
                )
                Text(
                    text = "High business mobile money velocity (+10 pts)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (useMoMo) FontWeight.Bold else FontWeight.Normal,
                    color = TextDark,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
fun AuthScreen(
    isLoading: Boolean,
    errorMessage: String?,
    currentLanguageCode: String,
    onShowLanguagePicker: () -> Unit,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onForgotPassword: (String) -> Unit,
    onClearError: () -> Unit
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var agreeToTerms by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }

    // Forgot Password Flow state
    var showForgotPopup by remember { mutableStateOf(false) }
    var forgotEmailByForm by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // Password strength computation
    val passwordStrength = remember(password) {
        if (password.isEmpty()) 0
        else {
            var score = 0
            if (password.length >= 6) score += 1
            if (password.length >= 8) score += 1
            if (password.any { it.isDigit() }) score += 1
            if (password.any { !it.isLetterOrDigit() }) score += 1
            score
        }
    }
    val strengthText = when (passwordStrength) {
        0 -> ""
        1 -> "Weak"
        2 -> "Fair"
        3 -> "Strong"
        else -> "Very Strong"
    }
    val strengthColor = when (passwordStrength) {
        0 -> Color.LightGray
        1 -> RatingRed
        2 -> RatingAmber
        3 -> RatingGreen
        else -> GoldAccent
    }

    if (showForgotPopup) {
        AlertDialog(
            onDismissRequest = { showForgotPopup = false },
            title = {
                Text(
                    text = "Request Password Reset",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreenDark
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter your registered business email and we will dispatch a secured recovery credential link to you.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    OutlinedTextField(
                        value = forgotEmailByForm,
                        onValueChange = { forgotEmailByForm = it },
                        label = { Text("Business Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (forgotEmailByForm.trim().contains("@")) {
                            onForgotPassword(forgotEmailByForm.trim())
                            showForgotPopup = false
                        } else {
                            Toast.makeText(context, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Text("Send Recovery Email", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPopup = false }) {
                    Text("Cancel", color = ForestGreen)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(ForestGreenDark, BackgroundLight),
                    startY = 0f,
                    endY = 1000f
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Language switcher action row at top-right
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            LanguageSelectorButton(
                currentLanguageCode = currentLanguageCode,
                darkTheme = true,
                onClick = onShowLanguagePicker
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SME CreditReady Branding Header
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(GoldAccent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "SME CreditReady Key Logo",
                tint = ForestGreenDark,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = TranslationHelper.getString("app_title", currentLanguageCode),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Text(
            text = TranslationHelper.getString("app_subtitle", currentLanguageCode),
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Main Auth Fields Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isLoginMode) TranslationHelper.getString("welcome_back", currentLanguageCode) else TranslationHelper.getString("create_account", currentLanguageCode),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Text(
                    text = if (isLoginMode) TranslationHelper.getString("signin_desc", currentLanguageCode) else TranslationHelper.getString("signup_desc", currentLanguageCode),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = RatingRed.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, RatingRed.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error detail",
                                tint = RatingRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = RatingRed,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = onClearError,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss error",
                                    tint = RatingRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Email input
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; onClearError() },
                    label = { Text(TranslationHelper.getString("email_label", currentLanguageCode)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email address icon",
                            tint = ForestGreen
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_email_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ForestGreen,
                        focusedLabelColor = ForestGreen,
                        unfocusedBorderColor = Color.LightGray
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Uganda Phone Subscriber Number (Registration Only)
                if (!isLoginMode) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 9) {
                                phone = input
                            }
                        },
                        label = { Text("Uganda Phone Subscriber No") },
                        prefix = { Text("+256 ", fontWeight = FontWeight.Bold, color = ForestGreen) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Uganda business telephone",
                                tint = ForestGreen
                            )
                        },
                        placeholder = { Text("e.g. 772123456") },
                        supportingText = { Text("Enter 9 subscriber digits (e.g. 7xxxxxxxx)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_phone_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ForestGreen,
                            focusedLabelColor = ForestGreen,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Password input
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; onClearError() },
                    label = { Text(TranslationHelper.getString("password_label", currentLanguageCode)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Password shield icon",
                            tint = ForestGreen
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = "Toggle password view",
                                tint = Color.Gray
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_password_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ForestGreen,
                        focusedLabelColor = ForestGreen,
                        unfocusedBorderColor = Color.LightGray
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                // Password strength meter
                if (password.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 12.dp, start = 4.dp, end = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Password Strength:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            Text(
                                strengthText,
                                style = MaterialTheme.typography.labelSmall,
                                color = strengthColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // Draw multi segment bar
                        Row(
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (i in 1..4) {
                                val segmentColor = if (i <= passwordStrength) strengthColor else Color.LightGray.copy(alpha = 0.4f)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(segmentColor)
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Confirm Password (Registration Only)
                if (!isLoginMode) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Business Password") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Confirm password credential",
                                tint = ForestGreen
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.CheckCircle else Icons.Default.Info,
                                    contentDescription = "Toggle view",
                                    tint = Color.Gray
                                )
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_confirm_password_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ForestGreen,
                            focusedLabelColor = ForestGreen,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Remember Me checkbox (Login Only)
                if (isLoginMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { rememberMe = !rememberMe }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = ForestGreen)
                        )
                        Text(
                            text = "Remember this business profile",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Forgot password?",
                        style = MaterialTheme.typography.bodySmall,
                        color = ForestGreen,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable {
                                forgotEmailByForm = email
                                showForgotPopup = true
                            }
                            .padding(4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Terms of service checkbox (Registration Only)
                if (!isLoginMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { agreeToTerms = !agreeToTerms }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = agreeToTerms,
                            onCheckedChange = { agreeToTerms = it },
                            colors = CheckboxDefaults.colors(checkedColor = ForestGreen)
                        )
                        Text(
                            text = "Agree to official SME Financial Advisory Terms",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Submit Action Button
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            Toast.makeText(context, TranslationHelper.getString("fill_details", currentLanguageCode), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (password.length < 6) {
                            Toast.makeText(context, TranslationHelper.getString("pass_length", currentLanguageCode), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (!isLoginMode) {
                            if (confirmPassword != password) {
                                Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (phone.length < 9) {
                                Toast.makeText(context, "Please enter a valid 9-digit Ugandan subscriber number.", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            if (!agreeToTerms) {
                                Toast.makeText(context, "You must agree to the SME terms and conditions.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            onSignUp(email.trim(), password)
                        } else {
                            onSignIn(email.trim(), password)
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("auth_submit_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = if (isLoginMode) TranslationHelper.getString("btn_access_profile", currentLanguageCode) else TranslationHelper.getString("btn_register_business", currentLanguageCode),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle Auth Mode text Link
                TextButton(
                    onClick = {
                        isLoginMode = !isLoginMode
                        onClearError()
                    },
                    modifier = Modifier.testTag("toggle_auth_mode_button")
                ) {
                    Text(
                        text = if (isLoginMode) TranslationHelper.getString("new_user_create", currentLanguageCode) else TranslationHelper.getString("have_account", currentLanguageCode),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreenDark,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Security footer
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Secured encrypted session",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = TranslationHelper.getString("secure_session", currentLanguageCode),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

// ================= BRAND NEW FULL-FEATURE ENHANCEMENTS =================

val ugandaDistrictsByRegion = mapOf(
    "Central Region" to listOf(
        "Kampala", "Wakiso", "Mukono", "Masaka", "Luweero", "Mityana", "Mpigi", "Mubende", "Rakai", "Lyantonde", 
        "Sembabule", "Kalangala", "Buikwe", "Bukomansimbi", "Butambala", "Gomba", "Kalungu", "Kyankwanzi", 
        "Lwengo", "Nakaseke", "Nakasongola", "Kyotera", "Kassanda"
    ),
    "Eastern Region" to listOf(
        "Jinja", "Mbale", "Soroti", "Iganga", "Tororo", "Busia", "Kamuli", "Pallisa", "Amuria", "Budaka", 
        "Bududa", "Bugiri", "Bukedea", "Bukwo", "Bulambuli", "Buyende", "Kaberamaido", "Kaliro", "Kapchorwa", 
        "Kween", "Luuka", "Manafwa", "Mayuge", "Namayingo", "Namutumba", "Ngora", "Serere", "Sironko", 
        "Kapelebyong", "Bugweri", "Kalaki"
    ),
    "Northern Region" to listOf(
        "Gulu", "Lira", "Arua", "Kitgum", "Apac", "Adjumani", "Amolatar", "Amuru", "Dokolo", "Koboko", 
        "Kotido", "Moroto", "Moyo", "Nakapiripirit", "Nebbi", "Oyam", "Pader", "Yumbe", "Abim", "Agago", 
        "Alebtong", "Amudat", "Kaabong", "Kole", "Lamwo", "Maracha", "Napak", "Otuke", "Zombo", "Omoro", 
        "Pakwach", "Nabilatuk", "Kwania", "Karenga", "Madi-Okollo", "Terego", "Obongi"
    ),
    "Western Region" to listOf(
        "Mbarara", "Kabale", "Fort Portal", "Kasese", "Hoima", "Bushenyi", "Rukungiri", "Ntungamo", "Kisoro", 
        "Bundibugyo", "Ibanda", "Isingiro", "Kabarole", "Kamwenge", "Kanungu", "Kibaale", "Kiruhura", "Kiryandongo", 
        "Kyenjojo", "Masindi", "Mitooma", "Ntoroko", "Rubirizi", "Sheema", "Buliisa", "Kagadi", "Kakumiro", 
        "Rubanda", "Rukiga", "Kyegewa", "Bunyangabu", "Kazo", "Kitagwenda"
    )
)

@Composable
fun LanguageSelectorButton(
    currentLanguageCode: String,
    darkTheme: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLang = remember(currentLanguageCode) {
        TranslationHelper.languages.firstOrNull { it.code == currentLanguageCode } ?: TranslationHelper.languages.first()
    }
    val contentColor = if (darkTheme) Color.White else ForestGreen
    val bgColor = if (darkTheme) Color.White.copy(alpha = 0.15f) else ForestGreenLight.copy(alpha = 0.5f)
    val borderColor = if (darkTheme) Color.White.copy(alpha = 0.3f) else ForestGreen.copy(alpha = 0.3f)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "🌐",
            fontSize = 13.sp
        )
        Text(
            text = currentLang.name.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = contentColor
        )
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = "Switch Language",
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun LanguageSelectionDialog(
    currentLanguageCode: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredLanguages = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            TranslationHelper.languages
        } else {
            TranslationHelper.languages.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.localName.contains(searchQuery, ignoreCase = true) ||
                it.code.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val grouped = remember(filteredLanguages) {
        filteredLanguages.groupBy { it.category }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ForestGreen)
            }
        },
        title = {
            Text("Select Language / Kyusa Alimi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ForestGreen)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search languages...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ForestGreen,
                        unfocusedBorderColor = Color.LightGray
                    ),
                    singleLine = true
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                    grouped.forEach { (category, langs) ->
                        item {
                            Text(
                                text = category.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldAccentDark,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            )
                        }
                        items(langs) { lang ->
                            val isSelected = lang.code == currentLanguageCode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) ForestGreen.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable {
                                        onLanguageSelected(lang.code)
                                        onDismiss()
                                    }
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = lang.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) ForestGreenDark else TextDark
                                    )
                                    if (lang.localName.isNotEmpty() && lang.localName != lang.name) {
                                        Text(
                                            text = lang.localName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = ForestGreen, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

fun printReport(context: Context, result: CreditAssessmentResult, answers: BusinessAnswers) {
    try {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? android.print.PrintManager
        if (printManager != null) {
            val jobName = "${answers.businessName.ifEmpty { "SME" }}_CreditReady_Certificate"

            val htmlContent = """
                <html>
                <head>
                    <style>
                        body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; padding: 40px; color: #1E293B; background-color: #F8FAFC; }
                        .certificate { border: 10px double #15803D; padding: 40px; background-color: white; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.05); }
                        .header { text-align: center; margin-bottom: 40px; }
                        .logo { font-size: 36px; font-weight: bold; color: #15803D; margin-bottom: 5px; }
                        .subtitle { font-size: 14px; color: #B45309; font-weight: bold; letter-spacing: 2px; }
                        .title { font-size: 28px; font-weight: bold; text-align: center; margin-bottom: 20px; color: #0F172A; text-transform: uppercase; }
                        .score-box { text-align: center; margin: 30px auto; padding: 20px; background-color: #F0FDF4; border: 2.5px solid #15803D; border-radius: 12px; display: inline-block; width: 220px; }
                        .score { font-size: 48px; font-weight: 900; color: #15803D; }
                        .grade { font-size: 20px; font-weight: bold; color: #B45309; }
                        .details { margin-top: 30px; font-size: 14px; line-height: 1.6; }
                        .detail-row { display: flex; justify-content: space-between; border-bottom: 1px solid #E2E8F0; padding: 8px 0; }
                        .detail-row span:first-child { font-weight: bold; color: #64748B; }
                        .summary { margin-top: 30px; font-size: 14px; line-height: 1.6; color: #475569; font-style: italic; background: #FFFBEB; padding: 15px; border-left: 5px solid #D97706; border-radius: 4px; }
                        .footer { text-align: center; margin-top: 50px; font-size: 11px; color: #94A3B8; border-top: 1px dashed #E2E8F0; padding-top: 15px; }
                    </style>
                </head>
                <body>
                    <div class="certificate">
                        <div class="header">
                            <div class="logo">SME CreditReady</div>
                            <div class="subtitle">BANK OF UGANDA &bull; 60th ANNIVERSARY HACKATHON</div>
                        </div>

                        <div class="title">Official Credit Readiness Certificate</div>

                        <div style="text-align: center; margin-bottom: 30px;">
                            <div class="score-box">
                                <div class="score">${result.overall_score}%</div>
                                <div class="grade">GRADE ${result.grade}</div>
                            </div>
                        </div>

                        <div class="details">
                            <div class="detail-row"><span>SME Business Name:</span> <span>${answers.businessName.ifEmpty { "Maria Nakato Clothing Shop" }}</span></div>
                            <div class="detail-row"><span>Operating Location:</span> <span>${answers.district} District, Uganda</span></div>
                            <div class="detail-row"><span>Operation Sector:</span> <span>${answers.businessType.ifEmpty { "Retail Vendor" }}</span></div>
                            <div class="detail-row"><span>URSB Registered Status:</span> <span>${if (answers.isRegistered) "YES (Registered)" else "NO (Informal Trader)"}</span></div>
                            <div class="detail-row"><span>Record Style:</span> <span>${answers.recordKeeping}</span></div>
                            <div class="detail-row"><span>Target Funding Value:</span> <span>${answers.loanAmount} UGX Range</span></div>
                        </div>

                        <div class="summary">
                            <strong>Coach Analysis Summary:</strong><br/>
                            ${result.summary}
                        </div>

                        <div class="footer">
                            Encrypted session GMS authentication certified signature block. Bankability verification token: ${java.util.UUID.randomUUID().toString().take(18).uppercase()}
                        </div>
                    </div>
                </body>
                </html>
            """.trimIndent()

            val webView = android.webkit.WebView(context)
            webView.webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: android.webkit.WebView, url: String) {
                    val printAdapter = webView.createPrintDocumentAdapter(jobName)
                    printManager.print(jobName, printAdapter, android.print.PrintAttributes.Builder().build())
                }
            }
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
        } else {
            Toast.makeText(context, "System print is currently unavailable", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Log.e("MainAppScreen", "Failed to print certificate", e)
        Toast.makeText(context, "Printer failure", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun LoanCalculatorCard() {
    var borrowAmount by remember { mutableStateOf(5000000f) }
    var interestRate by remember { mutableStateOf(18f) }
    var termMonths by remember { mutableStateOf(12f) }

    val monthlyInterestPercent = (interestRate / 100f) / 12f
    val monthlyRepayment = remember(borrowAmount, interestRate, termMonths) {
        if (monthlyInterestPercent == 0f) {
            borrowAmount / termMonths
        } else {
            val discountFactor = ((Math.pow((1f + monthlyInterestPercent).toDouble(), termMonths.toDouble()) - 1f) / 
                    (monthlyInterestPercent * Math.pow((1f + monthlyInterestPercent).toDouble(), termMonths.toDouble())))
            if (discountFactor > 0f) {
                (borrowAmount.toDouble() / discountFactor).toFloat()
            } else {
                borrowAmount / termMonths
            }
        }
    }

    val totalPayment = monthlyRepayment * termMonths
    val totalInterest = totalPayment - borrowAmount

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(1.5.dp, GoldAccent.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GoldAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🧮", fontSize = 16.sp)
                }
                Column {
                    Text(
                        text = "LOAN REPAYMENT CALCULATOR",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Real-time cost & interest breakdown under BoU guidance",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray.copy(alpha = 0.7f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Borrow Amount", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = String.format("%,d UGX", borrowAmount.toInt()), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = GoldAccent)
            }
            Slider(
                value = borrowAmount,
                onValueChange = { borrowAmount = it },
                valueRange = 100000f..15000000f,
                steps = 149,
                colors = SliderDefaults.colors(
                    thumbColor = GoldAccent,
                    activeTrackColor = GoldAccent,
                    inactiveTrackColor = Color.LightGray.copy(alpha = 0.2f)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Annual Interest Rate", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "${interestRate.toInt()}% p.a.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color.White)
            }
            Slider(
                value = interestRate,
                onValueChange = { interestRate = it },
                valueRange = 5f..36f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.LightGray.copy(alpha = 0.2f)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Repayment Term", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "${termMonths.toInt()} Months", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color.White)
            }
            Slider(
                value = termMonths,
                onValueChange = { termMonths = it },
                valueRange = 1f..24f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.LightGray.copy(alpha = 0.2f)
                )
            )

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "MONTHLY COST", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("%,d UGX", monthlyRepayment.toInt()),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = RatingGreen
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "INTEREST CHARGES", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("%,d UGX", totalInterest.toInt()),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = GoldAccent
                        )
                    }
                }
            }
        }
    }
}
