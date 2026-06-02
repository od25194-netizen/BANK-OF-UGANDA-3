package com.example.ui.screens

import android.widget.Toast
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
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.CreditViewModel

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

    val context = LocalContext.current

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
            when (screen) {
                AppScreen.Landing -> LandingScreen(
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
                    text = "Begin Credit Assessment",
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
                        text = "Instant Live Demo (Maria's Story)",
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
    val categories = listOf("market vendor", "retail shop", "salon/barbershop", "boda boda/transport", "farming/agriculture", "food/restaurant", "other")
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

enum class DashboardTab { Score, Roadmap, Lenders, Profile }

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
        // Geometric shape indicator dot from design: Score (circle), Roadmap (diamond), Lenders (triangle), Profile (square)
        val symbol = when (tab) {
            DashboardTab.Score -> "●"
            DashboardTab.Roadmap -> "◆"
            DashboardTab.Lenders -> "▲"
            DashboardTab.Profile -> "■"
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
    answers: BusinessAnswers
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
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ForestGreen.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Business Symbol",
                        tint = ForestGreen,
                        modifier = Modifier.size(24.dp)
                    )
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
    onRetake: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(DashboardTab.Score) }
    val scrollState = rememberScrollState()
    val context = LocalContext.current

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

                    ProfileOverviewCard(answers = answers)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Dashboard Bottom Operations (Shared buttons)
            Button(
                onClick = {
                    Toast.makeText(
                        context,
                        "Downloading report secure copy. Sharing digital certificate with Pride Microfinance and PostBank loan officers!",
                        Toast.LENGTH_LONG
                    ).show()
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
    // Determine status color based on score value
    val gaugeColor = when {
        currentScore < 40 -> RatingRed
        currentScore < 70 -> RatingAmber
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
            val improvedSweepAngle = (potentialScore / 100f) * 270f
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
            val currentSweepAngle = (currentScore / 100f) * 270f
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
                text = currentScore.toString(),
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

                // Match Status badge
                Card(
                    colors = CardDefaults.cardColors(containerColor = badgeColor.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
            }
        }
    }
}
