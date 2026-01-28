package org.anonchatsecure.anonchat.headless.blogs

import org.briarproject.bramble.identity.output
import org.anonchatsecure.anonchat.api.blog.BlogPostHeader
import org.anonchatsecure.anonchat.api.blog.MessageType
import org.anonchatsecure.anonchat.headless.json.JsonDict
import java.util.Locale

internal fun BlogPostHeader.output(text: String) = JsonDict(
    "text" to text,
    "author" to author.output(),
    "authorStatus" to authorInfo.status.output(),
    "type" to type.output(),
    "id" to id.bytes,
    "parentId" to parentId?.bytes,
    "read" to isRead,
    "rssFeed" to isRssFeed,
    "timestamp" to timestamp,
    "timestampReceived" to timeReceived
)

internal fun MessageType.output() = name.lowercase(Locale.US)
