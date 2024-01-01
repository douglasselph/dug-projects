import unittest
from src.die.Die import *


class TestDie(unittest.TestCase):

    def test_average__8(self):
        # Arrange
        die = Die(DieSides.D8)
        # Act
        average = die.average
        # Assert
        self.assertEqual(4.5, average)

    def test_average__20(self):
        # Arrange
        die = Die(DieSides.D20)
        # Act
        average = die.average
        # Assert
        self.assertEqual(10.5, average)

    def test_roll__all__within_range(self):
        # Arrange
        count = 100
        # Act
        for sides in DieSides:
            if sides == DieSides.NONE:
                continue
            for i in range(count):
                value = Die(sides).roll().value
                # Assert
                self.assertTrue(value >= 1)
                self.assertTrue(value <= sides.value)

