package ru.kre4.compressor.report.utils

import ru.kre4.compressor.extensions.toUnsignedByte
import kotlin.math.ln

/**
 * H(X) = - sum (p(x) log2 p(x))
 *
 * H(X|X) = H(X_n | X_{n-1})
 *        = - sum ( p(x,y) log2 p(y|x))
 *        = - sum ( p(x,y) log2 ( p(x,y) / p(x) ) )
 *
 * H(X|XX) = H(X_n | X_{n-2}, X_{n-1})
 *         = - sum ( p(x,y,z) log2 p(z|x,y) )
 *         = - sum ( p(x,y,z) log2 ( p(x,y,z) / p(x,y) ) )
 *
 */
object EntropyEstimator {

    data class Estimates(
        val hX: Double,
        val hXgivenX: Double,
        val hXgivenXX: Double
    )

    fun estimate(data: ByteArray): Estimates {
        if (data.isEmpty()) {
            return Estimates(0.0, 0.0, 0.0)
        }
        return Estimates(
            hX = entropyOrder0(data),
            hXgivenX = conditionalEntropyOrder1(data),
            hXgivenXX = conditionalEntropyOrder2(data)
        )
    }

    private fun entropyOrder0(data: ByteArray): Double {
        val freq = IntArray(256)
        for (b in data) freq[b.toUnsignedByte()]++
        val n = data.size.toDouble()
        var h = 0.0
        for (c in freq) {
            if (c == 0) continue
            val p = c / n
            h -= p * log2(p)
        }
        return h
    }

    private fun conditionalEntropyOrder1(data: ByteArray): Double {
        if (data.size < 2) return 0.0
        val pairCounts = HashMap<Int, Int>()
        val prevCounts = IntArray(256)

        for (i in 1 until data.size) {
            val prev = data[i - 1].toUnsignedByte()
            val curr = data[i].toUnsignedByte()
            val key = (prev shl 8) or curr
            pairCounts[key] = (pairCounts[key] ?: 0) + 1
            prevCounts[prev]++
        }

        val totalPairs = (data.size - 1).toDouble()
        var h = 0.0
        for ((key, count) in pairCounts) {
            val prev = (key ushr 8) and 0xFF
            val pxy = count / totalPairs
            val pyGivenX = count.toDouble() / prevCounts[prev]
            h -= pxy * log2(pyGivenX)
        }
        return h
    }

    private fun conditionalEntropyOrder2(data: ByteArray): Double {
        if (data.size < 3) return 0.0
        val tripleCounts = HashMap<Int, Int>()
        val contextCounts = HashMap<Int, Int>()

        for (i in 2 until data.size) {
            val a = data[i - 2].toUnsignedByte()
            val b = data[i - 1].toUnsignedByte()
            val c = data[i].toUnsignedByte()
            val context = (a shl 8) or b
            val key = (context shl 8) or c
            tripleCounts[key] = (tripleCounts[key] ?: 0) + 1
            contextCounts[context] = (contextCounts[context] ?: 0) + 1
        }

        val totalTriples = (data.size - 2).toDouble()
        var h = 0.0
        for ((key, count) in tripleCounts) {
            val context = (key ushr 8) and 0xFFFF
            val pxyz = count / totalTriples
            val pzGivenXY = count.toDouble() / (contextCounts[context] ?: 1)
            h -= pxyz * log2(pzGivenXY)
        }
        return h
    }

    private fun log2(x: Double): Double = ln(x) / ln(2.0)
}
