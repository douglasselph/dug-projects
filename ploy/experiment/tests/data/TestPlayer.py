import unittest
from src.data.Player import Player


class TestPlayer(unittest.TestCase):

    def setUp(self):
        self.SUT = Player()

    def test_is_alive__returns_true_when_expected(self):
        # Arrange
        # Act
        is_alive = self.SUT.is_alive
        # Assert
        self.assertTrue(is_alive)

    def test_is_alive__returns_false_when_expected(self):
        # Arrange
        self.SUT.hp = 0
        # Act
        is_alive = self.SUT.is_alive
        # Assert
        self.assertFalse(is_alive)


