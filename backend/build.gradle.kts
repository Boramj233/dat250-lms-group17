plugins {
	java
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "no.hvl.dat250"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
    implementation(platform("software.amazon.awssdk:bom:2.59.1"))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:url-connection-client")
	implementation("org.springframework.boot:spring-boot-h2console")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client")
	implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    runtimeOnly("com.h2database:h2")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // Web
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    // Database
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-h2console")
    runtimeOnly("com.h2database:h2")

    // Security

    // Messaging

    // Tests
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// Explicit opt-in: ordinary test/build tasks do not require Docker.
val storageIntegrationTest by sourceSets.creating
configurations[storageIntegrationTest.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
configurations[storageIntegrationTest.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())
dependencies {
    add(storageIntegrationTest.implementationConfigurationName, sourceSets.main.get().output)
    // Reuse the team test JWT signer/decoder and isolated test profile.
    add(storageIntegrationTest.implementationConfigurationName, sourceSets.test.get().output)
    add(storageIntegrationTest.implementationConfigurationName, "org.testcontainers:testcontainers:2.0.5")
}
tasks.register<Test>("storageIntegrationTest") {
    description = "Tests the S3 adapter against a real, pinned SeaweedFS container (requires Docker)."
    group = "verification"
    testClassesDirs = storageIntegrationTest.output.classesDirs
    classpath = storageIntegrationTest.runtimeClasspath
    systemProperty("storage.init-script", rootProject.file("infra/seaweedfs/start.sh").absolutePath)
    shouldRunAfter(tasks.test)
}
