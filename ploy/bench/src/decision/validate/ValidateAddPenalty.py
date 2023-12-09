from __future__ import annotations
from typing import List, Optional
import random

from src.data.Card import Card, CardComposite
from src.data.Player import Player
from src.data.Game import Game
from src.data.Decision import DecisionIntention, coin_for


class ValidateAddPenalty:

    game: Game
    player: Player

    def set_game(self, game: Game) -> ValidateAddPenalty:
        self.game = game
        return self

    def set_player(self, player: Player) -> ValidateAddPenalty:
        self.player = player
        return self

    def select_coin_to_penalize(self, player: Player) -> Optional[DecisionIntention]:
        return coin_for(random.randint(1, 3))
