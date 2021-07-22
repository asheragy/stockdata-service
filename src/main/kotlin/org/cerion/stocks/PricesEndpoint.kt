package org.cerion.stocks

import io.grpc.stub.StreamObserver
import org.cerion.stocks.core.web.FetchInterval
import org.cerion.stocks.core.web.clients.YahooFinance
import org.cerion.stocks.proto.GetPricesReply
import org.cerion.stocks.proto.GetPricesRequest
import org.cerion.stocks.proto.PriceServiceGrpc
import javax.inject.Singleton

@Singleton
class PricesEndpoint : PriceServiceGrpc.PriceServiceImplBase() {
    private val dataSource = YahooFinance.instance

    override fun get(request: GetPricesRequest, responseObserver: StreamObserver<GetPricesReply>) {
        val prices = dataSource.getPrices("XLE", FetchInterval.MONTHLY, null)

        val reply = GetPricesReply.newBuilder().setMessage("Found: " + prices.size)
        responseObserver.onNext(reply.build())
        responseObserver.onCompleted()
    }
}