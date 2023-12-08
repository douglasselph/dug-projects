import unittest

from src.data.stat.StatsAll import StatsAll
from src.data.stat.StatsGame import StatsGame
from src.data.Game import Game
from src.data.Card import Card, CardWound


class TestStatsAll(unittest.TestCase):

    def setUp(self):
        self.SUT = StatsAll()
        self.game = Game()
        self.game.stat = StatsGame()

    def test_apply__games_increased_by_one(self):
        # Arrange
        # Act
        self.SUT.apply(self.game)
        # Assert
        self.assertEqual(1, self.SUT.games)

    def test_apply__highest_of_turns_advanced(self):
        # Arrange
        self.game.stat.turns = 10
        # Act
        self.SUT.apply(self.game)
        # Assert
        self.assertEqual(10, self.SUT.highest_turns)

    def test_apply__lowest_of_turns_advanced(self):
        # Arrange
        self.game.stat.turns = 10
        # Act
        self.SUT.apply(self.game)
        # Assert
        self.assertEqual(10, self.SUT.lowest_turns)

    def test_apply__with_existing__highest_of_turns_not_advanced(self):
        # Arrange
        self.SUT.highest_turns = 12
        self.game.stat.turns = 10
        # Act
        self.SUT.apply(self.game)
        # Assert
        self.assertEqual(12, self.SUT.highest_turns)

    def test_apply__with_existing__lowest_of_turns_not_advanced(self):
        # Arrange
        self.SUT.lowest_turns = 5
        self.game.stat.turns = 10
        # Act
        self.SUT.apply(self.game)
        # Assert
        self.assertEqual(5, self.SUT.lowest_turns)

    def test_apply__both_fatal__ties_increased(self):
        # Arrange
        self.game.agentPlayer.fatal_received = True
        self.game.opponent.fatal_received = True
        # Act
        self.SUT.apply(self.game)
        # Assert
        self.assertEqual(1, self.SUT.ties)

    def test_apply__agent_fatal__loss_increased(self):
        # Arrange
        self.game.agentPlayer.fatal_received = True
        # Act
        self.SUT.apply(self.game)
        # Assert
        self.assertEqual(1, self.SUT.fatal_loss)

    def test_apply__opponent_fatal__win_increased(self):
        # Arrange
        self.game.opponent.fatal_received = True
        # Act
        self.SUT.apply(self.game)
        # Assert
        self.assertEqual(1, self.SUT.fatal_wins)

    def test_apply__both_no_energy__tie_increased(self):
        # Arrange
        self.game.opponent.energy = 0
        self.game.agentPlayer.energy = 0
        self.SUT.ties = 3
        # Act
        self.SUT.apply(self.game)
        # Assert
        self.assertEqual(4, self.SUT.ties)

    def test_apply__no_energy__loss_increased(self):
        # Arrange
        self.game.agentPlayer.energy = 0
        # Act
        self.SUT.apply(self.game)
        # Assert
        self.assertEqual(1, self.SUT.energy_loss)

    def test_apply__opponent_no_energy__loss_increased(self):
        # Arrange
        self.game.opponent.energy = 0
        # Act
        self.SUT.apply(self.game)
        # Assert
        self.assertEqual(1, self.SUT.energy_wins)

    def test_apply__combat_num_totals_increased(self):
        # Arrange
        self.game.stat.num_attacks = 10
        self.game.stat.num_defends = 5
        self.game.stat.num_deploys = 7
        # Act
        self.SUT.apply(self.game)
        # Assert
        self.assertEqual(10, self.SUT.total_num_attacks)
        self.assertEqual(5, self.SUT.total_num_defends)
        self.assertEqual(7, self.SUT.total_num_deploys)

    def test_apply__combat_rolls_increased(self):
        # Arrange
        self.game.stat.total_attack_roll = 100
        self.game.stat.total_defend_roll = 50
        self.game.stat.total_deploy_roll = 75
        # Act
        self.SUT.apply(self.game)
        # Assert
        self.assertEqual(100, self.SUT.total_attack_roll)
        self.assertEqual(50, self.SUT.total_defend_roll)
        self.assertEqual(75, self.SUT.total_deploy_roll)

    def test_apply__num_cards_draw_increased(self):
        # Arrange
        self.game.agentPlayer.extend_to_draw([
            Card.D12_PROFESSIONAL_STABOTAGE,
            Card.D10_INNER_PIERCE,
            Card.D8_UNDERCOVER_CHOP,
            Card.D6_SLIT_TIGHT,
            Card.D4_SCARED_OUT_OF_YOUR_WHITTLES
        ])
        self.game.opponent.extend_to_draw([
            Card.D12_PROFESSIONAL_STABOTAGE,
            Card.D10_INNER_PIERCE,
            Card.D8_UNDERCOVER_CHOP,
            Card.D6_SLIT_TIGHT,
            Card.D4_SCARED_OUT_OF_YOUR_WHITTLES,
            CardWound.WOUND_ACUTE,
            CardWound.WOUND_DIRE
        ])
        # Act
        self.SUT.apply(self.game)
        # Assert
        self.assertEqual(5, self.SUT.total_draw_deck_size_agent)
        self.assertEqual(7, self.SUT.total_draw_deck_size_opponent)

    def test_apply__energy_loss_accumulated(self):
        # Arrange
        self.game.agentPlayer.energy = 12
        self.game.opponent.energy = 8
        expected_agent_value = 8
        expected_opponent_value = 12
        # Act
        self.SUT.apply(self.game)
        # Assert
        self.assertEqual(expected_agent_value, self.SUT.total_agent_energy_lost)
        self.assertEqual(expected_opponent_value, self.SUT.total_opponent_energy_lost)

    def test_apply__highest_combat_rolls_increased(self):
        # Arrange
        self.game.stat.highest_attack_roll = 100
        self.game.stat.highest_defend_roll = 90
        self.game.stat.highest_deploy_roll = 80
        # Act
        self.SUT.apply(self.game)
        # Assert
        self.assertEqual(100, self.SUT.highest_attack_roll)
        self.assertEqual(90, self.SUT.highest_defend_roll)
        self.assertEqual(80, self.SUT.highest_deploy_roll)

    def test_apply__lowest_combat_rolls_increased(self):
        # Arrange
        self.game.stat.lowest_attack_roll = 100
        self.game.stat.lowest_defend_roll = 90
        self.game.stat.lowest_deploy_roll = 80
        # Act
        self.SUT.apply(self.game)
        # Assert
        self.assertEqual(100, self.SUT.lowest_attack_roll)
        self.assertEqual(90, self.SUT.lowest_defend_roll)
        self.assertEqual(80, self.SUT.lowest_deploy_roll)
