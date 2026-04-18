package ru.kre4.compressor.executable

import ru.kre4.compressor.ArchiveCodec
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size != 2) {
        System.err.println("Usage: decoder zipfile decfile")
        exitProcess(1)
    }

    val inputFile = File(args[0])
    val outputFile = File(args[1])

    val codec = ArchiveCodec()
    codec.decodeFile(inputFile, outputFile)
    println("Decompressed '${inputFile.path}' -> '${outputFile.path}'")
}
