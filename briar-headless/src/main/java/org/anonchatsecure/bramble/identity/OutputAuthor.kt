package org.anonchatsecure.bramble.identity

import org.anonchatsecure.bramble.api.identity.Author
import org.anonchatsecure.anonchat.api.identity.AuthorInfo
import org.anonchatsecure.anonchat.headless.json.JsonDict
import java.util.Locale

fun Author.output() = JsonDict(
    "formatVersion" to formatVersion,
    "id" to id.bytes,
    "name" to name,
    "publicKey" to publicKey.encoded
)

fun AuthorInfo.Status.output() = name.lowercase(Locale.US)
