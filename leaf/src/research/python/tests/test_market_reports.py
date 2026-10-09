from __future__ import annotations

from dataclasses import replace
from pathlib import Path

import pytest

from leaf_experiments.market import (
    BuyOpportunityStats,
    CardResult,
    ExperimentMetadata,
    LearnerResult,
    MarketRawResult,
    MarketReportValidationError,
    render_card_purchases,
    render_card_summary,
    render_card_opportunity_summary,
    render_slot_opportunity_summary,
    render_card_opportunity_by_round,
    render_learner_win_shares,
    render_one_learner_only_cards,
    render_readme_first,
    render_slot_summary,
    render_vine_9_summary,
    render_zero_purchase_cards,
    write_market_reports,
)
from leaf_experiments.market.reader import EXPECTED_CURRENT_MARKET

GOLDEN = Path(__file__).parent / "fixtures" / "market_reports"


def make_result(
    *,
    learner: int,
    win_share: float,
    control_win_share: float = 0.25,
    overrides: dict[str, tuple[int, int]] | None = None,
    control_overrides: dict[str, tuple[int, int]] | None = None,
    players: int = 4,
) -> MarketRawResult:
    learned_values = overrides or {}
    control_values = control_overrides or {}
    cards: list[CardResult] = []
    learned_total = 0
    control_total = 0
    for identity, slot in EXPECTED_CURRENT_MARKET.items():
        learned_exposure, learned_purchases = learned_values.get(identity, (4, 0))
        control_exposure, control_purchases = control_values.get(identity, (4, 0))
        learned_total += learned_purchases
        control_total += control_purchases
        cards.append(
            CardResult(
                identity=identity,
                plant_type=slot.plant_type,
                cost=slot.cost,
                control_exposure=control_exposure,
                learned_exposure=learned_exposure,
                control_purchases=control_purchases,
                learned_purchases=learned_purchases,
                control_win_share_on_exposure_sum=control_exposure * control_win_share,
                learned_win_share_on_exposure_sum=learned_exposure * win_share,
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
            policy_path=f"output/{players}p/weights/learner-{learner}.weights",
            plant_overrides_path="data/research/4p/resync/resync-current.csv",
            plant_overrides_sha256="plant-sha",
            round_overrides_path="data/research/4p/resync/round-resync-current.csv",
            round_overrides_sha256="round-sha",
        ),
        control=LearnerResult("CONTROL", 20, control_win_share, control_total),
        learned=LearnerResult("LEARNED", 20, win_share, learned_total),
        cards=tuple(cards),
    )


def golden_results() -> tuple[MarketRawResult, ...]:
    one = make_result(
        learner=1,
        win_share=0.41,
        overrides={
            "Root_05_01": (3, 2),
            "Vine_09_01": (2, 1),
            "Vine_09_02": (0, 0),
            "Flower_17_04": (2, 5),
        },
        control_overrides={"Root_05_03": (4, 1)},
    )
    two = make_result(
        learner=2,
        win_share=0.52,
        overrides={
            "Root_05_01": (5, 0),
            "Root_05_02": (2, 0),
            "Vine_09_01": (3, 2),
            "Vine_09_02": (0, 0),
            "Flower_17_04": (1, 0),
        },
        control_overrides={"Root_05_03": (4, 1)},
    )
    return one, two


@pytest.mark.parametrize(
    ("filename", "renderer"),
    [
        ("card-purchases.tsv", render_card_purchases),
        ("card-summary.tsv", render_card_summary),
        ("slot-summary.tsv", render_slot_summary),
        ("zero-purchase-cards.tsv", render_zero_purchase_cards),
        ("one-learner-only-cards.tsv", render_one_learner_only_cards),
        ("vine-9-summary.tsv", render_vine_9_summary),
        ("learner-win-shares.tsv", render_learner_win_shares),
        ("README-FIRST.txt", lambda _results: render_readme_first()),
    ],
)
def test_reports_match_committed_golden_fixtures(filename: str, renderer) -> None:
    assert renderer(golden_results()) == (GOLDEN / filename).read_text(encoding="utf-8")


def test_column_order_and_precision_are_stable() -> None:
    lines = render_card_purchases(golden_results()).splitlines()
    assert lines[0] == (
        "players\tlearner\tcontrol_win_share_pct\theld_out_win_share_pct\tcard\ttype\tcost\t"
        "grove_exposures\tcontrol_purchases\tlearned_purchases\tlearned_purchases_per_exposure"
    )
    root = next(line for line in lines if "learner-1\t" in line and "\tRoot_05_01\t" in line)
    assert root.endswith("\t3\t0\t2\t0.666667")
    assert "\t25.000\t41.000\t" in root


def test_zero_exposure_is_rendered_as_na_not_zero() -> None:
    lines = render_card_purchases(golden_results()).splitlines()
    row = next(line for line in lines if "learner-1\t" in line and "\tVine_09_02\t" in line)
    assert row.endswith("\t0\t0\t0\tNA")

    summary = render_card_summary(golden_results()).splitlines()
    row = next(line for line in summary if "\tVine_09_02\t" in line)
    assert "\t0\t0\tNA\t" in row


def test_zero_purchase_filter_only_contains_zero_purchase_cards() -> None:
    rows = render_zero_purchase_cards(golden_results()).splitlines()[1:]
    assert rows
    assert all(int(row.split("\t")[5]) == 0 for row in rows)
    assert not any("\tRoot_05_01\t" in row for row in rows)


def test_one_learner_filter_counts_actual_purchasers() -> None:
    rows = render_one_learner_only_cards(golden_results()).splitlines()[1:]
    names = {row.split("\t")[1] for row in rows}
    assert "Root_05_01" in names
    assert "Flower_17_04" in names
    assert "Vine_09_01" not in names
    assert all(int(row.split("\t")[8]) == 1 for row in rows)


def test_vine_9_filter_is_exact() -> None:
    rows = render_vine_9_summary(golden_results()).splitlines()[1:]
    assert [row.split("\t")[1] for row in rows] == [
        "Vine_09_01",
        "Vine_09_02",
        "Vine_09_03",
        "Vine_09_04",
    ]
    assert all(row.split("\t")[2:4] == ["VINE", "9"] for row in rows)


def test_best_learner_win_share_is_best_actual_purchaser() -> None:
    rows = render_card_summary(golden_results()).splitlines()
    root = next(row for row in rows if "\tRoot_05_01\t" in row)
    vine = next(row for row in rows if "\tVine_09_01\t" in row)
    zero = next(row for row in rows if "\tRoot_05_02\t" in row)
    assert root.endswith("\t1\t41.000")
    assert vine.endswith("\t2\t52.000")
    assert zero.endswith("\t0\tNA")


def test_learner_win_share_output_uses_structured_outcomes() -> None:
    rows = render_learner_win_shares(golden_results()).splitlines()
    assert rows == [
        "players\tlearner\tcontrol_win_share_pct\theld_out_win_share_pct",
        "4\tlearner-1\t25.000\t41.000",
        "4\tlearner-2\t25.000\t52.000",
    ]


def test_report_row_order_is_independent_of_input_order() -> None:
    forward = golden_results()
    reverse = tuple(reversed(forward))
    assert render_card_purchases(forward) == render_card_purchases(reverse)
    assert render_card_summary(forward) == render_card_summary(reverse)
    assert render_slot_summary(forward) == render_slot_summary(reverse)


def test_write_market_reports_writes_all_expected_files(tmp_path: Path) -> None:
    paths = write_market_reports(golden_results(), tmp_path)
    assert set(paths) == {
        "card-purchases.tsv",
        "card-summary.tsv",
        "slot-summary.tsv",
        "zero-purchase-cards.tsv",
        "one-learner-only-cards.tsv",
        "vine-9-summary.tsv",
        "learner-win-shares.tsv",
        "card-opportunity-summary.tsv",
        "slot-opportunity-summary.tsv",
        "card-opportunity-by-round.tsv",
        "card-substitution-summary.tsv",
        "card-substitutions-by-learner.tsv",
        "README-FIRST.txt",
    }
    for name, path in paths.items():
        assert path == tmp_path / name
        if (GOLDEN / name).exists():
            assert path.read_text(encoding="utf-8") == (GOLDEN / name).read_text(encoding="utf-8")
        else:
            assert path.read_text(encoding="utf-8").splitlines()[0]


def test_missing_current_market_card_fails_before_reporting() -> None:
    result = golden_results()[0]
    broken = replace(result, cards=tuple(card for card in result.cards if card.identity != "Root_05_04"))
    with pytest.raises(MarketReportValidationError, match="complete current 36-card market"):
        render_card_summary([broken])


def test_duplicate_card_identity_fails_before_reporting() -> None:
    result = golden_results()[0]
    cards = list(result.cards)
    cards[-1] = cards[0]
    broken = replace(result, cards=tuple(cards))
    with pytest.raises(MarketReportValidationError, match="duplicate card identities"):
        render_card_summary([broken])


def test_wrong_slot_metadata_fails_before_reporting() -> None:
    result = golden_results()[0]
    broken_cards = tuple(
        replace(card, cost=7) if card.identity == "Root_05_01" else card for card in result.cards
    )
    broken = replace(result, cards=broken_cards)
    with pytest.raises(MarketReportValidationError, match="slot ROOT 5 must contain 4 cards"):
        render_card_summary([broken])


def test_impossible_positive_purchase_with_zero_exposure_fails_loudly() -> None:
    result = golden_results()[0]
    broken_cards = tuple(
        replace(card, learned_exposure=0, learned_purchases=1)
        if card.identity == "Root_05_01"
        else card
        for card in result.cards
    )
    # Keep the overall result internally reconciled so this specifically tests
    # the impossible exposure/purchase invariant.
    broken = replace(result, cards=broken_cards, learned=replace(result.learned, plant_purchases=7))
    with pytest.raises(MarketReportValidationError, match="learned purchases with zero exposure"):
        render_card_summary([broken])


def test_per_card_purchase_total_must_reconcile_to_outcome() -> None:
    result = golden_results()[0]
    broken = replace(result, learned=replace(result.learned, plant_purchases=999))
    with pytest.raises(MarketReportValidationError, match="do not reconcile"):
        render_card_summary([broken])



def test_duplicate_learner_result_is_rejected_before_aggregation() -> None:
    result = golden_results()[0]
    with pytest.raises(MarketReportValidationError, match="duplicate learner result for 4p"):
        render_card_summary([result, result])


def test_ambiguous_duplicate_learner_label_is_rejected() -> None:
    one, two = golden_results()
    ambiguous = replace(
        two,
        metadata=replace(two.metadata, policy_path="other/location/learner-1.weights"),
    )
    with pytest.raises(MarketReportValidationError, match="duplicate learner label for 4p: learner-1"):
        render_learner_win_shares([one, ambiguous])

def test_player_counts_are_reported_separately_and_sorted() -> None:
    two = make_result(learner=1, players=2, win_share=0.60, overrides={"Root_05_01": (3, 1)})
    four = golden_results()[0]
    lines = render_learner_win_shares([four, two]).splitlines()
    assert lines[1].startswith("2\tlearner-1\t")
    assert lines[2].startswith("4\tlearner-1\t")


def test_reports_accept_legal_slot_moves_even_when_identity_contains_historical_tier() -> None:
    result = golden_results()[0]
    moves = {
        "Root_07_01": ("ROOT", 9),
        "Root_09_01": ("ROOT", 7),
        "Vine_07_01": ("VINE", 11),
        "Vine_11_03": ("VINE", 7),
    }
    moved_cards = tuple(
        replace(card, plant_type=moves[card.identity][0], cost=moves[card.identity][1])
        if card.identity in moves else card
        for card in result.cards
    )
    moved = replace(result, cards=moved_cards)

    text = render_card_summary([moved])

    assert "Root_07_01\tROOT\t9" in text
    assert "Root_09_01\tROOT\t7" in text
    assert "Vine_07_01\tVINE\t11" in text
    assert "Vine_11_03\tVINE\t7" in text


def test_v2_opportunity_reports_distinguish_access_from_rejection() -> None:
    base = make_result(learner=1, win_share=0.50)
    cards = []
    for card in base.cards:
        learned = BuyOpportunityStats(
            market_decisions=10, affordable_decisions=2, graftable_decisions=10,
            legal_decisions=2, selected_decisions=card.learned_purchases,
            player_done_while_legal=1 if card.identity == "Flower_17_04" else 0,
            no_legal_items_while_market=4, first_decision_legal=2, post_purchase_legal=0,
            legal_with_higher_cost_plant=0, purchasing_power_on_market_sum=90,
            market_by_cultivation_round=((1, 4), (2, 6)),
            affordable_by_cultivation_round=((2, 2),),
            legal_by_cultivation_round=((2, 2),),
            selected_by_cultivation_round=(),
        )
        control = BuyOpportunityStats(
            market_decisions=10, affordable_decisions=5, graftable_decisions=10,
            legal_decisions=5, selected_decisions=card.control_purchases,
            first_decision_legal=5, purchasing_power_on_market_sum=110,
            market_by_cultivation_round=((1, 5), (2, 5)),
            affordable_by_cultivation_round=((2, 5),),
            legal_by_cultivation_round=((2, 5),),
            selected_by_cultivation_round=(),
        )
        cards.append(replace(card, control_opportunity=control, learned_opportunity=learned))
    result = replace(base, schema_version=2, cards=tuple(cards))

    card_report = render_card_opportunity_summary([result])
    flower = next(line for line in card_report.splitlines() if "LEARNED\tFlower_17_04" in line)
    fields = flower.split("\t")
    assert fields[6:14] == ["10", "2", "0.200000", "10", "1.000000", "2", "0.200000", "0"]
    assert fields[-1] == "9.000"

    slot_report = render_slot_opportunity_summary([result])
    assert "4\tLEARNED\tFLOWER\t17" in slot_report

    round_report = render_card_opportunity_by_round([result])
    assert "4\tLEARNED\tFlower_17_04\tFLOWER\t17\t1\t4\t0\t0\t0" in round_report
    assert "4\tLEARNED\tFlower_17_04\tFLOWER\t17\t2\t6\t2\t2\t0" in round_report


def test_substitution_reports_show_exact_alternative_selected() -> None:
    from leaf_experiments.market import RejectedLegalAlternative, render_card_substitution_summary, render_card_substitutions_by_learner

    base = golden_results()[0]
    cards = []
    for card in base.cards:
        if card.identity == "Flower_14_01":
            learned = BuyOpportunityStats(
                market_decisions=10,
                affordable_decisions=10,
                graftable_decisions=10,
                legal_decisions=10,
                selected_decisions=2,
                first_decision_legal=10,
                rejected_legal_alternatives=(
                    RejectedLegalAlternative("PURCHASE", "PLANT", "Flower_17_04", 17, 5),
                    RejectedLegalAlternative("PURCHASE", "DIE", "D20", 12, 2),
                    RejectedLegalAlternative("PLAYER_DONE", None, None, None, 1),
                ),
            )
            cards.append(replace(card, learned_opportunity=learned))
        else:
            cards.append(card)
    result = replace(base, schema_version=3, cards=tuple(cards))

    summary = render_card_substitution_summary([result]).splitlines()
    flower17 = next(row for row in summary if "\tFlower_14_01\t" in row and "\tFlower_17_04\t" in row)
    assert flower17.endswith("\t5\t0.625000\t1")

    by_learner = render_card_substitutions_by_learner([result]).splitlines()
    die = next(row for row in by_learner if "\tFlower_14_01\t" in row and "\tD20\t" in row)
    assert die.endswith("\t2\t0.250000")
