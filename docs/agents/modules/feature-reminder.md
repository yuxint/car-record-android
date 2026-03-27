# feature/reminder 模块最小上下文

## 必读

- `feature/reminder/src/main/java/com/tx/carrecord/feature/reminder/domain/ReminderRules.kt`
- `feature/reminder/src/main/java/com/tx/carrecord/feature/reminder/domain/ReminderRuleModels.kt`
- `feature/reminder/src/main/java/com/tx/carrecord/feature/reminder/data/ReminderRepository.kt`
- `feature/reminder/src/main/java/com/tx/carrecord/feature/reminder/ui/ReminderUiPlaceholder.kt`

## 仅当以下改动才读

- 改“今天/日期换算/到期”语义：补读 `docs/agents/runtime-contexts.md` 与 `docs/agents/modules/core-datastore.md`。
- 改阈值或默认项目规则：补读 `docs/agents/data-model.md`。
- 改提醒后跳转记录页行为：补读 `docs/agents/modules/app.md`。

## 改完自检 3 条

- 进度规则是否仍为“时间/里程谁先到”。
- 是否仍统一走时间上下文。
- 当前车辆切换后提醒列表是否按车辆隔离。
