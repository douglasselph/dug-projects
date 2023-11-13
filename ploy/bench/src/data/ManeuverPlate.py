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
    _cards: List[CardComposite]
    _faceUp: List[bool]
    maxSize: int

    def __init__(self, max_size: int):
        self.maxSize = max_size
        self._cards = []
        self._faceUp = []

    def add(self, card: CardComposite) -> bool:
        self._cards.append(card)
        self._faceUp.append(False)
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
        self._faceUp.pop(position)
        return self._cards.pop(position)

    def is_face_up(self, position: int) -> bool:
        return self._faceUp[position]

    def has_face_up(self) -> bool:
        return any(self._faceUp)

    def show(self, position: int):
        self._faceUp[position] = True

    def discard_face_up(self):
        discarded_cards = []
        new_cards = []
        new_face_up = []

        for card, is_face_up in zip(self._cards, self._faceUp):
            if is_face_up:
                discarded_cards.append(card)
            else:
                new_cards.append(card)
                new_face_up.append(is_face_up)

        self._cards = new_cards
        self._faceUp = new_face_up

        return discarded_cards


class ManeuverPlate:
    central_maneuver_card: Card
    level: int
    _lines: List[Line]
    _coins: List[IntentionID]

    def __init__(self):
        self.central_maneuver_card = Card.MANEUVER_BUST_A_CUT
        self.level = 1
        self._lines = [Line(5), Line(4), Line(3), Line(3)]
        self._coins = [IntentionID.NONE, IntentionID.NONE, IntentionID.NONE, IntentionID.NONE]

    def add_card(self, line: LineID, card: CardComposite, coin: IntentionID) -> bool:
        pos = self._position(line)
        self._coins[pos] = coin
        return self._lines[pos].add(card)

    # Return
    def all_card_values(self) -> List[int]:
        card_values = []

        for line in self._lines:
            for position in range(line.maxSize):
                if line.has_card(position):
                    card = line.query(position)
                    card_values.append(card_ordinal(card))
                else:
                    card_values.append(0)  # Append 0 if no card is present

        return card_values

    def visible_card_values(self) -> List[int]:
        card_values = []

        for line in self._lines:
            for position in range(line.maxSize):
                if line.has_card(position):
                    if line.is_face_up(position):
                        card = line.query(position)
                        card_values.append(card_ordinal(card))
                    else:
                        card_values.append(card_ordinal(Card.FACE_DOWN))
                else:
                    card_values.append(0)  # Append 0 if no card is present

        return card_values

    def all_intention_values(self) -> List[int]:
        intention_values = []
        for coin in self._coins:
            intention_values.append(coin.value)
        return intention_values

    # Return IntentionID.FACE_DOWN if no cards in the line are face up.
    def visible_intention_values(self) -> List:
        intention_values = []
        for index, coin in enumerate(self._coins):
            if self._lines[index].has_face_up():
                intention_values.append(coin.value)
            else:
                intention_values.append(IntentionID.FACE_DOWN)

        return intention_values

    @staticmethod
    def _position(line: LineID) -> int:
        return line.value - 1
