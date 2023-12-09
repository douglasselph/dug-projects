# package src.data
from typing import List, Optional

from src.data.Card import Card, CardComposite, card_ordinal, DieSides, CardWound
from src.data.Decision import DecisionLine, DecisionIntention


class Line:

    maxSize: int
    intention: DecisionIntention
    intention_face_up: bool
    cards: List[CardComposite]
    cards_face_up: bool
    penalty: int

    def __init__(self, max_size: int):
        self.maxSize = max_size
        self.intention = DecisionIntention.NONE
        self.intention_face_up = False
        self.cards = []
        self.cards_face_up = False
        self.penalty = 0

    def add(self, card: CardComposite) -> bool:
        self.cards.append(card)
        return len(self.cards) <= self.limit

    @property
    def can_add(self) -> bool:
        return len(self.cards) < self.limit

    def can_take_card(self, position: int) -> bool:
        return position < self.limit

    def has_card(self, position: int) -> bool:
        return position < len(self.cards) and self.can_take_card(position) and self.cards[position] != Card.NONE

    def has_sides(self, sides: DieSides) -> bool:
        for card in self.cards:
            if isinstance(card, Card):
                if card.die_bonus == sides:
                    return True
        return False

    def remove_sides(self, sides: DieSides) -> Optional[Card]:
        for card in self.cards:
            if isinstance(card, Card):
                if card.die_bonus == sides:
                    self.cards.remove(card)
                    return card
        return None

    def query(self, position: int) -> Optional[CardComposite]:
        if position >= len(self.cards):
            return Card.NONE
        return self.cards[position]

    def pull(self, position: int) -> Optional[CardComposite]:
        if position >= len(self.cards):
            return Card.NONE
        return self.cards.pop(position)

    @property
    def is_at_max(self) -> bool:
        return len(self.cards) >= self.limit

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
        return len(self.cards) < self.limit

    @property
    def limit(self) -> int:
        return self.maxSize - self.penalty

    def collect_dice(self) -> List[DieSides]:
        result: List[DieSides] = []
        for card in self.cards:
            bonus = card.die_bonus
            if bonus != DieSides.NONE:
                result.append(bonus)
        return result


class ManeuverPlate:

    held_coins_per_intention = 3
    initial_line_card_sizes = [5, 4, 3, 3]
    central_maneuver_card: Card
    level: int
    lines: List[Line]
    penalty_coins: List[DecisionIntention]

    def __init__(self):
        self.central_maneuver_card = Card.MANEUVER_BUST_A_CUT
        self.level = 1
        self.lines = []
        self.penalty_coins = []
        for size in self.initial_line_card_sizes:
            self.lines.append(Line(size))

    def add_card(self, card: CardComposite, line: DecisionLine, coin: DecisionIntention):
        pos = line.pos
        if self.lines[pos].intention == DecisionIntention.NONE or len(self.lines[pos].cards) == 0:
            self.lines[pos].set_intention(coin)
        self.lines[pos].add(card)

    def is_set_intention_legal(self, line: DecisionLine, coin: DecisionIntention) -> bool:
        pos = line.pos
        line = self.lines[pos]
        if not line.is_set_intention_legal:
            return False
        count = self.num_used_intentions_coins_for(coin)
        return count < self.num_held_intention_coins_for(coin)

    def num_held_intention_coins_for(self, coin: DecisionIntention) -> int:
        penalty = 0
        for check in self.penalty_coins:
            if check == coin:
                penalty += 1
        return self.held_coins_per_intention - penalty

    def num_used_intentions_coins_for(self, coin: DecisionIntention) -> int:
        count = 0
        for line in self.lines:
            if line.intention == coin:
                count += 1
        return count

    def is_add_card_legal(self, line: DecisionLine) -> bool:
        return self.lines[line.pos].is_add_card_legal

    @property
    def lines_num_cards(self) -> List[int]:
        num_cards = []
        for line in self.lines:
            num_cards.append(len(line.cards))
        return num_cards

    def line_intention_of(self, line: DecisionLine) -> DecisionIntention:
        return self.lines[line.pos].intention

    def nn_line_card_values(self, line: DecisionLine) -> List[int]:
        card_values = []
        for_line = self.lines[line.pos]
        for position in range(for_line.maxSize):
            if for_line.has_card(position):
                card = for_line.query(position)
                card_values.append(card_ordinal(card))
            else:
                card_values.append(0)  # Append 0 if no card is present
        return card_values

    def set_intention_face_up(self, line: DecisionLine):
        self.lines[line.pos].intention_face_up = True

    def set_line_face_up(self, line: DecisionLine):
        self.lines[line.pos].cards_face_up = True

    def reveal_all_intentions(self):
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

    def has_revealed_intention(self, coin: DecisionIntention) -> bool:
        for line in self.lines:
            if line.intention == coin and line.intention_face_up:
                return True
        return False

    def has_face_up_sides(self, intention: DecisionIntention, sides: DieSides) -> bool:
        for line in self.lines:
            if line.cards_face_up and line.intention == intention:
                if line.has_sides(sides):
                    return True
        return False

    def remove_sides_on_face_up(self, intention: DecisionIntention, sides: DieSides) -> Optional[Card]:
        for line in self.lines:
            if line.cards_face_up and line.intention == intention:
                card = line.remove_sides(sides)
                if card:
                    return card
        return None

    def discard_face_up(self) -> List[CardComposite]:
        cards: List[CardComposite] = []
        for line in self.lines:
            if line.cards_face_up:
                cards.extend(line.discard())
                line.cards_face_up = False
        return cards

    def discard_all(self) -> List[CardComposite]:
        cards: List[CardComposite] = []
        for line in self.lines:
            line.cards_face_up = True
            cards.extend(line.discard())
        return cards

    @property
    def wounds_face_up(self) -> List[CardWound]:
        cards: List[CardWound] = []
        for line in self.lines:
            if line.cards_face_up:
                for card in line.cards:
                    if isinstance(card, CardWound):
                        cards.append(card)
        return cards

    def replace_wound_face_up(self, wound: CardWound, new_wound: CardWound) -> bool:
        for line in self.lines:
            if line.cards_face_up:
                for card in line.cards:
                    if card == wound:
                        line.remove(card)
                        line.add(new_wound)
                        return True
        return False

    def reduce_reach_on(self, line: DecisionLine):
        self.lines[line.pos].penalty += 1

    def remove_on_face_up(self, match: CardComposite) -> Optional[Line]:
        for line in self.lines:
            if line.cards_face_up:
                for card in line.cards:
                    if card == match:
                        line.remove(card)
                        return line
        return None

    @property
    def face_up_lines(self) -> List[Line]:
        result: List[Line] = []
        for line in self.lines:
            if line.cards_face_up:
                result.append(line)
        return result

