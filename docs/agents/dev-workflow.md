# 本地开发与测试

## 常用命令入口

- 全量构建：

```sh
./gradlew build
```

- 仅构建 debug：

```sh
./gradlew assembleDebug
```

- 运行单元测试：

```sh
./gradlew test
```

- 指定模块测试（示例）：

```sh
./gradlew :feature:my:test
./gradlew :feature:records:test
./gradlew :feature:reminder:test
```

- 静态检查：

```sh
./gradlew lint
```

## 执行建议

- 优先先跑“改动相关模块”的最小验证，再视情况补全量验证。
- 若遇环境问题，优先检查本地 JDK/Android SDK 与 Gradle 配置是否一致。
