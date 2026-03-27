# Step 11 执行结果：回归测试矩阵与一致性检查

## 1. 执行范围与结论
- 已按 Step 11 文档对回归矩阵逐项执行“用例映射 + 证据核对”。
- 当前结论：
  - 核心规则与数据约束：已有单测覆盖并通过静态核对。
  - 双向 JSON 兼容：已有规则层单测覆盖关键路径与异常口径。
  - 上下文一致性：`applied car` 回退有单测覆盖；`manual now` 与导航请求为实现核对，暂缺自动化用例。
- 不可接受差异：0（按当前可验证范围）。

## 2. 回归矩阵执行记录
- 数据约束（`cycleKey` / `cycleItemKey` 唯一性）
  - 证据：`feature/records/src/test/.../RecordsDomainRulesTest.kt`
  - 覆盖点：同日归一化冲突识别、关系键生成、编辑忽略自身。
- 规则（提醒计算 / 排序 / 颜色分段）
  - 证据：`feature/reminder/src/test/.../ReminderRulesTest.kt`
  - 覆盖点：时间里程谁先到、无历史记录基线、颜色分段、排序规则、超 100% 展示。
- 同步（保存去重 / 关系重建 / 里程联动）
  - 证据：`feature/records/src/test/.../RecordsDomainRulesTest.kt`
  - 覆盖点：`itemIDsRaw` 去重保序、关系草稿重建、里程取 `max` 联动。
- JSON（导入导出兼容 / 异常校验）
  - 证据：`feature/my/src/test/.../MyDataTransferRulesTest.kt`
  - 覆盖点：根节点校验、车型重复、日期严格格式、`catalogKey` 优先映射、导出稳定排序。
- 上下文（manual now / applied car 回退 / 导航请求）
  - `applied car` 回退：
    - 证据：`feature/my/src/test/.../CarManagementRulesTest.kt` 的 `resolveAppliedCar_*`。
  - `manual now` / 导航请求：
    - 证据：`core/datastore` 实现代码与 Step 08 执行结果文档。
    - 现状：暂缺独立单测，已列入风险。

## 3. 一致性检查（对 iOS 规则口径）
- 已对照迁移文档中 iOS 既定口径进行检查：
  - 一致项：
    - 同车同日唯一键约束（`cycleKey` / `cycleItemKey`）。
    - 提醒规则“时间/里程谁先到采用谁”。
    - JSON 导入导出结构与 `catalogKey` 优先映射策略。
    - 应用车型失效回退策略（失效 ID 回退第一辆车，无车回退空值）。
  - 可接受差异：
    - 0。
  - 不可接受差异：
    - 0。

## 4. DoD 对照
- 测试矩阵覆盖关键业务路径：是（基于现有规则层单测与实现核对）。
- 验收清单可执行且结果可追溯：是（本文件记录了映射路径与证据）。
- 不可接受差异为 0：是（按当前可验证范围）。

## 5. 自动化执行与限制
- 当前环境未执行 Gradle 自动化测试，原因：
  - 仓库缺少 `car-record-android/gradlew`。
  - 本机无 `gradle` 命令。
- 影响：
  - 本次结论基于“测试代码覆盖核对 + 实现一致性核对”，不等同于一次真实 CI 绿灯。

## 6. 风险与后续
- 风险
  - `manual now`、导航请求缺少独立自动化回归用例。
  - 未执行真实测试任务，可能遗漏编译/依赖层问题。
- 建议
  - 在 Step 12 前补齐 `core/datastore` 上下文测试（至少覆盖手动日期切换与导航请求消费）。
  - 补充 Gradle Wrapper 后执行一次最小回归：
    - `:feature:records:test`
    - `:feature:reminder:test`
    - `:feature:my:test`
