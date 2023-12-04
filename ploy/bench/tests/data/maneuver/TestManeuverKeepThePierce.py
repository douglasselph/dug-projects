import unittest

from src.data.Deck import Deck
from src.data.Card import Card, CardWound


class TestManeuverKeepThePierce(unittest.TestCase):

    def setUp(self):
        self.SUT = Card()

    def test_append_places_new_cards_on_bottom(self):
        # Arrange
        card1 = Card.D20_CUTASTROPHE
        card2 = Card.D10_INNER_PIERCE
        # Act
        self.SUT.append(card1)
        self.SUT.append(card2)
        # Assert
        confirm_card1 = self.SUT.draw_deck[0]
        confirm_card2 = self.SUT.draw_deck[1]
        self.assertEqual(card1, confirm_card1)
        self.assertEqual(card2, confirm_card2)

    def test_extend_adds_group_of_cards_on_bottom(self):
        # Arrange
        card1 = Card.D20_CUTASTROPHE
        card2 = Card.D10_INNER_PIERCE
        card3 = CardWound.WOUND_GRAVE
        card4 = CardWound.WOUND_ACUTE
        self.SUT.append(card1)
        self.SUT.append(card2)
        # Act
        self.SUT.extend([card3, card4])
        # Assert
        confirm_card1 = self.SUT.draw_deck[0]
        confirm_card2 = self.SUT.draw_deck[1]
        confirm_card3 = self.SUT.draw_deck[2]
        confirm_card4 = self.SUT.draw_deck[3]
        self.assertEqual(card1, confirm_card1)
        self.assertEqual(card2, confirm_card2)
        self.assertEqual(card3, confirm_card3)
        self.assertEqual(card4, confirm_card4)

    def test_draw_cards_moved_from_draw_deck_to_faceUp_deck(self):
        # Arrange
        card1 = Card.D20_CUTASTROPHE
        card2 = Card.D10_INNER_PIERCE
        card3 = CardWound.WOUND_GRAVE
        card4 = CardWound.WOUND_ACUTE
        self.SUT.extend([card1, card2, card3, card4])
        # Act
        self.SUT.draw()
        # Assert
        confirm_dr_card1 = self.SUT.draw_deck[0]
        confirm_dr_card2 = self.SUT.draw_deck[1]
        confirm_dr_card3 = self.SUT.draw_deck[2]
        confirm_up_card1 = self.SUT.faceUp_deck[0]
        self.assertEqual(card2, confirm_dr_card1)
        self.assertEqual(card3, confirm_dr_card2)
        self.assertEqual(card4, confirm_dr_card3)
        self.assertEqual(card1, confirm_up_card1)

    def test_draw_when_no_more_cards_does_nothing(self):
        # Arrange
        card1 = Card.D20_CUTASTROPHE
        card2 = Card.D10_INNER_PIERCE
        card3 = CardWound.WOUND_GRAVE
        card4 = CardWound.WOUND_ACUTE
        self.SUT.extend([card1, card2, card3, card4])
        # Act
        self.SUT.draw(6)
        # Assert
        count_faceUp = self.SUT.faceUp_deck
        self.assertEqual(4, count_faceUp)
        count_draw = self.SUT.draw_deck
        self.assertEqual(0, count_draw)

    def test_can_draw_false_if_no_cards(self):
        # Arrange
        # Act
        flag = self.SUT.can_draw
        # Assert
        self.assertFalse(flag)

    def test_can_draw_true_if_cards(self):
        # Arrange
        card1 = Card.D20_CUTASTROPHE
        card2 = Card.D10_INNER_PIERCE
        self.SUT.append(card1)
        self.SUT.append(card2)
        # Act
        flag = self.SUT.can_draw
        # Assert
        self.assertTrue(flag)

    def test_query_face_up_card_no_cards_then_no_card_acquired(self):
        # Arrange
        card1 = Card.D20_CUTASTROPHE
        card2 = Card.D10_INNER_PIERCE
        card3 = CardWound.WOUND_GRAVE
        card4 = CardWound.WOUND_ACUTE
        self.SUT.extend([card1, card2, card3, card4])
        # Act
        card = self.SUT.query_face_up_card()
        # Assert
        self.assertEqual(None, card)

    def test_query_face_up_card_expected_card_acquired(self):
        # Arrange
        card1 = Card.D20_CUTASTROPHE
        card2 = Card.D10_INNER_PIERCE
        card3 = CardWound.WOUND_GRAVE
        card4 = CardWound.WOUND_ACUTE
        self.SUT.extend([card1, card2, card3, card4])
        self.SUT.draw()
        # Act
        card = self.SUT.query_face_up_card()
        # Assert
        self.assertEqual(card1, card)

    def test_pull_face_up_card_expected_card_acquired(self):
        # Arrange
        card1 = Card.D20_CUTASTROPHE
        card2 = Card.D10_INNER_PIERCE
        card3 = CardWound.WOUND_GRAVE
        card4 = CardWound.WOUND_ACUTE
        self.SUT.extend([card1, card2, card3, card4])
        self.SUT.draw()
        # Act
        card = self.SUT.pull_face_up_card()
        remaining = self.SUT.query_face_up_card()
        # Assert
        self.assertEqual(card1, card)
        self.assertEqual(None, remaining)

    def test_cards_total_expected_cards_found_1(self):
        # Arrange
        card1 = Card.D20_CUTASTROPHE
        card2 = Card.D10_INNER_PIERCE
        card3 = CardWound.WOUND_GRAVE
        card4 = CardWound.WOUND_ACUTE
        self.SUT.extend([card1, card2, card3, card4])
        self.SUT.draw(2)
        # Act
        count = self.SUT.cards_total
        # Assert
        self.assertEqual(4, count)

    def test_cards_total_expected_cards_found_2(self):
        # Arrange
        card1 = Card.D20_CUTASTROPHE
        card2 = Card.D10_INNER_PIERCE
        card3 = CardWound.WOUND_GRAVE
        card4 = CardWound.WOUND_ACUTE
        self.SUT.extend([card1, card2, card3, card4])
        self.SUT.draw()
        self.SUT.pull_face_up_card()
        # Act
        count = self.SUT.cards_total
        # Assert
        self.assertEqual(3, count)

    def test_nn_face_up_cards_less_than_size_ensure_size_returned(self):
        # Arrange
        card1 = Card.D20_CUTASTROPHE
        card2 = Card.D10_INNER_PIERCE
        card3 = CardWound.WOUND_GRAVE
        card4 = CardWound.WOUND_ACUTE
        self.SUT.extend([card1, card2, card3, card4])
        self.SUT.draw(4)
        size = 8
        # Act
        cards = self.SUT.nn_face_up_cards(size)
        # Assert
        self.assertEqual(size, len(cards))

    def test_nn_face_up_cards_more_than_size_ensure_size_returned(self):
        # Arrange
        card1 = Card.D20_CUTASTROPHE
        card2 = Card.D10_INNER_PIERCE
        card3 = CardWound.WOUND_GRAVE
        card4 = CardWound.WOUND_ACUTE
        self.SUT.extend([card1, card2, card3, card4, card1, card2, card3, card4])
        self.SUT.draw(8)
        size = 4
        # Act
        cards = self.SUT.nn_face_up_cards(size)
        # Assert
        self.assertEqual(size, len(cards))

    def test_nn_face_up_cards_when_not_enough_returns_0_for_remainder(self):
        # Arrange
        card1 = Card.D20_CUTASTROPHE
        card2 = Card.D10_INNER_PIERCE
        card3 = CardWound.WOUND_GRAVE
        card4 = CardWound.WOUND_ACUTE
        self.SUT.extend([card1, card2, card3, card4])
        self.SUT.draw()
        size = 4
        # Act
        cards = self.SUT.nn_face_up_cards(size)
        # Assert
        self.assertEqual(size, len(cards))
        self.assertEqual(card1, cards[0])
        self.assertEqual(Card.NONE, cards[1])
        self.assertEqual(Card.NONE, cards[2])
        self.assertEqual(Card.NONE, cards[3])

    def test_nn_wound_value_adds_up_as_expected(self):
        # Arrange
        self.SUT.extend([
            Card.D20_CUTASTROPHE,
            Card.D10_INNER_PIERCE,
            CardWound.WOUND_GRAVE,
            Card.D6_SLIT_TIGHT,
            CardWound.WOUND_MINOR,
            CardWound.WOUND_DIRE
        ])
        self.SUT.draw(4)
        expected_value = 1 + 4 + 8
        # Act
        value = self.SUT.nn_wound_value
        # Assert
        self.assertEqual(expected_value, value)

    def test_nn_wound_value_adds_up_as_expected2(self):
        # Arrange
        self.SUT.extend([
            Card.D20_CUTASTROPHE,
            Card.D10_INNER_PIERCE,
            CardWound.WOUND_GRAVE,
            CardWound.WOUND_ACUTE,
            CardWound.WOUND_MINOR,
            CardWound.WOUND_DIRE
        ])
        expected_value = 1 + 2 + 4 + 8
        # Act
        value = self.SUT.nn_wound_value
        # Assert
        self.assertEqual(expected_value, value)
