"""Typed market-evaluation result models and strict JSON loading."""

from .models import (
    CardResult,
    ExperimentMetadata,
    LearnerResult,
    MarketRawResult,
    MarketSlotIdentity,
)
from .reader import (
    MARKET_SCHEMA,
    MARKET_SCHEMA_VERSION,
    MarketResultValidationError,
    load_market_result,
    parse_market_result,
)

__all__ = [
    "CardResult",
    "ExperimentMetadata",
    "LearnerResult",
    "MarketRawResult",
    "MarketSlotIdentity",
    "MARKET_SCHEMA",
    "MARKET_SCHEMA_VERSION",
    "MarketResultValidationError",
    "load_market_result",
    "parse_market_result",
]

from .aggregation import (
    CardAggregate,
    LearnerPerformance,
    MarketAggregates,
    PlayerCountMarketAggregate,
    SlotAggregate,
    aggregate_market_results,
    aggregate_player_count,
    one_learner_only_cards,
    safe_rate,
    zero_purchase_cards,
)

__all__ += [
    "CardAggregate",
    "LearnerPerformance",
    "MarketAggregates",
    "PlayerCountMarketAggregate",
    "SlotAggregate",
    "aggregate_market_results",
    "aggregate_player_count",
    "one_learner_only_cards",
    "safe_rate",
    "zero_purchase_cards",
]
