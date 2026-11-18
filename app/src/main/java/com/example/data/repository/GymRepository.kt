package com.example.data.repository

import android.content.Context
import com.example.data.local.GymDatabase
import com.example.data.local.GymJsonCodec
import com.example.data.local.GymRecordDao
import com.example.data.local.GymRecordEntity
import com.example.data.model.AttendanceRecord
import com.example.data.model.AuditLog
import com.example.data.model.BodyProgressLog
import com.example.data.model.Branch
import com.example.data.model.DietPlan
import com.example.data.model.Equipment
import com.example.data.model.Expense
import com.example.data.model.FeeEngine
import com.example.data.model.FeePayment
import com.example.data.model.Member
import com.example.data.model.OwnerProfile
import com.example.data.model.Supplement
import com.example.data.model.Trainer
import com.example.data.model.WorkoutPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class GymRepository(
    private val dao: GymRecordDao
) {
    constructor(context: Context) : this(
        GymDatabase.getInstance(context.applicationContext).gymRecordDao()
    )

    private object Collections {
        const val OWNER_PROFILE = "owner_profile"
        const val BRANCHES = "branches"
        const val MEMBERS = "members"
        const val PAYMENTS = "payments"
        const val BODY_LOGS = "body_logs"
        const val ATTENDANCE = "attendance"
        const val TRAINERS = "trainers"
        const val WORKOUT_PLANS = "workout_plans"
        const val DIET_PLANS = "diet_plans"
        const val SUPPLEMENTS = "supplements"
        const val EXPENSES = "expenses"
        const val EQUIPMENT = "equipment"
        const val AUDIT_LOGS = "audit_logs"
    }

    fun observeOwnerProfile(uid: String): Flow<OwnerProfile?> =
        dao.observeCollection(uid, Collections.OWNER_PROFILE).map { list ->
            list.firstOrNull()?.let { GymJsonCodec.decodeOwnerProfile(it.jsonPayload) }
        }

    fun observeBranches(uid: String): Flow<List<Branch>> =
        dao.observeCollection(uid, Collections.BRANCHES).map { list ->
            list.map { GymJsonCodec.decodeBranch(it.jsonPayload) }
        }

    fun observeMembers(uid: String): Flow<List<Member>> =
        dao.observeCollection(uid, Collections.MEMBERS).map { list ->
            list.map { GymJsonCodec.decodeMember(it.jsonPayload) }.sortedBy { it.fullName }
        }

    fun observePayments(uid: String): Flow<List<FeePayment>> =
        dao.observeCollection(uid, Collections.PAYMENTS).map { list ->
            list.map { GymJsonCodec.decodePayment(it.jsonPayload) }.sortedByDescending { it.paidAtMillis }
        }

    fun observeBodyLogs(uid: String): Flow<List<BodyProgressLog>> =
        dao.observeCollection(uid, Collections.BODY_LOGS).map { list ->
            list.map { GymJsonCodec.decodeBodyLog(it.jsonPayload) }.sortedBy { it.recordedAtMillis }
        }

    fun observeAttendance(uid: String): Flow<List<AttendanceRecord>> =
        dao.observeCollection(uid, Collections.ATTENDANCE).map { list ->
            list.map { GymJsonCodec.decodeAttendance(it.jsonPayload) }.sortedByDescending { it.checkInMillis }
        }

    fun observeTrainers(uid: String): Flow<List<Trainer>> =
        dao.observeCollection(uid, Collections.TRAINERS).map { list ->
            list.map { GymJsonCodec.decodeTrainer(it.jsonPayload) }.sortedBy { it.name }
        }

    fun observeWorkoutPlans(uid: String): Flow<List<WorkoutPlan>> =
        dao.observeCollection(uid, Collections.WORKOUT_PLANS).map { list ->
            list.map { GymJsonCodec.decodeWorkoutPlan(it.jsonPayload) }
        }

    fun observeDietPlans(uid: String): Flow<List<DietPlan>> =
        dao.observeCollection(uid, Collections.DIET_PLANS).map { list ->
            list.map { GymJsonCodec.decodeDietPlan(it.jsonPayload) }
        }

    fun observeSupplements(uid: String): Flow<List<Supplement>> =
        dao.observeCollection(uid, Collections.SUPPLEMENTS).map { list ->
            list.map { GymJsonCodec.decodeSupplement(it.jsonPayload) }.sortedBy { it.name }
        }

    fun observeExpenses(uid: String): Flow<List<Expense>> =
        dao.observeCollection(uid, Collections.EXPENSES).map { list ->
            list.map { GymJsonCodec.decodeExpense(it.jsonPayload) }.sortedByDescending { it.expenseDateMillis }
        }

    fun observeEquipment(uid: String): Flow<List<Equipment>> =
        dao.observeCollection(uid, Collections.EQUIPMENT).map { list ->
            list.map { GymJsonCodec.decodeEquipment(it.jsonPayload) }.sortedBy { it.nextMaintenanceMillis }
        }

    fun observeAuditLogs(uid: String): Flow<List<AuditLog>> =
        dao.observeCollection(uid, Collections.AUDIT_LOGS).map { list ->
            list.map { GymJsonCodec.decodeAuditLog(it.jsonPayload) }.sortedByDescending { it.timestampMillis }
        }

    suspend fun ensureOwnerSetup(uid: String, email: String?, displayName: String?): Result<Unit> {
        return try {
            val existingProfile = dao.getOne(uid, Collections.OWNER_PROFILE, uid)
            if (existingProfile == null) {
                val profile = OwnerProfile(
                    id = uid,
                    userId = uid,
                    gymName = "Iron Forge Fitness Club",
                    ownerName = displayName?.takeIf { it.isNotBlank() } ?: "Imran",
                    ownerPhone = "+923001234567",
                    email = email ?: "local-server@127.0.0.1",
                    language = "en",
                    activeBranchId = "main"
                )
                dao.upsert(
                    GymRecordEntity(
                        ownerId = uid,
                        collection = Collections.OWNER_PROFILE,
                        id = uid,
                        jsonPayload = GymJsonCodec.encodeOwnerProfile(profile)
                    )
                )
            }
            val mainBranch = dao.getOne(uid, Collections.BRANCHES, "main")
            if (mainBranch == null) {
                val branch = Branch(
                    id = "main",
                    userId = uid,
                    name = "Main Branch",
                    city = "Lahore",
                    phone = "+923001234567",
                    address = "Main Boulevard",
                    active = true
                )
                dao.upsert(
                    GymRecordEntity(
                        ownerId = uid,
                        collection = Collections.BRANCHES,
                        id = "main",
                        jsonPayload = GymJsonCodec.encodeBranch(branch)
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOwnerProfile(
        uid: String,
        gymName: String,
        ownerName: String,
        ownerPhone: String,
        language: String,
        activeBranchId: String,
        twoFactorPin: String?
    ): Result<Unit> {
        return try {
            val current = dao.getOne(uid, Collections.OWNER_PROFILE, uid)
                ?.let { GymJsonCodec.decodeOwnerProfile(it.jsonPayload) }
                ?: OwnerProfile(id = uid, userId = uid)
            val updated = current.copy(
                gymName = gymName.trim().take(100).ifBlank { current.gymName },
                ownerName = ownerName.trim().take(100).ifBlank { current.ownerName },
                ownerPhone = ownerPhone.trim().take(30),
                language = if (language == "ur") "ur" else "en",
                activeBranchId = activeBranchId.ifBlank { "main" },
                twoFactorPin = twoFactorPin?.trim()?.takeIf { it.isNotBlank() },
                updatedAtMillis = System.currentTimeMillis()
            )
            dao.upsert(
                GymRecordEntity(
                    ownerId = uid,
                    collection = Collections.OWNER_PROFILE,
                    id = uid,
                    jsonPayload = GymJsonCodec.encodeOwnerProfile(updated)
                )
            )
            logAudit(uid, "UPDATE_SETTINGS", "Updated gym profile ($gymName, lang=$language)")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveBranch(uid: String, branch: Branch): Result<Unit> {
        return try {
            val docId = branch.id.ifBlank { "br_${UUID.randomUUID().toString().take(8)}" }
            val saved = branch.copy(
                id = docId,
                userId = uid,
                name = branch.name.trim().take(100),
                city = branch.city.trim().take(80),
                phone = branch.phone.trim().take(30),
                address = branch.address.trim().take(250),
                updatedAtMillis = System.currentTimeMillis()
            )
            dao.upsert(
                GymRecordEntity(
                    ownerId = uid,
                    collection = Collections.BRANCHES,
                    id = docId,
                    jsonPayload = GymJsonCodec.encodeBranch(saved)
                )
            )
            logAudit(uid, "SAVE_BRANCH", "Branch ${saved.name} (${saved.city})")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerOrUpdateMember(uid: String, member: Member): Result<String> {
        return try {
            val isNew = member.id.isBlank()
            val docId = member.id.ifBlank { "mem_${UUID.randomUUID().toString().take(10)}" }
            val saved = member.copy(
                id = docId,
                userId = uid,
                updatedAtMillis = System.currentTimeMillis()
            )
            dao.upsert(
                GymRecordEntity(
                    ownerId = uid,
                    collection = Collections.MEMBERS,
                    id = docId,
                    jsonPayload = GymJsonCodec.encodeMember(saved)
                )
            )
            logAudit(
                uid,
                if (isNew) "REGISTER_MEMBER" else "UPDATE_MEMBER",
                "${if (isNew) "Registered" else "Updated"} ${saved.fullName} (${saved.membershipId})"
            )
            Result.success(docId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun collectFee(
        uid: String,
        member: Member,
        amount: Long,
        discount: Long,
        paymentMethod: String,
        forMonthLabel: String,
        dueDateMillis: Long,
        notes: String?
    ): Result<FeePayment> {
        return try {
            val payId = "pay_${UUID.randomUUID().toString().take(10)}"
            val now = System.currentTimeMillis()
            val receiptNo = "RCP-${(100000..999999).random()}"
            val payment = FeePayment(
                id = payId,
                userId = uid,
                memberId = member.id,
                memberName = member.fullName,
                membershipId = member.membershipId,
                branchId = member.branchId,
                amount = amount,
                discount = discount,
                paymentMethod = paymentMethod,
                forMonthLabel = forMonthLabel,
                dueDateMillis = dueDateMillis,
                paidAtMillis = now,
                receiptNumber = receiptNo,
                notes = notes
            )

            val nextPaidThrough = FeeEngine.nextPaidThroughAfterPayment(member)
            val newExpiry = maxOf(member.expiryDateMillis, nextPaidThrough)
            val updatedMember = member.copy(
                userId = uid,
                pendingAmount = 0L,
                lastPaymentDateMillis = now,
                paidThroughMillis = nextPaidThrough,
                expiryDateMillis = newExpiry,
                status = "ACTIVE",
                updatedAtMillis = now
            )

            dao.upsertAll(
                listOf(
                    GymRecordEntity(
                        ownerId = uid,
                        collection = Collections.PAYMENTS,
                        id = payId,
                        sortKey = now,
                        jsonPayload = GymJsonCodec.encodePayment(payment)
                    ),
                    GymRecordEntity(
                        ownerId = uid,
                        collection = Collections.MEMBERS,
                        id = member.id,
                        jsonPayload = GymJsonCodec.encodeMember(updatedMember)
                    )
                )
            )
            logAudit(uid, "COLLECT_FEE", "Collected Rs. $amount ($receiptNo) from ${member.fullName}")
            Result.success(payment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addBodyProgressLog(uid: String, log: BodyProgressLog): Result<Unit> {
        return try {
            val docId = log.id.ifBlank { "blog_${UUID.randomUUID().toString().take(10)}" }
            val saved = log.copy(id = docId, userId = uid)
            dao.upsert(
                GymRecordEntity(
                    ownerId = uid,
                    collection = Collections.BODY_LOGS,
                    id = docId,
                    sortKey = saved.recordedAtMillis,
                    jsonPayload = GymJsonCodec.encodeBodyLog(saved)
                )
            )
            dao.getOne(uid, Collections.MEMBERS, log.memberId)?.let { entity ->
                val mem = GymJsonCodec.decodeMember(entity.jsonPayload)
                val updatedMem = mem.copy(
                    weightKg = log.weightKg,
                    bodyFatPercent = log.bodyFatPercent,
                    updatedAtMillis = System.currentTimeMillis()
                )
                dao.upsert(entity.copy(jsonPayload = GymJsonCodec.encodeMember(updatedMem)))
            }
            logAudit(uid, "BODY_PROGRESS", "Logged ${log.weightKg}kg for ${log.memberName}")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkInMember(uid: String, member: Member, method: String): Result<Unit> {
        return try {
            val docId = "att_${UUID.randomUUID().toString().take(10)}"
            val now = System.currentTimeMillis()
            val record = AttendanceRecord(
                id = docId,
                userId = uid,
                memberId = member.id,
                memberName = member.fullName,
                membershipId = member.membershipId,
                branchId = member.branchId,
                method = method,
                checkInMillis = now,
                dateKey = FeeEngine.todayDateKey()
            )
            dao.upsert(
                GymRecordEntity(
                    ownerId = uid,
                    collection = Collections.ATTENDANCE,
                    id = docId,
                    sortKey = now,
                    jsonPayload = GymJsonCodec.encodeAttendance(record)
                )
            )
            logAudit(uid, "CHECK_IN", "${member.fullName} checked in via $method")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkOutAttendance(uid: String, attendanceId: String): Result<Unit> {
        return try {
            val existing = dao.getOne(uid, Collections.ATTENDANCE, attendanceId)
            if (existing != null) {
                val decoded = GymJsonCodec.decodeAttendance(existing.jsonPayload)
                val updated = decoded.copy(
                    checkOutMillis = System.currentTimeMillis(),
                    updatedAtMillis = System.currentTimeMillis()
                )
                dao.upsert(existing.copy(jsonPayload = GymJsonCodec.encodeAttendance(updated)))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveTrainer(uid: String, trainer: Trainer): Result<Unit> {
        return try {
            val docId = trainer.id.ifBlank { "tr_${UUID.randomUUID().toString().take(8)}" }
            val saved = trainer.copy(id = docId, userId = uid, updatedAtMillis = System.currentTimeMillis())
            dao.upsert(
                GymRecordEntity(
                    ownerId = uid,
                    collection = Collections.TRAINERS,
                    id = docId,
                    jsonPayload = GymJsonCodec.encodeTrainer(saved)
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveWorkoutPlan(uid: String, plan: WorkoutPlan): Result<Unit> {
        return try {
            val docId = plan.id.ifBlank { "wp_${UUID.randomUUID().toString().take(8)}" }
            val saved = plan.copy(id = docId, userId = uid, updatedAtMillis = System.currentTimeMillis())
            dao.upsert(
                GymRecordEntity(
                    ownerId = uid,
                    collection = Collections.WORKOUT_PLANS,
                    id = docId,
                    jsonPayload = GymJsonCodec.encodeWorkoutPlan(saved)
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveDietPlan(uid: String, plan: DietPlan): Result<Unit> {
        return try {
            val docId = plan.id.ifBlank { "dp_${UUID.randomUUID().toString().take(8)}" }
            val saved = plan.copy(id = docId, userId = uid, updatedAtMillis = System.currentTimeMillis())
            dao.upsert(
                GymRecordEntity(
                    ownerId = uid,
                    collection = Collections.DIET_PLANS,
                    id = docId,
                    jsonPayload = GymJsonCodec.encodeDietPlan(saved)
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveSupplement(uid: String, supplement: Supplement): Result<Unit> {
        return try {
            val docId = supplement.id.ifBlank { "sup_${UUID.randomUUID().toString().take(8)}" }
            val saved = supplement.copy(id = docId, userId = uid, updatedAtMillis = System.currentTimeMillis())
            dao.upsert(
                GymRecordEntity(
                    ownerId = uid,
                    collection = Collections.SUPPLEMENTS,
                    id = docId,
                    jsonPayload = GymJsonCodec.encodeSupplement(saved)
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordSupplementSale(uid: String, supplement: Supplement, qty: Long): Result<Unit> {
        return try {
            val actualQty = qty.coerceAtMost(supplement.stockQty).coerceAtLeast(1L)
            val updated = supplement.copy(
                userId = uid,
                stockQty = (supplement.stockQty - actualQty).coerceAtLeast(0L),
                soldQty = supplement.soldQty + actualQty,
                updatedAtMillis = System.currentTimeMillis()
            )
            dao.upsert(
                GymRecordEntity(
                    ownerId = uid,
                    collection = Collections.SUPPLEMENTS,
                    id = supplement.id,
                    jsonPayload = GymJsonCodec.encodeSupplement(updated)
                )
            )
            logAudit(uid, "SUPPLEMENT_SALE", "Sold $actualQty x ${supplement.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveExpense(uid: String, expense: Expense): Result<Unit> {
        return try {
            val docId = expense.id.ifBlank { "exp_${UUID.randomUUID().toString().take(8)}" }
            val saved = expense.copy(id = docId, userId = uid, updatedAtMillis = System.currentTimeMillis())
            dao.upsert(
                GymRecordEntity(
                    ownerId = uid,
                    collection = Collections.EXPENSES,
                    id = docId,
                    sortKey = saved.expenseDateMillis,
                    jsonPayload = GymJsonCodec.encodeExpense(saved)
                )
            )
            logAudit(uid, "ADD_EXPENSE", "Recorded ${expense.category}: ${expense.title} (Rs. ${expense.amount})")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveEquipment(uid: String, equipment: Equipment): Result<Unit> {
        return try {
            val docId = equipment.id.ifBlank { "eq_${UUID.randomUUID().toString().take(8)}" }
            val saved = equipment.copy(id = docId, userId = uid, updatedAtMillis = System.currentTimeMillis())
            dao.upsert(
                GymRecordEntity(
                    ownerId = uid,
                    collection = Collections.EQUIPMENT,
                    id = docId,
                    jsonPayload = GymJsonCodec.encodeEquipment(saved)
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logAudit(uid: String, action: String, details: String) {
        try {
            val docId = "aud_${UUID.randomUUID().toString().take(10)}"
            val now = System.currentTimeMillis()
            val entry = AuditLog(
                id = docId,
                userId = uid,
                action = action.take(60),
                actorEmail = "$uid@local.server",
                details = details.take(500),
                timestampMillis = now
            )
            dao.upsert(
                GymRecordEntity(
                    ownerId = uid,
                    collection = Collections.AUDIT_LOGS,
                    id = docId,
                    sortKey = now,
                    jsonPayload = GymJsonCodec.encodeAuditLog(entry)
                )
            )
        } catch (_: Exception) {
        }
    }
}
