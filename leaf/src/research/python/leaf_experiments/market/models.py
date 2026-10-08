"""Domain types for one raw Kotlin Buy-policy market evaluation."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True, order=True)
class MarketSlotIdentity:
    """One legal Plant-market slot, identified by type and cost."""

    plant_type: str
    cost: int


@dataclass(frozen=True)
class ExperimentMetadata:
    """Experiment provenance emitted by the Kotlin evaluator."""

    players: int
    samples: int
    mechanical_seed_start: int
    strategy_seed_start: int
    grove_seed_start: int | None
    grove_pattern: str | None
    round_pattern: str
    policy_path: str
    plant_overrides_path: str | None
    plant_overrides_sha256: str | None
    round_overrides_path: str | None
    round_overrides_sha256: str | None


@dataclass(frozen=True)
class LearnerResult:
    """Outcome summary for one side of the matched CONTROL/LEARNED evaluation.

    The name is retained because the market research pipeline primarily consumes
    these records as policy/learner results. ``role`` makes CONTROL explicit too.
    """

    role: str
    sample_count: int
    win_share: float
    plant_purchases: int


@dataclass(frozen=True)
class CardResult:
    """Raw per-card market facts. No purchase/exposure rate is derived here."""

    identity: str
    plant_type: str
    cost: int
    control_exposure: int
    learned_exposure: int
    control_purchases: int
    learned_purchases: int
    control_win_share_on_exposure_sum: float
    learned_win_share_on_exposure_sum: float

    @property
    def slot(self) -> MarketSlotIdentity:
        return MarketSlotIdentity(self.plant_type, self.cost)


@dataclass(frozen=True)
class MarketRawResult:
    """Validated raw result loaded from the Kotlin market-evaluation JSON."""

    schema: str
    schema_version: int
    metadata: ExperimentMetadata
    control: LearnerResult
    learned: LearnerResult
    cards: tuple[CardResult, ...]
    source_path: Path | None = None

    @property
    def cards_by_identity(self) -> dict[str, CardResult]:
        return {card.identity: card for card in self.cards}
