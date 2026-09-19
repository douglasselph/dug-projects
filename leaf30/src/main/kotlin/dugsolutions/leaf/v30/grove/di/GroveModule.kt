package dugsolutions.leaf.v30.grove.di

import dugsolutions.leaf.v30.grove.Grove
import org.koin.core.module.Module
import org.koin.dsl.module

val groveModule: Module = module {

    single { Grove(get()) }
}
