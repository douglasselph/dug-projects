import unittest

from src.DieCollection import DieCollection
from src.ManeuverKeepThePierce import ManeuverKeepThePierce


class TestManeuverKeepThePierce(unittest.TestCase):

    default_sides = [4, 6, 8, 10, 12, 20]

    def setUp(self):
        self.SUT = ManeuverKeepThePierce()
        self.collection = DieCollection(self.default_sides)

    def test_adjust_re_roll_of_d20(self):
        # Arrange
        values = self.collection.roll()
        # Act
        self.SUT.adjust(values, 1)
        # Assert

