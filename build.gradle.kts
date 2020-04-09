plugins {
    `java-library`
    checkstyle
    pmd
    signing
    `maven-publish`
    `project-report`
    `build-dashboard`
    jacoco
    id("com.github.spotbugs")
    id("org.danilopianini.git-sensitive-semantic-versioning")
    id("org.danilopianini.javadoc.io-linker")
    id("org.danilopianini.publish-on-central")
    id("org.jlleitschuh.gradle.ktlint")
}

gitSemVer {
    version = computeGitSemVer()
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(Libs.guava)
    testImplementation(Libs.junit)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

spotbugs {
    setEffort("max")
    setReportLevel("low")
    showProgress.set(true)
    val excludeFile = File("${project.rootProject.projectDir}/config/spotbugs/excludes.xml")
    if (excludeFile.exists()) {
        excludeFilter.set(excludeFile)
    }
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask> {
    reports {
        create("html") { enabled = true }
    }
}

pmd {
    ruleSets = listOf()
    ruleSetConfig = resources.text.fromFile("${project.rootProject.projectDir}/config/pmd/pmd.xml")
}

group = "org.danilopianini"
publishOnCentral {
    projectDescription.set("Java spatial indexing tools")
    projectLongName.set("Java QuadTree")
}

if (System.getenv("CI") == true.toString()) {
    signing {
        val signingKey: String? by project
        val signingPassword: String? by project
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
}

publishing {
    publications {
        withType<MavenPublication> {
            pom {
                developers {
                    developer {
                        name.set("Danilo Pianini")
                        email.set("danilo.pianini@gmail.com")
                        url.set("http://www.danilopianini.org/")
                    }
                }
            }
        }
    }
}
