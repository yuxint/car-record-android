# 关键业务约束与修改约定

## 绝对约束（不要破坏）

- 提醒进度按“时间/里程谁先到就采用谁”的规则计算。
- 保养记录录入与编辑必须遵守“同车同日唯一”的业务口径。
- 单条保养记录内项目集合必须去重，且遵守“同车同日同项目唯一”的业务口径。
- 保养项目展示名称不能作为稳定主键；涉及映射时优先用 `id` 与 `catalogKey`。
- 时间相关逻辑（今天、车龄、提醒）必须走统一时间上下文，不直接散落系统时间调用。
- 涉及应用车型与跨 Tab 跳转时，必须复用既有上下文入口，不绕过上下文直接改状态。

## 编码约定（最小）

- 目录组织遵循 `app/core/feature` 分层，职责不要跨层。
- `feature/*` 不再新增对其他 `feature/*` 的模块依赖；跨 feature 协作优先上移到 `app` 编排。
- 公共工具、公共弹窗、公共选择器等可复用能力优先下沉到 `core`，不要重新放回某个 feature 内部。
- 避免在 `feature/*` 中直接持有其他 feature 的 `ViewModel`、`Section`、`Page` 或 `Screen` 类型。
- 数据模型/持久化变更请同步参考：`docs/agents/data-model.md`。
- 涉及存储或仓储返回值时，优先复用 `RepositoryResult` 与现有错误映射风格。
- UI 文案、注释与命名以中文语义为主，新增内容与现有风格保持一致。

## 相关文档

- 数据模型与持久化：`docs/agents/data-model.md`
- 运行时上下文：`docs/agents/runtime-contexts.md`
- Feature 依赖收敛记录：`docs/feature-module-dependencies.md`
