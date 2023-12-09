import unittest
from src.data.ManeuverPlate import ManeuverPlate
from src.data.Card import Card, DieSides, CardWound, card_ordinal
from src.data.Decision import *


class TestManeuverPlate(unittest.TestCase):

    def setUp(self):
        self.SUT = ManeuverPlate()

    def test_add_card__card_added_to_expected_line(self):
        # Arrange
        card = Card.D6_SLIT_TIGHT
        line = DecisionLine.LINE_4
        intention = DecisionIntention.DEPLOY
        # Act
        self.SUT.add_card(card, line, intention)
        # Assert
        self.assertEqual(card, self.SUT.lines[line.pos].cards[0])
        self.assertEqual(intention, self.SUT.lines[line.pos].intention)

    def test_is_set_intention_legal__when_not_set__returns_true(self):
        # Arrange
        line = DecisionLine.LINE_4
        intention = DecisionIntention.DEPLOY
        # Act
        is_legal = self.SUT.is_set_intention_legal(line, intention)
        # Assert
        self.assertTrue(is_legal)

    def test_is_set_intention_legal__when_set__returns_false(self):
        # Arrange
        line = DecisionLine.LINE_4
        intention = DecisionIntention.DEPLOY
        self.SUT.add_card(Card.D10_INNER_PIERCE, line, intention)
        # Act
        is_legal = self.SUT.is_set_intention_legal(line, intention)
        # Assert
        self.assertFalse(is_legal)

    def test_is_set_intention_legal__with_less_than_3_intentions__returns_true(self):
        # Arrange
        intention = DecisionIntention.DEPLOY
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_1, intention)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_2, intention)
        # Act
        is_legal = self.SUT.is_set_intention_legal(DecisionLine.LINE_4, intention)
        # Assert
        self.assertTrue(is_legal)

    def test_is_set_intention_legal__with_3_intentions_already__returns_false(self):
        # Arrange
        intention = DecisionIntention.DEPLOY
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_1, intention)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_2, intention)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_3, intention)
        # Act
        is_legal = self.SUT.is_set_intention_legal(DecisionLine.LINE_4, intention)
        # Assert
        self.assertFalse(is_legal)

    def test_is_set_intention_legal__with_penalty_on_intention__returns_false(self):
        # Arrange
        intention = DecisionIntention.DEPLOY
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_1, intention)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_2, intention)
        self.SUT.penalty_coins.append(intention)
        # Act
        is_legal = self.SUT.is_set_intention_legal(DecisionLine.LINE_4, intention)
        # Assert
        self.assertFalse(is_legal)

    def test_num_held_intention_coins_for__with_nothing_played__returns_expected(self):
        # Arrange
        # Act
        count = self.SUT.num_held_intention_coins_for(DecisionIntention.ATTACK)
        # Assert
        self.assertEquals(3, count)

    def test_num_used_intentions_coins_for__with_2_played__returns_2(self):
        # Arrange
        intention = DecisionIntention.DEPLOY
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_1, intention)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_2, intention)
        # Act
        count = self.SUT.num_used_intentions_coins_for(intention)
        # Assert
        self.assertEqual(2, count)

    def test_is_add_card_legal__returns_true(self):
        # Arrange
        line = DecisionLine.LINE_2
        # Act
        is_legal = self.SUT.is_add_card_legal(line)
        # Assert
        self.assertTrue(is_legal)

    def test_is_add_card_legal__with_max_cards__returns_false(self):
        # Arrange
        limit = ManeuverPlate.initial_line_card_sizes[0]
        line = DecisionLine.LINE_1
        self.SUT.add_card(Card.D10_INNER_PIERCE, line, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, line, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, line, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, line, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, line, DecisionIntention.ATTACK)
        # Act
        is_legal = self.SUT.is_add_card_legal(line)
        # Assert
        self.assertEquals(5, limit)
        self.assertFalse(is_legal)

    def test_line_num_cards__returns_expected_count(self):
        # Arrange
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_2, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_2, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_2, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_3, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_4, DecisionIntention.DEFEND)
        # Act
        counts = self.SUT.lines_num_cards
        # Assert
        self.assertEquals([4, 3, 2, 1], counts)

    def test_get_line_intention_id__none_set__returns_none(self):
        # Arrange
        line = DecisionLine.LINE_3
        # Act
        intention = self.SUT.line_intention_of(line)
        # Assert
        self.assertEquals(DecisionIntention.NONE, intention)

    def test_line_intention_of__set_after_set__returns_last_set(self):
        # Arrange
        line = DecisionLine.LINE_3
        self.SUT.add_card(Card.D20_CUTASTROPHE, line, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D12_PROFESSIONAL_STABOTAGE, line, DecisionIntention.ATTACK)
        # Act
        intention = self.SUT.line_intention_of(line)
        # Assert
        self.assertEqual(DecisionIntention.DEPLOY, intention)

    def test_line_card_values(self):
        # Arrange
        # Act
        self.SUT.nn_line_card_values(DecisionLine.LINE_1)
        # Assert

    def test_set_intention_face_up__as_expected(self):
        # Arrange
        # Act
        self.SUT.set_intention_face_up(DecisionLine.LINE_2)
        # Assert
        self.assertTrue(self.SUT.lines[DecisionLine.LINE_2.pos].intention_face_up)

    def test_set_cards_face_up__as_expected(self):
        # Arrange
        # Act
        self.SUT.set_line_face_up(DecisionLine.LINE_2)
        # Assert
        self.assertTrue(self.SUT.lines[DecisionLine.LINE_2.pos].cards_face_up)

    def test_reveal_intention_on_all_lines__as_expected(self):
        # Arrange
        # Act
        self.SUT.reveal_all_intentions()
        # Assert
        for line in DecisionLine:
            self.assertTrue(self.SUT.lines[line.pos].intention_face_up)

    def test_reveal_intentions_if_maxed__has_one_at_max__just_reveals_the_one_maxed(self):
        # Arrange
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_1, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_4, DecisionIntention.ATTACK)
        # Act
        self.SUT.reveal_intentions_if_maxed()
        # Assert
        self.assertFalse(self.SUT.lines[DecisionLine.LINE_1.pos].intention_face_up)
        self.assertFalse(self.SUT.lines[DecisionLine.LINE_2.pos].intention_face_up)
        self.assertTrue(self.SUT.lines[DecisionLine.LINE_3.pos].intention_face_up)
        self.assertFalse(self.SUT.lines[DecisionLine.LINE_4.pos].intention_face_up)

    def test_reveal_intentions_of__reveals_indicated_only(self):
        # Arrange
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_4, DecisionIntention.ATTACK)
        # Act
        self.SUT.reveal_intentions_of(DecisionIntention.DEPLOY)
        # Assert
        self.assertFalse(self.SUT.lines[DecisionLine.LINE_1.pos].intention_face_up)
        self.assertTrue(self.SUT.lines[DecisionLine.LINE_2.pos].intention_face_up)
        self.assertTrue(self.SUT.lines[DecisionLine.LINE_3.pos].intention_face_up)
        self.assertFalse(self.SUT.lines[DecisionLine.LINE_4.pos].intention_face_up)

    def test_reveal_cards_with_intentions__reveals_indicated_only(self):
        # Arrange
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_4, DecisionIntention.ATTACK)
        self.SUT.reveal_intentions_of(DecisionIntention.DEPLOY)
        # Act
        self.SUT.reveal_cards_with_revealed_intentions()
        # Assert
        self.assertFalse(self.SUT.lines[DecisionLine.LINE_1.pos].cards_face_up)
        self.assertTrue(self.SUT.lines[DecisionLine.LINE_2.pos].cards_face_up)
        self.assertTrue(self.SUT.lines[DecisionLine.LINE_3.pos].cards_face_up)
        self.assertFalse(self.SUT.lines[DecisionLine.LINE_4.pos].cards_face_up)

    def test_collect_dice_for__for_face_up_only__returns_expected_number_of_dice(self):
        # Arrange
        self.SUT.add_card(Card.D20_CUTASTROPHE, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D12_PROFESSIONAL_STABOTAGE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D8_UNDERCOVER_CHOP, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_SLIT_TIGHT, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES, DecisionLine.LINE_4, DecisionIntention.ATTACK)
        self.SUT.set_line_face_up(DecisionLine.LINE_1)
        self.SUT.set_line_face_up(DecisionLine.LINE_2)
        expected_value = [DieSides.D10, DieSides.D12]
        # Act
        dice = self.SUT.collect_dice_for(DecisionIntention.DEPLOY)
        # Assert
        self.assertEqual(expected_value, dice)

    def test_collect_face_up_cards_for__for_face_up_only__returns_expected_cards(self):
        # Arrange
        self.SUT.add_card(Card.D20_CUTASTROPHE, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D12_PROFESSIONAL_STABOTAGE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D8_UNDERCOVER_CHOP, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_SLIT_TIGHT, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES, DecisionLine.LINE_4, DecisionIntention.DEPLOY)
        self.SUT.set_line_face_up(DecisionLine.LINE_1)
        self.SUT.set_line_face_up(DecisionLine.LINE_2)
        self.SUT.set_line_face_up(DecisionLine.LINE_3)
        expected_value = [
            Card.D10_INNER_PIERCE,
            Card.D12_PROFESSIONAL_STABOTAGE,
            Card.D8_UNDERCOVER_CHOP,
            Card.D6_SLIT_TIGHT
        ]
        # Act
        cards = self.SUT.collect_face_up_cards_for(DecisionIntention.DEPLOY)
        # Assert
        self.assertEqual(expected_value, cards)

    def test_has_intention__with_none__returns_false(self):
        # Arrange
        # Act
        has = self.SUT.has_intention(DecisionIntention.DEFEND)
        # Assert
        self.assertFalse(has)

    def test_has_intention__has_other__returns_false(self):
        # Arrange
        self.SUT.add_card(Card.D20_CUTASTROPHE, DecisionLine.LINE_1, DecisionIntention.DEFEND)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D12_PROFESSIONAL_STABOTAGE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D8_UNDERCOVER_CHOP, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        # Act
        has = self.SUT.has_intention(DecisionIntention.ATTACK)
        # Assert
        self.assertFalse(has)

    def test_has_intention__has_one__returns_true(self):
        # Arrange
        self.SUT.add_card(Card.D20_CUTASTROPHE, DecisionLine.LINE_1, DecisionIntention.DEFEND)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D12_PROFESSIONAL_STABOTAGE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D8_UNDERCOVER_CHOP, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        # Act
        has = self.SUT.has_intention(DecisionIntention.DEFEND)
        # Assert
        self.assertTrue(has)

    def test_has_revealed_intention__with_none__returns_false(self):
        # Arrange
        # Act
        has = self.SUT.has_revealed_intention(DecisionIntention.DEFEND)
        # Assert
        self.assertFalse(has)

    def test_has_revealed_intention__with_revealed__returns_true(self):
        # Arrange
        self.SUT.add_card(Card.D20_CUTASTROPHE, DecisionLine.LINE_1, DecisionIntention.DEFEND)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D12_PROFESSIONAL_STABOTAGE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D8_UNDERCOVER_CHOP, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.reveal_intentions_of(DecisionIntention.DEFEND)
        # Act
        has = self.SUT.has_revealed_intention(DecisionIntention.DEFEND)
        # Assert
        self.assertTrue(has)

    def test_has_sides_on_face_up__with_none__returns_false(self):
        # Arrange
        # Act
        has = self.SUT.has_face_up_sides(DecisionIntention.ATTACK, DieSides.D6)
        # Assert
        self.assertFalse(has)

    def test_has_sides_on_face_up__with_one_but_not_face_up__returns_false(self):
        # Arrange
        self.SUT.add_card(Card.D20_CUTASTROPHE, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D12_PROFESSIONAL_STABOTAGE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D8_UNDERCOVER_CHOP, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_SLIT_TIGHT, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES, DecisionLine.LINE_4, DecisionIntention.DEPLOY)
        # Act
        has = self.SUT.has_face_up_sides(DecisionIntention.DEPLOY, DieSides.D6)
        # Assert
        self.assertFalse(has)

    def test_has_sides_on_face_up__with_one_on_face_up__returns_true(self):
        # Arrange
        self.SUT.add_card(Card.D20_CUTASTROPHE, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D12_PROFESSIONAL_STABOTAGE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D8_UNDERCOVER_CHOP, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_SLIT_TIGHT, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES, DecisionLine.LINE_4, DecisionIntention.DEPLOY)
        self.SUT.set_line_face_up(DecisionLine.LINE_3)
        # Act
        has = self.SUT.has_face_up_sides(DecisionIntention.DEPLOY, DieSides.D6)
        # Assert
        self.assertTrue(has)

    def test_has_sides_on_face_up__with_one_on_face_up_but_wrong_intention__returns_false(self):
        # Arrange
        self.SUT.add_card(Card.D20_CUTASTROPHE, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D12_PROFESSIONAL_STABOTAGE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D8_UNDERCOVER_CHOP, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_SLIT_TIGHT, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES, DecisionLine.LINE_4, DecisionIntention.DEPLOY)
        self.SUT.set_line_face_up(DecisionLine.LINE_3)
        # Act
        has = self.SUT.has_face_up_sides(DecisionIntention.ATTACK, DieSides.D6)
        # Assert
        self.assertFalse(has)

    def test_remove_sides_on_face_up__with_one_but_not_face_up__nothing_done(self):
        # Arrange
        self.SUT.add_card(Card.D20_CUTASTROPHE, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D12_PROFESSIONAL_STABOTAGE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D8_UNDERCOVER_CHOP, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_SLIT_TIGHT, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES, DecisionLine.LINE_4, DecisionIntention.DEPLOY)
        # Act
        card = self.SUT.remove_sides_on_face_up(DecisionIntention.DEPLOY, DieSides.D6)
        # Assert
        self.assertEquals(None, card)
        card_counts = self.SUT.lines_num_cards
        self.assertEquals([1, 2, 2, 1], card_counts)

    def test_remove_sides_on_face_up__with_one_on_face_up__removed(self):
        # Arrange
        self.SUT.add_card(Card.D20_CUTASTROPHE, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D12_PROFESSIONAL_STABOTAGE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D8_UNDERCOVER_CHOP, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_SLIT_TIGHT, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES, DecisionLine.LINE_4, DecisionIntention.DEPLOY)
        self.SUT.set_line_face_up(DecisionLine.LINE_3)
        # Act
        card = self.SUT.remove_sides_on_face_up(DecisionIntention.DEPLOY, DieSides.D6)
        # Assert
        self.assertEquals(Card.D6_SLIT_TIGHT, card)
        card_counts = self.SUT.lines_num_cards
        self.assertEquals([1, 2, 1, 1], card_counts)

    def test_remove_sides_on_face_up__with_one_on_face_up_but_wrong_intention__nothing_done(self):
        # Arrange
        self.SUT.add_card(Card.D20_CUTASTROPHE, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D12_PROFESSIONAL_STABOTAGE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D8_UNDERCOVER_CHOP, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_SLIT_TIGHT, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES, DecisionLine.LINE_4, DecisionIntention.DEPLOY)
        self.SUT.set_line_face_up(DecisionLine.LINE_3)
        # Act
        card = self.SUT.remove_sides_on_face_up(DecisionIntention.ATTACK, DieSides.D6)
        # Assert
        self.assertEquals(None, card)
        card_counts = self.SUT.lines_num_cards
        self.assertEquals([1, 2, 2, 1], card_counts)

    def test_discard_face_up__no_face_up__nothing_done(self):
        # Arrange
        self.SUT.add_card(Card.D20_CUTASTROPHE, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D8_D20_MY_INCISION_IS_FINAL, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D12_PROFESSIONAL_STABOTAGE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D8_UNDERCOVER_CHOP, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_SLIT_TIGHT, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES, DecisionLine.LINE_4, DecisionIntention.DEPLOY)
        # Act
        cards = self.SUT.discard_face_up()
        # Assert
        card_counts = self.SUT.lines_num_cards
        self.assertEquals([3, 2, 2, 1], card_counts)
        self.assertEquals(0, len(cards))

    def test_discard_face_up__has_face_up__cards_discarded(self):
        # Arrange
        self.SUT.add_card(Card.D20_CUTASTROPHE, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D8_D20_MY_INCISION_IS_FINAL, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D12_PROFESSIONAL_STABOTAGE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D8_UNDERCOVER_CHOP, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_SLIT_TIGHT, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES, DecisionLine.LINE_4, DecisionIntention.DEPLOY)
        self.SUT.set_line_face_up(DecisionLine.LINE_1)
        self.SUT.set_line_face_up(DecisionLine.LINE_4)
        # Act
        cards = self.SUT.discard_face_up()
        # Assert
        card_counts = self.SUT.lines_num_cards
        self.assertEquals([0, 2, 2, 0], card_counts)
        self.assertFalse(self.SUT.lines[DecisionLine.LINE_1.pos].cards_face_up)
        self.assertFalse(self.SUT.lines[DecisionLine.LINE_4.pos].cards_face_up)
        self.assertEquals(4, len(cards))

    def test_discard_all__has_face_up__all_cards_discarded(self):
        # Arrange
        self.SUT.add_card(Card.D20_CUTASTROPHE, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D8_D20_MY_INCISION_IS_FINAL, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D12_PROFESSIONAL_STABOTAGE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D8_UNDERCOVER_CHOP, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_SLIT_TIGHT, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES, DecisionLine.LINE_4, DecisionIntention.DEPLOY)
        self.SUT.set_line_face_up(DecisionLine.LINE_1)
        self.SUT.set_line_face_up(DecisionLine.LINE_4)
        # Act
        cards = self.SUT.discard_all()
        # Assert
        card_counts = self.SUT.lines_num_cards
        self.assertEqual([0, 0, 0, 0], card_counts)
        self.assertFalse(self.SUT.lines[DecisionLine.LINE_1.pos].cards_face_up)
        self.assertFalse(self.SUT.lines[DecisionLine.LINE_4.pos].cards_face_up)
        self.assertEqual(8, len(cards))

    def test_wounds_face_up__has_none__returns_none(self):
        # Arrange
        self.SUT.add_card(Card.D20_CUTASTROPHE, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D8_D20_MY_INCISION_IS_FINAL, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D12_PROFESSIONAL_STABOTAGE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D8_UNDERCOVER_CHOP, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_SLIT_TIGHT, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES, DecisionLine.LINE_4, DecisionIntention.DEPLOY)
        self.SUT.set_line_face_up(DecisionLine.LINE_1)
        self.SUT.set_line_face_up(DecisionLine.LINE_3)
        # Act
        cards = self.SUT.wounds_face_up
        # Assert
        self.assertEqual(0, len(cards))

    def test_wounds_face_up__has_wounds_but_not_on_face_up__returns_none(self):
        # Arrange
        self.SUT.add_card(Card.D20_CUTASTROPHE, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D8_D20_MY_INCISION_IS_FINAL, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(CardWound.WOUND_DIRE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D12_PROFESSIONAL_STABOTAGE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D8_UNDERCOVER_CHOP, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_SLIT_TIGHT, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES, DecisionLine.LINE_4, DecisionIntention.DEPLOY)
        self.SUT.set_line_face_up(DecisionLine.LINE_1)
        self.SUT.set_line_face_up(DecisionLine.LINE_3)
        # Act
        cards = self.SUT.wounds_face_up
        # Assert
        self.assertEqual(0, len(cards))

    def test_wounds_face_up__has_wounds_on_face_up__returns_wounds(self):
        # Arrange
        self.SUT.add_card(Card.D20_CUTASTROPHE, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(CardWound.WOUND_MINOR, DecisionLine.LINE_1, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D8_D20_MY_INCISION_IS_FINAL, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(CardWound.WOUND_DIRE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D12_PROFESSIONAL_STABOTAGE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D8_UNDERCOVER_CHOP, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(CardWound.WOUND_ACUTE, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_SLIT_TIGHT, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES, DecisionLine.LINE_4, DecisionIntention.DEPLOY)
        self.SUT.set_line_face_up(DecisionLine.LINE_1)
        self.SUT.set_line_face_up(DecisionLine.LINE_3)
        expected_value = [CardWound.WOUND_MINOR, CardWound.WOUND_ACUTE]
        # Act
        cards = self.SUT.wounds_face_up
        # Assert
        self.assertEqual(2, len(cards))
        self.assertEqual(expected_value, cards)

    def test_replace_wound_face_up__no_wounds_on_face_up__nothing_done(self):
        # Arrange
        self.SUT.add_card(Card.D20_CUTASTROPHE, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(CardWound.WOUND_MINOR, DecisionLine.LINE_1, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D8_D20_MY_INCISION_IS_FINAL, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(CardWound.WOUND_DIRE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D12_PROFESSIONAL_STABOTAGE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D8_UNDERCOVER_CHOP, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(CardWound.WOUND_ACUTE, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_SLIT_TIGHT, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES, DecisionLine.LINE_4, DecisionIntention.DEPLOY)
        self.SUT.set_line_face_up(DecisionLine.LINE_4)
        expected_values = [
            card_ordinal(Card.D20_CUTASTROPHE),
            card_ordinal(CardWound.WOUND_MINOR),
            card_ordinal(Card.D8_D20_MY_INCISION_IS_FINAL),
            card_ordinal(Card.D6_D12_EXECUTIVE_INCISION),
            0
        ]
        # Act
        done = self.SUT.replace_wound_face_up(CardWound.WOUND_MINOR, CardWound.WOUND_ACUTE)
        # Assert
        self.assertFalse(done)
        values = self.SUT.nn_line_card_values(DecisionLine.LINE_1)
        self.assertEqual(expected_values, values)

    def test_replace_wound_face_up__wounds_on_face_up__card_replaced(self):
        # Arrange
        self.SUT.add_card(Card.D20_CUTASTROPHE, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(CardWound.WOUND_MINOR, DecisionLine.LINE_1, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D8_D20_MY_INCISION_IS_FINAL, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(CardWound.WOUND_DIRE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D12_PROFESSIONAL_STABOTAGE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D8_UNDERCOVER_CHOP, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(CardWound.WOUND_ACUTE, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_SLIT_TIGHT, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES, DecisionLine.LINE_4, DecisionIntention.DEPLOY)
        self.SUT.set_line_face_up(DecisionLine.LINE_1)
        expected_values = [
            card_ordinal(Card.D20_CUTASTROPHE),
            card_ordinal(Card.D8_D20_MY_INCISION_IS_FINAL),
            card_ordinal(Card.D6_D12_EXECUTIVE_INCISION),
            card_ordinal(CardWound.WOUND_ACUTE),
            0
        ]
        # Act
        done = self.SUT.replace_wound_face_up(CardWound.WOUND_MINOR, CardWound.WOUND_ACUTE)
        # Assert
        self.assertTrue(done)
        values = self.SUT.nn_line_card_values(DecisionLine.LINE_1)
        self.assertEqual(expected_values, values)

    def test_reduce_reach_on__penalty_applied(self):
        # Arrange
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_1, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_1, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_1, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_1, DecisionIntention.DEPLOY)
        # Act
        self.SUT.reduce_reach_on(DecisionLine.LINE_1)
        # Assert
        can_add = self.SUT.lines[DecisionLine.LINE_1.pos].can_add
        self.assertFalse(can_add)

    def test_remove_on_face_up__has_card__card_removed(self):
        # Arrange
        self.SUT.add_card(Card.D20_CUTASTROPHE, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(CardWound.WOUND_MINOR, DecisionLine.LINE_1, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D8_D20_MY_INCISION_IS_FINAL, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(CardWound.WOUND_DIRE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D12_PROFESSIONAL_STABOTAGE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D8_UNDERCOVER_CHOP, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(CardWound.WOUND_ACUTE, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_SLIT_TIGHT, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES, DecisionLine.LINE_4, DecisionIntention.DEPLOY)
        self.SUT.set_line_face_up(DecisionLine.LINE_1)
        expected_values = [
            card_ordinal(Card.D20_CUTASTROPHE),
            card_ordinal(Card.D8_D20_MY_INCISION_IS_FINAL),
            card_ordinal(Card.D6_D12_EXECUTIVE_INCISION),
            0, 0
        ]
        # Act
        done = self.SUT.remove_on_face_up(CardWound.WOUND_MINOR)
        # Assert
        self.assertTrue(done)
        values = self.SUT.nn_line_card_values(DecisionLine.LINE_1)
        self.assertEquals(expected_values, values)

    def test_remove_on_face_up__has_card_but_not_face_up__nothing_done(self):
        # Arrange
        self.SUT.add_card(Card.D20_CUTASTROPHE, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(CardWound.WOUND_MINOR, DecisionLine.LINE_1, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D8_D20_MY_INCISION_IS_FINAL, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(CardWound.WOUND_DIRE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D12_PROFESSIONAL_STABOTAGE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D8_UNDERCOVER_CHOP, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(CardWound.WOUND_ACUTE, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_SLIT_TIGHT, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES, DecisionLine.LINE_4, DecisionIntention.DEPLOY)
        self.SUT.set_line_face_up(DecisionLine.LINE_2)
        expected_values = [
            card_ordinal(Card.D20_CUTASTROPHE),
            card_ordinal(CardWound.WOUND_MINOR),
            card_ordinal(Card.D8_D20_MY_INCISION_IS_FINAL),
            card_ordinal(Card.D6_D12_EXECUTIVE_INCISION),
            0
        ]
        # Act
        done = self.SUT.remove_on_face_up(CardWound.WOUND_MINOR)
        # Assert
        self.assertFalse(done)
        values = self.SUT.nn_line_card_values(DecisionLine.LINE_1)
        self.assertEqual(expected_values, values)

    def test_face_up_lines__returns_face_up_lines(self):
        # Arrange
        self.SUT.add_card(Card.D20_CUTASTROPHE, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(CardWound.WOUND_MINOR, DecisionLine.LINE_1, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D8_D20_MY_INCISION_IS_FINAL, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D6_D12_EXECUTIVE_INCISION, DecisionLine.LINE_1, DecisionIntention.ATTACK)
        self.SUT.add_card(Card.D10_INNER_PIERCE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(CardWound.WOUND_DIRE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D12_PROFESSIONAL_STABOTAGE, DecisionLine.LINE_2, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D8_UNDERCOVER_CHOP, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(CardWound.WOUND_ACUTE, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D6_SLIT_TIGHT, DecisionLine.LINE_3, DecisionIntention.DEPLOY)
        self.SUT.add_card(Card.D4_SCARED_OUT_OF_YOUR_WHITTLES, DecisionLine.LINE_4, DecisionIntention.DEPLOY)
        self.SUT.set_line_face_up(DecisionLine.LINE_2)
        self.SUT.set_line_face_up(DecisionLine.LINE_4)
        expected_lines = [
            self.SUT.lines[DecisionLine.LINE_2.pos],
            self.SUT.lines[DecisionLine.LINE_4.pos]
        ]
        # Act
        lines = self.SUT.face_up_lines
        # Assert
        self.assertEquals(expected_lines, lines)