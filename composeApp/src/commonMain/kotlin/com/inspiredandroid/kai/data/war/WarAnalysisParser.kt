package com.inspiredandroid.kai.data.war

import com.inspiredandroid.kai.data.SharedJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class WarAnalysisResult(
    val commonPoints: List<String>,
    val aspects: List<WarAspect>,
)

object WarAnalysisParser {

    fun parseAnalysis(raw: String): WarAnalysisResult? {
        val jsonText = extractJsonObject(raw) ?: return null
        val root = runCatching { SharedJson.parseToJsonElement(jsonText).jsonObject }.getOrNull() ?: return null

        val commonPoints = root["commonPoints"]?.jsonArray?.mapNotNull { el ->
            el.jsonPrimitive.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
        } ?: emptyList()

        val aspects = root["aspects"]?.jsonArray?.mapNotNull { el ->
            parseAspect(el.jsonObject)
        }?.take(WarPromptBuilder.MAX_ASPECTS) ?: emptyList()

        return WarAnalysisResult(commonPoints = commonPoints, aspects = aspects)
    }

    fun parseVotes(
        raw: String,
        aspects: List<WarAspect>,
        modelKey: String,
        modelLabel: String,
    ): List<WarModelVote> {
        val aspectIds = aspects.associateBy { it.id }
        val parsed = mutableMapOf<String, WarModelVote>()

        val jsonText = extractJsonObject(raw)
        if (jsonText != null) {
            val root = runCatching { SharedJson.parseToJsonElement(jsonText).jsonObject }.getOrNull()
            root?.get("votes")?.jsonArray?.forEach { el ->
                val obj = el.jsonObject
                val aspectId = obj["aspectId"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                if (aspectId.isBlank() || aspectId !in aspectIds) return@forEach
                val agree = parseAgreeValue(obj["agree"])
                val reason = obj["reason"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                parsed[aspectId] = WarModelVote(
                    modelKey = modelKey,
                    modelLabel = modelLabel,
                    choice = when (agree) {
                        true -> WarVoteChoice.AGREE.name
                        false -> WarVoteChoice.DISAGREE.name
                        null -> WarVoteChoice.ABSTAIN.name
                    },
                    reason = reason,
                    aspectId = aspectId,
                )
            }
        }

        return aspects.map { aspect ->
            parsed[aspect.id] ?: WarModelVote(
                modelKey = modelKey,
                modelLabel = modelLabel,
                choice = WarVoteChoice.ABSTAIN.name,
                reason = "",
                aspectId = aspect.id,
            )
        }
    }

    fun aggregateAspectResults(
        aspects: List<WarAspect>,
        allVotes: List<List<WarModelVote>>,
    ): List<WarAspectResult> {
        val flat = allVotes.flatten()
        val useIds = flat.any { it.aspectId.isNotBlank() }
        return aspects.mapIndexed { index, aspect ->
            val votes = if (useIds) {
                flat.filter { it.aspectId == aspect.id }
            } else {
                allVotes.mapNotNull { voteList -> voteList.getOrNull(index) }
            }
            WarAspectResult(aspect = aspect, votes = votes)
        }
    }

    private fun parseAspect(obj: JsonObject): WarAspect? {
        val id = obj["id"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val title = obj["title"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val description = obj["description"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        if (id.isBlank() || title.isBlank()) return null
        val proposedBy = obj["proposedBy"]?.jsonArray?.mapNotNull { el ->
            el.jsonPrimitive.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
        } ?: emptyList()
        return WarAspect(
            id = id,
            title = title,
            description = description,
            proposedByLabels = proposedBy,
        )
    }

    private fun parseAgreeValue(element: kotlinx.serialization.json.JsonElement?): Boolean? {
        if (element == null) return null
        return when (element) {
            is JsonPrimitive -> {
                element.booleanOrNull
                    ?: when (element.contentOrNull?.trim()?.lowercase()) {
                        "true", "yes", "同意", "是", "支持" -> true
                        "false", "no", "不同意", "否", "反对" -> false
                        else -> null
                    }
            }
            else -> null
        }
    }

    private fun extractJsonObject(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) return trimmed
        val fenceRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
        fenceRegex.find(trimmed)?.groupValues?.getOrNull(1)?.trim()?.let { inner ->
            if (inner.startsWith("{")) return inner
        }
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1)
        }
        return null
    }
}
