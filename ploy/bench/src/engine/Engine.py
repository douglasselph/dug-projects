import random
from typing import List, Optional
from src.data.Decision import DecisionLine, DecisionIntention
from src.data.Game import Game, PlayerID
from src.data.Player import Player
from src.data.Card import Card, CardWound, DieSides, CardComposite, TrashBonusDie, TrashBonus, TrashBonusPips, CardCost
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

        if defender.has_revealed_intention(DecisionIntention.DEFEND):
            defender.apply_feeling_feint(DecisionIntention.DEFEND)

        incident_attacker = IncidentBundle()
        incident_defender = IncidentBundle()
        incident_attacker.cards = attacker.collect_face_up_cards_for(DecisionIntention.ATTACK)
        incident_defender.cards = defender.collect_face_up_cards_for(DecisionIntention.DEFEND)
        incident_attacker.cards.append(attacker.central_maneuver_card)
        incident_defender.cards.append(defender.central_maneuver_card)
        incident_attacker.pull_dice()
        incident_defender.pull_dice()

        bonus_attacker = self._apply_trash_pre_roll(incident_attacker, attacker)
        bonus_defender = self._apply_trash_pre_roll(incident_defender, defender)

        incident_attacker.apply_cards_pre_roll(incident_defender.dice)
        incident_defender.apply_cards_pre_roll(incident_attacker.dice)

        incident_attacker.roll()
        incident_defender.roll()

        incident_attacker.apply_cards_post_roll(incident_defender.values)
        incident_defender.apply_cards_post_roll(incident_attacker.values)

        self._apply_trash_post_roll(incident_attacker, bonus_attacker)
        self._apply_trash_post_roll(incident_defender, bonus_defender)

        wounds = incident_attacker.total - incident_defender.total

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

    def _apply_trash_pre_roll(self, incident: IncidentBundle, player: Player) -> TrashBonus:
        bonus = self._acquire_bonus(incident, player)
        if isinstance(bonus, TrashBonusDie):
            incident.dice.add_die(bonus.sides)
        return bonus

    @staticmethod
    def _apply_trash_post_roll(incident: IncidentBundle, bonus: TrashBonus):
        if isinstance(bonus, TrashBonusPips):
            incident.values.add(Die(DieSides.D4, bonus.pips))

    def _acquire_bonus(self, incident: IncidentBundle, player: Player) -> TrashBonus:
        if not self._should_trash(incident.cards, player.num_cards_draw):
            return TrashBonus()
        card = self._select_card_to_trash(incident.cards)
        bonus = card.trash
        if isinstance(bonus, TrashBonusDie) or isinstance(bonus, TrashBonusPips):
            player.trash(card)
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

    ###############################################################################
    # Deploy Resolve:

    def resolve_deploy(self):

        self._resolve_deploy_gather_pips(self.game.agentPlayer)
        self._resolve_deploy_gather_pips(self.game.opponent)

        for count in range(10):
            if self.game.initiativeOn == PlayerID.PLAYER_1:
                buy1 = DeployBuy(self.game, self.game.agentPlayer, self.game.opponent)
                buy2 = DeployBuy(self.game, self.game.opponent, self.game.agentPlayer)
            else:
                buy2 = DeployBuy(self.game, self.game.opponent, self.game.agentPlayer)
                buy1 = DeployBuy(self.game, self.game.agentPlayer, self.game.opponent)

            if not buy1.did_activity and not buy2.did_activity:
                break

    @staticmethod
    def _resolve_deploy_gather_pips(player: Player):

        player.reveal_intentions_with_intention(DecisionIntention.DEPLOY)
        player.reveal_cards_with_revealed_intentions()
        player.apply_feeling_feint(DecisionIntention.DEPLOY)

        if player.has_revealed_intention(DecisionIntention.DEPLOY):
            player.apply_feeling_feint(DecisionIntention.DEPLOY)

        incident = IncidentBundle()
        incident.cards = player.collect_face_up_cards_for(DecisionIntention.DEPLOY)
        incident.cards.append(player.central_maneuver_card)
        incident.pull_dice()

        incident.apply_cards_pre_roll(incident.dice)
        incident.roll()
        incident.apply_cards_post_roll(incident.values)

        player.pips += incident.total

    ###############################################################################
    # Cleanup:
    def cleanup(self):

        self.game.agentPlayer.discard()
        self.game.opponent.discard()
        self.game.endOfGame = \
            self.game.agentPlayer.fatal_received or \
            self.game.agentPlayer.energy <= 0 or \
            self.game.opponent.fatal_received or \
            self.game.opponent.energy <= 0


class DeployBuy:

    def __init__(self, game: Game, player: Player, opponent: Player):

        self.game = game
        self.player = player
        self.opponent = opponent
        self.commonDrawDeck = game.commonDrawDeck

        self.chooseCommonDrawDeck = False
        self.chooseOpponentStash = False
        self.choosePersonalStash = False
        self.stash_block_value = 0
        self.common_block_value = 0
        self.opponent_block_value = 0
        self.stash_blocked_value = 0
        self.common_blocked_value = 0
        self.opponent_blocked_value = 0
        self.stash_has_draw = False
        self.common_has_draw = False
        self.stash_card_face_up: Optional[Card] = None
        self.common_card_face_up: Optional[CardComposite] = None
        self.opponent_card_face_up: Optional[CardComposite] = None

        if player.pips == 0:
            self.did_activity = False
        else:
            self._resolve_deploy_choose_card()
            self.did_activity = self._resolve_buy_card()

    def _resolve_deploy_choose_card(self):

        player = self.player
        opponent = self.opponent
        self.stash_card_face_up: Optional[CardComposite] = None
        self.stash_has_draw = False
        stash_can_afford = False
        stash_card_value = 0
        self.stash_block_value = 0
        self.choosePersonalStash = False
        self.common_card_face_up: Optional[CardComposite] = None
        self.common_has_draw = False
        common_can_afford = False
        common_card_value = 0
        self.common_block_value = 0
        self.chooseCommonDrawDeck = False
        self.opponent_card_face_up: Optional[CardComposite] = None
        opponent_can_afford = False
        opponent_card_value = 0
        self.opponent_block_value = 0
        self.chooseOpponentStash = False

        if player.num_cards_stash > 0:
            cards = player.stash_cards_face_up
            if len(cards) > 0:
                self.stash_card_face_up = cards[0]
                stash_can_afford = self._can_afford(self.stash_card_face_up.cost, player)
                stash_card_value = self.stash_card_face_up.ff_value
                self.stash_block_value = player.pips - self.stash_card_face_up.cost.pips

            self.stash_has_draw = len(player.stash_cards_draw) > 0

        common_cards = self.commonDrawDeck
        if common_cards.cards_total > 0:
            cards = common_cards.faceUp_deck
            if len(cards) > 0:
                self.common_card_face_up = cards[0]
                common_can_afford = self._can_afford(self.stash_card_face_up.cost, player)
                common_card_value = self.common_card_face_up.ff_value
                self.common_block_value = player.pips - self.stash_card_face_up.cost.pips

            self.common_has_draw = len(common_cards.draw_deck) > 0

        if opponent.num_cards_stash > 0:
            cards = opponent.stash_cards_face_up
            if len(cards) > 0:
                self.opponent_card_face_up = cards[0]
                opponent_can_afford = self._can_afford(self.opponent_card_face_up.cost, player)
                opponent_card_value = self.opponent_card_face_up.ff_value
                self.opponent_block_value = player.pips - self.opponent_card_face_up.cost.pips

        if opponent_card_value > 0 and opponent_can_afford:
            if opponent_card_value > stash_card_value and opponent_card_value > common_card_value:
                self.chooseOpponentStash = True

        if stash_card_value > 0 and stash_can_afford:
            if stash_card_value > common_card_value:
                self.choosePersonalStash = True

        if common_card_value > 0 and common_can_afford:
            self.chooseCommonDrawDeck = True

    def _resolve_buy_card(self) -> bool:
        if self.chooseOpponentStash:
            if self.opponent.pips > self.opponent_block_value:
                self.opponent.pips -= self.opponent_block_value + 1
            else:
                card = self.opponent_card_face_up
                cost = card.cost
                self.player.pips -= cost.pips

                if cost.sides != DieSides.NONE and self.player.plate_has_sides(cost.sides):
                    trashed_card = self.player.plate_remove_sides(cost.sides)
                    self.game.trash.append(trashed_card)
                elif cost.energy > 0:
                    self.player.energy -= cost.energy
                self.player.draw.append(card)
                return True

        if self.choosePersonalStash:
            if self.opponent.pips > self.stash_blocked_value:
                self.opponent.pips -= self.stash_blocked_value + 1
            else:
                card = self.stash_card_face_up
                cost = card.cost
                self.player.pips -= cost.pips

                if cost.sides != DieSides.NONE and self.player.plate_has_sides(cost.sides):
                    trashed_card = self.player.plate_remove_sides(cost.sides)
                    self.game.trash.append(trashed_card)
                elif cost.energy > 0:
                    self.player.energy -= cost.energy
                self.player.draw.append(card)
                return True

        if self.chooseCommonDrawDeck:
            if self.opponent.pips > self.common_block_value:
                self.opponent.pips -= self.common_block_value + 1
            else:
                card = self.common_card_face_up
                cost = card.cost
                self.player.pips -= cost.pips

                if cost.sides != DieSides.NONE and self.player.plate_has_sides(cost.sides):
                    trashed_card = self.player.plate_remove_sides(cost.sides)
                    self.game.trash.append(trashed_card)
                elif cost.energy > 0:
                    self.player.energy -= cost.energy
                self.player.draw.append(card)
                return True

        if self.player.pips > 0:
            if self.stash_has_draw:
                self.player.draw.draw()
                self.player.pips -= 1
                return True

            if self.common_has_draw:
                self.commonDrawDeck.draw()
                self.player.pips -= 1
                return True

        return False

    @staticmethod
    def _can_afford(card: CardCost, player: Player) -> bool:
        if player.pips < card.pips:
            return False
        if not player.plate_has_sides(card.sides):
            return False
        if player.energy <= 5:
            return False
        return True
