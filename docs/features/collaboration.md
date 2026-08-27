# Collaboration Mode

**Last verified:** 2026-08-27

Collaboration mode sends the **same user instruction in parallel** to every configured model whose **model-test total score is strictly greater than 0**. There are no task-party or supervisor roles—each model answers independently. Windows (desktop) and Android share the same implementation in `composeApp` common code.

## Eligibility

Only models with benchmark **total score > 0** participate. Run **模型测试** in Settings first; models that fail the probe (no response) receive **0** and are excluded.

## Round 1

The user question is sent identically to all eligible models in parallel.

## Follow-up rounds

After a round completes, if at least one model succeeded, the user can tap **下一轮**. Only models that **successfully answered in the previous round** receive the next prompt, formatted as:

```
【原始问题】
{question}

【你上一次的回答】
{previous answer}

【审阅要求】
请审阅你上一次的回答是否存在问题。若存在问题请说明并修正你的回答；若认为没有问题请明确回复「没有问题」。
```

Users may continue for unlimited rounds while successful models remain.

## Settings

| Parameter | Default | Purpose |
|---|---|---|
| Max wait (seconds) | 60 | Per-model call timeout |
| Retry count | 2 | Retries after failure |
| Model system prompt | built-in default | System prompt for all models |

## Copy all

**一键复制** exports the user question plus every round’s model replies (multi-round content).

## Chat visibility

- The user question appears at the top of the collaboration panel.
- **Overview** shows all models; each **model tab** filters to that model’s events.
- Each round’s summary is appended to chat history as an assistant message.

## Release builds

Tagged releases (`v*`) publish Windows (`.zip` portable + `.msi`) and Android (`.apk`) to GitHub Releases via `.github/workflows/release.yml`.

## Key Files

| File | Purpose |
|---|---|
| `composeApp/src/commonMain/.../data/collaboration/CollaborationModel.kt` | Config, events, follow-up and copy helpers |
| `composeApp/src/commonMain/.../data/collaboration/CollaborationOrchestrator.kt` | Parallel broadcast and multi-round engine |
| `composeApp/src/commonMain/.../ui/chat/composables/CollaborationPanel.kt` | Overview, per-model views, copy, next round |
| `composeApp/src/commonMain/.../ui/settings/CollaborationSettings.kt` | Timeout and prompt configuration |
| `.github/workflows/release.yml` | Windows zip/MSI + Android APK GitHub Releases |
