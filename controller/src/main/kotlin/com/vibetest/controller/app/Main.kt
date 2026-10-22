package com.vibetest.controller.app

import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent
import com.github.kwhat.jnativehook.mouse.NativeMouseListener
import com.vibetest.controller.midi.InputToMidi
import com.vibetest.controller.midi.listMidiPorts
import com.vibetest.controller.midi.openMidiOutput
import com.vibetest.controller.midi.outputDevices
import com.vibetest.controller.midi.receiverFor
import com.vibetest.controller.ui.ControllerWindow
import javax.sound.midi.MidiDevice
import javax.sound.midi.MidiUnavailableException
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.system.exitProcess

/**
 * Controller application: captures global keyboard and mouse input and forwards
 * it as MIDI to a port (e.g. a loopMIDI virtual port).
 *
 * Usage: controller [--list] [--port <name-or-index>] [--interactive] [--channel <0-15>]
 */
fun main(args: Array<String>) {
    val options = parseArgs(args)

    if (options.list) {
        listMidiPorts()
        return
    }

    if (options.interactive) {
        val device = selectPortInteractively() ?: return
        runController(device, options.channel)
        return
    }

    if (options.port != "0" && options.interactive) {
        System.err.println("--port and --interactive are mutually exclusive.")
        exitProcess(1)
    }

    val device = openMidiOutput(options.port) ?: run {
        System.err.println("Use --list to see available ports.")
        exitProcess(1)
    }

    val midi = InputToMidi(receiverFor(device), options.channel)
    println("Sending MIDI to '${device.deviceInfo.name}' on channel ${options.channel + 1}.")
    println("Keys Z-M play notes, S/D/G/H/J play sharps, arrow keys change octave. Ctrl+C to quit.")

    // jnativehook logs every event at INFO level by default; silence it.
    // Class.getPackageName() is Java 9+; use getPackage().getName() for Java 8 compat.
    Logger.getLogger(GlobalScreen::class.java.`package`?.name ?: "").level = Level.WARNING

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

    ControllerWindow(midi)

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
    val port: String = "0",
    val channel: Int = 0,
    val interactive: Boolean = false,
)

private fun parseArgs(args: Array<String>): Options {
    var options = Options()
    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--list" -> options = options.copy(list = true)
            "--port" -> options = options.copy(port = args.getOrNull(++i) ?: options.port)
            "--interactive", "-i" -> options = options.copy(interactive = true)
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

private fun selectPortInteractively(): MidiDevice? {
    val outputs = outputDevices()
    if (outputs.isEmpty()) {
        println("No MIDI output devices found.")
        return null
    }

    println("Available MIDI outputs:")
    outputs.forEachIndexed { index, device ->
        println("  [$index] ${device.deviceInfo.name} (${device.deviceInfo.vendor})")
    }

    print("Select port [0-${outputs.lastIndex}]: ")
    val input = readlnOrNull()?.trim()
    val index = input?.toIntOrNull()
        ?.takeIf { it in outputs.indices }
        ?: run {
            System.err.println("Invalid selection.")
            return null
        }

    val device = outputs[index]
    try {
        device.open()
        return device
    } catch (e: MidiUnavailableException) {
        System.err.println("Could not open MIDI output '${device.deviceInfo.name}': ${e.message}")
        outputs.filter { it !== device }.forEach { it.close() }
        return null
    }
}

private fun runController(device: MidiDevice, channel: Int) {
    val midi = InputToMidi(receiverFor(device), channel)
    println("Sending MIDI to '${device.deviceInfo.name}' on channel ${channel + 1}.")
    println("Keys Z-M play notes, S/D/G/H/J play sharps, arrow keys change octave. Ctrl+C to quit.")

    Logger.getLogger(GlobalScreen::class.java.`package`?.name ?: "").level = Level.WARNING

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

    Thread.currentThread().join()
}
