package com.vibetest.controller.config

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent

sealed interface MidiAction

data class NoteMidiAction(val note: Int) : MidiAction
data class OctaveMidiAction(val shift: Int) : MidiAction
data class SustainMidiAction(val dummy: Boolean = false) : MidiAction

class MidiConfig(
    val keyNoteMap: Map<Int, MidiAction>,
    val mouseButtonMap: Map<Int, MidiAction>,
    val xChannel: String,
    val yChannel: String,
    val wheelChannel: String,
)

fun defaultMidiConfig() = MidiConfig(
    mapOf(
        KeyEvent.VK_Z to NoteMidiAction(0),
        KeyEvent.VK_S to NoteMidiAction(1),
        KeyEvent.VK_X to NoteMidiAction(2),
        KeyEvent.VK_D to NoteMidiAction(3),
        KeyEvent.VK_C to NoteMidiAction(4),
        KeyEvent.VK_V to NoteMidiAction(5),
        KeyEvent.VK_G to NoteMidiAction(6),
        KeyEvent.VK_B to NoteMidiAction(7),
        KeyEvent.VK_H to NoteMidiAction(8),
        KeyEvent.VK_N to NoteMidiAction(9),
        KeyEvent.VK_J to NoteMidiAction(10),
        KeyEvent.VK_M to NoteMidiAction(11),
        KeyEvent.VK_COMMA to NoteMidiAction(12),
        KeyEvent.VK_Q to NoteMidiAction(12),
        KeyEvent.VK_2 to NoteMidiAction(13),
        KeyEvent.VK_W to NoteMidiAction(14),
        KeyEvent.VK_3 to NoteMidiAction(15),
        KeyEvent.VK_E to NoteMidiAction(16),
        KeyEvent.VK_R to NoteMidiAction(17),
        KeyEvent.VK_5 to NoteMidiAction(18),
        KeyEvent.VK_T to NoteMidiAction(19),
        KeyEvent.VK_6 to NoteMidiAction(20),
        KeyEvent.VK_Y to NoteMidiAction(21),
        KeyEvent.VK_7 to NoteMidiAction(22),
        KeyEvent.VK_U to NoteMidiAction(23),
        KeyEvent.VK_I to NoteMidiAction(24),
        KeyEvent.VK_SHIFT to OctaveMidiAction(1),
        KeyEvent.VK_CONTROL to OctaveMidiAction(-1),
    ),
    mapOf(
        MouseEvent.BUTTON3 to SustainMidiAction()
    ),
    "PITCH",
    "7",
    "1",
)
