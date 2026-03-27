# app 模块最小上下文

## 必读

- `app/src/main/java/com/tx/carrecord/MainActivity.kt`
- `app/src/main/java/com/tx/carrecord/app/CarRecordApp.kt`
- `app/src/main/java/com/tx/carrecord/app/ui/CarRecordRoot.kt`
- `app/src/main/java/com/tx/carrecord/app/ui/AppShellViewModel.kt`

## 仅当以下改动才读

- 改根导航与跨 Tab 消费：补读 `docs/agents/runtime-contexts.md`。
- 改模块装配或依赖方向：补读 `docs/agents/gradle-wiring-rules.md`。
- 改入口层错误处理或通用返回：看 `core/common/RepositoryResult.kt`。

## 改完自检 3 条

- Tab 切换与导航请求消费是否一致。
- 入口层是否未下沉 feature 业务规则。
- 模块依赖方向是否保持 `app -> feature/core` 单向。
