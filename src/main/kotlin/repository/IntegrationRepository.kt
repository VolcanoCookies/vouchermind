package repository

import com.mongodb.client.MongoCollection
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.UserBehavior
import kotlinx.datetime.Clock
import model.data.Integration
import model.data.MessageLocation
import org.bson.types.ObjectId
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.litote.kmongo.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class IntegrationRepository : KoinComponent {

	private val collection: MongoCollection<Integration> by inject(named<Integration>())

	fun getById(id: Id<Integration>): Integration? {
		return collection.findOneById(ObjectId(id.toString()))
	}

	fun save(integration: Integration) {
		collection.save(integration)
	}

	fun getLatest(guild: GuildBehavior, user: UserBehavior): Integration? {
		return collection.find(
			Integration::guild eq guild.id,
			Integration::snowflake eq user.id
		)
			.sort(descending(Integration::integratedAt))
			.first()
	}

	fun getLatestActive(guild: GuildBehavior, user: UserBehavior): Integration? {
		return collection.find(
			Integration::guild eq guild.id,
			Integration::snowflake eq user.id,
			or(
				Integration::terminated exists false,
				Integration::terminated eq null
			)
		)
			.sort(descending(Integration::integratedAt))
			.first()
	}

	fun getByMessage(message: MessageBehavior): Integration? {
		return collection.findOne(
			Integration::message / MessageLocation::id eq message.id
		)
	}

	fun getMadeIntegrations(guild: GuildBehavior, user: UserBehavior): Set<Integration> {
		return collection.find(
			Integration::guild eq guild.id,
			Integration::integratedBy eq user.id
		).toSet()
	}

	fun getRecent(guild: GuildBehavior, integrator: Snowflake, window: Duration): List<Integration> {
		if (window <= 0.seconds) return emptyList()
		val after = Clock.System.now() - window
		return collection.find(
			Integration::guild eq guild.id,
			Integration::integratedBy eq integrator,
			Integration::integratedAt gte after
		).toList()
	}

}