"""Pure aggregation of validated Buy-policy market evaluation results.

This module deliberately operates only on :class:`MarketRawResult` values that
have already passed the strict JSON reader/validator.  It performs no file I/O,
launches no processes, and never infers exposure from purchase counts.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Iterable

from .models import MarketRawResult, MarketSlotIdentity


@dataclass(frozen=True)
class LearnerPerformance:
    """Identity and held-out performance for one learned policy result."""

    player_count: int
    policy_path: str
    win_share: float


@dataclass(frozen=True)
class CardAggregate:
    """Aggregate learned-policy demand for one Plant card."""

    player_count: int
    identity: str
    plant_type: str
    cost: int
    total_exposure: int
    total_purchases: int
    purchases_per_exposure: float | None
    learners_evaluated: int
    learners_purchasing: int
    best_purchasing_learner: LearnerPerformance | None

    @property
    def slot(self) -> MarketSlotIdentity:
        return MarketSlotIdentity(self.plant_type, self.cost)


@dataclass(frozen=True)
class SlotAggregate:
    """Aggregate learned-policy demand for one legal type/cost slot."""

    player_count: int
    slot: MarketSlotIdentity
    total_exposure: int
    total_purchases: int
    purchases_per_exposure: float | None
    card_count: int
    learners_evaluated: int


@dataclass(frozen=True)
class PlayerCountMarketAggregate:
    """All market aggregates for one player-count environment."""

    player_count: int
    learners_evaluated: int
    cards: tuple[CardAggregate, ...]
    slots: tuple[SlotAggregate, ...]

    @property
    def cards_by_identity(self) -> dict[str, CardAggregate]:
        return {card.identity: card for card in self.cards}

    @property
    def slots_by_identity(self) -> dict[MarketSlotIdentity, SlotAggregate]:
        return {slot.slot: slot for slot in self.slots}

    @property
    def zero_purchase_cards(self) -> tuple[CardAggregate, ...]:
        return tuple(card for card in self.cards if card.total_purchases == 0)

    @property
    def one_learner_only_cards(self) -> tuple[CardAggregate, ...]:
        return tuple(card for card in self.cards if card.learners_purchasing == 1)

    @property
    def total_exposure(self) -> int:
        return sum(card.total_exposure for card in self.cards)

    @property
    def total_purchases(self) -> int:
        return sum(card.total_purchases for card in self.cards)


@dataclass(frozen=True)
class MarketAggregates:
    """Player-count-specific aggregate collection."""

    by_player_count: tuple[PlayerCountMarketAggregate, ...]

    def for_players(self, players: int) -> PlayerCountMarketAggregate:
        for aggregate in self.by_player_count:
            if aggregate.player_count == players:
                return aggregate
        raise KeyError(players)


def safe_rate(numerator: int, denominator: int) -> float | None:
    """Return ``numerator / denominator`` or ``None`` when undefined.

    Zero exposure is not equivalent to a zero purchase rate.  The undefined
    state is preserved explicitly so downstream reporting cannot silently turn
    missing opportunity into apparent rejection.
    """

    if denominator == 0:
        return None
    return numerator / denominator


def aggregate_market_results(results: Iterable[MarketRawResult]) -> MarketAggregates:
    """Aggregate validated raw results independently for each player count.

    Input ordering has no effect on output ordering or values.  Within a player
    count all raw results must describe the same card identities and slot
    metadata.  Exposure and purchases are taken directly from each result's
    learned side; no values are reconstructed or inferred.
    """

    grouped: dict[int, list[MarketRawResult]] = {}
    for result in results:
        grouped.setdefault(result.metadata.players, []).append(result)

    player_aggregates = tuple(
        _aggregate_player_count(players, grouped[players]) for players in sorted(grouped)
    )
    return MarketAggregates(player_aggregates)


def aggregate_player_count(results: Iterable[MarketRawResult]) -> PlayerCountMarketAggregate:
    """Aggregate results that must all belong to one player count."""

    materialized = list(results)
    if not materialized:
        raise ValueError("at least one market result is required")
    players = materialized[0].metadata.players
    mismatched = sorted({result.metadata.players for result in materialized if result.metadata.players != players})
    if mismatched:
        raise ValueError(
            f"aggregate_player_count requires one player count; expected {players}, also found {mismatched}"
        )
    return _aggregate_player_count(players, materialized)


def zero_purchase_cards(aggregate: PlayerCountMarketAggregate) -> tuple[CardAggregate, ...]:
    """Return cards no learned policy purchased for this player count."""

    return aggregate.zero_purchase_cards


def one_learner_only_cards(aggregate: PlayerCountMarketAggregate) -> tuple[CardAggregate, ...]:
    """Return cards purchased by exactly one learned policy."""

    return aggregate.one_learner_only_cards


def _aggregate_player_count(
    players: int, results: list[MarketRawResult]
) -> PlayerCountMarketAggregate:
    if not results:
        raise ValueError("at least one market result is required")

    canonical = results[0].cards_by_identity
    canonical_names = set(canonical)
    for result in results[1:]:
        cards = result.cards_by_identity
        if set(cards) != canonical_names:
            missing = sorted(canonical_names - set(cards))
            extra = sorted(set(cards) - canonical_names)
            raise ValueError(
                f"market identities differ within {players}p aggregation; missing={missing}, extra={extra}"
            )
        for identity, expected in canonical.items():
            actual = cards[identity]
            if actual.plant_type != expected.plant_type or actual.cost != expected.cost:
                raise ValueError(
                    f"card metadata differs within {players}p aggregation for {identity}: "
                    f"expected {expected.plant_type} {expected.cost}, got {actual.plant_type} {actual.cost}"
                )

    card_aggregates: list[CardAggregate] = []
    for identity in sorted(canonical_names):
        metadata = canonical[identity]
        exposure = 0
        purchases = 0
        purchasing_learners: list[LearnerPerformance] = []
        for result in results:
            card = result.cards_by_identity[identity]
            exposure += card.learned_exposure
            purchases += card.learned_purchases
            if card.learned_purchases > 0:
                purchasing_learners.append(
                    LearnerPerformance(
                        player_count=players,
                        policy_path=result.metadata.policy_path,
                        win_share=result.learned.win_share,
                    )
                )

        best = (
            max(purchasing_learners, key=lambda learner: (learner.win_share, learner.policy_path))
            if purchasing_learners
            else None
        )
        card_aggregates.append(
            CardAggregate(
                player_count=players,
                identity=identity,
                plant_type=metadata.plant_type,
                cost=metadata.cost,
                total_exposure=exposure,
                total_purchases=purchases,
                purchases_per_exposure=safe_rate(purchases, exposure),
                learners_evaluated=len(results),
                learners_purchasing=len(purchasing_learners),
                best_purchasing_learner=best,
            )
        )

    slots: dict[MarketSlotIdentity, list[CardAggregate]] = {}
    for card in card_aggregates:
        slots.setdefault(card.slot, []).append(card)

    slot_aggregates = tuple(
        SlotAggregate(
            player_count=players,
            slot=slot,
            total_exposure=sum(card.total_exposure for card in cards),
            total_purchases=sum(card.total_purchases for card in cards),
            purchases_per_exposure=safe_rate(
                sum(card.total_purchases for card in cards),
                sum(card.total_exposure for card in cards),
            ),
            card_count=len(cards),
            learners_evaluated=len(results),
        )
        for slot, cards in sorted(slots.items())
    )

    return PlayerCountMarketAggregate(
        player_count=players,
        learners_evaluated=len(results),
        cards=tuple(card_aggregates),
        slots=slot_aggregates,
    )
