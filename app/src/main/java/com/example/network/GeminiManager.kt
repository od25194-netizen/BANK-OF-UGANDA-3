package com.example.network

import com.example.model.CreditAssessmentResult
import com.example.model.Pillar
import com.example.model.RoadmapStep
import com.example.model.LoanMatch
import com.example.model.BusinessAnswers
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import android.util.Log

// --- Gemini request/response model declarations for Moshi ---

@JsonClass(generateAdapter = true)
data class Part(val text: String? = null)

@JsonClass(generateAdapter = true)
data class Content(val parts: List<Part>)

@JsonClass(generateAdapter = true)
data class ResponseFormat(val responseMimeType: String)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(val content: Content)

@JsonClass(generateAdapter = true)
data class GeminiResponse(val candidates: List<Candidate>?)

interface GeminiApiService {
  @POST("v1beta/models/gemini-3.5-flash:generateContent")
  suspend fun generateContent(
      @Query("key") apiKey: String,
      @Body request: GeminiRequest
  ): GeminiResponse
}

object GeminiManager {
    private const val TAG = "GeminiManager"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val apiService = retrofit.create(GeminiApiService::class.java)

    /**
     * Executes AI Scoring with Gemini or returns fallback mock data if API limits are reached
     * or API key is not set.
     */
    suspend fun assessBusiness(answers: BusinessAnswers, useMock: Boolean = false): CreditAssessmentResult {
        if (useMock) {
            Log.d(TAG, "Using mock data by user request.")
            return getMariaNakatoFallback()
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Empty or placeholder GEMINI_API_KEY detected. Fast failing to mock data.")
            return getMariaNakatoFallback()
        }

        val systemInstruction = """
            You are SME CreditReady, a world-class financial inclusion expert specializing in Ugandan SME lending and credit readiness coaching.
            Your role is to analyze a small business or informal trader's details and provide a realistic credit eligibility score, educational roadmap, and lender matches.
            
            You understand the exact mechanics of Uganda's diverse financial system:
            1. SACCOs (Savings and Credit Co-operative Societies, suitable for informal traders/lowest barriers, e.g. Owino Market SACCO, Boda Boda SACCOs)
            2. Microfinance Institutions (e.g., Pride Microfinance, FINCA Uganda, UGAFODE, Opportunity Bank - offer medium barriers, require basic record keeping)
            3. Tier 1 Commercial Banks (e.g., Stanbic Bank, DFCU Bank, Centenary Bank, Equity Bank, PostBank - require registration, 6-12 months records, higher revenue)
            4. Digital / Mobile Money lenders (e.g., MTN MoMo Wewole, Airtel Money / Jumo, which look specifically at mobile money cash flows and require no paperwork)

            Evaluate the user's details across five core pillars, scoring each 0 to 100:
            - "Cash Flow Evidence"
            - "Record Keeping"
            - "Business Formality"
            - "Credit History"
            - "Asset Base"

            Determine an overall credit readiness score (0-100) and letter grade (A, B, C, D, F, with + or - accents if appropriate).
            
            Be highly encouraging but realistic in your review. Mention specific Ugandan entities, SACCOs, mobile money platforms, and regional options. All advice must use currency 'UGX'.
            
            YOU MUST RETURN RESPONSE MATCHING THIS EXACT JSON STRUCTURE, WITH NO OTHER TEXT, MARKDOWN FORMATTING OR PREAMBLE:
            {
              "overall_score": 58,
              "grade": "C+",
              "summary": "2-3 sentence financial assessment in plain English focusing specifically on Ugandan challenges.",
              "pillars": [
                {
                  "name": "Cash Flow Evidence",
                  "score": 70,
                  "status": "weak|fair|good",
                  "explanation": "Why they got this score",
                  "quick_win": "Specific actionable next step to perform this week"
                },
                ... (must supply exactly 5 pillars)
              ],
              "roadmap": [
                {
                  "timeline": "This week",
                  "action": "Immediate tactical task",
                  "impact": "Score benefit estimation",
                  "resource": "Specific Ugandan agency, app (e.g., Mauzo, Kyaddondo SACCO, URBS portal) or digital platform"
                },
                ... (provide exactly 4 roadmap steps: 'This week', 'This month', 'In 3 months', 'In 6 months')
              ],
              "loan_matches": [
                {
                  "lender": "Lender Name (e.g. Pride Microfinance or MTN Wewole)",
                  "product": "Product Name (e.g. MoMo Business Loan or Boda Boda MicroLoan)",
                  "eligible_now": true,
                  "amount_range": "UGX 500k - 2M",
                  "reason": "Why they qualify or don't",
                  "requirement_to_qualify": "What specific item is missing to unlock this lender"
                }
              ],
              "score_if_improved": 75,
              "improvement_message": "A supportive sentence stating what the score can become and which tier of banking they could unlock."
            }
        """.trimIndent()

        val prompt = answers.toFormattedPrompt()

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.4f
            )
        )

        return try {
            val response = apiService.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                Log.d(TAG, "Raw response: $jsonText")
                val cleanJson = cleanJsonResponse(jsonText)
                val adapter = moshi.adapter(CreditAssessmentResult::class.java)
                adapter.fromJson(cleanJson) ?: throw Exception("Moshi returned null for parsed result")
            } else {
                throw Exception("Response text body is null")
            }
        } catch (e: Exception) {
            Log.e(TAG, "API failed with exception. Falling back to mock data.", e)
            getMariaNakatoFallback()
        }
    }

    /**
     * Cleans up potential markdown code fences from the API string.
     */
    private fun cleanJsonResponse(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.substringAfter("```json").substringBeforeLast("```").trim()
        } else if (clean.startsWith("```")) {
            clean = clean.substringAfter("```").substringBeforeLast("```").trim()
        }
        return clean
    }

    /**
     * Maria Nakato standard fallback state for demo offline presentation
     */
    fun getMariaNakatoFallback(): CreditAssessmentResult {
        return CreditAssessmentResult(
            overall_score = 52,
            grade = "C",
            summary = "Maria Nakato runs a clothing supply business in Owino Market. While she has strong cash flows through regular MTN Mobile Money usage, her lack of formal URSB business registration and raw record-keeping limits access to main commercial banks. By setting up structured sales logs and formalizing her shop profile, she is highly likely to secure attractive financing options.",
            pillars = listOf(
                Pillar(
                    name = "Cash Flow Evidence",
                    score = 68,
                    status = "fair",
                    explanation = "Extensive daily volume on MTN Mobile Money, showing robust turn-over but undocumented monthly profitability patterns.",
                    quick_win = "Separate personal mobile money transactions from business payments immediately."
                ),
                Pillar(
                    name = "Record Keeping",
                    score = 22,
                    status = "weak",
                    explanation = "Currently relies entirely on memory and basic physical notes. This lacks the formal ledger systems that formal commercial banks mandate.",
                    quick_win = "Download the Mauzo digital record app and log every single apparel sale and stock purchase."
                ),
                Pillar(
                    name = "Business Formality",
                    score = 35,
                    status = "weak",
                    explanation = "Operating as an informal sole proprietor in Kampala without a URSB business license or formal local government trade permit.",
                    quick_win = "Save UGX 24,000 to register a business name 'Nakato Clothing Enterprise' online at the URSB website."
                ),
                Pillar(
                    name = "Credit History",
                    score = 60,
                    status = "fair",
                    explanation = "No historical default record and consistent airtime loans paid, but has no credit history within the Credit Reference Bureau (CRB).",
                    quick_win = "Initiate small micro-loans of UGX 50,000 on MTN MoMo Wewole and repay on time before the due date."
                ),
                Pillar(
                    name = "Asset Base",
                    score = 55,
                    status = "fair",
                    explanation = "Owns basic productive equipment (sewing machines) and holds a localized market tenancy stall in Kampala, but lacks formal land titles.",
                    quick_win = "Keep written receipts of all sewing machine upgrades and market slot lease payments to present as non-financial assets."
                )
            ),
            roadmap = listOf(
                RoadmapStep(
                    timeline = "This week",
                    action = "Download Mauzo app and begin logging sales",
                    impact = "Creates immediate verifiable ledger history to bypass bank-statement rejection.",
                    resource = "Mauzo bookkeeping mobile application (available on Google Play Store)."
                ),
                RoadmapStep(
                    timeline = "This month",
                    action = "Apply for a URSB business name reservation",
                    impact = "Unlocks formal company status to access tailored enterprise credit with lower interest rates.",
                    resource = "URSB (Uganda Registration Services Bureau) online name search portal."
                ),
                RoadmapStep(
                    timeline = "In 3 months",
                    action = "Establish a dedicated business wallet / basic bank account",
                    impact = "Builds structured transaction records showing consistent working income.",
                    resource = "Centenary Bank 'SupaWoman' account or Pride Microfinance business savings plan."
                ),
                RoadmapStep(
                    timeline = "In 6 months",
                    action = "Present updated digital record records for review",
                    impact = "Unlocks competitive collateral-free credit lines from Tier-2 microfinance institutions.",
                    resource = "Pride Microfinance SME growth capital loan."
                )
            ),
            loan_matches = listOf(
                LoanMatch(
                    lender = "MTN MoMo Wewole (powered by Jumo)",
                    product = "MoMo Cash Advance",
                    eligible_now = true,
                    amount_range = "UGX 100,000 – 1,500,000",
                    reason = "Approved instantly because her historical MTN transaction volume is highly positive and demonstrates regular cash velocity.",
                    requirement_to_qualify = "None - fully eligible immediately based on MTN transactions."
                ),
                LoanMatch(
                    lender = "Pride Microfinance",
                    product = "Group Business Loan",
                    eligible_now = false,
                    amount_range = "UGX 500,000 – 3,000,000",
                    reason = "Requires membership in a Kampala market vendor credit group or basic recorded ledger books to prove 3 months of regular income.",
                    requirement_to_qualify = "Provide 3 months of documented bookkeeping logs from the Mauzo app."
                ),
                LoanMatch(
                    lender = "PostBank Uganda",
                    product = "SME Progression Loan",
                    eligible_now = false,
                    amount_range = "UGX 2,000,000 – 15,000,000",
                    reason = "Requires full URSB business registration certificate and a dedicated business bank account showing regular cash deposits.",
                    requirement_to_qualify = "Register business name at URSB and present formal trade license history."
                )
            ),
            score_if_improved = 74,
            improvement_message = "If you complete the 3-month roadmap, your credit readiness score could reach 74 — enough to formally qualify at PostBank and unlock low-interest microfinance credit lines."
        )
    }
}
