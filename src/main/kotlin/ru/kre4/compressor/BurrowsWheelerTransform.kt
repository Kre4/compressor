package ru.kre4.compressor

import ru.kre4.compressor.extensions.toUnsignedByte

class BurrowsWheelerTransform {

    data class EncodedBlock(
        val primaryIndex: Int,
        val transformed: ByteArray
    )

    fun encode(block: ByteArray): EncodedBlock {
        if (block.isEmpty()) {
            return EncodedBlock(0, ByteArray(0))
        }

        val n = block.size
        val rotations = Array(n) { it }

        rotations.sortWith { a, b -> compareRotations(block, a, b) }

        val result = ByteArray(n)
        var primary = -1
        for (i in 0 until n) {
            val start = rotations[i]
            if (start == 0) {
                primary = i
            }
            val lastPos = if (start == 0) n - 1 else start - 1
            result[i] = block[lastPos]
        }

        require(primary >= 0)

        return EncodedBlock(primary, result)
    }

    fun decode(primaryIndex: Int, transformed: ByteArray): ByteArray {
        if (transformed.isEmpty()) {
            return ByteArray(0)
        }
        val n = transformed.size
        require(primaryIndex in 0 until n)

        val count = IntArray(256)
        for (b in transformed) {
            count[b.toUnsignedByte()]++
        }

        val starts = IntArray(256)
        var sum = 0
        for (i in 0..255) {
            starts[i] = sum
            sum += count[i]
        }

        val occurrenceCount = IntArray(256) // типо хэш-таблицы - счётчик, сколько раз мы встретили символ i
        val next = IntArray(n)
        for (i in 0 until n) {
            val c = transformed[i].toUnsignedByte()
            next[i] = starts[c] + occurrenceCount[c]
            occurrenceCount[c]++
        }

        val original = ByteArray(n)
        var row = primaryIndex
        for (i in n - 1 downTo 0) {
            original[i] = transformed[row]
            row = next[row]
        }

        return original
    }

    private fun compareRotations(block: ByteArray, startA: Int, startB: Int): Int {
        val n = block.size
        for (offset in 0 until n) {
            val a = block[(startA + offset) % n].toUnsignedByte()
            val b = block[(startB + offset) % n].toUnsignedByte()
            if (a != b) {
                return a - b
            }
        }
        return 0
    }
}
