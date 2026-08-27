# Collaboration mode

**Last verified:** 2026-08-27

## Overview

Collaboration sends one user question to every configured model whose **model-test total score is strictly greater than** a threshold you choose in the wizard. Each model runs in its own conversation using the same single-mode pipeline (tools, memory, plan, sandbox session). Results are stored under a three-level folder tree in chat history.

## Starting a task

1. In the chat screen, tap **协作模式** (not the main send box — this opens the wizard).
2. Enter your question → **下一步**.
3. Set the minimum test score (models with score **>** this value participate) → **下一步**.
4. Configure max wait per call (default 60s), retry count, failure alerts, and round-end alerts → **开始**.

## Chat history folders

| Level | Example | Contents |
|-------|---------|----------|
| 1 | `单一模式` / `协作模式` | Root folders |
| 2 | `2026-02-20-任务1` | One collaboration task |
| 3 | `opencode-hy3` | One model’s Q&A thread |

Level-3 rows show status color: yellow = running, green = completed, red = failed. Tap a model folder for the WeChat-style view (copy, retry, score slider). Each folder row has a copy button that exports formatted text for that branch.

## Settings

Collaboration settings only document participation rules. Run parameters are set in the wizard each time.

## Key Files

| File | Role |
|------|------|
| `composeApp/src/commonMain/.../data/collaboration/CollaborationTaskRunner.kt` | Parallel task orchestration |
| `composeApp/src/commonMain/.../data/collaboration/CollaborationModel.kt` | Config and wizard params |
| `composeApp/src/commonMain/.../data/ConversationFolderManager.kt` | Folder hierarchy |
| `composeApp/src/commonMain/.../data/ConversationCopyFormatter.kt` | Branch copy text |
| `composeApp/src/commonMain/.../ui/chat/composables/CollaborationWizardSheet.kt` | Wizard UI |
| `composeApp/src/commonMain/.../ui/chat/composables/ChatHistoryTreeSheet.kt` | History tree |
| `composeApp/src/commonMain/.../ui/chat/composables/CollaborationModelChatView.kt` | Per-model WeChat UI |
| `composeApp/src/commonMain/.../ui/settings/CollaborationSettings.kt` | Settings help text |
