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
    aggregate_market_opportunities,
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
CARD_OPPORTUNITY_SUMMARY = "card-opportunity-summary.tsv"
SLOT_OPPORTUNITY_SUMMARY = "slot-opportunity-summary.tsv"
CARD_OPPORTUNITY_BY_ROUND = "card-opportunity-by-round.tsv"
CARD_SUBSTITUTION_SUMMARY = "card-substitution-summary.tsv"
CARD_SUBSTITUTIONS_BY_LEARNER = "card-substitutions-by-learner.tsv"
README_FIRST = "README-FIRST.txt"

REPORT_FILENAMES = (
    CARD_PURCHASES,
    CARD_SUMMARY,
    SLOT_SUMMARY,
    ZERO_PURCHASE_CARDS,
    ONE_LEARNER_ONLY_CARDS,
    VINE_9_SUMMARY,
    LEARNER_WIN_SHARES,
    CARD_OPPORTUNITY_SUMMARY,
    SLOT_OPPORTUNITY_SUMMARY,
    CARD_OPPORTUNITY_BY_ROUND,
    CARD_SUBSTITUTION_SUMMARY,
    CARD_SUBSTITUTIONS_BY_LEARNER,
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


_OPPORTUNITY_HEADER = (
    "players", "role", "card", "type", "cost", "learners_evaluated",
    "market_card_opportunities", "affordable_opportunities", "affordable_rate",
    "graftable_opportunities", "graftable_rate", "legal_opportunities", "legal_rate",
    "selected_purchases", "selection_per_legal", "player_done_while_legal",
    "done_while_legal_rate", "no_legal_items_while_market", "first_decision_legal",
    "post_purchase_legal", "legal_with_higher_cost_plant", "avg_purchasing_power_when_market",
)
_SLOT_OPPORTUNITY_HEADER = (
    "players", "role", "type", "cost", "card_rows", "market_card_opportunities",
    "affordable_opportunities", "affordable_rate", "graftable_opportunities",
    "graftable_rate", "legal_opportunities", "legal_rate", "selected_purchases",
    "selection_per_legal", "player_done_while_legal", "done_while_legal_rate",
    "legal_with_higher_cost_plant", "avg_purchasing_power_when_market",
)
_ROUND_OPPORTUNITY_HEADER = (
    "players", "role", "card", "type", "cost", "cultivation_round",
    "market_card_opportunities", "affordable_opportunities", "legal_opportunities",
    "selected_purchases",
)
_SUBSTITUTION_SUMMARY_HEADER = (
    "players", "role", "target_card", "target_type", "target_cost",
    "legal_rejections", "alternative_outcome", "alternative_kind",
    "alternative_item", "alternative_type", "alternative_cost",
    "alternative_count", "share_of_target_rejections", "learners_using_alternative",
)
_SUBSTITUTION_BY_LEARNER_HEADER = (
    "players", "learner", "role", "target_card", "target_type", "target_cost",
    "legal_rejections", "alternative_outcome", "alternative_kind",
    "alternative_item", "alternative_type", "alternative_cost",
    "alternative_count", "share_of_target_rejections",
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
        CARD_OPPORTUNITY_SUMMARY: _render_card_opportunity_summary(materialized),
        SLOT_OPPORTUNITY_SUMMARY: _render_slot_opportunity_summary(materialized),
        CARD_OPPORTUNITY_BY_ROUND: _render_card_opportunity_by_round(materialized),
        CARD_SUBSTITUTION_SUMMARY: _render_card_substitution_summary(materialized),
        CARD_SUBSTITUTIONS_BY_LEARNER: _render_card_substitutions_by_learner(materialized),
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


def render_card_opportunity_summary(results: Iterable[MarketRawResult]) -> str:
    materialized = tuple(results)
    _validate_report_inputs(materialized)
    return _render_card_opportunity_summary(materialized)


def render_slot_opportunity_summary(results: Iterable[MarketRawResult]) -> str:
    materialized = tuple(results)
    _validate_report_inputs(materialized)
    return _render_slot_opportunity_summary(materialized)


def render_card_opportunity_by_round(results: Iterable[MarketRawResult]) -> str:
    materialized = tuple(results)
    _validate_report_inputs(materialized)
    return _render_card_opportunity_by_round(materialized)


def render_card_substitution_summary(results: Iterable[MarketRawResult]) -> str:
    materialized = tuple(results)
    _validate_report_inputs(materialized)
    return _render_card_substitution_summary(materialized)


def render_card_substitutions_by_learner(results: Iterable[MarketRawResult]) -> str:
    materialized = tuple(results)
    _validate_report_inputs(materialized)
    return _render_card_substitutions_by_learner(materialized)


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
  card-opportunity-summary.tsv
      Buy-decision opportunity diagnostics: market availability, affordability,
      graftability, legality, actual selection, and explicit pass/Done behavior.
  slot-opportunity-summary.tsv
      The same opportunity diagnostics aggregated by Plant type/cost slot.
  card-opportunity-by-round.tsv
      Market/affordable/legal/selected opportunity counts by Cultivation round.
  card-substitution-summary.tsv
      When a Plant was legal but rejected, what was selected instead, aggregated
      across learners. Includes exact Plant identity or die identity/cost.
  card-substitutions-by-learner.tsv
      The same rejected-legal substitution evidence separated by learner.

Semantics:
  - grove_exposures and total_grove_exposures use LEARNED-side direct Grove
    exposure counts from typed telemetry.
  - purchase/exposure is purchases divided by the corresponding learned exposure.
  - a zero exposure denominator is rendered as NA, never as a fabricated 0.0.
  - best_learner_win_share_pct means the best held-out win share among learners
    that actually purchased the card.  It is NA when no learner purchased it.
  - raw purchase and exposure counts are retained in every aggregate report.
  - opportunity reports distinguish "in the market" from "affordable",
    "graftable", "legal now", and "actually selected". This is specifically
    intended to separate tier-access/economic problems from card-preference problems.
  - player_done_while_legal counts a strong rejection signal: the player ended
    its Buy turn while that Plant was a legal purchase.
  - substitution reports condition on the target Plant being legal and not
    selected, then report the actual alternative purchase (or PLAYER_DONE).

Before any report is written, the writer validates complete 36-card market
membership, unique identities, legal four-card slots, purchase/exposure
consistency, per-card purchase reconciliation, slot reconciliation, and
whole-market reconciliation.  Invalid inputs fail loudly instead of producing
plausible-looking TSV output.
"""


def _opportunity_rows(results: Sequence[MarketRawResult]):
    if not results or any(result.schema_version < 2 for result in results):
        return ()
    return aggregate_market_opportunities(results)


def _rate_text(numerator: int, denominator: int) -> str:
    return "NA" if denominator == 0 else f"{numerator / denominator:.6f}"


def _avg_text(total: int, count: int) -> str:
    return "NA" if count == 0 else f"{total / count:.3f}"


def _render_card_opportunity_summary(results: Sequence[MarketRawResult]) -> str:
    lines = ["\t".join(_OPPORTUNITY_HEADER)]
    for row in _opportunity_rows(results):
        lines.append("\t".join((
            str(row.player_count), row.role, row.identity, row.plant_type, str(row.cost),
            str(row.learners_evaluated), str(row.market_decisions), str(row.affordable_decisions),
            _rate_text(row.affordable_decisions, row.market_decisions),
            str(row.graftable_decisions), _rate_text(row.graftable_decisions, row.market_decisions),
            str(row.legal_decisions), _rate_text(row.legal_decisions, row.market_decisions),
            str(row.selected_decisions), _rate_text(row.selected_decisions, row.legal_decisions),
            str(row.player_done_while_legal), _rate_text(row.player_done_while_legal, row.legal_decisions),
            str(row.no_legal_items_while_market), str(row.first_decision_legal),
            str(row.post_purchase_legal), str(row.legal_with_higher_cost_plant),
            _avg_text(row.purchasing_power_on_market_sum, row.market_decisions),
        )))
    return "\n".join(lines) + "\n"


def _render_slot_opportunity_summary(results: Sequence[MarketRawResult]) -> str:
    rows = _opportunity_rows(results)
    lines = ["\t".join(_SLOT_OPPORTUNITY_HEADER)]
    grouped = {}
    for row in rows:
        grouped.setdefault((row.player_count, row.role, row.plant_type, row.cost), []).append(row)
    for key in sorted(grouped, key=lambda k: (k[0], 0 if k[1] == "CONTROL" else 1, _TYPE_ORDER[k[2]], k[3])):
        members = grouped[key]
        players, role, plant_type, cost = key
        market = sum(r.market_decisions for r in members)
        affordable = sum(r.affordable_decisions for r in members)
        graftable = sum(r.graftable_decisions for r in members)
        legal = sum(r.legal_decisions for r in members)
        selected = sum(r.selected_decisions for r in members)
        done = sum(r.player_done_while_legal for r in members)
        higher = sum(r.legal_with_higher_cost_plant for r in members)
        power = sum(r.purchasing_power_on_market_sum for r in members)
        lines.append("\t".join((
            str(players), role, plant_type, str(cost), str(len(members)), str(market),
            str(affordable), _rate_text(affordable, market), str(graftable), _rate_text(graftable, market),
            str(legal), _rate_text(legal, market), str(selected), _rate_text(selected, legal),
            str(done), _rate_text(done, legal), str(higher), _avg_text(power, market),
        )))
    return "\n".join(lines) + "\n"


def _render_card_opportunity_by_round(results: Sequence[MarketRawResult]) -> str:
    lines = ["\t".join(_ROUND_OPPORTUNITY_HEADER)]
    for row in _opportunity_rows(results):
        market = dict(row.market_by_cultivation_round)
        affordable = dict(row.affordable_by_cultivation_round)
        legal = dict(row.legal_by_cultivation_round)
        selected = dict(row.selected_by_cultivation_round)
        rounds = sorted(set(market) | set(affordable) | set(legal) | set(selected))
        for round_number in rounds:
            lines.append("\t".join((
                str(row.player_count), row.role, row.identity, row.plant_type, str(row.cost), str(round_number),
                str(market.get(round_number, 0)), str(affordable.get(round_number, 0)),
                str(legal.get(round_number, 0)), str(selected.get(round_number, 0)),
            )))
    return "\n".join(lines) + "\n"



def _role_opportunity(card: CardResult, role: str):
    return card.control_opportunity if role == "CONTROL" else card.learned_opportunity


def _alternative_metadata(result: MarketRawResult, kind: str | None, item_name: str | None, cost: int | None):
    if kind == "PLANT" and item_name in result.cards_by_identity:
        card = result.cards_by_identity[item_name]
        return card.plant_type, card.cost
    return ("NA", cost if cost is not None else None)


def _render_card_substitutions_by_learner(results: Sequence[MarketRawResult]) -> str:
    lines = ["\t".join(_SUBSTITUTION_BY_LEARNER_HEADER)]
    if not results or any(result.schema_version < 3 for result in results):
        return "\n".join(lines) + "\n"
    for result in sorted(results, key=_result_sort_key):
        learner = _learner_label(result)
        for role in ("CONTROL", "LEARNED"):
            for card in sorted(result.cards, key=_card_sort_key):
                stats = _role_opportunity(card, role)
                rejected = stats.legal_decisions - stats.selected_decisions
                for alt in stats.rejected_legal_alternatives:
                    alt_type, alt_cost = _alternative_metadata(result, alt.kind, alt.item_name, alt.cost)
                    lines.append("\t".join((
                        str(result.metadata.players), learner, role, card.identity, card.plant_type, str(card.cost),
                        str(rejected), alt.outcome, alt.kind or "NA", alt.item_name or "NA", alt_type,
                        "NA" if alt_cost is None else str(alt_cost), str(alt.count), _rate(alt.count, rejected),
                    )))
    return "\n".join(lines) + "\n"


def _render_card_substitution_summary(results: Sequence[MarketRawResult]) -> str:
    lines = ["\t".join(_SUBSTITUTION_SUMMARY_HEADER)]
    if not results or any(result.schema_version < 3 for result in results):
        return "\n".join(lines) + "\n"
    grouped: dict[tuple, dict[str, object]] = {}
    for result in sorted(results, key=_result_sort_key):
        learner = _learner_label(result)
        for role in ("CONTROL", "LEARNED"):
            for card in result.cards:
                stats = _role_opportunity(card, role)
                rejected = stats.legal_decisions - stats.selected_decisions
                for alt in stats.rejected_legal_alternatives:
                    alt_type, alt_cost = _alternative_metadata(result, alt.kind, alt.item_name, alt.cost)
                    key = (result.metadata.players, role, card.identity, card.plant_type, card.cost,
                           alt.outcome, alt.kind, alt.item_name, alt_type, alt_cost)
                    row = grouped.setdefault(key, {"count": 0, "rejected": 0, "learners": set()})
                    row["count"] += alt.count
                    row["learners"].add(learner)
    # Recompute target denominators independently to avoid alternative-count fanout.
    denominators: dict[tuple[int, str, str], int] = {}
    for result in results:
        for role in ("CONTROL", "LEARNED"):
            for card in result.cards:
                stats = _role_opportunity(card, role)
                denominators[(result.metadata.players, role, card.identity)] = (
                    denominators.get((result.metadata.players, role, card.identity), 0)
                    + stats.legal_decisions - stats.selected_decisions
                )
    for key in sorted(grouped, key=lambda k: (k[0], 0 if k[1] == "CONTROL" else 1, _TYPE_ORDER.get(k[3], 999), k[4], k[2], str(k[5:]))) :
        players, role, target, target_type, target_cost, outcome, kind, item_name, alt_type, alt_cost = key
        row = grouped[key]
        rejected = denominators[(players, role, target)]
        lines.append("\t".join((
            str(players), role, target, target_type, str(target_cost), str(rejected), outcome, kind or "NA",
            item_name or "NA", alt_type, "NA" if alt_cost is None else str(alt_cost), str(row["count"]),
            _rate(row["count"], rejected), str(len(row["learners"])),
        )))
    return "\n".join(lines) + "\n"

def _validated_aggregates(results: Iterable[MarketRawResult]) -> MarketAggregates:
    materialized = tuple(results)
    _validate_report_inputs(materialized)
    aggregates = aggregate_market_results(materialized)
    _validate_aggregates(aggregates)
    return aggregates


def _validate_report_inputs(results: Sequence[MarketRawResult]) -> None:
    if not results:
        raise MarketReportValidationError("at least one market result is required")

    # A learner result is identified by its policy path within one player-count
    # environment.  Counting the same learner twice would silently inflate
    # learners_evaluated / learners_with_purchase and distort aggregate rates.
    seen_learners: set[tuple[int, str]] = set()
    seen_labels: set[tuple[int, str]] = set()
    for result in results:
        learner_key = (result.metadata.players, result.metadata.policy_path)
        if learner_key in seen_learners:
            raise MarketReportValidationError(
                f"duplicate learner result for {result.metadata.players}p: "
                f"{result.metadata.policy_path}"
            )
        seen_learners.add(learner_key)

        label_key = (result.metadata.players, _learner_label(result))
        if label_key in seen_labels:
            raise MarketReportValidationError(
                f"duplicate learner label for {result.metadata.players}p: "
                f"{_learner_label(result)}"
            )
        seen_labels.add(label_key)

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
            # Stable card identity names may contain a historical tier that no
            # longer matches the effective current slot after a research override.
            # The typed card.slot value is authoritative; validate legality and
            # complete four-card slot cardinality instead of re-deriving metadata
            # from the identity string.
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
