package com.vibetest.controller.ui

import com.vibetest.controller.config.defaultMidiConfig
import com.vibetest.controller.midi.InputToMidi
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities

class ControllerWindow(midi: InputToMidi) {
    lateinit var frame: JFrame
    lateinit var mousePanel: ControllerMousePanel

    private var xLabel: JLabel? = null
    private var yLabel: JLabel? = null
    private var zLabel: JLabel? = null

    init {
        SwingUtilities.invokeLater { createGui(midi) }
    }

    fun createGui(midi: InputToMidi) {
        frame = JFrame("MIDI Controller")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

        val panel = JPanel(BorderLayout())

        mousePanel = ControllerMousePanel(midi, this)
        panel.add(mousePanel, BorderLayout.NORTH)

        val infoPanel = JPanel(BorderLayout())
        xLabel = JLabel()
        yLabel = JLabel()
        zLabel = JLabel()
        infoPanel.add(xLabel!!, BorderLayout.WEST)
        infoPanel.add(yLabel!!, BorderLayout.EAST)
        infoPanel.add(zLabel!!, BorderLayout.SOUTH)

        panel.add(infoPanel, BorderLayout.SOUTH)

        frame.add(panel)
        frame.pack()

        object : WindowAdapter() {
            override fun windowGainedFocus(e: WindowEvent?) {
                mousePanel.requestFocusInWindow()
            }

            override fun windowLostFocus(e: WindowEvent?) {
                midi.allNotesOff()
            }

            override fun windowIconified(e: WindowEvent?) {
                midi.allNotesOff()
            }
        }.let {
            frame.addWindowFocusListener(it)
            frame.addWindowListener(it)
        }

        frame.setLocationRelativeTo(null)
        frame.isVisible = true

        updateLabels()
    }

    fun updateLabels() {
        xLabel?.text = "X: ${mousePanel.xOffset}"
        yLabel?.text = "Y: ${mousePanel.yOffset}"
        zLabel?.text = "Wheel: ${mousePanel.zOffset}"
        frame.pack()
    }
}

class ControllerMousePanel(val midi: InputToMidi, private val window: ControllerWindow) : JPanel() {
    companion object {
        val WIDTH = 400
        val HEIGHT = 400
    }

    private var startingPoint = Point(WIDTH / 2, HEIGHT / 2)
    private var lastPoint = Point(0, 0)
    private var tracking = false

    var xOffset = 0.0
        private set
    var yOffset = 0.0
        private set
    var zOffset = 0.0
        private set

    init {
        preferredSize = Dimension(WIDTH, HEIGHT)
        minimumSize = Dimension(WIDTH, HEIGHT)
        background = Color.BLUE
        isFocusable = true

        val mouseAdapter = object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                repaint()
            }

            override fun mouseExited(e: MouseEvent?) {
                repaint()
            }

            override fun mousePressed(e: MouseEvent) {
                requestFocusInWindow()
                when (e.button) {
                    MouseEvent.BUTTON1 -> {
                        lastPoint = e.point
                        startTrackingMouse()
                    }
                }
                midi.keyPress(e.button, midi.config.mouseButtonMap)
                repaint()
            }

            override fun mouseReleased(e: MouseEvent) {
                when (e.button) {
                    MouseEvent.BUTTON1 -> {
                        lastPoint = e.point
                        stopTrackingMouse()
                    }
                }
                midi.keyRelease(e.button, midi.config.mouseButtonMap)
                repaint()
            }

            override fun mouseDragged(e: MouseEvent) {
                lastPoint = e.point
                trackMouse()
                repaint()
            }

            override fun mouseMoved(e: MouseEvent) {
                lastPoint = e.point
                trackMouse()
                repaint()
            }

            override fun mouseWheelMoved(e: MouseWheelEvent) {
                zOffset += e.wheelRotation / 32.0
                trackMouseWheel()
                repaint()
            }
        }

        addMouseListener(mouseAdapter)
        addMouseMotionListener(mouseAdapter)
        addMouseWheelListener(mouseAdapter)

        val keyListener = object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                midi.keyPress(e.keyCode)
            }

            override fun keyReleased(e: KeyEvent) {
                //val note = config.keyNoteMap[e.keyCode] ?: return
                midi.keyRelease(e.keyCode)
            }
        }
        addKeyListener(keyListener)

        requestFocusInWindow()
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)

        val g2d = g as Graphics2D

        val red = if (tracking) (lastPoint.x.toFloat() / WIDTH * 255).toInt().coerceIn(0, 255) else 0
        val green = if (tracking) (lastPoint.y.toFloat() / HEIGHT * 255).toInt().coerceIn(0, 255) else 0
        val blue = if (midi.sustain) 255 else 0

        g2d.color = Color(red, green, blue)
        g2d.fillRect(0, 0, width, height)
    }

    private fun startTrackingMouse() {
        tracking = true
        startingPoint = lastPoint
    }

    private fun stopTrackingMouse() {
        tracking = false
    }

    private fun trackMouse() {
        if (!tracking) return
        xOffset = ((lastPoint.x - startingPoint.x).toDouble() / WIDTH * 2)
        yOffset = ((lastPoint.y - startingPoint.y).toDouble() / HEIGHT * 2)
        midi.pitchBend(xOffset)
        midi.tremolo(yOffset)
        window.updateLabels()
    }

    private fun trackMouseWheel() {
        midi.cc(2, zOffset)
        window.updateLabels()
    }
}
