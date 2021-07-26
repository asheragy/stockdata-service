package org.cerion.stocks

import io.grpc.stub.StreamObserver
import org.cerion.stocks.core.web.FetchInterval
import org.cerion.stocks.core.web.clients.YahooFinance
import org.cerion.stocks.proto.GetPricesReply
import org.cerion.stocks.proto.GetPricesRequest
import org.cerion.stocks.proto.Interval
import org.cerion.stocks.proto.PriceServiceGrpc
import java.lang.IllegalArgumentException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PricesEndpoint : PriceServiceGrpc.PriceServiceImplBase() {
    private val dataSource = YahooFinance.instance

    @Inject
    lateinit var repository: PriceRepository

    override fun get(request: GetPricesRequest, responseObserver: StreamObserver<GetPricesReply>) {
        val symbol = request.symbol
        val interval = when(request.interval) {
            Interval.Daily -> FetchInterval.DAILY
            Interval.Weekly -> FetchInterval.WEEKLY
            Interval.Monthly -> FetchInterval.MONTHLY
            else -> throw IllegalArgumentException()
        }

        val cached = repository.findByList(symbol, interval)
        val prices = if(cached.isNotEmpty())
            cached
        else {
            val updated = dataSource.getPrices(request.symbol, interval, null)
            val pricesDb = updated.map { it.toDb(request.symbol, interval) }

            repository.saveAll(pricesDb).toList()
        }

        val reply = GetPricesReply.newBuilder()
                .setCached(cached.isNotEmpty())
                .setSymbol(symbol)
                .setInterval(when(interval) {
                    FetchInterval.DAILY -> Interval.Daily
                    FetchInterval.WEEKLY -> Interval.Weekly
                    FetchInterval.MONTHLY -> Interval.Monthly
                })
                .addAllPrices(prices.map { org.cerion.stocks.proto.Price.newBuilder()
                        .setDate(it.date)
                        .setOpen(it.open)
                        .setHigh(it.high)
                        .setLow(it.low)
                        .setClose(it.close)
                        .setVolume(it.volume)
                        .build()
                })
        responseObserver.onNext(reply.build())
        responseObserver.onCompleted()
    }
}