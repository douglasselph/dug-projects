"""Strict reader/validator for Kotlin ``leaf.market-evaluation`` JSON."""

from __future__ import annotations

import json
import math
from pathlib import Path
from typing import Any, Mapping

from .models import CardResult, ExperimentMetadata, LearnerResult, MarketRawResult, MarketSlotIdentity

MARKET_SCHEMA = "leaf.market-evaluation"
MARKET_SCHEMA_VERSION = 1

LEGAL_SLOT_COSTS: dict[str, tuple[int, ...]] = {
    "ROOT": (5, 7, 9),
    "VINE": (7, 9, 11),
    "FLOWER": (11, 14, 17),
}
LEGAL_SLOTS = frozenset(
    MarketSlotIdentity(plant_type, cost)
    for plant_type, costs in LEGAL_SLOT_COSTS.items()
    for cost in costs
)

# The 36 card identities are stable identifiers. Their embedded numeric tier is
# historical naming, NOT authoritative current slot metadata: research overrides
# may deliberately move a card to a different legal type/cost slot.
#
# This mapping is retained as a convenient canonical-name fixture for tests and
# synthetic data construction. Complete-market validation must use only its keys
# for identity membership; current type/cost comes from the typed JSON itself and
# is validated by the legal four-cards-per-slot invariant below.
EXPECTED_CURRENT_MARKET: dict[str, MarketSlotIdentity] = {
    f"{prefix}_{cost:02d}_{ordinal:02d}": MarketSlotIdentity(plant_type, cost)
    for plant_type, prefix in (("ROOT", "Root"), ("VINE", "Vine"), ("FLOWER", "Flower"))
    for cost in LEGAL_SLOT_COSTS[plant_type]
    for ordinal in range(1, 5)
}


class MarketResultValidationError(ValueError):
    """Raised when raw evaluator JSON violates the supported market schema."""


def load_market_result(path: Path | str, *, complete_market: bool = True) -> MarketRawResult:
    """Load and strictly validate one Kotlin market-evaluation JSON file."""

    source = Path(path)
    try:
        payload = json.loads(source.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError) as exc:
        raise MarketResultValidationError(f"Could not read valid market JSON from {source}: {exc}") from exc
    result = parse_market_result(payload, complete_market=complete_market)
    return MarketRawResult(
        schema=result.schema,
        schema_version=result.schema_version,
        metadata=result.metadata,
        control=result.control,
        learned=result.learned,
        cards=result.cards,
        source_path=source.resolve(),
    )


def parse_market_result(payload: Any, *, complete_market: bool = True) -> MarketRawResult:
    """Validate a decoded JSON value and return immutable typed domain objects."""

    root = _object(payload, "$")
    schema = _string(_required(root, "schema", "$"), "$.schema")
    version = _integer(_required(root, "schemaVersion", "$"), "$.schemaVersion")
    if schema != MARKET_SCHEMA:
        raise MarketResultValidationError(f"$.schema: expected {MARKET_SCHEMA!r}, got {schema!r}")
    if version != MARKET_SCHEMA_VERSION:
        raise MarketResultValidationError(
            f"$.schemaVersion: unsupported version {version}; supported={MARKET_SCHEMA_VERSION}"
        )

    experiment = _object(_required(root, "experiment", "$"), "$.experiment")
    players = _positive_int(_required(experiment, "players", "$.experiment"), "$.experiment.players")
    samples = _nonnegative_int(_required(experiment, "samples", "$.experiment"), "$.experiment.samples")
    metadata = ExperimentMetadata(
        players=players,
        samples=samples,
        mechanical_seed_start=_integer(
            _required(experiment, "mechanicalSeedStart", "$.experiment"),
            "$.experiment.mechanicalSeedStart",
        ),
        strategy_seed_start=_integer(
            _required(experiment, "strategySeedStart", "$.experiment"),
            "$.experiment.strategySeedStart",
        ),
        grove_seed_start=_optional_integer(experiment.get("groveSeedStart"), "$.experiment.groveSeedStart"),
        grove_pattern=_optional_string(experiment.get("grovePattern"), "$.experiment.grovePattern"),
        round_pattern=_string(_required(experiment, "roundPattern", "$.experiment"), "$.experiment.roundPattern"),
        policy_path=_string(_required(experiment, "policyPath", "$.experiment"), "$.experiment.policyPath"),
        plant_overrides_path=_override_path(experiment, "plantOverrides"),
        plant_overrides_sha256=_override_sha(experiment, "plantOverrides"),
        round_overrides_path=_override_path(experiment, "roundOverrides"),
        round_overrides_sha256=_override_sha(experiment, "roundOverrides"),
    )

    outcomes = _object(_required(root, "outcomes", "$"), "$.outcomes")
    control = _parse_outcome(_required(outcomes, "control", "$.outcomes"), "$.outcomes.control", "CONTROL")
    learned = _parse_outcome(_required(outcomes, "learned", "$.outcomes"), "$.outcomes.learned", "LEARNED")
    for name, outcome in (("control", control), ("learned", learned)):
        if outcome.sample_count != samples:
            raise MarketResultValidationError(
                f"$.outcomes.{name}.sampleCount={outcome.sample_count} does not match experiment samples={samples}"
            )

    raw_cards = _required(root, "cards", "$")
    if not isinstance(raw_cards, list):
        raise MarketResultValidationError("$.cards: expected array")
    cards = tuple(_parse_card(item, f"$.cards[{index}]") for index, item in enumerate(raw_cards))

    identities = [card.identity for card in cards]
    duplicates = sorted({name for name in identities if identities.count(name) > 1})
    if duplicates:
        raise MarketResultValidationError(f"$.cards: duplicate card identities: {', '.join(duplicates)}")

    _validate_slots(cards)
    if complete_market:
        _validate_complete_current_market(cards)

    control_purchase_total = sum(card.control_purchases for card in cards)
    learned_purchase_total = sum(card.learned_purchases for card in cards)
    if control_purchase_total != control.plant_purchases:
        raise MarketResultValidationError(
            "control plantPurchases does not reconcile with sum of per-card controlPurchases: "
            f"{control.plant_purchases} != {control_purchase_total}"
        )
    if learned_purchase_total != learned.plant_purchases:
        raise MarketResultValidationError(
            "learned plantPurchases does not reconcile with sum of per-card learnedPurchases: "
            f"{learned.plant_purchases} != {learned_purchase_total}"
        )

    return MarketRawResult(
        schema=schema,
        schema_version=version,
        metadata=metadata,
        control=control,
        learned=learned,
        cards=cards,
    )


def _parse_outcome(value: Any, path: str, expected_role: str) -> LearnerResult:
    obj = _object(value, path)
    role = _string(_required(obj, "role", path), f"{path}.role")
    if role != expected_role:
        raise MarketResultValidationError(f"{path}.role: expected {expected_role!r}, got {role!r}")
    return LearnerResult(
        role=role,
        sample_count=_nonnegative_int(_required(obj, "sampleCount", path), f"{path}.sampleCount"),
        win_share=_finite_float(_required(obj, "winShare", path), f"{path}.winShare"),
        plant_purchases=_nonnegative_int(_required(obj, "plantPurchases", path), f"{path}.plantPurchases"),
    )


def _parse_card(value: Any, path: str) -> CardResult:
    obj = _object(value, path)
    identity = _string(_required(obj, "identity", path), f"{path}.identity")
    plant_type = _string(_required(obj, "type", path), f"{path}.type")
    cost = _nonnegative_int(_required(obj, "cost", path), f"{path}.cost")
    if MarketSlotIdentity(plant_type, cost) not in LEGAL_SLOTS:
        raise MarketResultValidationError(f"{path}: illegal market slot {plant_type} {cost}")

    control_exposure = _nonnegative_int(_required(obj, "controlExposure", path), f"{path}.controlExposure")
    learned_exposure = _nonnegative_int(_required(obj, "learnedExposure", path), f"{path}.learnedExposure")
    control_purchases = _nonnegative_int(_required(obj, "controlPurchases", path), f"{path}.controlPurchases")
    learned_purchases = _nonnegative_int(_required(obj, "learnedPurchases", path), f"{path}.learnedPurchases")
    if control_purchases > 0 and control_exposure == 0:
        raise MarketResultValidationError(
            f"{path}: controlPurchases={control_purchases} with controlExposure=0 is impossible"
        )
    if learned_purchases > 0 and learned_exposure == 0:
        raise MarketResultValidationError(
            f"{path}: learnedPurchases={learned_purchases} with learnedExposure=0 is impossible"
        )

    return CardResult(
        identity=identity,
        plant_type=plant_type,
        cost=cost,
        control_exposure=control_exposure,
        learned_exposure=learned_exposure,
        control_purchases=control_purchases,
        learned_purchases=learned_purchases,
        control_win_share_on_exposure_sum=_finite_float(
            _required(obj, "controlWinShareOnExposureSum", path),
            f"{path}.controlWinShareOnExposureSum",
        ),
        learned_win_share_on_exposure_sum=_finite_float(
            _required(obj, "learnedWinShareOnExposureSum", path),
            f"{path}.learnedWinShareOnExposureSum",
        ),
    )


def _validate_slots(cards: tuple[CardResult, ...]) -> None:
    by_slot: dict[MarketSlotIdentity, list[str]] = {}
    for card in cards:
        by_slot.setdefault(card.slot, []).append(card.identity)

    # Only enforce exactly four per slot when a result represents a full 36-card
    # catalog. Small fixtures/non-complete reads remain useful for unit testing.
    if len(cards) == 36:
        for slot in sorted(LEGAL_SLOTS):
            count = len(by_slot.get(slot, ()))
            if count != 4:
                raise MarketResultValidationError(
                    f"$.cards: market slot {slot.plant_type} {slot.cost} must contain exactly 4 cards; got {count}"
                )
        unexpected = sorted(set(by_slot) - LEGAL_SLOTS)
        if unexpected:
            raise MarketResultValidationError(f"$.cards: illegal market slots: {unexpected}")


def _validate_complete_current_market(cards: tuple[CardResult, ...]) -> None:
    if len(cards) != 36:
        raise MarketResultValidationError(f"$.cards: complete-market mode requires exactly 36 cards; got {len(cards)}")
    actual = {card.identity: card.slot for card in cards}
    expected_names = set(EXPECTED_CURRENT_MARKET)
    actual_names = set(actual)
    if actual_names != expected_names:
        missing = sorted(expected_names - actual_names)
        extra = sorted(actual_names - expected_names)
        raise MarketResultValidationError(
            f"$.cards: current market identity mismatch; missing={missing or '[]'} extra={extra or '[]'}"
        )
    # Do NOT compare current slot metadata to the tier embedded in the stable
    # identity (for example Root_07_01). Current research overrides may legally
    # move that card to Root 9. _validate_slots() above is the authoritative
    # structural check: every card must occupy a legal slot and a complete
    # market must have exactly four cards in each of the nine slots.


def _override_path(experiment: Mapping[str, Any], key: str) -> str | None:
    value = _required(experiment, key, "$.experiment")
    if value is None:
        return None
    obj = _object(value, f"$.experiment.{key}")
    return _string(_required(obj, "path", f"$.experiment.{key}"), f"$.experiment.{key}.path")


def _override_sha(experiment: Mapping[str, Any], key: str) -> str | None:
    value = _required(experiment, key, "$.experiment")
    if value is None:
        return None
    obj = _object(value, f"$.experiment.{key}")
    return _optional_string(obj.get("sha256"), f"$.experiment.{key}.sha256")


def _required(obj: Mapping[str, Any], key: str, path: str) -> Any:
    if key not in obj:
        raise MarketResultValidationError(f"{path}: missing required field {key!r}")
    return obj[key]


def _object(value: Any, path: str) -> Mapping[str, Any]:
    if not isinstance(value, dict):
        raise MarketResultValidationError(f"{path}: expected object")
    return value


def _string(value: Any, path: str) -> str:
    if not isinstance(value, str) or not value:
        raise MarketResultValidationError(f"{path}: expected non-empty string")
    return value


def _optional_string(value: Any, path: str) -> str | None:
    if value is None:
        return None
    return _string(value, path)


def _integer(value: Any, path: str) -> int:
    if isinstance(value, bool) or not isinstance(value, int):
        raise MarketResultValidationError(f"{path}: expected integer")
    return value


def _optional_integer(value: Any, path: str) -> int | None:
    if value is None:
        return None
    return _integer(value, path)


def _nonnegative_int(value: Any, path: str) -> int:
    result = _integer(value, path)
    if result < 0:
        raise MarketResultValidationError(f"{path}: must be non-negative")
    return result


def _positive_int(value: Any, path: str) -> int:
    result = _integer(value, path)
    if result <= 0:
        raise MarketResultValidationError(f"{path}: must be positive")
    return result


def _finite_float(value: Any, path: str) -> float:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise MarketResultValidationError(f"{path}: expected number")
    result = float(value)
    if not math.isfinite(result):
        raise MarketResultValidationError(f"{path}: expected finite number")
    return result
