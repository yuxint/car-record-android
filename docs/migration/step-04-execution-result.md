# Step 04 执行结果：保养提醒规则

## 1. 已完成
- 在 `feature/reminder/domain` 完成规则输入输出模型定义：
  - `ReminderCarSnapshot`
  - `ReminderItemOptionSnapshot`
  - `ReminderRecordSnapshot`
  - `ReminderRow`
  - `ReminderProgressColorLevel`
- 完成提醒规则纯函数实现（`ReminderRules`）：
  - 进度规则：时间/里程同时存在时取更高进度（谁先到采用谁）。
  - 基线规则：有历史记录使用最近记录日期+里程；无记录使用购车日期+0 里程。
  - 颜色规则：`NORMAL / WARNING / DANGER`。
  - 排序规则：先按进度降序，再按优先级，再按项目名。
  - 边界规则：未启用/间隔无效回退“未设置提醒规则”；负进度与超 100% 场景已覆盖。
- 完成规则单元测试（`ReminderRulesTest`）：
  - 覆盖谁先到、无记录基线、无效间隔、超 100% 展示、最新记录索引、排序规则。

## 2. 本步输出文件
- `feature/reminder/src/main/java/com/tx/carrecord/feature/reminder/domain/ReminderRuleModels.kt`
- `feature/reminder/src/main/java/com/tx/carrecord/feature/reminder/domain/ReminderRules.kt`
- `feature/reminder/src/test/java/com/tx/carrecord/feature/reminder/domain/ReminderRulesTest.kt`
- `feature/reminder/build.gradle.kts`（新增 `testImplementation(kotlin("test"))`）

## 3. 验证说明
- 已完成静态自检：
  - 规则函数无数据层/UI 依赖，保持纯函数形态。
  - 输入/输出模型与规则职责分离，便于后续集成与单测。
- 未完成自动化执行：
  - 当前仓库缺少 `car-record-android/gradlew`，无法在本地直接执行 `:feature:reminder:testDebugUnitTest`。

## 4. 风险提示
- 当前时间上下文（manual now / applied car）将在 Step 08 接入；本步规则通过参数注入 `now`，避免散落系统时间调用。
- 当前里程文案未接入 iOS 的“万公里”格式化策略，后续 UI 集成阶段需确认显示口径是否继续按 iOS 细节对齐。
