# Car Record Android

Android 端迁移项目（从 iOS `CarRecord` 规则迁移）。

## 模块结构
- `app`：应用入口与根导航壳。
- `core/common`：通用类型与错误模型。
- `core/database`：Room 实体、DAO 与数据库依赖注入。
- `core/datastore`：运行时上下文（日期/当前车辆/导航请求）。
- `feature/reminder`：提醒规则与数据接线。
- `feature/records`：保养记录规则与数据接线。
- `feature/my`：车辆管理、备份导入导出规则与数据接线。

## 文档入口
- 迁移步骤与执行结果：`docs/migration/`
- 架构概览：`docs/architecture-overview.md`

## 当前状态（截至 2026-03-26）
- Step 01-12 迁移文档已齐备。
- 规则层与数据层已完成迁移并通过静态核对。
- 当前仓库未包含 Gradle Wrapper，自动化构建与测试需在补齐 Wrapper 后执行。

## 迁出为独立仓库
请先阅读：`docs/migration/step-12-execution-result.md`
