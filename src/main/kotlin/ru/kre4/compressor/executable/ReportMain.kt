package ru.kre4.compressor.executable

import ru.kre4.compressor.ArchiveCodec
import ru.kre4.compressor.report.utils.EntropyEstimator
import java.io.File
import java.util.Locale

fun main() {
    val datasetDir = File("src/main/resources/dataset")
    require(datasetDir.exists() && datasetDir.isDirectory) { "Dataset directory not found: ${datasetDir.path}" }

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
            require(it.exists() && it.isFile) { "Dataset file not found: ${it.path}" }
        }
    }

    data class Row(
        val name: String,
        val inBytes: Long,
        val hX: Double,
        val hXgivenX: Double,
        val hXgivenXX: Double,
        val avgBits: Double,
        val outBytes: Long
    )

    val rows = mutableListOf<Row>()
    var totalCompressed = 0L

    for (file in files) {
        val data = file.readBytes()
        val entropy = EntropyEstimator.estimate(data)
        val archive = File(outputDir, "${file.name}.bwh")
        val stats = codec.encodeFile(file, archive)
        rows += Row(
            file.name,
            stats.inputBytes,
            entropy.hX,
            entropy.hXgivenX,
            entropy.hXgivenXX,
            stats.averageBitsPerSymbol,
            stats.outputBytes
        )
        totalCompressed += stats.outputBytes
    }

    println("| File | Input bytes | H(X) | H(X\\|X) | H(X\\|XX) | Avg bits/symbol | Compressed bytes |")
    println("|---|---:|---:|---:|---:|---:|---:|")
    for (r in rows) {
        println(
            "| ${r.name} | ${r.inBytes} | " +
                "${fmt(r.hX)} | ${fmt(r.hXgivenX)} | ${fmt(r.hXgivenXX)} | " +
                "${fmt(r.avgBits)} | ${r.outBytes} |"
        )
    }
    println()
    println("Total compressed bytes (all files): $totalCompressed")
}

private fun fmt(v: Double): String = String.format(Locale.US, "%.4f", v)
