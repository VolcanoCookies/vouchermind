package commands

import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.user
import dev.kord.rest.builder.message.EmbedBuilder
import model.BeginIntegrationException
import model.Command
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import service.IntegrationService

class Integrate : KoinComponent,
	Command("integrate", "Request the integration of a user.") {

	private val integrationService: IntegrationService by inject()

	override suspend fun ChatInputCreateBuilder.build() {
		user("user", "User to request integration for.") {
			required = true
		}
		string("note", "Optional notes to provide, for example why this user should be integrated.") {
			required = false
			minLength = 5
			maxLength = EmbedBuilder.Field.Limits.value
		}
	}

	override suspend fun GuildChatInputCommandInteractionCreateEvent.on() {
		val target = interaction.command.users["user"]!!
		val note = interaction.command.strings["note"]


		try {
			integrationService.beginIntegration(interaction, target, note)

			interaction.respondPublic {
				content = "Requested the integration of ${target.mention}."
			}
		} catch (e: BeginIntegrationException) {
			interaction.respondEphemeral {
				content = e.message
			}
		}

	}

}