# 项目概况

- 纯 Android 客户端（Kotlin + Compose + Room + DataStore），无网络层。
- 工程文件：根工程 Gradle（`settings.gradle.kts` + 多模块 `build.gradle.kts`）。
- 主要源码：`app`、`core/*`、`feature/*`。
- 根 Tab：`保养提醒`、`保养记录`、`个人中心`。
- 状态组织：`ViewModel` + `StateFlow` + 本地持久化与运行时上下文。

## 仓库关键目录

- `app/src/main/java/com/tx/carrecord`：应用入口与根导航。
- `feature/reminder`：提醒规则与数据接线。
- `feature/records`：保养记录规则与数据接线。
- `feature/my`：个人中心页面展示与入口编排。
- `feature/addcar`：车辆管理与车型级项目配置规则。
- `feature/datatransfer`：备份导入导出与结构校验。
- `core/database`：Room 实体、DAO、数据库与错误映射。
- `core/datastore`：日期/当前车辆/跨 Tab 导航上下文。
- `core/common`：通用结果模型与错误类型。
- `docs/migration`：迁移步骤与执行结果记录。

## 下一步

- 先读：`docs/agents/context-routing.md`
