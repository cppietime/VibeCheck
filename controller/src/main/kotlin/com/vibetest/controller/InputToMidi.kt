package com.vibetest.controller

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
            0x5A to 0,  // Z  C
            0x58 to 2,  // X  D
            0x43 to 4,  // C  E
            0x56 to 5,  // V  F
            0x42 to 7,  // B  G
            0x4E to 9,  // N  A
            0x4D to 11, // M  B
            0xBC to 12, // ,  C (next octave)
            // Upper row: black keys
            0x53 to 1,  // S  C#
            0x44 to 3,  // D  D#
            0x47 to 6,  // G  F#
            0x48 to 8,  // H  G#
            0x4A to 10, // J  A#
            // Mouse buttons
            0x01 to 12, // left   -> C
            0x02 to 14, // middle -> D
            0x03 to 16, // right  -> E
        )
    }
}
