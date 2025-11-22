pluginManagement {
    repositories {
        google() // GoogleのMavenリポジトリを追加
        mavenCentral() // Maven Centralリポジトリを追加
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "timesignal"
include(":app")
