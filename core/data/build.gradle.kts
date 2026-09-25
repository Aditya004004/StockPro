plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.company.stockpro.core.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.work.runtime.ktx)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.functions)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.messaging)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
