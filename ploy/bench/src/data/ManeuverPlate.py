# package src.data
from typing import List
from enum import Enum
from Card import Card, CardComposite, card_ordinal


class IntentionID(Enum):
    NONE = 0
    FACE_DOWN = 1
    ATTACK = 2
    DEFEND = 3
    DEPLOY = 4


class LineID(Enum):
    NONE = 0
    LINE_1 = 1
    LINE_2 = 2
    LINE_3 = 3
    LINE_4 = 4
    LINE_5 = 5
    LINE_6 = 6


class Line:
    maxSize: int
    intention: IntentionID
    intentionFaceUp: bool
    cards: List[CardComposite]
    cardsFaceUp: bool

    def __init__(self, max_size: int):
        self.maxSize = max_size
        self.intention = IntentionID.NONE
        self.intentionFaceUp = False
        self.cards = []
        self.cardsFaceUp = False

    def add(self, card: CardComposite) -> bool:
        self.cards.append(card)
        return len(self.cards) <= self.maxSize

    def can_add(self) -> bool:
        return len(self.cards) < self.maxSize

    def replace(self, card: CardComposite, position: int):
        self.cards[position] = card

    def legal(self, position: int) -> bool:
        return position < self.maxSize

    def has_card(self, position: int) -> bool:
        return self.legal(position) and self.cards[position] != Card.NONE

    def query(self, position: int) -> CardComposite:
        return self.cards[position]

    def pull(self, position: int) -> CardComposite:
        return self.cards.pop(position)

    def is_intention_face_up(self) -> bool:
        return self.intentionFaceUp

    def are_cards_face_up(self) -> bool:
        return self.cardsFaceUp

    def show_intention(self):
        self.intentionFaceUp = True

    def show_cards(self):
        self.cardsFaceUp = True

    def discard(self):
        discarded_cards = self.cards
        self.cards = []
        self.intention = IntentionID.NONE
        self.intentionFaceUp = False
        self.cardsFaceUp = False
        return discarded_cards

    def set_intention(self, coin: IntentionID) -> bool:
        if coin == IntentionID.NONE:
            return True
        if len(self.cards) > 0 and self.intention != IntentionID.NONE:
            return False
        self.intention = coin
        return True


class ManeuverPlate:
    central_maneuver_card: Card
    level: int
    lines: List[Line]

    def __init__(self):
        self.central_maneuver_card = Card.MANEUVER_BUST_A_CUT
        self.level = 1
        self.lines = [Line(5), Line(4), Line(3), Line(3)]

    def add_card(self, line: LineID, card: CardComposite, coin: IntentionID) -> bool:
        pos = self._position(line)
        self.lines[pos].set_intention(coin)
        return self.lines[pos].add(card)

    # Return
    def agent_line_values(self) -> List[int]:
        card_values = []

        for line in self.lines:
            card_values.append(line.intention.value)
            for position in range(line.maxSize):
                if line.has_card(position):
                    card = line.query(position)
                    card_values.append(card_ordinal(card))
                else:
                    card_values.append(0)  # Append 0 if no card is present

        return card_values

    def opponent_line_values(self) -> List[int]:
        card_values = []

        for line in self.lines:
            if line.is_intention_face_up():
                card_values.append(line.intention.value)
            else:
                card_values.append(IntentionID.FACE_DOWN.value)
            for position in range(line.maxSize):
                if line.has_card(position):
                    if line.are_cards_face_up():
                        card = line.query(position)
                        card_values.append(card_ordinal(card))
                    else:
                        card_values.append(card_ordinal(Card.FACE_DOWN))
                else:
                    card_values.append(0)  # Append 0 if no card is present

        return card_values

    @staticmethod
    def _position(line: LineID) -> int:
        return line.value - 1
