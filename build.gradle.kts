plugins {
    kotlin("jvm") version "1.3.72"
    `java-library`
}

repositories {
    jcenter()
    maven(url = "https://libraries.minecraft.net")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.mojang:brigadier:1.0.17")
}
