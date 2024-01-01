import unittest
from src.decision.Decisions import Decisions
from src.engine.RunGame import RunGame
from src.data.Player import Player
from src.data.Game import Game


class TestRunGame(unittest.TestCase):

    def setUp(self):
        self.player1 = Player()
        self.game = Game()
        self.decisions = Decisions()
        self.SUT = RunGame(self.game, self.decisions)
