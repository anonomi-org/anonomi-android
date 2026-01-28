package org.briarproject.anonchat.headless.messaging

import org.anonchatsecure.bramble.api.contact.ContactId
import org.briarproject.bramble.identity.output
import org.anonchatsecure.anonchat.api.blog.BlogInvitationResponse
import org.anonchatsecure.anonchat.api.conversation.ConversationMessageHeader
import org.anonchatsecure.anonchat.api.conversation.ConversationResponse
import org.anonchatsecure.anonchat.api.forum.ForumInvitationResponse
import org.anonchatsecure.anonchat.api.introduction.IntroductionResponse
import org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationResponse
import org.anonchatsecure.anonchat.api.sharing.InvitationResponse
import org.anonchatsecure.anonchat.headless.json.JsonDict

internal fun ConversationResponse.output(contactId: ContactId): JsonDict {
    val dict = (this as ConversationMessageHeader).output(contactId)
    dict.putAll(
        "sessionId" to sessionId.bytes,
        "accepted" to wasAccepted()
    )
    return dict
}

internal fun IntroductionResponse.output(contactId: ContactId): JsonDict {
    val dict = (this as ConversationResponse).output(contactId)
    dict.putAll(
        "type" to "IntroductionResponse",
        "introducedAuthor" to introducedAuthor.output(),
        "introducer" to isIntroducer
    )
    return dict
}

internal fun InvitationResponse.output(contactId: ContactId): JsonDict {
    val dict = (this as ConversationResponse).output(contactId)
    dict["shareableId"] = shareableId.bytes
    return dict
}

internal fun BlogInvitationResponse.output(contactId: ContactId): JsonDict {
    val dict = (this as InvitationResponse).output(contactId)
    dict["type"] = "BlogInvitationResponse"
    return dict
}

internal fun ForumInvitationResponse.output(contactId: ContactId): JsonDict {
    val dict = (this as InvitationResponse).output(contactId)
    dict["type"] = "ForumInvitationResponse"
    return dict
}

internal fun GroupInvitationResponse.output(contactId: ContactId): JsonDict {
    val dict = (this as InvitationResponse).output(contactId)
    dict["type"] = "GroupInvitationResponse"
    return dict
}
