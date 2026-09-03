package dugsolutions.leaf.v35.random.die.di

import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.random.die.OneOfEachFaceBag

class DieFactoryOneOfEachFaceBag(
    private val randomizer: Randomizer
) : DieFactoryImpl {

    override fun invoke(sides: DieSides): Die {
        return OneOfEachFaceBag(sides.value, randomizer).roll()
    }

    override fun invoke(sides: Int): Die {
        return OneOfEachFaceBag(sides, randomizer).roll()
    }

}
