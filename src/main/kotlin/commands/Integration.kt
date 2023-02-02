package commands

import dev.kord.common.DiscordTimestampStyle
import dev.kord.common.toMessageFormat
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.create.embed
import model.Command
import model.CommandException
import model.data.*
import model.data.Integration
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.litote.kmongo.toId
import repository.IntegrationRepository

class Integration : KoinComponent, Command("integration", "Get information about a specific integration.") {

	private val integrationRepository: IntegrationRepository by inject()

	override suspend fun ChatInputCreateBuilder.build() {
		string("integrationid", "The id of the integration to get information about.") {
			required = true
		}
	}

	override suspend fun GuildChatInputCommandInteractionCreateEvent.on() {
		val integrationId = interaction.command.strings["integrationid"]!!
		val integration = integrationRepository.getById(integrationId.toId())
			?: throw CommandException("Integration with id `$integrationId` not found.")

		interaction.respondPublic {
			createIntegrationInformationEmbed(integration)
		}
	}

	companion object {

		fun MessageCreateBuilder.createIntegrationInformationEmbed(integration: Integration) {
			return embed {
				title = "Integration #${integration.id}"

				field {
					name = "Integration Status"
					value = when (integration) {
						is Requested -> "Requested"
						is Approved -> "Approved"
						is Completed -> "Complete"
					}
				}

				field {
					name = "Integrated By"
					value = "<@${integration.integratedBy}>"
				}
				field {
					name = "Integrated At"
					value = integration.integratedAt.toMessageFormat(DiscordTimestampStyle.LongDateTime)
				}

				when {
					integration is Approved -> {
						field {
							name = "Approved By"
							value = "<@${integration.approvedBy}>"
						}
						field {
							name = "Approved At"
							value = integration.approvedAt.toMessageFormat(DiscordTimestampStyle.LongDateTime)
						}
					}

					integration is Completed -> {
						field {
							name = "Approved By"
							value = "<@${integration.approvedBy}>"
						}
						field {
							name = "Approved At"
							value = integration.approvedAt.toMessageFormat(DiscordTimestampStyle.LongDateTime)
						}
						field {
							name = "Joined At"
							value = integration.joinedAt.toMessageFormat(DiscordTimestampStyle.LongDateTime)
						}
					}
				}

				field {
					name = "Integration Message"
					value = integration.message.link
				}


				integration.note?.let {
					field {
						name = "Integration Note"
						value = it
					}
				}

				when (integration.terminated) {
					is Terminated.Banned -> {
						field {
							name = "Terminated: Banned"
							value = integration.terminated!!.reason
						}
					}

					is Terminated.Cancelled -> {
						field {
							name = "Terminated: Cancelled"
							value = integration.terminated!!.reason
						}
					}

					is Terminated.Denied -> {
						field {
							name = "Terminated: Denied"
							value = integration.terminated!!.reason
						}
					}

					is Terminated.Kicked -> {
						field {
							name = "Terminated: Kicked"
							value = integration.terminated!!.reason
						}
					}

					is Terminated.Left -> {
						field {
							name = "Terminated: Left"
							value = integration.terminated!!.reason
						}
					}

					is Terminated.Disintegrated -> {
						field {
							name = "Terminated: Disintegrated"
							value = integration.terminated!!.reason
						}
					}

					null -> {}

				}

				integration.terminated?.let {
					field {
						name = "Terminated At"
						value = integration.terminated!!.at.toMessageFormat(DiscordTimestampStyle.LongDateTime)
					}
				}

			}
		}
	}

}