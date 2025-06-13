package dugsolutions.leaf.main.di

import dugsolutions.leaf.main.MainController
import dugsolutions.leaf.main.gather.GatherCardInfo
import dugsolutions.leaf.main.gather.GatherDiceInfo
import dugsolutions.leaf.main.gather.GatherGroveInfo
import dugsolutions.leaf.main.gather.GatherPlayerInfo
import dugsolutions.leaf.main.gather.MainGameManager
import dugsolutions.leaf.main.gather.MainOutputManager
import dugsolutions.leaf.main.local.MainDecisions
import dugsolutions.leaf.main.local.SelectGather
import dugsolutions.leaf.main.local.SelectItem
import org.koin.core.module.Module
import org.koin.dsl.module

val mainModule: Module = module {

    single { GatherCardInfo() }
    single { GatherDiceInfo() }
    single { GatherGroveInfo(get(), get(), get(), get()) }
    single { GatherPlayerInfo(get(), get()) }
    single { SelectItem() }
    single { SelectGather(get()) }
    single { MainGameManager(get(), get(), get(), get(), get(), get(), get()) }
    single { MainOutputManager() }
    single { MainDecisions(get(), get(), get(), get()) }

    single {
        MainController(
            get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get()
        )
    }

}
