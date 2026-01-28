package org.anonchatsecure.anonchat.headless

import org.anonchatsecure.bramble.api.crypto.KeyStrengthener
import org.anonchatsecure.bramble.api.db.DatabaseConfig
import java.io.File

internal class HeadlessDatabaseConfig(private val dbDir: File, private val keyDir: File) :
    DatabaseConfig {

    override fun getDatabaseDirectory() = dbDir

    override fun getDatabaseKeyDirectory() = keyDir

    override fun getKeyStrengthener(): KeyStrengthener? = null
}
