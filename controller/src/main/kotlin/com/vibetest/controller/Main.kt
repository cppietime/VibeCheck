package com.vibetest.controller

import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent
import com.github.kwhat.jnativehook.mouse.NativeMouseListener
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.system.exitProcess

/**
 * Controller application: captures global keyboard and mouse input and forwards
 * it as MIDI to a port (e.g. a loopMIDI virtual port).
 *
 * Usage: controller [--list] [--port <name-or-index>] [--channel <0-15>]
 */
fun main(args: Array<String>) {
    val options = parseArgs(args)

    if (options.list) {
        listMidiPorts()
        return
    }

    val device = openMidiOutput(options.port) ?: run {
        System.err.println("Use --list to see available ports.")
        exitProcess(1)
    }

    val midi = InputToMidi(receiverFor(device), options.channel)
    println("Sending MIDI to '${device.deviceInfo.name}' on channel ${options.channel + 1}.")
    println("Keys Z-M play notes, S/D/G/H/J play sharps, arrow keys change octave. Ctrl+C to quit.")

    // jnativehook logs every event at INFO level by default; silence it.
    Logger.getLogger(GlobalScreen::class.java.packageName).level = Level.WARNING

    val listener = object : NativeKeyListener, NativeMouseListener {
        override fun nativeKeyPressed(event: NativeKeyEvent) {
            when (event.keyCode) {
                NativeKeyEvent.VC_UP -> midi.shiftOctave(1)
                NativeKeyEvent.VC_DOWN -> midi.shiftOctave(-1)
                else -> midi.noteOn(event.keyCode)
            }
        }

        override fun nativeKeyReleased(event: NativeKeyEvent) {
            midi.noteOff(event.keyCode)
        }

        override fun nativeMousePressed(event: NativeMouseEvent) {
            midi.noteOn(event.button)
        }

        override fun nativeMouseReleased(event: NativeMouseEvent) {
            midi.noteOff(event.button)
        }
    }

    GlobalScreen.registerNativeHook()
    GlobalScreen.addNativeKeyListener(listener)
    GlobalScreen.addNativeMouseListener(listener)

    Runtime.getRuntime().addShutdownHook(Thread {
        midi.allNotesOff()
        GlobalScreen.unregisterNativeHook()
        device.close()
    })

    // The native hook runs on its own thread; keep the main thread alive.
    Thread.currentThread().join()
}

private data class Options(
    val list: Boolean = false,
    val port: String = "loopMIDI",
    val channel: Int = 0,
)

private fun parseArgs(args: Array<String>): Options {
    var options = Options()
    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--list" -> options = options.copy(list = true)
            "--port" -> options = options.copy(port = args.getOrNull(++i) ?: options.port)
            "--channel" -> {
                val channel = args.getOrNull(++i)?.toIntOrNull()
                if (channel == null || channel !in 0..15) {
                    System.err.println("--channel expects a number from 0 to 15.")
                    exitProcess(1)
                }
                options = options.copy(channel = channel)
            }
            else -> {
                System.err.println("Unknown argument: ${args[i]}")
                exitProcess(1)
            }
        }
        i++
    }
    return options
}
