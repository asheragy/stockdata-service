package org.cerion.stocks

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.Repository
import io.micronaut.data.repository.CrudRepository
import org.cerion.stocks.core.web.FetchInterval
import java.io.Serializable
import javax.persistence.*

@Entity
@Table(name = "PRICES")
@IdClass(PriceId::class)
data class PriceDb(@Id var symbol: String,
                   @Id var fetchInterval: FetchInterval,
                   @Id var date: String)


// TODO https://stackoverflow.com/questions/32038177/kotlin-with-jpa-default-constructor-hell
@Introspected
class PriceId : Serializable {
    var symbol: String? = null
    var fetchInterval: FetchInterval? = null
    var date: String? = null
}

@Repository
interface PriceRepository : CrudRepository<PriceDb, PriceId> {
    //@Executable
    //fun find(title: String): Book
}