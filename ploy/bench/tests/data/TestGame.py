import unittest
from src.data.Game import Game


class TestGame(unittest.TestCase):

    def setUp(self):
        self.SUT = Game()