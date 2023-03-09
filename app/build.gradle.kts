plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.10"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.vertx:vertx-core:4.3.3")
    implementation("io.vertx:vertx-web:4.3.3")
    implementation("io.vertx:vertx-web-client:4.3.3")
    implementation("io.vertx:vertx-lang-kotlin-coroutines:4.3.3")
    implementation("io.vertx:vertx-micrometer-metrics:4.3.3")
    implementation("io.micrometer:micrometer-registry-prometheus:1.9.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useKotlinTest("1.7.10")
        }
    }
}

application {
    mainClass.set("vertx.metrics.bug.AppKt")
}
