# 辅助脚本与迁移文档参考

当前仓库无独立 `scripts/` 目录；流程说明以 `docs/migration/` 为准。

## 主要参考文档

- 迁移总览与步骤结果：`docs/migration/step-*-execution-result.md`
- 规则映射：`docs/migration/ios-android-rule-mapping.md`
- 已知差异与待办：`docs/migration/known-differences-backlog.md`

## 使用约定

- 涉及“导入导出/恢复策略/兼容口径”改动时，先对照对应迁移步骤文档。
- 若新增自动化脚本，需在本文件补充入口命令、依赖与产物说明。
