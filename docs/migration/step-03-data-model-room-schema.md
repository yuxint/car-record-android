# Step 03 数据模型文档：Room 实体、索引与关系约束

## 1. 目标与范围
- 目标：将 iOS 四个核心实体映射为 Room 模型，保持业务语义一致。
- 范围：Entity、主键/唯一键、外键、索引、删除策略、字段语义。
- 非范围：业务流程与 UI。

## 2. 前置依赖
- 主约束：`step-01-foundation-tech-decisions.md`
- 上一步：`step-02-android-project-skeleton.md`

## 3. 设计与实现清单
- 实体映射
  - `CarEntity`：`id`、`brand`、`modelName`、`mileage`、`purchaseDate`、`disabledItemIDsRaw`。
  - `MaintenanceRecordEntity`：`id`、`carId`、`date`、`itemIDsRaw`、`cost`、`mileage`、`note`、`cycleKey`。
  - `MaintenanceRecordItemEntity`：`id`、`recordId`、`itemId`、`cycleItemKey`、`createdAt`。
  - `MaintenanceItemOptionEntity`：`id`、`ownerCarID`、`catalogKey`、阈值与提醒参数。
- 约束映射
  - `cycleKey` 唯一索引（同车同日唯一）。
  - `cycleItemKey` 唯一索引（同车同日同项目唯一）。
  - `record.carId -> car.id` 外键（删除车时级联删记录）。
  - `recordItem.recordId -> record.id` 外键（删除记录时级联删关系项）。
- 删除策略
  - 删除车辆时：关联记录级联删除；`ownerCarID` 命中的项目配置同步清理。
- 字段语义保持
  - `itemIDsRaw` 仍为 UUID 列表字符串。
  - `catalogKey` 用于默认项稳定映射，不依赖展示文案。

## 4. 验收标准
- 数据表结构能表达 iOS 当前约束。
- 关键唯一性由数据库层保证，不依赖 UI 层。
- 完成定义（DoD）
  - 四实体字段与索引定义齐全。
  - 级联删除路径定义齐全。
  - 字段语义与 iOS 保持一致说明齐全。

## 5. 风险与回滚
- 风险
  - 唯一键规则如果下沉不完整，会出现重复数据。
  - 删除链不完整导致脏数据残留。
- 回滚策略
  - 以 schema 迁移脚本与测试回归为准，必要时回退到上一个稳定 schema 版本。

## 6. 决策记录
- 本步骤无新增技术选型。
- 沿用 Step 01 约束：Room + SQLite、UUID 字符串持久化、唯一键硬约束。
