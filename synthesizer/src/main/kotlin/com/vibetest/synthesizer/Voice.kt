package com.vibetest.synthesizer

import kotlin.math.PI
import kotlin.math.sin

/**
 * A single polyphonic voice: a sine oscillator with a short attack/release
 * envelope. Voices are owned by [SynthEngine] and reused across notes.
 */
class Voice {
    private var phase = 0.0
    private var phaseIncrement = 0.0
    private var envelope = 0.0
    private var stage = Stage.IDLE

    var note: Int = -1
        private set

    private enum class Stage { IDLE, ATTACK, SUSTAIN, RELEASE }

    val isActive: Boolean get() = stage != Stage.IDLE

    fun start(note: Int, frequency: Double, sampleRate: Int) {
        this.note = note
        phase = 0.0
        phaseIncrement = 2.0 * PI * frequency / sampleRate
        stage = Stage.ATTACK
    }

    fun release() {
        if (stage != Stage.IDLE) stage = Stage.RELEASE
    }

    fun stop() {
        stage = Stage.IDLE
        envelope = 0.0
        note = -1
    }

    /** Adds this voice's next sample into [buffer] at [index]. */
    fun render(buffer: DoubleArray, index: Int) {
        if (stage == Stage.IDLE) return

        envelope = when (stage) {
            Stage.ATTACK -> {
                val next = envelope + ATTACK_STEP
                if (next >= 1.0) {
                    stage = Stage.SUSTAIN
                    1.0
                } else {
                    next
                }
            }
            Stage.SUSTAIN -> 1.0
            Stage.RELEASE -> {
                val next = envelope - RELEASE_STEP
                if (next <= 0.0) {
                    stop()
                    0.0
                } else {
                    next
                }
            }
            Stage.IDLE -> 0.0
        }

        buffer[index] += sin(phase) * envelope
        phase += phaseIncrement
        if (phase > 2.0 * PI) phase -= 2.0 * PI
    }

    companion object {
        private const val ATTACK_STEP = 0.005
        private const val RELEASE_STEP = 0.001
    }
}
