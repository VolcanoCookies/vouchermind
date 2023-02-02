package commands

import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.user
import model.Command
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import service.IntegrationService

class Cancel : KoinComponent,
	Command("cancel", "Cancel a users pending integration.") {

	private val integrationService: IntegrationService by inject()

	override suspend fun ChatInputCreateBuilder.build() {
		user("user", "User to cancel integration for.") {
			required = true
		}
		string("reason", "Reason why you are cancelling this users integration.") {
			required = true
			minLength = 3
		}
	}

	override suspend fun GuildChatInputCommandInteractionCreateEvent.on() {
		val target = interaction.command.users["user"]!!
		val reason = interaction.command.strings["reason"]!!

		integrationService.cancelIntegrationManual(interaction, reason, target)

		interaction.respondPublic {
			content = "Cancelled the integration of ${target.mention}."
		}
	}

}