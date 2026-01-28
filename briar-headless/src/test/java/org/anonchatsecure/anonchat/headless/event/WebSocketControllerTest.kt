package org.anonchatsecure.anonchat.headless.event

import io.javalin.plugin.json.JavalinJson.toJson
import io.javalin.websocket.WsContext
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.anonchatsecure.bramble.test.ImmediateExecutor
import org.anonchatsecure.bramble.test.TestUtils.getRandomId
import org.anonchatsecure.anonchat.api.autodelete.AutoDeleteConstants.NO_AUTO_DELETE_TIMER
import org.anonchatsecure.anonchat.api.client.SessionId
import org.anonchatsecure.anonchat.api.identity.AuthorInfo
import org.anonchatsecure.anonchat.api.identity.AuthorInfo.Status.VERIFIED
import org.anonchatsecure.anonchat.api.introduction.IntroductionRequest
import org.anonchatsecure.anonchat.api.introduction.event.IntroductionRequestReceivedEvent
import org.anonchatsecure.anonchat.api.messaging.PrivateMessageHeader
import org.anonchatsecure.anonchat.api.messaging.event.PrivateMessageReceivedEvent
import org.anonchatsecure.anonchat.headless.ControllerTest
import org.anonchatsecure.anonchat.headless.messaging.EVENT_CONVERSATION_MESSAGE
import org.anonchatsecure.anonchat.headless.messaging.output
import org.eclipse.jetty.websocket.api.WebSocketException
import org.eclipse.jetty.websocket.common.io.FutureWriteCallback
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.IOException

internal class WebSocketControllerTest : ControllerTest() {

    private val session1 = mockk<WsContext>()
    private val session2 = mockk<WsContext>()

    private val controller = WebSocketControllerImpl(ImmediateExecutor())

    private val header =
        PrivateMessageHeader(
            message.id,
            group.id,
            timestamp,
            true,
            true,
            true,
            true,
            true,
            emptyList(),
            NO_AUTO_DELETE_TIMER
        )
    private val event = PrivateMessageReceivedEvent(header, contact.id)
    private val outputEvent = OutputEvent(EVENT_CONVERSATION_MESSAGE, event.output(text))

    @Test
    fun testSendEvent() {
        val slot = CapturingSlot<String>()

        every { session1.send(capture(slot)) } returns FutureWriteCallback()

        controller.sessions.add(session1)
        controller.sendEvent(EVENT_CONVERSATION_MESSAGE, event.output(text))

        assertJsonEquals(slot.captured, outputEvent)
    }

    @Test
    fun testSendEventIOException() {
        testSendEventException(IOException())
    }

    @Test
    fun testSendEventWebSocketException() {
        testSendEventException(WebSocketException())
    }

    private fun testSendEventException(throwable: Throwable) {
        val slot = CapturingSlot<String>()

        every { session1.send(capture(slot)) } throws throwable
        every { session2.send(capture(slot)) } returns FutureWriteCallback()

        controller.sessions.add(session1)
        controller.sessions.add(session2)
        controller.sendEvent(EVENT_CONVERSATION_MESSAGE, event.output(text))

        verify { session2.send(slot.captured) }
    }

    @Test
    fun testIntroductionRequestEvent() {
        val introductionRequest = IntroductionRequest(
            message.id,
            group.id,
            timestamp,
            true,
            true,
            true,
            true,
            SessionId(getRandomId()),
            author,
            text,
            false,
            AuthorInfo(VERIFIED),
            NO_AUTO_DELETE_TIMER
        )
        val introductionRequestEvent =
            IntroductionRequestReceivedEvent(introductionRequest, contact.id)
        val introductionOutputEvent =
            OutputEvent(EVENT_CONVERSATION_MESSAGE, introductionRequestEvent.output())
        val slot = CapturingSlot<String>()

        every { session1.send(capture(slot)) } returns FutureWriteCallback()

        controller.sessions.add(session1)
        controller.sendEvent(EVENT_CONVERSATION_MESSAGE, introductionRequestEvent.output())
        assertJsonEquals(slot.captured, introductionOutputEvent)
        assertEquals("IntroductionRequest", introductionRequestEvent.output()["type"])
    }

    @Test
    fun testOutputConversationMessageReceivedEvent() {
        val json = """
        {
            "type": "event",
            "name": "ConversationMessageReceivedEvent",
            "data": ${toJson(header.output(contact.id, text))}
        }
        """
        assertJsonEquals(json, outputEvent)
    }

}
