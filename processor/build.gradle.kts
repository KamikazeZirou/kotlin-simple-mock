plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("maven")
    id("maven-publish")
}

group = "com.github.KamikzeZirou"
version = "0.0.1-SNAPSHOT"

sourceSets {
    getByName("main").java.srcDirs("src")
}

dependencies {
    implementation("com.squareup:kotlinpoet:1.7.2")
    implementation("com.squareup:kotlinpoet-metadata:1.7.2")
    implementation("com.squareup:kotlinpoet-metadata-specs:1.7.2")
    implementation("com.squareup:kotlinpoet-classinspector-elements:1.7.2")
    implementation("com.squareup:kotlinpoet-classinspector-reflective:1.7.2")
    implementation("com.google.auto.service:auto-service:1.0-rc7")
    implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.2.0")
    kapt("com.google.auto.service:auto-service:1.0-rc7")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.github.KamikzeZirou"
            artifactId = "kotlin-fast-mock"
            version = "0.0.1"
            from(components["kotlin"])
        }
    }
    repositories {
        maven {
            url = uri("file://$buildDir/repos")
        }
    }
}