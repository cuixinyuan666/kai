# Collaboration mode

**Last verified:** 2026-08-31

## Overview

Collaboration sends one user question to every configured model whose **model-test total score is at least** a threshold you choose in the wizard (default **50**, inclusive). Each model runs in its own conversation using the same single-mode pipeline (tools, memory, plan, sandbox session). Results are stored under a three-level folder tree in chat history.

For multi-round analysis and per-aspect voting, use **战争模式** (see [war-mode.md](war-mode.md)).

## Starting a task

1. In the chat screen, tap **协作模式** (not the main send box — this opens the wizard).
2. Enter your question → **下一步** (the wizard input has no send button).
3. Set the minimum test score (models with score **≥** this value participate; default 50) → **下一步**.
4. Configure max wait per call (default 60s). Wait means **no output at all** in that window; a model that is still streaming after the first byte is not cut off. Also set retry count, failure alerts, and round-end alerts → **开始**.

## Chat history folders

| Level | Example | Contents |
|-------|---------|----------|
| 1 | `单一模式` / `协作模式` | Root folders |
| 2 | `2026-02-20-任务1` | One collaboration task |
| 3 | `opencode-hy3` | One model’s Q&A thread |

Level-3 rows show status color: yellow = running, green = completed, red = failed. Tap a model folder for the WeChat-style view of the **full thread** (every user and assistant turn), plus copy, retry, and a score slider. Each folder row has a copy button that exports formatted text for that branch.

When a task **starts**, the chat history sheet opens automatically inside that task’s folder. When it **finishes**, the task is marked completed (green) and the history sheet stays on that folder so you can open model threads immediately.

Each model’s finished answer writes an automatic score (word-count/time, completion, stability, quality). A score the user set by hand is never overwritten.

## Folder attachments

Attaching a folder does **not** inline the whole tree into every model request. Each model receives the folder’s absolute path, a short directory summary, and an instruction to read files with `execute_shell_command`. Loose files picked alongside a folder are still attached as usual. `.git`, `node_modules`, `build`, and similar directories are omitted from the summary.

## Settings

Collaboration settings only document participation rules. Run parameters are set in the wizard each time.

## Key Files

| File | Role |
|------|------|
| `composeApp/src/commonMain/.../data/collaboration/CollaborationTaskRunner.kt` | Parallel task orchestration |
| `composeApp/src/commonMain/.../data/FolderAttachments.kt` | Folder path prefix; skip junk dirs |
| `composeApp/src/commonMain/.../data/collaboration/CollaborationModel.kt` | Config and wizard params |
| `composeApp/src/commonMain/.../data/ConversationFolderManager.kt` | Folder hierarchy |
| `composeApp/src/commonMain/.../data/ConversationCopyFormatter.kt` | Branch copy text |
| `composeApp/src/commonMain/.../ui/chat/composables/CollaborationWizardSheet.kt` | Wizard UI |
| `composeApp/src/commonMain/.../ui/chat/composables/ChatHistoryTreeSheet.kt` | History tree |
| `composeApp/src/commonMain/.../ui/chat/composables/CollaborationModelChatView.kt` | Per-model WeChat UI |
| `composeApp/src/commonMain/.../ui/settings/CollaborationSettings.kt` | Settings help text |
| `composeApp/src/commonMain/.../data/TaskAutoScore.kt` | Post-answer automatic scoring |
