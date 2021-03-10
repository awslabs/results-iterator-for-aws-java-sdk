plugins {
    kotlin("jvm") version "1.4.30"
    id("application")
    id("java")
    id("idea")
    id("java-library")
    id("maven")

    // Adds dependencyUpdates task
    id("com.github.ben-manes.versions") version "0.36.0"
}

extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
}

idea.module.setDownloadSources(true)
idea.module.setDownloadJavadoc(true)

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

val gradleDependencyVersion = "6.8.2"

tasks.wrapper {
    gradleVersion = gradleDependencyVersion
    distributionType = Wrapper.DistributionType.ALL
}

repositories {
    mavenCentral()
    mavenLocal()
    // Required for Gradle Tooling API
    maven(url = "https://repo.gradle.org/gradle/libs-releases-local/")
}


java {
    withSourcesJar()
    withJavadocJar()
}

// Specify all of our dependency versions
val awsSdk2Version = "2.16.3"
val junitVersion = "4.13.2"
val guavaVersion = "30.1-jre"
val hamcrestVersion = "2.2"
val vavrVersion = "0.10.3"
val immutablesValueVersion = "2.8.9-ea-1"
val daggerVersion = "2.32"
val commonsTextVersion = "1.9"
val commonsIoVersion = "2.8.0"
val ztZipVersion = "1.14"
val mockitoVersion = "3.7.7"
val bouncyCastleVersion = "1.68"
val jodahFailsafeVersion = "2.4.0"
val gsonVersion = "2.8.6"
val log4jVersion = "2.14.0"

group = "local"
version = "1.0-SNAPSHOT"

description = """"""

dependencies {
    annotationProcessor("org.immutables:value:$immutablesValueVersion")
    annotationProcessor("org.immutables:gson:$immutablesValueVersion")

    // Dagger code generation
    annotationProcessor("com.google.dagger:dagger-compiler:$daggerVersion")

    // Dependency injection with Dagger
    implementation("com.google.dagger:dagger:$daggerVersion")

    implementation("org.immutables:value:$immutablesValueVersion")
    implementation("org.immutables:gson:$immutablesValueVersion")

    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    api("org.apache.logging.log4j:log4j-slf4j18-impl:$log4jVersion")

    implementation("com.google.guava:guava:$guavaVersion")
    implementation("io.vavr:vavr:$vavrVersion")
    implementation("org.apache.commons:commons-text:$commonsTextVersion")

    // For building Lambda functions
    implementation("commons-io:commons-io:$commonsIoVersion")
    implementation("org.zeroturnaround:zt-zip:$ztZipVersion")
    implementation("org.gradle:gradle-tooling-api:$gradleDependencyVersion")

    // SDK v2
    api("software.amazon.awssdk:aws-core:$awsSdk2Version")
    api("software.amazon.awssdk:iam:$awsSdk2Version")
    api("software.amazon.awssdk:sts:$awsSdk2Version")
    api("software.amazon.awssdk:s3:$awsSdk2Version")
    api("software.amazon.awssdk:greengrass:$awsSdk2Version")
    api("software.amazon.awssdk:iot:$awsSdk2Version")
    api("software.amazon.awssdk:iotdataplane:$awsSdk2Version")
    api("software.amazon.awssdk:lambda:$awsSdk2Version")
    api("software.amazon.awssdk:sqs:$awsSdk2Version")
    api("software.amazon.awssdk:ec2:$awsSdk2Version")
    implementation("software.amazon.awssdk:apache-client:$awsSdk2Version")

    // For certificate based authentication
    implementation("org.bouncycastle:bcprov-jdk15on:$bouncyCastleVersion")
    implementation("org.bouncycastle:bcpkix-jdk15on:$bouncyCastleVersion")

    implementation("com.google.code.gson:gson:$gsonVersion")

    testImplementation("junit:junit:$junitVersion")
    testImplementation("software.amazon.awssdk:iot:$awsSdk2Version")
    testImplementation("software.amazon.awssdk:s3:$awsSdk2Version")
    testImplementation("software.amazon.awssdk:greengrass:$awsSdk2Version")
    testImplementation("org.hamcrest:hamcrest:$hamcrestVersion")
    testImplementation("org.hamcrest:hamcrest-core:$hamcrestVersion")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("net.jodah:failsafe:$jodahFailsafeVersion")
}
