package org.briarproject.anonchat.headless.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import org.bouncycastle.util.encoders.Base64
import org.bouncycastle.util.encoders.DecoderException
import org.anonchatsecure.bramble.api.contact.Contact
import org.anonchatsecure.bramble.api.contact.ContactId
import org.anonchatsecure.bramble.api.contact.ContactManager
import org.anonchatsecure.bramble.api.db.DatabaseExecutor
import org.anonchatsecure.bramble.api.db.NoSuchContactException
import org.anonchatsecure.bramble.api.event.Event
import org.anonchatsecure.bramble.api.event.EventListener
import org.anonchatsecure.bramble.api.sync.MessageId
import org.anonchatsecure.bramble.api.sync.event.MessagesAckedEvent
import org.anonchatsecure.bramble.api.sync.event.MessagesSentEvent
import org.anonchatsecure.bramble.api.system.Clock
import org.anonchatsecure.bramble.util.StringUtils.utf8IsTooLong
import org.anonchatsecure.anonchat.api.blog.BlogInvitationRequest
import org.anonchatsecure.anonchat.api.blog.BlogInvitationResponse
import org.anonchatsecure.anonchat.api.conversation.ConversationManager
import org.anonchatsecure.anonchat.api.conversation.ConversationMessageVisitor
import org.anonchatsecure.anonchat.api.conversation.event.ConversationMessageReceivedEvent
import org.anonchatsecure.anonchat.api.forum.ForumInvitationRequest
import org.anonchatsecure.anonchat.api.forum.ForumInvitationResponse
import org.anonchatsecure.anonchat.api.introduction.IntroductionRequest
import org.anonchatsecure.anonchat.api.introduction.IntroductionResponse
import org.anonchatsecure.anonchat.api.messaging.MessagingConstants.MAX_PRIVATE_MESSAGE_TEXT_LENGTH
import org.anonchatsecure.anonchat.api.messaging.MessagingManager
import org.anonchatsecure.anonchat.api.messaging.PrivateMessageFactory
import org.anonchatsecure.anonchat.api.messaging.PrivateMessageHeader
import org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationRequest
import org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationResponse
import org.anonchatsecure.anonchat.headless.event.WebSocketController
import org.anonchatsecure.anonchat.headless.event.output
import org.anonchatsecure.anonchat.headless.getContactIdFromPathParam
import org.anonchatsecure.anonchat.headless.getFromJson
import org.anonchatsecure.anonchat.headless.json.JsonDict
import java.util.concurrent.Executor
import javax.annotation.concurrent.Immutable
import javax.inject.Inject
import javax.inject.Singleton

internal const val EVENT_CONVERSATION_MESSAGE = "ConversationMessageReceivedEvent"
internal const val EVENT_MESSAGES_ACKED = "MessagesAckedEvent"
internal const val EVENT_MESSAGES_SENT = "MessagesSentEvent"

@Immutable
@Singleton
internal class MessagingControllerImpl
@Inject
constructor(
    private val messagingManager: MessagingManager,
    private val conversationManager: ConversationManager,
    private val privateMessageFactory: PrivateMessageFactory,
    private val contactManager: ContactManager,
    private val webSocketController: WebSocketController,
    @DatabaseExecutor private val dbExecutor: Executor,
    private val objectMapper: ObjectMapper,
    private val clock: Clock
) : MessagingController, EventListener {

    override fun list(ctx: Context): Context {
        val contact = getContact(ctx)
        val jsonVisitor = JsonVisitor(contact.id, messagingManager)
        val messages = conversationManager.getMessageHeaders(contact.id)
            .sortedBy { it.timestamp }
            .map { header -> header.accept(jsonVisitor) }
        return ctx.json(messages)
    }

    override fun write(ctx: Context): Context {
        val contact = getContact(ctx)

        val text = ctx.getFromJson(objectMapper, "text")
        if (utf8IsTooLong(text, MAX_PRIVATE_MESSAGE_TEXT_LENGTH))
            throw BadRequestResponse("Message text is too long")

        val group = messagingManager.getContactGroup(contact)
        val now = clock.currentTimeMillis()
        val m = privateMessageFactory.createLegacyPrivateMessage(group.id, now, text)

        messagingManager.addLocalMessage(m)
        return ctx.json(m.output(contact.id, text))
    }

    override fun markMessageRead(ctx: Context): Context {
        val contact = getContact(ctx)
        val groupId = messagingManager.getContactGroup(contact).id

        val messageIdString = ctx.getFromJson(objectMapper, "messageId")
        val messageId = deserializeMessageId(messageIdString)
        conversationManager.setReadFlag(groupId, messageId, true)
        return ctx.json(messageIdString)
    }

    private fun deserializeMessageId(idString: String): MessageId {
        val idBytes = try {
            Base64.decode(idString)
        } catch (e: DecoderException) {
            throw NotFoundResponse()
        }
        if (idBytes.size != MessageId.LENGTH) throw NotFoundResponse()
        return MessageId(idBytes)
    }

    override fun deleteAllMessages(ctx: Context): Context {
        val contactId = ctx.getContactIdFromPathParam()
        try {
            val result = conversationManager.deleteAllMessages(contactId)
            return ctx.json(result.output())
        } catch (e: NoSuchContactException) {
            throw NotFoundResponse()
        }
    }

    override fun eventOccurred(e: Event) {
        when (e) {
            is ConversationMessageReceivedEvent<*> -> {
                val h = e.messageHeader
                if (h is PrivateMessageHeader) dbExecutor.execute {
                    val text = messagingManager.getMessageText(h.id)
                    webSocketController.sendEvent(EVENT_CONVERSATION_MESSAGE, e.output(text))
                } else {
                    webSocketController.sendEvent(EVENT_CONVERSATION_MESSAGE, e.output())
                }
            }
            is MessagesSentEvent -> {
                webSocketController.sendEvent(EVENT_MESSAGES_SENT, e.output())
            }
            is MessagesAckedEvent -> {
                webSocketController.sendEvent(EVENT_MESSAGES_ACKED, e.output())
            }
        }
    }

    private fun getContact(ctx: Context): Contact {
        val contactId = ctx.getContactIdFromPathParam()
        return try {
            contactManager.getContact(contactId)
        } catch (e: NoSuchContactException) {
            throw NotFoundResponse()
        }
    }
}

private class JsonVisitor(
    private val contactId: ContactId,
    private val messagingManager: MessagingManager
) : ConversationMessageVisitor<JsonDict> {

    override fun visitPrivateMessageHeader(h: PrivateMessageHeader) =
        h.output(contactId, messagingManager.getMessageText(h.id))

    override fun visitBlogInvitationRequest(r: BlogInvitationRequest) = r.output(contactId)

    override fun visitBlogInvitationResponse(r: BlogInvitationResponse) = r.output(contactId)

    override fun visitForumInvitationRequest(r: ForumInvitationRequest) = r.output(contactId)

    override fun visitForumInvitationResponse(r: ForumInvitationResponse) = r.output(contactId)

    override fun visitGroupInvitationRequest(r: GroupInvitationRequest) = r.output(contactId)

    override fun visitGroupInvitationResponse(r: GroupInvitationResponse) = r.output(contactId)

    override fun visitIntroductionRequest(r: IntroductionRequest) = r.output(contactId)

    override fun visitIntroductionResponse(r: IntroductionResponse) = r.output(contactId)
}
