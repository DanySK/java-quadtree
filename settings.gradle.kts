plugins {
    id("com.gradle.develocity") version "4.0.1"
    id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.0.23"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
        uploadInBackground = !System.getenv("CI").toBoolean()
    }
}

gitHooks {
    commitMsg { conventionalCommits() }
    createHooks(true)
}

rootProject.name = "java-quadtree"
