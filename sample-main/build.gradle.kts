import org.jetbrains.kotlin.kapt3.base.Kapt.kapt

plugins {
    kotlin("jvm")
    kotlin("kapt")
}

kapt {
    generateStubs = true
}

sourceSets {
    getByName("main").java.srcDirs("src")
}

dependencies {
    kapt(project(":generator"))
    compileOnly(project(":generator"))
}