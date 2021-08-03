package org.cerion.stocks

import io.grpc.ManagedChannelBuilder
import io.micronaut.grpc.server.GrpcEmbeddedServer
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.*
import org.cerion.stocks.proto.GetPricesRequest
import org.cerion.stocks.proto.Interval
import org.cerion.stocks.proto.PriceServiceGrpc
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject


@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PriceEndpointIntegrationTest {

    @Inject
    lateinit var embeddedServer: GrpcEmbeddedServer

    lateinit var client: PriceServiceGrpc.PriceServiceBlockingStub

    @BeforeAll
    fun setup() {
        val channel = ManagedChannelBuilder.forAddress("127.0.0.1", embeddedServer.serverConfiguration.serverPort).usePlaintext().build()
        client = PriceServiceGrpc.newBlockingStub(channel)
    }

    @Test
    fun test() {
        val request = GetPricesRequest.newBuilder().setSymbol("XLE").setInterval(Interval.Monthly).build();
        var response = client.get(request)

        val count = response.pricesCount
        assertEquals("XLE", response.symbol)
        assertFalse(response.cached)
        assertEquals(Interval.Monthly, response.interval)
        assertTrue(response.pricesCount > 0)
        assertTrue(response.getPrices(0).close > 0.0)

        // Second request should use cache
        response = client.get(request)
        assertTrue(response.cached)
        assertEquals(count, response.pricesCount)
    }

    @Test
    fun concurrency() = runBlocking {
        // Avoid concurrent external requests by making the first one synchronously
        val request = GetPricesRequest.newBuilder().setSymbol("XLE").setInterval(Interval.Monthly).build();
        val initialCount = client.get(request).pricesCount
        val totalCount = AtomicInteger()
        val requests = 10

        val time = measureTimeMillis {
            withContext(Dispatchers.IO) {
                parallelRequest(requests) {
                    println("Starting Request")
                    val response = client.get(request)
                    totalCount.updateAndGet { x -> x + response.pricesCount }
                    println("Ending")
                }
            }
        }

        println("Total milliseconds: $time")
        assertEquals(initialCount * requests, totalCount.get())
    }

    suspend fun parallelRequest(count: Int, action: suspend () -> Unit) {
        coroutineScope {
            repeat(count) {
                launch {
                    action()
                }
            }
        }
    }

    inline fun measureTimeMillis(block: () -> Unit): Long {
        val start = System.currentTimeMillis()
        block()
        return System.currentTimeMillis() - start
    }
}