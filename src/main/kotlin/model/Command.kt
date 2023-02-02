package model

import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.AutoCompleteInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.RootInputChatBuilder
import dev.kord.rest.builder.interaction.subCommand

abstract class Command(
	val name: String,
	val description: String
) {

	val subcommands: MutableMap<String, SubCommand> = hashMapOf()

	abstract suspend fun ChatInputCreateBuilder.build()

	abstract suspend fun GuildChatInputCommandInteractionCreateEvent.on()

	open suspend fun AutoCompleteInteractionCreateEvent.autocomplete() {}

	suspend fun invoke(event: GuildChatInputCommandInteractionCreateEvent) {
		if (event.interaction.command is dev.kord.core.entity.interaction.SubCommand) {
			val subcommand =
				subcommands[(event.interaction.command as dev.kord.core.entity.interaction.SubCommand).name] ?: return
			event.apply {
				subcommand.apply {
					try {
						on()
					} catch (e: CommandException) {
						interaction.respondEphemeral {
							content = e.message!!
						}
					}
				}
			}
		} else {
			try {
				event.on()
			} catch (e: CommandException) {
				event.interaction.respondEphemeral {
					content = e.message!!
				}
			}
		}
	}

	suspend inline fun RootInputChatBuilder.subCommand(
		subCommand: SubCommand
	) {
		subCommand(subCommand.name, subCommand.description) {
			subCommand.apply {
				build()
			}
		}

		subcommands[subCommand.name] = subCommand
	}

}