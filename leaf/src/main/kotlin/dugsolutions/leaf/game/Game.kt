package dugsolutions.leaf.game

import dugsolutions.leaf.di.DieFactory
import dugsolutions.leaf.di.PlayerFactory
import dugsolutions.leaf.game.domain.GamePhase
import dugsolutions.leaf.game.domain.GameTurn
import dugsolutions.leaf.game.turn.PlayerOrder
import dugsolutions.leaf.game.turn.PlayerTurn
import dugsolutions.leaf.game.turn.config.IsEliminated
import dugsolutions.leaf.game.turn.config.IsEliminatedNoDiceNorCards
import dugsolutions.leaf.game.turn.config.PlayerBattlePhaseCheck
import dugsolutions.leaf.game.turn.config.PlayerBattlePhaseCheckBloom
import dugsolutions.leaf.game.turn.config.PlayerReadyForBattlePhase
import dugsolutions.leaf.game.turn.config.PlayerSetupForBattlePhase
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.components.PlayerScoreData
import dugsolutions.leaf.player.components.PlayersScoreData

class Game(
    private val playerTurn: PlayerTurn,
    private val playerFactory: PlayerFactory,
    private val playerOrder: PlayerOrder,
    private val playerReadyForBattlePhase: PlayerReadyForBattlePhase,
    private val playerSetupForBattlePhase: PlayerSetupForBattlePhase,
    private val gameTurn: GameTurn,
    playerBattlePhaseCheckBloom: PlayerBattlePhaseCheckBloom
) {

    var playerBattlePhaseCheck: PlayerBattlePhaseCheck = playerBattlePhaseCheckBloom
    var isEliminated: IsEliminated = IsEliminatedNoDiceNorCards()

    val score: PlayersScoreData
        get() {
            return PlayersScoreData(
                turn = gameTurn.turn,
                players = players.map { player -> PlayerScoreData(player, player.score ) }
            )
        }

    // Properties from GameState
    var players: List<Player> = emptyList()
        private set

    val isGameFinished: Boolean
        get() = players.count { !isEliminated(it) } <= 1

    var inCultivationPhase: Boolean = true
        private set

    data class Config(
        val numPlayers: Int,
        val playerBattlePhaseCheck: PlayerBattlePhaseCheck? = null,
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
        // Setup players
        config.isEliminated?.let { this.isEliminated = it }
        config.playerBattlePhaseCheck?.let { this.playerBattlePhaseCheck = it }

        val playersList = mutableListOf<Player>()

        for (i in 0 until config.numPlayers) {
            val player = playerFactory(config.dieFactory)
            playersList.add(player)
            config.setup(i, player)
            player.draw(2)
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
        val countReady = players.count { player ->
            playerReadyForBattlePhase(player, playerBattlePhaseCheck.isReady(player))
        }
        val totalPlayers = players.size
        inCultivationPhase = !(countReady >= (totalPlayers / 2f))
    }

    fun setupBattlePhase() {
        players.forEach { player ->
            playerSetupForBattlePhase(player, playerBattlePhaseCheck)
        }
    }

}
