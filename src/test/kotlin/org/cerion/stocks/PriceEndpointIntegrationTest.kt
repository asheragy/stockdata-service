package org.cerion.stocks

import io.grpc.ManagedChannelBuilder
import io.micronaut.grpc.server.GrpcEmbeddedServer
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.cerion.stocks.proto.GetPricesRequest
import org.cerion.stocks.proto.Interval
import org.cerion.stocks.proto.PriceServiceGrpc
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
        val response = client.get(request)

        assertEquals("XLE", response.symbol)
        assertFalse(response.cached)
        assertEquals(Interval.Monthly, response.interval)
        assertTrue(response.pricesCount > 0)
        assertTrue(response.getPrices(0).close > 0.0)
    }
}