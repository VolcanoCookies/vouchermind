val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project


plugins {
	kotlin("jvm") version "1.8.0"
	kotlin("plugin.serialization") version "1.8.0"
	id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "net.volcano"
version = "1.0-SNAPSHOT"

repositories {
	mavenCentral()
}

dependencies {
	// Kord discord lib
	implementation("dev.kord:kord-core:0.8.0-M17")
	// KMongo database lib
	implementation("org.litote.kmongo:kmongo-serialization:4.8.0")
	// Koin dependency injection
	implementation("io.insert-koin:koin-core:3.3.2")
	// Coroutines
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
	// Serialization
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
	// Logback
	implementation("ch.qos.logback:logback-classic:$logback_version")
}

tasks.withType<Jar> {
	manifest {
		attributes["Main-Class"] = "net.volcano.ApplicationKt"
	}
}