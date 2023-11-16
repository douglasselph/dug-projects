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
    _intention: IntentionID
    _intentionFaceUp: bool
    _cards: List[CardComposite]
    _cardsFaceUp: bool

    def __init__(self, max_size: int):
        self.maxSize = max_size
        self._intention = IntentionID.NONE
        self._intentionFaceUp = False
        self._cards = []
        self._cardsFaceUp = False

    def add(self, card: CardComposite) -> bool:
        self._cards.append(card)
        return len(self._cards) <= self.maxSize

    def can_add(self) -> bool:
        return len(self._cards) < self.maxSize

    def replace(self, card: CardComposite, position: int):
        self._cards[position] = card

    def legal(self, position: int) -> bool:
        return position < self.maxSize

    def has_card(self, position: int) -> bool:
        return self.legal(position) and self._cards[position] != Card.NONE

    def query(self, position: int) -> CardComposite:
        return self._cards[position]

    def pull(self, position: int) -> CardComposite:
        return self._cards.pop(position)

    def is_intention_face_up(self) -> bool:
        return self._intentionFaceUp

    def are_cards_face_up(self) -> bool:
        return self._cardsFaceUp

    def show_intention(self):
        self._intentionFaceUp = True

    def show_cards(self):
        self._cardsFaceUp = True

    def discard(self):
        discarded_cards = self._cards
        self._cards = []
        self._intention = IntentionID.NONE
        self._intentionFaceUp = False
        self._cardsFaceUp = False
        return discarded_cards

    def set_intention(self, coin: IntentionID) -> bool:
        if coin == IntentionID.NONE:
            return True
        if len(self._cards) > 0 and self._intention != IntentionID.NONE:
            return False
        self._intention = coin
        return True

    @property
    def intention(self) -> IntentionID:
        return self._intention


class ManeuverPlate:
    central_maneuver_card: Card
    level: int
    _lines: List[Line]

    def __init__(self):
        self.central_maneuver_card = Card.MANEUVER_BUST_A_CUT
        self.level = 1
        self._lines = [Line(5), Line(4), Line(3), Line(3)]

    def add_card(self, line: LineID, card: CardComposite, coin: IntentionID) -> bool:
        pos = self._position(line)
        self._lines[pos].set_intention(coin)
        return self._lines[pos].add(card)

    # Return
    def player_line_values(self) -> List[int]:
        card_values = []

        for line in self._lines:
            card_values.append(line.intention.value)
            for position in range(line.maxSize):
                if line.has_card(position):
                    card = line.query(position)
                    card_values.append(card_ordinal(card))
                else:
                    card_values.append(0)  # Append 0 if no card is present

        return card_values

    def other_line_values(self) -> List[int]:
        card_values = []

        for line in self._lines:
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
