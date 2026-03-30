# Gradle 与模块接线更新规则

当“模块清单、依赖关系、源码集路径”变化时，需要检查并更新 Gradle 与 Manifest 接线。

## 需要检查/更新的场景

- 新增/删除模块，或调整模块路径：更新 `settings.gradle.kts` 的 `include(...)`。
- 新增模块依赖或替换依赖方向：更新对应模块 `build.gradle.kts`。
- 新增 Android 组件（`Activity`/`Service` 等）：检查对应 `AndroidManifest.xml` 声明。
- 调整 Room 实体或版本：检查 `core/database` 的数据库版本与 schema 输出。

## 一般不需要更新的场景

- 仅修改现有 Kotlin 文件逻辑，不改模块边界与依赖。
- 仅修改文案、UI 样式或小范围规则常量。

## 执行建议

- 优先在仓库根目录执行命令，避免相对路径误判。
- 调整依赖后至少做一次 `app` 构建，确认 Hilt/Room 生成代码与模块链接正常。
