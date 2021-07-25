package org.cerion.stocks

import io.grpc.stub.StreamObserver
import org.cerion.stocks.core.Price
import org.cerion.stocks.core.web.FetchInterval
import org.cerion.stocks.core.web.clients.YahooFinance
import org.cerion.stocks.proto.GetPricesReply
import org.cerion.stocks.proto.GetPricesRequest
import org.cerion.stocks.proto.PriceServiceGrpc
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PricesEndpoint : PriceServiceGrpc.PriceServiceImplBase() {
    private val dataSource = YahooFinance.instance

    @Inject
    lateinit var repository: PriceRepository

    override fun get(request: GetPricesRequest, responseObserver: StreamObserver<GetPricesReply>) {
        val interval = FetchInterval.MONTHLY
        val symbol = request.symbol

        val cached = repository.findByList(symbol, interval)
        val prices = if(cached.isNotEmpty())
            cached
        else {
            val updated = dataSource.getPrices(request.symbol, interval, null)
            val pricesDb = updated.map { PriceDb(request.symbol, interval, it.date.toISOString()) }

            repository.saveAll(pricesDb).toList()
        }

        val reply = GetPricesReply.newBuilder()
                .setCached(cached.isNotEmpty())
                .setMessage("Found: " + prices.size)
        responseObserver.onNext(reply.build())
        responseObserver.onCompleted()
    }
}