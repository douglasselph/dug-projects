package dugsolutions.leaf.v30.table.di

import dugsolutions.leaf.v30.table.Table
import org.koin.core.module.Module
import org.koin.dsl.module

val tableModule: Module = module {

    single { Table(get(), get(), get()) }
    single { TableConfigFactory(get(), get(), get()) }
}
