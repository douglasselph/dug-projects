import unittest

from src.data.Card import CardWound


class TestCard(unittest.TestCase):

    def test_wound_upgrade__as_expected(self):
        # Arrange
        card = CardWound.WOUND_MINOR
        # Act
        card1 = card.upgrade
        card2 = card1.upgrade
        card3 = card2.upgrade
        # Assert
        self.assertEqual(CardWound.WOUND_ACUTE, card1)
        self.assertEqual(CardWound.WOUND_GRAVE, card2)
        self.assertEqual(CardWound.WOUND_DIRE, card3)

    def test_wound_energy_penalty__increases(self):
        # Assert
        self.assertTrue(CardWound.WOUND_MINOR.energy_penalty <= CardWound.WOUND_ACUTE.energy_penalty)
        self.assertTrue(CardWound.WOUND_ACUTE.energy_penalty <= CardWound.WOUND_GRAVE.energy_penalty)
        self.assertTrue(CardWound.WOUND_GRAVE.energy_penalty <= CardWound.WOUND_DIRE.energy_penalty)
