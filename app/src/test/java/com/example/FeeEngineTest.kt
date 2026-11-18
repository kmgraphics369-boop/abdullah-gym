package com.example

import com.example.data.model.FeeEngine
import com.example.data.model.FeeUrgency
import com.example.data.model.Member
import com.example.data.repository.DevSandboxData
import com.example.util.CommunicationIntents
import com.example.util.QrAndBarcodeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class FeeEngineTest {

    @Test
    fun `automatic due date anchors to joining day 15th and 25th`() {
        val ahmedJoined = FeeEngine.localDateToMillis(LocalDate.of(2026, 1, 15))
        val bilalJoined = FeeEngine.localDateToMillis(LocalDate.of(2026, 1, 25))

        val ahmed = Member(
            id = "m1",
            fullName = "Ahmed Khan",
            monthlyFee = 3000L,
            joiningDateMillis = ahmedJoined,
            paidThroughMillis = FeeEngine.localDateToMillis(LocalDate.of(2026, 11, 15)),
            dueDayOfMonth = 15L
        )
        val bilal = Member(
            id = "m2",
            fullName = "Bilal",
            monthlyFee = 3500L,
            joiningDateMillis = bilalJoined,
            paidThroughMillis = FeeEngine.localDateToMillis(LocalDate.of(2026, 11, 25)),
            dueDayOfMonth = 25L
        )

        val todayNov15 = LocalDate.of(2026, 11, 15)
        val ahmedStatus = FeeEngine.computeMemberFeeStatus(ahmed, todayNov15)
        assertEquals(LocalDate.of(2026, 11, 15), ahmedStatus.dueDate)
        assertEquals(FeeUrgency.DUE_TODAY, ahmedStatus.urgency)
        assertEquals(0L, ahmedStatus.daysRemaining)

        val bilalStatus = FeeEngine.computeMemberFeeStatus(bilal, LocalDate.of(2026, 11, 22))
        assertEquals(LocalDate.of(2026, 11, 25), bilalStatus.dueDate)
        assertEquals(FeeUrgency.DUE_SOON, bilalStatus.urgency)
        assertEquals(3L, bilalStatus.daysRemaining)

        // Short month clamp (e.g. Jan 31 -> Feb 28)
        assertEquals(LocalDate.of(2026, 2, 28), FeeEngine.anchoredDueDate(YearMonth.of(2026, 2), 31))
    }

    @Test
    fun `whatsapp message and qr matrix generate cleanly`() {
        val msg = CommunicationIntents.buildWhatsAppFeeMessage(
            memberName = "Ahmed Khan",
            amount = 3000,
            dueDate = "15 Nov 2026",
            gymName = "Iron Forge",
            ownerPhone = "+923001234567"
        )
        assertTrue(msg.contains("Assalam-o-Alaikum Ahmed Khan"))
        assertTrue(msg.contains("Rs 3000"))

        val qr = QrAndBarcodeUtils.generateQrMatrix("GYM-1001|Ahmed Khan|ACTIVE")
        assertEquals(29, qr.size)
        assertEquals(29, qr[0].size)
    }

    @Test
    fun `dev sandbox data covers all fee urgencies and responds to day shift`() {
        val fixedToday = LocalDate.of(2026, 11, 10)
        val seed = DevSandboxData.createSeedData(today = fixedToday)

        val urgencies = seed.dashboard.memberFeeStatuses.map { it.urgency }.toSet()
        assertTrue(urgencies.contains(FeeUrgency.DUE_TODAY))
        assertTrue(urgencies.contains(FeeUrgency.OVERDUE))
        assertTrue(urgencies.contains(FeeUrgency.DUE_SOON))
        assertTrue(urgencies.contains(FeeUrgency.PAID))
        assertEquals(5, seed.members.size)

        // Simulate +1 day: Ahmed Khan (due today) becomes OVERDUE; Usman Ali (due tomorrow) becomes DUE_TODAY
        val shiftedDashboard = FeeEngine.computeDashboard(
            members = seed.members,
            payments = seed.payments,
            expenses = seed.expenses,
            supplements = seed.supplements,
            equipmentList = seed.equipment,
            activeBranchId = seed.ownerProfile.activeBranchId,
            today = fixedToday.plusDays(1)
        )
        val ahmedShifted = shiftedDashboard.memberFeeStatuses.first { it.member.id == "mem_1" }
        val usmanShifted = shiftedDashboard.memberFeeStatuses.first { it.member.id == "mem_3" }
        assertEquals(FeeUrgency.OVERDUE, ahmedShifted.urgency)
        assertEquals(FeeUrgency.DUE_TODAY, usmanShifted.urgency)
    }
}
