# package src.data
from __future__ import annotations
from typing import List, Optional
from src.data.Card import CardComposite, Card, card_ordinal


class Deck:

    _separator = -2

    _draw: List[CardComposite]
    _faceUp: List[CardComposite]

    def __init__(self):
        self._draw = []
        self._faceUp = []

    # Add a card to the bottom of the draw deck.
    def append(self, value: CardComposite) -> Deck:
        self._draw.append(value)
        return self

    def draw(self) -> CardComposite:
        card = self._draw.pop(0)
        self._faceUp.insert(0, card)
        return card

    @property
    def can_draw(self) -> bool:
        return len(self._draw) > 0

    # Pull the card that is top most on the deck
    def query_face_up_card(self) -> Optional[CardComposite]:
        if len(self._faceUp) <= 0:
            return None
        return self._faceUp[0]

    def pull_face_up_card(self) -> Optional[CardComposite]:
        if len(self._faceUp) <= 0:
            return None
        return self._faceUp.pop(0)

    #
    # Return a neural net conditioned array of numbers representing a known deck.
    # Ensure the array returned is of size 'size'.
    #
    # The order is the top card is the top most face card, following by the rest of the face up cards,
    # followed by the seperator number followed by the face down cards. Extra slots are padded with zeros.
    #
    def nn_value_all_visible(self, size: int) -> List[int]:
        face_up_array = [card_ordinal(card) for card in self._faceUp]
        face_down_array = [card_ordinal(card) for card in self._draw]
        combined = face_up_array + [self._separator] + face_down_array
        if len(combined) > size:
            combined = combined[:size]
        else:
            combined += [0] * (size - len(combined))
        return combined

    #
    # Return a neural net conditioned array of representing a unknown deck.
    # Ensure the array returned is of size 'size'.
    #
    # The first cards will be the face up cards, which are all known, where the first card is on top.
    # Then the seperator.
    # Then a simple integer indicating the number of cards left in the deck.
    #
    def nn_value_hidden_draw(self, size: int) -> List[int]:
        face_up_array = [card_ordinal(card) for card in self._faceUp]
        combined = face_up_array + [self._separator] + [len(self._draw)]
        if len(combined) > size:
            face_up_array = [card_ordinal(card) for card in self._faceUp[:size-2]]
            combined = face_up_array + [self._separator] + [len(self._draw)]
        else:
            combined += [0] * (size - len(combined))
        return combined

