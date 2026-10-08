from __future__ import annotations

import json
from pathlib import Path

import pytest

from leaf_experiments.market import aggregate_market_results, load_market_result, write_market_reports
from leaf_experiments.market.reader import MarketResultValidationError

FIXTURES = Path(__file__).parent / "fixtures" / "market_e2e"


def _row_by_card(text: str, card: str) -> list[str]:
    for line in text.splitlines()[1:]:
        fields = line.split("\t")
        if len(fields) > 1 and fields[1] == card:
            return fields
    raise AssertionError(f"missing card row: {card}")


def _raw_row(text: str, learner: str, card: str) -> list[str]:
    for line in text.splitlines()[1:]:
        fields = line.split("\t")
        if len(fields) > 4 and fields[1] == learner and fields[4] == card:
            return fields
    raise AssertionError(f"missing raw row: {learner} / {card}")


def test_market_pipeline_end_to_end_from_kotlin_json_to_final_reports(tmp_path: Path) -> None:
    """Exercise the real structured market pipeline without running simulations.

    These JSON files intentionally model the machine-readable boundary emitted by
    Kotlin.  No human-readable evaluator output participates in the pipeline.
    """

    learner1 = load_market_result(FIXTURES / "learner-1.json")
    learner2 = load_market_result(FIXTURES / "learner-2.json")

    # Loader + validation have preserved the exact structured values requested by
    # the fixture before aggregation/reporting begins.
    l1 = learner1.cards_by_identity
    assert (l1["Root_05_01"].learned_exposure, l1["Root_05_01"].learned_purchases) == (3, 2)
    assert (l1["Root_05_02"].learned_exposure, l1["Root_05_02"].learned_purchases) == (2, 0)
    assert (l1["Root_05_03"].learned_exposure, l1["Root_05_03"].learned_purchases) == (1, 2)

    aggregates = aggregate_market_results((learner1, learner2)).for_players(4)
    cards = aggregates.cards_by_identity
    assert (cards["Root_05_01"].total_exposure, cards["Root_05_01"].total_purchases) == (7, 3)
    assert cards["Root_05_01"].purchases_per_exposure == pytest.approx(3 / 7)
    assert (cards["Root_05_02"].total_exposure, cards["Root_05_02"].total_purchases) == (4, 1)
    assert cards["Root_05_02"].purchases_per_exposure == pytest.approx(0.25)
    assert (cards["Root_05_03"].total_exposure, cards["Root_05_03"].total_purchases) == (2, 2)
    assert cards["Root_05_03"].purchases_per_exposure == pytest.approx(1.0)

    paths = write_market_reports((learner1, learner2), tmp_path)

    raw = paths["card-purchases.tsv"].read_text(encoding="utf-8")
    assert _raw_row(raw, "learner-1", "Root_05_01") == [
        "4", "learner-1", "25.000", "40.000", "Root_05_01", "ROOT", "5", "3", "0", "2", "0.666667"
    ]
    assert _raw_row(raw, "learner-1", "Root_05_02")[-4:] == ["2", "0", "0", "0.000000"]
    assert _raw_row(raw, "learner-1", "Root_05_03")[-4:] == ["1", "0", "2", "2.000000"]

    summary = paths["card-summary.tsv"].read_text(encoding="utf-8")
    assert _row_by_card(summary, "Root_05_01") == [
        "4", "Root_05_01", "ROOT", "5", "7", "3", "0.428571", "2", "2", "55.000"
    ]
    assert _row_by_card(summary, "Root_05_02") == [
        "4", "Root_05_02", "ROOT", "5", "4", "1", "0.250000", "2", "1", "55.000"
    ]
    assert _row_by_card(summary, "Root_05_03") == [
        "4", "Root_05_03", "ROOT", "5", "2", "2", "1.000000", "2", "1", "40.000"
    ]

    # Root-5 includes the fourth fixture card (1 exposure per learner, no buys),
    # proving final slot reporting is reconciled from the same structured values.
    slot = paths["slot-summary.tsv"].read_text(encoding="utf-8").splitlines()
    root5 = next(line for line in slot[1:] if line.startswith("4\tROOT\t5\t"))
    assert root5 == "4\tROOT\t5\t15\t6\t0.400000\t8"

    wins = paths["learner-win-shares.tsv"].read_text(encoding="utf-8").splitlines()
    assert wins == [
        "players\tlearner\tcontrol_win_share_pct\theld_out_win_share_pct",
        "4\tlearner-1\t25.000\t40.000",
        "4\tlearner-2\t25.000\t55.000",
    ]

    # Filtered final reports are built from the same aggregates, not reparsed
    # console data. Root_05_04 is never purchased; Root_05_02 and Root_05_03
    # are each purchased by exactly one learner.
    zero_rows = paths["zero-purchase-cards.tsv"].read_text(encoding="utf-8").splitlines()[1:]
    zero_names = {row.split("\t")[1] for row in zero_rows}
    assert "Root_05_04" in zero_names
    assert "Root_05_01" not in zero_names

    one_rows = paths["one-learner-only-cards.tsv"].read_text(encoding="utf-8").splitlines()[1:]
    one_names = {row.split("\t")[1] for row in one_rows}
    assert "Root_05_02" in one_names
    assert "Root_05_03" in one_names
    assert "Root_05_01" not in one_names

    # Best learner identity is retained in the aggregate even though the
    # compact card-summary report currently prints only that learner's win share.
    best = cards["Root_05_01"].best_purchasing_learner
    assert best is not None
    assert best.policy_path == "output/e2e/learner-2.weights"
    assert best.win_share == pytest.approx(0.55)


def test_regression_console_indentation_cannot_zero_structured_purchase_data(tmp_path: Path) -> None:
    """Regression: report purchases come from JSON, never console indentation."""

    # Deliberately read the noise fixture only to prove it exists and is irrelevant.
    # The pipeline receives no console text parameter at all.
    noise = (FIXTURES / "console-indentation-noise.txt").read_text(encoding="utf-8")
    assert "changed indentation" in noise

    result = load_market_result(FIXTURES / "learner-1.json")
    paths = write_market_reports((result,), tmp_path)
    raw = paths["card-purchases.tsv"].read_text(encoding="utf-8")

    assert _raw_row(raw, "learner-1", "Root_05_01")[-2:] == ["2", "0.666667"]
    assert _raw_row(raw, "learner-1", "Root_05_03")[-2:] == ["2", "2.000000"]
    assert sum(card.learned_purchases for card in result.cards) == 4


def test_regression_purchase_with_zero_exposure_fails_at_loader_boundary(tmp_path: Path) -> None:
    """Regression: impossible exposure=0/purchases>0 cannot reach reports."""

    payload = json.loads((FIXTURES / "learner-1.json").read_text(encoding="utf-8"))
    target = next(card for card in payload["cards"] if card["identity"] == "Root_05_01")
    target["learnedExposure"] = 0
    target["learnedPurchases"] = 2

    broken = tmp_path / "broken.json"
    broken.write_text(json.dumps(payload), encoding="utf-8")

    with pytest.raises(MarketResultValidationError, match="learnedPurchases=2 with learnedExposure=0 is impossible"):
        load_market_result(broken)
