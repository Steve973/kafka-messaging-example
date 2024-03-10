plugins {
    java
    id("org.springframework.boot") version "3.2.3"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "org.storck"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

sourceSets {
    create("itest") {
        java.srcDir("src/itest/java")
        resources.srcDir("src/itest/resources")
        compileClasspath += sourceSets.main.get().output + configurations.testRuntimeClasspath.get()
        runtimeClasspath += output + compileClasspath
    }
}

tasks.named<Copy>("processItestResources").configure {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.springBootStarterWeb)
    implementation(libs.kafkaStreams)
    implementation(libs.springKafka)
    implementation(libs.springdocOpenapiStarterWebmvcUi)
    implementation(libs.springKafka)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testImplementation(libs.springBootStarterTest)
    testImplementation(libs.junitJupiterApi)
    testImplementation(libs.junitJupiterEngine)
    testImplementation(libs.junitJupiterParams)
    testImplementation(libs.springBootTestcontainers)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainersJunitJupiter)
    testImplementation(libs.testcontainersRedpanda)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val itest by tasks.registering(Test::class) {
    group = "verification"
    description = "Runs the integration tests."
    testClassesDirs = sourceSets.named("itest").get().output.classesDirs
    classpath = sourceSets.named("itest").get().runtimeClasspath
    dependsOn("itestClasses")
}