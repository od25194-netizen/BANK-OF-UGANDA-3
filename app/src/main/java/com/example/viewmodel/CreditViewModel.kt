package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.BusinessAnswers
import com.example.model.CreditAssessmentResult
import com.example.network.GeminiManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

enum class AppScreen {
    Landing,
    Form,
    Loading,
    Results
}

class CreditViewModel : ViewModel() {
    private val TAG = "CreditViewModel"

    private val _currentScreen = MutableStateFlow(AppScreen.Landing)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _currentFormStep = MutableStateFlow(1) // 1 to 4
    val currentFormStep: StateFlow<Int> = _currentFormStep.asStateFlow()

    private val _answers = MutableStateFlow(BusinessAnswers())
    val answers: StateFlow<BusinessAnswers> = _answers.asStateFlow()

    private val _isAnalyzingMessage = MutableStateFlow("Analyzing your business profile...")
    val isAnalyzingMessage: StateFlow<String> = _isAnalyzingMessage.asStateFlow()

    private val _assessmentResult = MutableStateFlow<CreditAssessmentResult?>(null)
    val assessmentResult: StateFlow<CreditAssessmentResult?> = _assessmentResult.asStateFlow()

    private val _toastError = MutableStateFlow<String?>(null)
    val toastError: StateFlow<String?> = _toastError.asStateFlow()

    private val tips = listOf(
        "Did you know? MTN MoMo transaction history can substitute for bank statements at some Ugandan lenders.",
        "URSB business name registration costs only UGX 24,000 and greatly increases banking compatibility!",
        "SACCO networks in Kampala offer higher acceptance rates for micro-enterprise operations.",
        "Mobile lending apps look at payment stability, so keeping your billing active makes a huge difference.",
        "Ugandan microfinance institutions prioritize evidence of transaction history over land-titles."
    )
    
    private val stages = listOf(
        "Analyzing your business profile...",
        "Scoring your credit pillars...",
        "Matching you with Ugandan lenders...",
        "Preparing your personalized roadmap..."
    )

    private var rotationJob: Job? = null

    fun resetError() {
        _toastError.value = null
    }

    fun startAssessment() {
        _answers.value = BusinessAnswers()
        _currentFormStep.value = 1
        _currentScreen.value = AppScreen.Form
    }

    fun updateAnswers(updater: (BusinessAnswers) -> BusinessAnswers) {
        _answers.value = updater(_answers.value)
    }

    fun nextStep() {
        if (_currentFormStep.value < 4) {
            _currentFormStep.value += 1
        } else {
            submitAssessment()
        }
    }

    fun prevStep() {
        if (_currentFormStep.value > 1) {
            _currentFormStep.value -= 1
        } else {
            _currentScreen.value = AppScreen.Landing
        }
    }

    fun submitAssessment(forceMock: Boolean = false) {
        _currentScreen.value = AppScreen.Loading
        startLoadingAnimations()

        viewModelScope.launch {
            try {
                // Call Gemini API through GeminiManager
                val result = GeminiManager.assessBusiness(_answers.value, forceMock)
                _assessmentResult.value = result
                delay(1000) // smooth completion transition
                _currentScreen.value = AppScreen.Results
            } catch (e: Exception) {
                Log.e(TAG, "Assessment failed, fallback triggered automatically.", e)
                val fallback = GeminiManager.getMariaNakatoFallback()
                _assessmentResult.value = fallback
                _currentScreen.value = AppScreen.Results
            } finally {
                stopLoadingAnimations()
            }
        }
    }

    fun quickDemo() {
        // Preloads Maria Nakato's answers and triggers mock submission immediately
        _answers.value = BusinessAnswers(
            businessName = "Maria Nakato Clothes",
            businessType = "market vendor",
            operatingTime = "1–3 years",
            employeeCount = "0",
            isRegistered = false,
            monthlyRevenue = "500k–2M",
            monthlyExpenses = "500k–2M",
            mobileMoneyUsage = "both",
            mobileMoneyTime = "2+ years",
            hasBankStatement = false,
            recordKeeping = "sometimes",
            loanHistory = "never",
            savingsHabit = "yes at home",
            ownedAssets = "equipment/vehicles",
            loanReason = "buy stock/inventory",
            loanAmount = "1M–5M",
            targetTimeline = "within 3 months"
        )
        submitAssessment(forceMock = true)
    }

    fun retake() {
        _answers.value = BusinessAnswers()
        _currentFormStep.value = 1
        _currentScreen.value = AppScreen.Landing
        _assessmentResult.value = null
    }

    private fun startLoadingAnimations() {
        rotationJob = viewModelScope.launch {
            var counter = 0
            while (true) {
                _isAnalyzingMessage.value = "${stages[counter % stages.size]}\n\n💡 Tip: ${tips[counter % tips.size]}"
                delay(3000)
                counter++
            }
        }
    }

    private fun stopLoadingAnimations() {
        rotationJob?.cancel()
        rotationJob = null
    }
}
