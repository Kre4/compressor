package ru.kre4.compressor

import ru.kre4.compressor.extensions.toUnsignedByte

/**
 * Move-to-front (book stack) transform for bytes.
 */
class MoveToFront {

    fun encode(input: ByteArray): ByteArray {
        val symbols = IntArray(256) { it }
        val out = ByteArray(input.size)

        for (i in input.indices) {
            val symbol = input[i].toUnsignedByte()
            val pos = findPosition(symbols, symbol)
            out[i] = pos.toByte()
            moveToFront(symbols, pos)
        }

        return out
    }

    fun decode(input: ByteArray): ByteArray {
        val symbols = IntArray(256) { it }
        val out = ByteArray(input.size)

        for (i in input.indices) {
            val pos = input[i].toUnsignedByte()
            val symbol = symbols[pos]
            out[i] = symbol.toByte()
            moveToFront(symbols, pos)
        }

        return out
    }

    private fun findPosition(symbols: IntArray, value: Int): Int {
        for (i in symbols.indices) {
            if (symbols[i] == value) {
                return i
            }
        }
        throw IllegalStateException("Symbol $value not found")
    }

    private fun moveToFront(symbols: IntArray, pos: Int) {
        if (pos == 0) {
            return
        }
        val value = symbols[pos]
        for (i in pos downTo 1) {
            symbols[i] = symbols[i - 1]
        }
        symbols[0] = value
    }
}
