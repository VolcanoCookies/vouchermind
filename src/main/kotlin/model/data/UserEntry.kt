package model.data

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserEntry(
	@SerialName("_id")
	val snowflake: Snowflake,
	var blacklisted: Boolean = false,
)
