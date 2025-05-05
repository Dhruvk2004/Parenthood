plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id ("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "com.example.boardingscreen"
    compileSdk = 34

    defaultConfig {
        vectorDrawables.useSupportLibrary = true
        applicationId = "com.example.boardingscreen"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures{
        dataBinding = true
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.firebase:firebase-firestore:25.1.0")
    implementation(platform("com.google.firebase:firebase-bom:33.1.1"))
    implementation("com.google.firebase:firebase-auth:23.0.0")
    implementation ("com.firebaseui:firebase-ui-auth:8.0.0")
    implementation ("com.google.android.gms:play-services-auth:21.2.0")
    implementation ("com.google.android.gms:play-services-code-scanner:16.1.0")
    implementation ("com.google.android.gms:play-services-base:18.5.0")
    implementation ("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.firebase:firebase-database:21.0.0")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation ("com.cuberto:liquid-swipe:1.0.0")
    implementation ("androidx.viewpager2:viewpager2:1.1.0")


    implementation("org.greenrobot:eventbus:3.3.1")

    implementation ("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation ("com.google.zxing:core:3.3.0")

    //smooth nav bar
    implementation("com.github.ibrahimsn98:SmoothBottomBar:1.7.8")

    //Navigation Architecture Component
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.0")

    //curvedbottomnavigation
    implementation ("com.github.qamarelsafadi:CurvedBottomNavigation:0.1.3")

    implementation ("com.google.firebase:firebase-database-ktx")

    implementation ("com.google.code.gson:gson:2.8.9")
    implementation ("com.squareup.okhttp3:okhttp:4.9.3")

    implementation ("com.google.android.libraries.places:places:4.0.0")
    implementation ("com.google.maps:google-maps-services:0.18.0")


}
