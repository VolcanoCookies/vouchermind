package service

import org.koin.dsl.module

val serviceModule = module {

	single(createdAtStart = true) { CommandService() }
	single(createdAtStart = true) { IntegrationService() }

}
