package dugsolutions.leaf.v14.main.di

import dugsolutions.leaf.v14.main.MainController
import dugsolutions.leaf.v14.main.gather.GatherCardInfo
import dugsolutions.leaf.v14.main.gather.GatherDiceInfo
import dugsolutions.leaf.v14.main.gather.GatherGroveInfo
import dugsolutions.leaf.v14.main.gather.GatherPlayerInfo
import dugsolutions.leaf.v14.main.gather.MainActionManager
import dugsolutions.leaf.v14.main.gather.MainGameManager
import dugsolutions.leaf.v14.main.gather.MainOutputManager
import dugsolutions.leaf.v14.main.local.DecidingPlayer
import dugsolutions.leaf.v14.main.local.MainActionHandler
import dugsolutions.leaf.v14.main.local.MainDecisions
import dugsolutions.leaf.v14.main.local.SelectGather
import dugsolutions.leaf.v14.main.local.SelectItem
import org.koin.core.module.Module
import org.koin.dsl.module

val mainModule: Module = module {

    single { DecidingPlayer() }
    single { GatherCardInfo(get()) }
    single { GatherDiceInfo() }
    single { GatherGroveInfo(get(), get(), get(), get(), get()) }
    single { GatherPlayerInfo(get(), get(), get()) }
    single { SelectItem() }
    single { SelectGather(get()) }
    single { MainGameManager(get(), get(), get(), get(), get(), get()) }
    single { MainOutputManager() }
    single { MainDecisions(get(), get(), get(), get(), get(), get()) }
    single { MainActionManager(get()) }
    single { MainActionHandler(get()) }

    single {
        MainController(
            get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(),
            get(), get()
        )
    }

}
