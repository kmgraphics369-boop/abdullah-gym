package com.example.data.model

import androidx.annotation.Keep

@Keep
data class OwnerProfile(
    val id: String = "",
    val userId: String = "",
    val gymName: String = "Iron Forge Fitness Club",
    val ownerName: String = "Imran",
    val ownerPhone: String = "+923001234567",
    val email: String? = null,
    val language: String = "en",
    val activeBranchId: String = "main",
    val twoFactorPin: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Keep
data class Branch(
    val id: String = "",
    val userId: String = "",
    val name: String = "Main Branch",
    val city: String = "Lahore",
    val phone: String = "",
    val address: String = "",
    val active: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Keep
data class Member(
    val id: String = "",
    val userId: String = "",
    val membershipId: String = "",
    val branchId: String = "main",
    val fullName: String = "",
    val fatherName: String? = null,
    val cnic: String? = null,
    val mobile: String = "",
    val whatsapp: String = "",
    val emergencyContact: String? = null,
    val address: String? = null,
    val gender: String? = null,
    val dateOfBirth: String? = null,
    val bloodGroup: String? = null,
    val packageName: String = "Monthly Standard",
    val monthlyFee: Long = 3000L,
    val pendingAmount: Long = 0L,
    val joiningDateMillis: Long = System.currentTimeMillis(),
    val startDateMillis: Long = System.currentTimeMillis(),
    val expiryDateMillis: Long = System.currentTimeMillis(),
    val paidThroughMillis: Long = System.currentTimeMillis(),
    val lastPaymentDateMillis: Long? = null,
    val dueDayOfMonth: Long = 1L,
    val trainerId: String? = null,
    val trainerName: String? = null,
    val status: String = "ACTIVE",
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val bodyFatPercent: Double? = null,
    val fitnessGoal: String? = null,
    val competitionPrep: Boolean? = null,
    val photoWebpBase64: String? = null,
    val cnicFrontBase64: String? = null,
    val cnicBackBase64: String? = null,
    val medicalCertBase64: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Keep
data class FeePayment(
    val id: String = "",
    val userId: String = "",
    val memberId: String = "",
    val memberName: String = "",
    val membershipId: String = "",
    val branchId: String = "main",
    val amount: Long = 0L,
    val discount: Long = 0L,
    val paymentMethod: String = "CASH",
    val forMonthLabel: String = "",
    val dueDateMillis: Long = System.currentTimeMillis(),
    val paidAtMillis: Long = System.currentTimeMillis(),
    val receiptNumber: String = "",
    val notes: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Keep
data class BodyProgressLog(
    val id: String = "",
    val userId: String = "",
    val memberId: String = "",
    val memberName: String = "",
    val recordedAtMillis: Long = System.currentTimeMillis(),
    val weightKg: Double = 0.0,
    val bodyFatPercent: Double = 0.0,
    val muscleMassKg: Double = 0.0,
    val chestCm: Double? = null,
    val waistCm: Double? = null,
    val armsCm: Double? = null,
    val thighsCm: Double? = null,
    val photoTag: String = "PROGRESS",
    val photoWebpBase64: String? = null,
    val notes: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Keep
data class AttendanceRecord(
    val id: String = "",
    val userId: String = "",
    val memberId: String = "",
    val memberName: String = "",
    val membershipId: String = "",
    val branchId: String = "main",
    val method: String = "MANUAL",
    val checkInMillis: Long = System.currentTimeMillis(),
    val checkOutMillis: Long? = null,
    val dateKey: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Keep
data class Trainer(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val mobile: String = "",
    val salary: Long = 0L,
    val specialization: String = "",
    val branchId: String = "main",
    val active: Boolean = true,
    val attendanceDaysThisMonth: Long = 0L,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Keep
data class WorkoutExercise(
    val name: String = "",
    val sets: Long = 4L,
    val reps: String = "10-12",
    val weightKg: Long = 0L,
    val videoUrl: String = "",
    val instructions: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "sets" to sets,
        "reps" to reps,
        "weightKg" to weightKg,
        "videoUrl" to videoUrl,
        "instructions" to instructions
    )
}

@Keep
data class WorkoutPlan(
    val id: String = "",
    val userId: String = "",
    val dayCategory: String = "CHEST",
    val title: String = "",
    val assignedMemberId: String? = null,
    val assignedMemberName: String? = null,
    val exercises: List<Map<String, Any>> = emptyList(),
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    fun parsedExercises(): List<WorkoutExercise> = exercises.map { map ->
        WorkoutExercise(
            name = map["name"] as? String ?: "",
            sets = (map["sets"] as? Number)?.toLong() ?: 4L,
            reps = map["reps"] as? String ?: "10-12",
            weightKg = (map["weightKg"] as? Number)?.toLong() ?: 0L,
            videoUrl = map["videoUrl"] as? String ?: "",
            instructions = map["instructions"] as? String ?: ""
        )
    }
}

@Keep
data class DietPlan(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val goal: String = "",
    val assignedMemberId: String? = null,
    val assignedMemberName: String? = null,
    val breakfast: String = "",
    val lunch: String = "",
    val snacks: String = "",
    val dinner: String = "",
    val totalCalories: Long = 0L,
    val totalProteinGrams: Long = 0L,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Keep
data class Supplement(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "WHEY_PROTEIN",
    val brand: String = "",
    val stockQty: Long = 0L,
    val soldQty: Long = 0L,
    val unitPrice: Long = 0L,
    val expiryDateMillis: Long = System.currentTimeMillis(),
    val branchId: String = "main",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Keep
data class Expense(
    val id: String = "",
    val userId: String = "",
    val category: String = "RENT",
    val title: String = "",
    val amount: Long = 0L,
    val expenseDateMillis: Long = System.currentTimeMillis(),
    val branchId: String = "main",
    val notes: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Keep
data class Equipment(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "Strength",
    val purchaseDateMillis: Long = System.currentTimeMillis(),
    val cost: Long = 0L,
    val nextMaintenanceMillis: Long = System.currentTimeMillis(),
    val condition: String = "GOOD",
    val branchId: String = "main",
    val notes: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Keep
data class AuditLog(
    val id: String = "",
    val userId: String = "",
    val action: String = "",
    val actorEmail: String = "",
    val details: String = "",
    val timestampMillis: Long = System.currentTimeMillis(),
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)
