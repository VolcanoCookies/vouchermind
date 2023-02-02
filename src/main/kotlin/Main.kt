import dev.kord.core.Kord
import kotlinx.coroutines.coroutineScope
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext.startKoin
import service.CommandService
import service.serviceModule

class Application : KoinComponent {

	val kord: Kord by inject()
}

suspend fun main() = coroutineScope {

	startKoin {
		modules(databaseModule, discordModule, serviceModule)
	}
	
	Application().kord.login()
}
