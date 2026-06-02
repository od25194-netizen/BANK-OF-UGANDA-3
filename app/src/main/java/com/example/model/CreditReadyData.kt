package com.example.model

import com.squareup.moshi.JsonClass

data class BusinessAnswers(
    // Step 1: About your business
    val businessName: String = "",
    val businessType: String = "",
    val operatingTime: String = "",
    val employeeCount: String = "",
    val isRegistered: Boolean = false,
    
    // Step 2: Your money
    val monthlyRevenue: String = "",
    val monthlyExpenses: String = "",
    val mobileMoneyUsage: String = "",
    val mobileMoneyTime: String = "",
    val hasBankStatement: Boolean = false,
    
    // Step 3: Your records and history
    val recordKeeping: String = "",
    val loanHistory: String = "",
    val savingsHabit: String = "",
    val ownedAssets: String = "",
    
    // Step 4: Your loan goal
    val loanReason: String = "",
    val loanAmount: String = "",
    val targetTimeline: String = ""
) {
    fun toFormattedPrompt(): String {
        return """
            Business Profile:
            - Business Name: $businessName
            - Business Type: $businessType
            - Operating Time: $operatingTime
            - Employee Count: $employeeCount
            - Registered with URSB: ${if (isRegistered) "Yes" else "No"}
            
            Financial Details:
            - Monthly Revenue Range: $monthlyRevenue
            - Monthly Expenses Range: $monthlyExpenses
            - Mobile Money Usage: $mobileMoneyUsage
            - Mobile Money Time: $mobileMoneyTime
            - Has Business Bank Account: ${if (hasBankStatement) "Yes" else "No"}
            
            Records and Credit Background:
            - Record Keeping Style: $recordKeeping
            - Loan History Status: $loanHistory
            - Regular Savings Practice: $savingsHabit
            - Owned Business/Personal Assets: $ownedAssets
            
            Loan Request Information:
            - Reason for Loan: $loanReason
            - Target Loan Size: $loanAmount
            - Urgency/Time Needed: $targetTimeline
        """.trimIndent()
    }
}

@JsonClass(generateAdapter = true)
data class Pillar(
    val name: String,
    val score: Int,
    val status: String, // "good", "fair", "weak"
    val explanation: String,
    val quick_win: String
)

@JsonClass(generateAdapter = true)
data class RoadmapStep(
    val timeline: String, // "This week", "This month", "In 3 months", "In 6 months"
    val action: String,
    val impact: String,
    val resource: String
)

@JsonClass(generateAdapter = true)
data class LoanMatch(
    val lender: String,
    val product: String,
    val eligible_now: Boolean,
    val amount_range: String,
    val reason: String,
    val requirement_to_qualify: String
)

@JsonClass(generateAdapter = true)
data class CreditAssessmentResult(
    val overall_score: Int,
    val grade: String,
    val summary: String,
    val pillars: List<Pillar>,
    val roadmap: List<RoadmapStep>,
    val loan_matches: List<LoanMatch>,
    val score_if_improved: Int,
    val improvement_message: String
)
