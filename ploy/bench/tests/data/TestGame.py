import unittest
from unittest.mock import Mock

from src.data.Card import CardWound, Card, card_ordinal, card_regular
from src.data.Decision import DecisionLine, DecisionIntention
from src.data.Game import Game
from src.data.Player import Player
from src.data.RewardConstants import RewardConstants


class TestGame(unittest.TestCase):

    def setUp(self):
        self.SUT = Game()

    def test_nn_next_cards__calls_player_next_cards(self):
        # Arrange
        player_mock = Mock(spec=Player)
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

    def test_agent_line_card_values__returns_ord_of_cards_with_zeros(self):
        # Arrange
        self.SUT.agentPlayer.append_to_draw(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES)
        self.SUT.agentPlayer.append_to_draw(Card.D8_UNDERCOVER_CHOP)
        self.SUT.agentPlayer.append_to_draw(Card.D12_PROFESSIONAL_STABOTAGE)
        self.SUT.agentPlayer.draw_hand()
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_2, DecisionIntention.DEFEND)
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_3, DecisionIntention.NONE)
        expected_values = [
            card_ordinal(Card.D12_PROFESSIONAL_STABOTAGE),
            card_ordinal(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES),
            0
        ]
        # Act
        values = self.SUT.agent_line_card_values(DecisionLine.LINE_3)
        # Assert
        self.assertEqual(len(expected_values), len(values))
        self.assertEqual(expected_values, values)

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
        expected_cards = [
            card_ordinal(Card.D12_PROFESSIONAL_STABOTAGE),
            card_ordinal(Card.MANEUVER_BUST_A_CUT),
            0,
            0,
        ]
        # Act
        values = self.SUT.nn_common_draw_deck_face_up_cards(4)
        # Assert
        self.assertEqual(4, len(values))
        self.assertEqual(expected_cards, values)

    def test_nn_common_draw_deck_face_up_cards_more_than_expected__returns_clipped(self):
        # Arrange
        cards = [
            Card.MANEUVER_BUST_A_CUT,
            Card.D12_PROFESSIONAL_STABOTAGE,
            CardWound.WOUND_ACUTE,
            Card.D20_CUTASTROPHE,
            Card.D4_SCARED_OUT_OF_YOUR_WHITTLES,
            Card.D12_PROFESSIONAL_STABOTAGE
        ]
        for card in cards:
            self.SUT.commonDrawDeck.append(card)
        self.SUT.commonDrawDeck.draw(len(cards))
        ord_values = []
        for card in reversed(cards):
            ord_values.append(card_ordinal(card))
        expected_cards = ord_values[:4]
        # Act
        values = self.SUT.nn_common_draw_deck_face_up_cards(4)
        # Assert
        self.assertEqual(4, len(values))
        self.assertEqual(expected_cards, values)

    def test_is_legal_intention_on_agent_plate__for_wrong_intention__returns_false(self):
        # Arrange
        line = DecisionLine.LINE_4
        intention = DecisionIntention.ATTACK
        self.SUT.agentPlayer.play_to_plate(line, DecisionIntention.DEFEND)
        # Act
        is_legal = self.SUT.is_legal_intention_on_agent_plate(line, intention)
        # Assert
        self.assertFalse(is_legal)

    def test_is_legal_intention_on_agent_plate__for_correct_intention__returns_true(self):
        # Arrange
        line = DecisionLine.LINE_1
        intention = DecisionIntention.ATTACK
        self.SUT.agentPlayer.play_to_plate(line, DecisionIntention.ATTACK)
        # Act
        is_legal = self.SUT.is_legal_intention_on_agent_plate(line, intention)
        # Assert
        self.assertTrue(is_legal)

    def test_is_legal_intention_on_agent_plate__for_no_intention__returns_true(self):
        # Arrange
        line = DecisionLine.LINE_1
        intention = DecisionIntention.ATTACK
        # Act
        is_legal = self.SUT.is_legal_intention_on_agent_plate(line, intention)
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
        self.assertEqual(Card.D20_CUTASTROPHE, cards[0])
        self.assertEqual(Card.MANEUVER_IN_HEW_OF, cards[1])

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
        expected_card = Card.D20_CUTASTROPHE
        # Act
        card = self.SUT.common_pull_face_up_card()
        # Assert
        self.assertEqual(expected_card, card_regular(card))

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

    def test_cleanup__has_some_face_up_matching_wounds__wounds_upgraded(self):
        # Arrange
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_MINOR)
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_MINOR)
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_GRAVE)
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_GRAVE)
        self.SUT.agentPlayer.draw_hand()
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.agentPlayer.plate.set_line_face_up(DecisionLine.LINE_3)
        self.SUT.agentPlayer.plate.set_line_face_up(DecisionLine.LINE_1)
        expected_cards = [
            CardWound.WOUND_MINOR,
            CardWound.WOUND_MINOR,
            CardWound.WOUND_DIRE
        ]
        # Act
        self.SUT.cleanup()
        # Assert
        cards = self.SUT.agentPlayer.draw_hand()
        self.assertEqual(expected_cards, cards)

    def test_cleanup__has_lots_face_up_matching_wounds__wounds_upgraded(self):
        # Arrange
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_MINOR)
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_MINOR)
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_ACUTE)
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_ACUTE)
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_ACUTE)
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_ACUTE)
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_ACUTE)
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_DIRE)
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_DIRE)
        self.SUT.agentPlayer.draw_hand()
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.agentPlayer.draw_hand()
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.agentPlayer.draw_hand()
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_2, DecisionIntention.ATTACK)
        self.SUT.agentPlayer.plate.set_line_face_up(DecisionLine.LINE_3)
        self.SUT.agentPlayer.plate.set_line_face_up(DecisionLine.LINE_1)
        expected_cards = [
            CardWound.WOUND_MINOR,
            CardWound.WOUND_MINOR,
            CardWound.WOUND_ACUTE
        ]
        # Act
        self.SUT.cleanup()
        # Assert
        cards = self.SUT.agentPlayer.draw_hand()
        self.assertEqual(expected_cards, cards)
        self.assertTrue(self.SUT.agentPlayer.fatal_received)

    def test_cleanup__has_wounds_chained__wounds_upgraded_via_chaining(self):
        # Arrange
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_MINOR)
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_MINOR)
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_ACUTE)
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_ACUTE)
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_GRAVE)
        self.SUT.agentPlayer.draw_hand()
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.agentPlayer.draw_hand()
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.agentPlayer.plate.set_line_face_up(DecisionLine.LINE_1)
        expected_cards = [
            CardWound.WOUND_MINOR,
            CardWound.WOUND_MINOR,
            CardWound.WOUND_DIRE
        ]
        # Act
        self.SUT.cleanup()
        # Assert
        cards = self.SUT.agentPlayer.draw_hand()
        self.assertEqual(expected_cards, cards)

    def test_cleanup__matching_wounds_but_not_face_up__nothing_done(self):
        # Arrange
        self.SUT.opponent.append_to_draw(CardWound.WOUND_MINOR)
        self.SUT.opponent.append_to_draw(CardWound.WOUND_MINOR)
        self.SUT.opponent.append_to_draw(CardWound.WOUND_GRAVE)
        self.SUT.opponent.append_to_draw(CardWound.WOUND_GRAVE)
        self.SUT.opponent.draw_hand()
        self.SUT.opponent.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.opponent.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.opponent.play_to_plate(DecisionLine.LINE_2, DecisionIntention.ATTACK)
        self.SUT.opponent.play_to_plate(DecisionLine.LINE_2, DecisionIntention.ATTACK)
        expected_cards = [
            CardWound.WOUND_MINOR,
            CardWound.WOUND_MINOR,
            CardWound.WOUND_GRAVE,
            CardWound.WOUND_GRAVE
        ]
        # Act
        self.SUT.cleanup()
        # Assert
        self.SUT.agentPlayer.draw_hand()
        self.assertEqual(2, len(self.SUT.opponent.plate.lines[DecisionLine.LINE_2.pos].cards))
        self.assertEqual(2, len(self.SUT.opponent.plate.lines[DecisionLine.LINE_3.pos].cards))

    def test_cleanup__has_matching_dire_wounds__upgraded_to_fatal(self):
        # Arrange
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_DIRE)
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_DIRE)
        self.SUT.agentPlayer.draw_hand()
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.agentPlayer.plate.set_line_face_up(DecisionLine.LINE_3)
        expected_cards = []
        # Act
        self.SUT.cleanup()
        # Assert
        self.assertTrue(self.SUT.agentPlayer.fatal_received)
        cards = self.SUT.agentPlayer.draw_hand()
        self.assertEqual(expected_cards, cards)

    def test_cleanup__has_chaining_wounds__chaining_upgraded_to_fatal(self):
        # Arrange
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_MINOR)
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_ACUTE)
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_ACUTE)
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_GRAVE)
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_DIRE)
        self.SUT.agentPlayer.draw_hand()
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_2, DecisionIntention.ATTACK)
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_2, DecisionIntention.ATTACK)
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_2, DecisionIntention.ATTACK)
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_2, DecisionIntention.ATTACK)
        self.SUT.agentPlayer.draw_hand()
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.agentPlayer.plate.set_line_face_up(DecisionLine.LINE_3)
        self.SUT.agentPlayer.plate.set_line_face_up(DecisionLine.LINE_2)
        expected_cards = [
            CardWound.WOUND_MINOR
        ]
        # Act
        self.SUT.cleanup()
        # Assert
        self.assertTrue(self.SUT.agentPlayer.fatal_received)
        cards = self.SUT.agentPlayer.draw_hand()
        self.assertEqual(expected_cards, cards)

    def test_cleanup__has_some_face_up_wounds__lose_energy_from_wounds(self):
        # Arrange
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_MINOR)
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_GRAVE)
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_DIRE)
        self.SUT.agentPlayer.append_to_draw(CardWound.WOUND_ACUTE)
        self.SUT.agentPlayer.draw_hand()
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.agentPlayer.play_to_plate(DecisionLine.LINE_2, DecisionIntention.ATTACK)
        self.SUT.agentPlayer.plate.set_line_face_up(DecisionLine.LINE_3)
        self.SUT.agentPlayer.plate.set_line_face_up(DecisionLine.LINE_1)
        expected_energy_loss = CardWound.WOUND_MINOR.energy_penalty + \
            CardWound.WOUND_GRAVE.energy_penalty + \
            CardWound.WOUND_DIRE.energy_penalty
        # Act
        self.SUT.cleanup()
        # Assert
        self.assertEqual(expected_energy_loss, self.SUT.agentPlayer.energy_loss)

    def test_cleanup__has_face_up_cards__face_up_cards_discarded(self):
        # Arrange
        self.SUT.opponent.append_to_draw(Card.D20_CUTASTROPHE)
        self.SUT.opponent.append_to_draw(Card.D12_PROFESSIONAL_STABOTAGE)
        self.SUT.opponent.append_to_draw(Card.D10_INNER_PIERCE)
        self.SUT.opponent.append_to_draw(Card.D8_UNDERCOVER_CHOP)
        self.SUT.opponent.draw_hand()
        self.SUT.opponent.play_to_plate(DecisionLine.LINE_1, DecisionIntention.DEPLOY)
        self.SUT.opponent.play_to_plate(DecisionLine.LINE_1, DecisionIntention.DEPLOY)
        self.SUT.opponent.play_to_plate(DecisionLine.LINE_1, DecisionIntention.DEPLOY)
        self.SUT.opponent.play_to_plate(DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.opponent.plate.set_line_face_up(DecisionLine.LINE_1)
        # Act
        self.SUT.cleanup()
        # Assert
        self.assertEqual([], self.SUT.opponent.plate.lines[DecisionLine.LINE_1.pos].cards)
        self.assertEqual([Card.D20_CUTASTROPHE], self.SUT.opponent.plate.lines[DecisionLine.LINE_2.pos].cards)

    def test_cleanup__nothing__endOfGame_false(self):
        # Arrange
        self.SUT.agentPlayer.fatal_received = True
        # Act
        self.SUT.cleanup()
        # Assert
        self.assertTrue(self.SUT.endOfGame)

    def test_cleanup__agentPlayer_fatal__endOfGame_true(self):
        # Arrange
        self.SUT.agentPlayer.fatal_received = True
        # Act
        self.SUT.cleanup()
        # Assert
        self.assertTrue(self.SUT.endOfGame)

    def test_cleanup__opponent_fatal__endOfGame_true(self):
        # Arrange
        self.SUT.opponent.fatal_received = True
        # Act
        self.SUT.cleanup()
        # Assert
        self.assertTrue(self.SUT.endOfGame)

    def test_cleanup__agentPlayer_energy_zero__endOfGame_true(self):
        # Arrange
        self.SUT.agentPlayer.energy = 0
        # Act
        self.SUT.cleanup()
        # Assert
        self.assertTrue(self.SUT.endOfGame)

    def test_cleanup__opponent_opponent_zero__endOfGame_true(self):
        # Arrange
        self.SUT.opponent.energy = 0
        # Act
        self.SUT.cleanup()
        # Assert
        self.assertTrue(self.SUT.endOfGame)

