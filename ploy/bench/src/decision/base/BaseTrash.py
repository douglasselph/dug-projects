from __future__ import annotations
from typing import List, Optional

from src.data.Card import Card, CardComposite
from src.data.Player import Player
from src.data.Game import Game


class BaseTrash:

    game: Game
    player: Player

    def set_game(self, game: Game) -> BaseTrash:
        self.game = game
        return self

    def set_player(self, player: Player) -> BaseTrash:
        self.player = player
        return self

    def select_card_to_trash(self, cards: List[CardComposite]) -> Optional[Card]:
        return None
