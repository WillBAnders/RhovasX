plugins {
    kotlin("jvm") version "1.4.0"
    application
}

group = "dev.willbanders.rhovas.x"
version = "0.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

application {
    mainClassName = "dev.willbanders.rhovas.x.MainKt"
}

tasks.getByName<JavaExec>("run") {
    standardInput = System.`in`
    standardOutput = System.`out`
}
