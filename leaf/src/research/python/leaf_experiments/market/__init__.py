"""Typed market-evaluation result models and strict JSON loading."""

from .models import (
    BuyOpportunityStats,
    CardResult,
    ExperimentMetadata,
    LearnerResult,
    MarketRawResult,
    MarketSlotIdentity,
    RejectedLegalAlternative,
)
from .reader import (
    MARKET_SCHEMA,
    MARKET_SCHEMA_VERSION,
    MarketResultValidationError,
    load_market_result,
    parse_market_result,
)

__all__ = [
    "BuyOpportunityStats",
    "CardResult",
    "ExperimentMetadata",
    "LearnerResult",
    "MarketRawResult",
    "MarketSlotIdentity",
    "RejectedLegalAlternative",
    "MARKET_SCHEMA",
    "MARKET_SCHEMA_VERSION",
    "MarketResultValidationError",
    "load_market_result",
    "parse_market_result",
]

from .aggregation import (
    CardAggregate,
    CardOpportunityAggregate,
    LearnerPerformance,
    MarketAggregates,
    PlayerCountMarketAggregate,
    SlotAggregate,
    aggregate_market_results,
    aggregate_market_opportunities,
    aggregate_player_count,
    one_learner_only_cards,
    safe_rate,
    zero_purchase_cards,
)

__all__ += [
    "CardAggregate",
    "CardOpportunityAggregate",
    "LearnerPerformance",
    "MarketAggregates",
    "PlayerCountMarketAggregate",
    "SlotAggregate",
    "aggregate_market_results",
    "aggregate_market_opportunities",
    "aggregate_player_count",
    "one_learner_only_cards",
    "safe_rate",
    "zero_purchase_cards",
]

from .reports import (
    MarketReportValidationError,
    render_card_purchases,
    render_card_summary,
    render_slot_summary,
    render_zero_purchase_cards,
    render_one_learner_only_cards,
    render_vine_9_summary,
    render_learner_win_shares,
    render_card_opportunity_summary,
    render_slot_opportunity_summary,
    render_card_opportunity_by_round,
    render_card_substitution_summary,
    render_card_substitutions_by_learner,
    render_readme_first,

    write_market_reports,
)

__all__ += [
    "MarketReportValidationError",
    "render_card_purchases",
    "render_card_summary",
    "render_slot_summary",
    "render_zero_purchase_cards",
    "render_one_learner_only_cards",
    "render_vine_9_summary",
    "render_learner_win_shares",
    "render_card_opportunity_summary",
    "render_slot_opportunity_summary",
    "render_card_opportunity_by_round",
    "render_card_substitution_summary",
    "render_card_substitutions_by_learner",
    "render_readme_first",
    "write_market_reports",
]
