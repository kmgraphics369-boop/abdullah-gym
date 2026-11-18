package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AttendanceRecord
import com.example.data.model.AuditLog
import com.example.data.model.BodyProgressLog
import com.example.data.model.Branch
import com.example.data.model.DashboardMetrics
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
import com.example.data.repository.DevSandboxData
import com.example.data.repository.GymRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

sealed interface GymUiState {
    data object Loading : GymUiState
    data class Ready(val data: GymAppData) : GymUiState
    data class Error(val message: String) : GymUiState
}

data class GymAppData(
    val ownerProfile: OwnerProfile = OwnerProfile(),
    val branches: List<Branch> = emptyList(),
    val members: List<Member> = emptyList(),
    val payments: List<FeePayment> = emptyList(),
    val bodyLogs: List<BodyProgressLog> = emptyList(),
    val attendance: List<AttendanceRecord> = emptyList(),
    val trainers: List<Trainer> = emptyList(),
    val workoutPlans: List<WorkoutPlan> = emptyList(),
    val dietPlans: List<DietPlan> = emptyList(),
    val supplements: List<Supplement> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val equipment: List<Equipment> = emptyList(),
    val auditLogs: List<AuditLog> = emptyList(),
    val dashboard: DashboardMetrics = DashboardMetrics()
)

class GymViewModel(
    private val repository: GymRepository?,
    private val currentUserId: String,
    private val userEmail: String?,
    private val userDisplayName: String?,
    val isDevSandbox: Boolean = false
) : ViewModel() {

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _pinUnlocked = MutableStateFlow(true)
    val pinUnlocked: StateFlow<Boolean> = _pinUnlocked.asStateFlow()

    private val _timeOffsetDays = MutableStateFlow(0L)
    val timeOffsetDays: StateFlow<Long> = _timeOffsetDays.asStateFlow()

    private val _sandboxData = MutableStateFlow(
        if (isDevSandbox || repository == null) DevSandboxData.createSeedData() else GymAppData()
    )

    private var pinCheckedOnce = false

    init {
        if (!isDevSandbox && repository != null) {
            viewModelScope.launch {
                repository.ensureOwnerSetup(currentUserId, userEmail, userDisplayName)
            }
        }
    }

    private val corePart1 = repository?.takeIf { !isDevSandbox }?.let { repo ->
        combine(
            repo.observeOwnerProfile(currentUserId),
            repo.observeBranches(currentUserId),
            repo.observeMembers(currentUserId),
            repo.observePayments(currentUserId),
            repo.observeBodyLogs(currentUserId)
        ) { profile, branches, members, payments, bodyLogs ->
            CorePart1(profile ?: OwnerProfile(userId = currentUserId), branches, members, payments, bodyLogs)
        }
    }

    private val corePart2 = repository?.takeIf { !isDevSandbox }?.let { repo ->
        combine(
            repo.observeAttendance(currentUserId),
            repo.observeTrainers(currentUserId),
            repo.observeWorkoutPlans(currentUserId),
            repo.observeDietPlans(currentUserId),
            repo.observeSupplements(currentUserId)
        ) { attendance, trainers, workouts, diets, supplements ->
            CorePart2(attendance, trainers, workouts, diets, supplements)
        }
    }

    private val corePart3 = repository?.takeIf { !isDevSandbox }?.let { repo ->
        combine(
            repo.observeExpenses(currentUserId),
            repo.observeEquipment(currentUserId),
            repo.observeAuditLogs(currentUserId)
        ) { expenses, equipment, auditLogs ->
            CorePart3(expenses, equipment, auditLogs)
        }
    }

    val uiState: StateFlow<GymUiState> = if (isDevSandbox || repository == null || corePart1 == null || corePart2 == null || corePart3 == null) {
        combine(_sandboxData, _timeOffsetDays) { rawData, offset ->
            if (!pinCheckedOnce && !rawData.ownerProfile.twoFactorPin.isNullOrBlank()) {
                pinCheckedOnce = true
                _pinUnlocked.value = false
            } else if (!pinCheckedOnce) {
                pinCheckedOnce = true
            }

            val simulatedToday = LocalDate.now().plusDays(offset)
            val dashboard = FeeEngine.computeDashboard(
                members = rawData.members,
                payments = rawData.payments,
                expenses = rawData.expenses,
                supplements = rawData.supplements,
                equipmentList = rawData.equipment,
                activeBranchId = rawData.ownerProfile.activeBranchId,
                today = simulatedToday
            )
            GymUiState.Ready(rawData.copy(dashboard = dashboard)) as GymUiState
        }
    } else {
        combine(corePart1, corePart2, corePart3, _timeOffsetDays) { p1, p2, p3, offset ->
            if (!pinCheckedOnce && !p1.profile.twoFactorPin.isNullOrBlank()) {
                pinCheckedOnce = true
                _pinUnlocked.value = false
            } else if (!pinCheckedOnce) {
                pinCheckedOnce = true
            }

            val simulatedToday = LocalDate.now().plusDays(offset)
            val dashboard = FeeEngine.computeDashboard(
                members = p1.members,
                payments = p1.payments,
                expenses = p3.expenses,
                supplements = p2.supplements,
                equipmentList = p3.equipment,
                activeBranchId = p1.profile.activeBranchId,
                today = simulatedToday
            )

            GymUiState.Ready(
                GymAppData(
                    ownerProfile = p1.profile,
                    branches = p1.branches,
                    members = p1.members,
                    payments = p1.payments,
                    bodyLogs = p1.bodyLogs,
                    attendance = p2.attendance,
                    trainers = p2.trainers,
                    workoutPlans = p2.workoutPlans,
                    dietPlans = p2.dietPlans,
                    supplements = p2.supplements,
                    expenses = p3.expenses,
                    equipment = p3.equipment,
                    auditLogs = p3.auditLogs,
                    dashboard = dashboard
                )
            ) as GymUiState
        }.catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing gym data", error)
            emit(GymUiState.Error(error.localizedMessage ?: "Failed to load local gym database"))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = if (isDevSandbox || repository == null) {
            GymUiState.Ready(DevSandboxData.createSeedData())
        } else {
            GymUiState.Loading
        }
    )

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun shiftTestDays(delta: Long) {
        val nextOffset = _timeOffsetDays.value + delta
        _timeOffsetDays.value = nextOffset
        val simDate = FeeEngine.formatLocalDate(LocalDate.now().plusDays(nextOffset))
        val sign = if (nextOffset > 0) "+" else ""
        _statusBanner.value = "Dev Time Travel: $simDate (${sign}${nextOffset}d)"
    }

    fun resetTestTime() {
        _timeOffsetDays.value = 0L
        _statusBanner.value = "Simulated date reset to Today"
    }

    fun resetSandboxData() {
        if (!isDevSandbox) return
        _timeOffsetDays.value = 0L
        _sandboxData.value = DevSandboxData.createSeedData()
        _statusBanner.value = "Developer Sandbox reset to sample gym dataset"
    }

    fun seedSampleDataToLocalDb() {
        val repo = repository ?: return
        if (isDevSandbox) return
        viewModelScope.launch {
            val seed = DevSandboxData.createSeedData()
            seed.branches.filter { it.id != "main" }.forEach { br ->
                repo.saveBranch(currentUserId, br.copy(id = "", userId = currentUserId))
            }
            seed.trainers.forEach { tr ->
                repo.saveTrainer(currentUserId, tr.copy(id = "", userId = currentUserId))
            }
            var firstSavedMember: Member? = null
            seed.members.forEach { m ->
                val res = repo.registerOrUpdateMember(
                    currentUserId,
                    m.copy(id = "", userId = currentUserId, trainerId = null)
                )
                val newId = res.getOrNull()
                if (firstSavedMember == null && newId != null) {
                    firstSavedMember = m.copy(id = newId, userId = currentUserId)
                }
            }
            firstSavedMember?.let { fm ->
                seed.bodyLogs.forEach { bl ->
                    repo.addBodyProgressLog(
                        currentUserId,
                        bl.copy(id = "", userId = currentUserId, memberId = fm.id, memberName = fm.fullName)
                    )
                }
                repo.checkInMember(currentUserId, fm, "QR")
            }
            seed.workoutPlans.forEach { wp ->
                repo.saveWorkoutPlan(
                    currentUserId,
                    wp.copy(id = "", userId = currentUserId, assignedMemberId = firstSavedMember?.id, assignedMemberName = firstSavedMember?.fullName)
                )
            }
            seed.dietPlans.forEach { dp ->
                repo.saveDietPlan(
                    currentUserId,
                    dp.copy(id = "", userId = currentUserId, assignedMemberId = firstSavedMember?.id, assignedMemberName = firstSavedMember?.fullName)
                )
            }
            seed.supplements.forEach { sup ->
                repo.saveSupplement(currentUserId, sup.copy(id = "", userId = currentUserId))
            }
            seed.expenses.forEach { exp ->
                repo.saveExpense(currentUserId, exp.copy(id = "", userId = currentUserId))
            }
            seed.equipment.forEach { eq ->
                repo.saveEquipment(currentUserId, eq.copy(id = "", userId = currentUserId))
            }
            _statusBanner.value = "Sample gym test dataset loaded into Local Server DB"
        }
    }

    fun unlockWithPin(pin: String): Boolean {
        val current = (uiState.value as? GymUiState.Ready)?.data?.ownerProfile?.twoFactorPin
        return if (current.isNullOrBlank() || pin == current) {
            _pinUnlocked.value = true
            true
        } else {
            false
        }
    }

    private fun appendSandboxAudit(logs: List<AuditLog>, action: String, details: String): List<AuditLog> {
        val entry = AuditLog(
            id = "aud_${UUID.randomUUID().toString().take(8)}",
            userId = currentUserId,
            action = action,
            actorEmail = userEmail ?: "dev-sandbox@gymfeemanager.local",
            details = details,
            timestampMillis = System.currentTimeMillis()
        )
        return listOf(entry) + logs
    }

    fun updateOwnerProfile(
        gymName: String,
        ownerName: String,
        ownerPhone: String,
        language: String,
        activeBranchId: String,
        twoFactorPin: String?
    ) {
        if (isDevSandbox || repository == null) {
            _sandboxData.update { cur ->
                val updated = cur.ownerProfile.copy(
                    gymName = gymName.trim().take(100).ifBlank { cur.ownerProfile.gymName },
                    ownerName = ownerName.trim().take(100).ifBlank { cur.ownerProfile.ownerName },
                    ownerPhone = ownerPhone.trim().take(30),
                    language = if (language == "ur") "ur" else "en",
                    activeBranchId = activeBranchId.ifBlank { "main" },
                    twoFactorPin = twoFactorPin?.trim()?.takeIf { it.isNotBlank() }
                )
                cur.copy(
                    ownerProfile = updated,
                    auditLogs = appendSandboxAudit(cur.auditLogs, "UPDATE_SETTINGS", "Updated sandbox settings ($gymName, lang=$language)")
                )
            }
            _statusBanner.value = "Settings saved (Sandbox)"
            return
        }
        viewModelScope.launch {
            val res = repository.updateOwnerProfile(
                currentUserId, gymName, ownerName, ownerPhone, language, activeBranchId, twoFactorPin
            )
            _statusBanner.value = if (res.isSuccess) "Settings saved" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun saveBranch(branch: Branch) {
        if (isDevSandbox || repository == null) {
            _sandboxData.update { cur ->
                val docId = branch.id.ifBlank { "br_${UUID.randomUUID().toString().take(8)}" }
                val saved = branch.copy(id = docId, userId = currentUserId)
                val updatedList = if (cur.branches.any { it.id == docId }) {
                    cur.branches.map { if (it.id == docId) saved else it }
                } else {
                    cur.branches + saved
                }
                cur.copy(
                    branches = updatedList,
                    auditLogs = appendSandboxAudit(cur.auditLogs, "SAVE_BRANCH", "Branch ${branch.name} (${branch.city})")
                )
            }
            _statusBanner.value = "Branch saved (Sandbox)"
            return
        }
        viewModelScope.launch {
            val res = repository.saveBranch(currentUserId, branch)
            _statusBanner.value = if (res.isSuccess) "Branch saved" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun registerOrUpdateMember(member: Member, onComplete: (Boolean) -> Unit = {}) {
        if (isDevSandbox || repository == null) {
            _sandboxData.update { cur ->
                val docId = member.id.ifBlank { "mem_${UUID.randomUUID().toString().take(8)}" }
                val saved = member.copy(id = docId, userId = currentUserId)
                val updatedMembers = if (cur.members.any { it.id == docId }) {
                    cur.members.map { if (it.id == docId) saved else it }
                } else {
                    (cur.members + saved).sortedBy { it.fullName }
                }
                cur.copy(
                    members = updatedMembers,
                    auditLogs = appendSandboxAudit(cur.auditLogs, "SAVE_MEMBER", "Saved ${member.fullName} (${member.membershipId})")
                )
            }
            _statusBanner.value = "Member ${member.fullName} saved (Sandbox)"
            onComplete(true)
            return
        }
        viewModelScope.launch {
            val res = repository.registerOrUpdateMember(currentUserId, member)
            _statusBanner.value = if (res.isSuccess) {
                "Member ${member.fullName} saved"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to save member"
            }
            onComplete(res.isSuccess)
        }
    }

    fun collectFee(
        member: Member,
        amount: Long,
        discount: Long,
        paymentMethod: String,
        forMonthLabel: String,
        dueDateMillis: Long,
        notes: String?,
        onReceiptReady: (FeePayment) -> Unit = {}
    ) {
        if (isDevSandbox || repository == null) {
            val simToday = LocalDate.now().plusDays(_timeOffsetDays.value)
            val paidAt = if (_timeOffsetDays.value == 0L) {
                System.currentTimeMillis()
            } else {
                FeeEngine.localDateToMillis(simToday) + 43_200_000L
            }
            val receiptNo = "RCP-${(100000..999999).random()}"
            val payment = FeePayment(
                id = "pay_${UUID.randomUUID().toString().take(8)}",
                userId = currentUserId,
                memberId = member.id,
                memberName = member.fullName,
                membershipId = member.membershipId,
                branchId = member.branchId,
                amount = amount,
                discount = discount,
                paymentMethod = paymentMethod,
                forMonthLabel = forMonthLabel,
                dueDateMillis = dueDateMillis,
                paidAtMillis = paidAt,
                receiptNumber = receiptNo,
                notes = notes
            )
            val nextPaidThrough = FeeEngine.nextPaidThroughAfterPayment(member, simToday)
            val newExpiry = maxOf(member.expiryDateMillis, nextPaidThrough)

            _sandboxData.update { cur ->
                val updatedMembers = cur.members.map { m ->
                    if (m.id == member.id) {
                        m.copy(
                            pendingAmount = 0L,
                            lastPaymentDateMillis = paidAt,
                            paidThroughMillis = nextPaidThrough,
                            expiryDateMillis = newExpiry,
                            status = "ACTIVE"
                        )
                    } else m
                }
                cur.copy(
                    members = updatedMembers,
                    payments = listOf(payment) + cur.payments,
                    auditLogs = appendSandboxAudit(cur.auditLogs, "COLLECT_FEE", "Collected Rs. $amount ($receiptNo) from ${member.fullName}")
                )
            }
            _statusBanner.value = "Collected Rs. ${payment.amount} from ${member.fullName} (Sandbox)"
            onReceiptReady(payment)
            return
        }
        viewModelScope.launch {
            val res = repository.collectFee(
                currentUserId, member, amount, discount, paymentMethod, forMonthLabel, dueDateMillis, notes
            )
            res.onSuccess { payment ->
                _statusBanner.value = "Collected Rs. ${payment.amount} from ${member.fullName}"
                onReceiptReady(payment)
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Fee collection failed"
            }
        }
    }

    fun logWhatsAppReminder(member: Member, amount: Long) {
        if (isDevSandbox || repository == null) {
            _sandboxData.update { cur ->
                cur.copy(
                    auditLogs = appendSandboxAudit(cur.auditLogs, "WHATSAPP_REMINDER", "Sent fee reminder (Rs. $amount) to ${member.fullName}")
                )
            }
            return
        }
        viewModelScope.launch {
            repository.logAudit(currentUserId, "WHATSAPP_REMINDER", "Sent fee reminder (Rs. $amount) to ${member.fullName}")
        }
    }

    fun addBodyProgressLog(log: BodyProgressLog, onDone: () -> Unit = {}) {
        if (isDevSandbox || repository == null) {
            _sandboxData.update { cur ->
                val saved = log.copy(id = "blog_${UUID.randomUUID().toString().take(8)}", userId = currentUserId)
                val updatedMembers = cur.members.map { m ->
                    if (m.id == log.memberId) m.copy(weightKg = log.weightKg, bodyFatPercent = log.bodyFatPercent) else m
                }
                cur.copy(
                    members = updatedMembers,
                    bodyLogs = (cur.bodyLogs + saved).sortedBy { it.recordedAtMillis },
                    auditLogs = appendSandboxAudit(cur.auditLogs, "BODY_PROGRESS", "Logged ${log.weightKg}kg for ${log.memberName}")
                )
            }
            _statusBanner.value = "Body progress logged (Sandbox)"
            onDone()
            return
        }
        viewModelScope.launch {
            val res = repository.addBodyProgressLog(currentUserId, log)
            _statusBanner.value = if (res.isSuccess) "Body progress logged" else res.exceptionOrNull()?.localizedMessage
            if (res.isSuccess) onDone()
        }
    }

    fun checkInMember(member: Member, method: String) {
        if (isDevSandbox || repository == null) {
            val simToday = LocalDate.now().plusDays(_timeOffsetDays.value)
            val dateMillis = if (_timeOffsetDays.value == 0L) System.currentTimeMillis() else FeeEngine.localDateToMillis(simToday) + 43_200_000L
            val record = AttendanceRecord(
                id = "att_${UUID.randomUUID().toString().take(8)}",
                userId = currentUserId,
                memberId = member.id,
                memberName = member.fullName,
                membershipId = member.membershipId,
                branchId = member.branchId,
                method = method,
                checkInMillis = dateMillis,
                dateKey = FeeEngine.dateKey(dateMillis)
            )
            _sandboxData.update { cur ->
                cur.copy(
                    attendance = listOf(record) + cur.attendance,
                    auditLogs = appendSandboxAudit(cur.auditLogs, "CHECK_IN", "${member.fullName} checked in via $method")
                )
            }
            _statusBanner.value = "${member.fullName} checked in ($method) (Sandbox)"
            return
        }
        viewModelScope.launch {
            val res = repository.checkInMember(currentUserId, member, method)
            _statusBanner.value = if (res.isSuccess) "${member.fullName} checked in ($method)" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun checkOutAttendance(attendanceId: String) {
        if (isDevSandbox || repository == null) {
            _sandboxData.update { cur ->
                cur.copy(
                    attendance = cur.attendance.map { a ->
                        if (a.id == attendanceId) a.copy(checkOutMillis = System.currentTimeMillis()) else a
                    }
                )
            }
            _statusBanner.value = "Member checked out (Sandbox)"
            return
        }
        viewModelScope.launch {
            val res = repository.checkOutAttendance(currentUserId, attendanceId)
            _statusBanner.value = if (res.isSuccess) "Member checked out" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun saveTrainer(trainer: Trainer) {
        if (isDevSandbox || repository == null) {
            _sandboxData.update { cur ->
                val docId = trainer.id.ifBlank { "tr_${UUID.randomUUID().toString().take(8)}" }
                val saved = trainer.copy(id = docId, userId = currentUserId)
                val updated = if (cur.trainers.any { it.id == docId }) {
                    cur.trainers.map { if (it.id == docId) saved else it }
                } else {
                    (cur.trainers + saved).sortedBy { it.name }
                }
                cur.copy(trainers = updated)
            }
            _statusBanner.value = "Trainer ${trainer.name} saved (Sandbox)"
            return
        }
        viewModelScope.launch {
            val res = repository.saveTrainer(currentUserId, trainer)
            _statusBanner.value = if (res.isSuccess) "Trainer ${trainer.name} saved" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun saveWorkoutPlan(plan: WorkoutPlan) {
        if (isDevSandbox || repository == null) {
            _sandboxData.update { cur ->
                val docId = plan.id.ifBlank { "wp_${UUID.randomUUID().toString().take(8)}" }
                val saved = plan.copy(id = docId, userId = currentUserId)
                val updated = if (cur.workoutPlans.any { it.id == docId }) {
                    cur.workoutPlans.map { if (it.id == docId) saved else it }
                } else {
                    cur.workoutPlans + saved
                }
                cur.copy(workoutPlans = updated)
            }
            _statusBanner.value = "Workout plan saved (Sandbox)"
            return
        }
        viewModelScope.launch {
            val res = repository.saveWorkoutPlan(currentUserId, plan)
            _statusBanner.value = if (res.isSuccess) "Workout plan saved" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun saveDietPlan(plan: DietPlan) {
        if (isDevSandbox || repository == null) {
            _sandboxData.update { cur ->
                val docId = plan.id.ifBlank { "dp_${UUID.randomUUID().toString().take(8)}" }
                val saved = plan.copy(id = docId, userId = currentUserId)
                val updated = if (cur.dietPlans.any { it.id == docId }) {
                    cur.dietPlans.map { if (it.id == docId) saved else it }
                } else {
                    cur.dietPlans + saved
                }
                cur.copy(dietPlans = updated)
            }
            _statusBanner.value = "Diet plan saved (Sandbox)"
            return
        }
        viewModelScope.launch {
            val res = repository.saveDietPlan(currentUserId, plan)
            _statusBanner.value = if (res.isSuccess) "Diet plan saved" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun saveSupplement(supplement: Supplement) {
        if (isDevSandbox || repository == null) {
            _sandboxData.update { cur ->
                val docId = supplement.id.ifBlank { "sup_${UUID.randomUUID().toString().take(8)}" }
                val saved = supplement.copy(id = docId, userId = currentUserId)
                val updated = if (cur.supplements.any { it.id == docId }) {
                    cur.supplements.map { if (it.id == docId) saved else it }
                } else {
                    (cur.supplements + saved).sortedBy { it.name }
                }
                cur.copy(supplements = updated)
            }
            _statusBanner.value = "Supplement saved (Sandbox)"
            return
        }
        viewModelScope.launch {
            val res = repository.saveSupplement(currentUserId, supplement)
            _statusBanner.value = if (res.isSuccess) "Supplement saved" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun recordSupplementSale(supplement: Supplement, qty: Long) {
        if (isDevSandbox || repository == null) {
            val actualQty = qty.coerceAtMost(supplement.stockQty).coerceAtLeast(1L)
            _sandboxData.update { cur ->
                cur.copy(
                    supplements = cur.supplements.map { s ->
                        if (s.id == supplement.id) {
                            s.copy(
                                stockQty = (s.stockQty - actualQty).coerceAtLeast(0L),
                                soldQty = s.soldQty + actualQty
                            )
                        } else s
                    },
                    auditLogs = appendSandboxAudit(cur.auditLogs, "SUPPLEMENT_SALE", "Sold $actualQty x ${supplement.name}")
                )
            }
            _statusBanner.value = "Recorded sale for ${supplement.name} (Sandbox)"
            return
        }
        viewModelScope.launch {
            val res = repository.recordSupplementSale(currentUserId, supplement, qty)
            _statusBanner.value = if (res.isSuccess) "Recorded sale for ${supplement.name}" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun saveExpense(expense: Expense) {
        if (isDevSandbox || repository == null) {
            _sandboxData.update { cur ->
                val saved = expense.copy(
                    id = expense.id.ifBlank { "exp_${UUID.randomUUID().toString().take(8)}" },
                    userId = currentUserId
                )
                cur.copy(
                    expenses = listOf(saved) + cur.expenses,
                    auditLogs = appendSandboxAudit(cur.auditLogs, "ADD_EXPENSE", "Recorded ${expense.category}: ${expense.title} (Rs. ${expense.amount})")
                )
            }
            _statusBanner.value = "Expense saved (Sandbox)"
            return
        }
        viewModelScope.launch {
            val res = repository.saveExpense(currentUserId, expense)
            _statusBanner.value = if (res.isSuccess) "Expense saved" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun saveEquipment(equipment: Equipment) {
        if (isDevSandbox || repository == null) {
            _sandboxData.update { cur ->
                val docId = equipment.id.ifBlank { "eq_${UUID.randomUUID().toString().take(8)}" }
                val saved = equipment.copy(id = docId, userId = currentUserId)
                val updated = if (cur.equipment.any { it.id == docId }) {
                    cur.equipment.map { if (it.id == docId) saved else it }
                } else {
                    (cur.equipment + saved).sortedBy { it.nextMaintenanceMillis }
                }
                cur.copy(equipment = updated)
            }
            _statusBanner.value = "Equipment saved (Sandbox)"
            return
        }
        viewModelScope.launch {
            val res = repository.saveEquipment(currentUserId, equipment)
            _statusBanner.value = if (res.isSuccess) "Equipment saved" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun buildFullBackupJson(data: GymAppData): String {
        val root = JSONObject()
        root.put("gymName", data.ownerProfile.gymName)
        root.put("ownerPhone", data.ownerProfile.ownerPhone)
        root.put("isDevSandbox", isDevSandbox)
        root.put("exportedAtMillis", System.currentTimeMillis())
        root.put("memberCount", data.members.size)
        root.put("paymentCount", data.payments.size)
        root.put("members", JSONArray(data.members.map { m ->
            JSONObject().apply {
                put("membershipId", m.membershipId)
                put("fullName", m.fullName)
                put("mobile", m.mobile)
                put("monthlyFee", m.monthlyFee)
                put("status", m.status)
            }
        }))
        root.put("payments", JSONArray(data.payments.map { p ->
            JSONObject().apply {
                put("receiptNumber", p.receiptNumber)
                put("memberName", p.memberName)
                put("amount", p.amount)
                put("forMonthLabel", p.forMonthLabel)
            }
        }))
        if (isDevSandbox || repository == null) {
            _sandboxData.update { cur ->
                cur.copy(
                    auditLogs = appendSandboxAudit(cur.auditLogs, "BACKUP_EXPORT", "Exported sandbox JSON backup (${data.members.size} members)")
                )
            }
        } else {
            viewModelScope.launch {
                repository.logAudit(currentUserId, "BACKUP_EXPORT", "Exported full JSON cloud backup (${data.members.size} members)")
            }
        }
        return root.toString(2)
    }

    private data class CorePart1(
        val profile: OwnerProfile,
        val branches: List<Branch>,
        val members: List<Member>,
        val payments: List<FeePayment>,
        val bodyLogs: List<BodyProgressLog>
    )

    private data class CorePart2(
        val attendance: List<AttendanceRecord>,
        val trainers: List<Trainer>,
        val workoutPlans: List<WorkoutPlan>,
        val dietPlans: List<DietPlan>,
        val supplements: List<Supplement>
    )

    private data class CorePart3(
        val expenses: List<Expense>,
        val equipment: List<Equipment>,
        val auditLogs: List<AuditLog>
    )

    private companion object {
        const val TAG = "GymViewModel"
    }
}
