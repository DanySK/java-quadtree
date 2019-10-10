import kotlin.String
import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.plugin.use.PluginDependencySpec

/**
 * Generated by https://github.com/jmfayard/buildSrcVersions
 *
 * Find which updates are available by running
 *     `$ ./gradlew buildSrcVersions`
 * This will only update the comments.
 *
 * YOU are responsible for updating manually the dependency version.
 */
object Versions {
    const val org_danilopianini_git_sensitive_semantic_versioning_gradle_plugin: String = "0.2.2"

    const val org_danilopianini_publish_on_central_gradle_plugin: String = "0.2.3"

    const val org_danilopianini_javadoc_io_linker_gradle_plugin: String = "0.1.4"

    const val org_jlleitschuh_gradle_ktlint_gradle_plugin: String = "9.0.0"

    const val de_fayard_buildsrcversions_gradle_plugin: String = "0.6.4"

    const val com_github_spotbugs_gradle_plugin: String = "1.6.9" // available: "2.0.0"

    const val ktlint: String = "0.34.2"

    const val guava: String = "28.1-jre"

    const val junit: String = "4.13-beta-3"

    /**
     * Current version: "5.6.2"
     * See issue 19: How to update Gradle itself?
     * https://github.com/jmfayard/buildSrcVersions/issues/19
     */
    const val gradleLatestVersion: String = "5.6.2"
}

/**
 * See issue #47: how to update buildSrcVersions itself
 * https://github.com/jmfayard/buildSrcVersions/issues/47
 */
val PluginDependenciesSpec.buildSrcVersions: PluginDependencySpec
    inline get() =
            id("de.fayard.buildSrcVersions").version(Versions.de_fayard_buildsrcversions_gradle_plugin)
