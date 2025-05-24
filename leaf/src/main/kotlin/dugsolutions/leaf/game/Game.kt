package dugsolutions.leaf.game

import dugsolutions.leaf.di.DieFactory
import dugsolutions.leaf.di.PlayerFactory
import dugsolutions.leaf.game.battle.BattlePhaseTransition
import dugsolutions.leaf.game.domain.GamePhase
import dugsolutions.leaf.game.domain.GameTurn
import dugsolutions.leaf.game.turn.PlayerOrder
import dugsolutions.leaf.game.turn.PlayerTurn
import dugsolutions.leaf.game.turn.config.IsEliminated
import dugsolutions.leaf.game.turn.config.IsEliminatedNoDiceNorCards
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.domain.PlayerScoreData
import dugsolutions.leaf.player.domain.PlayersScoreData

class Game(
    private val playerTurn: PlayerTurn,
    private val playerFactory: PlayerFactory,
    private val playerOrder: PlayerOrder,
    private val grove: Grove,
    private val gameTurn: GameTurn,
    private val battlePhaseTransition: BattlePhaseTransition
) {

    var isEliminated: IsEliminated = IsEliminatedNoDiceNorCards()

    val score: PlayersScoreData
        get() {
            return PlayersScoreData(
                turn = gameTurn.turn,
                players = players.map { player -> PlayerScoreData(player, player.score ) }
            )
        }

    var players: List<Player> = emptyList()
        private set

    val isGameFinished: Boolean
        get() = players.count { !isEliminated(it) } <= 1

    var inCultivationPhase: Boolean = true
        private set

    data class Config(
        val numPlayers: Int,
        val isEliminated: IsEliminated? = null,
        val dieFactory: DieFactory,
        val setup: (index: Int, player: Player) -> Unit
    )

    private fun clear() {
        gameTurn.turn = 0
        inCultivationPhase = true
        players = emptyList()
    }

    fun setup(config: Config) {
        Player.resetID()
        clear()

        config.isEliminated?.let { this.isEliminated = it }

        val playersList = mutableListOf<Player>()

        for (i in 0 until config.numPlayers) {
            val player = playerFactory(config.dieFactory)
            playersList.add(player)
            config.setup(i, player)
            player.drawHand(2)
        }
        players = playerOrder(playersList)
    }

    fun runOneCultivationTurn() {
        playerTurn(players, GamePhase.CULTIVATION)
    }

    fun runOneBattleTurn() {
        playerTurn(players, GamePhase.BATTLE)
    }

    fun detectBattlePhase() {
        inCultivationPhase = !grove.readyForBattlePhase
    }

    fun setupBattlePhase() {
        battlePhaseTransition(players)
    }

}
