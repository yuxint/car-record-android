# Feature 模块依赖梳理

对照当前代码，`feature/*` 之间的横向编译依赖已经清理完成。

## 1. 当前状态

以各模块 `build.gradle.kts` 为准，当前不存在 `feature -> feature` 的显式编译依赖。

当前依赖关系为：

- `app -> feature:addcar`
- `app -> feature:datatransfer`
- `app -> feature:my`
- `app -> feature:records`
- `app -> feature:reminder`
- `feature/* -> core:*`

这意味着当前模块边界已经回到预期结构：

```text
app
 ├── feature:addcar
 ├── feature:datatransfer
 ├── feature:my
 ├── feature:records
 └── feature:reminder

feature/* -> core/*
```

## 2. 本轮收敛结果

对照当前代码，原先存在的横向依赖已完成清理：

- `feature:addcar -> feature:datatransfer` 已移除
- `feature:my -> feature:addcar` 已移除
- `feature:my -> feature:datatransfer` 已移除
- `feature:records -> feature:addcar` 已移除
- `feature:reminder -> feature:records` 已移除

## 3. 当前实现方式

本轮收敛后的组织方式如下：

- 通用时间编解码已下沉到 `core/common/time/AppTimeCodec.kt`
- 通用日期/里程选择器已下沉到 `core/common/ui/AppPickerDialogs.kt`
- `my` 页内的车辆管理区块、数据管理区块由 `app` 负责组合
- 从提醒页进入新增记录页的流程由 `app` 统一编排

## 4. 后续维护建议

后续新增功能时，优先遵守以下边界：

- 公共工具、公共 UI 组件，优先下沉到 `core`
- 跨 feature 的页面编排和流程控制，优先上移到 `app`
- 避免在某个 feature 中放置会被其他 feature 直接 import 的通用能力

## 5. 当前仍存在的上层编排耦合

以下耦合点当前仍然存在，但它们位于 `app` 壳层，属于可接受的页面编排，不属于 `feature -> feature` 横向依赖：

### 5.1 `app` 统一编排“我的”页区块

位置：

- `app/src/main/java/com/tx/carrecord/app/ui/CarRecordRoot.kt`
- `feature/my/src/main/java/com/tx/carrecord/feature/my/ui/MyUiPlaceholder.kt`

当前方式：

- `MyScreen` 通过 `extraContent` 插槽接收额外内容
- `app` 负责把 `AddCarManagementSection` 和 `DataTransferSection` 组合进去

判断：

- 这是合理的壳层编排
- 若后续 `MyScreen` 又开始直接 import 其他 feature 的 ViewModel、Section 或 Page，就说明边界开始回退

### 5.2 `app` 统一编排“提醒 -> 新增记录”流程

位置：

- `app/src/main/java/com/tx/carrecord/app/ui/CarRecordRoot.kt`
- `feature/reminder/src/main/java/com/tx/carrecord/feature/reminder/ui/ReminderUiPlaceholder.kt`

当前方式：

- `ReminderScreen` 只暴露 `onOpenAddRecordPage`
- `app` 负责调用 `recordsViewModel.startNewRecordDraft()` 并展示 `AddRecordPage`

判断：

- 这是合理的跨页面流程编排
- 若后续 `ReminderScreen` 再次直接依赖 `RecordsViewModel` 或 `records` 模块中的页面类型，说明边界开始回退

## 6. 回归检查清单

后续改动可优先检查以下信号，避免横向依赖重新长回去：

- `feature/*/build.gradle.kts` 是否新增了 `implementation(project(":feature:..."))`
- `feature/*` 代码里是否新增了 `import com.tx.carrecord.feature.*`
- 某个 feature 是否开始直接持有另一个 feature 的 `ViewModel`
- 通用弹窗、格式化工具、选择器是否又被放回某个 feature 内部
- `app` 中的编排逻辑是否被重新下沉回某个 feature 页面

## 7. 验证记录

本次已验证：

- 删除 `feature/reminder/build.gradle.kts` 中对 `feature:records` 的依赖后，相关模块仍可正常编译
- 验证命令：
  - `:app:compileDebugKotlin`
  - `:feature:records:compileDebugKotlin`
  - `:feature:reminder:compileDebugKotlin`
