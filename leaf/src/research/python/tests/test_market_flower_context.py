import json
from pathlib import Path

from leaf_experiments.market.flower_context import collect, write_reports


def _write(path: Path, players: int, human: bool):
    path.parent.mkdir(parents=True, exist_ok=True)
    selected14 = 1 if human else 25
    selected17 = 2 if human else 30
    doc = {
        "experiment": {"players": players},
        "cards": [
            {"identity":"Flower_14_01","type":"FLOWER","cost":14,"learnedOpportunity":{"legalDecisionCount":1000,"selectedDecisionCount":selected14}},
            {"identity":"Flower_17_04","type":"FLOWER","cost":17,"learnedOpportunity":{"legalDecisionCount":500,"selectedDecisionCount":selected17}},
            {"identity":"Vine_11_01","type":"VINE","cost":11,"learnedOpportunity":{"legalDecisionCount":1,"selectedDecisionCount":1}},
        ]
    }
    path.write_text(json.dumps(doc))


def test_context_report_compares_selection_when_legal(tmp_path: Path):
    _write(tmp_path / "2p/eval/human-cultivation/learner-1.market.json", 2, True)
    _write(tmp_path / "2p/eval/learned-cultivation/learner-1.market.json", 2, False)
    rows = collect(tmp_path)
    assert len(rows) == 4
    write_reports(tmp_path, rows)
    text = (tmp_path / "reports/flower-context-by-learner.tsv").read_text()
    assert "Flower_14_01" in text
    assert "0.1000" in text
    assert "2.5000" in text
    tier = (tmp_path / "reports/flower-context-tier-summary.tsv").read_text()
    assert "learned-cultivation" in tier
    assert "human-cultivation" in tier
