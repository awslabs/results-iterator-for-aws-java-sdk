plugins {
    kotlin("jvm") version "1.6.10"
    id("application")
    id("java")
    id("idea")
    id("java-library")
    id("maven-publish")
}

// Required for Gradle 7.x and JitPack
publishing.publications.create<MavenPublication>("maven").from(components["java"])

extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
}

idea.module.isDownloadSources = true
idea.module.isDownloadJavadoc = true

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

val gradleDependencyVersion = "7.4.1"
val gradleToolingApiDependencyVersion = "7.1"

tasks.wrapper {
    gradleVersion = gradleDependencyVersion
    distributionType = Wrapper.DistributionType.ALL
}

// To avoid this error in distTar:
//   > Entry results-iterator-for-aws-java-sdk-1.0-SNAPSHOT/lib/gson-2.9.0.jar is a duplicate but no duplicate handling strategy has been set. Please refer to https://docs.gradle.org/7.4.1/dsl/org.gradle.api.tasks.Copy.html#org.gradle.api.tasks.Copy:duplicatesStrategy for details.
//
// We follow the advice from https://github.com/gradle/gradle/issues/17236#issuecomment-870525957
gradle.taskGraph.whenReady {
    allTasks
        .filter { it.hasProperty("duplicatesStrategy") } // Because it's some weird decorated wrapper that I can't cast.
        .forEach {
            it.setProperty("duplicatesStrategy", "INCLUDE")
        }
}

repositories {
    mavenCentral()
    google()
    // For Greengrass CLI libraries
    maven(url = "https://jitpack.io")
    // Required for Gradle Tooling API
    maven(url = "https://repo.gradle.org/gradle/libs-releases/")
}

java {
    withSourcesJar()
    withJavadocJar()
}

sourceSets.create("integrationTest") {
    java {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
        compileClasspath += sourceSets.test.get().output
        runtimeClasspath += sourceSets.test.get().output

        srcDir(file("src/integration-test/java"))
    }
}

configurations.getByName("integrationTestImplementation") { extendsFrom(configurations.testImplementation.get()) }
configurations.getByName("integrationTestApi") { extendsFrom(configurations.testApi.get()) }

val integrationTestTask = tasks.register("integrationTest", Test::class) {
    description = "Runs the integration tests."
    group = "verification"
    testClassesDirs = sourceSets.getByName("integrationTest").output.classesDirs
    classpath = sourceSets.getByName("integrationTest").runtimeClasspath
    outputs.upToDateWhen { false }
    mustRunAfter(tasks.getByName("test"))
}

// Specify all of our dependency versions
val awsSdk2Version = "2.20.148"
val junitVersion = "4.13.2"
val guavaVersion = "31.1-jre"
val hamcrestVersion = "2.2"
val vavrVersion = "0.10.4"
val vavrJacksonVersion = "0.10.3"
val vavrGsonVersion = "0.10.2"
val immutablesValueVersion = "2.9.0"
val daggerVersion = "2.42"
val commonsTextVersion = "1.9"
val commonsIoVersion = "2.11.0"
val ztZipVersion = "1.15"
val mockitoVersion = "4.6.1"
val bouncyCastleVersion = "1.70"
val jodahFailsafeVersion = "2.4.4"
val gsonVersion = "2.9.0"

configurations.all {
    // Check for updates on changing dependencies at most every 10 minutes
    resolutionStrategy.cacheChangingModulesFor(10, TimeUnit.MINUTES)
}

group = "local"
version = "1.0-SNAPSHOT"

dependencies {
    annotationProcessor("org.immutables:value:$immutablesValueVersion")
    annotationProcessor("org.immutables:gson:$immutablesValueVersion")

    // Dagger code generation
    annotationProcessor("com.google.dagger:dagger-compiler:$daggerVersion")

    // Dependency injection with Dagger
    api("com.google.dagger:dagger:$daggerVersion")

    api("org.immutables:value:$immutablesValueVersion")
    api("org.immutables:gson:$immutablesValueVersion")

    implementation("com.google.guava:guava:$guavaVersion")
    implementation("io.vavr:vavr:$vavrVersion")
    implementation("io.vavr:vavr-gson:$vavrGsonVersion")
    implementation("io.vavr:vavr-jackson:$vavrJacksonVersion")
    implementation("org.apache.commons:commons-text:$commonsTextVersion")

    // For building Lambda functions
    implementation("commons-io:commons-io:$commonsIoVersion")
    implementation("org.zeroturnaround:zt-zip:$ztZipVersion")
    api("org.gradle:gradle-tooling-api:$gradleToolingApiDependencyVersion")

    // SDK v2
    api("software.amazon.awssdk:aws-core:$awsSdk2Version")
    api("software.amazon.awssdk:iam:$awsSdk2Version")
    api("software.amazon.awssdk:sts:$awsSdk2Version")
    api("software.amazon.awssdk:s3:$awsSdk2Version")
    api("software.amazon.awssdk:greengrass:$awsSdk2Version")
    api("software.amazon.awssdk:greengrassv2:$awsSdk2Version")
    api("software.amazon.awssdk:iot:$awsSdk2Version")
    api("software.amazon.awssdk:iotdataplane:$awsSdk2Version")
    api("software.amazon.awssdk:lambda:$awsSdk2Version")
    api("software.amazon.awssdk:sqs:$awsSdk2Version")
    api("software.amazon.awssdk:ec2:$awsSdk2Version")
    api("software.amazon.awssdk:cloudformation:$awsSdk2Version")
    api("software.amazon.awssdk:dynamodb:$awsSdk2Version")
    api("software.amazon.awssdk:apache-client:$awsSdk2Version")

    // For certificate based authentication
    implementation("org.bouncycastle:bcprov-jdk15on:$bouncyCastleVersion")
    implementation("org.bouncycastle:bcpkix-jdk15on:$bouncyCastleVersion")

    implementation("com.google.code.gson:gson:$gsonVersion")

    // For GreengrassV2 ComponentRecipe class
    api("com.github.aws-greengrass:aws-greengrass-component-common:main-SNAPSHOT") { isChanging = true }

    testImplementation("junit:junit:$junitVersion")
    testImplementation("software.amazon.awssdk:iot:$awsSdk2Version")
    testImplementation("software.amazon.awssdk:s3:$awsSdk2Version")
    testImplementation("software.amazon.awssdk:greengrass:$awsSdk2Version")
    testImplementation("org.hamcrest:hamcrest:$hamcrestVersion")
    testImplementation("org.hamcrest:hamcrest-core:$hamcrestVersion")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("net.jodah:failsafe:$jodahFailsafeVersion")
}

