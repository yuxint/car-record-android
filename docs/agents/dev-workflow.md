# 本地开发与测试

## 环境变量

本地执行 Gradle 命令前，优先查看下面“常用命令入口”的环境变量示例。

## 常用命令入口

- 环境变量示例：

```sh
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
GRADLE_USER_HOME="/Users/tx/develepment/workspace/car-record-android/.gradle-local" \
ANDROID_HOME="/Users/tx/Library/Android/sdk" \
./gradlew build
```

## 按模块验证

通用命令模板：

```sh
./gradlew :<module-path>:compileDebugKotlin
./gradlew :<module-path>:test
./gradlew :<module-path>:lint
```

模块路径列表：

- `app`
- `core:common`
- `core:database`
- `core:datastore`
- `feature:addcar`
- `feature:datatransfer`
- `feature:reminder`
- `feature:records`
- `feature:my`

示例：

```sh
./gradlew :feature:addcar:compileDebugKotlin
./gradlew :feature:addcar:test
./gradlew :feature:addcar:lint
```

## 全量命令

```sh
./gradlew build
```

## 仅构建 debug

```sh
./gradlew assembleDebug
```

- 全局静态检查：

```sh
./gradlew lint
```

## 执行建议

- 优先先跑“改动相关模块”的最小验证，再视情况补全量验证。
