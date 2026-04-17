package ru.kre4.compressor

import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class ArchiveCodec(
    private val blockSize: Int = 64 * 1024
) {
    companion object {
        private const val MAGIC = "BWH1"
    }

    data class CompressionStats(
        val inputBytes: Long,
        val outputBytes: Long,
        val averageBitsPerSymbol: Double
    )

    private val bwt = BurrowsWheelerTransform()
    private val mtf = MoveToFront()
    private val huffman = HuffmanCodec()

    fun encodeFile(inputFile: File, outputFile: File): CompressionStats {
        require(inputFile.exists() && inputFile.isFile) { "Input file does not exist: ${inputFile.path}" }
        outputFile.parentFile?.mkdirs()

        var inputBytes = inputFile.length()

        BufferedInputStream(inputFile.inputStream()).use { input ->
            DataOutputStream(FileOutputStream(outputFile)).use { out ->
                out.writeBytes(MAGIC)
                out.writeInt(blockSize)

                val buffer = ByteArray(blockSize)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break

                    val block = if (read == buffer.size) buffer.copyOf() else buffer.copyOf(read)
                    val bwtBlock = bwt.encode(block)
                    val mtfBlock = mtf.encode(bwtBlock.transformed)
                    val hufBlock = huffman.encode(mtfBlock)

                    out.writeInt(block.size)
                    out.writeInt(bwtBlock.primaryIndex)
                    out.writeInt(hufBlock.bitLength)
                    out.writeInt(hufBlock.encodedBytes.size)
                    for (f in hufBlock.frequencies) {
                        out.writeInt(f)
                    }
                    out.write(hufBlock.encodedBytes)
                }

                out.writeInt(0)
            }
        }

        val outputBytes = outputFile.length()
        val avgBits = if (inputBytes == 0L) 0.0 else (outputBytes * 8.0) / inputBytes
        return CompressionStats(inputBytes, outputBytes, avgBits)
    }

    fun decodeFile(inputFile: File, outputFile: File) {
        require(inputFile.exists() && inputFile.isFile) { "Compressed file does not exist: ${inputFile.path}" }
        outputFile.parentFile?.mkdirs()

        DataInputStream(FileInputStream(inputFile)).use { input ->
            val magicBytes = ByteArray(4)
            input.readFully(magicBytes)
            val magic = String(magicBytes, Charsets.US_ASCII)
            require(magic == MAGIC) { "Invalid archive format: expected $MAGIC, got $magic" }

            input.readInt() // stored block size

            FileOutputStream(outputFile).use { out ->
                while (true) {
                    val originalBlockSize = try {
                        input.readInt()
                    } catch (_: EOFException) {
                        break
                    }
                    if (originalBlockSize == 0) break

                    val primaryIndex = input.readInt()
                    val bitLength = input.readInt()
                    val encodedSize = input.readInt()
                    require(bitLength >= 0) { "Negative bit length: $bitLength" }
                    require(encodedSize >= 0) { "Negative encoded size: $encodedSize" }

                    val frequencies = IntArray(256) { input.readInt() }
                    val encoded = ByteArray(encodedSize)
                    input.readFully(encoded)

                    val mtfBlock = huffman.decode(frequencies, bitLength, encoded, originalBlockSize)
                    val bwtLastColumn = mtf.decode(mtfBlock)
                    val originalBlock = bwt.decode(primaryIndex, bwtLastColumn)
                    require(originalBlock.size == originalBlockSize) {
                        "Decoded block size mismatch: expected $originalBlockSize, got ${originalBlock.size}"
                    }
                    out.write(originalBlock)
                }
            }
        }
    }
}
