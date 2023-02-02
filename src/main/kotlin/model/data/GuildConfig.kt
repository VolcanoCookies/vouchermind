package model.data

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Serializable
data class GuildConfig(
	@SerialName("_id")
	val snowflake: Snowflake,
	// Log channel
	var logChannel: Snowflake? = null,
	// List of roles that are allowed to integrate users
	val integratorRoles: MutableMap<Snowflake, IntegratorRole> = mutableMapOf(),
	// Role that can approve integrations
	var approvalRole: Snowflake? = null,
	// Time before a user can integrate other users
	var minMemberTime: Duration = 0.seconds,
	// Channel to use for integration requests
	var integrationRequestChannel: Snowflake? = null,
	// Automatically kick anyone joining the server
	var lockdown: Boolean = false
)

@Serializable
data class IntegratorRole(
	val snowflake: Snowflake,
	val cooldown: Duration,
	// Max amount of users to invite between cooldowns
	val limit: Int = 1
) {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is IntegratorRole) return false

		if (snowflake != other.snowflake) return false

		return true
	}

	override fun hashCode(): Int {
		return snowflake.hashCode()
	}
}