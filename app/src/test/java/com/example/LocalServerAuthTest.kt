package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.GymDatabase
import com.example.data.local.LocalAuthStore
import com.example.data.local.LocalGymHttpServer
import com.example.data.model.FeeEngine
import com.example.data.model.Member
import com.example.data.repository.GymRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalServerAuthTest {

    private lateinit var context: Context
    private lateinit var db: GymDatabase
    private lateinit var authStore: LocalAuthStore
    private lateinit var server: LocalGymHttpServer
    private lateinit var repo: GymRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, GymDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        authStore = LocalAuthStore(context)
        server = LocalGymHttpServer(db.gymRecordDao(), authStore)
        repo = GymRepository(db.gymRecordDao())
    }

    @After
    fun tearDown() {
        server.stop()
        db.close()
    }

    @Test
    fun `local auth accepts imran and imran123 and rejects wrong credentials`() = runBlocking {
        assertTrue(authStore.verifyCredentials("imran", "imran123"))
        assertTrue(authStore.verifyCredentials("Imran", "imran123"))
        assertFalse(authStore.verifyCredentials("imran", "wrongpass"))
        assertFalse(authStore.verifyCredentials("other", "imran123"))

        val loginOk = server.loginViaServer("imran", "imran123")
        assertTrue(loginOk.isSuccess)
        assertEquals("imran", loginOk.getOrThrow().appId)

        val loginBad = server.loginViaServer("imran", "bad")
        assertTrue(loginBad.isFailure)
    }

    @Test
    fun `room repository persists owner members and fee collection locally`() = runBlocking {
        repo.ensureOwnerSetup("imran", "http://127.0.0.1:8765", "Imran")
        val profile = repo.observeOwnerProfile("imran").first()
        assertEquals("imran", profile?.userId)

        val today = LocalDate.now()
        val member = Member(
            membershipId = "GYM-2001",
            fullName = "Zain Abbas",
            mobile = "+923001112233",
            whatsapp = "+923001112233",
            monthlyFee = 3500L,
            joiningDateMillis = FeeEngine.localDateToMillis(today),
            paidThroughMillis = FeeEngine.localDateToMillis(today),
            dueDayOfMonth = today.dayOfMonth.toLong()
        )
        val memId = repo.registerOrUpdateMember("imran", member).getOrThrow()
        val storedMembers = repo.observeMembers("imran").first()
        assertEquals(1, storedMembers.size)
        assertEquals("Zain Abbas", storedMembers.first().fullName)

        val payRes = repo.collectFee(
            uid = "imran",
            member = storedMembers.first().copy(id = memId),
            amount = 3500L,
            discount = 0L,
            paymentMethod = "CASH",
            forMonthLabel = "Nov 2026",
            dueDateMillis = member.paidThroughMillis,
            notes = "Paid at desk"
        )
        assertTrue(payRes.isSuccess)
        val payments = repo.observePayments("imran").first()
        assertEquals(1, payments.size)
        assertEquals(3500L, payments.first().amount)
    }
}
