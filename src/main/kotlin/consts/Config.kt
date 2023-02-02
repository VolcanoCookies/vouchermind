package consts

import dev.kord.common.entity.Snowflake
import getEnv
import getEnvNullable

object Config {

	val isDev = getEnv("IS_DEV", "false") == "true"
	val devGuild = getEnvNullable("DEV_GUILD")?.let { Snowflake(it) }
	val discordToken = getEnv("DISCORD_TOKEN")
	val mongoUrl = getEnv("MONGO_URL")
	val databaseName = getEnv("DATABASE_NAME")

	init {
		if (isDev) devGuild ?: error("No dev guild in dev mode")
	}

}