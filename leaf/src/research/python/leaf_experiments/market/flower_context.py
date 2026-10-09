from __future__ import annotations

import argparse
import csv
import json
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

FOCUS_COSTS = {14, 17}

@dataclass(frozen=True)
class CardObservation:
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


def load_observations(path: Path, learner: int, context: str) -> list[CardObservation]:
    doc = json.loads(path.read_text())
    players = int(doc["experiment"]["players"])
    out: list[CardObservation] = []
    for card in doc["cards"]:
        if card["type"] != "FLOWER" or int(card["cost"]) not in FOCUS_COSTS:
            continue
        opp = card["learnedOpportunity"]
        out.append(CardObservation(
            players=players,
            learner=learner,
            context=context,
            identity=card["identity"],
            cost=int(card["cost"]),
            legal=int(opp["legalDecisionCount"]),
            selected=int(opp["selectedDecisionCount"]),
        ))
    return out


def collect(root: Path) -> list[CardObservation]:
    rows: list[CardObservation] = []
    for pdir in sorted(root.glob("*p")):
        if not pdir.is_dir():
            continue
        for context in ("human-cultivation", "learned-cultivation"):
            edir = pdir / "eval" / context
            if not edir.is_dir():
                continue
            for path in sorted(edir.glob("learner-*.market.json")):
                learner = int(path.stem.split("-")[1].split(".")[0])
                rows.extend(load_observations(path, learner, context))
    return rows


def write_reports(root: Path, rows: Iterable[CardObservation]) -> None:
    rows = list(rows)
    report_dir = root / "reports"
    report_dir.mkdir(parents=True, exist_ok=True)

    indexed = {(r.players, r.learner, r.identity, r.context): r for r in rows}
    keys = sorted({(r.players, r.learner, r.identity, r.cost) for r in rows})
    card_path = report_dir / "flower-context-by-learner.tsv"
    with card_path.open("w", newline="") as f:
        w = csv.writer(f, delimiter="\t")
        w.writerow(["players","learner","identity","cost","human_legal","human_selected","human_select_when_legal_pct","learned_cult_legal","learned_cult_selected","learned_cult_select_when_legal_pct","delta_pp"])
        for players, learner, identity, cost in keys:
            h = indexed.get((players, learner, identity, "human-cultivation"))
            c = indexed.get((players, learner, identity, "learned-cultivation"))
            if h is None or c is None:
                continue
            w.writerow([players, learner, identity, cost, h.legal, h.selected, f"{h.rate_pct:.4f}", c.legal, c.selected, f"{c.rate_pct:.4f}", f"{c.rate_pct-h.rate_pct:+.4f}"])

    tier_path = report_dir / "flower-context-tier-summary.tsv"
    with tier_path.open("w", newline="") as f:
        w = csv.writer(f, delimiter="\t")
        w.writerow(["players","cost","context","legal","selected","select_when_legal_pct"])
        groups: dict[tuple[int,int,str], list[int]] = {}
        for r in rows:
            key = (r.players, r.cost, r.context)
            acc = groups.setdefault(key, [0,0])
            acc[0] += r.legal
            acc[1] += r.selected
        for (players, cost, context), (legal, selected) in sorted(groups.items()):
            rate = 100.0 * selected / legal if legal else 0.0
            w.writerow([players, cost, context, legal, selected, f"{rate:.4f}"])

    focus_path = report_dir / "README-FIRST.txt"
    lines = [
        "FLOWER 14/17 POLICY-CONTEXT ABLATION", "",
        "Question:",
        "  Is the <1% expert Buy selection rate caused partly by the Human Baseline Cultivation Main policy?", "",
        "Design:",
        "  The same certified Buy learner is evaluated twice on matched seeds/Groves.",
        "  HUMAN-CULTIVATION: learned Buy + Human Baseline Cultivation Main.",
        "  LEARNED-CULTIVATION: same learned Buy + a Cultivation Main learner trained with that Buy policy active.",
        "  Opponents remain Human Baseline. Only the surrounding Cultivation Main policy changes.", "",
        "Interpretation:",
        "  If Flower selection-when-legal stays near zero in both contexts, Human Cultivation Main is not a sufficient explanation.",
        "  If it rises substantially with learned Cultivation Main, the old Human Cultivation policy was suppressing or failing to exploit that Flower tier.",
        "  This is one causal ablation, not a universal card-value verdict. Continue with the next policy-family ablation only if needed.", "",
        "Reports:",
        "  flower-context-by-learner.tsv", "  flower-context-tier-summary.tsv", "",
    ]
    focus_path.write_text("\n".join(lines))


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description="Compare Flower 14/17 Buy selection under Human vs learned Cultivation Main context.")
    ap.add_argument("--root", type=Path, default=Path("output/experiments/flower-tier-why"))
    args = ap.parse_args(argv)
    rows = collect(args.root)
    if not rows:
        raise SystemExit(f"No market JSON found beneath {args.root}")
    write_reports(args.root, rows)
    print(args.root / "reports" / "README-FIRST.txt")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
