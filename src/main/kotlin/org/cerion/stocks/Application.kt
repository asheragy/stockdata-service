package org.cerion.stocks

import io.micronaut.runtime.Micronaut.*
fun main(args: Array<String>) {
	build()
	    .args(*args)
		.packages("org.cerion.stocks")
		.start()
}

