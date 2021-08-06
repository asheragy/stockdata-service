package org.cerion.stocks

import kotlinx.coroutines.*
import org.cerion.stocks.core.web.FetchInterval
import org.cerion.stocks.core.web.clients.YahooFinance
import org.cerion.stocks.proto.GetPricesReply
import org.cerion.stocks.proto.GetPricesRequest
import org.cerion.stocks.proto.Interval
import org.cerion.stocks.proto.PriceServiceImplBase
import java.util.concurrent.Executors.newFixedThreadPool
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PriceEndpoint : PriceServiceImplBase(coroutineContext = newFixedThreadPool(4).asCoroutineDispatcher()) {

    private val dataSource = YahooFinance.instance

    @Inject
    lateinit var repository: PriceRepository

    override suspend fun get(request: GetPricesRequest): GetPricesReply {
        val symbol = request.symbol
        val interval = when(request.interval) {
            Interval.Daily -> FetchInterval.DAILY
            Interval.Weekly -> FetchInterval.WEEKLY
            Interval.Monthly -> FetchInterval.MONTHLY
            else -> throw IllegalArgumentException()
        }

        val priceResponse = getPricesAsync(symbol, interval).await()
        val prices = priceResponse.first
        val cached = priceResponse.second

        val reply = GetPricesReply.newBuilder()
            .setCached(cached)
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

        return reply.build()
    }

    private fun getPricesAsync(symbol: String, interval: FetchInterval) = GlobalScope.async {
        val cached = repository.findByList(symbol, interval)
        val prices = if (cached.isNotEmpty())
            cached
        else {
            val updated = dataSource.getPrices(symbol, interval, null)
            val pricesDb = updated.map { it.toDb(symbol, interval) }

            repository.saveAll(pricesDb).toList()
        }

        Pair(prices, cached.isNotEmpty())
    }

}