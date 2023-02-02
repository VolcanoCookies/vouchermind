package commands

import commands.Integration.Companion.createIntegrationInformationEmbed
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.event.interaction.GuildButtonInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.user
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import model.Command
import model.data.Approved
import model.data.Completed
import model.data.Requested
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import repository.IntegrationRepository
import repository.UserRepository

class WhoIs : KoinComponent, Command("whois", "Get some basic information on a user.") {

	private val userRepository: UserRepository by inject()
	private val integrationRepository: IntegrationRepository by inject()
	private val kord: Kord by inject()

	override suspend fun ChatInputCreateBuilder.build() {
		user("user", "The user to get some basic information on.") {
			required = true
		}
	}

	override suspend fun GuildChatInputCommandInteractionCreateEvent.on() {
		val user = interaction.command.users["user"]!!
		val entry = userRepository.getUserEntry(user)

		val integration = integrationRepository.getLatest(interaction.guild, user)
		val madeIntegrations = integrationRepository.getMadeIntegrations(interaction.guild, user)

		interaction.respondPublic {
			embed {
				title = "${user.username}#${user.discriminator}"

				field {
					name = "Integration Status"
					value = when {
						integration == null -> "Not Integrated"
						integration.terminated != null -> "Terminated"
						integration is Requested -> "Requested"
						integration is Approved -> "Approved"
						integration is Completed -> "Completed"
						else -> "Unknown"
					}
				}

				if (madeIntegrations.isNotEmpty()) {
					field {
						name = "Integrations Made"
						value = madeIntegrations.size.toString()
					}
				}

				field {
					name = "Blacklisted"
					value = entry.blacklisted.toString()
				}

				user.avatar?.url?.let {
					thumbnail {
						url = it
					}
				}

				color = user.accentColor

				footer {
					text = user.id.toString()
				}
			}

			actionRow {
				interactionButton(ButtonStyle.Primary, "integrationinfo-${user.id}") {
					label = "Get latest integration"
					disabled = integration == null
				}
			}

		}
	}

	init {
		kord.on<GuildButtonInteractionCreateEvent> {
			if (!interaction.componentId.matches(Regex("integrationinfo-\\d+"))) return@on

			val userId = interaction.componentId.split("-")[1]
			val user = interaction.kord.getUser(Snowflake(userId)) ?: let {
				interaction.respondEphemeral {
					content = "Could not find user with id `$userId`"
				}
				return@on
			}
			val integration = integrationRepository.getLatest(interaction.guild, user) ?: let {
				interaction.respondEphemeral {
					content = "No integration found for user."
				}
				return@on
			}

			interaction.respondPublic {
				createIntegrationInformationEmbed(integration)
			}
		}
	}

}