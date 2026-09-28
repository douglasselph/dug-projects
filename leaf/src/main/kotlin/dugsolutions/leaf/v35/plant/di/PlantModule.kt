package dugsolutions.leaf.v35.plant.di

import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.plant.GrovePlantResolver
import org.koin.core.module.Module
import org.koin.dsl.module

val plantModule: Module = module {

    single { PlantCardRegistry(get()) }
    single { PlantCardManager() }
    single { GrovePlantResolver(get()) }

} 
