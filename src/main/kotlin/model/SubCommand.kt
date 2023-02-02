package model

import dev.kord.core.event.interaction.AutoCompleteInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.SubCommandBuilder

abstract class SubCommand(
	val name: String,
	val description: String
) {

	abstract suspend fun SubCommandBuilder.build()

	abstract suspend fun GuildChatInputCommandInteractionCreateEvent.on()

	open suspend fun AutoCompleteInteractionCreateEvent.autocomplete() {}

}

