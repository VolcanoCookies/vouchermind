import dev.kord.common.entity.AuditLogEvent
import dev.kord.core.Kord
import dev.kord.core.behavior.*
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.GuildInteractionBehavior
import dev.kord.core.entity.AuditLogEntry
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.guild.BanAddEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.UserMessageCreateBuilder
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import model.data.GuildConfig
import model.data.MessageLocation
import kotlin.time.Duration

fun getEnv(varName: String, default: String? = null) =
	System.getenv(varName) ?: default ?: error("Env $varName not defined.")

fun getEnvNullable(varName: String): String? = System.getenv(varName)

fun Duration.toPrettyIsoString(): String = this.toIsoString().replace("PT", "")

suspend fun GuildInteractionBehavior.log(config: GuildConfig, builder: UserMessageCreateBuilder.() -> Unit): Message? {
	return config.logChannel?.let {
		guild.getChannelOf<TextChannel>(it).createMessage(builder)
	}
}

suspend fun GuildBehavior.log(config: GuildConfig, builder: UserMessageCreateBuilder.() -> Unit): Message? {
	return config.logChannel?.let {
		getChannelOf<TextChannel>(it).createMessage(builder)
	}
}

fun EmbedBuilder.timestampNow() {
	this.timestamp = Clock.System.now()
}

suspend fun Kord.getMessage(message: MessageLocation): MessageBehavior? {
	return getGuildOrNull(message.guild)?.getChannelOfOrNull<TextChannel>(message.channel)?.getMessageOrNull(message.id)
}

suspend fun Message.location(): MessageLocation = MessageLocation(id, channelId, getGuild().id)

suspend fun BanAddEvent.getBanLog(): AuditLogEntry? {
	val possible = kord.getGuildOrNull(guildId)?.getAuditLogEntries {
		action = AuditLogEvent.MemberBanAdd
		this.limit = 25
	}?.toList() ?: return null
	return possible.first { it.targetId == user.id }
}

suspend fun MemberLeaveEvent.getKickLog(): AuditLogEntry? {
	val possible = kord.getGuildOrNull(guildId)?.getAuditLogEntries {
		action = AuditLogEvent.MemberKick
		this.limit = 25
	}?.toList() ?: return null
	return possible.first { it.targetId == user.id }
}