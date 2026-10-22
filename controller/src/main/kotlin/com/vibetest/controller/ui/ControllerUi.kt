package com.vibetest.controller.ui

import com.vibetest.controller.midi.InputToMidi
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities

class ControllerWindow(midi: InputToMidi) {
    lateinit var frame: JFrame
    lateinit var mousePanel: ControllerMousePanel

    init {
        SwingUtilities.invokeLater { createGui(midi) }
    }

    fun createGui(midi: InputToMidi) {
        frame = JFrame("MIDI Controller")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

        val panel = JPanel(BorderLayout())

        mousePanel = ControllerMousePanel(midi)
        panel.add(mousePanel, BorderLayout.NORTH)

        frame.add(panel)
        frame.pack()

        frame.setLocationRelativeTo(null)
        frame.isVisible = true
    }
}

class ControllerMousePanel(val midi: InputToMidi) : JPanel() {
    companion object {
        val WIDTH = 400
        val HEIGHT = 400
    }

    private var startingPoint = Point(WIDTH / 2, HEIGHT / 2)
    private var lastPoint = Point(0, 0)
    private var tracking = false

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
                startingPoint = e.point
                lastPoint = e.point
                tracking = true
                repaint()
            }

            override fun mouseReleased(e: MouseEvent) {
                lastPoint = e.point
                tracking = false
                repaint()
            }

            override fun mouseDragged(e: MouseEvent) {
                if (tracking) {
                    lastPoint = e.point
                    midi.pitchBend((e.point.x - startingPoint.x) * 0x3FFF / WIDTH)
                }
                repaint()
            }

            override fun mouseMoved(e: MouseEvent) {
                if (tracking) {
                    lastPoint = e.point
                    midi.pitchBend((e.point.x - startingPoint.x) * 0x3FFF / WIDTH)
                }
                repaint()
            }
        }

        addMouseListener(mouseAdapter)
        addMouseMotionListener(mouseAdapter)
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)

        val g2d = g as Graphics2D

        g2d.color = if (tracking) Color(
            (lastPoint.x.toFloat() / WIDTH * 255).toInt().coerceIn(0, 255),
            (lastPoint.y.toFloat() / HEIGHT * 255).toInt().coerceIn(0, 255),
            0
        ) else Color.BLACK
        g2d.fillRect(0, 0, width, height)
    }
}
