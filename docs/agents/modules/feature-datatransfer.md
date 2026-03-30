# feature/datatransfer 模块最小上下文

## 必读

- `feature/datatransfer/src/main/java/com/tx/carrecord/feature/datatransfer/data/BackupRepository.kt`
- `feature/datatransfer/src/main/java/com/tx/carrecord/feature/datatransfer/domain/MyDataTransferRules.kt`
- `feature/datatransfer/src/main/java/com/tx/carrecord/feature/datatransfer/domain/MyDataTransferRuleModels.kt`
- `feature/datatransfer/src/main/java/com/tx/carrecord/feature/datatransfer/domain/MyDataTransferTimeCodec.kt`
- `feature/datatransfer/src/main/java/com/tx/carrecord/feature/datatransfer/ui/DataTransferUi.kt`

## 仅当以下改动才读

- 改备份结构：补读 `docs/agents/data-model.md`。
- 改恢复策略（清空再导入等）：补读 `docs/agents/business-constraints.md`。

## 改完自检 3 条

- 恢复流程是否仍“先清空再导入”且计数正确。
- 导入导出结构变更是否同步规则与测试。
- 恢复完成后当前应用车辆是否仍会规范化回退到可用车辆。
