import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.10"
    id("maven")
    id("net.rdrei.android.buildtimetracker") version "0.11.0"
}

group = "com.github.KamikazeZirou"
version = "1.0-SNAPSHOT"

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath(kotlin("gradle-plugin", "1.4.10"))
    }
}

allprojects {
    tasks.withType<KotlinCompile>().all {
        kotlinOptions.freeCompilerArgs +=
            "-Xuse-experimental=" +
                "kotlin.Experimental," +
                "kotlinx.coroutines.ExperimentalCoroutinesApi," +
                "kotlinx.coroutines.FlowPreview"
    }

    repositories {
        mavenLocal()
        jcenter()
    }
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

buildtimetracker {
    reporters {
        register("summary") {
            options["ordered"] = "true"
            options["barstyle"] = "ascii"
            options["shortenTaskNames"] = "false"
        }
    }
}