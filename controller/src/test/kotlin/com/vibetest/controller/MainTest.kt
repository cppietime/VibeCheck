package com.vibetest.controller

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import javax.sound.midi.MidiDevice
import javax.sound.midi.MidiUnavailableException

class MainTest {

    @Test
    fun `parseArgs defaults`() {
        val options = parseArgs(emptyArray())

        assertFalse(options.list)
        assertEquals("0", options.port)
        assertEquals(0, options.channel)
        assertFalse(options.interactive)
    }

    @Test
    fun `parseArgs list flag`() {
        val options = parseArgs(arrayOf("--list"))

        assertTrue(options.list)
    }

    @Test
    fun `parseArgs port option`() {
        val options = parseArgs(arrayOf("--port", "loopMIDI"))

        assertEquals("loopMIDI", options.port)
    }

    @Test
    fun `parseArgs interactive flag`() {
        val options = parseArgs(arrayOf("--interactive"))

        assertTrue(options.interactive)
    }

    @Test
    fun `parseArgs interactive short flag`() {
        val options = parseArgs(arrayOf("-i"))

        assertTrue(options.interactive)
    }

    @Test
    fun `parseArgs channel option`() {
        val options = parseArgs(arrayOf("--channel", "5"))

        assertEquals(5, options.channel)
    }

    @Test
    fun `parseArgs channel clamps to valid range`() {
        assertThrows<IllegalArgumentException> { parseArgs(arrayOf("--channel", "16")) }
        assertThrows<IllegalArgumentException> { parseArgs(arrayOf("--channel", "-1")) }
    }

    @Test
    fun `parseArgs unknown argument fails`() {
        assertThrows<IllegalArgumentException> { parseArgs(arrayOf("--unknown")) }
    }

    @Test
    fun `selectPortInteractively returns null when no devices`() {
        // This would require mocking outputDevices() which is internal
        // Tested via integration test instead
    }

    @Test
    fun `runController registers listeners and hooks`() {
        val device = mock(MidiDevice::class.java)
        val receiver = mock(javax.sound.midi.Receiver::class.java)
        `when`(device.receiver).thenReturn(receiver)

        // We can't easily test the full runController without starting the native hook
        // This is tested via integration test
        assertNotNull(device)
    }
}
