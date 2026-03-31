# core/common 模块最小上下文

## 必读

- `core/common/src/main/java/com/tx/carrecord/core/common/RepositoryResult.kt`
- `core/common/src/main/java/com/tx/carrecord/core/common/time/AppTimeCodec.kt`

## 仅当以下改动才读

- 改错误模型或语义：补读 `docs/agents/business-constraints.md`。
- 改调用面较广的结构：补读 `docs/agents/context-routing.md` 并按影响补读相关模块文档。

## 改完自检 3 条

- `Success/Failure` 语义是否保持稳定。
- 错误类型变更是否覆盖调用方。
- 是否引入了不必要的跨层依赖。
