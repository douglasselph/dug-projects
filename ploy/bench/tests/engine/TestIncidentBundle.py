import unittest
from src.engine.Die import Die


class TestIncidentBundle(unittest.TestCase):

    def test_average_8(self):
        # Arrange
        die = Die(8)
        # Act
        average = die.average
        # Assert
        self.assertEqual(4.5, average)

    def test_average_20(self):
        # Arrange
        die = Die(20)
        # Act
        average = die.average
        # Assert
        self.assertEqual(10.5, average)

    def test_roll_20(self):
        # Arrange
        sides_list = [4, 6, 8, 10, 12, 20]
        count = 100
        # Act
        for sides in sides_list:
            for i in range(count):
                value = Die(sides).roll().value
                # Assert
                self.assertTrue(value >= 1)
                self.assertTrue(value <= sides)

