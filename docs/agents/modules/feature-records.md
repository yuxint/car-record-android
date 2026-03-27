# feature/records 模块最小上下文

## 必读

- `feature/records/src/main/java/com/tx/carrecord/feature/records/domain/RecordsDomainRules.kt`
- `feature/records/src/main/java/com/tx/carrecord/feature/records/domain/RecordSaveRuleModels.kt`
- `feature/records/src/main/java/com/tx/carrecord/feature/records/data/RecordRepository.kt`
- `feature/records/src/main/java/com/tx/carrecord/feature/records/ui/RecordsUiPlaceholder.kt`

## 仅当以下改动才读

- 改唯一键/关系同步：补读 `docs/agents/data-model.md` 与 `docs/agents/modules/core-database.md`。
- 改按当前车辆或日期行为：补读 `docs/agents/runtime-contexts.md` 与 `docs/agents/modules/core-datastore.md`。
- 改导航回跳或入口联动：补读 `docs/agents/modules/app.md`。

## 改完自检 3 条

- `cycleKey` 与 `cycleItemKey` 生成是否一致。
- 编辑保存后是否会误报重复或丢项目关系。
- 列表查询与保存后刷新是否按当前车辆生效。
