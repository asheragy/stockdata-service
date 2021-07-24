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
        val prices = dataSource.getPrices("XLE", FetchInterval.MONTHLY, null)

        try {
            val pricesDb = prices.map { PriceDb(request.symbol, FetchInterval.DAILY, it.date.toISOString()) }
            val p1 = pricesDb[0]

            repository.saveAll(pricesDb)

            val test = repository.findById(PriceId().apply {
                symbol = p1.symbol
                fetchInterval = p1.fetchInterval
                date = p1.date
            })

            println(test)

        }
        catch (e: Exception) {
            e.printStackTrace();
        }


        val reply = GetPricesReply.newBuilder().setMessage("Found: " + prices.size)
        responseObserver.onNext(reply.build())
        responseObserver.onCompleted()
    }
}