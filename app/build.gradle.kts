apply plugin: 'com.android.application'

android {
    compileSdk = 31

    defaultConfig {
        applicationId = "com.stackbleedctrl.pollyn"
        minSdk = 21
        targetSdk = 31
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro')
        }
    }
}

dependencies {
    implementation "org.jetbrains.kotlin:kotlin-stdlib:1.5.31"
    // Add other dependencies here
}
