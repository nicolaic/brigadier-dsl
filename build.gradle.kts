plugins {
    kotlin("jvm") version "1.4.21"
    `java-library`
}

repositories {
    jcenter()
    maven(url = "https://libraries.minecraft.net")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    api("com.mojang:brigadier:1.0.17")
}
