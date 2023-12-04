import unittest
from unittest.mock import Mock

from src.data.Game import Game
from src.data.Card import CardWound


class TestGame(unittest.TestCase):

    def setUp(self):
        self.SUT = Game()

    def test_nn_next_cards_calls_player_next_cards(self):
        # Arrange
        player_mock = Mock()
        self.SUT.agentPlayer = player_mock
        # Act
        self.SUT.nn_next_cards(8)
        # Assert
        player_mock.nn_next_cards.assert_called_once_with(8)

    def test_agent_energy_returns_agent_energy(self):
        # Arrange
        self.SUT.agentPlayer.energy = 10
        # Act
        energy = self.SUT.agent_energy
        # Assert
        self.assertEqual(10, energy)

    def test_agent_pips_returns_agent_pips(self):
        # Arrange
        self.SUT.agentPlayer.pips = 12
        # Act
        pips = self.SUT.agent_pips
        # Assert
        self.assertEqual(12, pips)

    def test_agent_stash_cards_total_makes_expected_call(self):
        # Arrange
        self.SUT.agentPlayer.stash.append(CardWound.WOUND_MINOR)
        self.SUT.agentPlayer.stash.append(CardWound.WOUND_MINOR)
        self.SUT.agentPlayer.stash.append(CardWound.WOUND_ACUTE)
        self.SUT.agentPlayer.stash.append(CardWound.WOUND_ACUTE)
        self.SUT.agentPlayer.stash.append(CardWound.WOUND_GRAVE)
        self.SUT.agentPlayer.stash.draw(2)
        # Act
        total = self.SUT.agent_stash_cards_total
        # Assert
        self.assertEqual(5, total)

    def test_agent_line_intention_id_returns_intention_on_indicated_line(self):
        # Arrange
        # Act
        intention = self.SUT.agent_line_intention_id(1)
        # Assert
