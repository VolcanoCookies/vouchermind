package repository

import com.mongodb.client.MongoCollection
import dev.kord.core.behavior.GuildBehavior
import model.data.GuildConfig
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.litote.kmongo.findOneById
import org.litote.kmongo.save

class GuildRepository : KoinComponent {

	private val collection: MongoCollection<GuildConfig> by inject(named<GuildConfig>())

	fun getGuildConfig(guild: GuildBehavior): GuildConfig {
		return collection.findOneById(guild.id) ?: GuildConfig(guild.id)
	}

	fun save(config: GuildConfig) {
		collection.save(config)
	}

}