package commands

import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.SubCommandBuilder
import dev.kord.rest.builder.interaction.user
import dev.kord.rest.builder.message.create.embed
import log
import model.Command
import model.SubCommand
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import repository.GuildRepository
import repository.UserRepository
import timestampNow

class Blacklist : KoinComponent, Command("blacklist", "Being on the blacklist prevents users from being integrated.") {

	override suspend fun ChatInputCreateBuilder.build() {
		subCommand(BlacklistAdd())
		subCommand(BlacklistRemove())
		subCommand(BlacklistShow())
	}

	override suspend fun GuildChatInputCommandInteractionCreateEvent.on() {

	}

}

class BlacklistAdd : KoinComponent, SubCommand("add", "Add a user to the blacklist.") {

	private val userRepository: UserRepository by inject()
	private val guildRepository: GuildRepository by inject()

	override suspend fun SubCommandBuilder.build() {
		user("user", "The user to add to the blacklist.") {
			required = true
		}
	}

	override suspend fun GuildChatInputCommandInteractionCreateEvent.on() {
		val config = guildRepository.getGuildConfig(interaction.guild)
		val user = interaction.command.users["user"]!!
		val entry = userRepository.getUserEntry(user)

		when {
			config.approvalRole == null -> {
				interaction.respondEphemeral {
					content = "This guild does not have a approval role set, contact an administrator."
				}
				return
			}

			!interaction.user.roleIds.contains(config.approvalRole) -> {
				interaction.respondEphemeral {
					content = "The role <@&${config.approvalRole}> is required to edit the blacklist."
				}
				return
			}

			entry.blacklisted -> {
				interaction.respondEphemeral {
					content = "User is already blacklisted."
				}
				return
			}
		}

		entry.blacklisted = true
		userRepository.save(entry)

		interaction.respondPublic {
			content = "Blacklisted ${user.mention} from being integrated."
		}

		interaction.guild.log(config) {
			embed {
				description = "${interaction.user.mention} blacklisted ${user.mention}."
				timestampNow()
			}
		}
	}

}

class BlacklistRemove : KoinComponent, SubCommand("remove", "Remove a user from the blacklist.") {

	private val userRepository: UserRepository by inject()
	private val guildRepository: GuildRepository by inject()

	override suspend fun SubCommandBuilder.build() {
		user("user", "The user to remove from the blacklist.") {
			required = true
		}
	}

	override suspend fun GuildChatInputCommandInteractionCreateEvent.on() {
		val config = guildRepository.getGuildConfig(interaction.guild)
		val user = interaction.command.users["user"]!!
		val entry = userRepository.getUserEntry(user)

		when {
			config.approvalRole == null -> {
				interaction.respondEphemeral {
					content = "This guild does not have a approval role set, contact an administrator."
				}
				return
			}

			!interaction.user.roleIds.contains(config.approvalRole) -> {
				interaction.respondEphemeral {
					content = "The role <@&${config.approvalRole}> is required to edit the blacklist."
				}
				return
			}

			!entry.blacklisted -> {
				interaction.respondEphemeral {
					content = "User is not blacklisted."
				}
				return
			}
		}

		entry.blacklisted = false
		userRepository.save(entry)

		interaction.respondPublic {
			content = "Unblacklisted ${user.mention} from being integrated."
		}

		interaction.guild.log(config) {
			embed {
				description = "${interaction.user.mention} unblacklisted ${user.mention}."
				timestampNow()
			}
		}
	}

}

class BlacklistShow : KoinComponent, SubCommand("show", "Show all blacklisted users.") {

	private val userRepository: UserRepository by inject()
	private val guildRepository: GuildRepository by inject()

	override suspend fun SubCommandBuilder.build() {

	}

	override suspend fun GuildChatInputCommandInteractionCreateEvent.on() {
		val config = guildRepository.getGuildConfig(interaction.guild)
		val users = userRepository.getAllBlacklisted()

		when {
			config.approvalRole == null -> {
				interaction.respondEphemeral {
					content = "This guild does not have a approval role set, contact an administrator."
				}
				return
			}

			!interaction.user.roleIds.contains(config.approvalRole) -> {
				interaction.respondEphemeral {
					content = "The role <@&${config.approvalRole}> is required to view the blacklist."
				}
				return
			}
		}


		interaction.respondPublic {
			content = users.joinToString("\n") {
				"<@${it.snowflake}>"
			}.ifEmpty { "No blacklisted users" }
		}

	}

}