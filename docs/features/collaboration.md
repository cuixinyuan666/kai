# Collaboration Mode

**Last verified:** 2026-08-26

Collaboration mode runs a multi-model workflow on a single user question. **Task parties** and **supervisors** converse directly—there is no transmitter or feedback role. For each task party and each supervisor, the app creates an **isolated chat session** with its own history (e.g. task 1 with supervisor 1, task 1 with supervisor 2, …). Windows (desktop) and Android share the same implementation in `composeApp` common code.

## Roles

| Role | Count | Responsibility |
|---|---|---|
| Task party | One or more | Answer the user question; respond to supervisor questions in dialogue |
| Supervisor | One or more | Review task-party answers; ask follow-up questions per isolated session |

Role assignment can be **manual** (pick models in Settings → Collaboration) or **score-gated** (only benchmark-qualified models participate; task/supervisor split by ratio).

## Isolated sessions

If you configure 2 task parties and 3 supervisors, the orchestrator creates **6 independent sessions**:

- Task 1 ↔ Supervisor 1, Task 1 ↔ Supervisor 2, Task 1 ↔ Supervisor 3
- Task 2 ↔ Supervisor 1, Task 2 ↔ Supervisor 2, Task 2 ↔ Supervisor 3

Messages in one session never mix with another.

## Dialogue flow (per session)

1. The task party submits an answer to the supervisor.
2. The supervisor reviews and asks questions.
3. The system relays to the task party:  
   `针对你的上次回答，监督方提出的问题是：XXX，你对此的回答是什么？`
4. The task party replies; the system relays to the supervisor:  
   `针对你的上一次疑问，任务方的回答是：XXX，你对此还有哪些疑问？`
5. Steps 2–4 repeat until the supervisor sends a termination phrase or `maxRounds` is reached.

## Session termination

The orchestrator scans supervisor replies for end-of-dialogue keywords such as **没有问题**, **可以完成**, **确认**, **没有疑问**, etc. When `autoStopOnConfirm` is enabled (default), matching replies end that session automatically.

## Chat visibility

- The user question is stored in chat history and shown at the top of the collaboration panel.
- **Overview** shows all sessions; each **session tab** filters to that task party × supervisor pair.
- When collaboration finishes, a summary is appended to chat history as an assistant message.

## Release builds

Tagged releases (`v*`) publish **Windows** (`.zip` portable + `.msi`) and **Android** (`.apk`) to GitHub Releases via `.github/workflows/release.yml`.

## Key Files

| File | Purpose |
|---|---|
| `composeApp/src/commonMain/.../data/collaboration/CollaborationModel.kt` | Config, events, relay format helpers |
| `composeApp/src/commonMain/.../data/collaboration/CollaborationOrchestrator.kt` | Per-session dialogue engine |
| `composeApp/src/commonMain/.../data/collaboration/CollaborationExtractors.kt` | Termination keyword detection |
| `composeApp/src/commonMain/.../ui/chat/composables/CollaborationPanel.kt` | Overview + per-session views |
| `composeApp/src/commonMain/.../ui/settings/CollaborationSettings.kt` | Role and prompt configuration |
| `.github/workflows/release.yml` | Windows zip/MSI + Android APK GitHub Releases |
