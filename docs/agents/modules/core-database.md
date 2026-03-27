# core/database 模块最小上下文

## 必读

- `core/database/src/main/java/com/tx/carrecord/core/database/model/CarEntity.kt`
- `core/database/src/main/java/com/tx/carrecord/core/database/model/MaintenanceRecordEntity.kt`
- `core/database/src/main/java/com/tx/carrecord/core/database/model/MaintenanceRecordItemEntity.kt`
- `core/database/src/main/java/com/tx/carrecord/core/database/model/MaintenanceItemOptionEntity.kt`
- `core/database/src/main/java/com/tx/carrecord/core/database/dao/CarRecordDao.kt`
- `core/database/src/main/java/com/tx/carrecord/core/database/room/CarRecordDatabase.kt`

## 仅当以下改动才读

- 改唯一键/关系/删除策略：补读 `docs/agents/data-model.md`。
- 改错误映射策略：看 `error/RoomRepositoryErrorMapper.kt` 并补读 `docs/agents/business-constraints.md`。
- 改模块依赖或 Room 配置：补读 `docs/agents/gradle-wiring-rules.md`。

## 改完自检 3 条

- `cycleKey` 与 `cycleItemKey` 约束是否仍生效。
- 删除与恢复路径是否保持历史语义。
- Entity/DAO 变化是否同步评估测试与兼容性。
