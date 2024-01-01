

from src.decision.base.BaseRevealSupportingLine import BaseRevealSupportingLine
from src.data.Decision import DecisionIntention
from src.data.Player import Player


class TestRevealSupportingLine(BaseRevealSupportingLine):

    def __init__(self):
        self.got_apply = False

    def apply(self, player: Player, coin: DecisionIntention):
        player.reveal_intentions_of(coin)
        self.got_apply = True
