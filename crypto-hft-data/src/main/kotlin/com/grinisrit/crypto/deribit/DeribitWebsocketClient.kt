package com.grinisrit.crypto.deribit

import com.grinisrit.crypto.DeribitPlatform
import com.grinisrit.crypto.common.websocket.SingleRequestWebsocketClient
import org.zeromq.ZMQ

class DeribitWebsocketClient(
    platform: DeribitPlatform,
    socket : ZMQ.Socket,
    request: String
) : SingleRequestWebsocketClient(
    platform,
    socket,
    request,
    backendReconnectTimeout = 10000L,
    socketTimeoutMillis = 40000L// TODO()!!!!
)