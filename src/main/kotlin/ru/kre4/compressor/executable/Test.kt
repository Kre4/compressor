package ru.kre4.compressor.executable

import ru.kre4.compressor.BurrowsWheelerTransform

@OptIn(ExperimentalStdlibApi::class)
fun main() {
    val byte = -128;
    println(byte.toHexString()) // ffffff80
    println(0xFF) // 255
    println(byte and 0xFF) // 128
    println("---")

    val strToEncode = "bcvd321@creep"
    val bwt = BurrowsWheelerTransform()

    val encodeBwt = bwt.encode(strToEncode.toByteArray());
    println(encodeBwt.transformed.decodeToString())
    val decodedBwt = bwt.decode(encodeBwt.primaryIndex, encodeBwt.transformed)
    println(decodedBwt.decodeToString())
}
