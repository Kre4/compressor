package ru.kre4.compressor

import java.io.ByteArrayOutputStream

class BitWriter {
    private val out = ByteArrayOutputStream()
    private var currentByte = 0
    private var bitCount = 0

    fun writeBits(bits: String) {
        for (ch in bits) {
            val bit = if (ch == '1') 1 else 0
            currentByte = (currentByte shl 1) or bit
            bitCount++
            if (bitCount == 8) {
                out.write(currentByte)
                currentByte = 0
                bitCount = 0
            }
        }
    }

    fun toByteArray(): ByteArray {
        if (bitCount > 0) {
            currentByte = currentByte shl (8 - bitCount)
            out.write(currentByte)
            bitCount = 0
            currentByte = 0
        }
        return out.toByteArray()
    }
}

class BitReader(private val bytes: ByteArray, private val bitLength: Int) {
    private var pos = 0

    fun readBit(): Int {
        require(pos < bitLength)
        val byteIndex = pos / 8
        val bitIndex = 7 - (pos % 8)
        val bit = (bytes[byteIndex].toInt() ushr bitIndex) and 1
        pos++
        return bit
    }
}
