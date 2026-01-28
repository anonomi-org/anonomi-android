package org.anonchatsecure.anonchat.headless.event

import io.javalin.websocket.WsContext
import org.anonchatsecure.bramble.api.lifecycle.IoExecutor
import org.anonchatsecure.anonchat.headless.json.JsonDict
import javax.annotation.concurrent.ThreadSafe

@ThreadSafe
interface WebSocketController {

    val sessions: MutableSet<WsContext>

    /**
     * Sends an event to all open sessions using the [IoExecutor].
     */
    fun sendEvent(name: String, obj: JsonDict)

}
