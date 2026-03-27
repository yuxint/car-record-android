# feature/my 模块最小上下文

## 必读

- `feature/my/src/main/java/com/tx/carrecord/feature/my/ui/MyUiPlaceholder.kt`
- `app/src/main/java/com/tx/carrecord/app/ui/CarRecordRoot.kt`

## 仅当以下改动才读

- 改车辆添加/编辑逻辑：补读 `docs/agents/modules/feature-addcar.md`。
- 改备份恢复逻辑：补读 `docs/agents/modules/feature-datatransfer.md`。
- 改车辆切换/应用车型：补读 `docs/agents/runtime-contexts.md`。
- 改根 Tab 显隐联动：补读 `docs/agents/modules/app.md`。

## 改完自检 3 条

- 个人中心页面仅负责展示和入口编排，不承载仓储与导入导出规则实现。
- 添加车辆入口与备份恢复入口是否可达且交互提示正常。
- 页面级状态（消息、弹窗、底栏显隐）是否保持原有行为。
