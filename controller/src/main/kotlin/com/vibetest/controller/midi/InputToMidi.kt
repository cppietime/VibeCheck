package com.vibetest.controller.midi

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import javax.sound.midi.Receiver
import javax.sound.midi.ShortMessage

/**
 * Translates keyboard and mouse events into MIDI note messages.
 *
 * Keyboard keys are mapped to a simple chromatic layout: the home row plays
 * white keys and the row above plays the sharps, so a QWERTY keyboard behaves
 * like a piano. Mouse buttons map to the first three notes of the octave.
 *
 * Note-on/note-off pairs are tracked so a key held down does not retrigger and
 * a key released after an octave change still turns off the note it started.
 */
class InputToMidi(
    private val receiver: Receiver,
    private val channel: Int = 0,
) {
    private val activeKeys = mutableMapOf<Int, Int>()

    var octave: Int = 4
        set(value) {
            field = value.coerceIn(0, 8)
        }

    fun noteOn(keyCode: Int) {
        if (activeKeys.containsKey(keyCode)) return
        val note = noteFor(keyCode) ?: return
        activeKeys[keyCode] = note
        send(ShortMessage.NOTE_ON, note, VELOCITY)
    }

    fun noteOff(keyCode: Int) {
        val note = activeKeys.remove(keyCode) ?: return
        send(ShortMessage.NOTE_OFF, note, 0)
    }

    fun pitchBend(bend: Int) {
        val b = bend.coerceIn(0, 0x3FFF)
        send(ShortMessage.PITCH_BEND, (b and 0x7f), b.shr(8))
    }

    fun allNotesOff() {
        activeKeys.values.forEach { send(ShortMessage.NOTE_OFF, it, 0) }
        activeKeys.clear()
    }

    fun shiftOctave(delta: Int) {
        allNotesOff()
        octave += delta
    }

    private fun noteFor(keyCode: Int): Int? {
        val semitone = KEY_MAP[keyCode] ?: return null
        return (octave * 12 + semitone).coerceIn(0, 127)
    }

    private fun send(command: Int, note: Int, velocity: Int) {
        receiver.send(ShortMessage(command, channel, note, velocity), -1)
    }

    companion object {
        private const val VELOCITY = 100

        /** Key codes follow the AWT/VK numbering used by jnativehook. */
        private val KEY_MAP: Map<Int, Int> = mapOf(
            // Lower row: white keys
            NativeKeyEvent.VC_Z to 0,       // Z  C
            NativeKeyEvent.VC_X to 2,       // X  D
            NativeKeyEvent.VC_C to 4,       // C  E
            NativeKeyEvent.VC_V to 5,       // V  F
            NativeKeyEvent.VC_B to 7,       // B  G
            NativeKeyEvent.VC_N to 9,       // N  A
            NativeKeyEvent.VC_M to 11,      // M  B
            NativeKeyEvent.VC_COMMA to 12,  // ,  C (next octave)
            // Upper row: black keys
            NativeKeyEvent.VC_S to 1,       // S  C#
            NativeKeyEvent.VC_D to 3,       // D  D#
            NativeKeyEvent.VC_G to 6,       // G  F#
            NativeKeyEvent.VC_H to 8,       // H  G#
            NativeKeyEvent.VC_J to 10,      // J  A#
            // Mouse buttons (NativeMouseEvent constants)
            1 to 12,  // left   -> C
            2 to 14,  // middle -> D
            3 to 16,  // right  -> E
        )
    }
}
