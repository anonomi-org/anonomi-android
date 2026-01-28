package org.anonchatsecure.anonchat.headless.forums

import org.anonchatsecure.anonchat.api.forum.Forum
import org.anonchatsecure.anonchat.headless.json.JsonDict

internal fun Forum.output() = JsonDict(
    "name" to name,
    "id" to id.bytes
)

internal fun Collection<Forum>.output() = map { it.output() }
