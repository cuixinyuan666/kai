# Chat & Conversations

**Last verified:** 2026-09-01

Kai's chat system manages the message history, conversation persistence, file attachments, and speech output. Conversations are service-independent — switching providers does not affect which conversation is loaded or restored. Multiple conversations are persisted and browsable via a history sheet.

## Concepts

### Conversation

A persisted chat session containing an id (UUID), message list, timestamps (`createdAt`, `updatedAt`), a title, and a type (`chat`, `heartbeat`, or `interactive`). Conversations are stored in a local database (browser builds use the settings store) and remain available from the history sheet after a restart.

### History

The in-memory message list that drives the UI. Each entry has a role: USER, ASSISTANT, TOOL, or TOOL_EXECUTING. History is the source of truth during a session; it is written to a Conversation on save.

### Conversation Title

Auto-derived from the first user message when a conversation is saved for the first time. Truncated to ~50 characters at a word boundary. Once set, titles are not updated.

## Conversation Lifecycle

- Opening the app does **not** restore the last chat into the main pane. The initial screen shows the CUI name (no spinning progress indicator); saved conversations remain available from the history sheet
- If the persisted pointer references a conversation that no longer exists (or is null because the user started a new chat), the app opens to an empty new chat
- "New Chat" clears history, unsets the current conversation pointer, and persists the empty state
- A new conversation ID (UUID) is allocated when the user sends the first message; the conversation is persisted immediately and updated after each assistant response
- Conversations are saved when the user sends a message and again after each assistant response
- Only the most recent 20 exchanges are persisted per chat conversation; heartbeat conversations have a separate, larger cap of 50 messages so longer automation runs are not truncated as aggressively
- Multiple conversations are persisted — starting a new chat preserves previous conversations
- Conversations are service-independent — switching services does not affect which conversation is loaded
- Interactive vs normal chat mode is persisted while the app stays open; a restart still opens the empty CUI screen rather than an interactive session

## Chat History

- A history icon in the top bar opens a bottom sheet of saved conversations
- **Single-mode chats** (normal and interactive) appear directly at the root of the history sheet
- History lists can be sorted **by time newest-first** (default, latest tasks on top) or **oldest-first**
- Inside a **war task folder** (the list under a dated task such as `2026-8月-31-任务9`), rows sort by **parent model name** A→Z or Z→A instead of time; the task-result row stays pinned at the top
- Model conversations show a **parent model chip** plus a **child model chip**
- **Collaboration** and **War mode** tasks live under their respective mode folders; tap a folder to browse tasks and model sub-conversations
- The legacy flat list behavior is preserved for single-mode chats; only collaboration and war workflows use the folder tree
- Each item shows the title; tapping loads that conversation and dismisses the sheet
- Delete, copy, and undo behaviors match the previous flat history sheet where applicable
- Heartbeat conversations are included with a "Heartbeat" label badge, and can also be accessed via the heartbeat banner

## Message Sending

- User message is added to history, then an API call is made via the fallback chain
- Tool calls are executed inline (TOOL_EXECUTING shown during execution, TOOL result stored after)
- On success, the conversation is saved
- On failure, an error is displayed with a retry button
- **Free rate-limit upsell:** when the user has **no configured services** (only Free FAST/EXPERT) and the request fails with a rate-limit, quota-exhausted, or Free proxy capacity error (e.g. “All free providers failed”), the chat shows a special panel instead of the generic error. It explains that Free is rate-limited and shows compact provider chips (icon + name) for free-usage providers (Groq, Cerebras, Gemini, OpenRouter, Ollama Cloud) in a flow layout; tapping a chip opens that provider’s API-key page in the browser. A retry control remains available. The panel is not shown if any non-Free service is already configured.

## Cancel

- While a request is in progress, a stop button replaces the send button in the input field
- Clicking stop cancels the ongoing API request and any in-flight tool executions
- After cancellation, the loading state clears and the send button reappears when typing

## Retry & Regenerate

- **Retry** resends the current prompt
- **Regenerate** removes all messages after the last user message, then resends

## File Attachments

Multiple files can be attached to a single prompt. Each file is added one at a time via the file picker or drag-and-drop, and appears as a chip below the input. Clicking a chip removes that specific file from the queue. All queued files are cleared after the prompt is sent. Three categories of files are supported:

### Images
- Attach via file picker or drag-and-drop
- Compressed to JPEG and Base64-encoded
- Maximum raw input size: 50 MB; maximum size after compression: 15 MB — rejected with a size error if exceeded
- Sent as `image_url` (OpenAI-compatible), `image` block (Anthropic), or `inline_data` (Gemini)
- Not offered on the built-in Free service — its proxy fans out to text-only fallback models that reject multimodal content
- Shown as a preview thumbnail (max 200dp wide) inside the user message bubble
- Clicking the thumbnail opens a full-screen viewer with pinch-to-zoom, double-tap to toggle zoom, pan when zoomed, and a close button in the top-right (also dismissable via the Android back button or by tapping the backdrop; desktop has no keyboard shortcut for dismissal)

### Text files
- Supports `.txt`, `.md`, `.json`, `.csv`, `.xml`, `.yaml`, `.html`, `.css`, `.js`, `.ts`, `.kt`, `.py`, `.rs`, `.go`, `.c`, `.cpp`, `.swift`, `.sh`, `.sql`, `.toml`, `.ini`, `.log`, `.gradle`, and more
- Maximum size: 200 KB per file
- Content is decoded at send time and concatenated into the user message with a filename header per file
- Works with all providers (content is inlined as text)
- Shown as a filename chip in the user message bubble

### PDFs
- Base64-encoded without compression
- Maximum size: 20 MB — rejected with a size error if exceeded
- PDF attachments are advertised only by services with native document support: Anthropic, Gemini, OpenAI, and OpenRouter. The file picker offers PDF on those services only
- Sent as a `document` block (Anthropic) or `inline_data` (Gemini). On the OpenAI-compatible wire path (OpenAI, OpenRouter, and other OpenAI-compatible services), PDF binaries are currently dropped from the request body — only image parts are encoded as `image_url` — so a PDF attach on OpenAI/OpenRouter is accepted by the UI but not transmitted to the model
- Shown as a filename chip in the user message bubble

### General behavior
- The attachment button is shown whenever the active service supports file attachments (text files work with all remote models); it is hidden when the active service runs on-device, since on-device services do not support attachments
- Unsupported file types (e.g., `.zip`) show an error message
- Files exceeding the per-category size limit show a size error; size is checked by stat before the file is read, so multi-gigabyte attachments are rejected without allocating memory for the full contents
- A **folder** may be attached. In collaboration and war, the folder is described by path and a short listing; models read files with the shell instead of embedding the tree. In single-mode chat, a folder is still expanded into files, but `.git` / `node_modules` / `build` and similar directories are skipped and expansion stops after 40 files or about 400 KB of encoded payload
- Long filenames in chips are truncated with an ellipsis while preserving the extension
- File attachments persist across conversation save/restore via an `attachments` list on each message; older conversations saved with a single-file schema are migrated on load

## Speech Output (TTS)

- Toggle in the top bar enables auto-play of new assistant messages
- Per-message play button on assistant messages
- Markdown is stripped before speaking
- Speech uses the engine the user selected in the platform's system speech settings, so third-party engines are honoured; on Android, Google's engine is only used as a fallback when the system default cannot start
- The engine's own default voice is kept as-is. Kai only picks a different voice when the default one does not speak the system language, in which case the first voice matching that language is selected

## Conversation Storage

- On Android, iOS, and desktop, conversations live in a local SQLite database in app-private storage: one row per conversation plus one row per message, so saving a turn writes only the affected conversation instead of re-serializing the whole history
- The browser build has no persistent database and keeps the full conversation list as a JSON blob in the settings store (see [encryption.md](encryption.md))
- Conversations are upserted — updating a conversation replaces the existing entry by ID, new conversations are appended; the list loads ordered by creation time
- Each conversation also retains a rolling tail of its sandbox shell transcript (last ~10,000 characters) so that follow-up commands in a resumed conversation see the prior shell context; transcript updates write only that field
- Migration chain, run once on first load: the legacy encrypted `conversations.enc` file (XOR with a 32-byte random key) migrates into the settings-store blob; a settings-store blob found on a database-capable platform is imported into the database and removed. Settings import reuses the same path — imported conversations are staged in the settings store and absorbed into the database on the next load, replacing its content
- The database structure is versioned and lives on user devices: any future change to its tables or columns requires an accompanying migration step so existing installs upgrade in place (message contents are stored as JSON and tolerate unknown fields, so message-level additions do not need one)

## UI Elements

- **Top bar**: New Chat, Chat History, a Sandbox toggle (Android only, shown between History and TTS when the sandbox feature is available on the device), TTS toggle, Settings (on mobile; on non-mobile, Settings is in the navigation tab bar)
- **Scroll to bottom**: a small floating action button (down arrow) appears when the user has scrolled up past the latest messages; tapping it animates back to the bottom
- **Messages**: user (right-aligned, with optional image preview), assistant (Markdown-rendered + action buttons), tool executing (spinner), loading indicator, error with retry (or free-provider suggestions panel when Free is rate-limited with no services configured). When the fallback chain answered with an alternate service rather than the user's selected one, a small "Answered by …" label is shown under the assistant message naming the service that produced the response
- **Input**: text field, send/stop button, attachment button, file chip
- **Empty state**: CUI name on an empty new chat (no spinner). Letters use a 176 sp wordmark; the animated double-dot used as the `i` tittle is the same size as the standalone double-dot icon (52 dp) and sits on the stem
- **Hover tips**: sit **outside** the button (below when space allows, otherwise above). A thin transparent bridge keeps the tip from flickering when the pointer moves onto it; the tip surface does not cover the button
- **Speech input (STT)**: Windows desktop uses bundled Vosk models. A **中 / EN** control next to the microphone selects the recognizer; empty input defaults to Chinese. Typed CJK forces the Chinese model; a Latin-only draft can switch to English. After stop, a mismatched or replacement-character transcript is re-recognized with the other language model and replacement characters are stripped. Chinese transcripts also drop extra spaces between characters
- **Scrollbars (desktop)**: lists and long panes show a drag-able scrollbar (chat, history, war result table, settings, skills, MCP, sandbox, Kai Build, model picker)
- **Drag-and-drop**: supported for file attachments
- **History sheet**: bottom sheet listing saved conversations with title, date, active highlight, and delete
- **Auto score**: after a single-mode reply succeeds, the model receives an automatic score (word-count/time, completion, stability, quality). A user-set score is never overwritten

## Key Files

| File | Purpose |
|---|---|
| `composeApp/src/commonMain/.../data/Conversation.kt` | Conversation and message data classes, type constants |
| `composeApp/src/commonMain/.../data/ConversationStorage.kt` | In-memory conversation flow, transcript trimming, legacy migration |
| `composeApp/src/commonMain/.../data/ConversationPersistence.kt` | SQL and settings-blob persistence backends, staged-import handling |
| `composeApp/src/commonMain/sqldelight/com/inspiredandroid/kai/db/conversation.sq` | Conversation and message table schema and queries |
| `composeApp/src/commonMain/.../data/FileClassification.kt` | File category enum, MIME/extension classifier, size constants, file exceptions |
| `composeApp/src/commonMain/.../data/RemoteDataRepository.kt` | History management, conversation save/restore/delete, title derivation, message sending |
| `composeApp/src/commonMain/.../ui/chat/ChatViewModel.kt` | Chat UI state, send/retry/regenerate/cancel/loadConversation/deleteConversation actions |
| `composeApp/src/commonMain/.../ui/chat/ChatScreen.kt` | Chat UI composables, history sheet and heartbeat banner wiring |
| `composeApp/src/commonMain/.../ui/chat/composables/ChatHistorySheet.kt` | Bottom sheet listing saved conversations |
| `composeApp/src/commonMain/.../ui/chat/composables/HeartbeatBanner.kt` | Dismissable banner for heartbeat notifications |
| `composeApp/src/commonMain/.../ui/chat/composables/TopBar.kt` | Top bar with new chat, history, TTS, and settings icons |
| `composeApp/src/commonMain/.../ui/chat/composables/QuestionInput.kt` | Text input with send/stop button, STT language chip |
| `composeApp/src/commonMain/.../speech/SpeechLanguage.kt` | STT language resolve / cycle / transcript normalize |
| `composeApp/src/commonMain/.../ui/components/HoverTooltip.kt` | Hover popup tips without covering the button |
| `composeApp/src/commonMain/.../ui/components/DesktopScrollbar.kt` | Desktop drag-able scrollbars |
| `composeApp/src/commonMain/.../ui/components/CuiBranding.kt` | Empty-state CUI letters + animated i-dot |
| `composeApp/src/commonMain/.../data/TaskAutoScore.kt` | Post-task automatic scoring |
