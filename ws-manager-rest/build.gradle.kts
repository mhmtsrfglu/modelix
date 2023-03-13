description = "Allows multiple clients to work on the same set of modules from different sources with rest api"
val ktorVersion: String by rootProject
val kotlinCoroutinesVersion: String by rootProject
val kotlinVersion: String by rootProject
val logbackVersion: String by rootProject
val modelixCoreVersion: String by rootProject

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    kotlin("plugin.spring") version "1.6.21"
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("org.springframework.boot") version "2.7.9"
    id("io.spring.dependency-management") version "1.0.15.RELEASE"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    implementation(kotlin("stdlib"))


    implementation("ch.qos.logback", "logback-classic", logbackVersion)
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", kotlinCoroutinesVersion)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    implementation("com.charleskorn.kaml:kaml:0.40.0")
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.2.0.202206071550-r")
    implementation("org.apache.maven.shared:maven-invoker:3.1.0")
    implementation("org.zeroturnaround:zt-zip:1.14")
    implementation("org.apache.commons:commons-text:1.9")
    implementation("org.jasypt:jasypt:1.9.3")
    implementation("org.modelix:model-client:$modelixCoreVersion")
    implementation("org.modelix.mpsbuild:build-tools:1.0.6")

    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    //implementation("org.keycloak:keycloak-spring-boot-starter:20.0.3")
    implementation("org.jboss.resteasy:resteasy-client:4.7.7.Final")

    implementation(project(":workspaces"))
    implementation(project(":headless-mps"))
}

tasks.jar {
    manifest {
        archiveVersion.set("latest")
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}