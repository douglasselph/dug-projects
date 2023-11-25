import random
from typing import List, Optional
from src.data.Decision import DecisionLine, DecisionIntention
from src.data.Game import Game, PlayerID
from src.data.Player import Player
from src.data.Card import Card, CardWound, DieSides, CardComposite, TrashBonusDie, TrashBonus, TrashBonusPips
from src.engine.Die import Die
from src.engine.IncidentBundle import IncidentBundle


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

        bonus_attacker = self._apply_attacker_trash_pre_roll(incident, attacker)
        bonus_defender = self._apply_defender_trash_pre_roll(incident, defender)

        incident.apply_cards_pre_roll()
        incident.roll()
        incident.apply_cards_post_roll()

        self._apply_attacker_trash_post_roll(incident, bonus_attacker)
        self._apply_defender_trash_post_roll(incident, bonus_defender)

        wounds = incident.attacker_total - incident.defender_total

        if wounds >= 0:
            self._apply_wound_to(defender, wounds, attacker)
        else:
            self._apply_wound_to(attacker, wounds, defender)

        return True

    def _apply_wound_to(self, player: Player, wounds: int, attacker: Player):
        if wounds <= 6:
            return
        if wounds <= 16:
            player.draw.append(CardWound.WOUND_MINOR)
        elif wounds <= 22:
            player.draw.append(CardWound.WOUND_MINOR)
            self._apply_secondary_affliction(player, DieSides.D4)
        elif wounds <= 29:
            player.draw.append(CardWound.WOUND_ACUTE)
            self._apply_secondary_affliction(player, DieSides.D6)
        elif wounds <= 36:
            player.draw.append(CardWound.WOUND_ACUTE)
            self._apply_secondary_affliction(player, DieSides.D8)
        elif wounds <= 44:
            player.draw.append(CardWound.WOUND_GRAVE)
            self._apply_secondary_affliction(player, DieSides.D10)
        elif wounds <= 52:
            player.draw.append(CardWound.WOUND_GRAVE)
            self._apply_secondary_affliction(player, DieSides.D12)
        elif wounds <= 59:
            player.draw.append(CardWound.WOUND_DIRE)
            self._apply_secondary_affliction(player, DieSides.D12)
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

    def _apply_secondary_affliction(self, player: Player, sides: DieSides):
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
            self._add_card_to_trash(card)
        elif value == 10:
            player.reduce_reach()
            player.reduce_reach()
        elif value == 11:
            player.upgrade_highest_wound()
        else:
            self._apply_secondary_affliction(player, sides)
            self._apply_secondary_affliction(player, sides)

    def _add_card_to_trash(self, card: Card):
        self.game.trash.append(card)

    def _apply_attacker_trash_pre_roll(self, incident: IncidentBundle, attacker: Player) -> TrashBonus:
        bonus = self._acquire_attacker_bonus(incident, attacker)
        if isinstance(bonus, TrashBonusDie):
            incident.attacker_dice.add_die(bonus.sides)
        return bonus

    def _apply_defender_trash_pre_roll(self, incident: IncidentBundle, defender: Player) -> TrashBonus:
        bonus = self._acquire_defender_bonus(incident, defender)
        if isinstance(bonus, TrashBonusDie):
            incident.defender_dice.add_die(bonus.sides)
        return bonus

    @staticmethod
    def _apply_attacker_trash_post_roll(incident: IncidentBundle, bonus: TrashBonus):
        if isinstance(bonus, TrashBonusPips):
            incident.attacker_values.add(Die(DieSides.D4, bonus.pips))

    @staticmethod
    def _apply_defender_trash_post_roll(incident: IncidentBundle, bonus: TrashBonus):
        if isinstance(bonus, TrashBonusPips):
            incident.defender_values.add(Die(DieSides.D4, bonus.pips))

    def _acquire_attacker_bonus(self, incident: IncidentBundle, attacker: Player) -> TrashBonus:
        if not self._should_trash(incident.attacker_cards, attacker.num_draw_cards):
            return TrashBonus()
        card = self._select_card_to_trash(incident.attacker_cards)
        bonus = card.trash
        if isinstance(bonus, TrashBonusDie) or isinstance(bonus, TrashBonusPips):
            attacker.trash(card)
            self.game.trash.append(card)
        return bonus

    def _acquire_defender_bonus(self, incident: IncidentBundle, defender: Player) -> TrashBonus:
        if not self._should_trash(incident.defender_cards, defender.num_draw_cards):
            return TrashBonus()
        card = self._select_card_to_trash(incident.defender_cards)
        bonus = card.trash
        if isinstance(bonus, TrashBonusDie) or isinstance(bonus, TrashBonusPips):
            defender.trash(card)
            self.game.trash.append(card)
        return bonus

    def _should_trash(self, cards: List[CardComposite], num_cards: int) -> bool:
        current_average = self._average_of(cards)
        if current_average < 30 or current_average > 52:
            return False
        if num_cards < 14:
            return False
        chance = (current_average - 30) / 5
        roll = random.randint(1, 6)
        return roll <= chance

    @staticmethod
    def _average_of(cards: [CardComposite]) -> float:
        result = 0
        for card in cards:
            if isinstance(card, Card):
                result += card.die_bonus.average()
        return result

    @staticmethod
    def _select_card_to_trash(cards: List[CardComposite]) -> Optional[Card]:
        selected_card: Optional[Card] = None
        selected_card_value = 0
        for card in cards:
            if isinstance(card, Card):
                value = card.trash_choice_value
                if value > selected_card_value:
                    selected_card = card
                    selected_card_value = value
        return selected_card
