package com.example.data.repository

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
import com.example.data.model.WorkoutExercise
import com.example.data.model.WorkoutPlan
import com.example.ui.viewmodel.GymAppData
import java.time.LocalDate
import java.time.YearMonth

object DevSandboxData {

    fun createSeedData(today: LocalDate = LocalDate.now()): GymAppData {
        val now = System.currentTimeMillis()
        val uid = "dev_sandbox"

        val profile = OwnerProfile(
            id = uid,
            userId = uid,
            gymName = "Iron Forge Fitness Club",
            ownerName = "Tariq Mahmood (Dev)",
            ownerPhone = "+923001234567",
            email = "dev-sandbox@gymfeemanager.local",
            language = "en",
            activeBranchId = "main"
        )

        val branches = listOf(
            Branch(id = "main", userId = uid, name = "Gulberg Flagship", city = "Lahore", phone = "+923001234567", address = "MM Alam Road, Gulberg III", active = true),
            Branch(id = "dha", userId = uid, name = "DHA Phase 5", city = "Lahore", phone = "+923219876543", address = "Sector K Commercial", active = true)
        )

        val trainers = listOf(
            Trainer(id = "tr_1", userId = uid, name = "Coach Imran Butt", mobile = "+923214445566", salary = 55000L, specialization = "Bodybuilding & Contest Prep", branchId = "main", active = true, attendanceDaysThisMonth = 24L),
            Trainer(id = "tr_2", userId = uid, name = "Coach Salman Khan", mobile = "+923337778899", salary = 42000L, specialization = "Strength & Conditioning", branchId = "main", active = true, attendanceDaysThisMonth = 21L)
        )

        val todayDay = today.dayOfMonth
        val tomorrowDay = today.plusDays(1).dayOfMonth
        val overdueDate = today.minusDays(4)
        val paidNextMonth = FeeEngine.anchoredDueDate(YearMonth.from(today).plusMonths(1), 25)

        val members = listOf(
            Member(
                id = "mem_1",
                userId = uid,
                membershipId = "GYM-1001",
                branchId = "main",
                fullName = "Ahmed Khan",
                fatherName = "Rashid Khan",
                cnic = "35202-4567891-1",
                mobile = "+923001112233",
                whatsapp = "+923001112233",
                emergencyContact = "+923009998877",
                address = "Block H, Gulberg II, Lahore",
                gender = "Male",
                dateOfBirth = "1996-05-12",
                bloodGroup = "O+",
                packageName = "Pro Bodybuilding",
                monthlyFee = 3000L,
                pendingAmount = 0L,
                joiningDateMillis = FeeEngine.localDateToMillis(today.minusMonths(4)),
                startDateMillis = FeeEngine.localDateToMillis(today.minusMonths(1)),
                expiryDateMillis = FeeEngine.localDateToMillis(today.plusDays(15)),
                paidThroughMillis = FeeEngine.localDateToMillis(today),
                lastPaymentDateMillis = FeeEngine.localDateToMillis(today.minusMonths(1)),
                dueDayOfMonth = todayDay.toLong(),
                trainerId = "tr_1",
                trainerName = "Coach Imran Butt",
                status = "ACTIVE",
                heightCm = 179.0,
                weightKg = 86.4,
                bodyFatPercent = 11.2,
                fitnessGoal = "Classic Physique",
                competitionPrep = true
            ),
            Member(
                id = "mem_2",
                userId = uid,
                membershipId = "GYM-1002",
                branchId = "main",
                fullName = "Ali Raza",
                fatherName = "M. Aslam",
                cnic = "35201-8899001-3",
                mobile = "+923214567890",
                whatsapp = "+923214567890",
                address = "Model Town Link Road",
                gender = "Male",
                bloodGroup = "B+",
                packageName = "Monthly Standard",
                monthlyFee = 2500L,
                pendingAmount = 0L,
                joiningDateMillis = FeeEngine.localDateToMillis(overdueDate.minusMonths(2)),
                startDateMillis = FeeEngine.localDateToMillis(overdueDate.minusMonths(1)),
                expiryDateMillis = FeeEngine.localDateToMillis(overdueDate),
                paidThroughMillis = FeeEngine.localDateToMillis(overdueDate),
                lastPaymentDateMillis = FeeEngine.localDateToMillis(overdueDate.minusMonths(1)),
                dueDayOfMonth = overdueDate.dayOfMonth.toLong(),
                trainerId = "tr_2",
                trainerName = "Coach Salman Khan",
                status = "ACTIVE",
                heightCm = 174.0,
                weightKg = 78.0,
                bodyFatPercent = 16.5,
                fitnessGoal = "Lean Muscle",
                competitionPrep = false
            ),
            Member(
                id = "mem_3",
                userId = uid,
                membershipId = "GYM-1003",
                branchId = "main",
                fullName = "Usman Ali",
                fatherName = "Sajid Ali",
                mobile = "+923331122334",
                whatsapp = "+923331122334",
                address = "Garden Town, Lahore",
                gender = "Male",
                bloodGroup = "A+",
                packageName = "Strength + Cardio",
                monthlyFee = 3500L,
                pendingAmount = 0L,
                joiningDateMillis = FeeEngine.localDateToMillis(today.plusDays(1).minusMonths(3)),
                startDateMillis = FeeEngine.localDateToMillis(today.plusDays(1).minusMonths(1)),
                expiryDateMillis = FeeEngine.localDateToMillis(today.plusDays(1)),
                paidThroughMillis = FeeEngine.localDateToMillis(today.plusDays(1)),
                lastPaymentDateMillis = FeeEngine.localDateToMillis(today.plusDays(1).minusMonths(1)),
                dueDayOfMonth = tomorrowDay.toLong(),
                status = "ACTIVE",
                heightCm = 181.0,
                weightKg = 82.0,
                bodyFatPercent = 14.0,
                fitnessGoal = "Strength Gain"
            ),
            Member(
                id = "mem_4",
                userId = uid,
                membershipId = "GYM-1004",
                branchId = "main",
                fullName = "Bilal Siddiqui",
                fatherName = "Tariq Siddiqui",
                mobile = "+923459988776",
                whatsapp = "+923459988776",
                address = "Cavalry Ground, Lahore",
                gender = "Male",
                bloodGroup = "AB+",
                packageName = "VIP Coaching",
                monthlyFee = 6000L,
                pendingAmount = 0L,
                joiningDateMillis = FeeEngine.localDateToMillis(LocalDate.of(today.year, 1, 25)),
                startDateMillis = FeeEngine.localDateToMillis(today),
                expiryDateMillis = FeeEngine.localDateToMillis(paidNextMonth),
                paidThroughMillis = FeeEngine.localDateToMillis(paidNextMonth),
                lastPaymentDateMillis = now,
                dueDayOfMonth = 25L,
                trainerId = "tr_1",
                trainerName = "Coach Imran Butt",
                status = "ACTIVE",
                heightCm = 177.0,
                weightKg = 91.5,
                bodyFatPercent = 18.0,
                fitnessGoal = "Fat Loss"
            ),
            Member(
                id = "mem_5",
                userId = uid,
                membershipId = "GYM-1005",
                branchId = "main",
                fullName = "Hamza Sheikh",
                mobile = "+923015556677",
                whatsapp = "+923015556677",
                address = "Johar Town, Lahore",
                packageName = "Monthly Standard",
                monthlyFee = 3000L,
                pendingAmount = 500L,
                joiningDateMillis = FeeEngine.localDateToMillis(today.minusMonths(5)),
                startDateMillis = FeeEngine.localDateToMillis(today.minusMonths(1)),
                expiryDateMillis = FeeEngine.localDateToMillis(today.minusDays(2)),
                paidThroughMillis = FeeEngine.localDateToMillis(today.minusDays(2)),
                dueDayOfMonth = today.minusDays(2).dayOfMonth.toLong(),
                status = "EXPIRED",
                heightCm = 172.0,
                weightKg = 74.0,
                bodyFatPercent = 15.2,
                fitnessGoal = "Athletic Conditioning"
            )
        )

        val payments = listOf(
            FeePayment(
                id = "pay_1",
                userId = uid,
                memberId = "mem_4",
                memberName = "Bilal Siddiqui",
                membershipId = "GYM-1004",
                branchId = "main",
                amount = 6000L,
                discount = 0L,
                paymentMethod = "JAZZCASH",
                forMonthLabel = YearMonth.from(today).toString(),
                dueDateMillis = now,
                paidAtMillis = now,
                receiptNumber = "RCP-904512"
            ),
            FeePayment(
                id = "pay_2",
                userId = uid,
                memberId = "mem_1",
                memberName = "Ahmed Khan",
                membershipId = "GYM-1001",
                branchId = "main",
                amount = 3000L,
                discount = 0L,
                paymentMethod = "CASH",
                forMonthLabel = YearMonth.from(today.minusMonths(1)).toString(),
                dueDateMillis = FeeEngine.localDateToMillis(today.minusMonths(1)),
                paidAtMillis = FeeEngine.localDateToMillis(today.minusDays(3)),
                receiptNumber = "RCP-881209"
            )
        )

        val bodyLogs = listOf(
            BodyProgressLog(
                id = "bl_1", userId = uid, memberId = "mem_1", memberName = "Ahmed Khan",
                recordedAtMillis = FeeEngine.localDateToMillis(today.minusDays(90)),
                weightKg = 91.0, bodyFatPercent = 16.4, muscleMassKg = 39.2,
                chestCm = 109.0, waistCm = 86.0, armsCm = 41.0, thighsCm = 62.0, photoTag = "BEFORE"
            ),
            BodyProgressLog(
                id = "bl_2", userId = uid, memberId = "mem_1", memberName = "Ahmed Khan",
                recordedAtMillis = FeeEngine.localDateToMillis(today.minusDays(60)),
                weightKg = 89.2, bodyFatPercent = 14.5, muscleMassKg = 40.4,
                chestCm = 110.5, waistCm = 83.0, armsCm = 41.8, thighsCm = 63.0, photoTag = "PROGRESS"
            ),
            BodyProgressLog(
                id = "bl_3", userId = uid, memberId = "mem_1", memberName = "Ahmed Khan",
                recordedAtMillis = FeeEngine.localDateToMillis(today.minusDays(30)),
                weightKg = 87.6, bodyFatPercent = 12.8, muscleMassKg = 41.5,
                chestCm = 111.5, waistCm = 80.5, armsCm = 42.5, thighsCm = 63.5, photoTag = "PROGRESS"
            ),
            BodyProgressLog(
                id = "bl_4", userId = uid, memberId = "mem_1", memberName = "Ahmed Khan",
                recordedAtMillis = now,
                weightKg = 86.4, bodyFatPercent = 11.2, muscleMassKg = 42.3,
                chestCm = 112.5, waistCm = 78.5, armsCm = 43.2, thighsCm = 64.0, photoTag = "AFTER"
            )
        )

        val attendance = listOf(
            AttendanceRecord(
                id = "att_1", userId = uid, memberId = "mem_1", memberName = "Ahmed Khan",
                membershipId = "GYM-1001", branchId = "main", method = "QR",
                checkInMillis = now - 3600000L, dateKey = FeeEngine.todayDateKey()
            ),
            AttendanceRecord(
                id = "att_2", userId = uid, memberId = "mem_4", memberName = "Bilal Siddiqui",
                membershipId = "GYM-1004", branchId = "main", method = "BARCODE",
                checkInMillis = now - 7200000L, checkOutMillis = now - 1800000L, dateKey = FeeEngine.todayDateKey()
            )
        )

        val workoutPlans = listOf(
            WorkoutPlan(
                id = "wp_1", userId = uid, dayCategory = "CHEST", title = "Pro Chest Hypertrophy",
                assignedMemberId = "mem_1", assignedMemberName = "Ahmed Khan",
                exercises = listOf(
                    WorkoutExercise("Incline Barbell Press", 4, "8-10", 90, "", "Slow 3s eccentric").toMap(),
                    WorkoutExercise("Flat Dumbbell Fly", 3, "12-15", 24, "", "Full stretch at bottom").toMap()
                )
            ),
            WorkoutPlan(
                id = "wp_2", userId = uid, dayCategory = "LEG", title = "Heavy Quad & Hamstring Day",
                exercises = listOf(
                    WorkoutExercise("Barbell Back Squat", 5, "6-8", 140, "", "Break parallel cleanly").toMap(),
                    WorkoutExercise("Romanian Deadlift", 4, "10", 110, "", "Hinge hips back").toMap()
                )
            )
        )

        val dietPlans = listOf(
            DietPlan(
                id = "dp_1", userId = uid, title = "Contest Prep Cut 2650 kcal", goal = "Shredding / Fat Loss",
                assignedMemberId = "mem_1", assignedMemberName = "Ahmed Khan",
                breakfast = "8 Egg Whites, 60g Oats, Black Coffee",
                lunch = "250g Grilled Chicken Breast, 150g Basmati Rice, Greens",
                snacks = "1 Scoop Isolate Whey + 20g Almonds",
                dinner = "220g Grilled Fish, Steamed Broccoli",
                totalCalories = 2650L, totalProteinGrams = 235L
            )
        )

        val supplements = listOf(
            Supplement(id = "sup_1", userId = uid, name = "Gold Standard Whey 5lb", category = "WHEY_PROTEIN", brand = "Optimum Nutrition", stockQty = 12L, soldQty = 3L, unitPrice = 24500L, expiryDateMillis = now + 86400000L * 240L, branchId = "main"),
            Supplement(id = "sup_2", userId = uid, name = "Micronized Creatine 300g", category = "CREATINE", brand = "MuscleTech", stockQty = 2L, soldQty = 5L, unitPrice = 6500L, expiryDateMillis = now + 86400000L * 180L, branchId = "main")
        )

        val expenses = listOf(
            Expense(id = "exp_1", userId = uid, category = "RENT", title = "Gym Hall Rent", amount = 85000L, expenseDateMillis = now, branchId = "main"),
            Expense(id = "exp_2", userId = uid, category = "ELECTRICITY", title = "LESCO Commercial Bill", amount = 28500L, expenseDateMillis = now, branchId = "main")
        )

        val equipment = listOf(
            Equipment(id = "eq_1", userId = uid, name = "Commercial Treadmill #2", category = "Cardio", purchaseDateMillis = now - 86400000L * 300L, cost = 320000L, nextMaintenanceMillis = now + 86400000L * 2L, condition = "NEEDS_SERVICE", branchId = "main"),
            Equipment(id = "eq_2", userId = uid, name = "Hammer Strength Iso-Lateral Row", category = "Strength", purchaseDateMillis = now - 86400000L * 150L, cost = 240000L, nextMaintenanceMillis = now + 86400000L * 45L, condition = "EXCELLENT", branchId = "main")
        )

        val auditLogs = listOf(
            AuditLog(id = "aud_1", userId = uid, action = "DEV_SANDBOX_INIT", actorEmail = "dev-sandbox@gymfeemanager.local", details = "Initialized Developer Test Mode with sample members & due cycles", timestampMillis = now)
        )

        val dashboard = FeeEngine.computeDashboard(
            members = members,
            payments = payments,
            expenses = expenses,
            supplements = supplements,
            equipmentList = equipment,
            activeBranchId = profile.activeBranchId,
            today = today
        )

        return GymAppData(
            ownerProfile = profile,
            branches = branches,
            members = members,
            payments = payments,
            bodyLogs = bodyLogs,
            attendance = attendance,
            trainers = trainers,
            workoutPlans = workoutPlans,
            dietPlans = dietPlans,
            supplements = supplements,
            expenses = expenses,
            equipment = equipment,
            auditLogs = auditLogs,
            dashboard = dashboard
        )
    }
}
