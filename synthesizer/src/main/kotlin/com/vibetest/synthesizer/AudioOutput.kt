package com.vibetest.synthesizer

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.math.roundToInt

/**
 * Pulls blocks from [engine] and writes them to the default audio output.
 * Runs on its own thread; [stop] ends the loop and releases the line.
 */
class AudioOutput(
    private val engine: SynthEngine,
    private val sampleRate: Int,
    private val blockSize: Int = 256,
) {
    private val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
    private var line: SourceDataLine? = null
    private var thread: Thread? = null

    @Volatile
    private var running = false

    fun start() {
        val output = AudioSystem.getSourceDataLine(format)
        output.open(format, blockSize * 2 * 4)
        output.start()
        line = output

        running = true
        thread = Thread({ loop(output) }, "audio-output").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running = false
        thread?.join(1_000)
        line?.let {
            it.stop()
            it.close()
        }
        line = null
        thread = null
    }

    private fun loop(output: SourceDataLine) {
        val samples = DoubleArray(blockSize)
        val bytes = ByteArray(blockSize * 2)

        while (running) {
            engine.render(samples)
            for (i in samples.indices) {
                val value = (samples[i] * Short.MAX_VALUE).roundToInt().toShort()
                bytes[i * 2] = (value.toInt() and 0xFF).toByte()
                bytes[i * 2 + 1] = ((value.toInt() shr 8) and 0xFF).toByte()
            }
            output.write(bytes, 0, bytes.size)
        }
    }
}
