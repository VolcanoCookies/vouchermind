package commands

import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.boolean
import model.Command
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import repository.GuildRepository

class Lockdown : KoinComponent, Command("lockdown", "Lockdown the server, kicking anyone joining.") {

	private val guildRepository: GuildRepository by inject()

	override suspend fun ChatInputCreateBuilder.build() {
		boolean("lockdown", "If the server should be in lockdown or not.") {
			required = true
		}
	}

	override suspend fun GuildChatInputCommandInteractionCreateEvent.on() {
		val lockdown = interaction.command.booleans["lockdown"]!!
		val config = guildRepository.getGuildConfig(interaction.guild)

		when {
			lockdown && config.lockdown -> {
				interaction.respondEphemeral {
					content = "The server is already in lockdown."
				}
			}

			!lockdown && !config.lockdown -> {
				interaction.respondEphemeral {
					content = "The server is not currently in lockdown."
				}
			}

			else -> {
				config.lockdown = lockdown
				guildRepository.save(config)

				if (lockdown) {
					interaction.respondPublic {
						content = "The guild is now in lockdown, any new users who join will be kicked."
					}
				} else {
					interaction.respondPublic {
						content = "The guild is no longer in lockdown."
					}
				}
			}
		}

	}

}