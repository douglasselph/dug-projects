"""Deterministic report rendering for validated market-evaluation results.

Reports consume typed :class:`MarketRawResult` objects and the pure aggregation
layer.  They never inspect or parse human-readable Kotlin console output.
"""

from __future__ import annotations

from pathlib import Path
from typing import Iterable, Sequence

from .aggregation import (
    CardAggregate,
    MarketAggregates,
    PlayerCountMarketAggregate,
    aggregate_market_results,
)
from .models import CardResult, MarketRawResult, MarketSlotIdentity
from .reader import EXPECTED_CURRENT_MARKET, LEGAL_SLOTS

CARD_PURCHASES = "card-purchases.tsv"
CARD_SUMMARY = "card-summary.tsv"
SLOT_SUMMARY = "slot-summary.tsv"
ZERO_PURCHASE_CARDS = "zero-purchase-cards.tsv"
ONE_LEARNER_ONLY_CARDS = "one-learner-only-cards.tsv"
VINE_9_SUMMARY = "vine-9-summary.tsv"
LEARNER_WIN_SHARES = "learner-win-shares.tsv"
README_FIRST = "README-FIRST.txt"

REPORT_FILENAMES = (
    CARD_PURCHASES,
    CARD_SUMMARY,
    SLOT_SUMMARY,
    ZERO_PURCHASE_CARDS,
    ONE_LEARNER_ONLY_CARDS,
    VINE_9_SUMMARY,
    LEARNER_WIN_SHARES,
    README_FIRST,
)

_CARD_PURCHASES_HEADER = (
    "players",
    "learner",
    "control_win_share_pct",
    "held_out_win_share_pct",
    "card",
    "type",
    "cost",
    "grove_exposures",
    "control_purchases",
    "learned_purchases",
    "learned_purchases_per_exposure",
)
_CARD_SUMMARY_HEADER = (
    "players",
    "card",
    "type",
    "cost",
    "total_grove_exposures",
    "total_learned_purchases",
    "purchases_per_exposure",
    "learners_evaluated",
    "learners_with_purchase",
    "best_learner_win_share_pct",
)
_SLOT_SUMMARY_HEADER = (
    "players",
    "type",
    "cost",
    "total_grove_exposures",
    "total_learned_purchases",
    "purchases_per_exposure",
    "card_learner_rows",
)
_LEARNER_WIN_HEADER = (
    "players",
    "learner",
    "control_win_share_pct",
    "held_out_win_share_pct",
)

_TYPE_ORDER = {"ROOT": 0, "VINE": 1, "FLOWER": 2}


class MarketReportValidationError(ValueError):
    """Raised when typed input violates an invariant required for reporting."""


def write_market_reports(
    results: Iterable[MarketRawResult], output_dir: Path | str
) -> dict[str, Path]:
    """Validate, render, and write all current-market reports.

    The returned mapping is keyed by the report filename constants above.
    """

    materialized = tuple(results)
    _validate_report_inputs(materialized)
    aggregates = aggregate_market_results(materialized)
    _validate_aggregates(aggregates)

    rendered = {
        CARD_PURCHASES: _render_card_purchases_validated(materialized),
        CARD_SUMMARY: _render_card_summary_validated(aggregates),
        SLOT_SUMMARY: _render_slot_summary_validated(aggregates),
        ZERO_PURCHASE_CARDS: _render_card_summary_filter_validated(
            aggregates, lambda card: card.total_purchases == 0
        ),
        ONE_LEARNER_ONLY_CARDS: _render_card_summary_filter_validated(
            aggregates, lambda card: card.learners_purchasing == 1
        ),
        VINE_9_SUMMARY: _render_card_summary_filter_validated(
            aggregates, lambda card: card.plant_type == "VINE" and card.cost == 9
        ),
        LEARNER_WIN_SHARES: _render_learner_win_shares_validated(materialized),
        README_FIRST: render_readme_first(),
    }

    directory = Path(output_dir)
    directory.mkdir(parents=True, exist_ok=True)
    paths: dict[str, Path] = {}
    for name in REPORT_FILENAMES:
        path = directory / name
        path.write_text(rendered[name], encoding="utf-8")
        paths[name] = path
    return paths


def render_card_purchases(results: Iterable[MarketRawResult]) -> str:
    materialized = tuple(results)
    _validate_report_inputs(materialized)
    return _render_card_purchases_validated(materialized)


def render_card_summary(results: Iterable[MarketRawResult]) -> str:
    aggregates = _validated_aggregates(results)
    return _render_card_summary_validated(aggregates)


def render_slot_summary(results: Iterable[MarketRawResult]) -> str:
    aggregates = _validated_aggregates(results)
    return _render_slot_summary_validated(aggregates)


def render_zero_purchase_cards(results: Iterable[MarketRawResult]) -> str:
    aggregates = _validated_aggregates(results)
    return _render_card_summary_filter_validated(
        aggregates, lambda card: card.total_purchases == 0
    )


def render_one_learner_only_cards(results: Iterable[MarketRawResult]) -> str:
    aggregates = _validated_aggregates(results)
    return _render_card_summary_filter_validated(
        aggregates, lambda card: card.learners_purchasing == 1
    )


def render_vine_9_summary(results: Iterable[MarketRawResult]) -> str:
    aggregates = _validated_aggregates(results)
    return _render_card_summary_filter_validated(
        aggregates, lambda card: card.plant_type == "VINE" and card.cost == 9
    )


def render_learner_win_shares(results: Iterable[MarketRawResult]) -> str:
    materialized = tuple(results)
    _validate_report_inputs(materialized)
    return _render_learner_win_shares_validated(materialized)


def render_readme_first() -> str:
    return """CURRENT MARKET REPORTS — READ FIRST

These reports are generated from typed Kotlin market-evaluation JSON loaded and
validated by Python.  No market fact is reconstructed by parsing human-readable
Kotlin console output.

Primary reports:
  card-purchases.tsv
      Per learner/card raw learned exposure and purchase counts, plus control
      purchases and held-out outcome context.
  card-summary.tsv
      Per-card aggregation across independent learners at each player count.
  slot-summary.tsv
      Per-slot aggregation across the four cards in each legal type/cost slot.
  zero-purchase-cards.tsv
      Card-summary rows with zero learned purchases across all evaluated learners.
  one-learner-only-cards.tsv
      Card-summary rows purchased by exactly one evaluated learner.
  vine-9-summary.tsv
      Card-summary rows for the VINE / cost-9 slot.
  learner-win-shares.tsv
      CONTROL and LEARNED held-out win shares directly from structured results.

Semantics:
  - grove_exposures and total_grove_exposures use LEARNED-side direct Grove
    exposure counts from typed telemetry.
  - purchase/exposure is purchases divided by the corresponding learned exposure.
  - a zero exposure denominator is rendered as NA, never as a fabricated 0.0.
  - best_learner_win_share_pct means the best held-out win share among learners
    that actually purchased the card.  It is NA when no learner purchased it.
  - raw purchase and exposure counts are retained in every aggregate report.

Before any report is written, the writer validates complete 36-card market
membership, unique identities, legal four-card slots, purchase/exposure
consistency, per-card purchase reconciliation, slot reconciliation, and
whole-market reconciliation.  Invalid inputs fail loudly instead of producing
plausible-looking TSV output.
"""


def _validated_aggregates(results: Iterable[MarketRawResult]) -> MarketAggregates:
    materialized = tuple(results)
    _validate_report_inputs(materialized)
    aggregates = aggregate_market_results(materialized)
    _validate_aggregates(aggregates)
    return aggregates


def _validate_report_inputs(results: Sequence[MarketRawResult]) -> None:
    if not results:
        raise MarketReportValidationError("at least one market result is required")

    expected_names = set(EXPECTED_CURRENT_MARKET)
    for result in results:
        identities = [card.identity for card in result.cards]
        if len(identities) != len(set(identities)):
            duplicates = sorted({name for name in identities if identities.count(name) > 1})
            raise MarketReportValidationError(
                f"{result.metadata.policy_path}: duplicate card identities: {duplicates}"
            )
        actual_names = set(identities)
        if actual_names != expected_names:
            missing = sorted(expected_names - actual_names)
            extra = sorted(actual_names - expected_names)
            raise MarketReportValidationError(
                f"{result.metadata.policy_path}: expected complete current 36-card market; "
                f"missing={missing}, extra={extra}"
            )
        if len(result.cards) != 36:
            raise MarketReportValidationError(
                f"{result.metadata.policy_path}: expected exactly 36 cards; got {len(result.cards)}"
            )

        by_slot: dict[MarketSlotIdentity, list[CardResult]] = {}
        control_purchase_sum = 0
        learned_purchase_sum = 0
        for card in result.cards:
            expected_slot = EXPECTED_CURRENT_MARKET[card.identity]
            if card.slot != expected_slot:
                raise MarketReportValidationError(
                    f"{result.metadata.policy_path}: {card.identity} metadata mismatch: "
                    f"expected {expected_slot}, got {card.slot}"
                )
            if card.slot not in LEGAL_SLOTS:
                raise MarketReportValidationError(
                    f"{result.metadata.policy_path}: illegal slot {card.slot}"
                )
            if card.control_exposure < 0 or card.learned_exposure < 0:
                raise MarketReportValidationError(
                    f"{result.metadata.policy_path}: {card.identity} has negative exposure"
                )
            if card.control_purchases < 0 or card.learned_purchases < 0:
                raise MarketReportValidationError(
                    f"{result.metadata.policy_path}: {card.identity} has negative purchases"
                )
            if card.control_purchases > 0 and card.control_exposure == 0:
                raise MarketReportValidationError(
                    f"{result.metadata.policy_path}: {card.identity} has control purchases with zero exposure"
                )
            if card.learned_purchases > 0 and card.learned_exposure == 0:
                raise MarketReportValidationError(
                    f"{result.metadata.policy_path}: {card.identity} has learned purchases with zero exposure"
                )
            by_slot.setdefault(card.slot, []).append(card)
            control_purchase_sum += card.control_purchases
            learned_purchase_sum += card.learned_purchases

        for slot in sorted(LEGAL_SLOTS):
            count = len(by_slot.get(slot, ()))
            if count != 4:
                raise MarketReportValidationError(
                    f"{result.metadata.policy_path}: slot {slot.plant_type} {slot.cost} "
                    f"must contain 4 cards; got {count}"
                )
        if control_purchase_sum != result.control.plant_purchases:
            raise MarketReportValidationError(
                f"{result.metadata.policy_path}: control Plant purchases do not reconcile: "
                f"cards={control_purchase_sum}, outcome={result.control.plant_purchases}"
            )
        if learned_purchase_sum != result.learned.plant_purchases:
            raise MarketReportValidationError(
                f"{result.metadata.policy_path}: learned Plant purchases do not reconcile: "
                f"cards={learned_purchase_sum}, outcome={result.learned.plant_purchases}"
            )


def _validate_aggregates(aggregates: MarketAggregates) -> None:
    expected_names = set(EXPECTED_CURRENT_MARKET)
    for market in aggregates.by_player_count:
        card_names = [card.identity for card in market.cards]
        if len(card_names) != 36 or set(card_names) != expected_names:
            raise MarketReportValidationError(
                f"{market.player_count}p aggregate does not contain the exact current 36-card market"
            )
        if len(market.slots) != 9:
            raise MarketReportValidationError(
                f"{market.player_count}p aggregate must contain 9 slots; got {len(market.slots)}"
            )

        slot_exposure = 0
        slot_purchases = 0
        for slot in market.slots:
            members = [card for card in market.cards if card.slot == slot.slot]
            if slot.card_count != 4 or len(members) != 4:
                raise MarketReportValidationError(
                    f"{market.player_count}p slot {slot.slot} must reconcile to exactly 4 cards"
                )
            expected_exposure = sum(card.total_exposure for card in members)
            expected_purchases = sum(card.total_purchases for card in members)
            if slot.total_exposure != expected_exposure:
                raise MarketReportValidationError(
                    f"{market.player_count}p slot {slot.slot} exposure does not reconcile"
                )
            if slot.total_purchases != expected_purchases:
                raise MarketReportValidationError(
                    f"{market.player_count}p slot {slot.slot} purchases do not reconcile"
                )
            slot_exposure += slot.total_exposure
            slot_purchases += slot.total_purchases

        if market.total_exposure != slot_exposure:
            raise MarketReportValidationError(
                f"{market.player_count}p whole-market exposure does not reconcile to slots"
            )
        if market.total_purchases != slot_purchases:
            raise MarketReportValidationError(
                f"{market.player_count}p whole-market purchases do not reconcile to slots"
            )


def _render_card_purchases_validated(results: Sequence[MarketRawResult]) -> str:
    rows = ["\t".join(_CARD_PURCHASES_HEADER)]
    for result in sorted(results, key=_result_sort_key):
        for card in sorted(result.cards, key=_card_sort_key):
            rows.append(
                "\t".join(
                    (
                        str(result.metadata.players),
                        _learner_label(result),
                        _pct(result.control.win_share),
                        _pct(result.learned.win_share),
                        card.identity,
                        card.plant_type,
                        str(card.cost),
                        str(card.learned_exposure),
                        str(card.control_purchases),
                        str(card.learned_purchases),
                        _rate(card.learned_purchases, card.learned_exposure),
                    )
                )
            )
    return "\n".join(rows) + "\n"


def _render_card_summary_validated(aggregates: MarketAggregates) -> str:
    rows = ["\t".join(_CARD_SUMMARY_HEADER)]
    for market in aggregates.by_player_count:
        for card in sorted(market.cards, key=_card_aggregate_sort_key):
            rows.append(_card_summary_row(card))
    return "\n".join(rows) + "\n"


def _render_card_summary_filter_validated(aggregates: MarketAggregates, predicate) -> str:
    rows = ["\t".join(_CARD_SUMMARY_HEADER)]
    for market in aggregates.by_player_count:
        for card in sorted(market.cards, key=_card_aggregate_sort_key):
            if predicate(card):
                rows.append(_card_summary_row(card))
    return "\n".join(rows) + "\n"


def _card_summary_row(card: CardAggregate) -> str:
    best = card.best_purchasing_learner
    return "\t".join(
        (
            str(card.player_count),
            card.identity,
            card.plant_type,
            str(card.cost),
            str(card.total_exposure),
            str(card.total_purchases),
            _format_rate_value(card.purchases_per_exposure),
            str(card.learners_evaluated),
            str(card.learners_purchasing),
            _pct(best.win_share) if best is not None else "NA",
        )
    )


def _render_slot_summary_validated(aggregates: MarketAggregates) -> str:
    rows = ["\t".join(_SLOT_SUMMARY_HEADER)]
    for market in aggregates.by_player_count:
        for slot in sorted(market.slots, key=lambda item: _slot_sort_key(item.slot)):
            rows.append(
                "\t".join(
                    (
                        str(market.player_count),
                        slot.slot.plant_type,
                        str(slot.slot.cost),
                        str(slot.total_exposure),
                        str(slot.total_purchases),
                        _format_rate_value(slot.purchases_per_exposure),
                        str(slot.card_count * slot.learners_evaluated),
                    )
                )
            )
    return "\n".join(rows) + "\n"


def _render_learner_win_shares_validated(results: Sequence[MarketRawResult]) -> str:
    rows = ["\t".join(_LEARNER_WIN_HEADER)]
    for result in sorted(results, key=_result_sort_key):
        rows.append(
            "\t".join(
                (
                    str(result.metadata.players),
                    _learner_label(result),
                    _pct(result.control.win_share),
                    _pct(result.learned.win_share),
                )
            )
        )
    return "\n".join(rows) + "\n"


def _result_sort_key(result: MarketRawResult) -> tuple[int, str, str]:
    return (result.metadata.players, _learner_label(result), result.metadata.policy_path)


def _learner_label(result: MarketRawResult) -> str:
    stem = Path(result.metadata.policy_path).stem
    return stem or result.metadata.policy_path


def _slot_sort_key(slot: MarketSlotIdentity) -> tuple[int, int]:
    return (_TYPE_ORDER.get(slot.plant_type, 999), slot.cost)


def _card_sort_key(card: CardResult) -> tuple[int, int, str]:
    return (*_slot_sort_key(card.slot), card.identity)


def _card_aggregate_sort_key(card: CardAggregate) -> tuple[int, int, str]:
    return (*_slot_sort_key(card.slot), card.identity)


def _pct(value: float) -> str:
    return f"{value * 100.0:.3f}"


def _rate(numerator: int, denominator: int) -> str:
    if denominator == 0:
        return "NA"
    return f"{numerator / denominator:.6f}"


def _format_rate_value(value: float | None) -> str:
    return "NA" if value is None else f"{value:.6f}"
