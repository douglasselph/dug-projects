from __future__ import annotations

from pathlib import Path

import pytest

from leaf_experiments.market import (
    MARKET_SCHEMA,
    MARKET_SCHEMA_VERSION,
    MarketResultValidationError,
    MarketSlotIdentity,
    load_market_result,
)
from leaf_experiments.paths import fixture_path


FIXTURE_DIR = fixture_path("market")


def load(name: str, *, complete_market: bool = True):
    return load_market_result(FIXTURE_DIR / name, complete_market=complete_market)


def test_valid_complete_result_loads_and_preserves_metadata() -> None:
    result = load("valid_result.json")

    assert result.schema == MARKET_SCHEMA
    assert result.schema_version == MARKET_SCHEMA_VERSION
    assert result.metadata.players == 4
    assert result.metadata.samples == 20
    assert result.metadata.policy_path == "output/test/learner-2.weights"
    assert result.learned.role == "LEARNED"
    assert result.learned.win_share == pytest.approx(0.40)
    assert result.control.role == "CONTROL"
    assert len(result.cards) == 36
    assert result.source_path == (FIXTURE_DIR / "valid_result.json").resolve()


def test_zero_purchase_card_is_preserved_explicitly() -> None:
    result = load("zero_purchase.json")
    card = result.cards_by_identity["Vine_09_02"]

    assert card.learned_purchases == 0
    assert card.control_purchases == 0
    assert card.learned_exposure == 4
    assert card.control_exposure == 4


def test_zero_exposure_with_zero_purchase_is_valid_for_partial_fixture() -> None:
    result = load("zero_exposure.json", complete_market=False)
    card = result.cards[0]

    assert card.control_exposure == 0
    assert card.learned_exposure == 0
    assert card.control_purchases == 0
    assert card.learned_purchases == 0


def test_malformed_json_is_rejected() -> None:
    with pytest.raises(MarketResultValidationError, match="Could not read valid market JSON"):
        load("malformed.json")


def test_missing_required_field_is_rejected() -> None:
    with pytest.raises(MarketResultValidationError, match="missing required field 'players'"):
        load("missing_field.json")


def test_duplicate_card_identity_is_rejected() -> None:
    with pytest.raises(MarketResultValidationError, match="duplicate card identities"):
        load("duplicate_card.json")


def test_negative_purchase_or_exposure_count_is_rejected() -> None:
    with pytest.raises(MarketResultValidationError, match="must be non-negative"):
        load("negative_count.json", complete_market=False)


def test_purchase_with_zero_exposure_is_rejected() -> None:
    with pytest.raises(MarketResultValidationError, match="with learnedExposure=0 is impossible"):
        load("purchase_without_exposure.json", complete_market=False)


def test_unsupported_schema_version_is_rejected() -> None:
    with pytest.raises(MarketResultValidationError, match="unsupported version 99"):
        load("wrong_schema_version.json")


def test_complete_market_mode_rejects_missing_card() -> None:
    with pytest.raises(MarketResultValidationError, match="requires exactly 36 cards"):
        load("missing_market_card.json")


def test_complete_market_rejects_duplicate_slot_membership() -> None:
    with pytest.raises(MarketResultValidationError, match="ROOT 5 must contain exactly 4 cards; got 5"):
        load("duplicate_slot_membership.json")


def test_all_nine_legal_slots_have_exactly_four_cards() -> None:
    result = load("valid_result.json")
    counts: dict[MarketSlotIdentity, int] = {}
    for card in result.cards:
        counts[card.slot] = counts.get(card.slot, 0) + 1

    assert len(counts) == 9
    assert set(counts.values()) == {4}


def test_purchase_totals_reconcile_with_role_outcomes() -> None:
    result = load("valid_result.json")

    assert sum(card.control_purchases for card in result.cards) == result.control.plant_purchases == 1
    assert sum(card.learned_purchases for card in result.cards) == result.learned.plant_purchases == 3


def test_type_and_cost_are_preserved_in_slot_identity() -> None:
    result = load("valid_result.json")
    card = result.cards_by_identity["Flower_17_04"]

    assert card.plant_type == "FLOWER"
    assert card.cost == 17
    assert card.slot == MarketSlotIdentity("FLOWER", 17)


def test_complete_market_requires_exact_current_card_identities() -> None:
    with pytest.raises(MarketResultValidationError, match="current market identity mismatch"):
        load("wrong_market_identity.json")


def test_complete_market_accepts_legal_current_slot_moves_despite_historical_identity_tiers() -> None:
    result = load("valid_result.json")
    payload = __import__("json").loads((FIXTURE_DIR / "valid_result.json").read_text(encoding="utf-8"))

    # Mirror current research behavior: stable card IDs can move between legal
    # slots. Swap pairs so all nine slots still contain exactly four cards.
    moves = {
        "Root_07_01": ("ROOT", 9),
        "Root_09_01": ("ROOT", 7),
        "Vine_07_01": ("VINE", 11),
        "Vine_11_03": ("VINE", 7),
    }
    for card in payload["cards"]:
        if card["identity"] in moves:
            card["type"], card["cost"] = moves[card["identity"]]

    from leaf_experiments.market.reader import parse_market_result
    moved = parse_market_result(payload, complete_market=True)

    assert moved.cards_by_identity["Root_07_01"].slot == MarketSlotIdentity("ROOT", 9)
    assert moved.cards_by_identity["Root_09_01"].slot == MarketSlotIdentity("ROOT", 7)
    assert moved.cards_by_identity["Vine_07_01"].slot == MarketSlotIdentity("VINE", 11)
    assert moved.cards_by_identity["Vine_11_03"].slot == MarketSlotIdentity("VINE", 7)
