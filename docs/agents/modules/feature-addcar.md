# feature/addcar 模块最小上下文

## 必读

- `feature/addcar/src/main/java/com/tx/carrecord/feature/addcar/data/CarRepository.kt`
- `feature/addcar/src/main/java/com/tx/carrecord/feature/addcar/domain/CarManagementRules.kt`
- `feature/addcar/src/main/java/com/tx/carrecord/feature/addcar/domain/CarManagementRuleModels.kt`
- `feature/addcar/src/main/java/com/tx/carrecord/feature/addcar/ui/AddCarManagementUi.kt`

## 仅当以下改动才读

- 改车辆切换/应用车型：补读 `docs/agents/runtime-contexts.md`。
- 改库表查询或写入：补读 `docs/agents/modules/core-database.md`。
- 改唯一性或删除级联语义：补读 `docs/agents/data-model.md`。

## 改完自检 3 条

- 删除车辆后当前应用车辆是否正确回退。
- 车型唯一性（同品牌同车型）是否保持。
- 车型级项目配置保存后禁用项隔离是否仅作用于目标车辆。
