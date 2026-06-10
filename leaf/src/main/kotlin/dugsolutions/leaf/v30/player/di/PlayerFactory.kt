package dugsolutions.leaf.v30.player.di

import dugsolutions.leaf.v30.player.Player

class PlayerFactory {

    operator fun invoke(): Player {
        return Player()
    }
}
