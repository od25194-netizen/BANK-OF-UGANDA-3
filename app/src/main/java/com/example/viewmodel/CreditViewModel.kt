package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.BusinessAnswers
import com.example.model.CreditAssessmentResult
import com.example.network.GeminiManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppScreen {
    Auth,
    Landing,
    Form,
    Loading,
    Results
}

data class HistoryEntry(
    val timestamp: Long,
    val score: Int,
    val grade: String,
    val businessName: String
)

class CreditViewModel(application: Application) : AndroidViewModel(application) {
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

    private val firebaseDatabase: com.google.firebase.database.FirebaseDatabase? by lazy {
        try {
            com.google.firebase.database.FirebaseDatabase.getInstance()
        } catch (e: Exception) {
            Log.e("CreditViewModel", "Firebase Realtime Database missing or not configured.", e)
            null
        }
    }

    private val _currentUserEmail = MutableStateFlow<String?>(null)
    val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _currentLanguageCode = MutableStateFlow("en")
    val currentLanguageCode: StateFlow<String> = _currentLanguageCode.asStateFlow()

    private val _historyList = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val historyList: StateFlow<List<HistoryEntry>> = _historyList.asStateFlow()

    // --- Enterprise Grade States ---
    private val _isDarkTheme = MutableStateFlow<Boolean?>(null)
    val isDarkTheme: StateFlow<Boolean?> = _isDarkTheme.asStateFlow()

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone.asStateFlow()

    private val _gender = MutableStateFlow("")
    val gender: StateFlow<String> = _gender.asStateFlow()

    private val _dob = MutableStateFlow("")
    val dob: StateFlow<String> = _dob.asStateFlow()

    private val _nationalId = MutableStateFlow("")
    val nationalId: StateFlow<String> = _nationalId.asStateFlow()

    private val _businessLogoUriString = MutableStateFlow<String?>(null)
    val businessLogoUriString: StateFlow<String?> = _businessLogoUriString.asStateFlow()

    private val _profileImageUriString = MutableStateFlow<String?>(null)
    val profileImageUriString: StateFlow<String?> = _profileImageUriString.asStateFlow()

    private val _scoreAlertsEnabled = MutableStateFlow(true)
    val scoreAlertsEnabled: StateFlow<Boolean> = _scoreAlertsEnabled.asStateFlow()

    private val _roadmapRemindersEnabled = MutableStateFlow(true)
    val roadmapRemindersEnabled: StateFlow<Boolean> = _roadmapRemindersEnabled.asStateFlow()

    private val _lenderUpdatesEnabled = MutableStateFlow(true)
    val lenderUpdatesEnabled: StateFlow<Boolean> = _lenderUpdatesEnabled.asStateFlow()

    private val _shareWithLendersEnabled = MutableStateFlow(true)
    val shareWithLendersEnabled: StateFlow<Boolean> = _shareWithLendersEnabled.asStateFlow()

    private val _checkedRoadmapSteps = MutableStateFlow<Set<String>>(emptySet())
    val checkedRoadmapSteps: StateFlow<Set<String>> = _checkedRoadmapSteps.asStateFlow()

    private val _memberSince = MutableStateFlow("June 2026")
    val memberSince: StateFlow<String> = _memberSince.asStateFlow()

    private val _lastSeen = MutableStateFlow("")
    val lastSeen: StateFlow<String> = _lastSeen.asStateFlow()

    init {
        // Load language preference
        val prefs = getApplication<Application>().getSharedPreferences("credit_ready_prefs", Context.MODE_PRIVATE)
        _currentLanguageCode.value = prefs.getString("language_code", "en") ?: "en"

        // Load Dark Theme Pref
        if (prefs.contains("dark_theme_enabled")) {
            _isDarkTheme.value = prefs.getBoolean("dark_theme_enabled", false)
        } else {
            _isDarkTheme.value = null
        }

        // Load enterprise states
        _phone.value = prefs.getString("user_phone", "") ?: ""
        _gender.value = prefs.getString("user_gender", "") ?: ""
        _dob.value = prefs.getString("user_dob", "") ?: ""
        _nationalId.value = prefs.getString("user_nid", "") ?: ""
        _businessLogoUriString.value = prefs.getString("business_logo_uri", null)
        _profileImageUriString.value = prefs.getString("profile_image_uri", null)
        _scoreAlertsEnabled.value = prefs.getBoolean("score_alerts", true)
        _roadmapRemindersEnabled.value = prefs.getBoolean("roadmap_reminders", true)
        _lenderUpdatesEnabled.value = prefs.getBoolean("lender_updates", true)
        _shareWithLendersEnabled.value = prefs.getBoolean("share_with_lenders", true)
        _checkedRoadmapSteps.value = prefs.getStringSet("checked_roadmap_steps", emptySet())?.toSet() ?: emptySet()
        _memberSince.value = prefs.getString("member_since", "June 2026") ?: "June 2026"
        _lastSeen.value = prefs.getString("last_seen", "") ?: ""
        
        loadHistory()

        try {
            val auth = firebaseAuth
            if (auth?.currentUser != null) {
                _currentUserEmail.value = auth.currentUser?.email
                _currentScreen.value = AppScreen.Landing
                fetchLanguageFromFirebase()
                syncSessionActivity()
            } else {
                _currentScreen.value = AppScreen.Auth
            }
        } catch (e: Exception) {
            _currentScreen.value = AppScreen.Auth
        }
    }

    fun setDarkTheme(enabled: Boolean?) {
        _isDarkTheme.value = enabled
        val prefs = getApplication<Application>().getSharedPreferences("credit_ready_prefs", Context.MODE_PRIVATE)
        if (enabled == null) {
            prefs.edit().remove("dark_theme_enabled").apply()
        } else {
            prefs.edit().putBoolean("dark_theme_enabled", enabled).apply()
        }
    }

    fun updateProfileInfo(phoneVal: String, genderVal: String, dobVal: String, nationalIdVal: String) {
        _phone.value = phoneVal
        _gender.value = genderVal
        _dob.value = dobVal
        _nationalId.value = nationalIdVal

        val prefs = getApplication<Application>().getSharedPreferences("credit_ready_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("user_phone", phoneVal)
            .putString("user_gender", genderVal)
            .putString("user_dob", dobVal)
            .putString("user_nid", nationalIdVal)
            .apply()

        syncProfileToFirebase()
    }

    fun setProfileImageUri(uriString: String?) {
        _profileImageUriString.value = uriString
        val prefs = getApplication<Application>().getSharedPreferences("credit_ready_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("profile_image_uri", uriString).apply()
        syncProfileToFirebase()
    }

    fun setBusinessLogoUri(uriString: String?) {
        _businessLogoUriString.value = uriString
        val prefs = getApplication<Application>().getSharedPreferences("credit_ready_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("business_logo_uri", uriString).apply()
        syncProfileToFirebase()
    }

    fun toggleScoreAlerts(enabled: Boolean) {
        _scoreAlertsEnabled.value = enabled
        getApplication<Application>().getSharedPreferences("credit_ready_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("score_alerts", enabled).apply()
    }

    fun toggleRoadmapReminders(enabled: Boolean) {
        _roadmapRemindersEnabled.value = enabled
        getApplication<Application>().getSharedPreferences("credit_ready_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("roadmap_reminders", enabled).apply()
    }

    fun toggleLenderUpdates(enabled: Boolean) {
        _lenderUpdatesEnabled.value = enabled
        getApplication<Application>().getSharedPreferences("credit_ready_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("lender_updates", enabled).apply()
    }

    fun toggleShareWithLenders(enabled: Boolean) {
        _shareWithLendersEnabled.value = enabled
        getApplication<Application>().getSharedPreferences("credit_ready_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("share_with_lenders", enabled).apply()
    }

    fun toggleRoadmapStep(stepTitle: String) {
        val current = _checkedRoadmapSteps.value.toMutableSet()
        if (current.contains(stepTitle)) {
            current.remove(stepTitle)
        } else {
            current.add(stepTitle)
        }
        _checkedRoadmapSteps.value = current
        getApplication<Application>().getSharedPreferences("credit_ready_prefs", Context.MODE_PRIVATE)
            .edit().putStringSet("checked_roadmap_steps", current).apply()

        // Sync roadmap completion to database
        val uid = firebaseAuth?.currentUser?.uid
        val db = firebaseDatabase
        if (uid != null && db != null) {
            try {
                db.getReference("users").child(uid).child("checkedRoadmapSteps").setValue(current.toList())
            } catch (e: Exception) {
                Log.e(TAG, "Failed syncing roadmap steps to cloud", e)
            }
        }
    }

    fun resetPassword(email: String, onComplete: (Boolean, String?) -> Unit) {
        val auth = firebaseAuth
        if (auth != null) {
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onComplete(true, null)
                    } else {
                        onComplete(false, task.exception?.localizedMessage ?: "Failed to send reset link.")
                    }
                }
        } else {
            // Local fallback demo auth mode
            viewModelScope.launch {
                delay(1200)
                if (email.contains("@")) {
                    onComplete(true, null)
                } else {
                    onComplete(false, "Demo Mode: Invalid email address format.")
                }
            }
        }
    }

    fun changePassword(newPass: String, onComplete: (Boolean, String?) -> Unit) {
        val user = firebaseAuth?.currentUser
        if (user != null) {
            user.updatePassword(newPass)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onComplete(true, null)
                    } else {
                        onComplete(false, task.exception?.localizedMessage ?: "Failed updating password.")
                    }
                }
        } else {
            viewModelScope.launch {
                delay(1000)
                if (newPass.length >= 8) {
                    onComplete(true, null)
                } else {
                    onComplete(false, "Password must be at least 8 characters.")
                }
            }
        }
    }

    fun deleteAccount(onComplete: (Boolean, String?) -> Unit) {
        val user = firebaseAuth?.currentUser
        if (user != null) {
            user.delete()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        signOut()
                        onComplete(true, null)
                    } else {
                        onComplete(false, task.exception?.localizedMessage ?: "Failed to delete account.")
                    }
                }
        } else {
            viewModelScope.launch {
                delay(1000)
                signOut()
                onComplete(true, null)
            }
        }
    }

    private fun syncProfileToFirebase() {
        val uid = firebaseAuth?.currentUser?.uid ?: return
        val db = firebaseDatabase ?: return
        try {
            val userRef = db.getReference("users").child(uid)
            val profileMap = mapOf(
                "phone" to _phone.value,
                "gender" to _gender.value,
                "dob" to _dob.value,
                "nid" to _nationalId.value,
                "businessLogo" to (_businessLogoUriString.value ?: ""),
                "profileImage" to (_profileImageUriString.value ?: "")
            )
            userRef.child("profile").setValue(profileMap)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync profile specs to RTDB", e)
        }
    }

    private fun syncSessionActivity() {
        val uid = firebaseAuth?.currentUser?.uid ?: return
        val db = firebaseDatabase ?: return
        val timestamp = System.currentTimeMillis()
        val dateString = java.text.DateFormat.getDateTimeInstance().format(java.util.Date(timestamp))
        _lastSeen.value = dateString
        getApplication<Application>().getSharedPreferences("credit_ready_prefs", Context.MODE_PRIVATE)
            .edit().putString("last_seen", dateString).apply()

        try {
            val userRef = db.getReference("users").child(uid)
            userRef.child("lastSeen").setValue(dateString)
            userRef.child("memberSince").get().addOnSuccessListener { snap ->
                val existing = snap.getValue(String::class.java)
                if (existing.isNullOrEmpty()) {
                    val jDate = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.US).format(java.util.Date())
                    val memberSinceString = "Member since $jDate"
                    _memberSince.value = memberSinceString
                    userRef.child("memberSince").setValue(memberSinceString)
                    getApplication<Application>().getSharedPreferences("credit_ready_prefs", Context.MODE_PRIVATE)
                        .edit().putString("member_since", memberSinceString).apply()
                } else {
                    _memberSince.value = existing
                    getApplication<Application>().getSharedPreferences("credit_ready_prefs", Context.MODE_PRIVATE)
                        .edit().putString("member_since", existing).apply()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed syncing activity", e)
        }
    }

    fun setLanguage(code: String) {
        _currentLanguageCode.value = code
        val prefs = getApplication<Application>().getSharedPreferences("credit_ready_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("language_code", code).apply()

        // Sync to firebase Realtime Database
        val uid = firebaseAuth?.currentUser?.uid
        val db = firebaseDatabase
        if (uid != null && db != null) {
            try {
                db.getReference("users").child(uid).child("languageCode").setValue(code)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write languageCode to Realtime Database", e)
            }
        }
    }

    private fun fetchLanguageFromFirebase() {
        val uid = firebaseAuth?.currentUser?.uid ?: return
        val db = firebaseDatabase ?: return
        try {
            db.getReference("users").child(uid).child("languageCode").get()
                .addOnSuccessListener { snapshot ->
                    val code = snapshot.getValue(String::class.java)
                    if (!code.isNullOrEmpty()) {
                        _currentLanguageCode.value = code
                        val prefs = getApplication<Application>().getSharedPreferences("credit_ready_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putString("language_code", code).apply()
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read languageCode from RTDB", e)
        }
    }

    fun loadHistory() {
        val prefs = getApplication<Application>().getSharedPreferences("credit_ready_prefs", Context.MODE_PRIVATE)
        val dataStr = prefs.getString("assessment_history", "") ?: ""
        if (dataStr.isEmpty()) {
            _historyList.value = emptyList()
            return
        }
        val entries = dataStr.split("|").mapNotNull {
            try {
                val tokens = it.split(":")
                if (tokens.size >= 3) {
                    val timestamp = tokens[0].toLong()
                    val score = tokens[1].toInt()
                    val grade = tokens[2]
                    val bname = if (tokens.size >= 4) tokens[3] else "SME Shop"
                    HistoryEntry(timestamp, score, grade, bname)
                } else null
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.timestamp }
        _historyList.value = entries
    }

    fun saveAssessmentToHistory(score: Int, grade: String, businessName: String) {
        val timestamp = System.currentTimeMillis()
        val entry = "$timestamp:$score:$grade:${businessName.ifEmpty { "SME Shop" }.replace(":", "").replace("|", "")}"
        val prefs = getApplication<Application>().getSharedPreferences("credit_ready_prefs", Context.MODE_PRIVATE)
        val existing = prefs.getString("assessment_history", "") ?: ""
        val update = if (existing.isEmpty()) entry else "$existing|$entry"
        prefs.edit().putString("assessment_history", update).apply()
        
        loadHistory()

        // Sync to firebase Realtime Database
        val uid = firebaseAuth?.currentUser?.uid
        val db = firebaseDatabase
        if (uid != null && db != null) {
            try {
                val userRef = db.getReference("users").child(uid)
                val assessmentMap = mapOf(
                    "score" to score,
                    "grade" to grade,
                    "businessName" to businessName,
                    "timestamp" to timestamp
                )
                userRef.child("assessments").push().setValue(assessmentMap)
                    .addOnSuccessListener {
                        Log.d(TAG, "Synchronized assessment to Realtime Database.")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync assessment to Realtime Database", e)
            }
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
                        syncSessionActivity()
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
                    syncSessionActivity()
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
                        syncSessionActivity()
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
                    syncSessionActivity()
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

    val currentUserId: String
        get() = firebaseAuth?.currentUser?.uid ?: "SME-90210-UG"

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
                saveAssessmentToHistory(result.overall_score, result.grade, _answers.value.businessName)
                delay(1000) // smooth completion transition
                _currentScreen.value = AppScreen.Results
            } catch (e: Exception) {
                Log.e(TAG, "Assessment failed, fallback triggered automatically.", e)
                val fallback = GeminiManager.getMariaNakatoFallback()
                _assessmentResult.value = fallback
                saveAssessmentToHistory(fallback.overall_score, fallback.grade, _answers.value.businessName)
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
