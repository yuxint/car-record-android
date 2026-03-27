# Step 08 上下文文档：日期、应用车型与跨模块导航请求

## 1. 目标与范围
- 目标：定义 Android 运行时上下文，确保业务口径统一。
- 范围：DateContext、AppliedCarContext、NavigationRequestContext。
- 非范围：具体页面导航实现细节。

## 2. 前置依赖
- 主约束：`step-01-foundation-tech-decisions.md`
- 上一步：`step-07-json-backup-restore-compat.md`

## 3. 设计与实现清单
- DateContext
  - 统一 `now()` 入口。
  - 支持手动日期开关与手动日期持久化。
  - 统一日期格式化与 startOfDay 归一化策略。
- AppliedCarContext
  - `applied_car_id` 编解码与有效性解析。
  - 无效 ID 自动回退到第一辆车。
- NavigationRequestContext
  - 持久化目标 tab 与 nonce。
  - 通过 request API 发起跨模块跳转请求。
- 持久化位置
  - 统一放在 Datastore，避免散落 SharedPreferences 直接读写。

## 4. 验收标准
- 业务代码不直接调用系统时间作为业务“现在”。
- 应用车型与导航请求有统一入口。
- 完成定义（DoD）
  - 三类上下文接口定义齐全。
  - 键名、读写、回退策略文档化完成。
  - 调用约束（禁止绕过）文档化完成。

## 5. 风险与回滚
- 风险
  - 上下文分散存储导致状态不一致。
  - 手动日期未统一接入导致提醒逻辑偏差。
- 回滚策略
  - 强制入口收敛到 context API，发现绕过调用即整改。

## 6. 决策记录
- 候选方案对比
  - SharedPreferences 直连：快但散。
  - DataStore（推荐）：类型安全与一致性更好。
- 影响面
  - Context 统一层是后续 UI 和 domain 的依赖基础，需尽早锁定。
- 状态
  - 推荐 DataStore，待你确认后可回写 Step 01。
