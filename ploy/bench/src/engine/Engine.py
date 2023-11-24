from src.data.Decision import DecisionLine, DecisionIntention
from src.data.Game import Game, PlayerID
from src.data.Player import Player
from src.data.Card import Card, CardWound, DieSides
from src.engine.Die import Die
from src.engine.IncidentBundle import IncidentBundle


def _apply_secondary_affliction(player: Player, sides: DieSides):
    value = Die(sides).roll()
    if value == 1:
        player.pips = 0
    elif value == 2:
        player.draw_one_less_card = True
    elif value == 3:
        player.discard_all()
    elif value == 4:
        player.energy -= 1
    elif value == 5:
        player.energy -= 1
    elif value == 6:
        player.upgrade_lowest_wound()
    elif value == 7:
        player.add_penalty_coin()
    elif value == 8:
        player.reduce_reach()
    elif value == 9:
        card = player.trash_random_card()
        add_card_to_trash(card)
    elif value == 10:
        player.reduce_reach()
        player.reduce_reach()
    elif value == 11:
        player.upgrade_highest_wound()
    else:
        _apply_secondary_affliction(player, sides)
        _apply_secondary_affliction(player, sides)


class Engine:

    def __init__(self, game: Game):
        self.game = game

    ############################################################################
    # Draw Hand of 4 maneuver for all players.
    def draw_hands(self):
        self.game.agentPlayer.draw_hand()
        self.game.opponent.draw_hand()

    ############################################################################
    # Place the next card in the draw hand to the indicated place on the plate.
    # Return True if successful.
    # Return False if illegal indication.
    def agent_place_card(self, line: DecisionLine, coin: DecisionIntention) -> bool:
        return self._place_card(self.game.agentPlayer, line, coin)

    def opponent_place_card(self, line: DecisionLine, coin: DecisionIntention) -> bool:
        return self._place_card(self.game.opponent, line, coin)

    @staticmethod
    def _place_card(pl: Player, line: DecisionLine, coin: DecisionIntention) -> bool:
        if not pl.is_legal(line, coin):
            return False

        return pl.play_to_plate(line, coin)

    def agent_has_cards_to_place(self) -> bool:
        return self.game.agentPlayer.has_cards_to_play

    def opponent_has_cards_to_place(self) -> bool:
        return self.game.opponent.has_cards_to_play

    ###############################################################################
    # Reveal: any plate line at max has its intention coin revealed
    def reveal_intentions(self):
        self._reveal_intentions(self.game.agentPlayer)
        self._reveal_intentions(self.game.opponent)

    @staticmethod
    def _reveal_intentions(player: Player):
        if player.has_cards_to_draw:
            player.reveal_intentions_if_maxed()
        else:
            player.reveal_all_intentions()

    ###############################################################################
    # Attack Resolve:
    #   In initiative order:
    #     Attacker reveal supporting lines
    #     Defender reveal supporting lines
    #     Resolve
    def resolve_attacks(self):
        if self.game.initiativeOn == PlayerID.PLAYER_1:
            self._resolve_attack(self.game.agentPlayer, self.game.opponent)
            if self._resolve_attack(self.game.opponent, self.game.agentPlayer):
                self.game.initiativeOn = PlayerID.PLAYER_2
        else:
            self._resolve_attack(self.game.opponent, self.game.agentPlayer)
            if self._resolve_attack(self.game.agentPlayer, self.game.opponent):
                self.game.initiativeOn = PlayerID.PLAYER_1

    def _resolve_attack(self, attacker: Player, defender: Player) -> bool:

        if not attacker.has_intention(DecisionIntention.ATTACK):
            return False

        attacker.reveal_intentions_with_intention(DecisionIntention.ATTACK)
        defender.reveal_intentions_with_intention(DecisionIntention.DEFEND)
        attacker.reveal_cards_with_revealed_intentions()
        defender.reveal_cards_with_revealed_intentions()

        attacker.apply_to_die_four(DecisionIntention.ATTACK)
        defender.apply_to_die_four(DecisionIntention.DEFEND)

        attacker.apply_feeling_feint(DecisionIntention.ATTACK)
        defender.apply_feeling_feint(DecisionIntention.DEFEND)

        incident = IncidentBundle()
        incident.attacker_cards = attacker.collect_face_up_cards_for(DecisionIntention.ATTACK)
        incident.defender_cards = defender.collect_face_up_cards_for(DecisionIntention.DEFEND)
        incident.attacker_cards.append(attacker.central_maneuver_card)
        incident.defender_cards.append(defender.central_maneuver_card)
        incident.attacker_pull_dice()
        incident.defender_pull_dice()
        incident.apply_cards_pre_roll()
        incident.roll()
        incident.apply_cards_post_roll()

        wounds = incident.attacker_total - incident.defender_total

        if wounds >= 0:
            self._apply_wound_to(defender, wounds, attacker)
        else:
            self._apply_wound_to(attacker, wounds, defender)

        return True

    @staticmethod
    def _apply_wound_to(player: Player, wounds: int, attacker: Player):
        if wounds <= 6:
            return
        if wounds <= 16:
            player.draw.append(CardWound.WOUND_MINOR)
        elif wounds <= 22:
            player.draw.append(CardWound.WOUND_MINOR)
            _apply_secondary_affliction(player, DieSides.D4)
        elif wounds <= 29:
            player.draw.append(CardWound.WOUND_ACUTE)
            _apply_secondary_affliction(player, DieSides.D6)
        elif wounds <= 36:
            player.draw.append(CardWound.WOUND_ACUTE)
            _apply_secondary_affliction(player, DieSides.D8)
        elif wounds <= 44:
            player.draw.append(CardWound.WOUND_GRAVE)
            _apply_secondary_affliction(player, DieSides.D10)
        elif wounds <= 52:
            player.draw.append(CardWound.WOUND_GRAVE)
            _apply_secondary_affliction(player, DieSides.D12)
        elif wounds <= 59:
            player.draw.append(CardWound.WOUND_DIRE)
            _apply_secondary_affliction(player, DieSides.D12)
        elif wounds <= 69:
            player.fatal_received = True
            pass
        elif wounds <= 76:
            player.fatal_received = True
            attacker.energy -= 1
            pass
        elif wounds <= 84:
            player.fatal_received = True
            attacker.energy -= 2
            pass
        else:
            player.fatal_received = True
            attacker.energy -= 3
            pass

