package com.vibetest.controller

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import javax.sound.midi.Receiver
import javax.sound.midi.ShortMessage

class InputToMidiTest {

    private lateinit var receiver: Receiver
    private lateinit var midi: InputToMidi

    @BeforeEach
    fun setUp() {
        receiver = mock(Receiver::class.java)
        midi = InputToMidi(receiver, 0)
    }

    @Test
    fun `noteOn sends NOTE_ON message for mapped key`() {
        midi.noteOn(NativeKeyEvent.VC_Z) // Z -> semitone 0, octave 4 -> note 48

        verify(receiver).send(argThat {
            (it as ShortMessage).command == ShortMessage.NOTE_ON
                    && (it as ShortMessage).data1 == 48
                    && (it as ShortMessage).data2 == 100
        }, eq(-1L))
    }

    @Test
    fun `noteOn does not retrigger for already pressed key`() {
        midi.noteOn(NativeKeyEvent.VC_Z)
        midi.noteOn(NativeKeyEvent.VC_Z) // Second press should be ignored

        verify(receiver, times(1)).send(any(), anyLong())
    }

    @Test
    fun `noteOff sends NOTE_OFF message for pressed key`() {
        midi.noteOn(NativeKeyEvent.VC_Z)
        midi.noteOff(NativeKeyEvent.VC_Z)

        verify(receiver).send(argThat {
            (it as ShortMessage).command == ShortMessage.NOTE_OFF
                    && (it as ShortMessage).data1 == 48
                    && (it as ShortMessage).data2 == 0
        }, eq(-1L))
    }

    @Test
    fun `noteOff does nothing for key not pressed`() {
        midi.noteOff(NativeKeyEvent.VC_Z) // Never pressed

        verifyNoInteractions(receiver)
    }

    @Test
    fun `allNotesOff sends NOTE_OFF for all active keys`() {
        midi.noteOn(NativeKeyEvent.VC_Z) // note 48
        midi.noteOn(NativeKeyEvent.VC_X) // note 50
        midi.allNotesOff()

        verify(receiver, times(2)).send(argThat { (it as ShortMessage).command == ShortMessage.NOTE_OFF }, eq(-1L))
    }

    @Test
    fun `shiftOctave changes octave and turns off active notes`() {
        midi.noteOn(NativeKeyEvent.VC_Z) // note 48 (octave 4)
        midi.shiftOctave(1) // octave 5, should turn off note 48

        verify(receiver).send(argThat {
            (it as ShortMessage).command == ShortMessage.NOTE_OFF
                    && (it as ShortMessage).data1 == 48
        }, eq(-1L))
        assertEquals(5, midi.octave)
    }

    @Test
    fun `shiftOctave clamps octave to valid range`() {
        midi.octave = 8
        midi.shiftOctave(1)
        assertEquals(8, midi.octave)

        midi.octave = 0
        midi.shiftOctave(-1)
        assertEquals(0, midi.octave)
    }

    @Test
    fun `noteFor returns correct note for white keys`() {
        // Using reflection to test private function
        val method = InputToMidi::class.java.getDeclaredMethod("noteFor", Int::class.java)
        method.isAccessible = true

        assertEquals(48, method.invoke(midi, NativeKeyEvent.VC_Z)) // C4
        assertEquals(50, method.invoke(midi, NativeKeyEvent.VC_X)) // D4
        assertEquals(52, method.invoke(midi, NativeKeyEvent.VC_C)) // E4
        assertEquals(53, method.invoke(midi, NativeKeyEvent.VC_V)) // F4
        assertEquals(55, method.invoke(midi, NativeKeyEvent.VC_B)) // G4
        assertEquals(57, method.invoke(midi, NativeKeyEvent.VC_N)) // A4
        assertEquals(59, method.invoke(midi, NativeKeyEvent.VC_M)) // B4
        assertEquals(60, method.invoke(midi, NativeKeyEvent.VC_COMMA)) // C5
    }

    @Test
    fun `noteFor returns correct note for black keys`() {
        val method = InputToMidi::class.java.getDeclaredMethod("noteFor", Int::class.java)
        method.isAccessible = true

        assertEquals(49, method.invoke(midi, NativeKeyEvent.VC_S)) // C#4
        assertEquals(51, method.invoke(midi, NativeKeyEvent.VC_D)) // D#4
        assertEquals(54, method.invoke(midi, NativeKeyEvent.VC_G)) // F#4
        assertEquals(56, method.invoke(midi, NativeKeyEvent.VC_H)) // G#4
        assertEquals(58, method.invoke(midi, NativeKeyEvent.VC_J)) // A#4
    }

    @Test
    fun `noteFor returns null for unmapped key`() {
        val method = InputToMidi::class.java.getDeclaredMethod("noteFor", Int::class.java)
        method.isAccessible = true

        assertNull(method.invoke(midi, NativeKeyEvent.VC_A)) // Not in map
        assertNull(method.invoke(midi, 999)) // Random key
    }

    @Test
    fun `noteFor respects octave changes`() {
        val method = InputToMidi::class.java.getDeclaredMethod("noteFor", Int::class.java)
        method.isAccessible = true

        midi.octave = 3
        assertEquals(36, method.invoke(midi, NativeKeyEvent.VC_Z)) // C3

        midi.octave = 5
        assertEquals(60, method.invoke(midi, NativeKeyEvent.VC_Z)) // C5
    }

    @Test
    fun `noteFor clamps note to valid MIDI range`() {
        val method = InputToMidi::class.java.getDeclaredMethod("noteFor", Int::class.java)
        method.isAccessible = true

        midi.octave = 8
        assertEquals(108, method.invoke(midi, NativeKeyEvent.VC_M)) // B8 = 108, not 120

        midi.octave = 0
        assertEquals(11, method.invoke(midi, NativeKeyEvent.VC_M)) // B0 = 11
    }

    @Test
    fun `different channels use different channel in message`() {
        val receiver2 = mock(Receiver::class.java)
        val midi2 = InputToMidi(receiver2, 5) // Channel 5 (0-indexed = MIDI channel 6)

        midi2.noteOn(NativeKeyEvent.VC_Z)

        verify(receiver2).send(argThat { (it as ShortMessage).channel == 5 }, eq(-1L))
    }
}
