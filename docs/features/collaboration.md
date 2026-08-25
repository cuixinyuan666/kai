# Collaboration Mode

**Last verified:** 2026-08-25

Collaboration mode runs a multi-model, multi-role workflow on a single user question. Task parties execute in parallel; a transmitter compacts their outputs for supervisors; supervisors evaluate each task party; a feedback role redistributes supervisor replies back to task parties for the next round. Windows (desktop) and Android share the same implementation in `composeApp` common code.

## Roles

| Role | Count | Responsibility |
|---|---|---|
| Task party | One or more | Execute the user question; improve from feedback on later rounds |
| Transmitter | One | Compact task-party outputs for supervisors (no judging) |
| Supervisor | One or more | Independently evaluate each task party; reply `确认` or give corrections |
| Feedback | One | Aggregate supervisor replies and route them back per task party |

Role assignment can be **manual** (pick models in Settings → Collaboration) or **score-gated** (only benchmark-qualified models participate; task/supervisor split by ratio).

## Chat visibility

After a collaboration task starts:

- The user question is stored in chat history and shown at the top of the collaboration panel.
- The **Overview** tab shows the full per-round process tree (all roles).
- **Task / Transmitter / Supervisor / Feedback** tabs filter the same event stream to that role’s status lines and full answer blocks, so each role’s independent view stays in sync with the main process log.
- When collaboration finishes, the final feedback summary is appended to chat history as an assistant message.

## Prompts & extraction

Each role has a default system prompt (customizable under Settings → Collaboration → Prompts):

- Task party, transmitter, supervisor, and feedback each have dedicated prompts with strict output formats.
- Transmitter summarizes only; supervisor prompts require `对任务方N的回复：…；评估：…` per task party.
- Feedback prompts require `任务方N：` sections listing each supervisor’s reply.
- The orchestrator uses shared extractors to pull per-task-party segments from supervisor and feedback outputs, drive confirmation checks, and pass feedback into the next task-party round.

## Release builds

Tagged releases (`v*`) publish Windows (`.msi`) and Android (`.apk`) assets to GitHub Releases via `.github/workflows/release.yml`, alongside other platform packages.

## Key Files

| File | Purpose |
|---|---|
| `composeApp/src/commonMain/.../data/collaboration/CollaborationModel.kt` | Config, events, default prompts |
| `composeApp/src/commonMain/.../data/collaboration/CollaborationOrchestrator.kt` | Multi-round workflow engine |
| `composeApp/src/commonMain/.../data/collaboration/CollaborationExtractors.kt` | Structured output parsing |
| `composeApp/src/commonMain/.../ui/chat/composables/CollaborationPanel.kt` | Overview + per-role views |
| `composeApp/src/commonMain/.../ui/settings/CollaborationSettings.kt` | Role and prompt configuration |
| `.github/workflows/release.yml` | Windows MSI + Android APK GitHub Releases |
