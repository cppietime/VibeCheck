package com.vibetest.controller.midi

import com.vibetest.controller.config.ChordMidiAction
import com.vibetest.controller.config.ChordModeMidiAction
import com.vibetest.controller.config.MidiAction
import com.vibetest.controller.config.MidiConfig
import com.vibetest.controller.config.NoteMidiAction
import com.vibetest.controller.config.OctaveMidiAction
import com.vibetest.controller.config.SustainMidiAction
import com.vibetest.controller.config.defaultMidiConfig
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
    val config: MidiConfig = defaultMidiConfig(),
) {
    private val activeKeys = mutableMapOf<Int, List<Int>>()
    private val sustainKeys = mutableMapOf<Int, List<Int>>()
    var sustain = false
        private set
    var chordMode = false
        private set
    var lastChord: ChordMidiAction? = null
        private set

    var octave: Int = 4
        set(value) {
            field = value.coerceIn(0, 8)
        }

    val currentKeyMap get() = if (chordMode) config.chordKeyNoteMap else config.keyNoteMap

    fun keyPress(keyCode: Int, map: Map<Int, MidiAction> = currentKeyMap) {
        val action = map[keyCode] ?: return
        when (action) {
            is NoteMidiAction -> noteOn(keyCode, action.note)
            is OctaveMidiAction -> shiftOctave(action.shift)
            is SustainMidiAction -> beginSustain()
            is ChordMidiAction -> selectChord(action)
            is ChordModeMidiAction -> {
                if (chordMode) endChord() else beginChord()
            }
        }
    }

    fun keyRelease(keyCode: Int, map: Map<Int, MidiAction> = currentKeyMap) {
        val action = map[keyCode] ?: return
        when (action) {
            is NoteMidiAction -> noteOff(keyCode)
            is SustainMidiAction -> endSustain()
            is ChordMidiAction -> selectChord(null)
            else -> {}
        }
    }

    fun noteOn(keyCode: Int, semitone: Int) {
        if (activeKeys.containsKey(keyCode)) return
        val note = noteFor(semitone)
        val notes = if (chordMode) lastChord?.intervals?.map { it + note } ?: listOf(note) else listOf(note)
        activeKeys[keyCode] = notes
        if (sustain) {
            sustainKeys[keyCode] = notes
        }
        notes.forEach { send(ShortMessage.NOTE_ON, it, VELOCITY) }
    }

    fun noteOff(keyCode: Int) {
        val notes = activeKeys.remove(keyCode) ?: return
        if (!sustain) {
            notes.forEach { note ->
                send(ShortMessage.NOTE_OFF, note, 0)
            }
        }
    }

    fun pitchBend(bend: Double) {
        val b = ((bend / 2 + 0.5).coerceIn(0.0, 1.0) * 0x3fff).toInt()
        send(ShortMessage.PITCH_BEND, (b and 0x7f), b.shr(7) and 0x7f)
    }

    fun tremolo(tremolo: Double) {
        val t = ((tremolo / 2 + 0.5).coerceIn(0.0, 1.0) * 0x7f).toInt()
        send(ShortMessage.CONTROL_CHANGE, 1, t)
    }

    fun cc(param: Int, amt: Double) {
        val t = ((amt / 2 + 0.5).coerceIn(0.0, 1.0) * 0x7f).toInt()
        send(ShortMessage.CONTROL_CHANGE, param, t)
    }

    fun allNotesOff() {
        activeKeys.values.forEach { notes -> notes.forEach { send(ShortMessage.NOTE_OFF, it, 0) } }
        activeKeys.clear()
    }

    fun shiftOctave(delta: Int) {
        allNotesOff()
        octave += delta
    }

    fun beginSustain() {
        if (sustain) return
        sustain = true
        for ((key, value) in activeKeys) {
            sustainKeys[key] = value
        }
    }

    fun endSustain() {
        if (!sustain) return
        sustain = false
        for (key in sustainKeys) {
            key.value.forEach {
                send(ShortMessage.NOTE_OFF, it, 0)
            }
        }
        sustainKeys.clear()
    }

    fun beginChord() {
        if (chordMode) return
        chordMode = true
        allNotesOff()
    }

    fun endChord() {
        if (!chordMode) return
        chordMode = false
        allNotesOff()
    }

    fun selectChord(chord: ChordMidiAction?) {
        if (!chordMode) return
        lastChord = chord
    }

    private fun noteFor(semitone: Int): Int {
        return (octave * 12 + semitone).coerceIn(0, 127)
    }

    private fun send(command: Int, note: Int, velocity: Int) {
        receiver.send(ShortMessage(command, channel, note, velocity), -1)
    }

    companion object {
        private const val VELOCITY = 100
    }
}
