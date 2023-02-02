import consts.Config
import dev.kord.core.Kord
import kotlinx.coroutines.runBlocking
import org.koin.dsl.module

val discordModule = module {
	single {
		runBlocking {
			Kord(Config.discordToken)
		}
	}
}
