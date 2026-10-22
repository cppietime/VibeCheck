package com.vibetest.controller.config

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import java.awt.event.KeyEvent

class MidiConfig(
    val keyNoteMap: Map<Int, Int>,
    val octaveUpKey: Int,
    val octaveDownKey: Int,
    val xChannel: String,
    val yChannel: String,
    val wheelChannel: String,
)

fun defaultMidiConfig() = MidiConfig(
    mapOf(
        KeyEvent.VK_Z to 0,
        KeyEvent.VK_S to 1,
        KeyEvent.VK_X to 2,
        KeyEvent.VK_D to 3,
        KeyEvent.VK_C to 4,
        KeyEvent.VK_V to 5,
        KeyEvent.VK_G to 6,
        KeyEvent.VK_B to 7,
        KeyEvent.VK_H to 8,
        KeyEvent.VK_N to 9,
        KeyEvent.VK_J to 10,
        KeyEvent.VK_M to 11,
        KeyEvent.VK_COMMA to 12,
    ),
    NativeKeyEvent.VC_UP,
    NativeKeyEvent.VC_DOWN,
    "PITCH",
    "7",
    "1",
)
