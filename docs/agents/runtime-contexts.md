# 运行时上下文

## 日期上下文

- 入口：`core/datastore/src/main/java/com/tx/carrecord/core/datastore/AppDateContext.kt`
- 默认实现：`DatastoreAppDateContext.kt`
- 禁止事项：不要散落系统时间调用作为业务“现在”。
- 常见坑：手动日期开关开启时，所有“今天/到期”逻辑都必须跟随上下文。

## 应用车型上下文

- 入口：`core/datastore/src/main/java/com/tx/carrecord/core/datastore/AppliedCarContext.kt`
- 默认实现：`DatastoreAppliedCarContext.kt`
- 禁止事项：不要绕过上下文直接写原始 ID。
- 常见坑：当前车辆失效时必须允许回退到可用车辆。

## 跨 Tab 导航上下文

- 入口：`core/datastore/src/main/java/com/tx/carrecord/core/datastore/AppNavigationContext.kt`
- 默认实现：`DatastoreAppNavigationContext.kt`
- 禁止事项：不要绕过上下文协议直接改根导航状态。
- 常见坑：目标 Tab 消费后要保证导航请求可正确去重/失效。
