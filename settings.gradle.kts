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

        // GitHub Packages for liquid-swipe library
        val githubPropertiesFile = File(rootDir, "github.properties")
        if (githubPropertiesFile.exists()) {
            val githubProperties = Properties()
            githubProperties.load(githubPropertiesFile.inputStream())
            
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/Cuberto/liquid-swipe-android")
                credentials {
                    username = githubProperties.getProperty("gpr.usr") ?: System.getenv("GPR_USER")
                    password = githubProperties.getProperty("gpr.key") ?: System.getenv("GPR_API_KEY")
                }
            }
        }

        maven {
            url = uri("https://jitpack.io")
        }
    }
}

rootProject.name = "ParentHood"
include(":app")
