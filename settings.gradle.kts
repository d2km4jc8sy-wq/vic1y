// 仓库顺序（2026-10-17）：google()/mavenCentral() 优先，aliyun 镜像降为兜底——
// GitHub Actions 美区 runner 访问 aliyun 不稳定（kotlin-reflect 2.0.20 解析失败实证），
// 本机有 Gradle 缓存且出口可达官方源，顺序调整对本地无感。
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
    }
}
rootProject.name = "S1Client"
include(":app")
