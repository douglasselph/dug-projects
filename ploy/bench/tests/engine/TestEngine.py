import unittest
from src.data.Game import Game
from src.engine.Engine import Engine
from src.decision.Decisions import Decisions


class TestEngine(unittest.TestCase):

    def setUp(self):
        self.game = Game()
        self.decisions = Decisions()
        self.SUT = Engine(self.game, self.decisions)
