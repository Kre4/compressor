package ru.kre4.compressor.executable

import ru.kre4.compressor.ArchiveCodec
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size != 2) {
        System.err.println("Usage: encoder infile zipfile")
        exitProcess(1)
    }

    val inputFile = File(args[0])
    val outputFile = File(args[1])

    val codec = ArchiveCodec()
    val stats = codec.encodeFile(inputFile, outputFile)
    println("Compressed '${inputFile.path}' -> '${outputFile.path}'")
    println("Avg bits/symbol: %.4f".format(stats.averageBitsPerSymbol))
    println("Compressed size (bytes): ${stats.outputBytes}")
}
