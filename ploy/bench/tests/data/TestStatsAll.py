import unittest

from src.data.Stats import *
from src.data.Game import Game


class TestStatsAll(unittest.TestCase):

    def setUp(self):
        self.SUT = StatsAll()
        self.game = Game()
        self.game.stat = StatsGame()

    def test_apply__games_increased_by_one(self):
        # Arrange
        # Act
        self.SUT.apply(self.game)
        # Assert
        self.assertEqual(1, self.SUT.games)


