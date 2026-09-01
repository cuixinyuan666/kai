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
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
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
    private var activeLanguage: String = "zh"
    private val pcmLock = Any()
    private var pcmBuffer = ByteArrayOutputStream()

    override val isSupported: Boolean = currentPlatform is Platform.Desktop.Windows
    override val isListening: Boolean get() = listening.get()

    override suspend fun startListening(languageTag: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isSupported) return@withContext Result.failure(IllegalStateException("Speech not supported"))
        if (listening.get()) return@withContext Result.success(Unit)
        val spec = specFor(languageTag)
        activeLanguage = if (languageTag.startsWith("zh")) "zh" else "en"
        val modelDir = resolveModelDirectory(spec)
        val modelPath = modelDir?.absolutePath
            ?: return@withContext Result.failure(IllegalStateException("无法加载语音识别模型"))
        runCatching {
            releaseRecognizer()
            synchronized(pcmLock) {
                pcmBuffer = ByteArrayOutputStream()
            }
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
        val rawJson = if (rec != null) rec.finalResult.orEmpty() else ""
        val raw = parseVoskText(rawJson)
        var language = activeLanguage
        var text = SpeechLanguage.normalizeTranscript(raw, language)
        val pcm = synchronized(pcmLock) { pcmBuffer.toByteArray() }
        if (SpeechLanguage.looksMismatched(text, language) && pcm.isNotEmpty()) {
            val other = if (language == "zh") "en" else "zh"
            val retry = recognizeBuffer(pcm, other)
            if (retry != null && !SpeechLanguage.looksMismatched(retry, other)) {
                language = other
                activeLanguage = other
                text = retry
            }
        }
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
                    synchronized(pcmLock) {
                        pcmBuffer.write(buffer, 0, read)
                    }
                }
            }
        } finally {
            line.stop()
            line.close()
        }
    }

    private fun recognizeBuffer(pcm: ByteArray, languageTag: String): String? {
        val spec = specFor(languageTag)
        val path = resolveModelDirectory(spec)?.absolutePath ?: return null
        var retryModel: Model? = null
        var retryRec: Recognizer? = null
        return try {
            retryModel = Model(path)
            retryRec = Recognizer(retryModel, SAMPLE_RATE)
            var offset = 0
            val chunk = 4096
            while (offset < pcm.size) {
                val n = minOf(chunk, pcm.size - offset)
                val slice = pcm.copyOfRange(offset, offset + n)
                retryRec.acceptWaveForm(slice, n)
                offset += n
            }
            SpeechLanguage.normalizeTranscript(parseVoskText(retryRec.finalResult.orEmpty()), languageTag)
        } catch (_: Exception) {
            null
        } finally {
            retryRec?.close()
            retryModel?.close()
        }
    }

    private fun releaseRecognizer() {
        recognizer?.close()
        recognizer = null
        model?.close()
        model = null
    }

    private fun resolveModelDirectory(spec: ModelSpec): File? {
        val userDir = System.getProperty("user.dir").orEmpty()
        val candidates = listOfNotNull(
            System.getProperty("compose.application.resources.dir")?.let { File(it, "vosk/${spec.folderName}") },
            File(getAppFilesDirectory(), "vosk/${spec.folderName}"),
            File(userDir, "composeApp/appResources/common/vosk/${spec.folderName}"),
            File(userDir, "appResources/common/vosk/${spec.folderName}"),
        )
        return candidates.firstOrNull { isModelReady(it) }
    }

    private fun isModelReady(dir: File): Boolean =
        dir.isDirectory && (dir.resolve("am/final.mdl").exists() || dir.resolve("conf/model.conf").exists())

    private fun parseVoskText(json: String): String {
        val direct = textField(json)
        val recovered = runCatching {
            textField(String(json.toByteArray(charset("GBK")), StandardCharsets.UTF_8))
        }.getOrDefault("")
        return pickReadable(direct, recovered)
    }

    private fun textField(json: String): String = runCatching {
        Json.parseToJsonElement(json).jsonObject["text"]?.jsonPrimitive?.content?.trim().orEmpty()
    }.getOrDefault("")

    private fun pickReadable(direct: String, recovered: String): String {
        if (direct.isBlank()) return recovered
        if (recovered.isBlank()) return direct
        val directBad = direct.count { it == '\uFFFD' }
        val recoveredBad = recovered.count { it == '\uFFFD' }
        if (recoveredBad < directBad) return recovered
        val directCjk = direct.count { it in '\u4e00'..'\u9fff' }
        val recoveredCjk = recovered.count { it in '\u4e00'..'\u9fff' }
        if (recoveredCjk > directCjk + 1) return recovered
        return direct
    }

    private fun specFor(languageTag: String): ModelSpec =
        if (languageTag.startsWith("zh")) MODEL_CN else MODEL_EN

    private data class ModelSpec(val folderName: String)

    private companion object {
        const val SAMPLE_RATE = 16000f
        val MODEL_EN = ModelSpec(folderName = "vosk-model-small-en-us-0.15")
        val MODEL_CN = ModelSpec(folderName = "vosk-model-small-cn-0.22")
    }
}

actual fun createSpeechToText(): SpeechToText? =
    if (currentPlatform is Platform.Desktop.Windows) VoskSpeechToText() else null
