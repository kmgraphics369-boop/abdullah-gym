package com.example.data.model

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class FeeUrgency {
    PAID,
    DUE_SOON,
    DUE_TODAY,
    OVERDUE
}

data class MemberFeeStatus(
    val member: Member,
    val dueDate: LocalDate,
    val dueDateMillis: Long,
    val daysRemaining: Long,
    val urgency: FeeUrgency,
    val dueAmount: Long,
    val cycleLabel: String
)

data class SmartNotificationItem(
    val id: String,
    val member: Member?,
    val equipment: Equipment?,
    val title: String,
    val subtitle: String,
    val urgency: FeeUrgency,
    val amount: Long
)

data class DashboardMetrics(
    val todaysCollection: Long = 0L,
    val todaysDueFees: Long = 0L,
    val overdueFees: Long = 0L,
    val totalMembers: Int = 0,
    val activeMembers: Int = 0,
    val expiredMembers: Int = 0,
    val monthlyRevenue: Long = 0L,
    val monthlyExpenses: Long = 0L,
    val totalProfit: Long = 0L,
    val memberFeeStatuses: List<MemberFeeStatus> = emptyList(),
    val priorityDueMembers: List<MemberFeeStatus> = emptyList(),
    val notifications: List<SmartNotificationItem> = emptyList(),
    val alertCount: Int = 0
)

object FeeEngine {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMM yyyy")
    private val isoDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun millisToLocalDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun localDateToMillis(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun formatDate(millis: Long): String = millisToLocalDate(millis).format(dateFormatter)

    fun formatLocalDate(date: LocalDate): String = date.format(dateFormatter)

    fun todayDateKey(): String = LocalDate.now(zone).format(isoDateFormatter)

    fun dateKey(millis: Long): String = millisToLocalDate(millis).format(isoDateFormatter)

    /**
     * Automatic due date calculation anchored to the joining date's day-of-month.
     * Example: Joined 15 Jan -> Every month 15th is due date.
     * Joined 25 Jan -> Every month 25th is due date.
     */
    fun anchoredDueDate(yearMonth: YearMonth, dueDayOfMonth: Int): LocalDate {
        val safeDay = dueDayOfMonth.coerceIn(1, yearMonth.lengthOfMonth())
        return yearMonth.atDay(safeDay)
    }

    fun nextCycleDate(currentDueDate: LocalDate, dueDayOfMonth: Int): LocalDate {
        val nextMonth = YearMonth.from(currentDueDate).plusMonths(1)
        return anchoredDueDate(nextMonth, dueDayOfMonth)
    }

    fun computeMemberFeeStatus(
        member: Member,
        today: LocalDate = LocalDate.now(zone)
    ): MemberFeeStatus {
        val joiningDate = millisToLocalDate(member.joiningDateMillis)
        val anchorDay = if (member.dueDayOfMonth in 1..31) {
            member.dueDayOfMonth.toInt()
        } else {
            joiningDate.dayOfMonth
        }

        val paidThroughDate = millisToLocalDate(member.paidThroughMillis)
        val dueDate = anchoredDueDate(YearMonth.from(paidThroughDate), anchorDay)
        val daysRemaining = ChronoUnit.DAYS.between(today, dueDate)

        val urgency = when {
            daysRemaining < 0L -> FeeUrgency.OVERDUE
            daysRemaining == 0L -> FeeUrgency.DUE_TODAY
            daysRemaining in 1L..5L -> FeeUrgency.DUE_SOON
            else -> FeeUrgency.PAID
        }

        val monthsBehind = if (daysRemaining < 0L) {
            val approxMonths = ChronoUnit.MONTHS.between(dueDate, today).coerceAtLeast(0L) + 1L
            approxMonths
        } else {
            1L
        }

        val computedDue = if (urgency == FeeUrgency.PAID) {
            member.pendingAmount
        } else {
            (member.monthlyFee * monthsBehind) + member.pendingAmount
        }

        return MemberFeeStatus(
            member = member,
            dueDate = dueDate,
            dueDateMillis = localDateToMillis(dueDate),
            daysRemaining = daysRemaining,
            urgency = urgency,
            dueAmount = computedDue,
            cycleLabel = dueDate.format(monthYearFormatter)
        )
    }

    fun nextPaidThroughAfterPayment(member: Member, today: LocalDate = LocalDate.now(zone)): Long {
        val status = computeMemberFeeStatus(member, today)
        val anchorDay = if (member.dueDayOfMonth in 1..31) {
            member.dueDayOfMonth.toInt()
        } else {
            millisToLocalDate(member.joiningDateMillis).dayOfMonth
        }
        val baseDate = if (status.dueDate.isBefore(today)) {
            // Advance at least past the overdue cycle, up to next month from today's cycle
            var candidate = nextCycleDate(status.dueDate, anchorDay)
            while (!candidate.isAfter(today)) {
                candidate = nextCycleDate(candidate, anchorDay)
            }
            candidate
        } else {
            nextCycleDate(status.dueDate, anchorDay)
        }
        return localDateToMillis(baseDate)
    }

    fun computeDashboard(
        members: List<Member>,
        payments: List<FeePayment>,
        expenses: List<Expense>,
        supplements: List<Supplement>,
        equipmentList: List<Equipment>,
        activeBranchId: String,
        today: LocalDate = LocalDate.now(zone)
    ): DashboardMetrics {
        val branchFilter: (String) -> Boolean = { branchId ->
            activeBranchId == "ALL" || branchId == activeBranchId
        }
        val filteredMembers = members.filter { branchFilter(it.branchId) }
        val filteredPayments = payments.filter { branchFilter(it.branchId) }
        val filteredExpenses = expenses.filter { branchFilter(it.branchId) }
        val filteredSupplements = supplements.filter { branchFilter(it.branchId) }
        val filteredEquipment = equipmentList.filter { branchFilter(it.branchId) }

        val currentMonth = YearMonth.from(today)

        val todaysCollection = filteredPayments
            .filter { millisToLocalDate(it.paidAtMillis) == today }
            .sumOf { it.amount }

        val monthlyFeeRevenue = filteredPayments
            .filter { YearMonth.from(millisToLocalDate(it.paidAtMillis)) == currentMonth }
            .sumOf { it.amount }

        val supplementSalesRevenue = filteredSupplements.sumOf { it.soldQty * it.unitPrice }
        val monthlyRevenue = monthlyFeeRevenue + supplementSalesRevenue

        val monthlyExpenses = filteredExpenses
            .filter { YearMonth.from(millisToLocalDate(it.expenseDateMillis)) == currentMonth }
            .sumOf { it.amount }

        val statuses = filteredMembers.map { computeMemberFeeStatus(it, today) }

        val todaysDueFees = statuses
            .filter { it.urgency == FeeUrgency.DUE_TODAY }
            .sumOf { it.dueAmount }

        val overdueFees = statuses
            .filter { it.urgency == FeeUrgency.OVERDUE }
            .sumOf { it.dueAmount }

        val activeCount = filteredMembers.count {
            it.status == "ACTIVE" && millisToLocalDate(it.expiryDateMillis) >= today
        }
        val expiredCount = filteredMembers.count {
            it.status == "EXPIRED" || millisToLocalDate(it.expiryDateMillis) < today
        }

        val priorityDue = statuses
            .filter { it.urgency == FeeUrgency.DUE_TODAY || it.urgency == FeeUrgency.OVERDUE || it.urgency == FeeUrgency.DUE_SOON }
            .sortedWith(
                compareBy<MemberFeeStatus> {
                    when (it.urgency) {
                        FeeUrgency.DUE_TODAY -> 0
                        FeeUrgency.OVERDUE -> 1
                        FeeUrgency.DUE_SOON -> 2
                        FeeUrgency.PAID -> 3
                    }
                }.thenBy { it.daysRemaining }
            )

        val memberNotifs = statuses
            .sortedBy { it.daysRemaining }
            .map { st ->
                val subtitle = when (st.urgency) {
                    FeeUrgency.DUE_TODAY -> "Rs. ${st.dueAmount} Due Today"
                    FeeUrgency.OVERDUE -> "Rs. ${st.dueAmount} Overdue (${-st.daysRemaining}d)"
                    FeeUrgency.DUE_SOON -> if (st.daysRemaining == 1L) "Fee Due Tomorrow • Rs. ${st.dueAmount}" else "Due in ${st.daysRemaining} days • Rs. ${st.dueAmount}"
                    FeeUrgency.PAID -> "Paid • Next due ${formatLocalDate(st.dueDate)}"
                }
                SmartNotificationItem(
                    id = "mem_${st.member.id}",
                    member = st.member,
                    equipment = null,
                    title = st.member.fullName,
                    subtitle = subtitle,
                    urgency = st.urgency,
                    amount = st.dueAmount
                )
            }

        val maintAlerts = filteredEquipment.mapNotNull { eq ->
            val maintDate = millisToLocalDate(eq.nextMaintenanceMillis)
            val days = ChronoUnit.DAYS.between(today, maintDate)
            if (days <= 7L || eq.condition == "NEEDS_SERVICE" || eq.condition == "OUT_OF_ORDER") {
                val urgency = if (days < 0L || eq.condition == "OUT_OF_ORDER") FeeUrgency.OVERDUE else if (days == 0L) FeeUrgency.DUE_TODAY else FeeUrgency.DUE_SOON
                SmartNotificationItem(
                    id = "eq_${eq.id}",
                    member = null,
                    equipment = eq,
                    title = "Equipment: ${eq.name}",
                    subtitle = if (days < 0) "Maintenance overdue by ${-days}d" else "Maintenance due ${formatLocalDate(maintDate)}",
                    urgency = urgency,
                    amount = eq.cost
                )
            } else null
        }

        val allNotifications = (memberNotifs + maintAlerts).sortedBy {
            when (it.urgency) {
                FeeUrgency.DUE_TODAY -> 0
                FeeUrgency.OVERDUE -> 1
                FeeUrgency.DUE_SOON -> 2
                FeeUrgency.PAID -> 3
            }
        }

        val alertCount = allNotifications.count { it.urgency != FeeUrgency.PAID }

        return DashboardMetrics(
            todaysCollection = todaysCollection,
            todaysDueFees = todaysDueFees,
            overdueFees = overdueFees,
            totalMembers = filteredMembers.size,
            activeMembers = activeCount,
            expiredMembers = expiredCount,
            monthlyRevenue = monthlyRevenue,
            monthlyExpenses = monthlyExpenses,
            totalProfit = monthlyRevenue - monthlyExpenses,
            memberFeeStatuses = statuses,
            priorityDueMembers = priorityDue,
            notifications = allNotifications,
            alertCount = alertCount
        )
    }
}
