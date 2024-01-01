from typing import Optional

from src.data.Player import Player
from src.decision.Decisions import Decisions
from src.data.Moves import Moves, MoveSupport
from src.data.Constants import Constants
from src.data.Game import Game
from src.data.ID import PlayerID


class RunBout:

    player1_support_choice: Optional[MoveSupport]
    player2_support_choice: Optional[MoveSupport]

    def __init__(self, game: Game, decisions: Decisions):
        self.player1: Player = game.player1
        self.player2: Player = game.player2
        self.player1_support_choice: Optional[MoveSupport]
        self.player2_support_choice: Optional[MoveSupport]
        self.decisions = decisions
        self.maneuversLeft = Constants.num_maneuvers
        self.moves = Moves()

    @property
    def has_maneuvers_left(self) -> bool:
        return self.maneuversLeft > 0

    def new_maneuver(self):
        self.player1.new_maneuver()
        self.player2.new_maneuver()

    def declare_maneuver(self):
        player1_declaration = self.decisions.declare.decide(PlayerID.PLAYER_1)
        player2_declaration = self.decisions.declare.decide(PlayerID.PLAYER_2)
        self.player1.apply_declaration(player1_declaration)
        self.player2.apply_declaration(player2_declaration)
        self.moves.add(player1_declaration)
        self.moves.add(player2_declaration)

    def support_maneuver(self):
        self.player1_support_choice = self.decisions.support.decide(PlayerID.PLAYER_1)
        self.player2_support_choice = self.decisions.support.decide(PlayerID.PLAYER_2)
        self.moves.add(self.player1_support_choice)
        self.moves.add(self.player2_support_choice)

    def execute_maneuver(self):
        pass
