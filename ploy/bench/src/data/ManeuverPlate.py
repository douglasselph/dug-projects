# package src.data
from typing import List

from src.data.Card import Card, CardComposite, card_ordinal, DieSides
from src.data.Decision import DecisionLine, DecisionIntention
from src.data.maneuver.ManeuverFeelingFeint import maneuver_feeling_feint


class Line:
    maxSize: int
    intention: DecisionIntention
    intention_face_up: bool
    cards: List[CardComposite]
    cards_face_up: bool

    def __init__(self, max_size: int):
        self.maxSize = max_size
        self.intention = DecisionIntention.NONE
        self.intention_face_up = False
        self.cards = []
        self.cards_face_up = False

    def add(self, card: CardComposite) -> bool:
        self.cards.append(card)
        return len(self.cards) <= self.maxSize

    def can_add(self) -> bool:
        return len(self.cards) < self.maxSize

    def replace(self, card: CardComposite, position: int):
        self.cards[position] = card

    def can_take_card(self, position: int) -> bool:
        return position < self.maxSize

    def has_card(self, position: int) -> bool:
        return self.can_take_card(position) and self.cards[position] != Card.NONE

    def query(self, position: int) -> CardComposite:
        return self.cards[position]

    def pull(self, position: int) -> CardComposite:
        return self.cards.pop(position)

    @property
    def is_at_max(self) -> bool:
        return len(self.cards) >= self.maxSize

    def discard(self) -> List[CardComposite]:
        discarded_cards = self.cards
        self.cards = []
        self.intention = DecisionIntention.NONE
        self.intention_face_up = False
        self.cards_face_up = False
        return discarded_cards

    def remove(self, card: Card):
        self.cards.remove(card)

    def set_intention(self, coin: DecisionIntention):
        self.intention = coin

    @property
    def is_set_intention_legal(self) -> bool:
        return self.intention == DecisionIntention.NONE

    @property
    def is_add_card_legal(self) -> bool:
        return len(self.cards) < self.maxSize

    def collect_dice(self) -> List[DieSides]:
        result: List[DieSides] = []
        for card in self.cards:
            bonus = card.die_bonus
            if bonus != DieSides.NONE:
                result.append(bonus)
        return result


class ManeuverPlate:
    line_card_sizes = [5, 4, 3, 3]
    central_maneuver_card: Card
    level: int
    lines: List[Line]

    def __init__(self):
        self.central_maneuver_card = Card.MANEUVER_BUST_A_CUT
        self.level = 1
        self.lines = []
        for size in self.line_card_sizes:
            self.lines.append(Line(size))

    def add_card(self, card: CardComposite, line: DecisionLine, coin: DecisionIntention):
        pos = self._position(line)
        self.lines[pos].set_intention(coin)
        self.lines[pos].add(card)

    def is_set_intention_legal(self, line: DecisionLine) -> bool:
        pos = self._position(line)
        return self.lines[pos].is_set_intention_legal

    def is_add_card_legal(self, line: DecisionLine) -> bool:
        pos = self._position(line)
        return self.lines[pos].is_add_card_legal

    @property
    def line_sizes(self) -> List[int]:
        sizes = []
        for line in self.lines:
            sizes.append(line.maxSize)
        return sizes

    @property
    def lines_num_cards(self) -> List[int]:
        num_cards = []
        for line in self.lines:
            num_cards.append(len(line.cards))
        return num_cards

    def line_intention_id(self, position: int) -> DecisionIntention:
        return self.lines[position].intention

    def line_card_values(self, position: int) -> List[int]:
        card_values = []
        line = self.lines[position]
        for position in range(line.maxSize):
            if line.has_card(position):
                card = line.query(position)
                card_values.append(card_ordinal(card))
            else:
                card_values.append(0)  # Append 0 if no card is present
        return card_values

    def set_intention_face_up(self, line: DecisionLine):
        pos = self._position(line)
        self.lines[pos].intention_face_up = True

    def set_line_face_up(self, line: DecisionLine):
        pos = self._position(line)
        self.lines[pos].cards_face_up = True

    def reveal_intention_on_all_lines(self):
        for line in self.lines:
            line.intention_face_up = True

    def reveal_intentions_if_maxed(self):
        for line in self.lines:
            if line.is_at_max:
                line.intention_face_up = True

    def reveal_intentions_of(self, coin: DecisionIntention):
        for line in self.lines:
            if line.intention == coin:
                line.intention_face_up = True

    def reveal_cards_with_revealed_intentions(self):
        for line in self.lines:
            if line.intention_face_up:
                line.cards_face_up = True

    def collect_dice_for(self, coin: DecisionIntention) -> List[DieSides]:
        result: List[DieSides] = []
        for line in self.lines:
            if line.cards_face_up and line.intention == coin:
                result.extend(line.collect_dice())
        return result

    def collect_face_up_cards_for(self, coin: DecisionIntention) -> List[CardComposite]:
        result: List[CardComposite] = []
        for line in self.lines:
            if line.cards_face_up and line.intention == coin:
                result.extend(line.cards)
        return result

    def has_intention(self, coin: DecisionIntention) -> bool:
        for line in self.lines:
            if line.intention == coin:
                return True
        return False

    def discard(self) -> List[CardComposite]:
        cards: List[CardComposite] = []
        for line in self.lines:
            if line.cards_face_up:
                cards.extend(line.discard())
        return cards

    def discard_all(self) -> List[CardComposite]:
        cards: List[CardComposite] = []
        for line in self.lines:
            line.cards_face_up = True
            cards.extend(line.discard())
        return cards

    def apply_feeling_feint(self, coin: DecisionIntention):
        times = 0
        for line in self.lines:
            if line.intention == coin:
                for card in line.cards:
                    if card == Card.MANEUVER_FEELING_FEINT:
                        times += 1
        for time in range(times):
            maneuver_feeling_feint(self, coin)

    @staticmethod
    def _position(line: DecisionLine) -> int:
        return line.value - 1
