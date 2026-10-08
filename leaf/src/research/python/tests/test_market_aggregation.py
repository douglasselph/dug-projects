from __future__ import annotations

from dataclasses import replace

import pytest

from leaf_experiments.market import (
    CardResult,
    ExperimentMetadata,
    LearnerResult,
    MarketRawResult,
    MarketSlotIdentity,
    aggregate_market_results,
    aggregate_player_count,
    one_learner_only_cards,
    safe_rate,
    zero_purchase_cards,
)
from leaf_experiments.market.reader import EXPECTED_CURRENT_MARKET


def make_result(
    *,
    players: int = 4,
    policy: str = "learner-1.weights",
    win_share: float = 0.40,
    learned_overrides: dict[str, tuple[int, int]] | None = None,
) -> MarketRawResult:
    overrides = learned_overrides or {}
    cards = []
    learned_purchase_total = 0
    for identity, slot in EXPECTED_CURRENT_MARKET.items():
        exposure, purchases = overrides.get(identity, (4, 0))
        learned_purchase_total += purchases
        cards.append(
            CardResult(
                identity=identity,
                plant_type=slot.plant_type,
                cost=slot.cost,
                control_exposure=4,
                learned_exposure=exposure,
                control_purchases=0,
                learned_purchases=purchases,
                control_win_share_on_exposure_sum=1.0,
                learned_win_share_on_exposure_sum=1.0,
            )
        )
    return MarketRawResult(
        schema="leaf.market-evaluation",
        schema_version=1,
        metadata=ExperimentMetadata(
            players=players,
            samples=20,
            mechanical_seed_start=100,
            strategy_seed_start=200,
            grove_seed_start=300,
            grove_pattern="RANDOM_PATTERN",
            round_pattern="3/2/2",
            policy_path=policy,
            plant_overrides_path=None,
            plant_overrides_sha256=None,
            round_overrides_path=None,
            round_overrides_sha256=None,
        ),
        control=LearnerResult("CONTROL", 20, 1.0 / players, 0),
        learned=LearnerResult("LEARNED", 20, win_share, learned_purchase_total),
        cards=tuple(cards),
    )


def test_safe_rate_preserves_undefined_zero_denominator() -> None:
    assert safe_rate(0, 0) is None
    assert safe_rate(2, 0) is None
    assert safe_rate(0, 2) == 0.0
    assert safe_rate(2, 3) == pytest.approx(2.0 / 3.0)


def test_one_learner_exact_card_arithmetic() -> None:
    result = make_result(learned_overrides={"Root_05_01": (3, 2)})
    aggregate = aggregate_player_count([result])
    card = aggregate.cards_by_identity["Root_05_01"]

    assert aggregate.player_count == 4
    assert aggregate.learners_evaluated == 1
    assert card.total_exposure == 3
    assert card.total_purchases == 2
    assert card.purchases_per_exposure == pytest.approx(2.0 / 3.0)
    assert card.learners_evaluated == 1
    assert card.learners_purchasing == 1


def test_exposed_twice_never_purchased_has_real_zero_rate() -> None:
    result = make_result(learned_overrides={"Vine_09_02": (2, 0)})
    card = aggregate_player_count([result]).cards_by_identity["Vine_09_02"]

    assert card.total_exposure == 2
    assert card.total_purchases == 0
    assert card.purchases_per_exposure == 0.0


def test_zero_exposure_rate_is_none_not_fabricated_zero() -> None:
    result = make_result(learned_overrides={"Flower_17_01": (0, 0)})
    card = aggregate_player_count([result]).cards_by_identity["Flower_17_01"]

    assert card.total_exposure == 0
    assert card.total_purchases == 0
    assert card.purchases_per_exposure is None


def test_multiple_purchases_per_exposure_are_preserved() -> None:
    result = make_result(learned_overrides={"Root_05_01": (2, 5)})
    card = aggregate_player_count([result]).cards_by_identity["Root_05_01"]

    assert card.total_exposure == 2
    assert card.total_purchases == 5
    assert card.purchases_per_exposure == 2.5


def test_multiple_learners_sum_exposure_and_purchases() -> None:
    r1 = make_result(policy="l1", learned_overrides={"Root_05_01": (3, 2)})
    r2 = make_result(policy="l2", learned_overrides={"Root_05_01": (5, 4)})
    card = aggregate_player_count([r1, r2]).cards_by_identity["Root_05_01"]

    assert card.total_exposure == 8
    assert card.total_purchases == 6
    assert card.purchases_per_exposure == pytest.approx(0.75)
    assert card.learners_evaluated == 2
    assert card.learners_purchasing == 2


def test_learners_purchasing_counts_only_positive_purchase_learners() -> None:
    r1 = make_result(policy="l1", learned_overrides={"Vine_09_02": (4, 0)})
    r2 = make_result(policy="l2", learned_overrides={"Vine_09_02": (4, 3)})
    r3 = make_result(policy="l3", learned_overrides={"Vine_09_02": (4, 0)})
    card = aggregate_player_count([r1, r2, r3]).cards_by_identity["Vine_09_02"]

    assert card.learners_evaluated == 3
    assert card.learners_purchasing == 1


def test_best_performing_learner_is_best_among_actual_purchasers_only() -> None:
    r1 = make_result(policy="purchaser-low", win_share=0.41, learned_overrides={"Vine_09_02": (4, 1)})
    r2 = make_result(policy="non-purchaser-high", win_share=0.90, learned_overrides={"Vine_09_02": (4, 0)})
    r3 = make_result(policy="purchaser-best", win_share=0.55, learned_overrides={"Vine_09_02": (4, 2)})
    card = aggregate_player_count([r1, r2, r3]).cards_by_identity["Vine_09_02"]

    assert card.best_purchasing_learner is not None
    assert card.best_purchasing_learner.policy_path == "purchaser-best"
    assert card.best_purchasing_learner.win_share == pytest.approx(0.55)


def test_zero_purchase_cards_and_one_learner_only_cards() -> None:
    r1 = make_result(policy="l1", learned_overrides={"Root_05_01": (4, 2)})
    r2 = make_result(policy="l2", learned_overrides={"Root_05_01": (4, 0), "Root_05_02": (4, 1)})
    aggregate = aggregate_player_count([r1, r2])

    zero_names = {card.identity for card in zero_purchase_cards(aggregate)}
    one_names = {card.identity for card in one_learner_only_cards(aggregate)}
    assert "Root_05_03" in zero_names
    assert "Root_05_01" in one_names
    assert "Root_05_02" in one_names
    assert "Root_05_01" not in zero_names


def test_slot_totals_equal_sum_of_its_four_cards() -> None:
    result = make_result(
        learned_overrides={
            "Vine_09_01": (3, 1),
            "Vine_09_02": (4, 2),
            "Vine_09_03": (5, 3),
            "Vine_09_04": (6, 4),
        }
    )
    aggregate = aggregate_player_count([result])
    slot_id = MarketSlotIdentity("VINE", 9)
    slot = aggregate.slots_by_identity[slot_id]
    cards = [card for card in aggregate.cards if card.slot == slot_id]

    assert slot.card_count == 4
    assert slot.total_exposure == sum(card.total_exposure for card in cards) == 18
    assert slot.total_purchases == sum(card.total_purchases for card in cards) == 10
    assert slot.purchases_per_exposure == pytest.approx(10 / 18)


def test_whole_market_totals_equal_sum_of_nine_slots() -> None:
    result = make_result(
        learned_overrides={
            "Root_05_01": (3, 2),
            "Vine_09_02": (5, 4),
            "Flower_17_04": (2, 1),
        }
    )
    aggregate = aggregate_player_count([result])

    assert len(aggregate.slots) == 9
    assert aggregate.total_exposure == sum(slot.total_exposure for slot in aggregate.slots)
    assert aggregate.total_purchases == sum(slot.total_purchases for slot in aggregate.slots)


def test_player_count_specific_aggregates_are_kept_separate() -> None:
    two = make_result(players=2, policy="2p", learned_overrides={"Root_05_01": (3, 3)})
    four = make_result(players=4, policy="4p", learned_overrides={"Root_05_01": (7, 1)})
    aggregates = aggregate_market_results([four, two])

    assert [item.player_count for item in aggregates.by_player_count] == [2, 4]
    assert aggregates.for_players(2).cards_by_identity["Root_05_01"].total_purchases == 3
    assert aggregates.for_players(4).cards_by_identity["Root_05_01"].total_purchases == 1


def test_results_are_independent_of_input_ordering() -> None:
    r1 = make_result(policy="a", win_share=0.42, learned_overrides={"Root_05_01": (3, 2)})
    r2 = make_result(policy="b", win_share=0.52, learned_overrides={"Root_05_01": (5, 1)})
    forward = aggregate_market_results([r1, r2])
    reverse = aggregate_market_results([r2, r1])

    assert forward == reverse


def test_aggregate_player_count_rejects_mixed_player_counts() -> None:
    with pytest.raises(ValueError, match="requires one player count"):
        aggregate_player_count([make_result(players=2), make_result(players=4)])


def test_aggregate_player_count_requires_input() -> None:
    with pytest.raises(ValueError, match="at least one"):
        aggregate_player_count([])


def test_card_metadata_mismatch_between_learners_is_rejected() -> None:
    r1 = make_result(policy="a")
    r2 = make_result(policy="b")
    changed = tuple(
        replace(card, cost=6) if card.identity == "Root_05_01" else card for card in r2.cards
    )
    r2_bad = replace(r2, cards=changed)

    with pytest.raises(ValueError, match="card metadata differs"):
        aggregate_player_count([r1, r2_bad])


def test_card_identity_mismatch_between_learners_is_rejected() -> None:
    r1 = make_result(policy="a")
    r2 = make_result(policy="b")
    changed = tuple(card for card in r2.cards if card.identity != "Root_05_04")
    r2_bad = replace(r2, cards=changed)

    with pytest.raises(ValueError, match="market identities differ"):
        aggregate_player_count([r1, r2_bad])
