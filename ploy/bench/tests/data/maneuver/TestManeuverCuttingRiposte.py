import unittest

from src.data.maneuver.ManeuverCuttingRiposte import ManeuverCuttingRiposte
from src.data.die.DieCollection import DieCollection
from src.data.die.Die import DieSides


class TestManeuverCuttingRiposte(unittest.TestCase):

    def setUp(self):
        self.SUT = ManeuverCuttingRiposte()

    def test_maneuver_cutting_riposte(self):
        # Arrange
        collection = DieCollection([
            DieSides.D4,
            DieSides.D6,
            DieSides.D8,
            DieSides.D10,
            DieSides.D12,
            DieSides.D20
        ])
        # Act
        self.assertTrue(DieSides.D20 in collection.sides)
        self.SUT.apply(collection)
        # Assert
        self.assertFalse(DieSides.D20 in collection.sides)


