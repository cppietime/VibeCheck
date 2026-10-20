package com.vibetest.synthesizer

import javax.sound.midi.MidiDevice
import javax.sound.midi.MidiSystem
import javax.sound.midi.MidiUnavailableException
import javax.sound.midi.Transmitter
import kotlin.system.exitProcess

/**
 * Synthesizer application: reads MIDI from an input port (e.g. a loopMIDI
 * virtual port) and renders it to the default audio device in real time.
 *
 * Usage: synthesizer [--list] [--port <name-or-index>] [--interactive] [--rate <hz>]
 */
fun main(args: Array<String>) {
    val options = parseArgs(args)

    if (options.list) {
        listMidiInputs()
        return
    }

    if (options.interactive) {
        val device = selectPortInteractively() ?: return
        runSynthesizer(device, options.sampleRate)
        return
    }

    if (options.port != "loopMIDI" && options.interactive) {
        System.err.println("--port and --interactive are mutually exclusive.")
        exitProcess(1)
    }

    val device = openMidiInput(options.port) ?: run {
        System.err.println("Use --list to see available ports.")
        exitProcess(1)
    }

    val engine = SynthEngine(options.sampleRate)
    val audio = AudioOutput(engine, options.sampleRate)
    audio.start()

    val transmitter: Transmitter = device.transmitter
    transmitter.receiver = MidiToSynth(engine)

    println("Listening for MIDI on '${device.deviceInfo.name}'.")
    println("Rendering at ${options.sampleRate} Hz to the default audio device. Ctrl+C to quit.")

    Runtime.getRuntime().addShutdownHook(Thread {
        transmitter.close()
        device.close()
        audio.stop()
    })

    Thread.currentThread().join()
}

private fun listMidiInputs() {
    val inputs = inputDevices()
    if (inputs.isEmpty()) {
        println("No MIDI input devices found.")
        return
    }

    println("Available MIDI inputs:")
    inputs.forEachIndexed { index, device ->
        println("  [$index] ${device.deviceInfo.name} (${device.deviceInfo.vendor})")
    }
}

private fun openMidiInput(selector: String): MidiDevice? {
    val inputs = inputDevices()

    val device = selector.toIntOrNull()
        ?.takeIf { it in inputs.indices }
        ?.let { inputs[it] }
        ?: inputs.firstOrNull { it.deviceInfo.name.contains(selector, ignoreCase = true) }

    if (device == null) {
        System.err.println("No MIDI input matched '$selector'.")
        return null
    }

    return try {
        device.open()
        device
    } catch (e: MidiUnavailableException) {
        System.err.println("Could not open MIDI input '${device.deviceInfo.name}': ${e.message}")
        null
    }
}

/**
 * Devices that can transmit MIDI. Capabilities are queried on unopened devices:
 * opening a port starts native I/O threads that would keep the JVM alive after
 * the command finishes.
 */
private fun inputDevices(): List<MidiDevice> =
    MidiSystem.getMidiDeviceInfo()
        .mapNotNull { info ->
            try {
                MidiSystem.getMidiDevice(info)
            } catch (e: MidiUnavailableException) {
                null
            }
        }
        .filter { it.maxTransmitters != 0 }

private data class Options(
    val list: Boolean = false,
    val port: String = "loopMIDI",
    val sampleRate: Int = 44_100,
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
            "--rate" -> {
                val rate = args.getOrNull(++i)?.toIntOrNull()
                if (rate == null || rate < 8_000) {
                    System.err.println("--rate expects a sample rate of at least 8000.")
                    exitProcess(1)
                }
                options = options.copy(sampleRate = rate)
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
    val inputs = inputDevices()
    if (inputs.isEmpty()) {
        println("No MIDI input devices found.")
        return null
    }

    println("Available MIDI inputs:")
    inputs.forEachIndexed { index, device ->
        println("  [$index] ${device.deviceInfo.name} (${device.deviceInfo.vendor})")
    }

    print("Select port [0-${inputs.lastIndex}]: ")
    val input = readlnOrNull()?.trim()
    val index = input?.toIntOrNull()
        ?.takeIf { it in inputs.indices }
        ?: run {
            System.err.println("Invalid selection.")
            return null
        }

    val device = inputs[index]
    return try {
        device.open()
        device
    } catch (e: MidiUnavailableException) {
        System.err.println("Could not open MIDI input '${device.deviceInfo.name}': ${e.message}")
        inputs.filter { it !== device }.forEach { it.close() }
        null
    }
}

private fun runSynthesizer(device: MidiDevice, sampleRate: Int) {
    val engine = SynthEngine(sampleRate)
    val audio = AudioOutput(engine, sampleRate)
    audio.start()

    val transmitter: Transmitter = device.transmitter
    transmitter.receiver = MidiToSynth(engine)

    println("Listening for MIDI on '${device.deviceInfo.name}'.")
    println("Rendering at $sampleRate Hz to the default audio device. Ctrl+C to quit.")

    Runtime.getRuntime().addShutdownHook(Thread {
        transmitter.close()
        device.close()
        audio.stop()
    })

    Thread.currentThread().join()
}
