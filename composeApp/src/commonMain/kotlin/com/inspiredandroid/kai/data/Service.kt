package com.inspiredandroid.kai.data

import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.ic_service_aihorde
import kai.composeapp.generated.resources.ic_service_aihubmix
import kai.composeapp.generated.resources.ic_service_ainative
import kai.composeapp.generated.resources.ic_service_aion
import kai.composeapp.generated.resources.ic_service_anthropic
import kai.composeapp.generated.resources.ic_service_atlascloud
import kai.composeapp.generated.resources.ic_service_bazaarlink
import kai.composeapp.generated.resources.ic_service_cerebras
import kai.composeapp.generated.resources.ic_service_cloudflare
import kai.composeapp.generated.resources.ic_service_cohere
import kai.composeapp.generated.resources.ic_service_deepinfra
import kai.composeapp.generated.resources.ic_service_deepseek
import kai.composeapp.generated.resources.ic_service_fireworksai
import kai.composeapp.generated.resources.ic_service_free_expert
import kai.composeapp.generated.resources.ic_service_free_fast
import kai.composeapp.generated.resources.ic_service_gemini
import kai.composeapp.generated.resources.ic_service_github
import kai.composeapp.generated.resources.ic_service_groqcloud
import kai.composeapp.generated.resources.ic_service_huggingface
import kai.composeapp.generated.resources.ic_service_kilo
import kai.composeapp.generated.resources.ic_service_litert
import kai.composeapp.generated.resources.ic_service_llm7
import kai.composeapp.generated.resources.ic_service_longcat
import kai.composeapp.generated.resources.ic_service_minimax
import kai.composeapp.generated.resources.ic_service_mistral
import kai.composeapp.generated.resources.ic_service_moonshot
import kai.composeapp.generated.resources.ic_service_nara
import kai.composeapp.generated.resources.ic_service_navy
import kai.composeapp.generated.resources.ic_service_nvidia
import kai.composeapp.generated.resources.ic_service_ollamacloud
import kai.composeapp.generated.resources.ic_service_openai
import kai.composeapp.generated.resources.ic_service_openai_compatible
import kai.composeapp.generated.resources.ic_service_agnes
import kai.composeapp.generated.resources.ic_service_opencode
import kai.composeapp.generated.resources.ic_service_openrouter
import kai.composeapp.generated.resources.ic_service_ovh
import kai.composeapp.generated.resources.ic_service_perplexity
import kai.composeapp.generated.resources.ic_service_pollinations
import kai.composeapp.generated.resources.ic_service_publicai
import kai.composeapp.generated.resources.ic_service_reka
import kai.composeapp.generated.resources.ic_service_requesty
import kai.composeapp.generated.resources.ic_service_routeway
import kai.composeapp.generated.resources.ic_service_sealion
import kai.composeapp.generated.resources.ic_service_together
import kai.composeapp.generated.resources.ic_service_venice
import kai.composeapp.generated.resources.ic_service_xai
import kai.composeapp.generated.resources.ic_service_zai
import kai.composeapp.generated.resources.ic_service_zhipu
import kai.composeapp.generated.resources.service_free_expert
import kai.composeapp.generated.resources.service_free_fast
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

enum class FreeMode(val modelId: String, val nameRes: StringResource, val icon: DrawableResource) {
    FAST("fast", Res.string.service_free_fast, Res.drawable.ic_service_free_fast),
    EXPERT("expert", Res.string.service_free_expert, Res.drawable.ic_service_free_expert),
    ;

    val instanceId: String get() = "free-$modelId"
}

data class ModelDefinition(
    val id: String,
    val subtitle: String,
    val descriptionRes: StringResource? = null,
)

/**
 * How a service handles a `reasoning_content` field on outgoing assistant messages.
 *
 * Default is [NONE] so any new provider is safe by default — Groq and Cerebras
 * return HTTP 400 when they see this field, so opt-in is the correct posture.
 * See `docs/features/reasoning.md` for the authoritative per-provider matrix.
 */
enum class ReasoningRequestMode {
    /** Strip the field before sending. Safe default. */
    NONE,

    /**
     * Echo `reasoning_content` back on assistant turns that previously produced
     * `tool_calls`. Truly required by Z.AI Coding Plan, OpenCode Zen (DeepSeek
     * route), and Moonshot kimi-k2.6 with `thinking.keep="all"`. Accepted as a
     * documented field by Fireworks, Z.AI standard, and OpenRouter (as an alias
     * for `reasoning`). Tolerated as an unknown field by LongCat, Venice, MiniMax.
     *
     * See `docs/features/reasoning.md` for the authoritative per-provider matrix
     * and known fidelity gaps (`reasoning_details`, `<think>`-in-content, paired
     * flags like `clear_thinking` and `reasoning_history`).
     */
    REASONING_CONTENT,
}

sealed class Service(
    val id: String,
    val displayName: String,
    val icon: DrawableResource,
    val requiresApiKey: Boolean,
    val supportsOptionalApiKey: Boolean = false,
    val anonymousKey: String? = null,
    val defaultModel: String?,
    val settingsKeyPrefix: String,
    val defaultModels: List<ModelDefinition> = emptyList(),
    val chatUrl: String = "",
    val modelsUrl: String? = null,
    val modelsResponseIsArray: Boolean = false,
    val filterActiveStrictly: Boolean = false,
    val filterByModelType: Boolean = false,
    val sortModelsById: Boolean = false,
    val apiKeyUrl: String? = null,
    val apiKeyUrlDisplay: String? = null,
    val isOnDevice: Boolean = false,
    val supportsPdf: Boolean = false,
    val supportsImages: Boolean = true,
    val reasoningRequestMode: ReasoningRequestMode = ReasoningRequestMode.NONE,
    val customHeaders: Map<String, String> = emptyMap(),
) {
    data object Free : Service(
        id = "free",
        displayName = "Free",
        icon = Res.drawable.ic_service_free_fast,
        requiresApiKey = false,
        defaultModel = null,
        settingsKeyPrefix = "",
        chatUrl = "https://api.kai9000.com/chat/completions",
        modelsUrl = null,
        // The kai9000 proxy fans out to a Mistral → Groq → OpenRouter chain. The Groq
        // fallback uses text-only models (gpt-oss-20b/120b) that reject content-parts
        // payloads, so images can't be promised reliably on this path.
        supportsImages = false,
    )

    data object AtlasCloud : Service(
        id = "atlascloud",
        displayName = "Atlas Cloud",
        icon = Res.drawable.ic_service_atlascloud,
        requiresApiKey = true,
        defaultModel = null,
        settingsKeyPrefix = "atlascloud",
        chatUrl = "https://api.atlascloud.ai/v1/chat/completions",
        modelsUrl = "https://api.atlascloud.ai/v1/models",
        apiKeyUrl = "https://www.atlascloud.ai/console/api-keys?utm_source=github&utm_medium=link&utm_campaign=Kai",
        apiKeyUrlDisplay = "atlascloud.ai/console/api-keys",
    )

    data object Groq : Service(
        id = "groqcloud",
        displayName = "GroqCloud",
        icon = Res.drawable.ic_service_groqcloud,
        requiresApiKey = true,
        defaultModel = null,
        settingsKeyPrefix = "groq",
        defaultModels = emptyList(),
        chatUrl = "https://api.groq.com/openai/v1/chat/completions",
        modelsUrl = "https://api.groq.com/openai/v1/models",
        filterActiveStrictly = true,
        apiKeyUrl = "https://console.groq.com/keys",
        apiKeyUrlDisplay = "console.groq.com/keys",
    )

    data object XAI : Service(
        id = "xai",
        displayName = "xAI",
        icon = Res.drawable.ic_service_xai,
        requiresApiKey = true,
        defaultModel = null,
        settingsKeyPrefix = "xai",
        defaultModels = emptyList(),
        chatUrl = "https://api.x.ai/v1/chat/completions",
        modelsUrl = "https://api.x.ai/v1/models",
        apiKeyUrl = "https://console.x.ai",
        apiKeyUrlDisplay = "console.x.ai",
    )

    data object OpenRouter : Service(
        id = "openrouter",
        displayName = "OpenRouter",
        icon = Res.drawable.ic_service_openrouter,
        requiresApiKey = true,
        defaultModel = null,
        settingsKeyPrefix = "openrouter",
        defaultModels = emptyList(),
        chatUrl = "https://openrouter.ai/api/v1/chat/completions",
        modelsUrl = "https://openrouter.ai/api/v1/models",
        apiKeyUrl = "https://openrouter.ai/settings/keys",
        apiKeyUrlDisplay = "openrouter.ai/settings/keys",
        supportsPdf = true,
        reasoningRequestMode = ReasoningRequestMode.REASONING_CONTENT,
    )

    data object Nvidia : Service(
        id = "nvidia",
        displayName = "NVIDIA",
        icon = Res.drawable.ic_service_nvidia,
        requiresApiKey = true,
        defaultModel = null,
        settingsKeyPrefix = "nvidia",
        defaultModels = emptyList(),
        chatUrl = "https://integrate.api.nvidia.com/v1/chat/completions",
        modelsUrl = "https://integrate.api.nvidia.com/v1/models",
        sortModelsById = true,
        apiKeyUrl = "https://build.nvidia.com/settings/api-keys",
        apiKeyUrlDisplay = "build.nvidia.com/settings/api-keys",
    )

    data object Gemini : Service(
        id = "gemini",
        displayName = "Gemini",
        icon = Res.drawable.ic_service_gemini,
        requiresApiKey = true,
        defaultModel = null,
        settingsKeyPrefix = "gemini",
        chatUrl = "https://generativelanguage.googleapis.com/v1beta/models/",
        modelsUrl = null,
        defaultModels = emptyList(),
        apiKeyUrl = "https://aistudio.google.com/apikey",
        apiKeyUrlDisplay = "aistudio.google.com/apikey",
        supportsPdf = true,
    )

    data object Anthropic : Service(
        id = "anthropic",
        displayName = "Anthropic",
        icon = Res.drawable.ic_service_anthropic,
        requiresApiKey = true,
        defaultModel = null,
        settingsKeyPrefix = "anthropic",
        chatUrl = "https://api.anthropic.com/v1/messages",
        modelsUrl = "https://api.anthropic.com/v1/models",
        apiKeyUrl = "https://console.anthropic.com/settings/keys",
        apiKeyUrlDisplay = "console.anthropic.com/settings/keys",
        supportsPdf = true,
    )

    data object OpenAI : Service(
        id = "openai",
        displayName = "OpenAI",
        icon = Res.drawable.ic_service_openai,
        requiresApiKey = true,
        defaultModel = null,
        settingsKeyPrefix = "openai",
        chatUrl = "https://api.openai.com/v1/chat/completions",
        modelsUrl = "https://api.openai.com/v1/models",
        apiKeyUrl = "https://platform.openai.com/api-keys",
        apiKeyUrlDisplay = "platform.openai.com/api-keys",
        supportsPdf = true,
    )

    data object DeepSeek : Service(
        id = "deepseek",
        displayName = "DeepSeek",
        icon = Res.drawable.ic_service_deepseek,
        requiresApiKey = true,
        defaultModel = null,
        settingsKeyPrefix = "deepseek",
        chatUrl = "https://api.deepseek.com/chat/completions",
        modelsUrl = "https://api.deepseek.com/models",
        apiKeyUrl = "https://platform.deepseek.com/api_keys",
        apiKeyUrlDisplay = "platform.deepseek.com/api_keys",
        reasoningRequestMode = ReasoningRequestMode.REASONING_CONTENT,
    )

    data object Mistral : Service(
        id = "mistral",
        displayName = "Mistral",
        icon = Res.drawable.ic_service_mistral,
        requiresApiKey = true,
        defaultModel = null,
        settingsKeyPrefix = "mistral",
        chatUrl = "https://api.mistral.ai/v1/chat/completions",
        modelsUrl = "https://api.mistral.ai/v1/models",
        apiKeyUrl = "https://console.mistral.ai/api-keys",
        apiKeyUrlDisplay = "console.mistral.ai/api-keys",
    )

    data object Cerebras : Service(
        id = "cerebras",
        displayName = "Cerebras",
        icon = Res.drawable.ic_service_cerebras,
        requiresApiKey = true,
        defaultModel = null,
        settingsKeyPrefix = "cerebras",
        chatUrl = "https://api.cerebras.ai/v1/chat/completions",
        modelsUrl = "https://api.cerebras.ai/v1/models",
        apiKeyUrl = "https://cloud.cerebras.ai/",
        apiKeyUrlDisplay = "cloud.cerebras.ai",
    )

    data object OllamaCloud : Service(
        id = "ollamacloud",
        displayName = "Ollama Cloud",
        icon = Res.drawable.ic_service_ollamacloud,
        requiresApiKey = true,
        defaultModel = null,
        settingsKeyPrefix = "ollamacloud",
        chatUrl = "https://ollama.com/v1/chat/completions",
        modelsUrl = "https://ollama.com/v1/models",
        apiKeyUrl = "https://ollama.com/settings/keys",
        apiKeyUrlDisplay = "ollama.com/settings/keys",
    )

    data object LongCat : Service(
        id = "longcat",
        displayName = "LongCat",
        icon = Res.drawable.ic_service_longcat,
        requiresApiKey = true,
        defaultModel = "LongCat-Flash-Lite",
        settingsKeyPrefix = "longcat",
        chatUrl = "https://api.longcat.chat/openai/v1/chat/completions",
        modelsUrl = "https://api.longcat.chat/openai/v1/models",
        defaultModels = listOf(
            ModelDefinition(id = "LongCat-Flash-Chat", subtitle = "LongCat"),
            ModelDefinition(id = "LongCat-Flash-Thinking", subtitle = "LongCat"),
            ModelDefinition(id = "LongCat-Flash-Thinking-2601", subtitle = "LongCat"),
            ModelDefinition(id = "LongCat-Flash-Lite", subtitle = "LongCat"),
            ModelDefinition(id = "LongCat-Flash-Omni-2603", subtitle = "LongCat"),
        ),
        apiKeyUrl = "https://longcat.chat/platform",
        apiKeyUrlDisplay = "longcat.chat/platform",
        reasoningRequestMode = ReasoningRequestMode.REASONING_CONTENT,
    )

    data object Together : Service(
        id = "together",
        displayName = "Together AI",
        icon = Res.drawable.ic_service_together,
        requiresApiKey = true,
        defaultModel = null,
        settingsKeyPrefix = "together",
        chatUrl = "https://api.together.xyz/v1/chat/completions",
        modelsUrl = "https://api.together.xyz/v1/models",
        modelsResponseIsArray = true,
        filterByModelType = true,
        apiKeyUrl = "https://api.together.ai/settings/api-keys",
        apiKeyUrlDisplay = "api.together.ai/settings/api-keys",
    )

    data object HuggingFace : Service(
        id = "huggingface",
        displayName = "Hugging Face",
        icon = Res.drawable.ic_service_huggingface,
        requiresApiKey = true,
        defaultModel = null,
        settingsKeyPrefix = "huggingface",
        chatUrl = "https://router.huggingface.co/v1/chat/completions",
        modelsUrl = "https://router.huggingface.co/v1/models",
        apiKeyUrl = "https://huggingface.co/settings/tokens",
        apiKeyUrlDisplay = "huggingface.co/settings/tokens",
    )

    data object Venice : Service(
        id = "venice",
        displayName = "Venice AI",
        icon = Res.drawable.ic_service_venice,
        requiresApiKey = true,
        defaultModel = null,
        settingsKeyPrefix = "venice",
        chatUrl = "https://api.venice.ai/api/v1/chat/completions",
        modelsUrl = "https://api.venice.ai/api/v1/models",
        apiKeyUrl = "https://venice.ai/settings/api?ref=DsZFKZ",
        apiKeyUrlDisplay = "venice.ai/settings/api",
        reasoningRequestMode = ReasoningRequestMode.REASONING_CONTENT,
    )

    data object Moonshot : Service(
        id = "moonshot",
        displayName = "Moonshot AI",
        icon = Res.drawable.ic_service_moonshot,
        requiresApiKey = true,
        defaultModel = null,
        settingsKeyPrefix = "moonshot",
        chatUrl = "https://api.moonshot.cn/v1/chat/completions",
        modelsUrl = "https://api.moonshot.cn/v1/models",
        apiKeyUrl = "https://platform.moonshot.cn/console/api-keys",
        apiKeyUrlDisplay = "platform.moonshot.cn/console/api-keys",
        reasoningRequestMode = ReasoningRequestMode.REASONING_CONTENT,
    )

    data object Zai : Service(
        id = "zai",
        displayName = "Z.AI",
        icon = Res.drawable.ic_service_zai,
        requiresApiKey = true,
        defaultModel = null,
        settingsKeyPrefix = "zai",
        chatUrl = "https://api.z.ai/api/paas/v4/chat/completions",
        modelsUrl = "https://api.z.ai/api/paas/v4/models",
        apiKeyUrl = "https://z.ai/manage-apikey/apikey-list",
        apiKeyUrlDisplay = "z.ai/manage-apikey/apikey-list",
        reasoningRequestMode = ReasoningRequestMode.REASONING_CONTENT,
    )

    data object ZaiCodingPlan : Service(
        id = "zai-coding-plan",
        displayName = "Z.AI Coding Plan",
        icon = Res.drawable.ic_service_zai,
        requiresApiKey = true,
        defaultModel = null,
        settingsKeyPrefix = "zai-coding-plan",
        chatUrl = "https://api.z.ai/api/coding/paas/v4/chat/completions",
        modelsUrl = "https://api.z.ai/api/coding/paas/v4/models",
        apiKeyUrl = "https://z.ai/manage-apikey/apikey-list",
        apiKeyUrlDisplay = "z.ai/manage-apikey/apikey-list",
        reasoningRequestMode = ReasoningRequestMode.REASONING_CONTENT,
    )

    data object Minimax : Service(
        id = "minimax",
        displayName = "MiniMax",
        icon = Res.drawable.ic_service_minimax,
        requiresApiKey = true,
        defaultModel = null,
        settingsKeyPrefix = "minimax",
        chatUrl = "https://api.minimax.io/v1/chat/completions",
        modelsUrl = "https://api.minimax.io/v1/models",
        apiKeyUrl = "https://platform.minimax.io",
        apiKeyUrlDisplay = "platform.minimax.io",
        reasoningRequestMode = ReasoningRequestMode.REASONING_CONTENT,
    )

    data object AiHubMix : Service(
        id = "aihubmix",
        displayName = "AIHubMix",
        icon = Res.drawable.ic_service_aihubmix,
        requiresApiKey = true,
        defaultModel = null,
        settingsKeyPrefix = "aihubmix",
        chatUrl = "https://aihubmix.com/v1/chat/completions",
        modelsUrl = "https://aihubmix.com/v1/models",
        apiKeyUrl = "https://aihubmix.com/token",
        apiKeyUrlDisplay = "aihubmix.com/token",
    )

    data object DeepInfra : Service(
        id = "deepinfra",
        displayName = "Deep Infra",
        icon = Res.drawable.ic_service_deepinfra,
        requiresApiKey = true,
        defaultModel = null,
        settingsKeyPrefix = "deepinfra",
        chatUrl = "https://api.deepinfra.com/v1/openai/chat/completions",
        modelsUrl = "https://api.deepinfra.com/v1/openai/models",
        apiKeyUrl = "https://deepinfra.com/dash/api_keys",
        apiKeyUrlDisplay = "deepinfra.com/dash/api_keys",
    )

    data object FireworksAI : Service(
        id = "fireworksai",
        displayName = "Fireworks AI",
        icon = Res.drawable.ic_service_fireworksai,
        requiresApiKey = true,
        defaultModel = null,
        settingsKeyPrefix = "fireworksai",
        chatUrl = "https://api.fireworks.ai/inference/v1/chat/completions",
        modelsUrl = "https://api.fireworks.ai/inference/v1/models",
        apiKeyUrl = "https://app.fireworks.ai/settings/users/api-keys",
        apiKeyUrlDisplay = "app.fireworks.ai/settings/users/api-keys",
        reasoningRequestMode = ReasoningRequestMode.REASONING_CONTENT,
    )

    data object OpenCode : Service(
        id = "opencode",
        displayName = "opencode api",
        icon = Res.drawable.ic_service_opencode,
        requiresApiKey = true,
        defaultModel = null,
        settingsKeyPrefix = "opencode",
        chatUrl = "https://opencode.ai/zen/v1/chat/completions",
        modelsUrl = "https://opencode.ai/zen/v1/models",
        apiKeyUrl = "https://opencode.ai/docs/zen/",
        apiKeyUrlDisplay = "opencode.ai/docs/zen",
        reasoningRequestMode = ReasoningRequestMode.REASONING_CONTENT,
    )

    /**
     * OpenCode 终端（opencode-terminal）：与 [OpenCode] 共享同一 Zen API 网关，
     * 但作为独立服务呈现，以便在模型选择器中与 "opencode api" 区分。
     * 支持 thinking / plan / build 调节（见 OpenAICompatibleChatRequestDto 扩展参数）。
     */
    data object OpenCodeTerminal : Service(
        id = "opencode-terminal",
        displayName = "opencode terminal",
        icon = Res.drawable.ic_service_opencode,
        requiresApiKey = true,
        defaultModel = null,
        settingsKeyPrefix = "opencode_terminal",
        chatUrl = "https://opencode.ai/zen/v1/chat/completions",
        modelsUrl = "https://opencode.ai/zen/v1/models",
        apiKeyUrl = "https://opencode.ai/docs/zen/",
        apiKeyUrlDisplay = "opencode.ai/docs/zen",
        reasoningRequestMode = ReasoningRequestMode.REASONING_CONTENT,
    )

    data object PublicAI : Service(
        id = "publicai",
        displayName = "Public AI",
        icon = Res.drawable.ic_service_publicai,
        requiresApiKey = true,
        defaultModel = null,
        settingsKeyPrefix = "publicai",
        chatUrl = "https://api.publicai.co/v1/chat/completions",
        modelsUrl = "https://api.publicai.co/v1/models",
        apiKeyUrl = "https://platform.publicai.co",
        apiKeyUrlDisplay = "platform.publicai.co",
    )

    /**
     * Crowdsourced LLM compute via the [AI Horde OpenAI proxy](https://oai.aihorde.net/).
     * Model availability depends on online volunteer workers. Anonymous key `0000000000`
     * works at lowest priority; register for a personal key at [aihorde.net/register](https://aihorde.net/register).
     */
    data object AIHorde : Service(
        id = "aihorde",
        displayName = "AI Horde",
        icon = Res.drawable.ic_service_aihorde,
        requiresApiKey = false,
        supportsOptionalApiKey = true,
        anonymousKey = "0000000000",
        defaultModel = null,
        settingsKeyPrefix = "aihorde",
        chatUrl = "https://oai.aihorde.net/v1/chat/completions",
        modelsUrl = "https://oai.aihorde.net/v1/models",
        apiKeyUrl = "https://aihorde.net/register",
        apiKeyUrlDisplay = "aihorde.net/register",
    )

    // Sonar chat-completions path. No authenticated /models list for Sonar (public /v1/models
    // is Agent API only), so the picker uses defaultModels; key validation is special-cased.
    data object Perplexity : Service(
        id = "perplexity",
        displayName = "Perplexity",
        icon = Res.drawable.ic_service_perplexity,
        requiresApiKey = true,
        defaultModel = "sonar-pro",
        settingsKeyPrefix = "perplexity",
        chatUrl = "https://api.perplexity.ai/chat/completions",
        modelsUrl = null,
        defaultModels = listOf(
            ModelDefinition(id = "sonar", subtitle = "Perplexity"),
            ModelDefinition(id = "sonar-pro", subtitle = "Perplexity"),
            ModelDefinition(id = "sonar-reasoning-pro", subtitle = "Perplexity"),
            ModelDefinition(id = "sonar-deep-research", subtitle = "Perplexity"),
        ),
        apiKeyUrl = "https://console.perplexity.ai",
        apiKeyUrlDisplay = "console.perplexity.ai",
    )

    data object OpenAICompatible : Service(
        id = "openai-compatible",
        displayName = "OpenAI-Compatible API",
        icon = Res.drawable.ic_service_openai_compatible,
        requiresApiKey = false,
        supportsOptionalApiKey = true,
        defaultModel = null,
        settingsKeyPrefix = "openai-compatible",
        chatUrl = "/chat/completions",
        modelsUrl = "/models",
        sortModelsById = true,
    )

    /**
     * Agnes AI — OpenAI-compatible multimodal API (chat + vision).
     *
     * Agnes publishes no `/models` listing, so the model picker uses the curated
     * [defaultModels] below. Image generation (`agnes-image-*`) and video generation
     * (`agnes-video-v2.0`) are separate Agnes endpoints not exposed by this chat path;
     * reach them via the Agnes API directly or Kai's Linux Sandbox.
     */
    data object Agnes : Service(
        id = "agnes",
        displayName = "Agnes AI",
        icon = Res.drawable.ic_service_agnes,
        requiresApiKey = true,
        defaultModel = "agnes-2.0-flash",
        settingsKeyPrefix = "agnes",
        chatUrl = "https://apihub.agnes-ai.com/v1/chat/completions",
        modelsUrl = null,
        defaultModels = listOf(
            ModelDefinition(id = "agnes-2.0-flash", subtitle = "Agnes · 多模态对话/推理/看图"),
            ModelDefinition(id = "agnes-1.5-flash", subtitle = "Agnes · 轻量多模态"),
        ),
        apiKeyUrl = "https://apihub.agnes-ai.com",
        apiKeyUrlDisplay = "apihub.agnes-ai.com",
        supportsImages = true,
        // Agnes returns `reasoning_content` (thinking) and accepts it back on
        // multi-turn turns; mirror the handling used by other thinking providers.
        reasoningRequestMode = ReasoningRequestMode.REASONING_CONTENT,
    )

    data object AINative : Service(
        id = "ainative",
        displayName = "AINative Studio",
        icon = Res.drawable.ic_service_ainative,
        requiresApiKey = true,
        defaultModel = "llama-4-maverick",
        settingsKeyPrefix = "ainative",
        chatUrl = "https://api.ainative.studio/api/v1/chat/completions",
        modelsUrl = "https://api.ainative.studio/api/v1/models",
        defaultModels = listOf(
            ModelDefinition(id = "llama-4-maverick", subtitle = "Llama 4 Maverick"),
            ModelDefinition(id = "qwen3-14b", subtitle = "Qwen3 14B"),
            ModelDefinition(id = "qwen3-32b", subtitle = "Qwen3 32B"),
            ModelDefinition(id = "qwen3-8b", subtitle = "Qwen3 8B"),
        ),
        apiKeyUrl = "https://ainative.studio",
        apiKeyUrlDisplay = "ainative.studio",
        supportsImages = true,
    )

    data object Aion : Service(
        id = "aion",
        displayName = "Aion Labs",
        icon = Res.drawable.ic_service_aion,
        requiresApiKey = true,
        defaultModel = "aion-labs/aion-2.0",
        settingsKeyPrefix = "aion",
        chatUrl = "https://api.aionlabs.ai/v1/chat/completions",
        modelsUrl = "https://api.aionlabs.ai/v1/models",
        defaultModels = listOf(
            ModelDefinition(id = "aion-labs/aion-2.0", subtitle = "Aion 2.0"),
            ModelDefinition(id = "aion-labs/aion-2.5", subtitle = "Aion 2.5"),
            ModelDefinition(id = "aion-labs/aion-3.0", subtitle = "Aion 3.0"),
            ModelDefinition(id = "aion-labs/aion-3.0-mini", subtitle = "Aion 3.0 Mini"),
            ModelDefinition(id = "aion-labs/aion-rp-llama-3.1-8b", subtitle = "Aion-RP Llama 3.1 8B"),
        ),
        apiKeyUrl = "https://aionlabs.ai",
        apiKeyUrlDisplay = "aionlabs.ai",
        supportsImages = true,
    )

    data object BazaarLink : Service(
        id = "bazaarlink",
        displayName = "BazaarLink",
        icon = Res.drawable.ic_service_bazaarlink,
        requiresApiKey = true,
        defaultModel = "auto:free",
        settingsKeyPrefix = "bazaarlink",
        chatUrl = "https://bazaarlink.ai/api/v1/chat/completions",
        modelsUrl = "https://bazaarlink.ai/api/v1/models",
        defaultModels = listOf(
            ModelDefinition(id = "auto:free", subtitle = "BazaarLink Auto (free router)"),
        ),
        apiKeyUrl = "https://bazaarlink.ai",
        apiKeyUrlDisplay = "bazaarlink.ai",
        supportsImages = true,
    )

    data object Cloudflare : Service(
        id = "cloudflare",
        displayName = "Cloudflare Workers AI",
        icon = Res.drawable.ic_service_cloudflare,
        requiresApiKey = true,
        defaultModel = "@cf/meta/llama-3.3-70b-instruct-fp8-fast",
        settingsKeyPrefix = "cloudflare",
        chatUrl = "/chat/completions",
        modelsUrl = "/models",
        defaultModels = listOf(
            ModelDefinition(id = "@cf/aisingapore/gemma-sea-lion-v4-27b-it", subtitle = "Gemma SEA-LION v4 27B"),
            ModelDefinition(id = "@cf/deepseek-ai/deepseek-r1-distill-qwen-32b", subtitle = "DeepSeek R1 Distill Qwen 32B"),
            ModelDefinition(id = "@cf/google/gemma-4-26b-a4b-it", subtitle = "Gemma 4 26B-A4B it"),
            ModelDefinition(id = "@cf/ibm-granite/granite-4.0-h-micro", subtitle = "Granite 4.0 H Micro"),
            ModelDefinition(id = "@cf/meta/llama-3.1-8b-instruct-fp8", subtitle = "Llama 3.1 8B"),
            ModelDefinition(id = "@cf/meta/llama-3.2-11b-vision-instruct", subtitle = "Llama 3.2 11B Vision"),
            ModelDefinition(id = "@cf/meta/llama-3.2-3b-instruct", subtitle = "Llama 3.2 3B"),
            ModelDefinition(id = "@cf/meta/llama-3.3-70b-instruct-fp8-fast", subtitle = "Llama 3.3 70B fp8-fast"),
            ModelDefinition(id = "@cf/meta/llama-4-scout-17b-16e-instruct", subtitle = "Llama 4 Scout"),
            ModelDefinition(id = "@cf/meta/llama-guard-3-8b", subtitle = "Llama Guard 3 8B"),
            ModelDefinition(id = "@cf/mistralai/mistral-small-3.1-24b-instruct", subtitle = "Mistral Small 3.1 24B"),
            ModelDefinition(id = "@cf/nvidia/nemotron-3-120b-a12b", subtitle = "Nemotron 3 120B"),
            ModelDefinition(id = "@cf/openai/gpt-oss-120b", subtitle = "GPT-OSS 120B"),
            ModelDefinition(id = "@cf/openai/gpt-oss-20b", subtitle = "GPT-OSS 20B"),
            ModelDefinition(id = "@cf/qwen/qwen2.5-coder-32b-instruct", subtitle = "Qwen2.5 Coder 32B"),
            ModelDefinition(id = "@cf/qwen/qwen3-30b-a3b-fp8", subtitle = "Qwen3 30B-A3B fp8"),
            ModelDefinition(id = "@cf/qwen/qwq-32b", subtitle = "QwQ 32B"),
            ModelDefinition(id = "@cf/zai-org/glm-4.7-flash", subtitle = "GLM-4.7 Flash"),
        ),
        apiKeyUrl = "https://dash.cloudflare.com/profile/api-tokens",
        apiKeyUrlDisplay = "dash.cloudflare.com/profile/api-tokens",
        supportsImages = false,
    )

    data object Cohere : Service(
        id = "cohere",
        displayName = "Cohere",
        icon = Res.drawable.ic_service_cohere,
        requiresApiKey = true,
        defaultModel = "command-a-plus-05-2026",
        settingsKeyPrefix = "cohere",
        chatUrl = "https://api.cohere.ai/compatibility/v1/chat/completions",
        modelsUrl = "https://api.cohere.ai/compatibility/v1/models",
        defaultModels = listOf(
            ModelDefinition(id = "c4ai-aya-expanse-32b", subtitle = "Aya Expanse 32B"),
            ModelDefinition(id = "c4ai-aya-vision-32b", subtitle = "Aya Vision 32B"),
            ModelDefinition(id = "command-a-03-2025", subtitle = "Command-A (03-2025)"),
            ModelDefinition(id = "command-a-plus-05-2026", subtitle = "Command A+ (05-2026)"),
            ModelDefinition(id = "command-a-reasoning-08-2025", subtitle = "Command A Reasoning (08-2025)"),
            ModelDefinition(id = "command-a-translate-08-2025", subtitle = "Command A Translate (08-2025)"),
            ModelDefinition(id = "command-a-vision-07-2025", subtitle = "Command A Vision (07-2025)"),
            ModelDefinition(id = "command-r-08-2024", subtitle = "Command R (08-2024)"),
            ModelDefinition(id = "command-r-plus-08-2024", subtitle = "Command R+ (08-2024)"),
            ModelDefinition(id = "command-r7b-12-2024", subtitle = "Command R7B (12-2024)"),
            ModelDefinition(id = "north-mini-code-1-0", subtitle = "North Mini Code"),
        ),
        apiKeyUrl = "https://dashboard.cohere.com/api-keys",
        apiKeyUrlDisplay = "dashboard.cohere.com/api-keys",
        supportsImages = false,
    )

    data object Kilo : Service(
        id = "kilo",
        displayName = "Kilo Gateway",
        icon = Res.drawable.ic_service_kilo,
        requiresApiKey = false,
        supportsOptionalApiKey = true,
        defaultModel = "kilo-auto/free",
        settingsKeyPrefix = "kilo",
        chatUrl = "https://api.kilo.ai/api/gateway/v1/chat/completions",
        modelsUrl = "https://api.kilo.ai/api/gateway/models",
        defaultModels = listOf(
            ModelDefinition(id = "cohere/north-mini-code:free", subtitle = "North Mini Code"),
            ModelDefinition(id = "kilo-auto/free", subtitle = "Kilo Auto Free"),
            ModelDefinition(id = "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free", subtitle = "Nemotron 3 Nano Omni Reasoning"),
            ModelDefinition(id = "nvidia/nemotron-3-super-120b-a12b:free", subtitle = "Nemotron 3 Super 120B"),
            ModelDefinition(id = "nvidia/nemotron-3-ultra-550b-a55b:free", subtitle = "Nemotron 3 Ultra 550B"),
            ModelDefinition(id = "nvidia/nemotron-3.5-content-safety:free", subtitle = "Nemotron 3.5 Content Safety"),
            ModelDefinition(id = "openrouter/free", subtitle = "Free Router"),
            ModelDefinition(id = "poolside/laguna-xs-2.1:free", subtitle = "Poolside Laguna XS 2.1"),
            ModelDefinition(id = "stepfun/step-3.7-flash:free", subtitle = "StepFun Step 3.7 Flash"),
        ),
        apiKeyUrl = "https://kilo.ai",
        apiKeyUrlDisplay = "kilo.ai",
        supportsImages = true,
    )

    data object LLM7 : Service(
        id = "llm7",
        displayName = "LLM7",
        icon = Res.drawable.ic_service_llm7,
        requiresApiKey = true,
        defaultModel = "codestral-latest",
        settingsKeyPrefix = "llm7",
        chatUrl = "https://api.llm7.io/v1/chat/completions",
        modelsUrl = "https://api.llm7.io/v1/models",
        defaultModels = listOf(
            // Probe-confirmed free roster (freellmapi baseline V13+, 100 req/hr anonymous)
            ModelDefinition(id = "gpt-oss-20b", subtitle = "GPT-OSS 20B"),
            ModelDefinition(id = "meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo", subtitle = "Llama 3.1 8B Turbo"),
            ModelDefinition(id = "codestral-latest", subtitle = "Codestral"),
            ModelDefinition(id = "ministral-8b-2512", subtitle = "Ministral 8B"),
            ModelDefinition(id = "GLM-4.6V-Flash", subtitle = "GLM-4.6V Flash"),
        ),
        apiKeyUrl = "https://llm7.io",
        apiKeyUrlDisplay = "llm7.io",
        supportsImages = true,
    )

    data object NaraRouter : Service(
        id = "nara",
        displayName = "NaraRouter",
        icon = Res.drawable.ic_service_nara,
        requiresApiKey = true,
        defaultModel = "mistral-large",
        settingsKeyPrefix = "nara",
        chatUrl = "https://router.bynara.id/v1/chat/completions",
        modelsUrl = "https://router.bynara.id/v1/models",
        defaultModels = listOf(
            ModelDefinition(id = "mistral-large", subtitle = "Mistral Large 3"),
            ModelDefinition(id = "mistral-medium-3-5", subtitle = "Mistral Medium 3.5"),
        ),
        apiKeyUrl = "https://router.bynara.id",
        apiKeyUrlDisplay = "router.bynara.id",
        supportsImages = true,
    )

    data object NavyAI : Service(
        id = "navy",
        displayName = "NavyAI",
        icon = Res.drawable.ic_service_navy,
        requiresApiKey = true,
        defaultModel = "deepseek-v4-flash",
        settingsKeyPrefix = "navy",
        chatUrl = "https://api.navy/v1/chat/completions",
        modelsUrl = "https://api.navy/v1/models",
        defaultModels = listOf(
            ModelDefinition(id = "c4ai-aya-expanse-32b", subtitle = "C4ai Aya Expanse 32B"),
            ModelDefinition(id = "c4ai-aya-vision-32b", subtitle = "C4ai Aya Vision 32B"),
            ModelDefinition(id = "codestral-2508", subtitle = "Codestral 2508"),
            ModelDefinition(id = "codestral-latest", subtitle = "Codestral Latest"),
            ModelDefinition(id = "command-a", subtitle = "Command A"),
            ModelDefinition(id = "command-a-plus", subtitle = "Command A Plus"),
            ModelDefinition(id = "command-a-reasoning", subtitle = "Command A Reasoning"),
            ModelDefinition(id = "command-a-vision", subtitle = "Command A Vision"),
            ModelDefinition(id = "command-r", subtitle = "Command R"),
            ModelDefinition(id = "command-r-7b", subtitle = "Command R 7B"),
            ModelDefinition(id = "command-r-plus", subtitle = "Command R Plus"),
            ModelDefinition(id = "deepseek-chat", subtitle = "Deepseek Chat"),
            ModelDefinition(id = "deepseek-reasoner", subtitle = "Deepseek Reasoner"),
            ModelDefinition(id = "deepseek-v3.2", subtitle = "Deepseek V3.2"),
            ModelDefinition(id = "deepseek-v4-flash", subtitle = "Deepseek V4 Flash"),
            ModelDefinition(id = "deepseek-v4-pro", subtitle = "Deepseek V4 Pro"),
            ModelDefinition(id = "gemini-2.5-flash", subtitle = "Gemini 2.5 Flash"),
            ModelDefinition(id = "gemini-2.5-flash-image", subtitle = "Gemini 2.5 Flash Image"),
            ModelDefinition(id = "gemini-2.5-flash-lite", subtitle = "Gemini 2.5 Flash Lite"),
            ModelDefinition(id = "gemini-2.5-flash-thinking", subtitle = "Gemini 2.5 Flash Thinking"),
            ModelDefinition(id = "gemini-3-flash-preview", subtitle = "Gemini 3 Flash Preview"),
            ModelDefinition(id = "gemini-3-flash-preview-thinking", subtitle = "Gemini 3 Flash Preview Thinking"),
            ModelDefinition(id = "gemini-3.1-flash-lite", subtitle = "Gemini 3.1 Flash Lite"),
            ModelDefinition(id = "gemini-3.1-flash-lite-thinking", subtitle = "Gemini 3.1 Flash Lite Thinking"),
            ModelDefinition(id = "gemma-4-26b-a4b-it", subtitle = "Gemma 4 26B A4b IT"),
            ModelDefinition(id = "gemma-4-31b-it", subtitle = "Gemma 4 31B IT"),
            ModelDefinition(id = "glm-5.1", subtitle = "GLM 5.1"),
            ModelDefinition(id = "glm-5.2", subtitle = "GLM 5.2"),
            ModelDefinition(id = "gpt-3.5-turbo", subtitle = "GPT 3.5 Turbo"),
            ModelDefinition(id = "gpt-4.1", subtitle = "GPT 4.1"),
            ModelDefinition(id = "gpt-4.1-mini", subtitle = "GPT 4.1 Mini"),
            ModelDefinition(id = "gpt-4.1-nano", subtitle = "GPT 4.1 Nano"),
            ModelDefinition(id = "gpt-4o", subtitle = "GPT 4o"),
            ModelDefinition(id = "gpt-4o-mini", subtitle = "GPT 4o Mini"),
            ModelDefinition(id = "gpt-4o-mini-search-preview", subtitle = "GPT 4o Mini Search Preview"),
            ModelDefinition(id = "gpt-4o-search-preview", subtitle = "GPT 4o Search Preview"),
            ModelDefinition(id = "gpt-5", subtitle = "GPT 5"),
            ModelDefinition(id = "gpt-5-mini", subtitle = "GPT 5 Mini"),
            ModelDefinition(id = "gpt-5-nano", subtitle = "GPT 5 Nano"),
            ModelDefinition(id = "gpt-5-search-api", subtitle = "GPT 5 Search API"),
            ModelDefinition(id = "gpt-5.1", subtitle = "GPT 5.1"),
            ModelDefinition(id = "gpt-5.2", subtitle = "GPT 5.2"),
            ModelDefinition(id = "gpt-5.3-codex", subtitle = "GPT 5.3 Codex"),
            ModelDefinition(id = "gpt-5.4", subtitle = "GPT 5.4"),
            ModelDefinition(id = "gpt-5.4-mini", subtitle = "GPT 5.4 Mini"),
            ModelDefinition(id = "gpt-5.4-nano", subtitle = "GPT 5.4 Nano"),
            ModelDefinition(id = "gpt-oss-120b", subtitle = "GPT Oss 120B"),
            ModelDefinition(id = "gpt-oss-20b", subtitle = "GPT Oss 20B"),
            ModelDefinition(id = "grok-4", subtitle = "Grok 4"),
            ModelDefinition(id = "grok-4-fast-non-reasoning", subtitle = "Grok 4 Fast Non Reasoning"),
            ModelDefinition(id = "grok-4-fast-reasoning", subtitle = "Grok 4 Fast Reasoning"),
            ModelDefinition(id = "grok-4.1-fast-non-reasoning", subtitle = "Grok 4.1 Fast Non Reasoning"),
            ModelDefinition(id = "grok-4.1-fast-reasoning", subtitle = "Grok 4.1 Fast Reasoning"),
            ModelDefinition(id = "grok-4.20-non-reasoning", subtitle = "Grok 4.20 Non Reasoning"),
            ModelDefinition(id = "grok-4.20-reasoning", subtitle = "Grok 4.20 Reasoning"),
            ModelDefinition(id = "grok-4.3", subtitle = "Grok 4.3"),
            ModelDefinition(id = "grok-code-fast-1", subtitle = "Grok Code Fast 1"),
            ModelDefinition(id = "hermes-4-405b", subtitle = "Hermes 4 405B"),
            ModelDefinition(id = "hermes-4-70b", subtitle = "Hermes 4 70B"),
            ModelDefinition(id = "kimi-k2.6", subtitle = "Kimi K2.6"),
            ModelDefinition(id = "kimi-k2.7-code", subtitle = "Kimi K2.7 Code"),
            ModelDefinition(id = "llama-3.1-8b-instruct", subtitle = "Llama 3.1 8B Instruct"),
            ModelDefinition(id = "llama-3.3-70b-instruct", subtitle = "Llama 3.3 70B Instruct"),
            ModelDefinition(id = "magistral-medium-2509", subtitle = "Magistral Medium 2509"),
            ModelDefinition(id = "magistral-medium-latest", subtitle = "Magistral Medium Latest"),
            ModelDefinition(id = "magistral-small-2509", subtitle = "Magistral Small 2509"),
            ModelDefinition(id = "magistral-small-latest", subtitle = "Magistral Small Latest"),
            ModelDefinition(id = "mimo-v2.5", subtitle = "MIMO V2.5"),
            ModelDefinition(id = "mimo-v2.5-pro", subtitle = "MIMO V2.5 Pro"),
            ModelDefinition(id = "minimax-m2.7", subtitle = "Minimax M2.7"),
            ModelDefinition(id = "minimax-m3", subtitle = "Minimax M3"),
            ModelDefinition(id = "mistral-large-2512", subtitle = "Mistral Large 2512"),
            ModelDefinition(id = "mistral-large-latest", subtitle = "Mistral Large Latest"),
            ModelDefinition(id = "mistral-medium-2508", subtitle = "Mistral Medium 2508"),
            ModelDefinition(id = "mistral-medium-3-5", subtitle = "Mistral Medium 3 5"),
            ModelDefinition(id = "mistral-medium-latest", subtitle = "Mistral Medium Latest"),
            ModelDefinition(id = "mistral-small-2603", subtitle = "Mistral Small 2603"),
            ModelDefinition(id = "mistral-small-latest", subtitle = "Mistral Small Latest"),
            ModelDefinition(id = "nemotron-3-super", subtitle = "Nemotron 3 Super"),
            ModelDefinition(id = "o3", subtitle = "O3"),
            ModelDefinition(id = "o3-mini", subtitle = "O3 Mini"),
            ModelDefinition(id = "o4-mini", subtitle = "O4 Mini"),
            ModelDefinition(id = "qwen3.5-397b-a17b", subtitle = "Qwen3.5 397B A17b"),
            ModelDefinition(id = "sonar", subtitle = "Sonar"),
            ModelDefinition(id = "sonar-deep-research", subtitle = "Sonar Deep Research"),
            ModelDefinition(id = "sonar-pro", subtitle = "Sonar Pro"),
            ModelDefinition(id = "sonar-reasoning-pro", subtitle = "Sonar Reasoning Pro"),
        ),
        apiKeyUrl = "https://api.navy",
        apiKeyUrlDisplay = "api.navy",
        supportsImages = true,
        customHeaders = mapOf("User-Agent" to "FreeLLMAPI/1.0"),
    )

    data object OVH : Service(
        id = "ovh",
        displayName = "OVH AI Endpoints",
        icon = Res.drawable.ic_service_ovh,
        requiresApiKey = false,
        supportsOptionalApiKey = true,
        defaultModel = "Meta-Llama-3_3-70B-Instruct",
        settingsKeyPrefix = "ovh",
        chatUrl = "https://oai.endpoints.kepler.ai.cloud.ovh.net/v1/chat/completions",
        modelsUrl = "https://oai.endpoints.kepler.ai.cloud.ovh.net/v1/models",
        defaultModels = listOf(
            ModelDefinition(id = "Meta-Llama-3_3-70B-Instruct", subtitle = "Llama 3.3 70B"),
            ModelDefinition(id = "Mistral-7B-Instruct-v0.3", subtitle = "Mistral 7B Instruct v0.3"),
            ModelDefinition(id = "Mistral-Nemo-Instruct-2407", subtitle = "Mistral Nemo"),
            ModelDefinition(id = "Mistral-Small-3.2-24B-Instruct-2506", subtitle = "Mistral Small 3.2 24B"),
            ModelDefinition(id = "Qwen2.5-VL-72B-Instruct", subtitle = "Qwen2.5 VL 72B"),
            ModelDefinition(id = "Qwen3-32B", subtitle = "Qwen3 32B"),
            ModelDefinition(id = "Qwen3-Coder-30B-A3B-Instruct", subtitle = "Qwen3-Coder 30B"),
            ModelDefinition(id = "Qwen3.5-397B-A17B", subtitle = "Qwen3.5 397B"),
            ModelDefinition(id = "Qwen3.6-27B", subtitle = "Qwen3.6 27B"),
            ModelDefinition(id = "Qwen3Guard-Gen-0.6B", subtitle = "Qwen3Guard Gen 0.6B (OVH safety)"),
            ModelDefinition(id = "Qwen3Guard-Gen-8B", subtitle = "Qwen3Guard Gen 8B (OVH safety)"),
            ModelDefinition(id = "gpt-oss-120b", subtitle = "GPT-OSS 120B"),
            ModelDefinition(id = "gpt-oss-20b", subtitle = "GPT-OSS 20B"),
        ),
        apiKeyUrl = "https://endpoints.ai.cloud.ovh.net",
        apiKeyUrlDisplay = "endpoints.ai.cloud.ovh.net",
        supportsImages = true,
    )

    data object Reka : Service(
        id = "reka",
        displayName = "Reka",
        icon = Res.drawable.ic_service_reka,
        requiresApiKey = true,
        defaultModel = "reka-flash",
        settingsKeyPrefix = "reka",
        chatUrl = "https://api.reka.ai/v1/chat/completions",
        modelsUrl = "https://api.reka.ai/v1/models",
        defaultModels = listOf(
            ModelDefinition(id = "reka-edge-2603", subtitle = "Reka Edge"),
            ModelDefinition(id = "reka-flash", subtitle = "Reka Flash"),
        ),
        apiKeyUrl = "https://platform.reka.ai",
        apiKeyUrlDisplay = "platform.reka.ai",
        supportsImages = true,
    )

    data object Requesty : Service(
        id = "requesty",
        displayName = "Requesty",
        icon = Res.drawable.ic_service_requesty,
        requiresApiKey = true,
        defaultModel = "nvidia/nemotron-3-super-120b-a12b",
        settingsKeyPrefix = "requesty",
        chatUrl = "https://router.requesty.ai/v1/chat/completions",
        modelsUrl = "https://router.requesty.ai/v1/models",
        defaultModels = listOf(
            ModelDefinition(id = "google/gemma-4-31b-it", subtitle = "Gemma 4 31B"),
            ModelDefinition(id = "mistral/leanstral-1-5", subtitle = "Leanstral 1.5"),
            ModelDefinition(id = "nvidia/nemotron-3-nano-30b-a3b", subtitle = "Nemotron 3 Nano 30B"),
            ModelDefinition(id = "nvidia/nemotron-3-super-120b-a12b", subtitle = "Nemotron 3 Super 120B"),
            ModelDefinition(id = "nvidia/nemotron-3-ultra-550b-a55b", subtitle = "Nemotron 3 Ultra 550B"),
            ModelDefinition(id = "nvidia/nemotron-3.5-content-safety", subtitle = "Nemotron 3.5 Content Safety"),
            ModelDefinition(id = "poolside/laguna-m.1", subtitle = "Poolside Laguna M.1"),
        ),
        apiKeyUrl = "https://requesty.ai",
        apiKeyUrlDisplay = "requesty.ai",
        supportsImages = true,
    )

    data object Routeway : Service(
        id = "routeway",
        displayName = "Routeway",
        icon = Res.drawable.ic_service_routeway,
        requiresApiKey = true,
        defaultModel = "llama-3.3-70b-instruct:free",
        settingsKeyPrefix = "routeway",
        chatUrl = "https://api.routeway.ai/v1/chat/completions",
        modelsUrl = "https://api.routeway.ai/v1/models",
        defaultModels = listOf(
            ModelDefinition(id = "llama-3.3-70b-instruct:free", subtitle = "Llama 3.3 70B Instruct"),
            ModelDefinition(id = "nemotron-3-nano-30b-a3b:free", subtitle = "Nemotron 3 Nano 30B A3B"),
            ModelDefinition(id = "nemotron-nano-9b-v2:free", subtitle = "Nemotron Nano 9B v2"),
            ModelDefinition(id = "step-3.7-flash:free", subtitle = "StepFun Step 3.7 Flash"),
        ),
        apiKeyUrl = "https://routeway.ai",
        apiKeyUrlDisplay = "routeway.ai",
        supportsImages = true,
        customHeaders = mapOf("User-Agent" to "Mozilla/5.0 FreeLLMAPI/1.0"),
    )

    data object SeaLion : Service(
        id = "sealion",
        displayName = "SEA-LION",
        icon = Res.drawable.ic_service_sealion,
        requiresApiKey = true,
        defaultModel = "aisingapore/Llama-SEA-LION-v3-70B-IT",
        settingsKeyPrefix = "sealion",
        chatUrl = "https://api.sea-lion.ai/v1/chat/completions",
        modelsUrl = "https://api.sea-lion.ai/v1/models",
        defaultModels = listOf(
            ModelDefinition(id = "aisingapore/Gemma-SEA-LION-v4-27B-IT", subtitle = "Gemma SEA-LION v4 27B"),
            ModelDefinition(id = "aisingapore/Llama-SEA-LION-v3-70B-IT", subtitle = "Llama SEA-LION v3 70B"),
            ModelDefinition(id = "aisingapore/Qwen-SEA-LION-v4-32B-IT", subtitle = "Qwen SEA-LION v4 32B"),
            ModelDefinition(id = "aisingapore/Qwen-SEA-LION-v4.5-27B-IT", subtitle = "Qwen SEA-LION v4.5 27B"),
        ),
        apiKeyUrl = "https://sea-lion.ai",
        apiKeyUrlDisplay = "sea-lion.ai",
        supportsImages = true,
    )

    data object Zhipu : Service(
        id = "zhipu",
        displayName = "Zhipu AI",
        icon = Res.drawable.ic_service_zhipu,
        requiresApiKey = true,
        defaultModel = "glm-4.7-flash",
        settingsKeyPrefix = "zhipu",
        chatUrl = "https://open.bigmodel.cn/api/paas/v4/chat/completions",
        modelsUrl = "https://open.bigmodel.cn/api/paas/v4/models",
        defaultModels = listOf(
            ModelDefinition(id = "glm-4.5-flash", subtitle = "GLM-4.5 Flash"),
            ModelDefinition(id = "glm-4.6v-flash", subtitle = "GLM-4.6V Flash"),
            ModelDefinition(id = "glm-4.7-flash", subtitle = "GLM-4.7 Flash"),
        ),
        apiKeyUrl = "https://open.bigmodel.cn",
        apiKeyUrlDisplay = "open.bigmodel.cn",
        supportsImages = true,
        // GLM-4.7-flash 为隐藏推理模型：推理阶段先输出 reasoning_content，
        // 需在后续轮次回传该字段（与 Z.AI 国际站一致）。
        reasoningRequestMode = ReasoningRequestMode.REASONING_CONTENT,
    )

    /**
     * GitHub Models — OpenAI-compatible (models.github.ai/inference). Free tier
     * requires a GitHub Personal Access Token (classic, `repo` scope or fine-grained
     * with Models access). Catalog uses `<publisher>/<model>` ids (e.g.
     * `openai/gpt-4.1`); the old Azure endpoint rejects that prefix, so the current
     * endpoint is used instead. GPT-5 family returns `unavailable_model` on free tier,
     * so the working roster is GPT-4o + GPT-4.1 (per freellmapi baseline).
     */
    data object GitHubModels : Service(
        id = "github",
        displayName = "GitHub Models",
        icon = Res.drawable.ic_service_github,
        requiresApiKey = true,
        defaultModel = "gpt-4o",
        settingsKeyPrefix = "github",
        chatUrl = "https://models.github.ai/inference/chat/completions",
        modelsUrl = "https://models.github.ai/inference/models",
        defaultModels = listOf(
            ModelDefinition(id = "gpt-4o", subtitle = "GPT-4o"),
            ModelDefinition(id = "openai/gpt-4.1", subtitle = "GPT-4.1"),
        ),
        apiKeyUrl = "https://github.com/settings/tokens",
        apiKeyUrlDisplay = "github.com/settings/tokens",
        supportsImages = true,
    )

    /**
     * Pollinations — OpenAI-compatible shared-capacity tier (gen.pollinations.ai/v1).
     * Free anonymous access (rate-limited, one pollen per IP per hour); a publishable
     * key from pollinations.ai raises capacity. Public model list returns a single
     * anonymous-tier entry, `openai-fast` (GPT-OSS 20B). Tool calls supported.
     */
    data object Pollinations : Service(
        id = "pollinations",
        displayName = "Pollinations",
        icon = Res.drawable.ic_service_pollinations,
        requiresApiKey = false,
        supportsOptionalApiKey = true,
        defaultModel = "openai-fast",
        settingsKeyPrefix = "pollinations",
        chatUrl = "https://gen.pollinations.ai/v1/chat/completions",
        modelsUrl = "https://gen.pollinations.ai/v1/models",
        defaultModels = listOf(
            ModelDefinition(id = "openai-fast", subtitle = "GPT-OSS 20B"),
        ),
        apiKeyUrl = "https://pollinations.ai",
        apiKeyUrlDisplay = "pollinations.ai",
        supportsImages = false,
    )

    data object LiteRT : Service(
        id = "litert",
        displayName = "Local Model",
        icon = Res.drawable.ic_service_litert,
        requiresApiKey = false,
        defaultModel = null,
        settingsKeyPrefix = "litert",
        isOnDevice = true,
    )

    companion object {
        val all: List<Service> get() = listOf(Free, AtlasCloud, Gemini, Anthropic, OpenAI, DeepSeek, Mistral, XAI, OpenRouter, Groq, Nvidia, Cerebras, OllamaCloud, LongCat, Together, HuggingFace, Venice, Moonshot, Zai, ZaiCodingPlan, Minimax, AiHubMix, DeepInfra, FireworksAI, OpenCode, OpenCodeTerminal, PublicAI, AIHorde, Perplexity, OpenAICompatible, Agnes, AINative, Aion, BazaarLink, Cloudflare, Cohere, GitHubModels, Kilo, LLM7, NaraRouter, NavyAI, OVH, Pollinations, Reka, Requesty, Routeway, SeaLion, Zhipu, LiteRT)

        const val DEFAULT_OPENAI_COMPATIBLE_BASE_URL = "http://localhost:11434/v1"

        fun fromId(id: String): Service = all.find { it.id == id } ?: Free
    }

    val apiKeyKey: String get() = "service_${settingsKeyPrefix}_api_key"
    val modelIdKey: String get() = "service_${settingsKeyPrefix}_model_id"
    val baseUrlKey: String get() = "service_${settingsKeyPrefix}_base_url"
}
