package com.example.data.local

import com.example.data.model.AttendanceRecord
import com.example.data.model.AuditLog
import com.example.data.model.BodyProgressLog
import com.example.data.model.Branch
import com.example.data.model.DietPlan
import com.example.data.model.Equipment
import com.example.data.model.Expense
import com.example.data.model.FeePayment
import com.example.data.model.Member
import com.example.data.model.OwnerProfile
import com.example.data.model.Supplement
import com.example.data.model.Trainer
import com.example.data.model.WorkoutPlan
import org.json.JSONArray
import org.json.JSONObject

object GymJsonCodec {

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (has(key) && !isNull(key)) optLong(key) else null

    private fun JSONObject.optDoubleOrNull(key: String): Double? =
        if (has(key) && !isNull(key)) optDouble(key) else null

    private fun JSONObject.optBoolOrNull(key: String): Boolean? =
        if (has(key) && !isNull(key)) optBoolean(key) else null

    fun encodeOwnerProfile(p: OwnerProfile): String = JSONObject().apply {
        put("id", p.id)
        put("userId", p.userId)
        put("gymName", p.gymName)
        put("ownerName", p.ownerName)
        put("ownerPhone", p.ownerPhone)
        put("email", p.email)
        put("language", p.language)
        put("activeBranchId", p.activeBranchId)
        put("twoFactorPin", p.twoFactorPin)
        put("createdAtMillis", p.createdAtMillis)
        put("updatedAtMillis", p.updatedAtMillis)
    }.toString()

    fun decodeOwnerProfile(json: String): OwnerProfile = JSONObject(json).let { o ->
        OwnerProfile(
            id = o.optString("id"),
            userId = o.optString("userId"),
            gymName = o.optString("gymName", "Iron Forge Fitness Club"),
            ownerName = o.optString("ownerName", "Imran"),
            ownerPhone = o.optString("ownerPhone", "+923001234567"),
            email = o.optStringOrNull("email"),
            language = o.optString("language", "en"),
            activeBranchId = o.optString("activeBranchId", "main"),
            twoFactorPin = o.optStringOrNull("twoFactorPin"),
            createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
            updatedAtMillis = o.optLong("updatedAtMillis", System.currentTimeMillis())
        )
    }

    fun encodeBranch(b: Branch): String = JSONObject().apply {
        put("id", b.id)
        put("userId", b.userId)
        put("name", b.name)
        put("city", b.city)
        put("phone", b.phone)
        put("address", b.address)
        put("active", b.active)
        put("createdAtMillis", b.createdAtMillis)
        put("updatedAtMillis", b.updatedAtMillis)
    }.toString()

    fun decodeBranch(json: String): Branch = JSONObject(json).let { o ->
        Branch(
            id = o.optString("id"),
            userId = o.optString("userId"),
            name = o.optString("name", "Main Branch"),
            city = o.optString("city", "Lahore"),
            phone = o.optString("phone"),
            address = o.optString("address"),
            active = o.optBoolean("active", true),
            createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
            updatedAtMillis = o.optLong("updatedAtMillis", System.currentTimeMillis())
        )
    }

    fun encodeMember(m: Member): String = JSONObject().apply {
        put("id", m.id)
        put("userId", m.userId)
        put("membershipId", m.membershipId)
        put("branchId", m.branchId)
        put("fullName", m.fullName)
        put("fatherName", m.fatherName)
        put("cnic", m.cnic)
        put("mobile", m.mobile)
        put("whatsapp", m.whatsapp)
        put("emergencyContact", m.emergencyContact)
        put("address", m.address)
        put("gender", m.gender)
        put("dateOfBirth", m.dateOfBirth)
        put("bloodGroup", m.bloodGroup)
        put("packageName", m.packageName)
        put("monthlyFee", m.monthlyFee)
        put("pendingAmount", m.pendingAmount)
        put("joiningDateMillis", m.joiningDateMillis)
        put("startDateMillis", m.startDateMillis)
        put("expiryDateMillis", m.expiryDateMillis)
        put("paidThroughMillis", m.paidThroughMillis)
        put("lastPaymentDateMillis", m.lastPaymentDateMillis)
        put("dueDayOfMonth", m.dueDayOfMonth)
        put("trainerId", m.trainerId)
        put("trainerName", m.trainerName)
        put("status", m.status)
        put("heightCm", m.heightCm)
        put("weightKg", m.weightKg)
        put("bodyFatPercent", m.bodyFatPercent)
        put("fitnessGoal", m.fitnessGoal)
        put("competitionPrep", m.competitionPrep)
        put("photoWebpBase64", m.photoWebpBase64)
        put("cnicFrontBase64", m.cnicFrontBase64)
        put("cnicBackBase64", m.cnicBackBase64)
        put("medicalCertBase64", m.medicalCertBase64)
        put("createdAtMillis", m.createdAtMillis)
        put("updatedAtMillis", m.updatedAtMillis)
    }.toString()

    fun decodeMember(json: String): Member = JSONObject(json).let { o ->
        Member(
            id = o.optString("id"),
            userId = o.optString("userId"),
            membershipId = o.optString("membershipId"),
            branchId = o.optString("branchId", "main"),
            fullName = o.optString("fullName"),
            fatherName = o.optStringOrNull("fatherName"),
            cnic = o.optStringOrNull("cnic"),
            mobile = o.optString("mobile"),
            whatsapp = o.optString("whatsapp"),
            emergencyContact = o.optStringOrNull("emergencyContact"),
            address = o.optStringOrNull("address"),
            gender = o.optStringOrNull("gender"),
            dateOfBirth = o.optStringOrNull("dateOfBirth"),
            bloodGroup = o.optStringOrNull("bloodGroup"),
            packageName = o.optString("packageName", "Monthly Standard"),
            monthlyFee = o.optLong("monthlyFee", 3000L),
            pendingAmount = o.optLong("pendingAmount", 0L),
            joiningDateMillis = o.optLong("joiningDateMillis", System.currentTimeMillis()),
            startDateMillis = o.optLong("startDateMillis", System.currentTimeMillis()),
            expiryDateMillis = o.optLong("expiryDateMillis", System.currentTimeMillis()),
            paidThroughMillis = o.optLong("paidThroughMillis", System.currentTimeMillis()),
            lastPaymentDateMillis = o.optLongOrNull("lastPaymentDateMillis"),
            dueDayOfMonth = o.optLong("dueDayOfMonth", 1L),
            trainerId = o.optStringOrNull("trainerId"),
            trainerName = o.optStringOrNull("trainerName"),
            status = o.optString("status", "ACTIVE"),
            heightCm = o.optDoubleOrNull("heightCm"),
            weightKg = o.optDoubleOrNull("weightKg"),
            bodyFatPercent = o.optDoubleOrNull("bodyFatPercent"),
            fitnessGoal = o.optStringOrNull("fitnessGoal"),
            competitionPrep = o.optBoolOrNull("competitionPrep"),
            photoWebpBase64 = o.optStringOrNull("photoWebpBase64"),
            cnicFrontBase64 = o.optStringOrNull("cnicFrontBase64"),
            cnicBackBase64 = o.optStringOrNull("cnicBackBase64"),
            medicalCertBase64 = o.optStringOrNull("medicalCertBase64"),
            createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
            updatedAtMillis = o.optLong("updatedAtMillis", System.currentTimeMillis())
        )
    }

    fun encodePayment(p: FeePayment): String = JSONObject().apply {
        put("id", p.id)
        put("userId", p.userId)
        put("memberId", p.memberId)
        put("memberName", p.memberName)
        put("membershipId", p.membershipId)
        put("branchId", p.branchId)
        put("amount", p.amount)
        put("discount", p.discount)
        put("paymentMethod", p.paymentMethod)
        put("forMonthLabel", p.forMonthLabel)
        put("dueDateMillis", p.dueDateMillis)
        put("paidAtMillis", p.paidAtMillis)
        put("receiptNumber", p.receiptNumber)
        put("notes", p.notes)
        put("createdAtMillis", p.createdAtMillis)
        put("updatedAtMillis", p.updatedAtMillis)
    }.toString()

    fun decodePayment(json: String): FeePayment = JSONObject(json).let { o ->
        FeePayment(
            id = o.optString("id"),
            userId = o.optString("userId"),
            memberId = o.optString("memberId"),
            memberName = o.optString("memberName"),
            membershipId = o.optString("membershipId"),
            branchId = o.optString("branchId", "main"),
            amount = o.optLong("amount", 0L),
            discount = o.optLong("discount", 0L),
            paymentMethod = o.optString("paymentMethod", "CASH"),
            forMonthLabel = o.optString("forMonthLabel"),
            dueDateMillis = o.optLong("dueDateMillis", System.currentTimeMillis()),
            paidAtMillis = o.optLong("paidAtMillis", System.currentTimeMillis()),
            receiptNumber = o.optString("receiptNumber"),
            notes = o.optStringOrNull("notes"),
            createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
            updatedAtMillis = o.optLong("updatedAtMillis", System.currentTimeMillis())
        )
    }

    fun encodeBodyLog(b: BodyProgressLog): String = JSONObject().apply {
        put("id", b.id)
        put("userId", b.userId)
        put("memberId", b.memberId)
        put("memberName", b.memberName)
        put("recordedAtMillis", b.recordedAtMillis)
        put("weightKg", b.weightKg)
        put("bodyFatPercent", b.bodyFatPercent)
        put("muscleMassKg", b.muscleMassKg)
        put("chestCm", b.chestCm)
        put("waistCm", b.waistCm)
        put("armsCm", b.armsCm)
        put("thighsCm", b.thighsCm)
        put("photoTag", b.photoTag)
        put("photoWebpBase64", b.photoWebpBase64)
        put("notes", b.notes)
        put("createdAtMillis", b.createdAtMillis)
        put("updatedAtMillis", b.updatedAtMillis)
    }.toString()

    fun decodeBodyLog(json: String): BodyProgressLog = JSONObject(json).let { o ->
        BodyProgressLog(
            id = o.optString("id"),
            userId = o.optString("userId"),
            memberId = o.optString("memberId"),
            memberName = o.optString("memberName"),
            recordedAtMillis = o.optLong("recordedAtMillis", System.currentTimeMillis()),
            weightKg = o.optDouble("weightKg", 0.0),
            bodyFatPercent = o.optDouble("bodyFatPercent", 0.0),
            muscleMassKg = o.optDouble("muscleMassKg", 0.0),
            chestCm = o.optDoubleOrNull("chestCm"),
            waistCm = o.optDoubleOrNull("waistCm"),
            armsCm = o.optDoubleOrNull("armsCm"),
            thighsCm = o.optDoubleOrNull("thighsCm"),
            photoTag = o.optString("photoTag", "PROGRESS"),
            photoWebpBase64 = o.optStringOrNull("photoWebpBase64"),
            notes = o.optStringOrNull("notes"),
            createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
            updatedAtMillis = o.optLong("updatedAtMillis", System.currentTimeMillis())
        )
    }

    fun encodeAttendance(a: AttendanceRecord): String = JSONObject().apply {
        put("id", a.id)
        put("userId", a.userId)
        put("memberId", a.memberId)
        put("memberName", a.memberName)
        put("membershipId", a.membershipId)
        put("branchId", a.branchId)
        put("method", a.method)
        put("checkInMillis", a.checkInMillis)
        put("checkOutMillis", a.checkOutMillis)
        put("dateKey", a.dateKey)
        put("createdAtMillis", a.createdAtMillis)
        put("updatedAtMillis", a.updatedAtMillis)
    }.toString()

    fun decodeAttendance(json: String): AttendanceRecord = JSONObject(json).let { o ->
        AttendanceRecord(
            id = o.optString("id"),
            userId = o.optString("userId"),
            memberId = o.optString("memberId"),
            memberName = o.optString("memberName"),
            membershipId = o.optString("membershipId"),
            branchId = o.optString("branchId", "main"),
            method = o.optString("method", "MANUAL"),
            checkInMillis = o.optLong("checkInMillis", System.currentTimeMillis()),
            checkOutMillis = o.optLongOrNull("checkOutMillis"),
            dateKey = o.optString("dateKey"),
            createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
            updatedAtMillis = o.optLong("updatedAtMillis", System.currentTimeMillis())
        )
    }

    fun encodeTrainer(t: Trainer): String = JSONObject().apply {
        put("id", t.id)
        put("userId", t.userId)
        put("name", t.name)
        put("mobile", t.mobile)
        put("salary", t.salary)
        put("specialization", t.specialization)
        put("branchId", t.branchId)
        put("active", t.active)
        put("attendanceDaysThisMonth", t.attendanceDaysThisMonth)
        put("createdAtMillis", t.createdAtMillis)
        put("updatedAtMillis", t.updatedAtMillis)
    }.toString()

    fun decodeTrainer(json: String): Trainer = JSONObject(json).let { o ->
        Trainer(
            id = o.optString("id"),
            userId = o.optString("userId"),
            name = o.optString("name"),
            mobile = o.optString("mobile"),
            salary = o.optLong("salary", 0L),
            specialization = o.optString("specialization"),
            branchId = o.optString("branchId", "main"),
            active = o.optBoolean("active", true),
            attendanceDaysThisMonth = o.optLong("attendanceDaysThisMonth", 0L),
            createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
            updatedAtMillis = o.optLong("updatedAtMillis", System.currentTimeMillis())
        )
    }

    fun encodeWorkoutPlan(w: WorkoutPlan): String = JSONObject().apply {
        put("id", w.id)
        put("userId", w.userId)
        put("dayCategory", w.dayCategory)
        put("title", w.title)
        put("assignedMemberId", w.assignedMemberId)
        put("assignedMemberName", w.assignedMemberName)
        put("exercises", JSONArray(w.exercises.map { ex -> JSONObject(ex) }))
        put("createdAtMillis", w.createdAtMillis)
        put("updatedAtMillis", w.updatedAtMillis)
    }.toString()

    fun decodeWorkoutPlan(json: String): WorkoutPlan = JSONObject(json).let { o ->
        val exArray = o.optJSONArray("exercises") ?: JSONArray()
        val parsedList = buildList<Map<String, Any>> {
            for (i in 0 until exArray.length()) {
                val obj = exArray.optJSONObject(i) ?: continue
                val map = mutableMapOf<String, Any>()
                obj.keys().forEach { k ->
                    map[k] = obj.get(k)
                }
                add(map)
            }
        }
        WorkoutPlan(
            id = o.optString("id"),
            userId = o.optString("userId"),
            dayCategory = o.optString("dayCategory", "CHEST"),
            title = o.optString("title"),
            assignedMemberId = o.optStringOrNull("assignedMemberId"),
            assignedMemberName = o.optStringOrNull("assignedMemberName"),
            exercises = parsedList,
            createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
            updatedAtMillis = o.optLong("updatedAtMillis", System.currentTimeMillis())
        )
    }

    fun encodeDietPlan(d: DietPlan): String = JSONObject().apply {
        put("id", d.id)
        put("userId", d.userId)
        put("title", d.title)
        put("goal", d.goal)
        put("assignedMemberId", d.assignedMemberId)
        put("assignedMemberName", d.assignedMemberName)
        put("breakfast", d.breakfast)
        put("lunch", d.lunch)
        put("snacks", d.snacks)
        put("dinner", d.dinner)
        put("totalCalories", d.totalCalories)
        put("totalProteinGrams", d.totalProteinGrams)
        put("createdAtMillis", d.createdAtMillis)
        put("updatedAtMillis", d.updatedAtMillis)
    }.toString()

    fun decodeDietPlan(json: String): DietPlan = JSONObject(json).let { o ->
        DietPlan(
            id = o.optString("id"),
            userId = o.optString("userId"),
            title = o.optString("title"),
            goal = o.optString("goal"),
            assignedMemberId = o.optStringOrNull("assignedMemberId"),
            assignedMemberName = o.optStringOrNull("assignedMemberName"),
            breakfast = o.optString("breakfast"),
            lunch = o.optString("lunch"),
            snacks = o.optString("snacks"),
            dinner = o.optString("dinner"),
            totalCalories = o.optLong("totalCalories", 0L),
            totalProteinGrams = o.optLong("totalProteinGrams", 0L),
            createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
            updatedAtMillis = o.optLong("updatedAtMillis", System.currentTimeMillis())
        )
    }

    fun encodeSupplement(s: Supplement): String = JSONObject().apply {
        put("id", s.id)
        put("userId", s.userId)
        put("name", s.name)
        put("category", s.category)
        put("brand", s.brand)
        put("stockQty", s.stockQty)
        put("soldQty", s.soldQty)
        put("unitPrice", s.unitPrice)
        put("expiryDateMillis", s.expiryDateMillis)
        put("branchId", s.branchId)
        put("createdAtMillis", s.createdAtMillis)
        put("updatedAtMillis", s.updatedAtMillis)
    }.toString()

    fun decodeSupplement(json: String): Supplement = JSONObject(json).let { o ->
        Supplement(
            id = o.optString("id"),
            userId = o.optString("userId"),
            name = o.optString("name"),
            category = o.optString("category", "WHEY_PROTEIN"),
            brand = o.optString("brand"),
            stockQty = o.optLong("stockQty", 0L),
            soldQty = o.optLong("soldQty", 0L),
            unitPrice = o.optLong("unitPrice", 0L),
            expiryDateMillis = o.optLong("expiryDateMillis", System.currentTimeMillis()),
            branchId = o.optString("branchId", "main"),
            createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
            updatedAtMillis = o.optLong("updatedAtMillis", System.currentTimeMillis())
        )
    }

    fun encodeExpense(e: Expense): String = JSONObject().apply {
        put("id", e.id)
        put("userId", e.userId)
        put("category", e.category)
        put("title", e.title)
        put("amount", e.amount)
        put("expenseDateMillis", e.expenseDateMillis)
        put("branchId", e.branchId)
        put("notes", e.notes)
        put("createdAtMillis", e.createdAtMillis)
        put("updatedAtMillis", e.updatedAtMillis)
    }.toString()

    fun decodeExpense(json: String): Expense = JSONObject(json).let { o ->
        Expense(
            id = o.optString("id"),
            userId = o.optString("userId"),
            category = o.optString("category", "RENT"),
            title = o.optString("title"),
            amount = o.optLong("amount", 0L),
            expenseDateMillis = o.optLong("expenseDateMillis", System.currentTimeMillis()),
            branchId = o.optString("branchId", "main"),
            notes = o.optStringOrNull("notes"),
            createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
            updatedAtMillis = o.optLong("updatedAtMillis", System.currentTimeMillis())
        )
    }

    fun encodeEquipment(e: Equipment): String = JSONObject().apply {
        put("id", e.id)
        put("userId", e.userId)
        put("name", e.name)
        put("category", e.category)
        put("purchaseDateMillis", e.purchaseDateMillis)
        put("cost", e.cost)
        put("nextMaintenanceMillis", e.nextMaintenanceMillis)
        put("condition", e.condition)
        put("branchId", e.branchId)
        put("notes", e.notes)
        put("createdAtMillis", e.createdAtMillis)
        put("updatedAtMillis", e.updatedAtMillis)
    }.toString()

    fun decodeEquipment(json: String): Equipment = JSONObject(json).let { o ->
        Equipment(
            id = o.optString("id"),
            userId = o.optString("userId"),
            name = o.optString("name"),
            category = o.optString("category", "Strength"),
            purchaseDateMillis = o.optLong("purchaseDateMillis", System.currentTimeMillis()),
            cost = o.optLong("cost", 0L),
            nextMaintenanceMillis = o.optLong("nextMaintenanceMillis", System.currentTimeMillis()),
            condition = o.optString("condition", "GOOD"),
            branchId = o.optString("branchId", "main"),
            notes = o.optStringOrNull("notes"),
            createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
            updatedAtMillis = o.optLong("updatedAtMillis", System.currentTimeMillis())
        )
    }

    fun encodeAuditLog(a: AuditLog): String = JSONObject().apply {
        put("id", a.id)
        put("userId", a.userId)
        put("action", a.action)
        put("actorEmail", a.actorEmail)
        put("details", a.details)
        put("timestampMillis", a.timestampMillis)
        put("createdAtMillis", a.createdAtMillis)
        put("updatedAtMillis", a.updatedAtMillis)
    }.toString()

    fun decodeAuditLog(json: String): AuditLog = JSONObject(json).let { o ->
        AuditLog(
            id = o.optString("id"),
            userId = o.optString("userId"),
            action = o.optString("action"),
            actorEmail = o.optString("actorEmail"),
            details = o.optString("details"),
            timestampMillis = o.optLong("timestampMillis", System.currentTimeMillis()),
            createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
            updatedAtMillis = o.optLong("updatedAtMillis", System.currentTimeMillis())
        )
    }
}
