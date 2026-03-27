# Step 12 执行结果：发布切换、目录迁出与独立项目化

## 1. 已完成
- 完成 Step 12 收尾文档落地，补齐独立项目化所需的最小文档集合：
  - 新增项目入口文档：`README.md`。
  - 新增架构概览：`docs/architecture-overview.md`。
  - 新增风险控制映射表：`docs/migration/ios-android-rule-mapping.md`。
  - 新增已知差异 backlog：`docs/migration/known-differences-backlog.md`。
- 完成迁出前现状核对：
  - `car-record-android/docs/migration/step-01` 至 `step-12` 文档齐备。
  - 当前仓库状态下，`car-record-android/` 仍为未跟踪目录（待冻结分支时统一提交）。

## 2. 迁出操作清单（可执行）
- 冻结当前迁移分支（建议命令）
  1. `git switch -c codex/android-migration-freeze`
  2. `git add car-record-android`
  3. `git commit -m "chore(android): freeze migration baseline before extract"`
  4. `git tag -a android-migration-freeze-20260326 -m "Android migration freeze before extract"`
- 导出 `car-record-android` 到新仓库（建议命令）
  1. `git subtree split --prefix=car-record-android -b tmp/car-record-android-split`
  2. `git init --bare /tmp/car-record-android.git`（本地演练）
  3. `git push /tmp/car-record-android.git tmp/car-record-android-split:main`
  4. 在目标远端仓库执行同样 push：`git push <new-remote> tmp/car-record-android-split:main`
- 新仓库重建项（发布前必须核对）
  - CI：补齐 Android 构建与测试流水线。
  - 版本：确认 `versionCode`/`versionName` 递增策略。
  - 签名：仅保留签名配置模板与环境变量，不提交证书/密钥。
  - 发布：建立最小发布核对清单（构建、安装、关键路径冒烟）。

## 3. 本步输出文件
- `README.md`
- `docs/architecture-overview.md`
- `docs/migration/ios-android-rule-mapping.md`
- `docs/migration/known-differences-backlog.md`
- `docs/migration/step-12-execution-result.md`

## 4. 验证说明
- 已完成文件存在性核对：
  - Step 01-12 文档完整保留。
  - Step 12 要求的“迁出清单 / 发布核对 / 差异 backlog”均已落地为文档。
- 未完成自动化构建验证：
  - 当前未包含 `gradlew`，且本机无 `gradle` 命令，无法执行独立构建/测试。

## 5. 风险与回滚
- 风险
  - 若在“冻结前”直接迁出，可能带入未审阅改动。
  - 若独立仓库未先补齐 CI/签名模板，发布切换阶段容易阻塞。
- 回滚策略
  - 以冻结标签 `android-migration-freeze-20260326` 作为回滚锚点。
  - 迁出失败时回到标签点，按清单重试。

## 6. DoD 对照
- 迁出操作清单可执行：是（见第 2 节命令序列）。
- 发布前核对项完整：是（见第 2 节“新仓库重建项”）。
- 已知差异与后续计划已登记：是（`known-differences-backlog.md`）。
