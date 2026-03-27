# 数据模型与持久化约束

## 关键实体（最小字段）

- `CarEntity`：`id`、`disabledItemIDsRaw`
- `MaintenanceRecordEntity`：`id`、`carID`、`itemIDsRaw`、`cycleKey(unique)`
- `MaintenanceRecordItemEntity`：`id`、`recordID(FK)`、`itemID`、`cycleItemKey(unique)`
- `MaintenanceItemOptionEntity`：`id`、`ownerCarID`、`catalogKey`

## 不可破坏规则

- “同车同日唯一”由 `MaintenanceRecordEntity.cycleKey` 保证。
- “同车同日同项目唯一”由 `MaintenanceRecordItemEntity.cycleItemKey` 保证。
- `itemIDsRaw` 持久化的是项目 ID 列表，不是展示名称。
- `MaintenanceItemOptionEntity` 通过 `ownerCarID` 做车辆隔离。
- 新增实体或字段时，要同步检查 `CarRecordDatabase`、`CarRecordDao` 与 Room schema 输出。

## 删除/恢复边界

- 删除车辆：应级联或显式清理该车记录与 `ownerCarID` 命中的项目选项。
- 清空业务数据：需覆盖车辆、项目、记录与记录-项目关系四类核心数据。
- 数据恢复优先使用结构化 JSON 恢复；仅在明确场景才允许整库覆盖。

## 兼容性提示

- 涉及 Entity 字段语义变化时，必须同步评估 migration 与历史备份兼容。
- 涉及导入导出结构变化时，必须同步评估 `feature/my` 的规则与测试。
