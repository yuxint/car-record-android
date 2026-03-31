# Step 03 执行结果：数据模型与 Room Schema

## 1. 已完成
- 完成四个核心实体的 Room 映射：`CarEntity`、`MaintenanceRecordEntity`、`MaintenanceRecordItemEntity`、`MaintenanceItemOptionEntity`。
- 完成关键约束下沉：
  - `maintenance_records.cycle_key` 唯一索引（同车同日唯一）。
  - `maintenance_record_items.cycle_item_key` 唯一索引（同车同日同项目唯一）。
- 完成级联删除链：
  - `maintenance_records.car_id -> cars.id`（删除车辆级联删除记录）。
  - `maintenance_record_items.record_id -> maintenance_records.id`（删除记录级联删除关系项）。
  - `maintenance_item_options.owner_car_id -> cars.id`（删除车辆同步清理 ownerCarID 命中项）。
- 完成 `RoomDatabase` 与最小 DAO 定义。

## 2. 已执行（原 TODO）
- 已新增统一时间编解码入口 `AppTimeCodec`，用于日期字符串与 epoch 秒转换。
- 已将 `MyDataTransferRules` 与 `RoomBackupRepository` 的日期/时间处理统一收口到该入口。
- 已保持 iOS JSON 契约：
  - `purchaseDate`、`log.date` 使用严格 `yyyy-MM-dd`。
  - `createdAt` 保持秒级时间戳（`Double`）语义，入库落地为 epoch 秒。
- 双向兼容回归样例尚未补充自动化测试（待后续测试步骤落地）。

## 3. 验证说明
- 已完成静态结构检查（字段、索引、外键、删除策略与 Step 03 文档一致）。
- 本地未执行构建命令：当前环境缺少 `gradle/gradlew`。
