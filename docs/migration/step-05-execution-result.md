# Step 05 执行结果：保养记录保存与同步规则

## 1. 已完成
- 在 `feature/records/domain` 完成记录保存规则输入输出模型定义：
  - `RecordSaveInput`
  - `ExistingRecordSnapshot`
  - `RecordItemRelationDraft`
  - `RecordSavePlan`
  - `RecordSaveDecision`
- 完成记录保存规则纯函数实现（`RecordsDomainRules`）：
  - 周期键规则：`cycleKey = carId|yyyy-MM-dd`，日期按 `LocalDate` 归一化。
  - 周期项目键规则：`cycleItemKey = cycleKey|itemId`。
  - 项目去重规则：`itemIDsRaw` 按 `|` 分隔去重并保序后回写。
  - 重复周期拦截：新增/编辑都校验同车同日冲突，编辑场景忽略自身记录。
  - 关系重建：按去重后的项目集合重建关系草稿，避免重复关系。
  - 记录 ID 对齐：保存计划显式输出 `targetRecordId`，保证主记录与关系重建可对齐。
  - 里程联动：保存计划输出 `max(车辆当前里程, 表单里程)`，防止里程回退。
- 完成规则单元测试（`RecordsDomainRulesTest`）：
  - 覆盖同日归一化、重复周期冲突、编辑忽略自身、项目去重保序与关系键生成。

## 2. 本步输出文件
- `feature/records/src/main/java/com/tx/carrecord/feature/records/domain/RecordSaveRuleModels.kt`
- `feature/records/src/main/java/com/tx/carrecord/feature/records/domain/RecordsDomainRules.kt`
- `feature/records/src/test/java/com/tx/carrecord/feature/records/domain/RecordsDomainRulesTest.kt`
- `feature/records/build.gradle.kts`（新增 `testImplementation(kotlin("test"))`）

## 3. 验证说明
- 已完成静态自检：
  - `cycleKey` 与 `cycleItemKey` 计算规则与 Step 05 文档一致。
  - `itemIDsRaw` 进入保存计划前完成去重保序。
  - 冲突分支输出业务层可识别的 `DuplicateCycleConflict`。
  - 关系草稿来自去重后的项目集合，不会生成重复 `cycleItemKey`。
- 未完成自动化执行：
  - 当前仓库缺少 `car-record-android/gradlew`，无法在本地直接执行 `:feature:records:testDebugUnitTest`。

## 4. 风险提示
- 当前实现为纯 domain 规则，事务包裹与 Room 异常映射将在数据层集成步骤（Step 09）接入。
- 当前时间上下文（manual now / applied car）将在 Step 08 接入；本步使用参数注入日期，不散落系统时间调用。
