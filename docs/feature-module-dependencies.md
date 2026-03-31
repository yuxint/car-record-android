# Feature 模块依赖梳理

本文记录当前 `feature/*` 模块之间的依赖关系、实际耦合点，以及后续抽取公共能力的建议顺序。

## 1. 模块级编译依赖

以各模块 `build.gradle.kts` 为准，当前 `feature` 之间的显式依赖如下：

- `feature:my -> feature:addcar`
- `feature:my -> feature:datatransfer`
- `feature:records -> feature:addcar`
- `feature:reminder -> feature:records`

若只看目标方向，依赖图如下：

```text
feature:datatransfer <---- feature:my
        ^
        |
feature:addcar <---- feature:records <---- feature:reminder
        ^
        |
     feature:my
```

对照文件：

- `feature/addcar/build.gradle.kts`
- `feature/datatransfer/build.gradle.kts`
- `feature/my/build.gradle.kts`
- `feature/records/build.gradle.kts`
- `feature/reminder/build.gradle.kts`

## 2. 实际依赖内容

### 2.1 `feature:addcar -> core:common`

依赖内容：

- `AppTimeCodec`

用途：

- `AddCarManagementUi` 用它做日期格式化，以及 `LocalDate -> epochSeconds` 转换
- `CarRepository` 用它做 `epochSeconds -> LocalDate` 转换

代码位置：

- `core/common/src/main/java/com/tx/carrecord/core/common/time/AppTimeCodec.kt`
- `feature/addcar/src/main/java/com/tx/carrecord/feature/addcar/ui/AddCarManagementUi.kt`
- `feature/addcar/src/main/java/com/tx/carrecord/feature/addcar/data/CarRepository.kt`

判断：

- 这是“通用时间编解码能力”被放进了 `datatransfer`，现在已经抽到 `core/common/time/AppTimeCodec.kt`
- `addcar` 已经不再需要直接依赖 `feature:datatransfer`

### 2.2 `feature:my -> feature:addcar`

依赖内容：

- `AddCarViewModel`
- `AddCarManagementSection`
- `CarPurchaseDatePickerDialog`

用途：

- 个人中心直接嵌入车辆管理区块
- 调试工具里的手动日期选择直接复用车辆模块里的日期弹窗

代码位置：

- `feature/my/src/main/java/com/tx/carrecord/feature/my/ui/MyUiPlaceholder.kt`
- `feature/addcar/src/main/java/com/tx/carrecord/feature/addcar/ui/AddCarManagementUi.kt`
- `feature/addcar/src/main/java/com/tx/carrecord/feature/addcar/ui/AddCarPickerDialogs.kt`

判断：

- 这里混合了两类依赖：
- 一类是“通用 UI 组件复用”，例如日期弹窗
- 一类是“页面聚合编排”，即 `my` 直接承载车辆管理能力
- 前者适合抽公共；后者更适合上移到 `app`

### 2.3 `feature:my -> feature:datatransfer`

依赖内容：

- `DataTransferSection`

用途：

- 个人中心直接嵌入备份/恢复区块

代码位置：

- `feature/my/src/main/java/com/tx/carrecord/feature/my/ui/MyUiPlaceholder.kt`
- `feature/datatransfer/src/main/java/com/tx/carrecord/feature/datatransfer/ui/DataTransferUi.kt`

判断：

- 这属于页面聚合编排，不是底层能力复用
- 如果后续要去掉横向依赖，优先做法不是把 `DataTransferSection` 抽到 `core`，而是让 `app` 负责组合页面区块

### 2.4 `feature:records -> feature:addcar`

依赖内容：

- `AppDatePickerDialog`
- `AppMileagePickerDialog`

用途：

- 记录编辑页复用新增车辆模块中的通用日期/里程选择器

代码位置：

- `feature/records/src/main/java/com/tx/carrecord/feature/records/ui/RecordsUiPlaceholder.kt`
- `feature/addcar/src/main/java/com/tx/carrecord/feature/addcar/ui/AddCarPickerDialogs.kt`

判断：

- 这是典型的“公共 UI 落在某个 feature 中”
- 适合抽为公共 Compose 组件，避免 `records` 为了两个弹窗依赖整个 `addcar`

### 2.5 `feature:reminder -> feature:records`

依赖内容：

- `RecordsViewModel`

用途：

- 提醒页点击右下角 `+` 时，直接调用记录模块的新增草稿初始化逻辑

代码位置：

- `feature/reminder/src/main/java/com/tx/carrecord/feature/reminder/ui/ReminderUiPlaceholder.kt`
- `app/src/main/java/com/tx/carrecord/app/ui/CarRecordRoot.kt`

判断：

- 这是当前耦合最重的一条横向依赖
- `reminder` 直接依赖 `records` 的页面状态与行为入口，不是公共能力复用
- 更合理的做法是由 `app` 统一编排“从提醒页进入新增记录页”的流程

## 3. 按耦合类型分类

### 3.1 可以抽为公共能力的内容

- 时间编解码
  - `AppTimeCodec`
- 通用选择器 UI
  - `AppDatePickerDialog`
  - `AppMileagePickerDialog`
  - `CarPurchaseDatePickerDialog` / `CarMileagePickerDialog` 可保留为业务语义薄包装

这些内容的共同特点是：

- 不依赖某个 feature 的业务状态
- 被多个模块复用
- 抽出后能直接减少显式模块依赖

### 3.2 不建议抽到公共层的内容

- `AddCarViewModel`
- `RecordsViewModel`
- `AddCarManagementSection`
- `DataTransferSection`

原因：

- 这些类型仍然带明确业务语义
- 复用方式不是“基础能力共享”，而是“页面拼装/功能编排”
- 若强行下沉到 `core`，会让 `core` 承担 feature 语义，边界更模糊

### 3.3 更适合上移到 `app` 的内容

- `my` 页面中对车辆管理区块、数据管理区块的组合
- `reminder` 页面跳转并初始化记录新增页的流程

判断标准：

- 逻辑本质是跨 feature 的协作入口
- 由应用壳层统一管理更符合当前项目的实际结构

## 4. 推荐拆解顺序

### 第一步：抽时间编解码

目标：

- 去掉 `feature:addcar -> feature:datatransfer`

建议：

- 将时间编解码能力统一到 `core/common/time/AppTimeCodec.kt`
- `addcar`、`datatransfer` 等模块统一改用公共时间工具

收益：

- 改动面较小
- 风险最低
- 能快速清掉一条不合理的反向业务依赖

### 第二步：抽通用选择器 UI

目标：

- 去掉 `feature:records -> feature:addcar`

建议：

- 将日期、里程选择器下沉到公共 UI 模块或公共 UI 包
- `addcar` 中保留业务语义化的包装方法即可

收益：

- 让 `records` 不再因为通用组件依赖 `addcar`
- 为后续其他 feature 复用弹窗提供稳定入口

### 第三步：把提醒到记录的协作上移到 `app`

目标：

- 去掉 `feature:reminder -> feature:records`

建议：

- `ReminderScreen` 不再直接持有 `RecordsViewModel`
- 改为接受 `onOpenAddRecordPage` 回调
- 由 `app` 负责调用 `recordsViewModel.startNewRecordDraft()` 并控制页面显示

收益：

- 解除 feature 之间对 ViewModel 的直接依赖
- 保持跨页面流程仍然集中在壳层编排

### 第四步：收敛 `my` 的聚合职责

目标：

- 减少 `feature:my -> feature:addcar`
- 减少 `feature:my -> feature:datatransfer`

建议：

- 让 `my` 更偏“容器页面”或“纯展示页面”
- 将区块注入、overlay 控制、跨模块联动放到 `app`

收益：

- `my` 不再成为功能聚合中心
- feature 边界更清晰

## 5. 优先级建议

若目标是“先用最小代价降低横向依赖”，推荐顺序：

1. 抽时间编解码
2. 抽通用选择器 UI
3. 上移 `reminder -> records` 协作
4. 收敛 `my` 的聚合职责

若目标是“先把架构边界理顺”，推荐顺序：

1. 上移 `reminder -> records` 协作
2. 收敛 `my` 的聚合职责
3. 抽时间编解码
4. 抽通用选择器 UI

## 6. 当前结论

当前 `feature` 模块之间已经存在横向依赖，但成因并不一致：

- 一部分是公共工具/公共 UI 放错层导致
- 一部分是跨 feature 协作被写在 feature 内部，而不是由 `app` 编排

因此后续治理不建议只做“抽公共类”这一种动作，而应区分两条路径：

- 对工具和通用组件，向 `core` 下沉
- 对跨 feature 编排，向 `app` 上移
