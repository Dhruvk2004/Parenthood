import java.io.FileInputStream
import java.util.Properties

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

        // Load GitHub properties for GitHubPackages repository
        val githubProperties = Properties()
        githubProperties.load(FileInputStream(rootDir.resolve("github.properties")))

        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Cuberto/liquid-swipe-android")
            credentials {
                username = githubProperties.getProperty("gpr.usr") ?: System.getenv("gpr")
                password = githubProperties.getProperty("gpr.key") ?: System.getenv("GPR_API_KEY")
            }
        }

        maven{
            url = uri("https://jitpack.io")
        }
    }
}

rootProject.name = "ParentHood"
include(":app")
