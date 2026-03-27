# Step 02 工程骨架文档：Android 工程结构、包结构与构建基线

## 1. 目标与范围
- 目标：定义 Android 工程壳结构，保证后续迁移可持续落地。
- 范围：模块边界、包结构、Gradle 版本基线、依赖清单、目录树、模块依赖图。
- 非范围：业务功能实现。

## 2. 前置依赖
- 主约束：`step-01-foundation-tech-decisions.md`
- 上一步：`step-01-foundation-tech-decisions.md`

## 3. 设计与实现清单
- 工程建议目录
  - `app/`：应用壳、入口、导航容器。
  - `core/common/`：通用常量、错误模型、工具。
  - `core/database/`：Room Database、Entity、Dao、TypeConverter。
  - `core/datastore/`：应用上下文持久化（manual now、applied car、navigation target）。
  - `feature/reminder/`、`feature/records/`、`feature/my/`：按功能域组织。
- 包结构建议
  - 根包：`com.tx.carrecord`
  - 按 `feature` + `data` + `domain` + `ui` 组织，不跨层直接依赖。
- 构建配置基线
  - Kotlin + AGP 稳定版本。
  - minSdk 28，targetSdk 使用当前稳定目标。
  - 启用 Compose、KSP（Room/Hilt）、kotlinx.serialization。
- 依赖基线
  - AndroidX Compose、Lifecycle、Navigation（UI 阶段使用）。
  - Room、Hilt、Datastore、kotlinx.serialization、Coroutines。
- 模块依赖图（文字版）
  - `app -> feature/* -> core/*`
  - `feature/* -> core/database | core/datastore | core/common`
  - 禁止 `feature/reminder` 与 `feature/records` 直接互相依赖。
- 目录树（目标态示例）
  - `car-record-android/`
  - `car-record-android/app/`
  - `car-record-android/core/database/`
  - `car-record-android/core/datastore/`
  - `car-record-android/feature/reminder/`
  - `car-record-android/feature/records/`
  - `car-record-android/feature/my/`

## 4. 验收标准
- 工程骨架可被后续步骤直接引用。
- 模块边界与依赖方向清晰，不出现反向依赖。
- 完成定义（DoD）
  - 输出模块列表、依赖方向、目录树。
  - 输出依赖与构建基线，不含业务实现。

## 5. 风险与回滚
- 风险
  - 过度拆模块导致前期迁移成本过高。
  - 边界不清导致后续规则实现跨层污染。
- 回滚策略
  - 优先保留模块边界与依赖方向；若需降复杂度，可先合并 feature 模块，语义保持不变。

## 6. 决策记录
- 候选方案对比
  - 单模块：上手快，但后续边界易失控。
  - data/domain/app 三模块：平衡复杂度与可维护性。
  - 多模块 + feature 细拆（推荐）：长期最清晰，适合迁移全量目标。
- 影响面
  - 模块粒度会影响构建速度、依赖治理和后续并行开发效率。
- 状态
  - 本步骤建议“多模块 + feature 细拆”，需你最终确认锁定到 Step 01。
