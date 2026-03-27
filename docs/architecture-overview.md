# Android 架构概览

## 1. 分层边界
- UI 层：`app` 与 `feature/*/ui`，负责状态展示与用户交互。
- 规则层：`feature/*/domain`，承载迁移后的核心业务规则。
- 数据层：`feature/*/data` 通过 `core/database` 与 `core/datastore` 提供能力。
- 基础层：`core/common` 提供跨模块复用模型与错误语义。

## 2. 模块依赖
- `app -> feature/*`
- `feature/* -> core/common + core/database + core/datastore`
- `core/*` 之间保持最小依赖，不跨 feature 直接耦合。

## 3. 关键业务约束
- `cycleKey`：同车同日唯一。
- `cycleItemKey`：同车同日同项目唯一。
- `itemIDsRaw`：持久化 UUID 列表，不使用展示名作为主键。
- 提醒进度：时间/里程谁先到采用谁。
- 当前时间：统一通过 `AppDateContext` 获取。

## 4. 数据兼容约束
- 备份 JSON 与 iOS `MyDataTransferPayload` 兼容。
- 导入优先按 `catalogKey` 进行项目映射。
- 导出保持稳定排序，避免无效 diff。

## 5. 运行时上下文
- `AppDateContext`：业务当前日期（支持手动日期）。
- `AppliedCarContext`：当前应用车辆。
- `AppNavigationContext`：跨 Tab 导航请求与单次消费。
