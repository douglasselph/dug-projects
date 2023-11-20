from src.data.Decision import DecisionLine, DecisionIntention
from src.data.Game import Game, PlayerID
from src.data.Player import Player


class Engine:

    def __init__(self, game: Game):
        self.game = game

    ############################################################################
    # Draw Hand of 4 cards for all players.
    def draw_hands(self):
        self.game.agentPlayer.draw_hand()
        self.game.opponent.draw_hand()

    ############################################################################
    # Place the next card in the draw hand to the indicated place on the plate.
    # Return True if successful.
    # Return False if illegal indication.
    def agent_place_card(self, line: DecisionLine, coin: DecisionIntention) -> bool:
        return self._place_card(self.game.agentPlayer, line, coin)

    def opponent_place_card(self, line: DecisionLine, coin: DecisionIntention) -> bool:
        return self._place_card(self.game.opponent, line, coin)

    @staticmethod
    def _place_card(pl: Player, line: DecisionLine, coin: DecisionIntention) -> bool:
        if not pl.is_legal(line, coin):
            return False

        return pl.play_to_plate(line, coin)

    def agent_has_cards_to_place(self) -> bool:
        return self.game.agentPlayer.has_cards_to_play

    def opponent_has_cards_to_place(self) -> bool:
        return self.game.opponent.has_cards_to_play

    ###############################################################################
    # Reveal: any plate line at max has its intention coin revealed
    def reveal_intentions(self):
        self._reveal_intentions(self.game.agentPlayer)
        self._reveal_intentions(self.game.opponent)

    @staticmethod
    def _reveal_intentions(player: Player):
        if player.has_cards_to_draw:
            player.reveal_intentions_if_maxed()
        else:
            player.reveal_all_intentions()

    ###############################################################################
    # Attack Resolve:
    #   In initiative order:
    #     Attacker reveal supporting lines
    #     Defender reveal supporting lines
    #     Resolve
    def resolve_attacks(self):
        if self.game.initiativeOn == PlayerID.PLAYER_1:
            self._resolve_attack(self.game.agentPlayer)
            if self._resolve_attack(self.game.opponent):
                self.game.initiativeOn = PlayerID.PLAYER_2
        else:
            self._resolve_attack(self.game.opponent)
            if self._resolve_attack(self.game.agentPlayer):
                self.game.initiativeOn = PlayerID.PLAYER_1

    @staticmethod
    def _resolve_attack(player: Player) -> bool:
        if not player.has_intention(DecisionIntention.ATTACK):
            return False


