import unittest

from src.data.maneuver.ManeuverFeelingFeint import ManeuverFeelingFeint
from src.data.ManeuverPlate import ManeuverPlate
from src.data.Decision import DecisionIntention


class TestManeuverFeelingFeint(unittest.TestCase):

    def setUp(self):
        self.SUT = ManeuverFeelingFeint()

    def test_apply(self):
        # Arrange
        plate = ManeuverPlate()
        coin = DecisionIntention.ATTACK
        # Act
        self.SUT.apply(plate, coin)
        # Assert
