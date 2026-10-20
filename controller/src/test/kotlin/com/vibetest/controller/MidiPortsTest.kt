package com.vibetest.controller

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import javax.sound.midi.MidiDevice
import javax.sound.midi.MidiMessage
import javax.sound.midi.MidiSystem
import javax.sound.midi.MidiUnavailableException
import javax.sound.midi.Receiver

class MidiPortsTest {

    @Test
    fun `receiverFor returns device receiver when available`() {
        val device = mock(MidiDevice::class.java)
        val receiver = mock(Receiver::class.java)
        `when`(device.receiver).thenReturn(receiver)

        val result = receiverFor(device)

        assertSame(receiver, result)
    }

    @Test
    fun `receiverFor returns no-op receiver when device has no receiver`() {
        val device = mock(MidiDevice::class.java)
        `when`(device.receiver).thenReturn(null)

        val result = receiverFor(device)

        assertNotNull(result)
        // Verify it's a no-op receiver by sending a message (should not throw)
        result.send(mock(MidiMessage::class.java), 0L)
        result.close()
    }

    @Test
    fun `outputDevices filters devices with maxReceivers > 0`() {
        // This test verifies the logic by checking the function exists and is callable
        // Actual MidiSystem behavior is tested in integration tests
        val outputs = outputDevices()
        assertNotNull(outputs)
        outputs.forEach { device ->
            assertTrue(device.maxReceivers != 0)
        }
    }
}
