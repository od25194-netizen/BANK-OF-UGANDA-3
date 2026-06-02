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
import com.google.firebase.auth.FirebaseAuth

enum class AppScreen {
    Auth,
    Landing,
    Form,
    Loading,
    Results
}

class CreditViewModel : ViewModel() {
    private val TAG = "CreditViewModel"

    private val _currentScreen = MutableStateFlow(AppScreen.Auth)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e("CreditViewModel", "Firebase App not initialized or missing config, operating in Demo Auth Mode.", e)
            null
        }
    }

    private val _currentUserEmail = MutableStateFlow<String?>(null)
    val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    init {
        try {
            val auth = firebaseAuth
            if (auth?.currentUser != null) {
                _currentUserEmail.value = auth.currentUser?.email
                _currentScreen.value = AppScreen.Landing
            } else {
                _currentScreen.value = AppScreen.Auth
            }
        } catch (e: Exception) {
            _currentScreen.value = AppScreen.Auth
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun signIn(email: String, password: String, onSuccess: () -> Unit) {
        _authLoading.value = true
        _authError.value = null
        val auth = firebaseAuth
        if (auth != null) {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    _authLoading.value = false
                    if (task.isSuccessful) {
                        _currentUserEmail.value = auth.currentUser?.email ?: email
                        _currentScreen.value = AppScreen.Landing
                        onSuccess()
                    } else {
                        _authError.value = task.exception?.localizedMessage ?: "Invalid login credentials."
                    }
                }
        } else {
            // Local fallback demo auth mode if firebase init failed or throws
            viewModelScope.launch {
                delay(1200)
                _authLoading.value = false
                if (email.contains("@") && password.length >= 6) {
                    _currentUserEmail.value = email
                    _currentScreen.value = AppScreen.Landing
                    onSuccess()
                } else {
                    _authError.value = "Demo Mode: Email must contain '@' and Password must be at least 6 characters."
                }
            }
        }
    }

    fun signUp(email: String, password: String, onSuccess: () -> Unit) {
        _authLoading.value = true
        _authError.value = null
        val auth = firebaseAuth
        if (auth != null) {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    _authLoading.value = false
                    if (task.isSuccessful) {
                        _currentUserEmail.value = auth.currentUser?.email ?: email
                        _currentScreen.value = AppScreen.Landing
                        onSuccess()
                    } else {
                        _authError.value = task.exception?.localizedMessage ?: "Sign up failed."
                    }
                }
        } else {
            // Local fallback demo auth mode
            viewModelScope.launch {
                delay(1200)
                _authLoading.value = false
                if (email.contains("@") && password.length >= 6) {
                    _currentUserEmail.value = email
                    _currentScreen.value = AppScreen.Landing
                    onSuccess()
                } else {
                    _authError.value = "Demo Mode: Email must contain '@' and Password must be at least 6 characters."
                }
            }
        }
    }

    fun signOut() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Sign out error", e)
        }
        _currentUserEmail.value = null
        _currentScreen.value = AppScreen.Auth
    }

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
