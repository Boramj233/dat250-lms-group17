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
<<<<<<< HEAD

// Windows runs npm through its command script; other systems use npm directly.
val npmCommand = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
    listOf("cmd", "/c", "npm.cmd")
} else {
    listOf("npm")
}

val installFrontend = tasks.register<Exec>("installFrontend") {
    workingDir = rootProject.file("frontend")
    commandLine(npmCommand + listOf("ci"))

    inputs.files(
        rootProject.file("frontend/package.json"),
        rootProject.file("frontend/package-lock.json")
    )
    outputs.dir(rootProject.file("frontend/node_modules"))
}

val buildFrontend = tasks.register<Exec>("buildFrontend") {
    dependsOn(installFrontend)

    workingDir = rootProject.file("frontend")
    commandLine(npmCommand + listOf("run", "build"))

    inputs.dir(rootProject.file("frontend/src"))
    inputs.files(
        rootProject.file("frontend/index.html"),
        rootProject.file("frontend/vite.config.ts"),
        rootProject.file("frontend/package.json")
    )
    outputs.dir(rootProject.file("frontend/dist"))
}
/* 
tasks.named<ProcessResources>("processResources") {
    dependsOn(buildFrontend)

    from(rootProject.file("frontend/dist")) {
        into("static")
    }
}
*/
=======
>>>>>>> 1c51e719e80e0c3db55092b924bb4f4cd8bce5d7
