pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // 国内镜像
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

rootProject.name = "car-record-android"

include(
    ":app",
    ":core:common",
    ":core:database",
    ":core:datastore",
    ":feature:addcar",
    ":feature:datatransfer",
    ":feature:reminder",
    ":feature:records",
    ":feature:my",
)
