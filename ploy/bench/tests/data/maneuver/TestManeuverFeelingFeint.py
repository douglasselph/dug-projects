import unittest
from typing import List, Optional

from src.data.maneuver.ManeuverFeelingFeint import ManeuverFeelingFeint
from src.data.ManeuverPlate import ManeuverPlate, Line
from src.data.Decision import DecisionIntention, DecisionLine
from src.decision.base.BaseFeelingFeint import BaseFeelingFeint
from src.data.Card import Card


class TestingFeelingFeint(BaseFeelingFeint):

    @staticmethod
    def select_card(plate: ManeuverPlate, coin: DecisionIntention) -> (Optional[Line], Card):
        return plate.lines[DecisionLine.LINE_1.pos], Card.MANEUVER_NICK_TO_DEATH


class TestManeuverFeelingFeint(unittest.TestCase):

    def setUp(self):
        self.decision = TestingFeelingFeint()
        self.SUT = ManeuverFeelingFeint(self.decision)

    def test_apply__card_moved_from_deploy_line_to_attack_line(self):
        # Arrange
        plate = ManeuverPlate()
        plate.add_card(Card.MANEUVER_NICK_TO_DEATH, DecisionLine.LINE_1, DecisionIntention.DEPLOY)
        plate.add_card(Card.D20_CUTASTROPHE, DecisionLine.LINE_2, DecisionIntention.ATTACK)
        plate.add_card(Card.MANEUVER_FEELING_FEINT, DecisionLine.LINE_2, DecisionIntention.ATTACK)
        plate.set_line_face_up(DecisionLine.LINE_2)
        coin = DecisionIntention.ATTACK
        # Act
        result_cards = self.SUT.apply(plate, coin)
        # Assert
        self.assertEqual([Card.MANEUVER_NICK_TO_DEATH], result_cards)
        cards = plate.lines[DecisionLine.LINE_2.pos].cards
        self.assertEqual(3, len(cards))
        self.assertEqual([
            Card.D20_CUTASTROPHE,
            Card.MANEUVER_FEELING_FEINT,
            Card.MANEUVER_NICK_TO_DEATH
        ], cards)

    def test_apply__not_face_up__nothing_done(self):
        # Arrange
        plate = ManeuverPlate()
        plate.add_card(Card.MANEUVER_NICK_TO_DEATH, DecisionLine.LINE_1, DecisionIntention.DEPLOY)
        plate.add_card(Card.D20_CUTASTROPHE, DecisionLine.LINE_2, DecisionIntention.ATTACK)
        plate.add_card(Card.MANEUVER_FEELING_FEINT, DecisionLine.LINE_2, DecisionIntention.ATTACK)
        coin = DecisionIntention.ATTACK
        # Act
        result_cards = self.SUT.apply(plate, coin)
        # Assert
        self.assertEqual([], result_cards)
        cards = plate.lines[DecisionLine.LINE_2.pos].cards
        self.assertEqual(2, len(cards))
        self.assertEqual([
            Card.D20_CUTASTROPHE,
            Card.MANEUVER_FEELING_FEINT
        ], cards)

