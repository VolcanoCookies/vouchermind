package commands

import dev.kord.common.entity.ChannelType
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.*
import dev.kord.rest.builder.message.create.embed
import model.Command
import model.SubCommand
import model.data.IntegratorRole
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import repository.GuildRepository
import toPrettyIsoString
import kotlin.time.Duration.Companion.seconds

class ConfigBase : KoinComponent, Command("config", "Change the server configs") {

	override suspend fun ChatInputCreateBuilder.build() {
		subCommand(ConfigShow())
		subCommand(ConfigLogChannel())
		subCommand(ConfigIntegrationRequestChannel())
		subCommand(ConfigMemberTime())
		subCommand(ConfigApprovalRole())
		subCommand(ConfigIntegratorRoleAdd())
		subCommand(ConfigIntegratorRoleRemove())
	}

	override suspend fun GuildChatInputCommandInteractionCreateEvent.on() {
	}
}

class ConfigLogChannel : KoinComponent, SubCommand("logs", "Set the log channel.") {

	private val guildRepository: GuildRepository by inject()

	override suspend fun SubCommandBuilder.build() {
		channel("logchannel", "The channel to use for logs.") {
			required = true
			channelTypes = listOf(ChannelType.GuildText)
		}
	}

	override suspend fun GuildChatInputCommandInteractionCreateEvent.on() {
		val channel = interaction.command.channels["logchannel"]!!

		val config = guildRepository.getGuildConfig(interaction.guild)
		config.logChannel = channel.id
		guildRepository.save(config)

		interaction.respondPublic {
			content = "Set the guild log channel to ${channel.mention}."
		}
	}

}

class ConfigIntegrationRequestChannel : KoinComponent,
	SubCommand("requests", "Set the integration request channel.") {

	private val guildRepository: GuildRepository by inject()

	override suspend fun SubCommandBuilder.build() {
		channel("requestchannel", "The channel to use for integration requests.") {
			required = true
			channelTypes = listOf(ChannelType.GuildText)
		}
	}

	override suspend fun GuildChatInputCommandInteractionCreateEvent.on() {
		val channel = interaction.command.channels["requestchannel"]!!

		val config = guildRepository.getGuildConfig(interaction.guild)
		config.integrationRequestChannel = channel.id
		guildRepository.save(config)

		interaction.respondPublic {
			content = "Set the guild integration request channel to ${channel.mention}."
		}
	}

}

class ConfigMemberTime : KoinComponent,
	SubCommand("membertime", "Set the minimum member time for integrating other users.") {

	private val guildRepository: GuildRepository by inject()

	override suspend fun SubCommandBuilder.build() {
		number("mintime", "Minimum time after joining in seconds for a member to integrate other users.") {
			required = true
			minValue = 0.0
		}
	}

	override suspend fun GuildChatInputCommandInteractionCreateEvent.on() {
		val seconds = interaction.command.numbers["mintime"]!!.toInt()
		val duration = seconds.seconds

		val config = guildRepository.getGuildConfig(interaction.guild)
		config.minMemberTime = duration
		guildRepository.save(config)

		interaction.respondPublic {
			content = "Set the minimum membership time to ${duration.toPrettyIsoString()}."
		}
	}

}

class ConfigApprovalRole : KoinComponent,
	SubCommand("approvalrole", "Set the required role for approving integrations.") {

	private val guildRepository: GuildRepository by inject()

	override suspend fun SubCommandBuilder.build() {
		role("approvalrole", "The role required for approving integrations.") {
			required = true
		}
	}

	override suspend fun GuildChatInputCommandInteractionCreateEvent.on() {
		val role = interaction.command.roles["approvalrole"]!!

		val config = guildRepository.getGuildConfig(interaction.guild)
		config.approvalRole = role.id
		guildRepository.save(config)

		interaction.respondPublic {
			content = "Set the approval role to ${role.mention}."
		}
	}

}

class ConfigIntegratorRoleAdd : KoinComponent,
	SubCommand("addrole", "Add a role that is allowed to integrate users.") {

	private val guildRepository: GuildRepository by inject()

	override suspend fun SubCommandBuilder.build() {
		role("role", "The role that lets users integrate other users.") {
			required = true
		}
		number("limit", "The amount of integrations a user can do at one time.") {
			required = true
			minValue = 1.0
		}
		number("cooldown", "The cooldown time in seconds between LIMIT amount of integrations") {
			required = true
			minValue = 0.0
		}
	}

	override suspend fun GuildChatInputCommandInteractionCreateEvent.on() {
		val role = interaction.command.roles["role"]!!
		val limit = interaction.command.numbers["limit"]!!.toInt()
		val cooldown = interaction.command.numbers["cooldown"]!!.toInt().seconds

		val config = guildRepository.getGuildConfig(interaction.guild)
		if (config.integratorRoles[role.id] != null) {
			config.integratorRoles[role.id] = IntegratorRole(
				role.id,
				cooldown,
				limit
			)
			guildRepository.save(config)

			interaction.respondPublic {
				content =
					"Set the limit to $limit and cooldown to ${cooldown.toPrettyIsoString()} for existing integrator role ${role.mention}."
			}
		} else {
			config.integratorRoles[role.id] = IntegratorRole(
				role.id,
				cooldown,
				limit
			)
			guildRepository.save(config)

			interaction.respondPublic {
				content =
					"Added new integrator role ${role.mention} with limit $limit and cooldown ${cooldown.toPrettyIsoString()}"
			}
		}

	}

}

class ConfigIntegratorRoleRemove : KoinComponent,
	SubCommand("removerole", "Stop a role from being able to integrate users.") {

	private val guildRepository: GuildRepository by inject()

	override suspend fun SubCommandBuilder.build() {
		role("role", "The integrator role to remove.") {
			required = true
		}
	}

	override suspend fun GuildChatInputCommandInteractionCreateEvent.on() {
		val role = interaction.command.roles["role"]!!

		val config = guildRepository.getGuildConfig(interaction.guild)
		if (config.integratorRoles.remove(role.id) != null) {
			guildRepository.save(config)
			interaction.respondPublic {
				content = "Removed integrator role ${role.mention}."
			}
		} else {
			interaction.respondPublic {
				content = "The role ${role.mention} is not a integrator role."
			}
		}

	}

}

class ConfigShow : KoinComponent, SubCommand("show", "Show the current guild config.") {

	private val guildRepository: GuildRepository by inject()

	override suspend fun SubCommandBuilder.build() {

	}

	override suspend fun GuildChatInputCommandInteractionCreateEvent.on() {
		val config = guildRepository.getGuildConfig(interaction.guild)
		interaction.respondPublic {
			embed {
				title = "consts.Config for ${interaction.getGuild().name}"
				field {
					name = "Log Channel"
					value = config.logChannel?.let { "<#$it>" } ?: "Not set"
				}
				field {
					name = "Approval Role"
					value = config.approvalRole?.let { "<@&$it>" } ?: "Not set"
				}
				field {
					name = "Member Time Before Integration"
					value = config.minMemberTime.toPrettyIsoString()
				}
				field {
					name = "Integrator Roles"
					value = if (config.integratorRoles.isEmpty()) {
						"Not set"
					} else {
						config.integratorRoles.values.joinToString("\n") {
							"<@&${it.snowflake}> - Cooldown: ${
								it.cooldown.toPrettyIsoString()
							} - Limit: ${it.limit}"
						}
					}
				}
			}
		}
	}

}