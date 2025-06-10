plugins {
    id("kotlin-kapt")
    id("com.google.gms.google-services")

    id("com.android.application")
    kotlin("android")

}

android {
    namespace = "com.example.gittest"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.gittest"
        minSdk = 31
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


    //추가한 것들
    // — 코루틴
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // — Jetpack Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.1")

    // — Jetpack Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.9.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.0")

    // — RecyclerView (현재 최신)
    implementation("androidx.recyclerview:recyclerview:1.3.0")

    // — Firebase (BOM 관리)
    implementation(platform("com.google.firebase:firebase-bom:32.1.1"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // — Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // — TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.3")
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // AndroidX
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.9.0")

    
//
//    kapt("com.github.bumptech.glide:compiler:4.16.0")
//
//// Firebase 코루틴 연동
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")
//
//// ViewPager2
//    implementation("androidx.viewpager2:viewpager2:1.0.0")

    // Glide core
    implementation("com.github.bumptech.glide:glide:4.16.0")

// Glide annotation processor (for @GlideModule)
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    implementation("androidx.gridlayout:gridlayout:1.0.0")
}

