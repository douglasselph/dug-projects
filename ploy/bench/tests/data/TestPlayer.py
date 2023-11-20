import unittest
from src.data.Player import Player


class TestPlayer(unittest.TestCase):

    def setUp(self):
        self.SUT = Player()
