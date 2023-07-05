from src.deck import Deck
from src.stats import Stats


class Game:

    deck: Deck

    def __init__(self, deck: Deck):
        self.deck = deck

    def rounds(self, count: int) -> Stats:

        stats = Stats()

        for _ in range(count):
            hand = self.deck.deal()
            self.deck.next_turn_add(hand.use_next_turn_instead())
            stats.min(hand.min())
            stats.max(hand.max())
            stats.average(hand.average())

        return stats


