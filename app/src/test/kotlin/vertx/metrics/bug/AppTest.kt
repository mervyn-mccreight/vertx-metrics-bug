package vertx.metrics.bug

import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AppTest {

    private lateinit var app: App

    @BeforeTest
    fun startApp() = runBlocking {
        app = App()
        app.start()
    }

    @AfterTest
    fun stopApp() = runBlocking {
        app.stop()
    }

    @Test
    fun catchMetrics(): Unit = runBlocking {
        val vertx = Vertx.vertx()
        val client = WebClient.create(vertx)

        try {
            client.get(app.apiPort, "localhost", "/metrics")
                .send()
                .await()
                .also {
                    println(it.bodyAsString())
                    println()
                }

            assertEquals("world", client.get(app.apiPort, "localhost", "/hello").send().await().bodyAsString())
            assertEquals(
                "will be done.",
                client.post(app.apiPort, "localhost", "/post-hello").send().await().bodyAsString()
            )

            client.get(app.apiPort, "localhost", "/metrics")
                .send()
                .await()
                .also {
                    println(it.bodyAsString())
                    println()
                }
        } finally {
            client.close()
            vertx.close().await()
        }
    }
}
