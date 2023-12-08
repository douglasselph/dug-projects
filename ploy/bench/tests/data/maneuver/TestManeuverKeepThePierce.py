import unittest

from src.data.die.Die import DieSides, Die
from src.data.die.DieValues import DieValues
from src.data.maneuver.ManeuverKeepThePierce import ManeuverKeepThePierce


class TestManeuverKeepThePierce(unittest.TestCase):

    def setUp(self):
        self.SUT = ManeuverKeepThePierce()

    def test_apply__opponent_has_a_one__corresponding_die_captured(self):
        # Arrange
        own = DieValues()
        own.add(Die(DieSides.D10, 4))
        opponent = DieValues()
        opponent.add(Die(DieSides.D4, 2))
        opponent.add(Die(DieSides.D8, 1))
        self.assertEqual(1, len(own.values))
        self.assertEqual(2, len(opponent.values))
        # Act
        self.SUT.apply(own, opponent)
        # Assert
        self.assertEqual(2, len(own.values))
        self.assertEqual(2, len(opponent.values))

