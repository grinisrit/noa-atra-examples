package com.grinisrit.crypto

import com.grinisrit.crypto.analysis.countTimeWeightedMetricsAndLiquidity
import com.grinisrit.crypto.coinbase.CoinbaseMongoClient
import com.grinisrit.crypto.coinbase.CoinbaseRefinedDataPublisher
import com.grinisrit.crypto.common.mongo.getMongoDBServer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList


// Make sure to add to the VM options:
// -Djava.library.path=${HOME}/.konan/third-party/noa-v0.0.1/cpp-build/jnoa
suspend fun main(args: Array<String>) = coroutineScope {
    val config = loadConf(args)

    val amount = 10
    val symbol = "BTC-USD"

    val bidAskPt = "../cryptofed/bidask/$DATE/coinbaseBidAsk${amount}BTCUSD.pt"
    val timePt = "../cryptofed/bidask/$DATE/coinbaseTime${amount}BTCUSD.pt"

    val mongoClient = CoinbaseMongoClient(config.mongodb.getMongoDBServer())
    val snapshotsList = mongoClient.loadSnapshots(symbol).toList()
    val updatesFlow = mongoClient.loadUpdates(symbol)
    val orderBookFlow = CoinbaseRefinedDataPublisher.orderBookFlow(snapshotsList, updatesFlow)
    val spreadMetrics = countTimeWeightedMetricsAndLiquidity(orderBookFlow, listOf(amount))[amount]!!

    saveBidAskMetric(spreadMetrics, bidAskPt, timePt)

}
