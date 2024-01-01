from __future__ import annotations

from src.data.Player import Player
from src.decision.base.BaseRevealSupportingLine import BaseRevealSupportingLine
from src.data.Decision import DecisionIntention


class ValidateRevealSupportingLine(BaseRevealSupportingLine):

    def apply(self, player: Player, coin: DecisionIntention):
        player.reveal_intentions_of(coin)
