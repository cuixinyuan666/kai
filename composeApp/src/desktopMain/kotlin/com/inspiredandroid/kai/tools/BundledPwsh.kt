package com.inspiredandroid.kai.tools

import com.inspiredandroid.kai.getAppFilesDirectory
import java.io.File

/**
 * Resolves the PowerShell used by desktop shell tools on Windows.
 *
 * Packaged Windows builds ship PowerShell 7 (pwsh) under Compose app resources
 * (`app/resources/pwsh/pwsh.exe`). `:run` uses the same copy after
 * `downloadPwsh7`. PATH `pwsh` and Windows PowerShell 5.1 are fallbacks only.
 */
internal object BundledPwsh {
    data class Resolved(val executable: String, val source: String, val isPowerShell7: Boolean)

    fun resolve(): Resolved = find()

    fun processBuilder(command: String): ProcessBuilder {
        val resolved = resolve()
        return ProcessBuilder(
            resolved.executable,
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-Command",
            command,
        )
    }

    private fun find(): Resolved {
        bundledPwsh()?.let { return Resolved(it.absolutePath, "bundled", true) }
        cachedPwsh()?.let { return Resolved(it.absolutePath, "appdata-cache", true) }
        findOnPath("pwsh")?.let { return Resolved(it.absolutePath, "path-pwsh", true) }
        findOnPath("powershell")?.let {
            return Resolved(it.absolutePath, "windows-powershell-5", false)
        }
        return Resolved("powershell", "powershell-unresolved", false)
    }

    private fun bundledPwsh(): File? {
        val root = System.getProperty("compose.application.resources.dir") ?: return null
        val exe = File(root, "pwsh/pwsh.exe")
        return exe.takeIf { it.isFile }
    }

    private fun cachedPwsh(): File? {
        val exe = File(getAppFilesDirectory(), "pwsh/pwsh.exe")
        return exe.takeIf { it.isFile }
    }

    private fun findOnPath(name: String): File? {
        val path = System.getenv("PATH") ?: return null
        val names = listOf(name, "$name.exe")
        for (dir in path.split(File.pathSeparator)) {
            if (dir.isBlank()) continue
            for (n in names) {
                val file = File(dir, n)
                if (file.isFile) return file
            }
        }
        return null
    }
}
