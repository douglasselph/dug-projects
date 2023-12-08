import unittest
from src.data.die.DieValues import DieValues
from src.data.die.Die import Die, DieSides
from unittest.mock import Mock


class TestDieValues(unittest.TestCase):

    def setUp(self):
        self.SUT = DieValues()
        self.sides = [DieSides.D4, DieSides.D6, DieSides.D8, DieSides.D10, DieSides.D12]
        for side in self.sides:
            self.SUT.add(Die(side))

    def test_add__actually_added(self):
        # Arrange
        self.SUT = DieValues()
        die = Die(DieSides.D4)
        # Act
        self.SUT.add(die)
        # Assert
        self.assertTrue(die in self.SUT.values)

    def test_dup__duplicates_everything(self):
        # Arrange
        # Act
        dup = self.SUT.dup()
        # Assert
        for index, side in enumerate(self.sides):
            self.assertEquals(side, dup.values[index].sides)

    def test_roll__all_die_have_values(self):
        # Arrange
        # Act
        self.SUT.roll()
        # Assert
        for die in self.SUT.values:
            self.assertTrue(die.value > 0)

    def test_total__expected_total_returned(self):
        # Arrange
        expected_total = 0
        self.SUT.roll()
        for die in self.SUT.values:
            expected_total += die.value
        # Act
        total = self.SUT.total
        # Assert
        self.assertEqual(expected_total, total)







