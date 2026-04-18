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

    enum class CompressionMode {
        HUFFMAN_ONLY,
        BWT_MTF_HUFFMAN
    }

    data class CompressionStats(
        val inputBytes: Long,
        val outputBytes: Long,
        val averageBitsPerSymbol: Double
    )

    private val bwt = BurrowsWheelerTransform()
    private val mtf = MoveToFront()
    private val huffman = HuffmanCodec()

    fun encodeFile(
        inputFile: File,
        outputFile: File,
        mode: CompressionMode = CompressionMode.BWT_MTF_HUFFMAN
    ): CompressionStats {
        require(inputFile.exists() && inputFile.isFile) { "Input file does not exist: ${inputFile.path}" }
        outputFile.parentFile?.mkdirs()

        val inputBytes = inputFile.length()

        BufferedInputStream(inputFile.inputStream()).use { input ->
            DataOutputStream(FileOutputStream(outputFile)).use { out ->
                out.writeInt(blockSize)

                val buffer = ByteArray(blockSize)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break

                    val block = if (read == buffer.size) buffer.copyOf() else buffer.copyOf(read)
                    out.writeInt(block.size)

                    val dataForHuffman: ByteArray
                    when (mode) {
                        CompressionMode.HUFFMAN_ONLY -> {
                            out.writeInt(-1) // no BWT index in this mode
                            dataForHuffman = block
                        }

                        CompressionMode.BWT_MTF_HUFFMAN -> {
                            val bwtBlock = bwt.encode(block)
                            out.writeInt(bwtBlock.primaryIndex)
                            dataForHuffman = mtf.encode(bwtBlock.transformed)
                        }
                    }

                    val hufBlock = huffman.encode(dataForHuffman)
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

    fun decodeFile(inputFile: File, outputFile: File, mode: CompressionMode = CompressionMode.BWT_MTF_HUFFMAN) {
        require(inputFile.exists() && inputFile.isFile) { "Compressed file does not exist: ${inputFile.path}" }
        outputFile.parentFile?.mkdirs()

        DataInputStream(FileInputStream(inputFile)).use { input ->
            input.readInt()

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
                    require(bitLength >= 0)
                    require(encodedSize >= 0)

                    val frequencies = IntArray(256) { input.readInt() }
                    val encoded = ByteArray(encodedSize)
                    input.readFully(encoded)

                    val huffmanDecoded = huffman.decode(frequencies, bitLength, encoded, originalBlockSize)
                    val originalBlock = when (mode) {
                        CompressionMode.HUFFMAN_ONLY -> huffmanDecoded
                        CompressionMode.BWT_MTF_HUFFMAN -> {
                            require(primaryIndex in 0 until originalBlockSize)
                            val bwtLastColumn = mtf.decode(huffmanDecoded)
                            bwt.decode(primaryIndex, bwtLastColumn)
                        }
                    }
                    require(originalBlock.size == originalBlockSize)
                    out.write(originalBlock)
                }
            }
        }
    }
}
