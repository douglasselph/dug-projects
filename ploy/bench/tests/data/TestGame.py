import unittest
from unittest.mock import Mock

from src.data.Game import Game
from src.data.Card import CardWound, Card, card_ordinal
from src.data.Decision import DecisionLine, DecisionIntention


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
        self.SUT.agentPlayer.plate.add_card(Card.MANEUVER_BUST_A_CUT, DecisionLine.LINE_2, DecisionIntention.ATTACK)
        # Act
        intention = self.SUT.agent_line_intention_id(DecisionLine.LINE_2)
        # Assert
        self.assertEqual(DecisionIntention.ATTACK, intention)

    def test_agent_line_card_values_returns_expected_values(self):
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

    def test_opponent_energy_returns_as_expected(self):
        # Arrange
        self.SUT.opponent.energy = 12
        # Act
        value = self.SUT.opponent_energy
        # Assert
        self.assertEqual(12, value)

    def test_opponent_pips_returns_as_expected(self):
        # Arrange
        self.SUT.opponent.pips = 6
        # Act
        value = self.SUT.opponent_pips
        # Assert
        self.assertEqual(6, value)

    def test_opponent_lines_num_cards(self):
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

    def test_nn_common_draw_deck_face_up_cards_less_than_expected_returns_size_with_zeros(self):
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

    def test_nn_common_draw_deck_face_up_cards_more_than_expected_returns_clipped(self):
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

    def test_is_legal_for_wrong_intention_returns_false(self):
        # Arrange
        line = DecisionLine.LINE_4
        intention = DecisionIntention.ATTACK
        self.SUT.agentPlayer.play_to_plate(line, DecisionIntention.DEFEND)
        # Act
        is_legal = self.SUT.is_legal_on_agent_plate(line, intention)
        # Assert
        self.assertFalse(is_legal)

    def test_is_legal_for_correct_intention_returns_true(self):
        # Arrange
        line = DecisionLine.LINE_1
        intention = DecisionIntention.ATTACK
        self.SUT.agentPlayer.play_to_plate(line, DecisionIntention.ATTACK)
        # Act
        is_legal = self.SUT.is_legal_on_agent_plate(line, intention)
        # Assert
        self.assertTrue(is_legal)

    def test_is_legal_for_no_intention_returns_true(self):
        # Arrange
        line = DecisionLine.LINE_1
        intention = DecisionIntention.ATTACK
        # Act
        is_legal = self.SUT.is_legal_on_agent_plate(line, intention)
        # Assert
        self.assertTrue(is_legal)

    def test_common_cards_face_up_returns_expected_positive_size(self):
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

    def test_common_cards_draw_returns_expected_positive_size(self):
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

    def test_common_pull_face_up_card_returns_expected_positive_size(self):
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

    def test_compute_reward_both_fatal_returns_zero(self):
        # Arrange
        self.SUT.agentPlayer.fatal_received = True
        self.SUT.opponent.fatal_received = True
        # Act
        value = self.SUT.compute_reward
        # Assert
        self.assertEqual(0, value)

    def test_compute_reward_agent_fatal_returns_negative_100(self):
        # Arrange
        self.SUT.agentPlayer.fatal_received = True
        # Act
        value = self.SUT.compute_reward
        # Assert
        self.assertEqual(-100, value)

    def test_compute_reward_agent_energy_zero_opponent_fatal_returns_90(self):
        # Arrange
        self.SUT.agentPlayer.fatal_received = True
        # Act
        value = self.SUT.compute_reward
        # Assert
        self.assertEqual(-100, value)