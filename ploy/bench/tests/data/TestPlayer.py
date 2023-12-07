import unittest
from unittest.mock import Mock

from src.data.Player import Player
from src.data.Card import Card, CardWound, DieSides
from src.data.Decision import *


class TestPlayer(unittest.TestCase):

    def setUp(self):
        self.SUT = Player()

    def test_append_to_draw__makes_expected_call(self):
        # Arrange
        card = Card.D6_D12_EXECUTIVE_INCISION
        deck_mock = Mock()
        self.SUT.draw = deck_mock
        # Act
        self.SUT.append_to_draw(card)
        # Assert
        deck_mock.append.assert_called_once_with(card)

    def test_extend_to_draw__makes_expected_call(self):
        # Arrange
        cards = [Card.D6_D12_EXECUTIVE_INCISION, Card.D20_CUTASTROPHE]
        deck_mock = Mock()
        self.SUT.draw = deck_mock
        # Act
        self.SUT.extend_to_draw(cards)
        # Assert
        deck_mock.extend.assert_called_once_with(cards)

    def test_append_to_stash__makes_expected_call(self):
        # Arrange
        card = Card.D6_D12_EXECUTIVE_INCISION
        stash_mock = Mock()
        self.SUT.stash = stash_mock
        # Act
        self.SUT.append_to_stash(card)
        # Assert
        stash_mock.append.assert_called_once_with(card)

    def test_extend_to_stash__makes_expected_call(self):
        # Arrange
        cards = [Card.D6_D12_EXECUTIVE_INCISION, Card.D20_CUTASTROPHE]
        stash_mock = Mock()
        self.SUT.stash = stash_mock
        # Act
        self.SUT.extend_to_stash(cards)
        # Assert
        stash_mock.extend.assert_called_once_with(cards)

    def test_draw_hand__makes_expected_call(self):
        # Arrange
        deck_mock = Mock()
        self.SUT.draw = deck_mock
        # Act
        self.SUT.draw_hand()
        # Assert
        deck_mock.draw.assert_called_once_with(4)

    def test_has_face_up_card__returns_expected_false(self):
        # Arrange
        self.SUT.draw.append(Card.D20_CUTASTROPHE)
        self.SUT.draw.append(Card.D12_PROFESSIONAL_STABOTAGE)
        # Act
        has_face_up = self.SUT.has_face_up_cards
        # Assert
        self.assertFalse(has_face_up)

    def test_has_face_up_card__returns_expected_true(self):
        # Arrange
        self.SUT.draw.append(Card.D20_CUTASTROPHE)
        self.SUT.draw.append(Card.D12_PROFESSIONAL_STABOTAGE)
        self.SUT.draw.append(Card.D10_INNER_PIERCE)
        self.SUT.draw.append(Card.MANEUVER_IN_HEW_OF)
        self.SUT.draw.append(Card.D8_UNDERCOVER_CHOP)
        self.SUT.draw.append(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES)
        self.SUT.draw_hand()
        # Act
        has_face_up = self.SUT.has_face_up_cards
        # Assert
        self.assertTrue(has_face_up)

    def test_has_cards_to_draw__returns_expected_true(self):
        # Arrange
        self.SUT.draw.append(Card.D20_CUTASTROPHE)
        self.SUT.draw.append(Card.D12_PROFESSIONAL_STABOTAGE)
        self.SUT.draw.append(Card.D10_INNER_PIERCE)
        self.SUT.draw.append(Card.MANEUVER_IN_HEW_OF)
        self.SUT.draw.append(Card.D8_UNDERCOVER_CHOP)
        self.SUT.draw.append(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES)
        self.SUT.draw_hand()
        # Act
        has_draw = self.SUT.has_cards_to_draw
        # Assert
        self.assertTrue(has_draw)

    def test_has_cards_to_draw__returns_expected_false(self):
        # Arrange
        self.SUT.draw.append(Card.D20_CUTASTROPHE)
        self.SUT.draw.append(Card.D10_INNER_PIERCE)
        self.SUT.draw.append(Card.MANEUVER_IN_HEW_OF)
        self.SUT.draw.append(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES)
        self.SUT.draw_hand()
        # Act
        has_draw = self.SUT.has_cards_to_draw
        # Assert
        self.assertFalse(has_draw)

    def test_play_to_plate__expected_card_played(self):
        # Arrange
        self.SUT.draw.append(Card.D20_CUTASTROPHE)
        self.SUT.draw.append(Card.D10_INNER_PIERCE)
        self.SUT.draw.append(Card.MANEUVER_IN_HEW_OF)
        self.SUT.draw.append(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES)
        self.SUT.draw_hand()
        # Act
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.DEFEND)
        # Assert
        card = self.SUT.plate.lines[DecisionLine.LINE_1.pos].cards[0]
        self.assertEqual(Card.D20_CUTASTROPHE, card)
        card = self.SUT.draw.query_face_up_card()
        self.assertEqual(Card.D10_INNER_PIERCE, card)

    def test_is_legal_intention__no_cards__returns_true(self):
        # Arrange
        line = DecisionLine.LINE_2
        intention = DecisionIntention.DEFEND
        # Act
        is_legal = self.SUT.is_legal_intention(line, intention)
        # Assert
        self.assertTrue(is_legal)

    def test_is_legal_intention__some_cards__returns_true(self):
        # Arrange
        line = DecisionLine.LINE_2
        intention = DecisionIntention.DEFEND
        self.SUT.draw.append(Card.D20_CUTASTROPHE)
        self.SUT.draw.append(Card.D10_INNER_PIERCE)
        self.SUT.draw.append(Card.MANEUVER_IN_HEW_OF)
        self.SUT.draw.append(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES)
        self.SUT.draw_hand()
        self.SUT.play_to_plate(line, intention)
        # Act
        is_legal = self.SUT.is_legal_intention(line, intention)
        # Assert
        self.assertTrue(is_legal)

    def test_is_legal_intention__some_cards__returns_false(self):
        # Arrange
        line = DecisionLine.LINE_2
        self.SUT.draw.append(Card.D20_CUTASTROPHE)
        self.SUT.draw.append(Card.D10_INNER_PIERCE)
        self.SUT.draw.append(Card.MANEUVER_IN_HEW_OF)
        self.SUT.draw.append(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES)
        self.SUT.draw_hand()
        self.SUT.play_to_plate(line, DecisionIntention.ATTACK)
        # Act
        is_legal = self.SUT.is_legal_intention(line, DecisionIntention.DEPLOY)
        # Assert
        self.assertFalse(is_legal)

    def test_is_legal_intention__max_cards__returns_false(self):
        # Arrange
        line = DecisionLine.LINE_3
        self.SUT.draw.append(Card.D20_CUTASTROPHE)
        self.SUT.draw.append(Card.D10_INNER_PIERCE)
        self.SUT.draw.append(Card.MANEUVER_IN_HEW_OF)
        self.SUT.draw.append(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES)
        self.SUT.draw_hand()
        self.SUT.play_to_plate(line, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(line, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(line, DecisionIntention.ATTACK)
        # Act
        is_legal = self.SUT.is_legal_intention(line, DecisionIntention.ATTACK)
        # Assert
        self.assertFalse(is_legal)

    def test_nn_next_cards__calls_expected_call(self):
        # Arrange
        deck_mock = Mock()
        self.SUT.draw = deck_mock
        # Act
        self.SUT.nn_next_cards(8)
        # Assert
        deck_mock.nn_next_cards.assert_called_once_with(8)

    def test_stash_cards_total__returns_expected_total(self):
        # Arrange
        self.SUT.extend_to_stash([
            Card.D6_D12_EXECUTIVE_INCISION,
            Card.D20_CUTASTROPHE,
            Card.D4_SCARED_OUT_OF_YOUR_WHITTLES,
            Card.D8_UNDERCOVER_CHOP
        ])
        # Act
        total = self.SUT.stash_cards_total
        # Assert
        self.assertEqual(4, total)

    def test_plate_has_intention__does_not_have__false(self):
        # Arrange
        self.SUT.extend_to_draw(
            [
                Card.D6_D12_EXECUTIVE_INCISION,
                Card.D20_CUTASTROPHE,
                Card.D4_SCARED_OUT_OF_YOUR_WHITTLES,
                Card.D8_UNDERCOVER_CHOP,
                Card.D10_INNER_PIERCE,
                Card.D6_D12_EXECUTIVE_INCISION
            ]
        )
        self.SUT.play_to_plate(DecisionLine.LINE_4, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        # Act
        has = self.SUT.plate_has_intention(DecisionIntention.DEFEND)
        # Assert
        self.assertFalse(has)

    def test_plate_has_intention__does_have__true(self):
        # Arrange
        self.SUT.extend_to_draw(
            [
                Card.D6_D12_EXECUTIVE_INCISION,
                Card.D20_CUTASTROPHE,
                Card.D4_SCARED_OUT_OF_YOUR_WHITTLES,
                Card.D8_UNDERCOVER_CHOP,
                Card.D10_INNER_PIERCE,
                Card.D6_D12_EXECUTIVE_INCISION
            ]
        )
        self.SUT.play_to_plate(DecisionLine.LINE_4, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        # Act
        has = self.SUT.plate_has_intention(DecisionIntention.ATTACK)
        # Assert
        self.assertTrue(has)

    def test_has_revealed_intention__has__true(self):
        # Arrange
        self.SUT.extend_to_draw(
            [
                Card.D6_D12_EXECUTIVE_INCISION,
                Card.D20_CUTASTROPHE,
                Card.D4_SCARED_OUT_OF_YOUR_WHITTLES,
                Card.D8_UNDERCOVER_CHOP,
                Card.D10_INNER_PIERCE,
                Card.D6_D12_EXECUTIVE_INCISION
            ]
        )
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.plate.lines[DecisionLine.LINE_3.pos].intention_face_up = True
        # Act
        has = self.SUT.has_revealed_intention(DecisionIntention.ATTACK)
        # Assert
        self.assertTrue(has)

    def test_has_revealed_intention__does_not_have__false(self):
        # Arrange
        self.SUT.extend_to_draw(
            [
                Card.D6_D12_EXECUTIVE_INCISION,
                Card.D20_CUTASTROPHE,
                Card.D4_SCARED_OUT_OF_YOUR_WHITTLES,
                Card.D8_UNDERCOVER_CHOP,
                Card.D10_INNER_PIERCE,
                Card.D6_D12_EXECUTIVE_INCISION
            ]
        )
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.plate.lines[DecisionLine.LINE_3.pos].intention_face_up = True
        # Act
        has = self.SUT.has_revealed_intention(DecisionIntention.DEPLOY)
        # Assert
        self.assertFalse(has)

    def test_line_intention_of__with_some_intention__as_expected(self):
        # Arrange
        self.SUT.extend_to_draw(
            [
                Card.D6_D12_EXECUTIVE_INCISION,
                Card.D20_CUTASTROPHE,
                Card.D4_SCARED_OUT_OF_YOUR_WHITTLES,
                Card.D8_UNDERCOVER_CHOP,
                Card.D10_INNER_PIERCE,
                Card.D6_D12_EXECUTIVE_INCISION
            ]
        )
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        # Act
        intention = self.SUT.line_intention_of(DecisionLine.LINE_3)
        # Assert
        self.assertEqual(DecisionIntention.ATTACK, intention)

    def test_line_intention_of__with_no_intention__none(self):
        # Arrange
        self.SUT.extend_to_draw(
            [
                Card.D6_D12_EXECUTIVE_INCISION,
                Card.D20_CUTASTROPHE,
                Card.D4_SCARED_OUT_OF_YOUR_WHITTLES,
                Card.D8_UNDERCOVER_CHOP,
                Card.D10_INNER_PIERCE,
                Card.D6_D12_EXECUTIVE_INCISION
            ]
        )
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        # Act
        intention = self.SUT.line_intention_of(DecisionLine.LINE_2)
        # Assert
        self.assertEqual(DecisionIntention.NONE, intention)

    def test_line_card_values__makes_expected_call(self):
        # Arrange
        line = DecisionLine.LINE_1
        mock_plate = Mock()
        self.SUT.plate = mock_plate
        # Act
        self.SUT.line_card_values(line)
        # Assert
        mock_plate.line_card_values.assert_called_once_with(line)

    def test_lines_num_cards__with_cards__returns_expected(self):
        self.SUT.extend_to_draw(
            [
                Card.D6_D12_EXECUTIVE_INCISION,
                Card.D20_CUTASTROPHE,
                Card.D4_SCARED_OUT_OF_YOUR_WHITTLES,
                Card.D8_UNDERCOVER_CHOP,
                Card.D10_INNER_PIERCE,
                Card.D6_D12_EXECUTIVE_INCISION
            ]
        )
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_2, DecisionIntention.DEFEND)
        self.SUT.play_to_plate(DecisionLine.LINE_2, DecisionIntention.DEFEND)
        self.SUT.play_to_plate(DecisionLine.LINE_4, DecisionIntention.DEPLOY)
        self.SUT.play_to_plate(DecisionLine.LINE_4, DecisionIntention.DEPLOY)
        self.SUT.play_to_plate(DecisionLine.LINE_4, DecisionIntention.DEPLOY)
        # Act
        count = self.SUT.lines_num_cards
        # Assert
        self.assertEqual([1, 2, 0, 3], count)

    def test_discard_all__all_cards_from_plate_discarded_to_bottom_of_draw_deck(self):
        # Arrange
        self.SUT.extend_to_draw(
            [
                Card.D6_D12_EXECUTIVE_INCISION,
                Card.D20_CUTASTROPHE,
                Card.D4_SCARED_OUT_OF_YOUR_WHITTLES,
                Card.D8_UNDERCOVER_CHOP,
                Card.D10_INNER_PIERCE,
                Card.D6_D12_EXECUTIVE_INCISION
            ]
        )
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.plate.set_line_face_up(DecisionLine.LINE_1)
        # Act
        self.SUT.discard_all()
        # Assert
        count = self.SUT.draw.cards_total
        self.assertEqual(5, count)

    def test_discard_face_up__face_up_cards_from_plate_discarded_to_bottom_of_draw_deck(self):
        # Arrange
        self.SUT.extend_to_draw(
            [
                Card.D6_D12_EXECUTIVE_INCISION,
                Card.D20_CUTASTROPHE,
                Card.D4_SCARED_OUT_OF_YOUR_WHITTLES,
                Card.D8_UNDERCOVER_CHOP,
                Card.D10_INNER_PIERCE,
                Card.D6_D12_EXECUTIVE_INCISION
            ]
        )
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.plate.set_line_face_up(DecisionLine.LINE_1)
        # Act
        self.SUT.discard_face_up()
        # Assert
        count = self.SUT.draw.cards_total
        self.assertEqual(2, count)

    def test_upgrade_face_up_wounds__matched_wounds_upgraded(self):
        # Arrange
        self.SUT.extend_to_draw(
            [
                CardWound.WOUND_ACUTE,
                CardWound.WOUND_ACUTE,
                Card.D4_SCARED_OUT_OF_YOUR_WHITTLES,
                CardWound.WOUND_MINOR,
                CardWound.WOUND_MINOR,
                CardWound.WOUND_GRAVE
            ]
        )
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.plate.set_line_face_up(DecisionLine.LINE_1)
        expected_cards_1 = [
            Card.D4_SCARED_OUT_OF_YOUR_WHITTLES,
            CardWound.WOUND_ACUTE,
            CardWound.WOUND_GRAVE
        ]
        # Act
        self.SUT.upgrade_face_up_wounds()
        # Assert
        cards_3 = self.SUT.plate.lines[DecisionLine.LINE_3.pos].cards
        cards_1 = self.SUT.plate.lines[DecisionLine.LINE_1.pos].cards
        self.assertEqual(2, len(cards_3))
        self.assertEqual(3, len(cards_1))
        self.assertEqual(expected_cards_1, cards_1)

    def test_lose_energy_from_face_up_wounds__expected_energy_loss_incurred(self):
        # Arrange
        self.SUT.extend_to_draw(
            [
                CardWound.WOUND_ACUTE,
                CardWound.WOUND_ACUTE,
                Card.D4_SCARED_OUT_OF_YOUR_WHITTLES,
                CardWound.WOUND_MINOR,
                CardWound.WOUND_MINOR,
                CardWound.WOUND_GRAVE
            ]
        )
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.plate.set_line_face_up(DecisionLine.LINE_1)
        expected_energy = self.SUT.energy - CardWound.WOUND_GRAVE.energy_penalty
        # Act
        self.SUT.lose_energy_from_face_up_wounds()
        # Assert
        self.assertEqual(expected_energy, self.SUT.energy)

    def test_upgrade_lowest_face_up_wound__expected_wound_upgraded(self):
        # Arrange
        self.SUT.extend_to_draw(
            [
                CardWound.WOUND_ACUTE,
                CardWound.WOUND_ACUTE,
                Card.D4_SCARED_OUT_OF_YOUR_WHITTLES,
                CardWound.WOUND_MINOR,
                CardWound.WOUND_MINOR,
                CardWound.WOUND_GRAVE
            ]
        )
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.plate.set_line_face_up(DecisionLine.LINE_1)
        expected_cards = [
            Card.D4_SCARED_OUT_OF_YOUR_WHITTLES,
            CardWound.WOUND_ACUTE,
            CardWound.WOUND_MINOR,
            CardWound.WOUND_GRAVE
        ]
        # Act
        self.SUT.upgrade_lowest_face_up_wound()
        # Assert
        cards = self.SUT.plate.lines[DecisionLine.LINE_1.pos].cards
        self.assertEqual(expected_cards, cards)

    def test_upgrade_highest_face_up_wound__expected_wound_upgraded(self):
        # Arrange
        self.SUT.extend_to_draw(
            [
                CardWound.WOUND_ACUTE,
                CardWound.WOUND_ACUTE,
                Card.D4_SCARED_OUT_OF_YOUR_WHITTLES,
                CardWound.WOUND_MINOR,
                CardWound.WOUND_MINOR,
                CardWound.WOUND_GRAVE
            ]
        )
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.plate.set_line_face_up(DecisionLine.LINE_1)
        expected_cards = [
            Card.D4_SCARED_OUT_OF_YOUR_WHITTLES,
            CardWound.WOUND_MINOR,
            CardWound.WOUND_MINOR,
            CardWound.WOUND_DIRE
        ]
        # Act
        self.SUT.upgrade_highest_face_up_wound()
        # Assert
        cards = self.SUT.plate.lines[DecisionLine.LINE_1.pos].cards
        self.assertEqual(expected_cards, cards)

    def test_all_draw_cards__expected_cards_acquired(self):
        # Arrange
        expected_cards = [
            Card.D6_D12_EXECUTIVE_INCISION,
            Card.D8_UNDERCOVER_CHOP,
            Card.D10_INNER_PIERCE,
            Card.D12_PROFESSIONAL_STABOTAGE,
            Card.D20_CUTASTROPHE
        ]
        self.SUT.draw.extend(expected_cards)
        self.SUT.draw_hand()
        # Act
        cards = self.SUT.all_draw_cards
        # Assert
        self.assertEqual(expected_cards, cards)

    def test_num_all_draw_cards__expected_count_gotten(self):
        # Arrange
        self.SUT.draw.extend([
            Card.D6_D12_EXECUTIVE_INCISION,
            Card.D8_UNDERCOVER_CHOP,
            Card.D10_INNER_PIERCE,
            Card.D12_PROFESSIONAL_STABOTAGE,
            Card.D20_CUTASTROPHE
        ])
        self.SUT.draw_hand()
        # Act
        count = self.SUT.num_all_draw_cards
        # Assert
        self.assertEqual(5, count)

    def test_num_cards_stash__returns_expected_count(self):
        # Arrange
        self.SUT.extend_to_stash([
            Card.D6_D12_EXECUTIVE_INCISION,
            Card.D8_UNDERCOVER_CHOP,
            Card.D10_INNER_PIERCE,
            Card.D12_PROFESSIONAL_STABOTAGE,
            Card.D20_CUTASTROPHE
        ])
        self.SUT.stash.draw(2)
        # Act
        count = self.SUT.num_cards_stash
        # Assert
        self.assertEqual(5, count)

    def test_stash_cards_face_up__returns_expected_count(self):
        # Arrange
        self.SUT.extend_to_stash([
            Card.D6_D12_EXECUTIVE_INCISION,
            Card.D8_UNDERCOVER_CHOP,
            Card.D10_INNER_PIERCE,
            Card.D12_PROFESSIONAL_STABOTAGE,
            Card.D20_CUTASTROPHE
        ])
        self.SUT.stash.draw(2)
        expected_cards = [
            Card.D6_D12_EXECUTIVE_INCISION,
            Card.D8_UNDERCOVER_CHOP
        ]
        # Act
        cards = self.SUT.stash_cards_face_up
        # Assert
        self.assertEqual(expected_cards, cards)

    def test_stash_cards_draw__returns_expected_count(self):
        # Arrange
        self.SUT.extend_to_stash([
            Card.D6_D12_EXECUTIVE_INCISION,
            Card.D8_UNDERCOVER_CHOP,
            Card.D10_INNER_PIERCE,
            Card.D12_PROFESSIONAL_STABOTAGE,
            Card.D20_CUTASTROPHE
        ])
        self.SUT.stash.draw(2)
        expected_cards = [
            Card.D10_INNER_PIERCE,
            Card.D12_PROFESSIONAL_STABOTAGE,
            Card.D20_CUTASTROPHE
        ]
        # Act
        cards = self.SUT.stash_cards_draw
        # Assert
        self.assertEqual(expected_cards, cards)

    def test_energy_loss__returns_as_expected(self):
        # Arrange
        self.SUT.energy -= 3
        # Act
        value = self.SUT.energy_loss
        # Assert
        self.assertEqual(3, value)

    def test_stash_pull_face_up_card__returns_expected_card(self):
        # Arrange
        self.SUT.extend_to_stash([
            Card.D6_D12_EXECUTIVE_INCISION,
            Card.D8_UNDERCOVER_CHOP,
            Card.D10_INNER_PIERCE,
            Card.D12_PROFESSIONAL_STABOTAGE,
            Card.D20_CUTASTROPHE
        ])
        self.SUT.stash.draw(2)
        # Act
        card = self.SUT.stash_pull_face_up_card()
        # Assert
        self.assertEqual(Card.D8_UNDERCOVER_CHOP, card)

    def test_plate_has_face_up_sides__with_card__returns_true(self):
        # Arrange
        self.SUT.extend_to_draw([
            Card.D4_SCARED_OUT_OF_YOUR_WHITTLES,
            Card.D6_D12_EXECUTIVE_INCISION,
            Card.D8_UNDERCOVER_CHOP,
            Card.D10_INNER_PIERCE,
            Card.D12_PROFESSIONAL_STABOTAGE,
            Card.D20_CUTASTROPHE
        ])
        self.SUT.draw_hand()
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.DEPLOY)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.DEPLOY)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.DEPLOY)
        self.SUT.plate.set_line_face_up(DecisionLine.LINE_1)
        # Act
        has = self.SUT.plate_has_face_up_sides(DecisionIntention.DEPLOY, DieSides.D6)
        # Assert
        self.assertTrue(has)

    def test_plate_has_face_up_sides__without_card__returns_false(self):
        # Arrange
        self.SUT.extend_to_draw([
            Card.D4_SCARED_OUT_OF_YOUR_WHITTLES,
            Card.D6_D12_EXECUTIVE_INCISION,
            Card.D8_UNDERCOVER_CHOP,
            Card.D10_INNER_PIERCE,
            Card.D12_PROFESSIONAL_STABOTAGE,
            Card.D20_CUTASTROPHE
        ])
        self.SUT.draw_hand()
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.DEPLOY)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.DEPLOY)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.DEPLOY)
        self.SUT.plate.set_line_face_up(DecisionLine.LINE_1)
        # Act
        has = self.SUT.plate_has_face_up_sides(DecisionIntention.DEPLOY, DieSides.D20)
        # Assert
        self.assertTrue(has)

    def test_plate_has_face_up_sides__not_face_up__returns_false(self):
        # Arrange
        self.SUT.extend_to_draw([
            Card.D4_SCARED_OUT_OF_YOUR_WHITTLES,
            Card.D6_D12_EXECUTIVE_INCISION,
            Card.D8_UNDERCOVER_CHOP,
            Card.D10_INNER_PIERCE,
            Card.D12_PROFESSIONAL_STABOTAGE,
            Card.D20_CUTASTROPHE
        ])
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.DEPLOY)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.DEPLOY)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.DEPLOY)
        # Act
        has = self.SUT.plate_has_face_up_sides(DecisionIntention.DEPLOY, DieSides.D6)
        # Assert
        self.assertFalse(has)

    def test_plate_has_face_up_sides__wrong_intention__returns_false(self):
        # Arrange
        self.SUT.extend_to_draw([
            Card.D4_SCARED_OUT_OF_YOUR_WHITTLES,
            Card.D6_D12_EXECUTIVE_INCISION,
            Card.D8_UNDERCOVER_CHOP,
            Card.D10_INNER_PIERCE,
            Card.D12_PROFESSIONAL_STABOTAGE,
            Card.D20_CUTASTROPHE
        ])
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.DEPLOY)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.DEPLOY)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.DEPLOY)
        self.SUT.plate.set_line_face_up(DecisionLine.LINE_1)
        # Act
        has = self.SUT.plate_has_face_up_sides(DecisionIntention.ATTACK, DieSides.D6)
        # Assert
        self.assertFalse(has)

    def test_plate_remove_sides__call_made(self):
        # Arrange
        mock_plate = Mock()
        self.SUT.plate = mock_plate
        intention = DecisionIntention.DEFEND
        # Act
        self.SUT.plate_remove_sides(intention, DieSides.D20)
        # Assert
        mock_plate.remove_sides_on_face_up.assert_called_once_with(intention, DieSides.D20)


    def test_compute_draw_wound_penalty_value__returns_expected_value(self):
        # Arrange
        self.SUT.draw.extend([
            Card.D20_CUTASTROPHE,
            Card.D10_INNER_PIERCE,
            CardWound.WOUND_GRAVE,
            Card.D6_SLIT_TIGHT,
            CardWound.WOUND_MINOR,
            CardWound.WOUND_DIRE
        ])
        self.SUT.draw_hand()
        expected_value = 1 + 4 + 8
        # Act
        value = self.SUT.compute_draw_wound_penalty_value
        # Assert
        self.assertEqual(expected_value, value)

    def test_reveal_intentions_if_maxed__expected_call_made_to_plate(self):
        # Arrange
        mock_plate = Mock()
        self.SUT.plate = mock_plate
        # Act
        self.SUT.reveal_intentions_if_maxed()
        # Assert
        mock_plate.reveal_intentions_if_maxed.assert_called()

    def test_reveal_all_intentions__expected_call_made_to_plate(self):
        # Arrange
        mock_plate = Mock()
        self.SUT.plate = mock_plate
        # Act
        self.SUT.reveal_all_intentions()
        # Assert
        mock_plate.reveal_all_intentions.assert_called()

    def test_reveal_cards_with_revealed_intentions__expected_call_made_to_plate(self):
        # Arrange
        mock_plate = Mock()
        self.SUT.plate = mock_plate
        # Act
        self.SUT.reveal_cards_with_revealed_intentions()
        # Assert
        mock_plate.reveal_cards_with_revealed_intentions.assert_called()

    def test_reveal_intentions_of__expected_call_made_to_plate(self):
        # Arrange
        mock_plate = Mock()
        self.SUT.plate = mock_plate
        # Act
        self.SUT.reveal_intentions_of(DecisionIntention.ATTACK)
        # Assert
        mock_plate.reveal_intentions_of.assert_called_once_with(DecisionIntention.ATTACK)

    def test_collect_dice_for__expected_call_made_to_plate(self):
        # Arrange
        mock_plate = Mock()
        self.SUT.plate = mock_plate
        # Act
        self.SUT.collect_dice_for(DecisionIntention.ATTACK)
        # Assert
        mock_plate.collect_dice_for.assert_called_once_with(DecisionIntention.ATTACK)

    def test_collect_face_up_cards_for__expected_call_made_to_plate(self):
        # Arrange
        mock_plate = Mock()
        self.SUT.plate = mock_plate
        # Act
        self.SUT.collect_face_up_cards_for(DecisionIntention.ATTACK)
        # Assert
        mock_plate.collect_face_up_cards_for.assert_called_once_with(DecisionIntention.ATTACK)

    def test_central_maneuver_card__returns_expected_card(self):
        # Arrange
        self.SUT.plate.central_maneuver_card = Card.MANEUVER_BUST_A_CUT
        # Act
        card = self.SUT.central_maneuver_card
        # Assert
        self.assertEqual(Card.MANEUVER_BUST_A_CUT, card)

    # TODO: need to move logic to DecisionTree.
    def test_apply_feeling_feint(self):
        # Arrange
        # Act
        self.SUT.apply_feeling_feint(DecisionIntention.ATTACK)
        # Assert

    # TODO: requires some additional processing, just in case card is a maneuver card.
    def test_apply_to_die_four(self):
        # Arrange
        # Act
        self.SUT.apply_to_die_four()
        # Assert

    def test_reduce_reach_on__expected_call_made(self):
        # Arrange
        mock_plate = Mock()
        self.SUT.plate = mock_plate
        # Act
        self.SUT.reduce_reach_on(DecisionLine.LINE_2)
        # Assert
        mock_plate.reduce_reach_on.assert_called_once_with(DecisionLine.LINE_2)

    # TODO: Move to decision tree
    def test_add_penalty_coin(self):
        # Arrange
        # Act
        self.SUT.add_penalty_coin()

    def test_trash_one_random_face_up_plate_cards__trash_all_cards__all_non_wound_cards_trashed(self):
        # Arrange
        normal_cards = [
            Card.D20_CUTASTROPHE,
            Card.D12_PROFESSIONAL_STABOTAGE,
            Card.D10_INNER_PIERCE,
            Card.D8_UNDERCOVER_CHOP,
            Card.D6_SLIT_TIGHT,
            Card.D4_SCARED_OUT_OF_YOUR_WHITTLES
        ]
        wound_cards = [
            CardWound.WOUND_MINOR,
            CardWound.WOUND_ACUTE,
            CardWound.WOUND_GRAVE,
            CardWound.WOUND_DIRE
        ]
        self.SUT.draw.extend(normal_cards)
        self.SUT.draw.extend(wound_cards)
        self.SUT.draw_hand()
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.draw_hand()
        self.SUT.play_to_plate(DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.play_to_plate(DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.play_to_plate(DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.draw_hand()
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.plate.set_line_face_up(DecisionLine.LINE_1)
        self.SUT.plate.set_line_face_up(DecisionLine.LINE_2)
        self.SUT.plate.set_line_face_up(DecisionLine.LINE_3)
        # Act/Assert
        for i in range(len(normal_cards)):
            card = self.SUT.trash_one_random_face_up_plate_cards()
            self.assertTrue(card in normal_cards)
        card = self.SUT.trash_one_random_face_up_plate_cards()
        self.assertEqual(None, card)

    def test_trash_face_up_card__with_valid_card__returns_true(self):
        # Arrange
        normal_cards = [
            Card.D20_CUTASTROPHE,
            Card.D12_PROFESSIONAL_STABOTAGE,
            Card.D10_INNER_PIERCE,
            Card.D8_UNDERCOVER_CHOP,
            Card.D6_SLIT_TIGHT,
            Card.D4_SCARED_OUT_OF_YOUR_WHITTLES
        ]
        wound_cards = [
            CardWound.WOUND_MINOR,
            CardWound.WOUND_ACUTE,
            CardWound.WOUND_GRAVE,
            CardWound.WOUND_DIRE
        ]
        self.SUT.draw.extend(normal_cards)
        self.SUT.draw.extend(wound_cards)
        self.SUT.draw_hand()
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.draw_hand()
        self.SUT.play_to_plate(DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.play_to_plate(DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.play_to_plate(DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.draw_hand()
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.plate.set_line_face_up(DecisionLine.LINE_1)
        self.SUT.plate.set_line_face_up(DecisionLine.LINE_2)
        self.SUT.plate.set_line_face_up(DecisionLine.LINE_3)
        card = Card.D12_PROFESSIONAL_STABOTAGE
        self.assertTrue(card in self.SUT.plate.lines[DecisionLine.LINE_1.pos].cards)
        # Act
        flag = self.SUT.trash_face_up_card(card)
        # Assert
        self.assertTrue(flag)
        self.assertTrue(card not in self.SUT.plate.lines[DecisionLine.LINE_1.pos].cards)

    def test_trash_face_up_card__with_face_down_card__returns_false(self):
        # Arrange
        normal_cards = [
            Card.D20_CUTASTROPHE,
            Card.D12_PROFESSIONAL_STABOTAGE,
            Card.D10_INNER_PIERCE,
            Card.D8_UNDERCOVER_CHOP,
            Card.D6_SLIT_TIGHT,
            Card.D4_SCARED_OUT_OF_YOUR_WHITTLES
        ]
        wound_cards = [
            CardWound.WOUND_MINOR,
            CardWound.WOUND_ACUTE,
            CardWound.WOUND_GRAVE,
            CardWound.WOUND_DIRE
        ]
        self.SUT.draw.extend(normal_cards)
        self.SUT.draw.extend(wound_cards)
        self.SUT.draw_hand()
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.play_to_plate(DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.draw_hand()
        self.SUT.play_to_plate(DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.play_to_plate(DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.play_to_plate(DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.draw_hand()
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.play_to_plate(DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.plate.set_line_face_up(DecisionLine.LINE_1)
        self.SUT.plate.set_line_face_up(DecisionLine.LINE_3)
        card = Card.D6_SLIT_TIGHT
        self.assertTrue(card in self.SUT.plate.lines[DecisionLine.LINE_2.pos].cards)
        # Act
        flag = self.SUT.trash_face_up_card(card)
        # Assert
        self.assertFalse(flag)
        self.assertTrue(card in self.SUT.plate.lines[DecisionLine.LINE_2.pos].cards)