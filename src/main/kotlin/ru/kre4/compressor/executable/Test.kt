package ru.kre4.compressor.executable

@OptIn(ExperimentalStdlibApi::class)
fun main() {
    val byte = -128;
    println(byte.toHexString()) // ffffff80
    println(0xFF) // 255
    println(byte and 0xFF) // 128

}
