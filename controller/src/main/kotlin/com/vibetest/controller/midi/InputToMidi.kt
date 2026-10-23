package com.vibetest.controller.midi

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
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
    private val activeKeys = mutableMapOf<Int, Int>()
    private val sustainKeys = mutableMapOf<Int, Int>()
    private var sustain = false

    var octave: Int = 4
        set(value) {
            field = value.coerceIn(0, 8)
        }

    fun keyPress(keyCode: Int, map: Map<Int, MidiAction> = config.keyNoteMap) {
        val action = map[keyCode] ?: return
        when (action) {
            is NoteMidiAction -> noteOn(keyCode, action.note)
            is OctaveMidiAction -> shiftOctave(action.shift)
            is SustainMidiAction -> beginSustain()
        }
    }

    fun keyRelease(keyCode: Int, map: Map<Int, MidiAction> = config.keyNoteMap) {
        val action = map[keyCode] ?: return
        when (action) {
            is NoteMidiAction -> noteOff(keyCode)
            is SustainMidiAction -> endSustain()
            else -> {}
        }
    }

    fun noteOn(keyCode: Int, semitone: Int) {
        if (activeKeys.containsKey(keyCode)) return
        val note = noteFor(semitone)
        activeKeys[keyCode] = note
        if (sustain) {
            sustainKeys[keyCode] = note
        }
        send(ShortMessage.NOTE_ON, note, VELOCITY)
    }

    fun noteOff(keyCode: Int) {
        val note = activeKeys.remove(keyCode) ?: return
        if (!sustain) {
            send(ShortMessage.NOTE_OFF, note, 0)
        }
    }

    fun pitchBend(bend: Double) {
        val b = ((bend / 2 + 0.5).coerceIn(0.0, 1.0) * 0x3fff).toInt()
        println("Vibrato $bend $b")
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
        activeKeys.values.forEach { send(ShortMessage.NOTE_OFF, it, 0) }
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
            send(ShortMessage.NOTE_OFF, key.value, 0)
        }
        sustainKeys.clear()
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
