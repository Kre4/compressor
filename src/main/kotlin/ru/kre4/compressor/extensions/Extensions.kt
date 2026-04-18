package ru.kre4.compressor.extensions

fun Byte.toUnsignedByte(): Int = this.toInt() and 0xFF

