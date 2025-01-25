plugins {
    `java-library`
    alias(libs.plugins.gitSemVer)
    alias(libs.plugins.java.qa)
    alias(libs.plugins.multiJvmTesting)
    alias(libs.plugins.publishOnCentral)
    alias(libs.plugins.taskTree)
}

repositories {
    mavenCentral()
}

multiJvm {
    jvmVersionForCompilation.set(8)
}

dependencies {
    compileOnly(libs.jsr305)
    testCompileOnly(libs.spotbugs.annotations)
    testImplementation(libs.junit4)
}

group = "org.danilopianini"
publishOnCentral {
    repoOwner = "DanySK"
    projectDescription = "Java spatial indexing tools"
    projectLongName = "Java QuadTree"
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
