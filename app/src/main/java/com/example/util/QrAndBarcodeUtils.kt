package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/**
 * Pure Kotlin QR Code (ISO/IEC 18004 Byte Mode, Version 2 & 3 ECC Level L) and Code-39 Barcode generator.
 * Produces real scannable QR matrices and Code-39 barcodes with zero external dependency overhead.
 */
object QrAndBarcodeUtils {

    private val GF_EXP = IntArray(512)
    private val GF_LOG = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            GF_EXP[i] = x
            GF_LOG[x] = i
            x = x shl 1
            if (x >= 256) x = x xor 0x11D
        }
        for (i in 255 until 512) {
            GF_EXP[i] = GF_EXP[i - 255]
        }
    }

    private fun gfMul(a: Int, b: Int): Int {
        if (a == 0 || b == 0) return 0
        return GF_EXP[GF_LOG[a] + GF_LOG[b]]
    }

    private fun rsGeneratorPoly(degree: Int): IntArray {
        var poly = intArrayOf(1)
        for (i in 0 until degree) {
            val factor = intArrayOf(1, GF_EXP[i])
            val next = IntArray(poly.size + 1)
            for (j in poly.indices) {
                for (k in factor.indices) {
                    next[j + k] = next[j + k] xor gfMul(poly[j], factor[k])
                }
            }
            poly = next
        }
        return poly
    }

    private fun rsRemainder(data: IntArray, ecCount: Int): IntArray {
        val gen = rsGeneratorPoly(ecCount)
        val result = IntArray(data.size + ecCount)
        System.arraycopy(data, 0, result, 0, data.size)
        for (i in data.indices) {
            val coef = result[i]
            if (coef != 0) {
                for (j in gen.indices) {
                    result[i + j] = result[i + j] xor gfMul(gen[j], coef)
                }
            }
        }
        return result.copyOfRange(data.size, result.size)
    }

    /**
     * Generates a Version 3 (29x29) ECC-L Byte-mode QR code (capacity up to 53 bytes).
     */
    fun generateQrMatrix(payload: String): Array<BooleanArray> {
        val rawBytes = payload.toByteArray(Charsets.UTF_8)
        // Version 3-L: 55 data codewords, 15 EC codewords (total 70 bytes, 1 block)
        val maxLen = 53
        val bytes = if (rawBytes.size > maxLen) rawBytes.copyOf(maxLen) else rawBytes

        val bits = ArrayList<Int>(55 * 8)
        fun pushBits(value: Int, count: Int) {
            for (i in (count - 1) downTo 0) {
                bits.add((value ushr i) and 1)
            }
        }

        // Mode indicator: 0100 (Byte mode)
        pushBits(0b0100, 4)
        // Character count indicator (8 bits for V3 byte mode)
        pushBits(bytes.size, 8)
        for (b in bytes) {
            pushBits(b.toInt() and 0xFF, 8)
        }
        // Terminator
        val capacityBits = 55 * 8
        val termLen = minOf(4, capacityBits - bits.size)
        pushBits(0, termLen)
        while (bits.size % 8 != 0) bits.add(0)
        var padToggle = true
        while (bits.size < capacityBits) {
            pushBits(if (padToggle) 0xEC else 0x11, 8)
            padToggle = !padToggle
        }

        val dataCodewords = IntArray(55) { i ->
            var v = 0
            for (b in 0 until 8) {
                v = (v shl 1) or bits[i * 8 + b]
            }
            v
        }
        val ecCodewords = rsRemainder(dataCodewords, 15)
        val allCodewords = dataCodewords + ecCodewords

        val size = 29
        val modules = Array(size) { BooleanArray(size) }
        val isFunction = Array(size) { BooleanArray(size) }

        fun setFunc(r: Int, c: Int, black: Boolean) {
            if (r in 0 until size && c in 0 until size) {
                modules[r][c] = black
                isFunction[r][c] = true
            }
        }

        fun drawFinder(top: Int, left: Int) {
            for (dr in -1..7) {
                for (dc in -1..7) {
                    val r = top + dr
                    val c = left + dc
                    if (r in 0 until size && c in 0 until size) {
                        val black = dr in 0..6 && dc in 0..6 &&
                            (dr == 0 || dr == 6 || dc == 0 || dc == 6 || (dr in 2..4 && dc in 2..4))
                        setFunc(r, c, black)
                    }
                }
            }
        }

        drawFinder(0, 0)
        drawFinder(0, size - 7)
        drawFinder(size - 7, 0)

        // Alignment pattern for V3 at (22, 22)
        for (dr in -2..2) {
            for (dc in -2..2) {
                val dist = maxOf(kotlin.math.abs(dr), kotlin.math.abs(dc))
                setFunc(22 + dr, 22 + dc, dist != 1)
            }
        }

        // Timing patterns
        for (i in 8 until size - 8) {
            setFunc(6, i, i % 2 == 0)
            setFunc(i, 6, i % 2 == 0)
        }

        // Dark module
        setFunc(4 * 3 + 9, 8, true)

        // Reserve format info
        for (i in 0..8) {
            if (i != 6) {
                setFunc(8, i, false)
                setFunc(i, 8, false)
            }
        }
        for (i in 0..7) {
            setFunc(8, size - 1 - i, false)
            setFunc(size - 1 - i, 8, false)
        }

        // Place data bits (Mask 0: (r + c) % 2 == 0)
        val allBits = IntArray(allCodewords.size * 8 + 7)
        var idx = 0
        for (cw in allCodewords) {
            for (b in 7 downTo 0) {
                allBits[idx++] = (cw ushr b) and 1
            }
        }

        var bitIdx = 0
        var upward = true
        var col = size - 1
        while (col > 0) {
            if (col == 6) col--
            for (step in 0 until size) {
                val r = if (upward) size - 1 - step else step
                for (dc in 0..1) {
                    val c = col - dc
                    if (!isFunction[r][c]) {
                        val bit = if (bitIdx < allBits.size) allBits[bitIdx++] == 1 else false
                        val mask = (r + c) % 2 == 0
                        modules[r][c] = bit xor mask
                    }
                }
            }
            upward = !upward
            col -= 2
        }

        // Format info for ECC-L (01) + Mask 0 (000) -> 15-bit BCH: 0b111011111000100
        val formatBits = 0b111011111000100
        for (i in 0..5) setFunc(8, i, ((formatBits ushr (14 - i)) and 1) == 1)
        setFunc(8, 7, ((formatBits ushr 8) and 1) == 1)
        setFunc(8, 8, ((formatBits ushr 7) and 1) == 1)
        setFunc(7, 8, ((formatBits ushr 6) and 1) == 1)
        for (i in 0..5) setFunc(5 - i, 8, ((formatBits ushr (5 - i)) and 1) == 1)

        for (i in 0..7) setFunc(8, size - 1 - i, ((formatBits ushr i) and 1) == 1)
        for (i in 0..6) setFunc(size - 1 - i, 8, ((formatBits ushr (14 - i)) and 1) == 1)

        return modules
    }

    fun generateQrBitmap(payload: String, pixelSize: Int = 360): Bitmap {
        val matrix = generateQrMatrix(payload)
        val n = matrix.size
        val quiet = 2
        val total = n + quiet * 2
        val cell = (pixelSize / total).coerceAtLeast(4)
        val outSize = total * cell
        val bmp = Bitmap.createBitmap(outSize, outSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)
        val paint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
        for (r in 0 until n) {
            for (c in 0 until n) {
                if (matrix[r][c]) {
                    val left = ((c + quiet) * cell).toFloat()
                    val top = ((r + quiet) * cell).toFloat()
                    canvas.drawRect(left, top, left + cell, top + cell, paint)
                }
            }
        }
        return bmp
    }

    private val CODE39_MAP = mapOf(
        '0' to "101001101101", '1' to "110100101011", '2' to "101100101011",
        '3' to "110110010101", '4' to "101001101011", '5' to "110100110101",
        '6' to "101100110101", '7' to "101001011011", '8' to "110100101101",
        '9' to "101100101101", 'A' to "110101001011", 'B' to "101101001011",
        'C' to "110110100101", 'D' to "101011001011", 'E' to "110101100101",
        'F' to "101101100101", 'G' to "101010011011", 'H' to "110101001101",
        'I' to "101101001101", 'J' to "101011001101", 'K' to "110101010011",
        'L' to "101101010011", 'M' to "110110101001", 'N' to "101011010011",
        'O' to "110101101001", 'P' to "101101101001", 'Q' to "101010110011",
        'R' to "110101011001", 'S' to "101101011001", 'T' to "101011011001",
        'U' to "110010101011", 'V' to "100110101011", 'W' to "110011010101",
        'X' to "100101101011", 'Y' to "110010110101", 'Z' to "100110110101",
        '-' to "100101011011", '.' to "110010101101", ' ' to "100110101101",
        '*' to "100101101101"
    )

    fun generateCode39Pattern(code: String): List<Boolean> {
        val clean = "*" + code.uppercase().filter { it in CODE39_MAP && it != '*' } + "*"
        val result = ArrayList<Boolean>(clean.length * 13)
        clean.forEachIndexed { idx, ch ->
            val pattern = CODE39_MAP[ch] ?: CODE39_MAP['-']!!
            pattern.forEach { result.add(it == '1') }
            if (idx < clean.lastIndex) result.add(false)
        }
        return result
    }
}
