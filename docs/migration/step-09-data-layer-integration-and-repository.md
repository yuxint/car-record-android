# Step 09 集成文档：数据层组装与 Repository 接口

## 1. 目标与范围
- 目标：将数据库、上下文、规则组装成可调用的数据层。
- 范围：Repository 接口、数据源组合、事务边界、错误模型。
- 非范围：完整 UI 功能。

## 2. 前置依赖
- 主约束：`step-01-foundation-tech-decisions.md`
- 上一步：`step-08-runtime-contexts-date-appliedcar-navigation.md`

## 3. 设计与实现清单
- Repository 划分
  - `CarRepository`、`RecordRepository`、`ReminderRepository`、`BackupRepository`。
- 数据源组装
  - `Room DAO + ContextStore + RuleEngine`。
- 事务与错误处理
  - 写操作按业务原子单元包裹事务。
  - 数据库异常映射为统一业务错误模型。
- 接口约束
  - 上层（ViewModel/UI）只依赖 repository，不直接碰 DAO。
  - 规则计算与 I/O 分离，便于单测。

## 4. 验收标准
- 主要业务链路可以通过 repository 完整跑通。
- 上层不绕过 repository 直连底层。
- 完成定义（DoD）
  - Repository 接口边界定义完成。
  - 事务边界定义完成。
  - 错误映射策略定义完成。

## 5. 风险与回滚
- 风险
  - Repository 过粗或过细导致后续维护困难。
  - 错误模型不统一导致 UI 处理分裂。
- 回滚策略
  - 以“单一职责 + 调用路径可追踪”为原则做接口收敛。

## 6. 决策记录
- 本步骤无新增技术选型。
- 沿用 Step 01 架构：纯 MVVM，保持最小必要分层。
