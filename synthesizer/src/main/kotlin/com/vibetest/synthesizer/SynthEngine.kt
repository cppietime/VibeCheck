package com.vibetest.synthesizer

import kotlin.math.pow

/**
 * Polyphonic synthesis engine. MIDI events arrive on the MIDI thread while the
 * audio thread pulls samples, so all shared state is guarded by [lock].
 */
class SynthEngine(
    private val sampleRate: Int,
    private val maxVoices: Int = 16,
) {
    private val lock = Any()
    private val voices = Array(maxVoices) { Voice() }
    private val pending = ArrayDeque<Event>()

    private sealed interface Event {
        data class NoteOn(val note: Int, val velocity: Int) : Event
        data class NoteOff(val note: Int) : Event
        data object AllNotesOff : Event
    }

    fun noteOn(note: Int, velocity: Int) {
        if (velocity == 0) {
            noteOff(note)
            return
        }
        synchronized(lock) { pending.addLast(Event.NoteOn(note, velocity)) }
    }

    fun noteOff(note: Int) {
        synchronized(lock) { pending.addLast(Event.NoteOff(note)) }
    }

    fun allNotesOff() {
        synchronized(lock) { pending.addLast(Event.AllNotesOff) }
    }

    /** Fills [buffer] with the next block of audio. Called from the audio thread. */
    fun render(buffer: DoubleArray) {
        synchronized(lock) {
            drainEvents()
            buffer.fill(0.0)
            voices.forEach { voice ->
                for (i in buffer.indices) voice.render(buffer, i)
            }
        }

        // Soft clip so stacked voices stay inside [-1, 1].
        for (i in buffer.indices) {
            buffer[i] = (buffer[i] * MASTER_GAIN).coerceIn(-1.0, 1.0)
        }
    }

    private fun drainEvents() {
        while (pending.isNotEmpty()) {
            when (val event = pending.removeFirst()) {
                is Event.NoteOn -> startVoice(event.note, event.velocity)
                is Event.NoteOff -> voices.filter { it.note == event.note }
                    .forEach { it.release() }
                Event.AllNotesOff -> voices.forEach { it.release() }
            }
        }
    }

    private fun startVoice(note: Int, velocity: Int) {
        // Retrigger an existing voice for the same note rather than stacking.
        voices.firstOrNull { it.note == note && it.isActive }?.stop()

        val voice = voices.firstOrNull { !it.isActive }
            ?: voices.minByOrNull { it.note }!!.also { it.stop() }

        voice.start(note, frequencyOf(note), sampleRate)
    }

    private fun frequencyOf(note: Int): Double =
        440.0 * 2.0.pow((note - 69) / 12.0)

    companion object {
        private const val MASTER_GAIN = 0.25
    }
}
