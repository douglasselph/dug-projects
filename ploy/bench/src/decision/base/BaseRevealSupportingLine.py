from __future__ import annotations

from src.data.Game import Game
from src.data.Player import Player
from src.data.Decision import DecisionIntention


class BaseRevealSupportingLine:
    game: Game
    player: Player

    def set_game(self, game: Game) -> BaseRevealSupportingLine:
        self.game = game
        return self

    def apply(self, player: Player, coin: DecisionIntention):
        pass
