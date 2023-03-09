package vertx.metrics.bug

import io.micrometer.core.instrument.Tag
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.micrometer.MicrometerMetricsOptions
import io.vertx.micrometer.PrometheusScrapingHandler
import io.vertx.micrometer.VertxPrometheusOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

class App {
    private val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    private val registryName = UUID.randomUUID().toString()
    private val vertxWithMetrics = Vertx.vertx(
        VertxOptions()
            .setMetricsOptions(
                MicrometerMetricsOptions()
                    .setRegistryName(registryName)
                    .setMicrometerRegistry(registry)
                    .setPrometheusOptions(VertxPrometheusOptions().setEnabled(true))
                    .setServerRequestTagsProvider {
                        if (it.method() == HttpMethod.GET ) {
                            setOf(Tag.of("foo", "bar"))
                        } else {
                            emptySet()
                        }
                    }
                    .setEnabled(true)
            )
    )
    private val metrics = object : CoroutineVerticle() {
        private var server: HttpServer? = null
        val port get() = server?.actualPort() ?: error("Start server first.")

        override suspend fun start() {
            val prometheusHandler = PrometheusScrapingHandler.create(registryName)
            val router = Router.router(vertx)
            router.get("/metrics")
                .coroutineHandler(this) {
                    prometheusHandler.handle(it)
                }
            server = vertx.createHttpServer().requestHandler(router).listen(0).await()
        }

        override suspend fun stop() {
            server?.close()?.await()
        }
    }

    private val api = object : CoroutineVerticle() {
        private var server: HttpServer? = null
        val port get() = server?.actualPort() ?: error("Start server first.")

        override suspend fun start() {
            val router = Router.router(vertx)
            router
                .get("/hello")
                .coroutineHandler(this) { it.response().end("world").await() }
            router
                .post("/post-hello")
                .coroutineHandler(this) { it.response().apply { statusCode = 202 }.end("will be done.").await() }

            server = vertx.createHttpServer().requestHandler(router).listen(0).await()
        }

        override suspend fun stop() {
            server?.close()?.await()
        }
    }

    val apiPort get() = api.port
    val metricsPort get() = metrics.port

    suspend fun start() {
        vertxWithMetrics.deployVerticle(api).await()
        vertxWithMetrics.deployVerticle(metrics).await()
    }

    suspend fun stop() {
        vertxWithMetrics.close().await()
        registry.close()
    }

    private inline fun Route.coroutineHandler(scope: CoroutineScope, crossinline fn: suspend (RoutingContext) -> Unit) =
        handler { context ->
            scope.launch(context.vertx().dispatcher()) {
                runCatching { fn(context) }
                    .getOrElse { context.fail(it) }
            }
        }
}
