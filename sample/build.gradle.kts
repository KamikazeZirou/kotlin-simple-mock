plugins {
    kotlin("jvm")
    kotlin("kapt")
}

kapt {
    generateStubs = true

    arguments {
        // Specify the path of the generated mock.
        // In the default path, the product code will contain mocks.
        // So, specify the directory of auto-generated files for testing
        arg("kotlin.fast.mock.generated", "${buildDir}/generated/source/kaptKotlin/test")
    }
}

sourceSets {
    getByName("main").java.srcDirs("src")
    getByName("test").java.srcDirs("test")
}

dependencies {
    kapt(project(":processor"))
    compileOnly(project(":processor"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.1")

    testImplementation("junit:junit:4.13.1")
    testImplementation("com.google.truth:truth:1.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.4.1")
    testImplementation("io.mockk:mockk:1.10.0")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
}