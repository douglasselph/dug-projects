from __future__ import annotations
from src.data.Game import Game
from src.data.Decision import DecisionDeck
from src.data.Card import Card
from src.data.Player import Player


class BaseDeployBlock:

    game: Game
    player: Player

    def set_game(self, game: Game) -> BaseDeployBlock:
        self.game = game
        return self

    def set_player(self, player: Player) -> BaseDeployBlock:
        self.player = player
        return self

    def acquire_block_value(self, decision: DecisionDeck, card: Card) -> int:
        return 0

