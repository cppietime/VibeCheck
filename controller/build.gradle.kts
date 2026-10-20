plugins {
    application
}

dependencies {
    implementation("com.github.kwhat:jnativehook:2.2.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
}

application {
    mainClass.set("com.vibetest.controller.MainKt")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
