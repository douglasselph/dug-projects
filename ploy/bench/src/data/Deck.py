# package src.data
from __future__ import annotations
from typing import List, Optional
from src.data.Card import CardComposite, Card, card_ordinal


class Deck:

    size: int
    _draw: List[CardComposite]
    _faceUp: List[CardComposite]

    def __init__(self, size: int):
        self.size = size
        self._draw = [Card.NONE] * size
        self._faceUp = []

    def append(self, value: CardComposite) -> Deck:
        # Scan for the next card in the array that is available
        for i in range(0, len(self._draw)):
            if self._draw[i] == Card.NONE:
                self._draw[i] = value
                break
        return self

    def draw(self) -> CardComposite:
        card = self._draw[0]
        self._faceUp.append(self._draw.pop(0))
        return card

    @property
    def can_draw(self) -> bool:
        return len(self._draw) > 0

    # Pull the card that is top most on the deck
    def query_face_up_card(self) -> Optional[CardComposite]:
        if len(self._faceUp) <= 0:
            return None
        return self._faceUp[len(self._faceUp)-1]

    def pull_face_up_card(self) -> Optional[CardComposite]:
        if len(self._faceUp) <= 0:
            return None
        return self._faceUp.pop(len(self._faceUp)-1)

    # From the face up cards, form a hand of the given size cards.
    # Ensure the array returned is of size 'size'
    def hand(self, size: int) -> List[int]:
        value_array = [card_ordinal(card) for card in self._faceUp[:size]]
        value_array += [0] * (size - len(value_array))
        return value_array

    # From the draw deck, form a collection of cards of the given size.
    # Ensure the array returned is of size 'size'
    def draw_deck(self, size: int) -> List[int]:
        value_array = [card_ordinal(card) for card in self._draw[:size]]
        value_array += [0] * (size - len(value_array))
        return value_array
