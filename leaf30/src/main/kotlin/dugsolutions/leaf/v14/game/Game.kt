package dugsolutions.leaf.v14.game

import dugsolutions.leaf.v14.player.di.PlayerFactory
import dugsolutions.leaf.v14.game.battle.BattlePhaseTransition
import dugsolutions.leaf.v14.game.domain.GamePhase
import dugsolutions.leaf.v14.game.domain.GameTime
import dugsolutions.leaf.v14.game.turn.PlayerOrder
import dugsolutions.leaf.v14.game.turn.PlayerTurn
import dugsolutions.leaf.v14.game.turn.config.IsEliminated
import dugsolutions.leaf.v14.game.turn.config.IsEliminatedNoDiceNorCards
import dugsolutions.leaf.v14.game.turn.handle.HandleDrawHand
import dugsolutions.leaf.v14.grove.Grove
import dugsolutions.leaf.v14.player.Player
import dugsolutions.leaf.v14.player.domain.PlayerScoreData
import dugsolutions.leaf.v14.player.domain.PlayersScoreData

class Game(
    private val playerTurn: PlayerTurn,
    private val playerFactory: PlayerFactory,
    private val playerOrder: PlayerOrder,
    private val handleDrawHand: HandleDrawHand,
    private val grove: Grove,
    private val gameTime: GameTime,
    private val battlePhaseTransition: BattlePhaseTransition
) {

    var isEliminated: IsEliminated = IsEliminatedNoDiceNorCards()

    val score: PlayersScoreData
        get() {
            return PlayersScoreData(
                turn = gameTime.turn,
                players = players.map { player -> PlayerScoreData(player, player.score) }
            )
        }

    var players: List<Player> = emptyList()
        private set

    val isGameFinished: Boolean
        get() = players.count { !isEliminated(it) } <= 1

    data class Config(
        val numPlayers: Int,
        val isEliminated: IsEliminated? = null,
        val setup: (index: Int, player: Player) -> Unit
    )

    private fun clear() {
        gameTime.turn = 0
        gameTime.phase = GamePhase.CULTIVATION
        players = emptyList()
    }

    fun setup(config: Config) {
        Player.resetID()
        clear()

        config.isEliminated?.let { this.isEliminated = it }

        val playersList = mutableListOf<Player>()

        for (i in 0 until config.numPlayers) {
            val player = playerFactory()
            playersList.add(player)
            config.setup(i, player)
            handleDrawHand(player, 2)
        }
        players = playerOrder(playersList)
    }

    suspend fun runOneCultivationTurn() {
        playerTurn(players, GamePhase.CULTIVATION)
    }

    suspend fun runOneBattleTurn() {
        playerTurn(players, GamePhase.BATTLE)
    }

    fun detectBattlePhase() {
        gameTime.phase = if (grove.readyForBattlePhase) GamePhase.BATTLE else GamePhase.CULTIVATION
    }

    suspend fun setupBattlePhase() {
        battlePhaseTransition(players)
    }


}
