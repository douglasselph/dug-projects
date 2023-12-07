import unittest
from unittest.mock import Mock

from src.data.Player import Player
from src.data.Card import Card
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
        

