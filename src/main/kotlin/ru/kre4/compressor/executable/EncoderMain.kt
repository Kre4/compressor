package ru.kre4.compressor.executable

import ru.kre4.compressor.ArchiveCodec
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size !in 2..3) {
        System.err.println("Usage: encoder infile zipfile [--huffman-only | --bwt-mtf-huffman]")
        exitProcess(1)
    }

    val inputFile = File(args[0])
    val outputFile = File(args[1])
    val mode = when (args.getOrNull(2)) {
        null, "--bwt-mtf-huffman" -> ArchiveCodec.CompressionMode.BWT_MTF_HUFFMAN
        "--huffman-only" -> ArchiveCodec.CompressionMode.HUFFMAN_ONLY
        else -> {
            System.err.println("Unknown mode flag: ${args[2]}")
            System.err.println("Usage: encoder infile zipfile [--huffman-only | --bwt-mtf-huffman]")
            exitProcess(1)
        }
    }

    val codec = ArchiveCodec()
    val stats = codec.encodeFile(inputFile, outputFile, mode)
    println("Compressed '${inputFile.path}' -> '${outputFile.path}'")
    println(
        "Mode: " + if (mode == ArchiveCodec.CompressionMode.HUFFMAN_ONLY) {
            "Huffman only"
        } else {
            "Huffman and transformations"
        }
    )
    println("Avg bits/symbol: %.4f".format(stats.averageBitsPerSymbol))
    println("Compressed size (bytes): ${stats.outputBytes}")
}
