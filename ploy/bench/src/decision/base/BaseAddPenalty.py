from __future__ import annotations
from typing import List, Optional

from src.data.Card import Card, CardComposite
from src.data.Player import Player
from src.data.Game import Game
from src.data.Decision import DecisionIntention


class BaseAddPenalty:

    game: Game
    player: Player

    def set_game(self, game: Game) -> BaseAddPenalty:
        self.game = game
        return self

    def select_coin_to_penalize(self, player: Player) -> Optional[DecisionIntention]:
        return None
