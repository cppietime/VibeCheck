package com.vibetest.synthesizer

import javax.sound.midi.MetaMessage
import javax.sound.midi.MidiMessage
import javax.sound.midi.Receiver
import javax.sound.midi.ShortMessage

/**
 * Receives MIDI messages from the input port and drives a [SynthEngine].
 * Only note on/off and all-notes-off are handled; everything else is ignored.
 */
class MidiToSynth(private val engine: SynthEngine) : Receiver {
    override fun send(message: MidiMessage, timeStamp: Long) {
        when (message) {
            is ShortMessage -> handleShort(message)
            is MetaMessage -> if (message.type == ALL_SOUND_OFF) engine.allNotesOff()
        }
    }

    private fun handleShort(message: ShortMessage) {
        when (message.command) {
            ShortMessage.NOTE_ON -> engine.noteOn(message.data1, message.data2)
            ShortMessage.NOTE_OFF -> engine.noteOff(message.data1)
            ShortMessage.CONTROL_CHANGE ->
                if (message.data1 == ALL_NOTES_OFF_CC) engine.allNotesOff()
        }
    }

    override fun close() = engine.allNotesOff()

    companion object {
        private const val ALL_NOTES_OFF_CC = 123
        private const val ALL_SOUND_OFF = 0x78
    }
}
