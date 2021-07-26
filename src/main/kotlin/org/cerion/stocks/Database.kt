package org.cerion.stocks

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.Repository
import io.micronaut.data.repository.CrudRepository
import io.micronaut.data.repository.GenericRepository
import org.cerion.stocks.core.PriceRow
import org.cerion.stocks.core.web.FetchInterval
import java.io.Serializable
import javax.persistence.*

@Entity
@Table(name = "prices")
@IdClass(PriceId::class)
data class PriceDb(@Id val symbol: String,
                   @Enumerated(EnumType.STRING)
                   @Id val fetch_interval: FetchInterval,
                   @Id val date: String,
                   val open: Float,
                   val high: Float,
                   val low: Float,
                   val close: Float,
                   val volume: Float)

@Introspected
data class PriceId(val symbol: String = "",
                   val fetch_interval: FetchInterval = FetchInterval.DAILY,
                   val date: String = "") : Serializable

@Repository
interface PriceRepository : CrudRepository<PriceDb, PriceId> {
    @io.micronaut.data.annotation.Query("FROM PriceDb p WHERE p.symbol = :symbol AND p.fetch_interval = :fetchInterval")
    fun findByList(symbol: String, fetchInterval: FetchInterval): List<PriceDb>
}


fun PriceRow.toDb(symbol: String, interval: FetchInterval) = PriceDb(symbol, interval, date.toISOString(), open, high, low, close, volume)