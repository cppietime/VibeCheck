plugins {
    application
}

dependencies {
    implementation("com.github.kwhat:jnativehook:2.2.2")
}

application {
    mainClass.set("com.vibetest.controller.MainKt")
}
