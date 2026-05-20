plugins {
    kotlin("jvm") version "2.3.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation(kotlin("test"))
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
    from(sourceSets.main.get().output)
}

kotlin {
    jvmToolchain(24)
}

tasks.test {
    useJUnitPlatform()
}