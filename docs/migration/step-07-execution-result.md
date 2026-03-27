# Step 07 执行结果：JSON 备份恢复兼容

## 1. 已完成
- 在 `feature/my/domain` 完成与 iOS `MyDataTransferPayload` 对齐的数据契约模型：
  - `MyDataTransferPayload`
  - `MyDataTransferModelProfilePayload`
  - `MyDataTransferItemPayload`
  - `MyDataTransferVehiclePayload`
  - `MyDataTransferCarPayload`
  - `MyDataTransferLogPayload`
- 完成 JSON 兼容规则实现（`MyDataTransferRules`）：
  - 根节点严格校验：必须包含 `modelProfiles` 与 `vehicles`，且均为数组。
  - 日期严格校验：`purchaseDate` / `date` 仅接受 `yyyy-MM-dd`。
  - 导入流程计划：先校验并产出 `BackupImportPlan`，明确 `shouldClearBusinessDataBeforeImport = true`，供 Step 09 事务执行。
  - 项目映射规则：导入日志项目时优先按 `catalogKey` 匹配，再按项目名称匹配。
  - 校验覆盖：车型重复、缺少车型配置、车辆 ID 重复、记录 ID 重复、空项目、车型内项目名称重复、车型内 `catalogKey` 重复。
  - 导出稳定性：对车辆/项目/记录做确定性排序，降低跨端 diff 噪声。
- 完成规则单元测试（`MyDataTransferRulesTest`）：
  - 覆盖根节点字段缺失、车型重复、严格日期、`catalogKey` 优先映射、导出排序与导出项目 token 规则。

## 2. 契约与错误文案清单
- 根节点契约：`modelProfiles + vehicles`。
- 错误码与文案策略（与 iOS 现有口径对齐）：
  - `1001`：根节点结构错误（缺字段 / 类型错误）。
  - `1002`：车辆上路日期格式错误。
  - `1003`：保养日期格式错误。
  - `1004`：车辆 ID 重复/非法。
  - `1005`：保养记录 ID 重复/非法。
  - `1006`：保养项目为空。
  - `1007`：日志项目未在车型配置中声明。
  - `1008`：保养项目名称为空（含项目 ID 非法场景）。
  - `1009`：车型内项目名称重复。
  - `1010`：车型配置重复。
  - `1011`：车辆存在但缺少车型配置。
  - `1012`：车型内 `catalogKey` 重复。
  - `1013`：车辆引用的车型缺少配置。
  - `1014`：同车型重复车辆。

## 3. 双向兼容回归样本清单
- iOS -> Android：
  - 使用 iOS 导出样本（含 `catalogKey` + 名称混合日志项）应可通过 `decodePayload + planImport`。
  - 含非法日期（`2026-2-3`）样本应被严格拦截。
- Android -> iOS：
  - Android 导出结果应保持根节点与字段命名一致，日期为 `yyyy-MM-dd`。
  - 日志 `itemNames` 导出应优先输出 `catalogKey`，无 key 时回退名称。

## 4. 本步输出文件
- `feature/my/src/main/java/com/tx/carrecord/feature/my/domain/MyDataTransferRuleModels.kt`
- `feature/my/src/main/java/com/tx/carrecord/feature/my/domain/MyDataTransferRules.kt`
- `feature/my/src/test/java/com/tx/carrecord/feature/my/domain/MyDataTransferRulesTest.kt`
- `docs/migration/step-07-execution-result.md`

## 5. 验证说明
- 已完成静态自检：
  - 导入流程保持“先校验、后清空、再写入”的事务友好形态（具体事务将在 Step 09 接入）。
  - 映射规则保持“`catalogKey` 优先，名称兜底”。
  - 日期规则使用严格 `yyyy-MM-dd` 校验。
- 未完成自动化执行：
  - 当前仓库缺少 `car-record-android/gradlew`，无法本地执行 `:feature:my:testDebugUnitTest`。

## 6. 风险提示
- 当前为 domain 规则与契约层，尚未接入 Room 事务与 DAO，真正的“清空+写入+回滚”在 Step 09 落地。
- iOS 侧 `JSONEncoder` 的可选字段输出细节（`null`/省略）在集成联调时建议再用真实样本做一轮比对。
