# core/datastore 模块最小上下文

## 必读

- `core/datastore/src/main/java/com/tx/carrecord/core/datastore/AppDateContext.kt`
- `core/datastore/src/main/java/com/tx/carrecord/core/datastore/DatastoreAppDateContext.kt`
- `core/datastore/src/main/java/com/tx/carrecord/core/datastore/AppliedCarContext.kt`
- `core/datastore/src/main/java/com/tx/carrecord/core/datastore/DatastoreAppliedCarContext.kt`
- `core/datastore/src/main/java/com/tx/carrecord/core/datastore/AppNavigationContext.kt`
- `core/datastore/src/main/java/com/tx/carrecord/core/datastore/DatastoreAppNavigationContext.kt`

## 仅当以下改动才读

- 改“今天/到期判断”语义：补读 `docs/agents/runtime-contexts.md`。
- 改 key、协议字段或默认行为：补读 `docs/agents/context-routing.md` 并追加受影响 feature 文档。
- 改注入与装配：补读 `docs/agents/gradle-wiring-rules.md`。

## 改完自检 3 条

- 手动日期与系统日期切换语义是否一致。
- 当前车辆失效回退是否保留。
- 跨 Tab 导航请求是否可被正确消费且不串扰。
