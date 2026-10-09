from __future__ import annotations

import csv
import json
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

FOCUS_COSTS = {14, 17}


@dataclass(frozen=True)
class FlowerAblationObservation:
    players: int
    learner: int
    context: str
    identity: str
    cost: int
    legal: int
    selected: int

    @property
    def rate_pct(self) -> float:
        return 100.0 * self.selected / self.legal if self.legal else 0.0


def load_observations(path: Path, learner: int, context: str) -> list[FlowerAblationObservation]:
    doc = json.loads(path.read_text(encoding="utf-8"))
    players = int(doc["experiment"]["players"])
    rows: list[FlowerAblationObservation] = []
    for card in doc["cards"]:
        if card["type"] != "FLOWER" or int(card["cost"]) not in FOCUS_COSTS:
            continue
        opportunity = card["learnedOpportunity"]
        rows.append(
            FlowerAblationObservation(
                players=players,
                learner=learner,
                context=context,
                identity=card["identity"],
                cost=int(card["cost"]),
                legal=int(opportunity["legalDecisionCount"]),
                selected=int(opportunity["selectedDecisionCount"]),
            )
        )
    return rows


def collect(root: Path, contexts: tuple[str, str]) -> list[FlowerAblationObservation]:
    rows: list[FlowerAblationObservation] = []
    for pdir in sorted(root.glob("*p")):
        if not pdir.is_dir():
            continue
        for context in contexts:
            edir = pdir / "eval" / context
            if not edir.is_dir():
                continue
            for path in sorted(edir.glob("learner-*.market.json")):
                learner = int(path.name.split("learner-")[1].split(".")[0])
                rows.extend(load_observations(path, learner, context))
    return rows


def write_reports(
    root: Path,
    rows: Iterable[FlowerAblationObservation],
    *,
    baseline_context: str,
    learned_context: str,
    policy_label: str,
) -> None:
    rows = list(rows)
    report_dir = root / "reports"
    report_dir.mkdir(parents=True, exist_ok=True)
    indexed = {(r.players, r.learner, r.identity, r.context): r for r in rows}
    keys = sorted({(r.players, r.learner, r.identity, r.cost) for r in rows})

    card_path = report_dir / "flower-context-by-learner.tsv"
    with card_path.open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, delimiter="\t")
        w.writerow([
            "players", "learner", "identity", "cost",
            "human_context_legal", "human_context_selected", "human_context_select_when_legal_pct",
            "learned_context_legal", "learned_context_selected", "learned_context_select_when_legal_pct",
            "delta_pp",
        ])
        for players, learner, identity, cost in keys:
            human = indexed.get((players, learner, identity, baseline_context))
            learned = indexed.get((players, learner, identity, learned_context))
            if human is None or learned is None:
                continue
            w.writerow([
                players, learner, identity, cost,
                human.legal, human.selected, f"{human.rate_pct:.4f}",
                learned.legal, learned.selected, f"{learned.rate_pct:.4f}",
                f"{learned.rate_pct-human.rate_pct:+.4f}",
            ])

    tier_path = report_dir / "flower-context-tier-summary.tsv"
    with tier_path.open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, delimiter="\t")
        w.writerow(["players", "cost", "context", "legal", "selected", "select_when_legal_pct"])
        groups: dict[tuple[int, int, str], list[int]] = {}
        for row in rows:
            acc = groups.setdefault((row.players, row.cost, row.context), [0, 0])
            acc[0] += row.legal
            acc[1] += row.selected
        for (players, cost, context), (legal, selected) in sorted(groups.items()):
            rate = 100.0 * selected / legal if legal else 0.0
            w.writerow([players, cost, context, legal, selected, f"{rate:.4f}"])

    (report_dir / "README-FIRST.txt").write_text(
        "\n".join([
            f"FLOWER 14/17 {policy_label.upper()} CONTEXT ABLATION",
            "",
            "Question:",
            f"  Is the expert Buy rejection of Flower 14/17 caused by the Human Baseline {policy_label} policy?",
            "",
            "Design:",
            "  The same certified Buy learner is evaluated on matched seeds/Groves.",
            f"  {baseline_context}: learned Buy + Human Baseline {policy_label}.",
            f"  {learned_context}: same learned Buy + a fresh {policy_label} learner trained with that Buy policy active.",
            "  Opponents remain Human Baseline. Only this surrounding policy family changes.",
            "",
            "Primary metric:",
            "  Flower selection when legal.",
            "",
            "Interpretation:",
            "  Near-zero in both contexts: this Human Baseline policy family is not a sufficient explanation.",
            "  Large revival in learned context: this policy family was suppressing/failing to exploit the tier.",
            "",
        ]),
        encoding="utf-8",
    )
