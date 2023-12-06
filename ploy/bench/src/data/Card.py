# package src.data
from __future__ import annotations
from enum import Enum
from typing import Union, Optional
from src.data.RewardConstants import RewardConstants
import random


class DieSides(Enum):
    NONE = 0
    D4 = 4
    D6 = 6
    D8 = 8
    D10 = 10
    D12 = 12
    D20 = 20

    def roll(self) -> int:
        if self == DieSides.NONE:
            return 0
        return random.randint(1, self.value)

    @property
    def sides(self) -> int:
        return self.value

    def downgrade(self) -> DieSides:
        if self == DieSides.D20:
            return DieSides.D12
        if self == DieSides.D12:
            return DieSides.D10
        if self == DieSides.D10:
            return DieSides.D8
        if self == DieSides.D8:
            return DieSides.D6
        if self == DieSides.D6:
            return DieSides.D4
        return DieSides.NONE

    def average(self) -> float:
        if self.value == 0:
            return 0
        return (self.value + 1) / 2


class CardCost:
    pips: int
    sides: Optional[DieSides]
    energy: int

    def __init__(self, pips: int, energy: int = 0, sides: DieSides = None):
        self.pips = pips  # Always this
        self.energy = energy  # Then this,
        self.sides = sides  # Or this

    def can_afford(self, pips: int, energy: int) -> bool:
        return pips >= self.pips and energy > self.energy


class TrashBonus:

    def __init__(self):
        self.valid = False


class TrashBonusDie(TrashBonus):
    sides: DieSides

    def __init__(self, sides: DieSides):
        super().__init__()
        self.sides = sides
        self.valid = True


class TrashBonusPips(TrashBonus):
    pips: int

    def __init__(self, pips: int):
        super().__init__()
        self.pips = pips
        self.valid = True


#
# Warning: in using the auto() function if an entry is ever inserted in the middle, that will totally mess up
# the existing neural net.
#
class Card(Enum):
    NONE = 0  # No card
    FACE_DOWN = 1  # The card is present but the specific card is unknown
    D4_SCARED_OUT_OF_YOUR_WHITTLES = 40
    D4_D10_SLASH_AND_BURN = 41
    D6_SLIT_TIGHT = 60
    D6_D12_EXECUTIVE_INCISION = 61
    D8_UNDERCOVER_CHOP = 80
    D8_D20_MY_INCISION_IS_FINAL = 81
    D10_INNER_PIERCE = 100
    D12_PROFESSIONAL_STABOTAGE = 120
    D20_CUTASTROPHE = 200
    MANEUVER_BUST_A_CUT = 500
    MANEUVER_CUTTING_RIPOSTE = 501
    MANEUVER_FEELING_FEINT = 502
    MANEUVER_FEINT_HEARTED = 503
    MANEUVER_IN_HEW_OF = 504
    MANEUVER_KEEP_THE_PIERCE = 505
    MANEUVER_NICK_TO_DEATH = 506
    MANEUVER_PRECISION = 507
    MANEUVER_TO_DIE_FOUR = 508

    def __str__(self) -> str:
        if self == Card.NONE:
            return "None"
        if self == Card.FACE_DOWN:
            return "FaceDown"
        if self == Card.D4_SCARED_OUT_OF_YOUR_WHITTLES:
            return "Scared Out of Your Whittles"
        if self == Card.D4_D10_SLASH_AND_BURN:
            return "Slash and Burn"
        if self == Card.D6_SLIT_TIGHT:
            return "Slit Tight"
        if self == Card.D6_D12_EXECUTIVE_INCISION:
            return "Executive Incision"
        if self == Card.D8_UNDERCOVER_CHOP:
            return "Undercover Chop"
        if self == Card.D8_D20_MY_INCISION_IS_FINAL:
            return "My Incision is Final"
        if self == Card.D10_INNER_PIERCE:
            return "Inner Pierce"
        if self == Card.D12_PROFESSIONAL_STABOTAGE:
            return "Professional Stabotage"
        if self == Card.D20_CUTASTROPHE:
            return "Cutastrophe"
        if self == Card.MANEUVER_BUST_A_CUT:
            return "Bust a Cut"
        if self == Card.MANEUVER_CUTTING_RIPOSTE:
            return "Cutting Riposte"
        if self == Card.MANEUVER_FEELING_FEINT:
            return "Feeling Feint"
        if self == Card.MANEUVER_FEINT_HEARTED:
            return "Feint Hearted"
        if self == Card.MANEUVER_IN_HEW_OF:
            return "In Hew of"
        if self == Card.MANEUVER_KEEP_THE_PIERCE:
            return "Keep the Pierce"
        if self == Card.MANEUVER_NICK_TO_DEATH:
            return "Nick to Death"
        if self == Card.MANEUVER_PRECISION:
            return "Precision"
        if self == Card.MANEUVER_TO_DIE_FOUR:
            return "To Die Four"
        return "Unset"

    @property
    def description(self) -> str:
        if self == Card.NONE:
            return "None"
        if self == Card.FACE_DOWN:
            return "Face down and therefore unknown"
        if self == Card.D4_SCARED_OUT_OF_YOUR_WHITTLES:
            return ""
        if self == Card.D4_D10_SLASH_AND_BURN:
            return ""
        if self == Card.D6_SLIT_TIGHT:
            return ""
        if self == Card.D6_D12_EXECUTIVE_INCISION:
            return ""
        if self == Card.D8_UNDERCOVER_CHOP:
            return ""
        if self == Card.D8_D20_MY_INCISION_IS_FINAL:
            return ""
        if self == Card.D10_INNER_PIERCE:
            return ""
        if self == Card.D12_PROFESSIONAL_STABOTAGE:
            return ""
        if self == Card.D20_CUTASTROPHE:
            return ""
        if self == Card.MANEUVER_BUST_A_CUT:
            return "You may reroll any one die. You must keep the second roll."
        if self == Card.MANEUVER_CUTTING_RIPOSTE:
            return "Before the roll, downgrade the number of sides of one of the your opponent's dice. For example, a D20 to a D12, or a D12 to a D10."
        if self == Card.MANEUVER_FEELING_FEINT:
            return "You may move any facedown card on another line into the line with this card and immediately reveal and apply it."
        if self == Card.MANEUVER_FEINT_HEARTED:
            return ""
        if self == Card.MANEUVER_IN_HEW_OF:
            return "Reroll all one's rolled"
        if self == Card.MANEUVER_KEEP_THE_PIERCE:
            return "Just once, when your opponent rolls a one, retain that value; however, thast specific die is captured and can be rerolled as part of one's own rolls."
        if self == Card.MANEUVER_NICK_TO_DEATH:
            return "Reroll a D4 to determine how many D4s are added to the roll."
        if self == Card.MANEUVER_PRECISION:
            return "After rolling, choose a die in the same group, it is not at max. A D20 costs 1 Energy"
        if self == Card.MANEUVER_TO_DIE_FOUR:
            return "Draw a card and apply to this line."
        return "Unset"

    @property
    def cost(self) -> CardCost:
        if self == Card.NONE:
            return CardCost(0)
        if self == Card.FACE_DOWN:
            return CardCost(0)
        if self == Card.D4_SCARED_OUT_OF_YOUR_WHITTLES:
            return CardCost(4)
        if self == Card.D4_D10_SLASH_AND_BURN:
            return CardCost(6)
        if self == Card.D6_SLIT_TIGHT:
            return CardCost(6)
        if self == Card.D6_D12_EXECUTIVE_INCISION:
            return CardCost(8)
        if self == Card.D8_UNDERCOVER_CHOP:
            return CardCost(8)
        if self == Card.D8_D20_MY_INCISION_IS_FINAL:
            return CardCost(10)
        if self == Card.D10_INNER_PIERCE:
            return CardCost(10, 1, DieSides.D8)
        if self == Card.D12_PROFESSIONAL_STABOTAGE:
            return CardCost(10, 1, DieSides.D10)
        if self == Card.D20_CUTASTROPHE:
            return CardCost(12, 2, DieSides.D12)
        if self == Card.MANEUVER_BUST_A_CUT:
            return CardCost(5, 1)
        if self == Card.MANEUVER_CUTTING_RIPOSTE:
            return CardCost(7)
        if self == Card.MANEUVER_FEELING_FEINT:
            return CardCost(7)
        if self == Card.MANEUVER_FEINT_HEARTED:
            return CardCost(1)
        if self == Card.MANEUVER_IN_HEW_OF:
            return CardCost(5, 1)
        if self == Card.MANEUVER_KEEP_THE_PIERCE:
            return CardCost(7, 1)
        if self == Card.MANEUVER_NICK_TO_DEATH:
            return CardCost(12)
        if self == Card.MANEUVER_PRECISION:
            return CardCost(10, 1)
        if self == Card.MANEUVER_TO_DIE_FOUR:
            return CardCost(4, 1)
        return CardCost(0)

    @property
    def trash(self) -> TrashBonus:
        if self == Card.NONE:
            return TrashBonus()
        if self == Card.FACE_DOWN:
            return TrashBonus()
        if self == Card.D4_SCARED_OUT_OF_YOUR_WHITTLES:
            return TrashBonusDie(DieSides.D4)
        if self == Card.D4_D10_SLASH_AND_BURN:
            return TrashBonusDie(DieSides.D10)
        if self == Card.D6_SLIT_TIGHT:
            return TrashBonusDie(DieSides.D6)
        if self == Card.D6_D12_EXECUTIVE_INCISION:
            return TrashBonusDie(DieSides.D12)
        if self == Card.D8_UNDERCOVER_CHOP:
            return TrashBonusDie(DieSides.D8)
        if self == Card.D8_D20_MY_INCISION_IS_FINAL:
            return TrashBonusDie(DieSides.D20)
        if self == Card.D10_INNER_PIERCE:
            return TrashBonusDie(DieSides.D10)
        if self == Card.D12_PROFESSIONAL_STABOTAGE:
            return TrashBonusDie(DieSides.D12)
        if self == Card.D20_CUTASTROPHE:
            return TrashBonusDie(DieSides.D20)
        if self == Card.MANEUVER_BUST_A_CUT:
            return TrashBonus()
        if self == Card.MANEUVER_CUTTING_RIPOSTE:
            return TrashBonus()
        if self == Card.MANEUVER_FEELING_FEINT:
            return TrashBonusPips(2)
        if self == Card.MANEUVER_FEINT_HEARTED:
            return TrashBonusPips(1)
        if self == Card.MANEUVER_IN_HEW_OF:
            return TrashBonus()
        if self == Card.MANEUVER_KEEP_THE_PIERCE:
            return TrashBonus()
        if self == Card.MANEUVER_NICK_TO_DEATH:
            return TrashBonus()
        if self == Card.MANEUVER_PRECISION:
            return TrashBonus()
        if self == Card.MANEUVER_TO_DIE_FOUR:
            return TrashBonusDie(DieSides.D4)
        return TrashBonus()

    @property
    def trash_choice_value(self) -> int:
        if self == Card.NONE:
            return 0
        if self == Card.FACE_DOWN:
            return 0
        if self == Card.D4_SCARED_OUT_OF_YOUR_WHITTLES:
            return 8
        if self == Card.D4_D10_SLASH_AND_BURN:
            return 10
        if self == Card.D6_SLIT_TIGHT:
            return 9
        if self == Card.D6_D12_EXECUTIVE_INCISION:
            return 12
        if self == Card.D8_UNDERCOVER_CHOP:
            return 7
        if self == Card.D8_D20_MY_INCISION_IS_FINAL:
            return 14
        if self == Card.D10_INNER_PIERCE:
            return 6
        if self == Card.D12_PROFESSIONAL_STABOTAGE:
            return 4
        if self == Card.D20_CUTASTROPHE:
            return 2
        if self == Card.MANEUVER_BUST_A_CUT:
            return 0
        if self == Card.MANEUVER_CUTTING_RIPOSTE:
            return 0
        if self == Card.MANEUVER_FEELING_FEINT:
            return 0
        if self == Card.MANEUVER_FEINT_HEARTED:
            return 16
        if self == Card.MANEUVER_IN_HEW_OF:
            return 0
        if self == Card.MANEUVER_KEEP_THE_PIERCE:
            return 0
        if self == Card.MANEUVER_NICK_TO_DEATH:
            return 0
        if self == Card.MANEUVER_PRECISION:
            return 0
        if self == Card.MANEUVER_TO_DIE_FOUR:
            return 0
        return 0

    @property
    def die_bonus(self) -> DieSides:
        if self == Card.NONE:
            return DieSides.NONE
        if self == Card.FACE_DOWN:
            return DieSides.NONE
        if self == Card.D4_SCARED_OUT_OF_YOUR_WHITTLES:
            return DieSides.D4
        if self == Card.D4_D10_SLASH_AND_BURN:
            return DieSides.D4
        if self == Card.D6_SLIT_TIGHT:
            return DieSides.D6
        if self == Card.D6_D12_EXECUTIVE_INCISION:
            return DieSides.D6
        if self == Card.D8_UNDERCOVER_CHOP:
            return DieSides.D8
        if self == Card.D8_D20_MY_INCISION_IS_FINAL:
            return DieSides.D8
        if self == Card.D10_INNER_PIERCE:
            return DieSides.D10
        if self == Card.D12_PROFESSIONAL_STABOTAGE:
            return DieSides.D12
        if self == Card.D20_CUTASTROPHE:
            return DieSides.D20
        if self == Card.MANEUVER_BUST_A_CUT:
            return DieSides.NONE
        if self == Card.MANEUVER_CUTTING_RIPOSTE:
            return DieSides.NONE
        if self == Card.MANEUVER_FEELING_FEINT:
            return DieSides.NONE
        if self == Card.MANEUVER_FEINT_HEARTED:
            return DieSides.NONE
        if self == Card.MANEUVER_IN_HEW_OF:
            return DieSides.D4
        if self == Card.MANEUVER_KEEP_THE_PIERCE:
            return DieSides.NONE
        if self == Card.MANEUVER_NICK_TO_DEATH:
            return DieSides.NONE
        if self == Card.MANEUVER_PRECISION:
            return DieSides.NONE
        if self == Card.MANEUVER_TO_DIE_FOUR:
            return DieSides.D4
        return DieSides.NONE

    @property
    def ff_value(self) -> int:
        if self == Card.NONE:
            return 0
        if self == Card.FACE_DOWN:
            return 0
        if self == Card.D4_SCARED_OUT_OF_YOUR_WHITTLES:
            return 4
        if self == Card.D4_D10_SLASH_AND_BURN:
            return 5
        if self == Card.D6_SLIT_TIGHT:
            return 6
        if self == Card.D6_D12_EXECUTIVE_INCISION:
            return 7
        if self == Card.D8_UNDERCOVER_CHOP:
            return 8
        if self == Card.D8_D20_MY_INCISION_IS_FINAL:
            return 9
        if self == Card.D10_INNER_PIERCE:
            return 10
        if self == Card.D12_PROFESSIONAL_STABOTAGE:
            return 12
        if self == Card.D20_CUTASTROPHE:
            return 20
        if self == Card.MANEUVER_BUST_A_CUT:
            return 7
        if self == Card.MANEUVER_CUTTING_RIPOSTE:
            return 6
        if self == Card.MANEUVER_FEELING_FEINT:
            return 5
        if self == Card.MANEUVER_FEINT_HEARTED:
            return 1
        if self == Card.MANEUVER_IN_HEW_OF:
            return 6
        if self == Card.MANEUVER_KEEP_THE_PIERCE:
            return 8
        if self == Card.MANEUVER_NICK_TO_DEATH:
            return 7
        if self == Card.MANEUVER_PRECISION:
            return 20
        if self == Card.MANEUVER_TO_DIE_FOUR:
            return 13
        return 0


class CardWound(Enum):
    WOUND_NONE = 1000
    WOUND_MINOR = 1001
    WOUND_ACUTE = 1002
    WOUND_GRAVE = 1003
    WOUND_DIRE = 1004

    def __str__(self) -> str:
        if self == CardWound.WOUND_MINOR:
            return "Minor Wound"
        if self == CardWound.WOUND_ACUTE:
            return "Acute Wound"
        if self == CardWound.WOUND_GRAVE:
            return "Grave Wound"
        if self == CardWound.WOUND_DIRE:
            return "Dire Wound"
        return ""

    @property
    def pip_penalty(self) -> int:
        if self == CardWound.WOUND_MINOR:
            return 0
        if self == CardWound.WOUND_ACUTE:
            return 1
        if self == CardWound.WOUND_GRAVE:
            return 2
        if self == CardWound.WOUND_DIRE:
            return 4
        return 0

    @property
    def energy_penalty(self) -> int:
        if self == CardWound.WOUND_MINOR:
            return 0
        if self == CardWound.WOUND_ACUTE:
            return 0
        if self == CardWound.WOUND_GRAVE:
            return 1
        if self == CardWound.WOUND_DIRE:
            return 2
        return 0

    @property
    def upgrade(self) -> Optional[CardWound]:
        if self == CardWound.WOUND_MINOR:
            return CardWound.WOUND_ACUTE
        if self == CardWound.WOUND_ACUTE:
            return CardWound.WOUND_GRAVE
        if self == CardWound.WOUND_GRAVE:
            return CardWound.WOUND_DIRE
        return None


CardComposite = Union[Card, CardWound]


def card_title(card: CardComposite) -> str:
    if isinstance(card, Card):
        return str(card)
    if isinstance(card, CardWound):
        return str(card)
    return ""


def card_wound_penalty_value(card: CardComposite) -> int:
    if isinstance(card, Card):
        return 0
    if isinstance(card, CardWound):
        if card == CardWound.WOUND_MINOR:
            return RewardConstants.WOUND_PENALTY_MINOR
        if card == CardWound.WOUND_ACUTE:
            return RewardConstants.WOUND_PENALTY_ACUTE
        if card == CardWound.WOUND_GRAVE:
            return RewardConstants.WOUND_PENALTY_GRAVE
        if card == CardWound.WOUND_DIRE:
            return RewardConstants.WOUND_PENALTY_DIRE
    return 0


def card_regular(card: CardComposite) -> Optional[Card]:
    if isinstance(card, Card):
        return card
    return None


def card_wound(card: CardComposite) -> Optional[CardWound]:
    if isinstance(card, CardWound):
        return card
    return None


def card_ordinal(card: CardComposite) -> int:
    if isinstance(card, Card):
        return card.value
    if isinstance(card, CardWound):
        return card.value
    return 0
