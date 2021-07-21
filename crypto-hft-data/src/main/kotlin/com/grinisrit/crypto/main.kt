package com.grinisrit.crypto

import kotlinx.cli.*
import ch.qos.logback.classic.LoggerContext

import com.grinisrit.crypto.binance.*
import com.grinisrit.crypto.bitstamp.BitstampMongoDBHandler
import com.grinisrit.crypto.bitstamp.BitstampWebsocketClient
import com.grinisrit.crypto.bitstamp.BitstampWebsocketRequestBuilder
import com.grinisrit.crypto.coinbase.*
import com.grinisrit.crypto.common.getPubSocket
import com.grinisrit.crypto.common.getSubSocket
import com.grinisrit.crypto.common.mongodb.MongoDBClient
import com.grinisrit.crypto.deribit.*
import com.grinisrit.crypto.kraken.*
import kotlinx.coroutines.*
import org.litote.kmongo.KMongo

import java.io.File
import org.slf4j.LoggerFactory
import kotlin.concurrent.thread


@OptIn(ObsoleteCoroutinesApi::class)
fun main(args: Array<String>) {


    val cliParser = ArgParser("data")

    val configPathArg by cliParser.argument(ArgType.String, description = "Path to .yaml config file").optional()

    cliParser.parse(args)

    val configPath = configPathArg ?: "conf.yaml"

    val config = parseConf(File(configPath).readText())

    // TODO something better
    (LoggerFactory.getILoggerFactory() as LoggerContext).getLogger("org.mongodb.driver").level =
        ch.qos.logback.classic.Level.ERROR


    val pubSocket = getPubSocket(config.zeromq)

    val subSocket = getSubSocket(config.zeromq)


    runBlocking {

        with(config.mongodb) {
            if (isOn) {
                val kMongoClient = KMongo.createClient(address)
                val client = MongoDBClient(subSocket).apply {
                    addHandlers(
                        BinanceMongoDBHandler(kMongoClient),
                        CoinbaseMongoDBHandler(kMongoClient),
                        DeribitMongoDBHandler(kMongoClient),
                        KrakenMongoDBHandler(kMongoClient),
                        BitstampMongoDBHandler(kMongoClient),
                    )
                }

                GlobalScope.launch (Dispatchers.IO) {
                    client.run(this)
                }
            }
        }

        with(config.platforms.binance) {
            if (isOn) {

                val request = BinanceWebsocketRequestBuilder.buildRequest(symbols).first()

                val websocketClient = BinanceWebsocketClient(
                    this,
                    pubSocket,
                    request
                )
                launch {
                    websocketClient.run()
                }


                val apiClient = BinanceAPIClient(this, getSubSocket(config.zeromq, "binance"))
                launch(Dispatchers.IO) {
                    delay(2000)
                    apiClient.run(this)
                }




            }
        }

        with(config.platforms.coinbase) {
            if (isOn) {

                val request = CoinbaseWebsocketRequestBuilder.buildRequest(symbols).first()

                val websocketClient = CoinbaseWebsocketClient(
                    this,
                    pubSocket,
                    request
                )

                launch {
                    websocketClient.run()
                }

            }
        }

        with(config.platforms.deribit) {
            if (isOn) {

                val request = DeribitWebsocketRequestBuilder.buildRequest(symbols).first()

                val websocketClient = DeribitWebsocketClient(
                    this,
                    pubSocket,
                    request
                )

                launch {
                    websocketClient.run()
                }


            }
        }

        with(config.platforms.kraken) {
            if (isOn) {

                val requests = KrakenWebsocketRequestBuilder.buildRequest(symbols)

                val websocketClient = KrakenWebsocketClient(
                    this,
                    pubSocket,
                    requests
                )
                launch {
                    websocketClient.run()
                }


            }
        }

        with(config.platforms.bitstamp) {
            if (isOn) {

                val requests = BitstampWebsocketRequestBuilder.buildRequest(symbols)

                val websocketClient = BitstampWebsocketClient(
                    this,
                    pubSocket,
                    requests
                )
                launch {
                    websocketClient.run()
                }


            }
        }

    }

}
