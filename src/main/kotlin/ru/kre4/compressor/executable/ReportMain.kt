package ru.kre4.compressor.executable

import ru.kre4.compressor.ArchiveCodec
import ru.kre4.compressor.report.utils.EntropyEstimator
import java.io.File
import java.util.Locale

fun main() {
    val datasetDir = File("src/main/resources/dataset")
    require(datasetDir.exists() && datasetDir.isDirectory) { "Dataset not found: ${datasetDir.path}" }

    val outputDir = File("build/tmp/report-huffman")
    outputDir.mkdirs()

    val codec = ArchiveCodec()
    val calgary14 = listOf(
        "bib", "book1", "book2", "news",
        "paper1", "paper2", "paper3", "paper4", "paper5", "paper6",
        "progc", "progl", "progp", "trans"
    )
    val files = calgary14.map { name ->
        File(datasetDir, name).also {
            require(it.exists() && it.isFile) { "Dataset not found: ${it.path}" }
        }
    }

    data class Row(
        val name: String,
        val inBytes: Long,
        val hX: Double,
        val hXgivenX: Double,
        val hXgivenXX: Double,
        val avgBitsHuffmanOnly: Double,
        val outBytesHuffmanOnly: Long,
        val avgBitsBwtMtfHuffman: Double,
        val outBytesBwtMtfHuffman: Long
    )

    val rows = mutableListOf<Row>()
    var totalCompressedHuffmanOnly = 0L
    var totalCompressedBwtMtfHuffman = 0L
    var initialSize = 0L

    for (file in files) {
        val data = file.readBytes()
        val entropy = EntropyEstimator.estimate(data)
        val huffmanOnlyArchive = File(outputDir, "${file.name}.huf")
        val bwtMtfHuffmanArchive = File(outputDir, "${file.name}.bwh")
        val huffmanOnlyStats = codec.encodeFile(file, huffmanOnlyArchive, ArchiveCodec.CompressionMode.HUFFMAN_ONLY)
        val bwtMtfHuffmanStats = codec.encodeFile(file, bwtMtfHuffmanArchive, ArchiveCodec.CompressionMode.BWT_MTF_HUFFMAN)
        rows += Row(
            file.name,
            bwtMtfHuffmanStats.inputBytes,
            entropy.hX,
            entropy.hXgivenX,
            entropy.hXgivenXX,
            huffmanOnlyStats.averageBitsPerSymbol,
            huffmanOnlyStats.outputBytes,
            bwtMtfHuffmanStats.averageBitsPerSymbol,
            bwtMtfHuffmanStats.outputBytes
        )
        initialSize += file.length()
        totalCompressedHuffmanOnly += huffmanOnlyStats.outputBytes
        totalCompressedBwtMtfHuffman += bwtMtfHuffmanStats.outputBytes
    }

    println(
        "| File | Input bytes | H(X) | H(X\\|X) | H(X\\|XX) | " +
            "Avg bits/symbol (Huffman) | Compressed bytes (Huffman) | " +
            "Avg bits/symbol (Huffman + transform) | Compressed bytes (Huffman + transform) |"
    )
    println("|---|---:|---:|---:|---:|---:|---:|---:|---:|")
    for (r in rows) {
        println(
            "| ${r.name} | ${r.inBytes} | " +
                "${fmt(r.hX)} | ${fmt(r.hXgivenX)} | ${fmt(r.hXgivenXX)} | " +
                "${fmt(r.avgBitsHuffmanOnly)} | ${r.outBytesHuffmanOnly} | " +
                "${fmt(r.avgBitsBwtMtfHuffman)} | ${r.outBytesBwtMtfHuffman} |"
        )
    }
    println()
    println("Before compression: $initialSize")
    println("Dataset size after compression (Huffman only): $totalCompressedHuffmanOnly")
    println("Dataset size after compression (Huffman + transformation): $totalCompressedBwtMtfHuffman")
}

private fun fmt(v: Double): String = String.format(Locale.US, "%.4f", v)
