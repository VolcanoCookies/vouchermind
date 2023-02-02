package service

import consts.Colors
import consts.Config
import dev.kord.common.DiscordTimestampStyle
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.common.toMessageFormat
import dev.kord.core.Kord
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createInvite
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.interaction.GuildButtonInteraction
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.core.event.guild.BanAddEvent
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import dev.kord.core.event.interaction.GlobalButtonInteractionCreateEvent
import dev.kord.core.event.interaction.GuildButtonInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import getBanLog
import getKickLog
import getMessage
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import location
import log
import model.BeginIntegrationException
import model.CancelIntegrationException
import model.CommandException
import model.data.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.litote.kmongo.newId
import org.litote.kmongo.toId
import repository.GuildRepository
import repository.IntegrationRepository
import repository.UserRepository
import timestampNow
import toPrettyIsoString
import kotlin.time.Duration.Companion.days

class IntegrationService : KoinComponent {

	private val kord: Kord by inject()
	private val userRepository: UserRepository by inject()
	private val guildRepository: GuildRepository by inject()
	private val integrationRepository: IntegrationRepository by inject()

	@Throws(BeginIntegrationException::class)
	suspend fun beginIntegration(interaction: GuildChatInputCommandInteraction, target: User, note: String?) {
		val config = guildRepository.getGuildConfig(interaction.guild)

		val entry = userRepository.getUserEntry(target)
		val integration = integrationRepository.getLatestActive(interaction.guild, target)

		val canIntegrateAfter = interaction.user.joinedAt + config.minMemberTime
		val topRole = interaction.user.roles.toList().sortedBy { it.rawPosition }
			.firstOrNull { config.integratorRoles[it.id] != null }
			?.let { config.integratorRoles[it.id] }
		when {
			canIntegrateAfter > Clock.System.now() -> throw BeginIntegrationException(
				"You need to wait until ${
					canIntegrateAfter.toMessageFormat(
						DiscordTimestampStyle.RelativeTime
					)
				} before being able to integrate users."
			)

			topRole == null -> throw BeginIntegrationException("You do not have any of the roles required for integrating members.")
			interaction.user == target -> throw BeginIntegrationException("Cannot integrate yourself.")
			target.isBot -> throw BeginIntegrationException("Cannot integrate a bot.")
			entry.blacklisted -> throw BeginIntegrationException("${target.mention} is blacklisted.")
			integration is Requested -> throw BeginIntegrationException("${target.mention} has already begun their integration.")
			integration is Approved -> throw BeginIntegrationException("${target.mention} has already begun their integration.")
			integration is Completed -> throw BeginIntegrationException("${target.mention} is already integrated.")
			config.integrationRequestChannel == null -> throw BeginIntegrationException("This guild has no integration request channel, please contact an administrator.")
		}
		topRole!!

		val recentIntegrationLogs =
			integrationRepository.getRecent(interaction.guild, interaction.user.id, topRole.cooldown)
		if (recentIntegrationLogs.size >= topRole.limit && recentIntegrationLogs.isNotEmpty()) {
			val oldest = recentIntegrationLogs.minBy { it.integratedAt }
			val timeLeft = oldest.integratedAt - (Clock.System.now() - topRole.cooldown)

			throw BeginIntegrationException("You have hit your integration limit, please wait ${timeLeft.toPrettyIsoString()} before integrating anyone.")
		}

		val requestChannel = kord.getChannel(config.integrationRequestChannel!!)!!.asChannelOf<TextChannel>()

		val message = requestChannel.createMessage {
			embed {
				title = "Integration Request"
				description = "${interaction.user.mention} requested the integration of ${target.mention}."
				target.avatar?.url?.let {
					thumbnail {
						url = it
					}
				}

				field {
					name = "Requested By"
					value = interaction.user.mention
				}
				field {
					name = "Requested At"
					value = interaction.id.timestamp.toMessageFormat(DiscordTimestampStyle.LongDateTime)
				}
			}
			actionRow {
				interactionButton(ButtonStyle.Success, "integration-approve-${target.id}") {
					label = "Approve"
				}
				interactionButton(ButtonStyle.Danger, "integration-deny-${target.id}") {
					label = "Deny"
				}
			}
		}

		val request = Requested(
			newId(),
			interaction.guildId,
			target.id,
			interaction.user.id,
			interaction.id.timestamp,
			note,
			message.location(),
			null
		)
		integrationRepository.save(request)
	}

	@Throws(CancelIntegrationException::class)
	suspend fun cancelIntegrationManual(interaction: GuildChatInputCommandInteraction, reason: String?, target: User) {
		val integration = integrationRepository.getLatest(interaction.guild, target)


		if (integration != null) {
			if (integration.integratedBy != interaction.user.id)
				throw CancelIntegrationException("${target.mention} is not being integrated by you.")
		}

		val terminated = Terminated.Cancelled(
			interaction.id.timestamp,
			reason ?: "unknown"
		)

		cancelIntegration(interaction.guild, target, terminated, cachedIntegration = integration)
	}

	@Throws(CancelIntegrationException::class)
	suspend fun cancelIntegration(
		guild: GuildBehavior,
		target: User,
		terminated: Terminated,
		cachedConfig: GuildConfig? = null,
		cachedEntry: UserEntry? = null,
		cachedIntegration: Integration? = null
	) {
		val config = cachedConfig ?: guildRepository.getGuildConfig(guild)
		val entry = cachedEntry ?: userRepository.getUserEntry(target)
		val integration = cachedIntegration ?: integrationRepository.getLatest(guild, target)

		when {
			integration == null -> throw CancelIntegrationException("${target.mention} is not currently integrated.")
			integration.terminated != null -> throw CancelIntegrationException("${target.mention} users integration has already been terminated.")
			integration is Completed -> throw CancelIntegrationException("${target.mention} has already been fully integrated.")
		}
		integration!!

		integration.terminated = terminated
		integrationRepository.save(integration)

		val message = kord.getMessage(integration.message)?.asMessageOrNull() ?: let {
			guild.log(config) {
				content = "Cannot find integration request channel or message for integration $integration"
			}
			return
		}

		val oldEmbed = message.embeds.firstOrNull()
		message.edit {
			embed {
				oldEmbed?.apply(this)

				title = "Cancelled Integration Request"

				field {
					name = "Reason"
					value = terminated.reason
				}
				field {
					name = "Cancelled At"
					value = terminated.at.toMessageFormat(DiscordTimestampStyle.LongDateTime)
				}

				color = Colors.cancel
			}

			components = mutableListOf()
		}
	}

	private suspend fun approveIntegration(
		interaction: GuildButtonInteraction,
		integration: Requested,
		config: GuildConfig
	) {

		val member = interaction.guild.getMemberOrNull(integration.snowflake)

		val approved = integration.approve(interaction.user.id, interaction.id.timestamp)
		if (member == null) {
			integrationRepository.save(approved)
		} else {
			val completed = approved.complete(member.joinedAt)
			integrationRepository.save(completed)
		}


		interaction.log(config) {
			embed {
				color = Colors.approve
				description =
					"${interaction.user.mention} approved <@${integration.integratedBy}>'s integration request for <@${integration.snowflake}>."
				timestampNow()
			}
		}

		interaction.message.edit {
			embed {
				interaction.message.embeds.firstOrNull()?.apply(this)

				field {
					name = "Approved By"
					value = interaction.user.mention
				}
				field {
					name = "Approved At"
					value = interaction.id.timestamp.toMessageFormat(DiscordTimestampStyle.LongDateTime)
				}

				color = Colors.approve
			}

			components = mutableListOf()
		}

		kord.getUser(integration.integratedBy)?.getDmChannelOrNull()?.createMessage {
			if (member == null) {
				content =
					"You request for integrating <@${approved.snowflake}> has been approved by <@${approved.approvedBy}>."
				actionRow {
					interactionButton(ButtonStyle.Primary, "7dayinvitelink-${integration.id}") {
						label = "Generate one-time invite link"
					}
				}
			} else {
				content =
					"You request for integrating <@${approved.snowflake}> has been approved by <@${approved.approvedBy}>, they are already a member of the guild thus no invite link will be generated."
			}
		}

	}

	private suspend fun denyIntegration(
		interaction: GuildButtonInteraction,
		integration: Integration,
		config: GuildConfig
	) {
		integration.terminated = Terminated.Denied(
			interaction.id.timestamp,
			interaction.user.id
		)
		integrationRepository.save(integration)

		val entry = userRepository.getById(integration.snowflake) ?: let {
			interaction.guild.log(config) {
				content = "User entry with id ${integration.snowflake} was not found for integration $integration"
			}
			return
		}

		entry.blacklisted = true
		userRepository.save(entry)

		interaction.log(config) {
			embed {
				color = Colors.deny
				description =
					"${interaction.user.mention} denied ${interaction.user.mention}'s integration request for <@${entry.snowflake}>."
				timestampNow()
			}
		}

		interaction.message.edit {
			embed {
				interaction.message.embeds.firstOrNull()?.apply(this)

				field {
					name = "Denied By"
					value = interaction.user.mention
				}
				field {
					name = "Denied At"
					value = interaction.id.timestamp.toMessageFormat(DiscordTimestampStyle.LongDateTime)
				}

				color = Colors.deny
			}

			components = mutableListOf()
		}

	}

	suspend fun disintegrate(interaction: GuildChatInputCommandInteraction, target: User, reason: String) {
		val config = guildRepository.getGuildConfig(interaction.guild)
		val integration = integrationRepository.getLatest(interaction.guild, target)

		when {
			config.approvalRole == null -> throw CommandException("Approval role not set, contact an administrator.")
			!interaction.user.roleIds.contains(config.approvalRole) -> throw CommandException("You do not have the required role <@&${config.approvalRole}> to disintegrate.")
			integration == null -> throw CommandException("${target.mention} is not integrated.")
			integration.terminated != null -> throw CommandException("${target.mention}s integration has already been terminated.")
		}

		integration!!.terminated = Terminated.Disintegrated(
			interaction.id.timestamp,
			reason,
			interaction.user.id
		)
		integrationRepository.save(integration)

		if (!Config.isDev)
			interaction.guild.getMemberOrNull(target.id)?.kick("Disintegrated by ${interaction.user.mention}")
	}

	init {
		kord.on<GuildButtonInteractionCreateEvent> {
			if (!interaction.componentId.matches(Regex("integration-(approve|deny)-\\d+"))) return@on

			val config = guildRepository.getGuildConfig(interaction.guild)

			val approve = interaction.componentId.split("-")[1] == "approve"

			when {
				config.approvalRole == null -> {
					interaction.respondEphemeral {
						content = "This guild has no approval role set, please contact an administrator."
					}
					return@on
				}

				!interaction.user.roleIds.contains(config.approvalRole) -> {
					interaction.respondEphemeral {
						content =
							"You do not have the required role <@&${config.approvalRole}> to approve integrations, if you believe this is an error please contact an administrator."
					}
					return@on
				}
			}

			val integration = integrationRepository.getByMessage(interaction.message) ?: let {
				interaction.respondEphemeral {
					content = "Could not find the associated integration associated with this message."
				}
				return@on
			}

			if (integration.isTerminated) {
				interaction.respondEphemeral {
					content = "Integration has been terminated."
				}
				return@on
			}

			when (integration) {
				is Approved -> {
					interaction.respondEphemeral {
						content = "Integration has already been approved by <@${integration.approvedBy}>."
					}
				}

				is Completed -> {
					interaction.respondEphemeral {
						content =
							"User has already completed their integration, approved by <@${integration.approvedBy}>."
					}
				}

				is Requested -> {
					if (approve) {
						approveIntegration(interaction, integration, config)
					} else {
						denyIntegration(interaction, integration, config)
					}
				}
			}

		}

		kord.on<MemberJoinEvent> {
			if (member.isBot) return@on

			val config = guildRepository.getGuildConfig(guild)
			val user = member.asUser()

			if (config.lockdown) {
				member.getDmChannelOrNull()?.createMessage {
					content = "${getGuild().name} is currently in lockdown."
				}
				member.kick("Guild in lockdown")
				guild.log(config) {
					content = "Prevented ${user.mention} from joining, the guild is in lockdown."
				}
			}

			val entry = userRepository.getUserEntry(user)
			if (entry.blacklisted) {
				member.getDmChannelOrNull()?.createMessage {
					content = "You have not been integrated into ${getGuild().name} yet."
				}
				member.kick("User blacklisted")
				guild.log(config) {
					content = "Prevented ${user.mention} from joining, they have been blacklisted."
				}
				return@on
			}

			val integration = integrationRepository.getLatest(guild, user)
			when {
				integration == null -> {
					member.getDmChannelOrNull()?.createMessage {
						content = "You have not been integrated into ${getGuild().name} yet."
					}
					member.kick("User not integrated")
					guild.log(config) {
						content = "Prevented ${user.mention} from joining, they have not been integrated."
					}
					return@on
				}

				integration.terminated != null -> {
					member.getDmChannelOrNull()?.createMessage {
						content = "You have not been integrated into ${getGuild().name} yet."
					}
					member.kick("User integration terminated")
					guild.log(config) {
						content = "Prevented ${user.mention} from joining, their integration has been terminated."
					}
					return@on
				}

				integration is Requested -> {
					member.getDmChannelOrNull()?.createMessage {
						content = "Your integration into ${getGuild().name} has not been approved yet."
					}
					member.kick("User integration not approved")
					guild.log(config) {
						content = "Prevented ${user.mention} from joining, theír integration awaiting approval."
					}
					return@on
				}

				integration is Approved -> {
					val completed = integration.complete(member.joinedAt)
					integrationRepository.save(completed)
				}
			}

			guild.log(config) {
				content = "${user.mention} has joined the server, integrated by ${integration!!.integratedBy}."
			}

		}

		kord.on<MemberLeaveEvent> {
			val integration = integrationRepository.getLatest(guild, user)
			if (integration == null || integration.terminated != null) return@on

			val config = guildRepository.getGuildConfig(guild)

			val log = getKickLog() ?: let {
				integration.terminated = Terminated.Left(
					Clock.System.now()
				)
				integrationRepository.save(integration)

				guild.log(config) {
					content =
						"${user.mention} has left the server, their integration status has now been set to terminated."
				}
				return@on
			}

			integration.terminated = Terminated.Kicked(
				Clock.System.now(),
				log.reason ?: "unknown",
				log.userId
			)
			integrationRepository.save(integration)

			guild.log(config) {
				content =
					"${user.mention} has been kicked from the server, their integration status has now been set to terminated."
			}
		}

		kord.on<BanAddEvent> {
			val integration = integrationRepository.getLatest(guild, user)
			if (integration == null || integration.terminated != null) return@on

			val config = guildRepository.getGuildConfig(guild)

			val ban = getBanOrNull() ?: let {
				integration.terminated = Terminated.Left(
					Clock.System.now()
				)
				integrationRepository.save(integration)

				guild.log(config) {
					content =
						"${user.mention} has been banned from the server, their integration status has now been set to terminated."
				}

				return@on
			}

			val log = getBanLog() ?: let {
				integration.terminated = Terminated.Banned(
					Clock.System.now(),
					"unknown",
					Snowflake(0),
				)
				integrationRepository.save(integration)

				guild.log(config) {
					content =
						"${user.mention} has been banned from the server, their integration status has now been set to terminated."
				}

				return@on
			}

			integration.terminated = Terminated.Banned(
				Clock.System.now(),
				log.reason ?: "unknown",
				log.userId
			)


			guild.log(config) {
				content =
					"${user.mention} has been banned from the server, their integration status has now been set to terminated."
			}

		}

		kord.on<GlobalButtonInteractionCreateEvent> {
			if (!interaction.componentId.matches(Regex("7dayinvitelink-.*"))) return@on

			val integrationId = interaction.componentId.split("-")[1]
			val integration = integrationRepository.getById(integrationId.toId()) ?: let {
				interaction.respondEphemeral { content = "Could not find integration with id $integrationId." }
				return@on
			}

			val guild = kord.getGuildOrNull(integration.guild) ?: let {
				interaction.respondEphemeral { content = "Could not guild with id ${integration.guild}." }
				return@on
			}

			val channel = guild.getSystemChannel() ?: let {
				interaction.respondEphemeral {
					content =
						"Guild with id ${integration.guild} has no system channel, please contact an administrator."
				}
				return@on
			}

			val invite = channel.createInvite {
				unique = true
				maxAge = 7.days
				maxUses = 1
			}

			interaction.respondPublic {
				content = "https://discord.gg/${invite.code}"
			}


			interaction.message.edit {
				actionRow {
					interactionButton(ButtonStyle.Primary, "disabled") {
						label = "Generate one-time invite link"
						disabled = true
					}
				}
			}

		}
	}

}