dependencies {
    api(libs.jackson.databind)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

publishing {
    publications {
        named<MavenPublication>("maven") {
            pom {
                name.set("forceteller-targeting-core")
                description.set("Forceteller targeting rule engine — framework-agnostic core")
            }
        }
    }
}
