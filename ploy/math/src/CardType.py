from typing import List, Optional
from enum import Enum


class CardType(Enum):
    def __str__(self):
        return self.value


class BonusType(CardType):
    CARD_BONUS = "card_bonus"
    REACH_BONUS = "reach_bonus"
    CARD_DRAW_BONUS = "card_draw_bonus"


class CardTypeManeuver(CardType):
    BUST_A_CUT = "bust_a_cut"
    PRECISION = "precision"
    FEELING_FEINT = "feeling_feint"
    TO_DIE_FOUR = "to_die_four"
    IN_HEW_OF = "in_hew_of"
    CUTTING_RIPOSTE = "cutting_riposte"
    NICK_TO_DEATH = "nick_to_death"
    KEEP_THE_PIERCE = "keep_the_pierce"


class CardTypeDie(CardType):
    D20 = "D20"
    D12 = "D12"
    D10 = "D10"
    D8 = "D8"
    D6 = "D6"
    D4 = "D4"


def card_type_possibilities_string() -> List[str]:
    values_list: List[str] = []
    for subclass in CardType.__subclasses__():
        for item in subclass:
            values_list.append(str(item))
    return values_list


def card_type_from(s: str) -> Optional[CardType]:
    for subclass in CardType.__subclasses__():
        for item in subclass:
            if str(item) == s:
                return item
    return None



