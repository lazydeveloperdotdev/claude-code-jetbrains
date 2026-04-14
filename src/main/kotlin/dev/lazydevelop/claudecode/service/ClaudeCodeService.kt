package dev.lazydevelop.claudecode.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jediterm.terminal.TtyConnector
import com.pty4j.PtyProcessBuilder
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import java.io.File
import java.nio.charset.StandardCharsets

@Service(Service.Level.PROJECT)
class ClaudeCodeService(private val project: Project) : Disposable {
    private val logger = Logger.getInstance(ClaudeCodeService::class.java)
    private var tabCounter = 1
    private val widgets = mutableListOf<ShellTerminalWidget>()

    fun createNewClaudeTab(): ShellTerminalWidget? {
        try {
            val projectPath = project.basePath ?: System.getProperty("user.home")
            val tabName = "Claude ${tabCounter++}"

            val claudePath = findClaudeExecutable()
            if (claudePath == null) {
                logger.error("Claude executable not found in PATH")
                return null
            }

            logger.info("Found claude at: $claudePath")

            val shellPath = getShellPath()
            val envVars = getShellEnvironment()

            logger.info("Using shell: $shellPath")
            logger.info("PATH: ${envVars["PATH"]}")

            val ptyProcess = PtyProcessBuilder()
                .setCommand(arrayOf(shellPath, "-i", "-l"))
                .setDirectory(projectPath)
                .setEnvironment(envVars)
                .setConsole(false)
                .setCygwin(false)
                .start()

            val connector = object : TtyConnector {
                private var isConnected = true

                override fun close() {
                    isConnected = false
                    ptyProcess.destroy()
                }

                override fun getName() = tabName

                override fun read(buf: CharArray, offset: Int, length: Int): Int {
                    val bytes = ByteArray(length)
                    val bytesRead = ptyProcess.inputStream.read(bytes)
                    if (bytesRead <= 0) {
                        return bytesRead
                    }

                    val str = String(bytes, 0, bytesRead, StandardCharsets.UTF_8)
                    val charCount = minOf(str.length, length)
                    str.toCharArray(buf, offset, 0, charCount)
                    return charCount
                }

                override fun write(bytes: ByteArray) {
                    ptyProcess.outputStream.write(bytes)
                    ptyProcess.outputStream.flush()
                }

                override fun isConnected() = isConnected && ptyProcess.isAlive

                override fun write(string: String) {
                    write(string.toByteArray(StandardCharsets.UTF_8))
                }

                override fun waitFor(): Int {
                    return ptyProcess.waitFor()
                }

                override fun ready(): Boolean = ptyProcess.inputStream.available() > 0

                override fun resize(termWinSize: com.jediterm.core.util.TermSize) {
                    ptyProcess.winSize = com.pty4j.WinSize(termWinSize.columns, termWinSize.rows)
                }
            }

            val runner = LocalTerminalDirectRunner.createTerminalRunner(project)
            val widget = ShellTerminalWidget(project, runner.settingsProvider, this)

            widget.createTerminalSession(connector)
            widget.start()

            widgets.add(widget)

            Thread.sleep(500)

            connector.write("clear; $claudePath\n")

            Thread {
                Thread.sleep(400)
                try {
                    if (connector.isConnected) {
                        val clearSequence = "\u001b[1A\u001b[2K\r"
                        connector.write(clearSequence)
                    }
                } catch (e: Exception) {
                    logger.warn("Could not clear command line: ${e.message}")
                }
            }.start()

            logger.info("Claude session started in tab: $tabName")
            return widget

        } catch (e: Exception) {
            logger.error("Failed to start Claude session", e)
            return null
        }
    }

    private fun findClaudeExecutable(): String? {
        val commonPaths = listOf(
            "/opt/homebrew/bin/claude",
            "/usr/local/bin/claude",
            "/usr/bin/claude",
            System.getProperty("user.home") + "/.local/bin/claude"
        )

        for (path in commonPaths) {
            if (File(path).exists() && File(path).canExecute()) {
                return path
            }
        }

        try {
            val process = ProcessBuilder("which", "claude")
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()

            if (process.exitValue() == 0 && output.isNotEmpty()) {
                return output
            }
        } catch (e: Exception) {
            logger.warn("Could not execute 'which claude': ${e.message}")
        }

        return null
    }

    private fun getShellPath(): String {
        val shell = System.getenv("SHELL") ?: "/bin/zsh"
        return if (File(shell).exists()) shell else "/bin/bash"
    }

    private fun getShellEnvironment(): MutableMap<String, String> {
        val shellPath = getShellPath()

        try {
            val process = ProcessBuilder(shellPath, "-i", "-l", "-c", "printenv")
                .redirectErrorStream(true)
                .start()

            val envOutput = process.inputStream.bufferedReader().readText()
            process.waitFor()

            val envMap = mutableMapOf<String, String>()
            envOutput.lines().forEach { line ->
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) {
                    envMap[parts[0]] = parts[1]
                }
            }

            if (!envMap.containsKey("HOME")) {
                envMap["HOME"] = System.getProperty("user.home")
            }
            if (!envMap.containsKey("USER")) {
                envMap["USER"] = System.getProperty("user.name")
            }

            envMap["TERM"] = "xterm-256color"
            envMap["HISTFILE"] = "/dev/null"
            envMap["HISTSIZE"] = "0"
            envMap["SAVEHIST"] = "0"
            envMap["HISTFILESIZE"] = "0"

            return envMap
        } catch (e: Exception) {
            logger.warn("Could not get shell environment, using System.getenv(): ${e.message}")
            val envMap = System.getenv().toMutableMap()
            envMap["TERM"] = "xterm-256color"
            envMap["HISTFILE"] = "/dev/null"
            envMap["HISTSIZE"] = "0"
            envMap["SAVEHIST"] = "0"
            envMap["HISTFILESIZE"] = "0"
            return envMap
        }
    }

    override fun dispose() {
        widgets.forEach { widget ->
            try {
                widget.close()
            } catch (e: Exception) {
                logger.warn("Error closing widget", e)
            }
        }
        widgets.clear()
    }
}
