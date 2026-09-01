package com.inspiredandroid.kai.data

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/**
 * Folder attachments for chat / collaboration / war.
 *
 * Collaboration and war fan out to many models. Recursively inlining a project tree
 * once per model freezes the UI and blows past provider size limits. Directories are
 * therefore described by absolute path plus a short listing; models read files via
 * `execute_shell_command`.
 */
internal object FolderAttachments {

    private val skipDirectoryNames = setOf(
        ".git",
        "node_modules",
        ".gradle",
        "build",
        "dist",
        "out",
        ".idea",
        "kotlin-js-store",
        "__pycache__",
        ".dart_tool",
        "target",
        ".cursor",
        ".kotlin",
    )

    const val MAX_INLINE_FILES = 40
    const val MAX_INLINE_BYTES = 400_000
    private const val MAX_LISTING_LINES = 200
    private const val MAX_LISTING_DEPTH = 3

    fun isSkippedDirectoryName(name: String): Boolean {
        val n = name.lowercase()
        if (n in skipDirectoryNames) return true
        if (n.startsWith("kai-") && (n.contains("windows") || n.contains("linux") || n.contains("macos"))) {
            return true
        }
        return false
    }

    fun withoutDirectories(files: List<PlatformFile>): List<PlatformFile> =
        files.filter { !it.isDirectory() }

    suspend fun promptPrefix(files: List<PlatformFile>): String {
        val dirs = files.filter { runCatching { it.isDirectory() }.getOrDefault(false) }
        if (dirs.isEmpty()) return ""
        val body = buildString {
            for (dir in dirs) {
                val path = runCatching { dir.path }.getOrElse { dir.name }
                append("【附加文件夹】\n")
                append("路径：$path\n")
                append("请使用 execute_shell_command 在该路径下查看和读取文件。")
                append("Windows 使用内置 PowerShell 7（pwsh），可用 working_dir 指向该路径。")
                append("不要一次性把整棵目录读进内存。跳过 .git、node_modules、build 等目录。\n")
                append("目录摘要：\n")
                var lines = 0
                suspend fun walk(current: PlatformFile, rel: String, depth: Int) {
                    coroutineContext.ensureActive()
                    if (lines >= MAX_LISTING_LINES) return
                    val children = try {
                        current.list()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        emptyList()
                    }
                    for (child in children.sortedBy { it.name.lowercase() }) {
                        coroutineContext.ensureActive()
                        if (lines >= MAX_LISTING_LINES) return
                        val childRel = if (rel.isEmpty()) child.name else "$rel/${child.name}"
                        val isDir = try {
                            child.isDirectory()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            false
                        }
                        if (isDir) {
                            if (isSkippedDirectoryName(child.name)) continue
                            append("  $childRel/\n")
                            lines++
                            if (depth < MAX_LISTING_DEPTH) walk(child, childRel, depth + 1)
                        } else {
                            append("  $childRel\n")
                            lines++
                        }
                    }
                }
                walk(dir, "", 0)
                if (lines >= MAX_LISTING_LINES) append("  …（已截断）\n")
                append('\n')
            }
        }
        return body
    }
}
