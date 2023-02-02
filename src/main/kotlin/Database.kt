import consts.Config
import model.data.GuildConfig
import model.data.Integration
import model.data.UserEntry
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.litote.kmongo.KMongo
import org.litote.kmongo.getCollection
import repository.GuildRepository
import repository.IntegrationRepository
import repository.UserRepository

val databaseModule = module {

	val mongo = KMongo.createClient(Config.mongoUrl)
	val database = mongo.getDatabase(Config.databaseName)

	single { mongo }
	single { database }

	single(named<UserEntry>()) { database.getCollection<UserEntry>() }
	single(named<GuildConfig>()) { database.getCollection<GuildConfig>() }
	single(named<Integration>()) { database.getCollection<Integration>() }

	single { UserRepository() }
	single { GuildRepository() }
	single { IntegrationRepository() }

}