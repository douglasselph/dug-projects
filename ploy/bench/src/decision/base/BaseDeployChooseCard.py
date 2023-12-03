from __future__ import annotations
from src.data.Decision import DecisionDeck
from src.data.Game import Game
from src.data.Player import Player


class BaseDeployChooseCard:

    game: Game

    def set_game(self, game: Game) -> BaseDeployChooseCard:
        self.game = game
        return self

    def card_to_acquire(self, player: Player, opponent: Player) -> DecisionDeck:
        return DecisionDeck.NONE
