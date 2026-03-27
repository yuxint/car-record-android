# Step 08 执行结果：运行时上下文（日期 / 应用车型 / 跨模块导航）

## 1. 已完成
- 在 `core/datastore` 定义并落地三个运行时上下文接口与实现：
  - `AppDateContext` + `DatastoreAppDateContext`
  - `AppliedCarContext` + `DatastoreAppliedCarContext`
  - `AppNavigationContext` + `DatastoreAppNavigationContext`
- 统一 DataStore 持久化键位（替代 SharedPreferences 直连）：
  - `app_date_use_manual_now`
  - `app_date_manual_now_timestamp`
  - `applied_car_id`
  - `root_tab_navigation_target`
  - `root_tab_navigation_nonce`
- 日期上下文能力收敛：
  - 统一 `now()` / `nowFlow()` 业务“当前日期”入口。
  - 支持手动日期开关与手动日期持久化。
  - 手动日期持久化按当天 `startOfDay` 归一化（以 `LocalDate` 语义落盘）。
  - 统一短日期格式化为 `yyyy-MM-dd`。
- 应用车型上下文能力收敛：
  - 提供 `applied_car_id` 的 UUID 编解码（空串 / 非法串按未设置处理）。
  - 提供失效 ID 回退策略（回退到第一辆车；无车回退空）。
  - 提供 `normalizeAndPersist`，确保持久化值最终为“有效 ID 或空串”。
- 跨模块导航请求能力收敛：
  - 提供 `requestNavigation(to)` 统一发起入口。
  - 请求会同时持久化目标 Tab 与 nonce，确保跨层监听可感知“新请求”。
  - 提供 `clearNavigationRequest()` 清理已消费请求。
- 完成 Hilt 组装：
  - DataStore Provider（`runtime_context_store`）
  - 三个 Context 的接口绑定。

## 2. 本步输出文件
- `core/datastore/src/main/java/com/tx/carrecord/core/datastore/AppDateContext.kt`
- `core/datastore/src/main/java/com/tx/carrecord/core/datastore/AppliedCarContext.kt`
- `core/datastore/src/main/java/com/tx/carrecord/core/datastore/AppNavigationContext.kt`
- `core/datastore/src/main/java/com/tx/carrecord/core/datastore/RuntimeContextPreferences.kt`
- `core/datastore/src/main/java/com/tx/carrecord/core/datastore/DatastoreAppDateContext.kt`
- `core/datastore/src/main/java/com/tx/carrecord/core/datastore/DatastoreAppliedCarContext.kt`
- `core/datastore/src/main/java/com/tx/carrecord/core/datastore/DatastoreAppNavigationContext.kt`
- `core/datastore/src/main/java/com/tx/carrecord/core/datastore/di/RuntimeContextModule.kt`
- `docs/migration/step-08-execution-result.md`

## 3. 调用约束（禁止绕过）
- 禁止业务代码直接读取系统时间作为业务“现在”，统一通过 `AppDateContext`。
- 禁止直接读写 `applied_car_id` 原始字符串，统一通过 `AppliedCarContext`。
- 禁止直接改 RootTab 状态触发跳转，统一通过 `AppNavigationContext.requestNavigation(to)`。

## 4. 验证说明
- 已完成静态自检：
  - 键名与 iOS 既有口径对齐。
  - 回退策略与 Step 06 规则一致（失效 ID -> 第一辆车 / 无车空值）。
  - DataStore 作为统一持久化入口，未引入 SharedPreferences 直连。
- 未完成自动化执行：
  - 当前仓库缺少 `car-record-android/gradlew`，无法本地执行模块级测试或编译任务。

## 5. 风险提示
- 当前仅完成 Context 层收敛，尚未在各 feature 的 ViewModel / Repository 全量接入（计划在 Step 09+ 组装）。
- `nowFlow()` 在非手动模式下依赖订阅触发更新，不适合作为“跨天自动 tick”机制；跨天刷新应由上层触发重拉。
