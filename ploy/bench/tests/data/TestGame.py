import unittest
from unittest.mock import Mock

from src.data.Game import Game
from src.data.Card import CardWound, Card, card_ordinal
from src.data.Decision import DecisionLine, DecisionIntention
from src.data.RewardConstants import RewardConstants
from src.data.Stats import StatsAll


class TestGame(unittest.TestCase):

    def setUp(self):
        self.SUT = Game()

    def test_nn_next_cards__calls_player_next_cards(self):
        # Arrange
        player_mock = Mock()
        self.SUT.agentPlayer = player_mock
        # Act
        self.SUT.nn_next_cards(8)
        # Assert
        player_mock.nn_next_cards.assert_called_once_with(8)

    def test_agent_energy__returns_agent_energy(self):
        # Arrange
        self.SUT.agentPlayer.energy = 10
        # Act
        energy = self.SUT.agent_energy
        # Assert
        self.assertEqual(10, energy)

    def test_agent_pips__returns_agent_pips(self):
        # Arrange
        self.SUT.agentPlayer.pips = 12
        # Act
        pips = self.SUT.agent_pips
        # Assert
        self.assertEqual(12, pips)

    def test_agent_stash_cards_total__makes_expected_call(self):
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

    def test_agent_line_intention_id__returns_intention_on_indicated_line(self):
        # Arrange
        self.SUT.agentPlayer.plate.add_card(Card.MANEUVER_BUST_A_CUT, DecisionLine.LINE_2, DecisionIntention.ATTACK)
        # Act
        intention = self.SUT.agent_line_intention_id(DecisionLine.LINE_2)
        # Assert
        self.assertEqual(DecisionIntention.ATTACK, intention)

    def test_agent_line_card_values__returns_expected_values(self):
        # Arrange
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_3, Card.D4_SCARED_OUT_OF_YOUR_WHITTLES)
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_2, Card.D8_UNDERCOVER_CHOP)
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_3, Card.D12_PROFESSIONAL_STABOTAGE)
        # Act
        values = self.SUT.agent_line_card_values(DecisionLine.LINE_3)
        # Assert
        self.assertEqual(2, len(values))
        self.assertEqual(card_ordinal(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES), values[0])
        self.assertEqual(card_ordinal(Card.D12_PROFESSIONAL_STABOTAGE), values[1])

    def test_opponent_energy__returns_as_expected(self):
        # Arrange
        self.SUT.opponent.energy = 12
        # Act
        value = self.SUT.opponent_energy
        # Assert
        self.assertEqual(12, value)

    def test_opponent_pips__returns_as_expected(self):
        # Arrange
        self.SUT.opponent.pips = 6
        # Act
        value = self.SUT.opponent_pips
        # Assert
        self.assertEqual(6, value)

    def test_opponent_lines_num_cards__expected_cards(self):
        # Arrange
        self.SUT.opponent.plate.add_card(Card.D12_PROFESSIONAL_STABOTAGE, DecisionLine.LINE_2, DecisionIntention.DEFEND)
        self.SUT.opponent.plate.add_card(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES, DecisionLine.LINE_3,
                                         DecisionIntention.ATTACK)
        self.SUT.opponent.plate.add_card(Card.MANEUVER_BUST_A_CUT, DecisionLine.LINE_1, DecisionIntention.DEPLOY)
        self.SUT.opponent.plate.add_card(Card.D6_SLIT_TIGHT, DecisionLine.LINE_4, DecisionIntention.DEFEND)
        self.SUT.opponent.plate.add_card(Card.MANEUVER_IN_HEW_OF, DecisionLine.LINE_1, DecisionIntention.NONE)
        self.SUT.opponent.plate.add_card(Card.D8_UNDERCOVER_CHOP, DecisionLine.LINE_2, DecisionIntention.NONE)
        # Act
        value = self.SUT.opponent_lines_num_cards
        # Assert
        self.assertEqual([2, 2, 1, 1], value)

    def test_nn_common_draw_deck_face_up_cards__less_than_expected__returns_size_with_zeros(self):
        # Arrange
        self.SUT.commonDrawDeck.append(Card.MANEUVER_BUST_A_CUT)
        self.SUT.commonDrawDeck.append(Card.D12_PROFESSIONAL_STABOTAGE)
        self.SUT.commonDrawDeck.append(CardWound.WOUND_ACUTE)
        self.SUT.commonDrawDeck.append(Card.D20_CUTASTROPHE)
        self.SUT.commonDrawDeck.append(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES)
        self.SUT.commonDrawDeck.append(Card.D12_PROFESSIONAL_STABOTAGE)
        self.SUT.commonDrawDeck.draw(2)
        # Act
        value = self.SUT.nn_common_draw_deck_face_up_cards(4)
        # Assert
        self.assertEqual(4, len(value))
        self.assertEqual(card_ordinal(Card.MANEUVER_BUST_A_CUT), value[0])
        self.assertEqual(card_ordinal(Card.D12_PROFESSIONAL_STABOTAGE), value[1])
        self.assertEqual(0, value[2])
        self.assertEqual(0, value[3])

    def test_nn_common_draw_deck_face_up_cards_more_than_expected__returns_clipped(self):
        # Arrange
        self.SUT.commonDrawDeck.append(Card.MANEUVER_BUST_A_CUT)
        self.SUT.commonDrawDeck.append(Card.D12_PROFESSIONAL_STABOTAGE)
        self.SUT.commonDrawDeck.append(CardWound.WOUND_ACUTE)
        self.SUT.commonDrawDeck.append(Card.D20_CUTASTROPHE)
        self.SUT.commonDrawDeck.append(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES)
        self.SUT.commonDrawDeck.append(Card.D12_PROFESSIONAL_STABOTAGE)
        self.SUT.commonDrawDeck.draw(6)
        # Act
        value = self.SUT.nn_common_draw_deck_face_up_cards(4)
        # Assert
        self.assertEqual(4, len(value))
        self.assertEqual(card_ordinal(Card.MANEUVER_BUST_A_CUT), value[0])
        self.assertEqual(card_ordinal(Card.D12_PROFESSIONAL_STABOTAGE), value[1])
        self.assertEqual(CardWound.WOUND_ACUTE, value[2])
        self.assertEqual(Card.D20_CUTASTROPHE, value[3])

    def test_is_legal__for_wrong_intention__returns_false(self):
        # Arrange
        line = DecisionLine.LINE_4
        intention = DecisionIntention.ATTACK
        self.SUT.agentPlayer.play_to_plate(line, DecisionIntention.DEFEND)
        # Act
        is_legal = self.SUT.is_legal_on_agent_plate(line, intention)
        # Assert
        self.assertFalse(is_legal)

    def test_is_legal__for_correct_intention__returns_true(self):
        # Arrange
        line = DecisionLine.LINE_1
        intention = DecisionIntention.ATTACK
        self.SUT.agentPlayer.play_to_plate(line, DecisionIntention.ATTACK)
        # Act
        is_legal = self.SUT.is_legal_on_agent_plate(line, intention)
        # Assert
        self.assertTrue(is_legal)

    def test_is_legal__for_no_intention__returns_true(self):
        # Arrange
        line = DecisionLine.LINE_1
        intention = DecisionIntention.ATTACK
        # Act
        is_legal = self.SUT.is_legal_on_agent_plate(line, intention)
        # Assert
        self.assertTrue(is_legal)

    def test_common_cards_face_up__returns_expected_positive_size(self):
        # Arrange
        self.SUT.commonDrawDeck.append(Card.MANEUVER_IN_HEW_OF)
        self.SUT.commonDrawDeck.append(Card.D20_CUTASTROPHE)
        self.SUT.commonDrawDeck.append(Card.D12_PROFESSIONAL_STABOTAGE)
        self.SUT.commonDrawDeck.append(Card.D6_D12_EXECUTIVE_INCISION)
        self.SUT.commonDrawDeck.draw(2)
        # Act
        cards = self.SUT.common_cards_face_up
        # Assert
        self.assertEqual(2, len(cards))
        self.assertEqual(Card.MANEUVER_IN_HEW_OF, cards[0])
        self.assertEqual(Card.D20_CUTASTROPHE, cards[1])

    def test_common_cards_draw__returns_expected_positive_size(self):
        # Arrange
        self.SUT.commonDrawDeck.append(Card.MANEUVER_IN_HEW_OF)
        self.SUT.commonDrawDeck.append(Card.D20_CUTASTROPHE)
        self.SUT.commonDrawDeck.append(Card.D12_PROFESSIONAL_STABOTAGE)
        self.SUT.commonDrawDeck.append(Card.D6_D12_EXECUTIVE_INCISION)
        self.SUT.commonDrawDeck.draw(2)
        # Act
        cards = self.SUT.common_cards_draw
        # Assert
        self.assertEqual(2, len(cards))
        self.assertEqual(Card.D12_PROFESSIONAL_STABOTAGE, cards[0])
        self.assertEqual(Card.D6_D12_EXECUTIVE_INCISION, cards[1])

    def test_common_pull_face_up_card__returns_expected_positive_size(self):
        # Arrange
        self.SUT.commonDrawDeck.append(Card.MANEUVER_IN_HEW_OF)
        self.SUT.commonDrawDeck.append(Card.D20_CUTASTROPHE)
        self.SUT.commonDrawDeck.append(Card.D12_PROFESSIONAL_STABOTAGE)
        self.SUT.commonDrawDeck.append(Card.D6_D12_EXECUTIVE_INCISION)
        self.SUT.commonDrawDeck.draw(2)
        # Act
        card = self.SUT.common_pull_face_up_card
        # Assert
        self.assertEqual(Card.D20_CUTASTROPHE, card)

    def test_compute_base_reward__both_fatal__returns_zero(self):
        # Arrange
        self.SUT.agentPlayer.fatal_received = True
        self.SUT.opponent.fatal_received = True
        # Act
        value = self.SUT.compute_base_reward
        # Assert
        self.assertEqual(0, value)

    def test_compute_base_reward__agent_fatal__returns_penalty_loss(self):
        # Arrange
        self.SUT.agentPlayer.fatal_received = True
        # Act
        value = self.SUT.compute_base_reward
        # Assert
        self.assertEqual(RewardConstants.BASE_PENALTY_LOSS, value)

    def test_compute_base_reward__agent_energy_zero_opponent_fatal__returns_exhausted_win(self):
        # Arrange
        self.SUT.opponent.fatal_received = True
        self.SUT.agentPlayer.energy = 0
        expected_value = RewardConstants.BASE_REWARD_WIN - RewardConstants.EXHAUSTED_WIN_PENALTY
        # Act
        value = self.SUT.compute_base_reward
        # Assert
        self.assertEqual(expected_value, value)

    def test_compute_base_reward__agent_energy_zero__returns_penalty_loss(self):
        # Arrange
        self.SUT.agentPlayer.energy = 0
        # Act
        value = self.SUT.compute_base_reward
        # Assert
        self.assertEqual(RewardConstants.BASE_PENALTY_LOSS, value)

    def test_compute_base_reward__opponent_fatal__returns_reward_win(self):
        # Arrange
        self.SUT.opponent.fatal_received = True
        # Act
        value = self.SUT.compute_base_reward
        # Assert
        self.assertEqual(RewardConstants.BASE_REWARD_WIN, value)

    def test_compute_base_reward__opponent_energy_zero__returns_reward_win(self):
        # Arrange
        self.SUT.opponent.energy = 0
        # Act
        value = self.SUT.compute_base_reward
        # Assert
        self.assertEqual(RewardConstants.BASE_REWARD_WIN, value)

    def test_compute_wound_reward__opponent_has_one_of_each_wound__returns_expected_value(self):
        # Arrange
        self.SUT.opponent.append_to_draw(CardWound.WOUND_MINOR)
        self.SUT.opponent.append_to_draw(CardWound.WOUND_ACUTE)
        self.SUT.opponent.append_to_draw(CardWound.WOUND_GRAVE)
        self.SUT.opponent.append_to_draw(CardWound.WOUND_DIRE)
        self.SUT.opponent.draw_hand()
        expected_value = RewardConstants.WOUND_PENALTY_MINOR + \
                         RewardConstants.WOUND_PENALTY_ACUTE + \
                         RewardConstants.WOUND_PENALTY_GRAVE + \
                         RewardConstants.WOUND_PENALTY_DIRE
        # Act
        value = self.SUT.compute_wound_reward
        # Assert
        self.assertEqual(expected_value, value)

    def test_compute_wound_reward__both_has_wounds__returns_difference(self):
        # Arrange
        self.SUT.opponent.append_to_draw(CardWound.WOUND_MINOR)
        self.SUT.opponent.append_to_draw(CardWound.WOUND_ACUTE)
        self.SUT.opponent.append_to_draw(CardWound.WOUND_GRAVE)
        self.SUT.opponent.append_to_draw(CardWound.WOUND_DIRE)
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_GRAVE)
        expected_value = RewardConstants.WOUND_PENALTY_MINOR + \
                         RewardConstants.WOUND_PENALTY_ACUTE + \
                         RewardConstants.WOUND_PENALTY_DIRE
        # Act
        value = self.SUT.compute_wound_reward
        # Assert
        self.assertEqual(expected_value, value)

    def test_compute_energy_penalty__not_at_threshold__returns_0(self):
        # Arrange
        # Act
        value = self.SUT.compute_energy_penalty
        # Assert
        self.assertEqual(0, value)

    def test_compute_energy_penalty__at_threshold__returns_0(self):
        # Arrange
        self.SUT.agentPlayer.energy = RewardConstants.ENERGY_PENALTY_THRESHOLD
        # Act
        value = self.SUT.compute_energy_penalty
        # Assert
        self.assertEqual(0, value)

    def test_compute_energy_penalty__below_threshold__returns_diff(self):
        # Arrange
        expected_value = 2
        self.SUT.agentPlayer.energy = RewardConstants.ENERGY_PENALTY_THRESHOLD - expected_value
        # Act
        value = self.SUT.compute_energy_penalty
        # Assert
        self.assertEqual(expected_value, value)

    def test_compute_reward__simple_quick_win__expected_formula_applied(self):
        # Arrange
        turns = 20
        self.SUT.stat.turns = turns
        self.SUT.opponent.fatal_received = True
        expected_value = RewardConstants.BASE_REWARD_WIN - RewardConstants.TURNS_PENALTY_SCALE * turns
        # Act
        value = self.SUT.compute_reward
        # Assert
        self.assertEqual(expected_value, value)

    def test_compute_reward__win_with_wounds__expected_formula_applied(self):
        # Arrange
        turns = 30
        self.SUT.stat.turns = turns
        self.SUT.opponent.fatal_received = True
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_DIRE)
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_MINOR)
        expected_value = RewardConstants.BASE_REWARD_WIN - \
                         RewardConstants.TURNS_PENALTY_SCALE * turns - \
                         self.SUT.compute_wound_reward * RewardConstants.WOUND_PENALTY_SCALE
        # Act
        value = self.SUT.compute_reward
        # Assert
        self.assertEqual(expected_value, value)

    def test_compute_reward__win_with_energy_penalty__expected_formula_applied(self):
        # Arrange
        turns = 50
        self.SUT.stat.turns = turns
        self.SUT.opponent.energy = 0
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_DIRE)
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_ACUTE)
        self.SUT.agentPlayer.energy = 2
        expected_value = RewardConstants.BASE_REWARD_WIN - \
                         RewardConstants.TURNS_PENALTY_SCALE * turns - \
                         self.SUT.compute_wound_reward * RewardConstants.WOUND_PENALTY_SCALE - \
                         self.SUT.compute_energy_penalty * RewardConstants.ENERGY_PENALTY_SCALE
        # Act
        value = self.SUT.compute_reward
        # Assert
        self.assertEqual(expected_value, value)

    def test_apply_to_all_stats__number_of_games_advanced(self):
        # Arrange
        all_stats = StatsAll()
        # Act
        self.SUT.apply_to_all_stats(all_stats)
        # Assert
        self.assertEqual(1, all_stats.games)

    def test_apply_to_all_stats__highest_of_turns_advanced(self):
        # Arrange
        all_stats = StatsAll()
        self.SUT.stat.turns = 10
        # Act
        self.SUT.apply_to_all_stats(all_stats)
        # Assert
        self.assertEqual(10, all_stats.highest_turns)

    def test_apply_to_all_stats__lowest_of_turns_advanced(self):
        # Arrange
        all_stats = StatsAll()
        self.SUT.stat.turns = 10
        # Act
        self.SUT.apply_to_all_stats(all_stats)
        # Assert
        self.assertEqual(10, all_stats.lowest_turns)

    def test_apply_to_all_stats__with_existing__highest_of_turns_not_advanced(self):
        # Arrange
        all_stats = StatsAll()
        all_stats.highest_turns = 12
        self.SUT.stat.turns = 10
        # Act
        self.SUT.apply_to_all_stats(all_stats)
        # Assert
        self.assertEqual(12, all_stats.highest_turns)

    def test_apply_to_all_stats__with_existing__lowest_of_turns_not_advanced(self):
        # Arrange
        all_stats = StatsAll()
        all_stats.lowest_turns = 5
        self.SUT.stat.turns = 10
        # Act
        self.SUT.apply_to_all_stats(all_stats)
        # Assert
        self.assertEqual(5, all_stats.lowest_turns)

    def test_apply_to_all_stats__both_fatal__ties_increased(self):
        # Arrange
        all_stats = StatsAll()
        self.SUT.agentPlayer.fatal_received = True
        self.SUT.opponent.fatal_received = True
        # Act
        self.SUT.apply_to_all_stats(all_stats)
        # Assert
        self.assertEqual(1, all_stats.ties)

    def test_apply_to_all_stats__agent_fatal__loss_increased(self):
        # Arrange
        all_stats = StatsAll()
        self.SUT.agentPlayer.fatal_received = True
        # Act
        self.SUT.apply_to_all_stats(all_stats)
        # Assert
        self.assertEqual(1, all_stats.fatal_loss)

    def test_apply_to_all_stats__opponent_fatal__win_increased(self):
        # Arrange
        all_stats = StatsAll()
        self.SUT.opponent.fatal_received = True
        # Act
        self.SUT.apply_to_all_stats(all_stats)
        # Assert
        self.assertEqual(1, all_stats.fatal_wins)

    def test_apply_to_all_stats__both_no_energy__tie_increased(self):
        # Arrange
        all_stats = StatsAll()
        self.SUT.opponent.energy = 0
        self.SUT.agentPlayer.energy = 0
        all_stats.ties = 3
        # Act
        self.SUT.apply_to_all_stats(all_stats)
        # Assert
        self.assertEqual(4, all_stats.ties)

    def test_apply_to_all_stats__no_energy__loss_increased(self):
        # Arrange
        all_stats = StatsAll()
        self.SUT.agentPlayer.energy = 0
        # Act
        self.SUT.apply_to_all_stats(all_stats)
        # Assert
        self.assertEqual(1, all_stats.energy_loss)

    def test_apply_to_all_stats__opponent_no_energy__loss_increased(self):
        # Arrange
        all_stats = StatsAll()
        self.SUT.opponent.energy = 0
        # Act
        self.SUT.apply_to_all_stats(all_stats)
        # Assert
        self.assertEqual(1, all_stats.energy_wins)

    def test_apply_to_all_stats__combat_num_totals_increased(self):
        # Arrange
        all_stats = StatsAll()
        self.SUT.stat.num_attacks = 10
        self.SUT.stat.num_defends = 5
        self.SUT.stat.num_deploys = 7
        # Act
        self.SUT.apply_to_all_stats(all_stats)
        # Assert
        self.assertEqual(10, all_stats.total_num_attacks)
        self.assertEqual(5, all_stats.total_num_defends)
        self.assertEqual(7, all_stats.total_num_deploys)

    def test_apply_to_all_stats__combat_rolls_increased(self):
        # Arrange
        all_stats = StatsAll()
        self.SUT.stat.total_attack_roll = 100
        self.SUT.stat.total_defend_roll = 50
        self.SUT.stat.total_deploy_roll = 75
        # Act
        self.SUT.apply_to_all_stats(all_stats)
        # Assert
        self.assertEqual(100, all_stats.total_attack_roll)
        self.assertEqual(50, all_stats.total_defend_roll)
        self.assertEqual(75, all_stats.total_deploy_roll)

    def test_apply_to_all_stats__num_cards_draw_increased(self):
        # Arrange
        all_stats = StatsAll()
        self.SUT.agentPlayer.extend_to_draw([
            Card.D12_PROFESSIONAL_STABOTAGE,
            Card.D10_INNER_PIERCE,
            Card.D8_UNDERCOVER_CHOP,
            Card.D6_SLIT_TIGHT,
            Card.D4_SCARED_OUT_OF_YOUR_WHITTLES
        ])
        self.SUT.opponent.extend_to_draw([
            Card.D12_PROFESSIONAL_STABOTAGE,
            Card.D10_INNER_PIERCE,
            Card.D8_UNDERCOVER_CHOP,
            Card.D6_SLIT_TIGHT,
            Card.D4_SCARED_OUT_OF_YOUR_WHITTLES,
            CardWound.WOUND_ACUTE,
            CardWound.WOUND_DIRE
        ])
        # Act
        self.SUT.apply_to_all_stats(all_stats)
        # Assert
        self.assertEqual(5, all_stats.total_draw_deck_size_agent)
        self.assertEqual(7, all_stats.total_draw_deck_size_opponent)

    def test_apply_to_all_stats__energy_loss_accumulated(self):
        # Arrange
        all_stats = StatsAll()
        self.SUT.agentPlayer.energy = 12
        self.SUT.opponent.energy = 8
        expected_agent_value = 8
        expected_opponent_value = 12
        # Act
        self.SUT.apply_to_all_stats(all_stats)
        # Assert
        self.assertEqual(expected_agent_value, all_stats.total_agent_energy_lost)
        self.assertEqual(expected_opponent_value, all_stats.total_opponent_energy_lost)

    def test_apply_to_all_stats__highest_combat_rolls_increased(self):
        # Arrange
        all_stats = StatsAll()
        self.SUT.stat.highest_attack_roll = 100
        self.SUT.stat.highest_defend_roll = 90
        self.SUT.stat.highest_deploy_roll = 80
        # Act
        self.SUT.apply_to_all_stats(all_stats)
        # Assert
        self.assertEqual(100, all_stats.highest_attack_roll)
        self.assertEqual(90, all_stats.highest_defend_roll)
        self.assertEqual(80, all_stats.highest_deploy_roll)

    def test_apply_to_all_stats__lowest_combat_rolls_increased(self):
        # Arrange
        all_stats = StatsAll()
        self.SUT.stat.lowest_attack_roll = 100
        self.SUT.stat.lowest_defend_roll = 90
        self.SUT.stat.lowest_deploy_roll = 80
        # Act
        self.SUT.apply_to_all_stats(all_stats)
        # Assert
        self.assertEqual(100, all_stats.lowest_attack_roll)
        self.assertEqual(90, all_stats.lowest_defend_roll)
        self.assertEqual(80, all_stats.lowest_deploy_roll)