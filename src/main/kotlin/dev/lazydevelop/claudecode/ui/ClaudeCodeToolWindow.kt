package dev.lazydevelop.claudecode.ui

import dev.lazydevelop.claudecode.service.ClaudeCodeService
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class ClaudeCodeToolWindow(private val project: Project) {
    private val logger = Logger.getInstance(ClaudeCodeToolWindow::class.java)
    private val claudeService = project.getService(ClaudeCodeService::class.java)
    private val tabbedPane = JBTabbedPane()

    private val mainPanel = JPanel(BorderLayout()).apply {
        border = JBUI.Borders.empty(5)
        add(tabbedPane, BorderLayout.CENTER)
    }

    init {
        createNewTab()
        addPlusTab()
    }

    fun getContent(): JComponent = mainPanel

    private fun addPlusTab() {
        val plusPanel = JPanel()
        tabbedPane.addTab("", plusPanel)

        val tabComponent = JPanel(FlowLayout(FlowLayout.CENTER, 0, 0)).apply {
            isOpaque = false

            val button = object : JButton() {
                var isHovered = false

                init {
                    preferredSize = Dimension(24, 24)
                    minimumSize = Dimension(24, 24)
                    maximumSize = Dimension(24, 24)
                    isBorderPainted = false
                    isContentAreaFilled = false
                    isFocusPainted = false
                    isOpaque = false
                    toolTipText = "New Claude Tab"
                    border = null

                    icon = object : Icon {
                        override fun getIconWidth() = 16
                        override fun getIconHeight() = 16
                        override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) {
                            g as Graphics2D
                            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                            g.color = c?.foreground ?: Color.GRAY
                            g.stroke = BasicStroke(2f)
                            g.drawLine(x + 8, y + 4, x + 8, y + 12)
                            g.drawLine(x + 4, y + 8, x + 12, y + 8)
                        }
                    }

                    addMouseListener(object : MouseAdapter() {
                        override fun mouseEntered(e: MouseEvent?) {
                            isHovered = true
                            repaint()
                        }

                        override fun mouseExited(e: MouseEvent?) {
                            isHovered = false
                            repaint()
                        }
                    })

                    addActionListener {
                        createNewTab()
                    }
                }

                override fun paintComponent(g: Graphics?) {
                    if (isHovered) {
                        g?.color = Color(128, 128, 128, 40)
                        g?.fillRect(0, 0, width, height)
                    }
                    super.paintComponent(g)
                }
            }

            add(button)
        }

        tabbedPane.setTabComponentAt(tabbedPane.tabCount - 1, tabComponent)

        tabbedPane.addChangeListener {
            if (tabbedPane.selectedIndex == tabbedPane.tabCount - 1 && tabbedPane.tabCount > 1) {
                tabbedPane.selectedIndex = tabbedPane.tabCount - 2
            }
        }
    }

    private fun createNewTab() {
        val widget = claudeService.createNewClaudeTab()

        if (widget != null) {
            val currentClaudeTabs = if (tabbedPane.tabCount > 0) tabbedPane.tabCount - 1 else 0
            val tabNumber = currentClaudeTabs + 1
            val initialTabTitle = "$tabNumber: Claude Code"

            val tabPanel = JPanel(BorderLayout()).apply {
                add(widget.component, BorderLayout.CENTER)
            }

            val insertIndex = maxOf(0, tabbedPane.tabCount - 1)
            if (tabbedPane.tabCount > 0 && insertIndex < tabbedPane.tabCount) {
                tabbedPane.insertTab(initialTabTitle, null, tabPanel, null, insertIndex)
            } else {
                tabbedPane.addTab(initialTabTitle, tabPanel)
            }

            val titleLabel = JLabel(initialTabTitle)
            val closeTabComponent = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
            closeTabComponent.isOpaque = false
            closeTabComponent.add(titleLabel)
            closeTabComponent.add(Box.createHorizontalStrut(5))

            val closeButton = object : JButton("×") {
                var isHovered = false

                init {
                    preferredSize = Dimension(18, 18)
                    isBorderPainted = false
                    isContentAreaFilled = false
                    isFocusPainted = false
                    isOpaque = false
                    toolTipText = "Close"
                    border = null

                    addMouseListener(object : MouseAdapter() {
                        override fun mouseEntered(e: MouseEvent?) {
                            isHovered = true
                            repaint()
                        }

                        override fun mouseExited(e: MouseEvent?) {
                            isHovered = false
                            repaint()
                        }
                    })
                }

                override fun paintComponent(g: Graphics?) {
                    if (isHovered) {
                        g?.color = Color(128, 128, 128, 40)
                        g?.fillRect(0, 0, width, height)
                    }
                    super.paintComponent(g)
                }
            }

            closeButton.addActionListener {
                val idx = tabbedPane.indexOfTabComponent(closeTabComponent)
                if (idx >= 0) {
                    try {
                        widget.close()
                    } catch (e: Exception) {
                    }
                    tabbedPane.removeTabAt(idx)
                }
            }
            closeTabComponent.add(closeButton)

            val tabIndex = if (insertIndex < tabbedPane.tabCount - 1) insertIndex else tabbedPane.tabCount - 1
            tabbedPane.setTabComponentAt(tabIndex, closeTabComponent)

            Thread {
                var lastTitle = initialTabTitle
                var lastBufferSnapshot = ""

                while (widget.ttyConnector?.isConnected == true) {
                    try {
                        val terminalPanel = widget.terminalPanel
                        val textBuffer = terminalPanel?.terminalTextBuffer

                        if (textBuffer != null) {
                            val screenLines = textBuffer.screenLinesCount

                            val recentLines = mutableListOf<String>()
                            for (i in 0 until screenLines) {
                                val line = textBuffer.getLine(i)
                                if (line != null) {
                                    val lineText = line.text.trim()
                                    if (lineText.isNotEmpty()) {
                                        recentLines.add(lineText)
                                    }
                                }
                            }

                            val currentSnapshot = recentLines.joinToString("\n")

                            if (currentSnapshot != lastBufferSnapshot) {
                                lastBufferSnapshot = currentSnapshot

                                val newTitle = extractContextFromTerminal(recentLines, tabNumber)

                                if (newTitle != lastTitle) {
                                    logger.info("Extracted context for tab $tabNumber: '$newTitle'")
                                    lastTitle = newTitle

                                    SwingUtilities.invokeLater {
                                        titleLabel.text = newTitle
                                        closeTabComponent.revalidate()
                                        closeTabComponent.repaint()
                                    }
                                }
                            }
                        }

                        Thread.sleep(3000)
                    } catch (e: Exception) {
                        logger.warn("Error monitoring terminal buffer: ${e.message}", e)
                        break
                    }
                }
            }.start()

            tabbedPane.selectedIndex = tabIndex

            widget.component.requestFocus()
        } else {
            JOptionPane.showMessageDialog(
                mainPanel,
                "Failed to start Claude. Make sure 'claude' CLI is installed.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    private fun extractContextFromTerminal(lines: List<String>, tabNumber: Int): String {
        var userQuestion: String? = null

        for (i in lines.indices) {
            val trimmed = lines[i].trim()

            if (trimmed.startsWith("❯")) {
                val hasContentAfter = i < lines.size - 1 && lines.subList(i + 1, lines.size).any {
                    it.trim().isNotEmpty() && !it.trim().startsWith("❯")
                }

                if (hasContentAfter) {
                    userQuestion = trimmed.substring(1).trim()
                    logger.info("Found completed user input: '$userQuestion'")
                    break
                }
            }
        }

        if (userQuestion != null && userQuestion.isNotEmpty()) {
            val maxLength = 45
            val title = if (userQuestion.length > maxLength) {
                userQuestion.take(maxLength).trim() + "..."
            } else {
                userQuestion
            }
            return "$tabNumber: $title"
        }

        return "$tabNumber: Claude Code"
    }
}
