package com.inspiredandroid.kai.speech

import com.inspiredandroid.kai.Platform
import com.inspiredandroid.kai.currentPlatform
import com.inspiredandroid.kai.getAppFilesDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

/**
 * 离线语音转文字（Vosk）：中英文小型模型随 Windows 安装包内置（app/resources/vosk/），
 * 亦兼容此前下载到 ~/.kai/vosk/ 的缓存。模型来源：https://alphacephei.com/vosk/models （Apache 2.0）
 */
internal class VoskSpeechToText : SpeechToText {
    private val listening = AtomicBoolean(false)
    private var captureThread: Thread? = null
    private var recognizer: Recognizer? = null
    private var model: Model? = null

    override val isSupported: Boolean = currentPlatform is Platform.Desktop.Windows
    override val isListening: Boolean get() = listening.get()

    override suspend fun startListening(languageTag: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isSupported) return@withContext Result.failure(IllegalStateException("Speech not supported"))
        if (listening.get()) return@withContext Result.success(Unit)
        val spec = if (languageTag.startsWith("zh")) MODEL_CN else MODEL_EN
        val modelPath = resolveModelDirectory(spec)?.absolutePath
            ?: return@withContext Result.failure(IllegalStateException("无法加载语音识别模型"))
        runCatching {
            releaseRecognizer()
            model = Model(modelPath)
            recognizer = Recognizer(model, SAMPLE_RATE)
            listening.set(true)
            captureThread = Thread(::captureFromMic, "vosk-mic-capture").apply { start() }
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { e ->
                listening.set(false)
                releaseRecognizer()
                Result.failure(e)
            },
        )
    }

    override suspend fun stopListening(): Result<String> = withContext(Dispatchers.IO) {
        if (!listening.get()) return@withContext Result.failure(IllegalStateException("Not listening"))
        listening.set(false)
        captureThread?.join(20_000)
        captureThread = null
        val rec = recognizer
        val text = if (rec != null) parseVoskText(rec.finalResult) else ""
        releaseRecognizer()
        Result.success(text)
    }

    override fun cancel() {
        listening.set(false)
        captureThread?.join(2_000)
        captureThread = null
        releaseRecognizer()
    }

    private fun captureFromMic() {
        val rec = recognizer ?: return
        val format = AudioFormat(SAMPLE_RATE, 16, 1, true, false)
        val info = DataLine.Info(TargetDataLine::class.java, format)
        val line = AudioSystem.getLine(info) as TargetDataLine
        line.open(format)
        line.start()
        val buffer = ByteArray(4096)
        try {
            while (listening.get()) {
                val read = line.read(buffer, 0, buffer.size)
                if (read > 0) {
                    rec.acceptWaveForm(buffer, read)
                }
            }
        } finally {
            line.stop()
            line.close()
        }
    }

    private fun releaseRecognizer() {
        recognizer?.close()
        recognizer = null
        model?.close()
        model = null
    }

    private fun resolveModelDirectory(spec: ModelSpec): File? {
        val bundledRoot = System.getProperty("compose.application.resources.dir")
        if (bundledRoot != null) {
            val bundled = File(bundledRoot, "vosk/${spec.folderName}")
            if (isModelReady(bundled)) return bundled
        }
        val cached = File(getAppFilesDirectory(), "vosk/${spec.folderName}")
        if (isModelReady(cached)) return cached
        return null
    }

    private fun isModelReady(dir: File): Boolean =
        dir.isDirectory && (dir.resolve("am/final.mdl").exists() || dir.resolve("conf/model.conf").exists())

    private fun parseVoskText(json: String): String = runCatching {
        Json.parseToJsonElement(json).jsonObject["text"]?.jsonPrimitive?.content?.trim() ?: ""
    }.getOrDefault("")

    private data class ModelSpec(val folderName: String)

    private companion object {
        const val SAMPLE_RATE = 16000f

        val MODEL_EN = ModelSpec(folderName = "vosk-model-small-en-us-0.15")

        val MODEL_CN = ModelSpec(folderName = "vosk-model-small-cn-0.22")
    }
}

actual fun createSpeechToText(): SpeechToText? =
    if (currentPlatform is Platform.Desktop.Windows) VoskSpeechToText() else null
