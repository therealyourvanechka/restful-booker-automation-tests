plugins {
    id("java")
    id("io.qameta.allure") version "4.0.2"
}

group = "com.booker"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

allure {
    version = "2.42.0"
    report {
        reportDir.set(layout.buildDirectory.dir("allure-report"))
    }
}

dependencies {
    // REST Assured + Jackson
    implementation("io.rest-assured:rest-assured:5.5.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.3")
    implementation("io.github.cdimascio:dotenv-java:3.1.0")

    // JUnit 5
    testImplementation(platform("org.junit:junit-bom:5.11.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // AssertJ
    testImplementation("org.assertj:assertj-core:3.26.3")

    // JSON Schema Validation
    testImplementation("io.rest-assured:json-schema-validator:5.5.0")

    // Allure
    implementation("io.qameta.allure:allure-java-commons:2.29.1")
    testImplementation("io.qameta.allure:allure-rest-assured:2.29.1")

    // DataFaker
    testImplementation("net.datafaker:datafaker:2.4.3")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")
    testCompileOnly("org.projectlombok:lombok:1.18.36")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.36")
}

tasks.register("generateAllureEnvironment") {
    doLast {
        val resultsDir = file("build/allure-results")
        resultsDir.mkdirs()
        resultsDir.resolve("environment.properties").writeText("""
            Base.URL=https://restful-booker.herokuapp.com
            Test.Type=API
            Build.Version=${project.version}
        """.trimIndent())
    }
}

tasks.test {
    useJUnitPlatform()
    finalizedBy("generateAllureEnvironment")
    finalizedBy("allureReport")
    doFirst {
        delete("build/allure-results")
    }
}
tasks.named("allureReport") {
    mustRunAfter("generateAllureEnvironment")
}

tasks.allureReport {
    clean.set(true)
    doFirst {
        delete(layout.buildDirectory.dir("allure-report"))
    }
}