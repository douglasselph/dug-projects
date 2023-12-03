from __future__ import annotations

from src.data.Card import Card
from src.data.Decision import DecisionDeck
from src.decision.base.BaseDeployBlock import BaseDeployBlock


class ValidateDeployBlock(BaseDeployBlock):

    def acquire_block_value(self, decision: DecisionDeck, card: Card) -> int:

        if self.player.pips > card.cost.pips:
            block_value = self.player.pips - card.cost.pips
        else:
            block_value = 0

        return block_value
