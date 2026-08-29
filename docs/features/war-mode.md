# War mode (战争模式)

**Last verified:** 2026-08-29

## Overview

War mode runs a **two-round multi-model workflow**: every eligible model answers the same task in parallel (round 1), a **summary model** extracts common ground and disputed aspects, then each successful model votes agree/disagree on each disputed aspect in one batched prompt (round 2). Results show per-aspect tallies such as **同意 3/5 · 不同意 2/5**.

Collaboration mode only broadcasts one question and stores per-model threads. War mode adds analysis and voting with a task-level result dashboard.

## Starting a task

1. Tap **战争模式** in the chat top bar (gavel icon, separate from **协作模式**).
2. Enter your task → **下一步**.
3. Set the minimum model-test score threshold → **下一步**.
4. Configure wait time, retries, notifications, and optionally override the **summary model** (default: highest-scored participant) → **开始**.

## Chat history folders

| Level | Example | Contents |
|-------|---------|----------|
| 1 | `单一模式` / `协作模式` / `战争模式` | Root folders |
| 2 | `2026-08-29-任务1` | One war task |
| 3 | `任务结果` + model folders | Result metadata + per-model R1/R2 threads |

Tapping a war task opens the **任务结果** view (common points, disputed aspects, vote breakdown). Model folders open the same WeChat-style thread view as collaboration.

## Summary model

By default the participant with the **highest model-test total score** performs JSON analysis. You can pick another eligible model in the wizard.

## Key Files

| File | Role |
|------|------|
| `composeApp/src/commonMain/.../data/war/WarTaskRunner.kt` | Three-phase orchestration |
| `composeApp/src/commonMain/.../data/war/WarModel.kt` | Params, phases, result types |
| `composeApp/src/commonMain/.../data/war/WarAnalysisParser.kt` | JSON parse + vote aggregation |
| `composeApp/src/commonMain/.../data/war/WarPromptBuilder.kt` | Analysis and vote prompts |
| `composeApp/src/commonMain/.../data/war/WarCopyFormatter.kt` | Copy report text |
| `composeApp/src/commonMain/.../data/collaboration/CollaborationSupport.kt` | Shared eligible-model + retry helpers |
| `composeApp/src/commonMain/.../ui/chat/composables/WarWizardSheet.kt` | Wizard UI |
| `composeApp/src/commonMain/.../ui/chat/composables/WarResultView.kt` | Task result dashboard |
