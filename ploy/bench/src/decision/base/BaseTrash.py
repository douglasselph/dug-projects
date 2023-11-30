from typing import List, Optional

from src.data.Card import Card, CardComposite
from src.data.Player import Player


class BaseTrash:

    def select_card_to_trash(self, cards: List[CardComposite], player: Player) -> Optional[Card]:
        return None
