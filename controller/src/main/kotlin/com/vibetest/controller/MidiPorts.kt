package com.vibetest.controller

import javax.sound.midi.MidiDevice
import javax.sound.midi.MidiMessage
import javax.sound.midi.MidiSystem
import javax.sound.midi.MidiUnavailableException
import javax.sound.midi.Receiver

/**
 * Lists the MIDI ports that can be opened for output, so the user can pick one
 * to send to.
 */
fun listMidiPorts() {
    val outputs = outputDevices()
    if (outputs.isEmpty()) {
        println("No MIDI output devices found.")
        return
    }

    println("Available MIDI outputs:")
    outputs.forEachIndexed { index, device ->
        println("  [$index] ${device.deviceInfo.name} (${device.deviceInfo.vendor})")
    }
}

/**
 * Resolves a MIDI device by index or case-insensitive name substring and opens
 * it for output. Returns null if nothing matched or the device failed to open.
 */
fun openMidiOutput(selector: String): MidiDevice? {
    val outputs = outputDevices()

    val device = selector.toIntOrNull()
        ?.takeIf { it in outputs.indices }
        ?.let { outputs[it] }
        ?: outputs.firstOrNull { it.deviceInfo.name.contains(selector, ignoreCase = true) }

    if (device == null) {
        System.err.println("No MIDI output matched '$selector'.")
        return null
    }

    return try {
        device.open()
        device
    } catch (e: MidiUnavailableException) {
        System.err.println("Could not open MIDI output '${device.deviceInfo.name}': ${e.message}")
        null
    }
}

/**
 * Returns the receiver that sends messages to [device]. Output devices always
 * expose one once opened; the no-op fallback keeps callers free of null checks.
 */
fun receiverFor(device: MidiDevice): Receiver =
    device.receiver ?: object : Receiver {
        override fun send(message: MidiMessage, timeStamp: Long) = Unit
        override fun close() = Unit
    }

/**
 * Devices that can receive MIDI. Capabilities are queried on unopened devices:
 * opening a port starts native I/O threads that would keep the JVM alive after
 * the command finishes.
 */
private fun outputDevices(): List<MidiDevice> =
    MidiSystem.getMidiDeviceInfo()
        .mapNotNull { info ->
            try {
                MidiSystem.getMidiDevice(info)
            } catch (e: MidiUnavailableException) {
                null
            }
        }
        .filter { it.maxReceivers != 0 }
