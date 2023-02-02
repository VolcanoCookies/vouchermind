package commands

import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.user
import dev.kord.rest.builder.message.EmbedBuilder
import model.Command
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import repository.GuildRepository
import repository.IntegrationRepository
import service.IntegrationService

class Disintegrate : KoinComponent,
	Command("disintegrate", "Disintegrate a integrated user, kicking them from the server if they are a member.") {

	private val integrationService: IntegrationService by inject()
	private val guildRepository: GuildRepository by inject()
	private val integrationRepository: IntegrationRepository by inject()

	override suspend fun ChatInputCreateBuilder.build() {
		user("user", "User to request integration for.") {
			required = true
		}
		string("reason", "Reason for disintegrating this user.") {
			required = true
			minLength = 5
			maxLength = EmbedBuilder.Field.Limits.value
		}
	}

	override suspend fun GuildChatInputCommandInteractionCreateEvent.on() {
		val target = interaction.command.users["user"]!!
		val reason = interaction.command.strings["reason"]!!

		integrationService.disintegrate(interaction, target, reason)

		interaction.respondPublic {
			content = "Disintegrated ${target.mention} with the reason $reason."
		}
	}

}