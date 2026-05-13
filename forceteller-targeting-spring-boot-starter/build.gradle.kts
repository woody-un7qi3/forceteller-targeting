dependencies {
    api(project(":forceteller-targeting-core"))
    implementation(libs.spring.boot.autoconfigure)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.spring.boot.starter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

publishing {
    publications {
        named<MavenPublication>("maven") {
            pom {
                name.set("forceteller-targeting-spring-boot-starter")
                description.set("Spring Boot auto-configuration for forceteller-targeting")
            }
        }
    }
}
