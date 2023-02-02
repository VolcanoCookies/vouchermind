package service

import commands.*
import consts.Config
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.cache.data.ApplicationCommandData
import dev.kord.core.event.interaction.AutoCompleteInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.kordLogger
import dev.kord.core.on
import dev.kord.rest.builder.interaction.GuildMultiApplicationCommandBuilder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import model.Command
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.collections.set

class CommandService : KoinComponent {

	private val kord: Kord by inject()
	private val commands: MutableMap<String, Command> = hashMapOf()

	private lateinit var existingCommands: Map<String, ApplicationCommandData>

	private suspend fun registerCommand(command: Command) = coroutineScope {
		kordLogger.info("Registered command ${command.name}")
		commands[command.name] = command

		async {
			if (Config.isDev) {

				kord.createGuildChatInputCommand(Config.devGuild!!, command.name, command.description) {
					command.apply {
						build()
					}
				}
			} else {
				kord.createGlobalChatInputCommand(command.name, command.description) {
					command.apply {
						build()
					}
				}
			}
		}
	}

	private suspend fun GuildMultiApplicationCommandBuilder.registerInput(command: Command) {
		kordLogger.info("Registered command ${command.name}")
		this@CommandService.commands[command.name] = command

		this.input(command.name, command.description) {
			command.apply {
				build()
			}
		}
	}

	init {
		kordLogger.info("Starting command service")

		kord.on<GuildChatInputCommandInteractionCreateEvent> {
			val name = interaction.invokedCommandName
			val command = commands[name] ?: let {
				interaction.respondEphemeral {
					content = "Command $name not yet implemented, contact bot owner."
				}
				return@on
			}
			command.invoke(this)
		}

		kord.on<AutoCompleteInteractionCreateEvent> {
			val name = interaction.command.rootName
			val command = commands[name] ?: return@on
			command.apply {
				autocomplete()
			}
		}

		GlobalScope.async {
			if (Config.isDev) {
				kord.createGuildApplicationCommands(Config.devGuild!!) {
					registerInput(Integrate())
					registerInput(ConfigBase())
					registerInput(WhoIs())
					registerInput(Lockdown())
					registerInput(Blacklist())
					registerInput(Integration())
					registerInput(Cancel())
					registerInput(Disintegrate())
				}
			} else {

			}
		}
	}

}