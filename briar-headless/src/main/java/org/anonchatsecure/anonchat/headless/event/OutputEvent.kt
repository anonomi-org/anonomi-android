package org.anonchatsecure.anonchat.headless.event

import org.anonchatsecure.anonchat.api.blog.BlogInvitationRequest
import org.anonchatsecure.anonchat.api.blog.BlogInvitationResponse
import org.anonchatsecure.anonchat.api.conversation.event.ConversationMessageReceivedEvent
import org.anonchatsecure.anonchat.api.forum.ForumInvitationRequest
import org.anonchatsecure.anonchat.api.forum.ForumInvitationResponse
import org.anonchatsecure.anonchat.api.introduction.IntroductionRequest
import org.anonchatsecure.anonchat.api.introduction.IntroductionResponse
import org.anonchatsecure.anonchat.api.messaging.PrivateMessageHeader
import org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationRequest
import org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationResponse
import org.anonchatsecure.anonchat.headless.json.JsonDict
import org.anonchatsecure.anonchat.headless.messaging.output
import javax.annotation.concurrent.Immutable

@Immutable
@Suppress("unused")
internal class OutputEvent(val name: String, val data: JsonDict) {
    val type = "event"
}

internal fun ConversationMessageReceivedEvent<*>.output(text: String?): JsonDict {
    check(messageHeader is PrivateMessageHeader)
    return (messageHeader as PrivateMessageHeader).output(contactId, text)
}

internal fun ConversationMessageReceivedEvent<*>.output() = when (messageHeader) {
    // requests
    is ForumInvitationRequest -> (messageHeader as ForumInvitationRequest).output(contactId)
    is BlogInvitationRequest -> (messageHeader as BlogInvitationRequest).output(contactId)
    is GroupInvitationRequest -> (messageHeader as GroupInvitationRequest).output(contactId)
    is IntroductionRequest -> (messageHeader as IntroductionRequest).output(contactId)
    // responses
    is ForumInvitationResponse -> (messageHeader as ForumInvitationResponse).output(contactId)
    is BlogInvitationResponse -> (messageHeader as BlogInvitationResponse).output(contactId)
    is GroupInvitationResponse -> (messageHeader as GroupInvitationResponse).output(contactId)
    is IntroductionResponse -> (messageHeader as IntroductionResponse).output(contactId)
    // unknown
    else -> throw IllegalStateException()
}
