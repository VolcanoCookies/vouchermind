package repository

import com.mongodb.client.MongoCollection
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.User
import model.data.UserEntry
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.litote.kmongo.eq
import org.litote.kmongo.findOneById
import org.litote.kmongo.save

class UserRepository : KoinComponent {

	private val collection: MongoCollection<UserEntry> by inject(named<UserEntry>())

	fun getUserEntry(user: User): UserEntry {
		return getById(user.id) ?: UserEntry(user.id)
	}

	fun getById(id: Snowflake): UserEntry? {
		return collection.findOneById(id)
	}

	fun getAllBlacklisted(): List<UserEntry> {
		return collection.find(
			UserEntry::blacklisted eq true
		).toList()
	}

	fun save(userEntry: UserEntry) {
		collection.save(userEntry)
	}

}