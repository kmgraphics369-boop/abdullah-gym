package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

object CommunicationIntents {

    fun buildWhatsAppFeeMessage(
        memberName: String,
        amount: Long,
        dueDate: String,
        gymName: String,
        ownerPhone: String
    ): String {
        return """Assalam-o-Alaikum $memberName

Your Gym Membership Fee of Rs $amount is due.

Due Date: $dueDate

Please submit your fee at your earliest convenience.

Thank You

$gymName
$ownerPhone"""
    }

    fun normalizePhoneForWhatsApp(rawPhone: String): String {
        val digits = rawPhone.filter { it.isDigit() }
        return when {
            digits.startsWith("00") -> digits.drop(2)
            digits.startsWith("0") && digits.length == 11 -> "92" + digits.drop(1)
            digits.length == 10 && digits.startsWith("3") -> "92$digits"
            else -> digits
        }
    }

    fun openWhatsAppReminder(
        context: Context,
        phone: String,
        message: String
    ) {
        val normalized = normalizePhoneForWhatsApp(phone)
        val encoded = URLEncoder.encode(message, "UTF-8")
        val waUri = Uri.parse("https://wa.me/$normalized?text=$encoded")
        val viewIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(viewIntent)
        } catch (_: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(Intent.createChooser(shareIntent, "Send Reminder").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (_: Exception) {
                Toast.makeText(context, "Unable to open WhatsApp or messaging app", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun dialMember(context: Context, phone: String) {
        val cleaned = phone.trim()
        if (cleaned.isBlank()) {
            Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "No phone dialer found", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendSms(context: Context, phone: String, message: String) {
        val cleaned = phone.trim()
        if (cleaned.isBlank()) {
            Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$cleaned")).apply {
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "No SMS app found", Toast.LENGTH_SHORT).show()
        }
    }

    fun openVideoLink(context: Context, url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return
        val safeUrl = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Cannot open video link", Toast.LENGTH_SHORT).show()
        }
    }
}
