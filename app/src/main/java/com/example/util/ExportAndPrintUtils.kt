package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.FeeEngine
import com.example.data.model.FeePayment
import com.example.data.model.Member
import com.example.data.model.OwnerProfile
import java.io.File
import java.io.FileOutputStream

object ExportAndPrintUtils {

    private fun ensureExportDir(context: Context): File {
        return File(context.cacheDir, "exports").apply { mkdirs() }
    }

    private fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(Intent.createChooser(intent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {
            Toast.makeText(context, "Saved to ${file.name}", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportCsv(context: Context, fileName: String, headers: List<String>, rows: List<List<String>>) {
        val dir = ensureExportDir(context)
        val file = File(dir, "$fileName.csv")
        val sb = StringBuilder()
        sb.appendLine(headers.joinToString(",") { escapeCsv(it) })
        rows.forEach { row ->
            sb.appendLine(row.joinToString(",") { escapeCsv(it) })
        }
        file.writeText(sb.toString())
        shareFile(context, file, "text/csv", "Export CSV Report")
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\"", "\"\"")
        return "\"$clean\""
    }

    fun exportExcelXml(context: Context, fileName: String, sheetTitle: String, headers: List<String>, rows: List<List<String>>) {
        val dir = ensureExportDir(context)
        val file = File(dir, "$fileName.xls")
        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0"?>""")
        sb.appendLine("""<?mso-application progid="Excel.Sheet"?>""")
        sb.appendLine("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">""")
        sb.appendLine("""<Worksheet ss:Name="${escapeXml(sheetTitle.take(30))}"><Table>""")
        sb.appendLine("<Row>" + headers.joinToString("") { """<Cell><Data ss:Type="String">${escapeXml(it)}</Data></Cell>""" } + "</Row>")
        rows.forEach { row ->
            sb.appendLine("<Row>" + row.joinToString("") { """<Cell><Data ss:Type="String">${escapeXml(it)}</Data></Cell>""" } + "</Row>")
        }
        sb.appendLine("</Table></Worksheet></Workbook>")
        file.writeText(sb.toString())
        shareFile(context, file, "application/vnd.ms-excel", "Export Excel Report")
    }

    private fun escapeXml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    fun exportPdfReport(
        context: Context,
        fileName: String,
        title: String,
        subtitle: String,
        headers: List<String>,
        rows: List<List<String>>
    ) {
        val dir = ensureExportDir(context)
        val file = File(dir, "$fileName.pdf")
        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val headerPaint = Paint().apply {
            color = Color.parseColor("#E11D48")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 11f
        }
        val colHeaderPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val rowPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9.5f
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        val rowsPerPage = 32
        val totalPages = ((rows.size.coerceAtLeast(1) - 1) / rowsPerPage) + 1

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawText(title, 32f, 44f, headerPaint)
            canvas.drawText(subtitle, 32f, 64f, subPaint)
            canvas.drawLine(32f, 74f, pageWidth - 32f, 74f, linePaint)

            val colCount = headers.size.coerceAtLeast(1)
            val colWidth = (pageWidth - 64f) / colCount
            headers.forEachIndexed { idx, h ->
                canvas.drawText(h.take(18), 32f + idx * colWidth, 94f, colHeaderPaint)
            }
            canvas.drawLine(32f, 102f, pageWidth - 32f, 102f, linePaint)

            var y = 122f
            val sliceStart = pageIndex * rowsPerPage
            val sliceEnd = minOf(sliceStart + rowsPerPage, rows.size)
            for (r in sliceStart until sliceEnd) {
                val row = rows[r]
                row.forEachIndexed { idx, cell ->
                    canvas.drawText(cell.take(20), 32f + idx * colWidth, y, rowPaint)
                }
                y += 20f
            }
            doc.finishPage(page)
        }

        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        shareFile(context, file, "application/pdf", "Export PDF Report")
    }

    fun exportJsonBackup(context: Context, jsonContent: String) {
        val dir = ensureExportDir(context)
        val file = File(dir, "gym_backup_${System.currentTimeMillis()}.json")
        file.writeText(jsonContent)
        shareFile(context, file, "application/json", "Export Gym Cloud Backup")
    }

    fun printFeeReceipt(
        context: Context,
        ownerProfile: OwnerProfile,
        member: Member,
        payment: FeePayment?
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "Receipt_${member.membershipId}"
        printManager.print(jobName, object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                val doc = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(420, 620, 1).create()
                val page = doc.startPage(pageInfo)
                val c = page.canvas

                val brandPaint = Paint().apply {
                    color = Color.parseColor("#E11D48")
                    textSize = 20f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val boldPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 13f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val textPaint = Paint().apply {
                    color = Color.DKGRAY
                    textSize = 12f
                }
                val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1.5f }

                c.drawText(ownerProfile.gymName, 28f, 44f, brandPaint)
                c.drawText("Official Membership Fee Receipt", 28f, 66f, textPaint)
                c.drawText("Owner Phone: ${ownerProfile.ownerPhone}", 28f, 84f, textPaint)
                c.drawLine(28f, 98f, 392f, 98f, linePaint)

                var y = 126f
                val lines = listOf(
                    "Receipt No: ${payment?.receiptNumber ?: "RCP-PENDING"}",
                    "Member Name: ${member.fullName}",
                    "Membership ID: ${member.membershipId}",
                    "Phone: ${member.mobile}",
                    "Package: ${member.packageName}",
                    "Monthly Fee: Rs. ${member.monthlyFee}",
                    "Paid Amount: Rs. ${payment?.amount ?: 0}",
                    "Pending Balance: Rs. ${member.pendingAmount}",
                    "Payment Method: ${payment?.paymentMethod ?: "N/A"}",
                    "Paid Date: ${payment?.let { FeeEngine.formatDate(it.paidAtMillis) } ?: "Not Paid"}",
                    "Next Due / Expiry: ${FeeEngine.formatDate(member.paidThroughMillis)}"
                )
                lines.forEach { line ->
                    c.drawText(line, 28f, y, if (line.startsWith("Paid Amount")) boldPaint else textPaint)
                    y += 26f
                }

                c.drawLine(28f, y + 6f, 392f, y + 6f, linePaint)
                val qrBmp = QrAndBarcodeUtils.generateQrBitmap("${member.membershipId}|${member.fullName}|${member.status}", 140)
                c.drawBitmap(qrBmp, 28f, y + 20f, null)
                qrBmp.recycle()

                doc.finishPage(page)
                try {
                    FileOutputStream(destination!!.fileDescriptor).use { out ->
                        doc.writeTo(out)
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                } finally {
                    doc.close()
                }
            }
        }, null)
    }

    fun printOrShareDigitalCard(
        context: Context,
        ownerProfile: OwnerProfile,
        member: Member,
        shareAsImage: Boolean
    ) {
        val cardW = 900
        val cardH = 540
        val bmp = Bitmap.createBitmap(cardW, cardH, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.parseColor("#0D0D12"))

        val accentPaint = Paint().apply { color = Color.parseColor("#E11D48") }
        c.drawRect(0f, 0f, 18f, cardH.toFloat(), accentPaint)
        c.drawRect(0f, 0f, cardW.toFloat(), 90f, Paint().apply { color = Color.parseColor("#181822") })

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val redLabel = Paint().apply {
            color = Color.parseColor("#FF2E55")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#D4D4DD")
            textSize = 22f
        }

        c.drawText(ownerProfile.gymName.uppercase(), 42f, 56f, titlePaint)
        c.drawText("OFFICIAL MEMBER PASS", 620f, 54f, redLabel)

        val photoBmp = ImageWebpUtils.decodeBase64ToBitmap(member.photoWebpBase64)
        if (photoBmp != null) {
            val scaled = Bitmap.createScaledBitmap(photoBmp, 160, 160, true)
            c.drawBitmap(scaled, 42f, 130f, null)
            scaled.recycle()
            photoBmp.recycle()
        } else {
            c.drawRect(42f, 130f, 202f, 290f, Paint().apply { color = Color.parseColor("#272733") })
            c.drawText(member.fullName.take(2).uppercase(), 95f, 222f, titlePaint)
        }

        c.drawText(member.fullName, 228f, 165f, titlePaint)
        c.drawText("ID: ${member.membershipId}", 228f, 205f, redLabel)
        c.drawText("Package: ${member.packageName}", 228f, 245f, bodyPaint)
        c.drawText("Status: ${member.status}", 228f, 282f, bodyPaint)
        c.drawText("Expiry: ${FeeEngine.formatDate(member.expiryDateMillis)}", 228f, 320f, bodyPaint)
        c.drawText("Phone: ${member.mobile}", 228f, 358f, bodyPaint)

        val qrBmp = QrAndBarcodeUtils.generateQrBitmap("${member.membershipId}|${member.fullName}|${member.status}", 220)
        c.drawBitmap(qrBmp, 635f, 135f, null)
        qrBmp.recycle()

        c.drawText("SCAN AT RECEPTION FOR CHECK-IN", 42f, 485f, bodyPaint)

        if (shareAsImage) {
            val dir = ensureExportDir(context)
            val file = File(dir, "card_${member.membershipId}.png")
            FileOutputStream(file).use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bmp.recycle()
            shareFile(context, file, "image/png", "Download / Share Digital Membership Card")
        } else {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager == null) {
                bmp.recycle()
                return
            }
            printManager.print("Card_${member.membershipId}", object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    val info = PrintDocumentInfo.Builder("Card_${member.membershipId}.pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_PHOTO)
                        .setPageCount(1)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    val doc = PdfDocument()
                    val pageInfo = PdfDocument.PageInfo.Builder(595, 400, 1).create()
                    val page = doc.startPage(pageInfo)
                    val scaled = Bitmap.createScaledBitmap(bmp, 535, 321, true)
                    page.canvas.drawBitmap(scaled, 30f, 36f, null)
                    scaled.recycle()
                    doc.finishPage(page)
                    try {
                        FileOutputStream(destination!!.fileDescriptor).use { doc.writeTo(it) }
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    } finally {
                        doc.close()
                        bmp.recycle()
                    }
                }
            }, null)
        }
    }
}
