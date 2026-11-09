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
	// Web
	implementation("org.springframework.boot:spring-boot-starter-webmvc")

	// Database
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-h2console")
	runtimeOnly("com.h2database:h2")

	// Security: OAuth 2.0 client (login with Keycloak) and resource server (our own API tokens)
	implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client")
	implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")

	// Messaging

	// Tests
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// bootRun reads the repository's .env (as Docker Compose does), so the local backend
// gets KEYCLOAK_CLIENT_SECRET without copying the secret anywhere else.
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
	val envFile = rootProject.file(".env")
	if (envFile.exists()) {
		envFile.readLines()
			.map { it.trim() }
			.filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
			.forEach { line ->
				val name = line.substringBefore("=").trim()
				if (System.getenv(name) == null) {
					environment(name, line.substringAfter("=").trim())
				}
			}
	}
}
