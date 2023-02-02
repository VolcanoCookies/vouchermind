package model.data

import dev.kord.common.entity.Snowflake
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.litote.kmongo.Id

@Serializable
sealed class Integration {

	@SerialName("_id")
	@Contextual
	abstract val id: Id<Integration>
	abstract val guild: Snowflake
	abstract val snowflake: Snowflake
	abstract val integratedBy: Snowflake
	abstract val integratedAt: Instant
	abstract val note: String?
	abstract val message: MessageLocation
	abstract var terminated: Terminated?

	val isTerminated: Boolean
		get() = terminated != null

}

@Serializable
data class Requested(
	@SerialName("_id")
	@Contextual
	override val id: Id<Integration>,
	override val guild: Snowflake,
	override val snowflake: Snowflake,
	override val integratedBy: Snowflake,
	override val integratedAt: Instant,
	override val note: String?,
	override val message: MessageLocation,
	override var terminated: Terminated?,
) : Integration() {

	fun approve(approvedBy: Snowflake, approvedAt: Instant): Approved {
		return Approved(
			id, guild, snowflake, integratedBy, integratedAt, note, message, approvedBy, approvedAt
		)
	}
}

@Serializable
data class Approved(
	@SerialName("_id")
	@Contextual
	override val id: Id<Integration>,
	override val guild: Snowflake,
	override val snowflake: Snowflake,
	override val integratedBy: Snowflake,
	override val integratedAt: Instant,
	override val note: String?,
	override val message: MessageLocation,
	val approvedBy: Snowflake,
	val approvedAt: Instant,
	override var terminated: Terminated? = null
) : Integration() {

	fun complete(joinedAt: Instant): Completed {
		return Completed(
			id,
			guild,
			snowflake,
			integratedBy,
			integratedAt,
			note,
			message,
			approvedBy,
			approvedAt,
			joinedAt,
			terminated
		)
	}
}

@Serializable
data class Completed(
	@SerialName("_id")
	@Contextual
	override val id: Id<Integration>,
	override val guild: Snowflake,
	override val snowflake: Snowflake,
	override val integratedBy: Snowflake,
	override val integratedAt: Instant,
	override val note: String?,
	override val message: MessageLocation,
	val approvedBy: Snowflake,
	val approvedAt: Instant,
	val joinedAt: Instant,
	override var terminated: Terminated? = null
) : Integration()

@Serializable
sealed class Terminated {

	abstract val at: Instant
	abstract val reason: String

	@Serializable
	data class Left(override val at: Instant) : Terminated() {

		override val reason: String
			get() = "Left guild"
	}

	@Serializable
	data class Kicked(
		override val at: Instant,
		override val reason: String,
		val by: Snowflake
	) : Terminated()

	@Serializable
	data class Banned(
		override val at: Instant,
		override val reason: String,
		val by: Snowflake
	) : Terminated()

	@Serializable
	data class Cancelled(
		override val at: Instant,
		override val reason: String
	) : Terminated()

	@Serializable
	data class Denied(
		override val at: Instant,
		val by: Snowflake,
	) : Terminated() {

		override val reason: String
			get() = "Denied by <@$by>"
	}

	@Serializable
	data class Disintegrated(
		override val at: Instant,
		override val reason: String,
		val by: Snowflake,
	) : Terminated()

}

@Serializable
data class MessageLocation(
	val id: Snowflake,
	val channel: Snowflake,
	val guild: Snowflake
) {

	val link: String
		get() = "https://discord.com/channels/$guild/$channel/$id"
}