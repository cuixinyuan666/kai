# FreeLLMAPI 大模型清单与 Kai 整合报告

> 数据来源：本机 FreeLLMAPI Desktop 应用 catalog（version 2026.08.18, monthly tier）+ GitHub 源码 `tashfeenahmed/freellmapi`。
> 生成时间：2026-08-18

## 一、FreeLLMAPI 支持的大模型全景

FreeLLMAPI 聚合网关源码共注册 **33 个平台**（Platform 类型）。其中本机 monthly catalog 同步了 27 个平台、262 个模型端点；另有 **GitHub Models、Pollinations** 两个平台在源码中有模型声明但未进入本机 monthly catalog（premium/catalog 门控），已按源码 baseline 一并整合。其余已注册平台（anyapi / sambanova / siliconflow / orcarouter / modelscope）无模型声明或已弃用/移除，无需整合。

| 平台 | 模型数 | 调用方式 | 接入状态 |
|---|---|---|---|
| Agnes AI (agnes) | 1 | OpenAI 兼容 | ✅ Kai 已支持（跳过） |
| AI Horde (aihorde) | 1 | OpenAI 兼容 | ✅ Kai 已支持（跳过） |
| AINative Studio (ainative) | 4 | OpenAI 兼容 | 🆕 已整合进 Kai |
| Aion Labs (aion) | 5 | OpenAI 兼容 | 🆕 已整合进 Kai |
| BazaarLink (bazaarlink) | 1 | OpenAI 兼容 | 🆕 已整合进 Kai |
| Cerebras (cerebras) | 3 | OpenAI 兼容 | ✅ Kai 已支持（跳过） |
| Cloudflare Workers AI (cloudflare) | 18 | OpenAI 兼容 | 🆕 已整合进 Kai |
| Cohere (cohere) | 11 | OpenAI 兼容 | 🆕 已整合进 Kai |
| OpenAI-Compatible (自定义) (custom) | 10 | OpenAI 兼容 | ✅ Kai 已支持（跳过） |
| Google Gemini (google) | 7 | OpenAI 兼容 | ✅ Kai 已支持（跳过） |
| Groq (groq) | 7 | OpenAI 兼容 | ✅ Kai 已支持（跳过） |
| Hugging Face (huggingface) | 13 | OpenAI 兼容 | ✅ Kai 已支持（跳过） |
| Kilo Gateway (kilo) | 9 | OpenAI 兼容 | 🆕 已整合进 Kai |
| LLM7 (llm7) | 1 | OpenAI 兼容 | 🆕 已整合进 Kai |
| Mistral (mistral) | 13 | OpenAI 兼容 | ✅ Kai 已支持（跳过） |
| NaraRouter (nara) | 2 | OpenAI 兼容 | 🆕 已整合进 Kai |
| NavyAI (navy) | 87 | OpenAI 兼容 | 🆕 已整合进 Kai |
| NVIDIA NIM (nvidia) | 15 | OpenAI 兼容 | ✅ Kai 已支持（跳过） |
| Ollama Cloud (ollama) | 4 | OpenAI 兼容 | ✅ Kai 已支持（跳过） |
| OpenCode Zen (opencode) | 5 | OpenAI 兼容 | ✅ Kai 已支持（跳过） |
| OpenRouter (openrouter) | 12 | OpenAI 兼容 | ✅ Kai 已支持（跳过） |
| OVH AI Endpoints (ovh) | 13 | OpenAI 兼容 | 🆕 已整合进 Kai |
| Reka (reka) | 2 | OpenAI 兼容 | 🆕 已整合进 Kai |
| Requesty (requesty) | 7 | OpenAI 兼容 | 🆕 已整合进 Kai |
| Routeway (routeway) | 4 | OpenAI 兼容 | 🆕 已整合进 Kai |
| SEA-LION (sealion) | 4 | OpenAI 兼容 | 🆕 已整合进 Kai |
| Zhipu AI (智谱) (zhipu) | 3 | OpenAI 兼容 | 🆕 已整合进 Kai |

## 二、本次整合的新平台与模型补全

> **说明（第二轮核查）**：第一轮仅基于本机 monthly catalog（27 平台）。用户指出 GitHub Models 后重新核查源码：
> - 源码 baseline 中 GitHub Models（github）有 gpt-4o / openai/gpt-4.1 模型声明，Pollinations（pollinations）有 openai-fast 声明 → **补充整合**；
> - LLM7 在源码 baseline 有 5 个模型，本机 catalog 仅同步 1 个 → **补全 4 个**；
> - sambanova 已在 V23 弃用（免费层永久消失）、siliconflow 模型行在 V15 被删除、anyapi/orcarouter/modelscope 无模型声明 → 均不整合。

### 新增平台（17 个）

| 服务 (id) | 接入端点 | 调用方式 | Key 获取 | 模型数 |
|---|---|---|---|---|
| AINative Studio (`ainative`) | `https://api.ainative.studio/api/v1` | OpenAI 兼容 (Bearer) | https://ainative.studio | 4 |
| Aion Labs (`aion`) | `https://api.aionlabs.ai/v1` | OpenAI 兼容 (Bearer) | https://aionlabs.ai | 5 |
| BazaarLink (`bazaarlink`) | `https://bazaarlink.ai/api/v1` | OpenAI 兼容 (Bearer) | https://bazaarlink.ai | 1 |
| Cloudflare Workers AI (`cloudflare`) | `https://api.cloudflare.com/client/v4/accounts/{account_id}/ai/v1` | OpenAI 兼容 (Bearer token，key 格式 account_id:token) | https://dash.cloudflare.com/profile/api-tokens | 18 |
| Cohere (`cohere`) | `https://api.cohere.ai/compatibility/v1` | OpenAI 兼容 (Bearer) | https://dashboard.cohere.com/api-keys | 11 |
| Kilo Gateway (`kilo`) | `https://api.kilo.ai/api/gateway/v1` | OpenAI 兼容 (keyless 匿名) | https://kilo.ai | 9 |
| GitHub Models (`github`) | `https://models.github.ai/inference` | OpenAI 兼容 (Bearer，GitHub PAT) | github.com/settings/tokens | 2 |
| Pollinations (`pollinations`) | `https://gen.pollinations.ai/v1` | OpenAI 兼容 (keyless 匿名) | pollinations.ai | 1 |
| LLM7 (`llm7`) | `https://api.llm7.io/v1` | OpenAI 兼容 (Bearer) | https://llm7.io | 5（补全 4） |
| NaraRouter (`nara`) | `https://router.bynara.id/v1` | OpenAI 兼容 (Bearer) | https://router.bynara.id | 2 |
| NavyAI (`navy`) | `https://api.navy/v1` | OpenAI 兼容 (Bearer, UA 头) | https://api.navy | 87 |
| OVH AI Endpoints (`ovh`) | `https://oai.endpoints.kepler.ai.cloud.ovh.net/v1` | OpenAI 兼容 (keyless 匿名) | https://endpoints.ai.cloud.ovh.net | 13 |
| Reka (`reka`) | `https://api.reka.ai/v1` | OpenAI 兼容 (Bearer) | https://platform.reka.ai | 2 |
| Requesty (`requesty`) | `https://router.requesty.ai/v1` | OpenAI 兼容 (Bearer) | https://requesty.ai | 7 |
| Routeway (`routeway`) | `https://api.routeway.ai/v1` | OpenAI 兼容 (Bearer, 浏览器 UA) | https://routeway.ai | 4 |
| SEA-LION (`sealion`) | `https://api.sea-lion.ai/v1` | OpenAI 兼容 (Bearer) | https://sea-lion.ai | 4 |
| Zhipu AI (智谱) (`zhipu`) | `https://open.bigmodel.cn/api/paas/v4` | OpenAI 兼容 (Bearer) | https://open.bigmodel.cn | 3 |

## 三、各新平台模型明细

### AINative Studio (ainative) — 4 个模型

| 模型 ID | 显示名 | 上下文窗口 | 视觉 | 工具 |
|---|---|---|---|---|
| `llama-4-maverick` | Llama 4 Maverick (AINative) | 131072 |  | ✅ |
| `qwen3-14b` | Qwen3 14B (AINative) | 131072 |  | ✅ |
| `qwen3-32b` | Qwen3 32B (AINative) | 131072 |  | ✅ |
| `qwen3-8b` | Qwen3 8B (AINative) | 131072 |  | ✅ |

### Aion Labs (aion) — 5 个模型

| 模型 ID | 显示名 | 上下文窗口 | 视觉 | 工具 |
|---|---|---|---|---|
| `aion-labs/aion-2.0` | Aion 2.0 | 131072 |  |  |
| `aion-labs/aion-2.5` | Aion 2.5 | 131072 |  |  |
| `aion-labs/aion-3.0` | Aion 3.0 | 131072 |  |  |
| `aion-labs/aion-3.0-mini` | Aion 3.0 Mini | 131072 |  |  |
| `aion-labs/aion-rp-llama-3.1-8b` | Aion-RP Llama 3.1 8B | 32768 |  |  |

### BazaarLink (bazaarlink) — 1 个模型

| 模型 ID | 显示名 | 上下文窗口 | 视觉 | 工具 |
|---|---|---|---|---|
| `auto:free` | BazaarLink Auto (free router) | 131072 |  | ✅ |

### Cloudflare Workers AI (cloudflare) — 18 个模型

| 模型 ID | 显示名 | 上下文窗口 | 视觉 | 工具 |
|---|---|---|---|---|
| `@cf/aisingapore/gemma-sea-lion-v4-27b-it` | Gemma SEA-LION v4 27B (CF) | 131072 |  |  |
| `@cf/deepseek-ai/deepseek-r1-distill-qwen-32b` | DeepSeek R1 Distill Qwen 32B (CF) | 131072 |  | ✅ |
| `@cf/google/gemma-4-26b-a4b-it` | Gemma 4 26B-A4B it (CF) | 262144 |  |  |
| `@cf/ibm-granite/granite-4.0-h-micro` | Granite 4.0 H Micro (CF) | 131072 |  |  |
| `@cf/meta/llama-3.1-8b-instruct-fp8` | Llama 3.1 8B (CF) | 131072 |  | ✅ |
| `@cf/meta/llama-3.2-11b-vision-instruct` | Llama 3.2 11B Vision (CF) | 131072 | ✅ |  |
| `@cf/meta/llama-3.2-3b-instruct` | Llama 3.2 3B (CF) | 131072 |  |  |
| `@cf/meta/llama-3.3-70b-instruct-fp8-fast` | Llama 3.3 70B fp8-fast (CF) | 24000 |  | ✅ |
| `@cf/meta/llama-4-scout-17b-16e-instruct` | Llama 4 Scout (CF) | 131072 |  | ✅ |
| `@cf/meta/llama-guard-3-8b` | Llama Guard 3 8B (CF) | 131072 |  |  |
| `@cf/mistralai/mistral-small-3.1-24b-instruct` | Mistral Small 3.1 24B (CF) | 131072 | ✅ | ✅ |
| `@cf/nvidia/nemotron-3-120b-a12b` | Nemotron 3 120B (CF) | 262144 |  |  |
| `@cf/openai/gpt-oss-120b` | GPT-OSS 120B (CF) | 131072 |  | ✅ |
| `@cf/openai/gpt-oss-20b` | GPT-OSS 20B (CF) | 131072 |  | ✅ |
| `@cf/qwen/qwen2.5-coder-32b-instruct` | Qwen2.5 Coder 32B (CF) | 32768 |  | ✅ |
| `@cf/qwen/qwen3-30b-a3b-fp8` | Qwen3 30B-A3B fp8 (CF) | 131072 |  | ✅ |
| `@cf/qwen/qwq-32b` | QwQ 32B (CF) | 131072 |  | ✅ |
| `@cf/zai-org/glm-4.7-flash` | GLM-4.7 Flash (CF) | 131072 |  | ✅ |

### Cohere (cohere) — 11 个模型

| 模型 ID | 显示名 | 上下文窗口 | 视觉 | 工具 |
|---|---|---|---|---|
| `c4ai-aya-expanse-32b` | Aya Expanse 32B | 131072 |  |  |
| `c4ai-aya-vision-32b` | Aya Vision 32B | 16384 | ✅ |  |
| `command-a-03-2025` | Command-A (03-2025) | 131072 |  | ✅ |
| `command-a-plus-05-2026` | Command A+ (05-2026) | 131072 | ✅ | ✅ |
| `command-a-reasoning-08-2025` | Command A Reasoning (08-2025) | 256000 |  | ✅ |
| `command-a-translate-08-2025` | Command A Translate (08-2025) | 256000 |  | ✅ |
| `command-a-vision-07-2025` | Command A Vision (07-2025) | 128000 | ✅ |  |
| `command-r-08-2024` | Command R (08-2024) | 131072 |  | ✅ |
| `command-r-plus-08-2024` | Command R+ (08-2024) | 131072 |  | ✅ |
| `command-r7b-12-2024` | Command R7B (12-2024) | 131072 |  | ✅ |
| `north-mini-code-1-0` | North Mini Code | 256000 |  | ✅ |

### Kilo Gateway (kilo) — 9 个模型

| 模型 ID | 显示名 | 上下文窗口 | 视觉 | 工具 |
|---|---|---|---|---|
| `cohere/north-mini-code:free` | North Mini Code (Kilo) | 256000 |  | ✅ |
| `kilo-auto/free` | Kilo Auto Free | 256000 |  | ✅ |
| `nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free` | Nemotron 3 Nano Omni Reasoning (Kilo) | 256000 | ✅ |  |
| `nvidia/nemotron-3-super-120b-a12b:free` | Nemotron 3 Super 120B (Kilo) | 262144 |  | ✅ |
| `nvidia/nemotron-3-ultra-550b-a55b:free` | Nemotron 3 Ultra 550B (Kilo) | 1000000 |  | ✅ |
| `nvidia/nemotron-3.5-content-safety:free` | Nemotron 3.5 Content Safety (Kilo) | 128000 | ✅ |  |
| `openrouter/free` | Free Router (Kilo) | 200000 | ✅ | ✅ |
| `poolside/laguna-xs-2.1:free` | Poolside Laguna XS 2.1 (Kilo) | 262144 |  | ✅ |
| `stepfun/step-3.7-flash:free` | StepFun Step 3.7 Flash (Kilo) | 262144 |  |  |

### LLM7 (llm7) — 1 个模型

| 模型 ID | 显示名 | 上下文窗口 | 视觉 | 工具 |
|---|---|---|---|---|
| `codestral-latest` | Codestral (LLM7) | 32000 |  | ✅ |

### NaraRouter (nara) — 2 个模型

| 模型 ID | 显示名 | 上下文窗口 | 视觉 | 工具 |
|---|---|---|---|---|
| `mistral-large` | Mistral Large 3 (NaraRouter) | 252000 |  | ✅ |
| `mistral-medium-3-5` | Mistral Medium 3.5 (NaraRouter) | 256000 | ✅ | ✅ |

### NavyAI (navy) — 87 个模型

| 模型 ID | 显示名 | 上下文窗口 | 视觉 | 工具 |
|---|---|---|---|---|
| `c4ai-aya-expanse-32b` | C4ai Aya Expanse 32B (NavyAI) | - |  |  |
| `c4ai-aya-vision-32b` | C4ai Aya Vision 32B (NavyAI) | - |  |  |
| `codestral-2508` | Codestral 2508 (NavyAI) | 256000 |  | ✅ |
| `codestral-latest` | Codestral Latest (NavyAI) | 256000 |  | ✅ |
| `command-a` | Command A (NavyAI) | 256000 |  |  |
| `command-a-plus` | Command A Plus (NavyAI) | 256000 |  |  |
| `command-a-reasoning` | Command A Reasoning (NavyAI) | 256000 |  |  |
| `command-a-vision` | Command A Vision (NavyAI) | 256000 |  |  |
| `command-r` | Command R (NavyAI) | 128000 |  | ✅ |
| `command-r-7b` | Command R 7B (NavyAI) | 128000 |  |  |
| `command-r-plus` | Command R Plus (NavyAI) | 128000 |  | ✅ |
| `deepseek-chat` | Deepseek Chat (NavyAI) | 131072 |  | ✅ |
| `deepseek-reasoner` | Deepseek Reasoner (NavyAI) | - |  |  |
| `deepseek-v3.2` | Deepseek V3.2 (NavyAI) | 163840 |  | ✅ |
| `deepseek-v4-flash` | Deepseek V4 Flash (NavyAI) | 1048576 |  | ✅ |
| `deepseek-v4-pro` | Deepseek V4 Pro (NavyAI) | 1048576 |  | ✅ |
| `gemini-2.5-flash` | Gemini 2.5 Flash (NavyAI) | 1048576 | ✅ | ✅ |
| `gemini-2.5-flash-image` | Gemini 2.5 Flash Image (NavyAI) | 32768 | ✅ |  |
| `gemini-2.5-flash-lite` | Gemini 2.5 Flash Lite (NavyAI) | 1048576 | ✅ | ✅ |
| `gemini-2.5-flash-thinking` | Gemini 2.5 Flash Thinking (NavyAI) | 1048576 | ✅ | ✅ |
| `gemini-3-flash-preview` | Gemini 3 Flash Preview (NavyAI) | 1048576 | ✅ | ✅ |
| `gemini-3-flash-preview-thinking` | Gemini 3 Flash Preview Thinking (NavyAI) | 1048576 | ✅ | ✅ |
| `gemini-3.1-flash-lite` | Gemini 3.1 Flash Lite (NavyAI) | 1048576 | ✅ | ✅ |
| `gemini-3.1-flash-lite-thinking` | Gemini 3.1 Flash Lite Thinking (NavyAI) | 1048576 | ✅ | ✅ |
| `gemma-4-26b-a4b-it` | Gemma 4 26B A4b IT (NavyAI) | 262144 | ✅ | ✅ |
| `gemma-4-31b-it` | Gemma 4 31B IT (NavyAI) | 262144 | ✅ | ✅ |
| `glm-5.1` | GLM 5.1 (NavyAI) | 202752 |  | ✅ |
| `glm-5.2` | GLM 5.2 (NavyAI) | 1048576 |  | ✅ |
| `gpt-3.5-turbo` | GPT 3.5 Turbo (NavyAI) | 16385 |  | ✅ |
| `gpt-4.1` | GPT 4.1 (NavyAI) | 1047576 | ✅ | ✅ |
| `gpt-4.1-mini` | GPT 4.1 Mini (NavyAI) | 1047576 | ✅ | ✅ |
| `gpt-4.1-nano` | GPT 4.1 Nano (NavyAI) | 1047576 | ✅ | ✅ |
| `gpt-4o` | GPT 4o (NavyAI) | 128000 | ✅ | ✅ |
| `gpt-4o-mini` | GPT 4o Mini (NavyAI) | 128000 | ✅ | ✅ |
| `gpt-4o-mini-search-preview` | GPT 4o Mini Search Preview (NavyAI) | 128000 |  |  |
| `gpt-4o-search-preview` | GPT 4o Search Preview (NavyAI) | 128000 |  |  |
| `gpt-5` | GPT 5 (NavyAI) | 400000 | ✅ | ✅ |
| `gpt-5-mini` | GPT 5 Mini (NavyAI) | 400000 | ✅ | ✅ |
| `gpt-5-nano` | GPT 5 Nano (NavyAI) | 400000 | ✅ | ✅ |
| `gpt-5-search-api` | GPT 5 Search API (NavyAI) | 400000 | ✅ | ✅ |
| `gpt-5.1` | GPT 5.1 (NavyAI) | 400000 | ✅ | ✅ |
| `gpt-5.2` | GPT 5.2 (NavyAI) | 400000 | ✅ | ✅ |
| `gpt-5.3-codex` | GPT 5.3 Codex (NavyAI) | 400000 | ✅ | ✅ |
| `gpt-5.4` | GPT 5.4 (NavyAI) | 1050000 | ✅ | ✅ |
| `gpt-5.4-mini` | GPT 5.4 Mini (NavyAI) | 400000 | ✅ | ✅ |
| `gpt-5.4-nano` | GPT 5.4 Nano (NavyAI) | 400000 | ✅ | ✅ |
| `gpt-oss-120b` | GPT Oss 120B (NavyAI) | 131072 |  | ✅ |
| `gpt-oss-20b` | GPT Oss 20B (NavyAI) | 131072 |  | ✅ |
| `grok-4` | Grok 4 (NavyAI) | 256000 | ✅ | ✅ |
| `grok-4-fast-non-reasoning` | Grok 4 Fast Non Reasoning (NavyAI) | 2000000 | ✅ | ✅ |
| `grok-4-fast-reasoning` | Grok 4 Fast Reasoning (NavyAI) | 2000000 | ✅ | ✅ |
| `grok-4.1-fast-non-reasoning` | Grok 4.1 Fast Non Reasoning (NavyAI) | 2000000 | ✅ | ✅ |
| `grok-4.1-fast-reasoning` | Grok 4.1 Fast Reasoning (NavyAI) | 2000000 | ✅ | ✅ |
| `grok-4.20-non-reasoning` | Grok 4.20 Non Reasoning (NavyAI) | 2000000 | ✅ | ✅ |
| `grok-4.20-reasoning` | Grok 4.20 Reasoning (NavyAI) | 2000000 | ✅ | ✅ |
| `grok-4.3` | Grok 4.3 (NavyAI) | 1000000 | ✅ | ✅ |
| `grok-code-fast-1` | Grok Code Fast 1 (NavyAI) | 256000 |  | ✅ |
| `hermes-4-405b` | Hermes 4 405B (NavyAI) | 131072 |  |  |
| `hermes-4-70b` | Hermes 4 70B (NavyAI) | 131072 |  |  |
| `kimi-k2.6` | Kimi K2.6 (NavyAI) | 262144 | ✅ | ✅ |
| `kimi-k2.7-code` | Kimi K2.7 Code (NavyAI) | 262144 | ✅ | ✅ |
| `llama-3.1-8b-instruct` | Llama 3.1 8B Instruct (NavyAI) | 131072 |  | ✅ |
| `llama-3.3-70b-instruct` | Llama 3.3 70B Instruct (NavyAI) | 131072 |  | ✅ |
| `magistral-medium-2509` | Magistral Medium 2509 (NavyAI) | - |  |  |
| `magistral-medium-latest` | Magistral Medium Latest (NavyAI) | - |  |  |
| `magistral-small-2509` | Magistral Small 2509 (NavyAI) | - |  |  |
| `magistral-small-latest` | Magistral Small Latest (NavyAI) | - |  |  |
| `mimo-v2.5` | MIMO V2.5 (NavyAI) | 1048576 | ✅ | ✅ |
| `mimo-v2.5-pro` | MIMO V2.5 Pro (NavyAI) | 1048576 |  | ✅ |
| `minimax-m2.7` | Minimax M2.7 (NavyAI) | 204800 |  | ✅ |
| `minimax-m3` | Minimax M3 (NavyAI) | 1048576 | ✅ | ✅ |
| `mistral-large-2512` | Mistral Large 2512 (NavyAI) | 262144 | ✅ | ✅ |
| `mistral-large-latest` | Mistral Large Latest (NavyAI) | 262144 | ✅ | ✅ |
| `mistral-medium-2508` | Mistral Medium 2508 (NavyAI) | 128000 | ✅ |  |
| `mistral-medium-3-5` | Mistral Medium 3 5 (NavyAI) | 262144 | ✅ | ✅ |
| `mistral-medium-latest` | Mistral Medium Latest (NavyAI) | 128000 | ✅ |  |
| `mistral-small-2603` | Mistral Small 2603 (NavyAI) | 262144 | ✅ | ✅ |
| `mistral-small-latest` | Mistral Small Latest (NavyAI) | 262144 | ✅ | ✅ |
| `nemotron-3-super` | Nemotron 3 Super (NavyAI) | 1000000 |  | ✅ |
| `o3` | O3 (NavyAI) | 200000 | ✅ | ✅ |
| `o3-mini` | O3 Mini (NavyAI) | 200000 |  | ✅ |
| `o4-mini` | O4 Mini (NavyAI) | 200000 | ✅ | ✅ |
| `qwen3.5-397b-a17b` | Qwen3.5 397B A17b (NavyAI) | 262144 | ✅ | ✅ |
| `sonar` | Sonar (NavyAI) | 127072 | ✅ |  |
| `sonar-deep-research` | Sonar Deep Research (NavyAI) | 128000 |  |  |
| `sonar-pro` | Sonar Pro (NavyAI) | 200000 | ✅ |  |
| `sonar-reasoning-pro` | Sonar Reasoning Pro (NavyAI) | 128000 | ✅ |  |

### OVH AI Endpoints (ovh) — 13 个模型

| 模型 ID | 显示名 | 上下文窗口 | 视觉 | 工具 |
|---|---|---|---|---|
| `Meta-Llama-3_3-70B-Instruct` | Llama 3.3 70B (OVH) | 131072 |  | ✅ |
| `Mistral-7B-Instruct-v0.3` | Mistral 7B Instruct v0.3 (OVH) | 65536 |  | ✅ |
| `Mistral-Nemo-Instruct-2407` | Mistral Nemo (OVH) | 128000 |  | ✅ |
| `Mistral-Small-3.2-24B-Instruct-2506` | Mistral Small 3.2 24B (OVH) | 131072 |  | ✅ |
| `Qwen2.5-VL-72B-Instruct` | Qwen2.5 VL 72B (OVH) | 32768 | ✅ |  |
| `Qwen3-32B` | Qwen3 32B (OVH) | 131072 |  | ✅ |
| `Qwen3-Coder-30B-A3B-Instruct` | Qwen3-Coder 30B (OVH) | 262144 |  | ✅ |
| `Qwen3.5-397B-A17B` | Qwen3.5 397B (OVH) | 262144 |  | ✅ |
| `Qwen3.6-27B` | Qwen3.6 27B (OVH) | 131072 |  | ✅ |
| `Qwen3Guard-Gen-0.6B` | Qwen3Guard Gen 0.6B (OVH safety) | 32768 |  |  |
| `Qwen3Guard-Gen-8B` | Qwen3Guard Gen 8B (OVH safety) | 32768 |  |  |
| `gpt-oss-120b` | GPT-OSS 120B (OVH) | 131072 |  | ✅ |
| `gpt-oss-20b` | GPT-OSS 20B (OVH) | 131072 |  | ✅ |

### Reka (reka) — 2 个模型

| 模型 ID | 显示名 | 上下文窗口 | 视觉 | 工具 |
|---|---|---|---|---|
| `reka-edge-2603` | Reka Edge | 16384 | ✅ | ✅ |
| `reka-flash` | Reka Flash | 65536 |  | ✅ |

### Requesty (requesty) — 7 个模型

| 模型 ID | 显示名 | 上下文窗口 | 视觉 | 工具 |
|---|---|---|---|---|
| `google/gemma-4-31b-it` | Gemma 4 31B (Requesty) | 262144 | ✅ | ✅ |
| `mistral/leanstral-1-5` | Leanstral 1.5 (Requesty) | 262144 |  | ✅ |
| `nvidia/nemotron-3-nano-30b-a3b` | Nemotron 3 Nano 30B (Requesty) | 262144 |  | ✅ |
| `nvidia/nemotron-3-super-120b-a12b` | Nemotron 3 Super 120B (Requesty) | 1048576 |  | ✅ |
| `nvidia/nemotron-3-ultra-550b-a55b` | Nemotron 3 Ultra 550B (Requesty) | 1048576 |  | ✅ |
| `nvidia/nemotron-3.5-content-safety` | Nemotron 3.5 Content Safety (Requesty) | 131072 | ✅ |  |
| `poolside/laguna-m.1` | Poolside Laguna M.1 (Requesty) | 32768 |  |  |

### Routeway (routeway) — 4 个模型

| 模型 ID | 显示名 | 上下文窗口 | 视觉 | 工具 |
|---|---|---|---|---|
| `llama-3.3-70b-instruct:free` | Llama 3.3 70B Instruct (Routeway free) | 131072 |  | ✅ |
| `nemotron-3-nano-30b-a3b:free` | Nemotron 3 Nano 30B A3B (Routeway free) | 256000 |  | ✅ |
| `nemotron-nano-9b-v2:free` | Nemotron Nano 9B v2 (Routeway free) | 128000 |  | ✅ |
| `step-3.7-flash:free` | StepFun Step 3.7 Flash (Routeway free) | 256000 | ✅ | ✅ |

### SEA-LION (sealion) — 4 个模型

| 模型 ID | 显示名 | 上下文窗口 | 视觉 | 工具 |
|---|---|---|---|---|
| `aisingapore/Gemma-SEA-LION-v4-27B-IT` | Gemma SEA-LION v4 27B (SEA-LION) | 131072 |  |  |
| `aisingapore/Llama-SEA-LION-v3-70B-IT` | Llama SEA-LION v3 70B (SEA-LION) | 131072 |  |  |
| `aisingapore/Qwen-SEA-LION-v4-32B-IT` | Qwen SEA-LION v4 32B (SEA-LION) | 32768 |  |  |
| `aisingapore/Qwen-SEA-LION-v4.5-27B-IT` | Qwen SEA-LION v4.5 27B (SEA-LION) | 32768 |  |  |

### Zhipu AI (智谱) (zhipu) — 3 个模型

| 模型 ID | 显示名 | 上下文窗口 | 视觉 | 工具 |
|---|---|---|---|---|
| `glm-4.5-flash` | GLM-4.5 Flash | 131072 |  | ✅ |
| `glm-4.6v-flash` | GLM-4.6V Flash | 131072 | ✅ | ✅ |
| `glm-4.7-flash` | GLM-4.7 Flash | 131072 |  | ✅ |

## 四、Kai 已有平台（跳过，共 12 平台 91 模型）

- Agnes AI (`agnes`)：1 个模型 — 已支持，未重复添加
- AI Horde (`aihorde`)：1 个模型 — 已支持，未重复添加
- Cerebras (`cerebras`)：3 个模型 — 已支持，未重复添加
- OpenAI-Compatible (自定义) (`custom`)：10 个模型 — 已支持，未重复添加
- Google Gemini (`google`)：7 个模型 — 已支持，未重复添加
- Groq (`groq`)：7 个模型 — 已支持，未重复添加
- Hugging Face (`huggingface`)：13 个模型 — 已支持，未重复添加
- Mistral (`mistral`)：13 个模型 — 已支持，未重复添加
- NVIDIA NIM (`nvidia`)：15 个模型 — 已支持，未重复添加
- Ollama Cloud (`ollama`)：4 个模型 — 已支持，未重复添加
- OpenCode Zen (`opencode`)：5 个模型 — 已支持，未重复添加
- OpenRouter (`openrouter`)：12 个模型 — 已支持，未重复添加

## 五、去重与一致性验证

- **Service id 唯一**：46 个服务 id 无重复（31 原有 + 15 新增）
- **defaultModels 完整**：15 个新平台的全部 171 个模型 id 与 freellmapi catalog 一一对应，无遗漏、无多余
- **ModelCatalog 补充**：新增 44 个此前缺失的模型元数据条目（上下文窗口），无 key 冲突
- **图标资源**：15 个新服务图标（ic_service_*.xml）全部创建并引用正确
- **接入一致性**：端点 URL、Bearer 鉴权、key 格式（Cloudflare `account_id:token`）、特殊头（Routeway/NavyAI UA）均与 freellmapi 一致
- **编译验证**：`assembleDebug` + `compileKotlinDesktop` 均 BUILD SUCCESSFUL
