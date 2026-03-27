# iOS -> Android 规则映射表（迁出期保留）

> 用途：Step 12 风险控制要求，作为迁出后的规则核对基线。

| 规则主题 | iOS 口径 | Android 对应实现 | 当前状态 |
| --- | --- | --- | --- |
| 同车同日唯一键 | `cycleKey` 同车同日唯一 | `feature/records/domain/RecordsDomainRules.kt` | 已对齐 |
| 同车同日同项目唯一键 | `cycleItemKey` 唯一 | `feature/records/domain/RecordsDomainRules.kt` | 已对齐 |
| 关系键持久化 | `itemIDsRaw` 为 UUID 列表 | `feature/records/domain/RecordSaveRuleModels.kt` + `RecordsDomainRules.kt` | 已对齐 |
| 提醒进度规则 | 时间/里程谁先到采用谁 | `feature/reminder/domain/ReminderRules.kt` | 已对齐 |
| 备份导入映射 | 优先 `catalogKey` | `feature/my/domain/MyDataTransferRules.kt` | 已对齐 |
| 当前应用车辆回退 | 失效 ID 回退第一辆车 | `feature/my/domain/CarManagementRules.kt` | 已对齐 |
| 当前时间上下文 | 统一上下文入口，不直取系统时间 | `core/datastore/AppDateContext.kt` + `DatastoreAppDateContext.kt` | 已对齐 |
| 跨 Tab 导航请求 | 请求单次消费 | `core/datastore/AppNavigationContext.kt` + `DatastoreAppNavigationContext.kt` + `app/ui/AppShellViewModel.kt` | 已对齐 |

## 验证来源
- `docs/migration/step-11-execution-result.md`
- 各模块规则层测试：
  - `feature/reminder/src/test/.../ReminderRulesTest.kt`
  - `feature/records/src/test/.../RecordsDomainRulesTest.kt`
  - `feature/my/src/test/.../MyDataTransferRulesTest.kt`
  - `feature/my/src/test/.../CarManagementRulesTest.kt`
