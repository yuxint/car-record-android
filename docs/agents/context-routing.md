# 按任务最小读取路由

目标：只读取必要上下文，避免每次全量浏览 `docs/agents`。

## 默认读取顺序

1. 先读当前文件（本文件）。
2. 根据改动模块只读对应模块文档（`docs/agents/modules/*.md`）。
3. 仅在触发条件满足时，补读公共文档（见下文“按需补读触发条件”）。

默认上限：除模块文档外，最多再补读 2 份公共文档；超过上限必须有明确风险理由。

## 模块入口

- 改 `app` 壳层：`docs/agents/modules/app.md`
- 改 `core/common`：`docs/agents/modules/core-common.md`
- 改 `core/database`：`docs/agents/modules/core-database.md`
- 改 `core/datastore`：`docs/agents/modules/core-datastore.md`
- 改 `feature/my`：`docs/agents/modules/feature-my.md`
- 改 `feature/addcar`：`docs/agents/modules/feature-addcar.md`
- 改 `feature/datatransfer`：`docs/agents/modules/feature-datatransfer.md`
- 改 `feature/records`：`docs/agents/modules/feature-records.md`
- 改 `feature/reminder`：`docs/agents/modules/feature-reminder.md`

## 按需补读触发条件

- 触发“时间相关逻辑”（今天、手动日期、到期、日期格式）：
  - 补读 `docs/agents/runtime-contexts.md`
- 触发“当前应用车辆”或“跨 Tab 跳转”：
  - 补读 `docs/agents/runtime-contexts.md`
- 触发“实体字段/唯一约束/删除恢复/导入导出”：
  - 补读 `docs/agents/data-model.md`
- 触发“新增/删除/移动模块、Gradle 配置、Manifest 与依赖接线”
  - 补读 `docs/agents/gradle-wiring-rules.md`
- 触发“脚本调用、备份恢复流程、迁移步骤联动”：
  - 补读 `docs/agents/scripts-reference.md`
- 触发“架构边界、命名、文案、通用约束”：
  - 补读 `docs/agents/business-constraints.md`

## 跨模块影响检查（提交前）

- 是否改了 `core/database/*`（实体、DAO、Schema、错误映射）。
- 是否改了 `core/datastore/*`（日期/选车/导航上下文）。
- 是否改了 `CarRecordDatabase` 或 `CarRecordDao` 的对外契约。
- 是否改了备份结构（`MyDataTransferRuleModels` / `BackupRepository`）或相关时间编解码。

若任一项为“是”，需要补读对应公共文档并做回归检查。
