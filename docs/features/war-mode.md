# War mode (战争模式)

**Last verified:** 2026-08-31

War mode runs a **multi-round multi-model workflow**. Using three participant models as “我 / 你 / 他”:

1. **Answer round:** each participant proposes solutions. A **summary model** extracts **common points** (e.g. 方案 A、方案 C) and **disputed solutions** (e.g. 方案 B from 我/他, 方案 D from 你), including who proposed each dispute.
2. **First cross-vote round:** each disputed solution is sent only to models that did **not** propose it (方案 D → 我 and 他; 方案 B → 你). Each model answers agree / disagree plus a reason.
3. **Later vote rounds:** the same cross-dispatch, but each prompt also includes the **previous round’s agree/disagree reasons** for those solutions.
4. After the wizard **vote-round count** (default **2**, counting only the vote rounds after analysis), the summary model writes a **final recap** of all solutions.

The result page shows a vote table (solutions across the top, rounds down the side) with ratios such as `3/3`, `2/3-`, `2/3↑`, `1/3↓`. Clicking a ratio lists the models that voted that round, including proposers who were **skipped** (shown as 提出方（不投票）) so a model that put forward every remaining dispute still appears in history and in the table. Clicking a model jumps to that model’s reply in the thread. Collaboration mode only broadcasts one question and stores per-model threads.

## Starting a task

1. Tap **战争模式** in the chat top bar (gavel icon, separate from **协作模式**).
2. Enter your task → **下一步**.
3. Set the minimum model-test score (models with score **≥** this value participate; default **50**) → **下一步**.
4. Configure wait time (timeout means **no output at all** during the wait, not “still generating”), retries, **vote rounds** (default **2**), notifications, and optionally override the **summary model** (default: highest-scored first, then the next if that one fails) → **开始**.

## Chat history folders

| Level | Example | Contents |
|-------|---------|----------|
| 1 | `单一模式` / `协作模式` / `战争模式` | Root folders |
| 2 | `2026-08-29-任务1` | One war task |
| 3 | `任务结果` + model folders | Result metadata + per-model threads, including a **总结** summary-model thread |

Tapping a war task opens the **任务结果** view (common points, vote table, final recap). Model names use parent + child chips. The summary model’s answers use a distinct color and type style in the chat thread. The result screen uses high-contrast text and icons (including the back arrow) on the current theme background. Each model folder shows the **full thread**, including a skip note when that model was the proposer of every remaining dispute in a vote round. Vote tallies on the result page refresh as each model finishes voting.

Inside a war task folder, the model list sorts by **parent model name** (A→Z / Z→A), not by time. War-mode root still lists tasks by time. After each model’s answer round, an automatic score is stored unless the user already set a custom score.

When a war task **starts** or **finishes**, the chat history sheet opens on that task’s folder and the task is marked completed when done. The wizard input has no send button; start from **开始**. Max wait is an idle/no-output timeout: streaming that already produced bytes is allowed to finish.

## Vote rounds

- **Vote round 1:** cross-dispatch disputed solutions to non-proposers; collect agree / disagree and reasons. A model that proposed every remaining dispute does not receive a vote prompt; its thread still records the skip, and the table lists it as 提出方（不投票）.
- **Vote round 2 and later:** same cross-dispatch, and the prompt includes the previous round’s reasons for those solutions.
- **Vote round 2 and later:** same cross-dispatch, and the prompt includes the previous round’s reasons for those solutions.
- After the configured vote-round count, the summary model produces a final recap. The copyable report includes the vote table, per-round model stances, and that recap.

## Folder attachments

Same as collaboration: a folder is passed as an absolute path plus a short listing, not as thousands of inlined files. Models should open files with `execute_shell_command`. Vote rounds do not re-attach the folder.

## Summary model

By default participants are tried **from highest model-test score to lowest**. If the current summary model fails to return valid analysis JSON, the next-highest is used, and so on. A wizard override is tried first, then the remaining models by score. Each candidate is attempted twice before falling through. Analysis text and the final recap are stored on a dedicated summary-model conversation so they appear in chat history.

## Key Files

| File | Role |
|------|------|
| `composeApp/src/commonMain/.../data/war/WarTaskRunner.kt` | Answer round, cross-vote rounds, final recap |
| `composeApp/src/commonMain/.../data/FolderAttachments.kt` | Folder path prefix shared with collaboration |
| `composeApp/src/commonMain/.../data/war/WarModel.kt` | Params, phases, result types |
| `composeApp/src/commonMain/.../data/war/WarAnalysisParser.kt` | JSON parse + vote aggregation |
| `composeApp/src/commonMain/.../data/war/WarPromptBuilder.kt` | Analysis, cross-vote, and final-recap prompts |
| `composeApp/src/commonMain/.../data/war/WarVoting.kt` | Cross-dispatch (skip proposers) and vote-table trend marks |
| `composeApp/src/commonMain/.../data/war/WarCopyFormatter.kt` | Copy report text |
| `composeApp/src/commonMain/.../data/collaboration/CollaborationSupport.kt` | Shared eligible-model + retry helpers |
| `composeApp/src/commonMain/.../ui/chat/composables/WarWizardSheet.kt` | Wizard UI |
| `composeApp/src/commonMain/.../ui/chat/composables/WarResultView.kt` | Task result dashboard and vote table |
| `composeApp/src/commonMain/.../data/TaskAutoScore.kt` | Post-answer automatic scoring |
