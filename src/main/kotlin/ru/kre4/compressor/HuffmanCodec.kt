package ru.kre4.compressor

import ru.kre4.compressor.extensions.toUnsignedByte
import java.util.*

class HuffmanCodec {

    data class EncodedData(
        val frequencies: IntArray,
        val bitLength: Int,
        val encodedBytes: ByteArray
    )

    private data class Node(
        val freq: Int,
        val symbol: Int? = null,
        val left: Node? = null,
        val right: Node? = null
    ) {
        val isLeaf: Boolean get() = symbol != null
    }

    fun encode(input: ByteArray): EncodedData {
        if (input.isEmpty()) {
            return EncodedData(IntArray(256), 0, ByteArray(0))
        }

        val freq = IntArray(256)
        for (b in input) {
            freq[b.toUnsignedByte()]++
        }

        val root = buildTree(freq)
        val codeTable = Array<String?>(256) { null }
        buildCodes(root, "", codeTable)

        var bitLen = 0
        for (i in 0..255) {
            val f = freq[i]
            if (f > 0) {
                bitLen += f * (codeTable[i]?.length ?: 0)
            }
        }

        val writer = BitWriter()
        for (b in input) {
            val symbol = b.toUnsignedByte()
            val code = codeTable[symbol]!!
            writer.writeBits(code)
        }

        return EncodedData(freq, bitLen, writer.toByteArray())
    }

    fun decode(frequencies: IntArray, bitLength: Int, encodedBytes: ByteArray, outputSize: Int): ByteArray {
        if (outputSize == 0) {
            return ByteArray(0)
        }

        val root = buildTree(frequencies)
        if (root.isLeaf) {
            val symbol = root.symbol!!
            return ByteArray(outputSize) { symbol.toByte() }
        }

        val out = ByteArray(outputSize)
        val reader = BitReader(encodedBytes, bitLength)
        var current: Node = root
        var written = 0

        while (written < outputSize) {
            val bit = reader.readBit()
            current = (if (bit == 0) {
                current.left
            } else {
                current.right
            })!!

            if (current.isLeaf) {
                out[written++] = (current.symbol)!!.toByte()
                current = root
            }
        }

        return out
    }

    private fun buildTree(freq: IntArray): Node {
        val pq = PriorityQueue<Node>(compareBy<Node> { it.freq }.thenBy { it.symbol ?: -1 })
        for (i in 0..255) {
            if (freq[i] > 0) {
                pq.add(Node(freq = freq[i], symbol = i))
            }
        }

        if (pq.isEmpty()) {
            return Node(0, symbol = 0)
        }
        if (pq.size == 1) {
            val only = pq.poll()
            return Node(freq = only.freq, left = only, right = null)
        }

        while (pq.size > 1) {
            val a = pq.poll()
            val b = pq.poll()
            pq.add(Node(freq = a.freq + b.freq, left = a, right = b))
        }

        return pq.poll()
    }

    private fun buildCodes(node: Node, prefix: String, table: Array<String?>) {
        if (node.isLeaf) {
            val symbol = node.symbol!!
            table[symbol] = if (prefix.isEmpty()) "0" else prefix
            return
        }

        node.left?.let { buildCodes(it, "$prefix" + "0", table) }
        node.right?.let { buildCodes(it, "$prefix" + "1", table) }
    }
}
