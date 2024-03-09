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
}

tasks.withType<Test> {
    useJUnitPlatform()
}
