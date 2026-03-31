# AGENTS.md

本文件保留最小导航信息；详细规则已拆分到 `docs/agents/`，按需查阅。

## 快速入口

- 最小读取路由（先读）：`docs/agents/context-routing.md`
- 项目概况与仓库入口：`docs/agents/project-overview.md`
- 数据模型与持久化约束：`docs/agents/data-model.md`
- 运行时上下文（日期/选车/导航）：`docs/agents/runtime-contexts.md`
- 本地开发与验证流程：`docs/agents/dev-workflow.md`
- 关键业务约束与修改约定：`docs/agents/business-constraints.md`
- Gradle/模块接线规则：`docs/agents/gradle-wiring-rules.md`

## 使用约定

- 变更前先读 `docs/agents/context-routing.md`，再按模块最小清单读取，不做全量通读。
- 若文档描述与代码实现冲突，以当前代码事实为准，并同步更新文档。
- 若 `docs/agents/` 下文档与仓库内更近层级规则冲突，以更近规则优先。
- 若要参考ios项目，需开启子线程只读不修改，将结果交由主线程，ios项目目录：`/Users/tx/develepment/workspace/car-record`，具体阅读`/Users/tx/develepment/workspace/car-record/AGENTS.md`，参考ios项目目的是统一业务规则，逻辑，文案等，视觉和组件风格还是要按Android的Material体系处理

