package dugsolutions.leaf.v35.grove.di

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Grove itself is intentionally NOT registered as a singleton.
 *
 * GroveFactory is stateless with respect to game state and may safely be
 * application-wide. Each game invokes it to receive a new independent Grove.
 */
val groveModule: Module = module {

    single {
        GroveFactory(
            wispCardManager = get()
        )
    }
}
