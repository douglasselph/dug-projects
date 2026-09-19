# Leaf to Legend: Project Overview

## Purpose
This project is **not** a digital implementation of the Leaf to Legend card game for player use. Instead, it serves as a sophisticated analysis and simulation tool designed to refine and balance the physical card game through data-driven insights.

## Core Analysis Capabilities

### 1. Game Balance & Economy Analysis

#### Card Cost Analysis
- Evaluate cost-to-impact ratios across all card types
- Identify potential power imbalances
- Test cost curve adjustments for natural progression
- Analyze early-game vs. late-game card utility

#### Resource Economy
- Model dice roll probabilities and their impact on purchasing power
- Analyze the effectiveness of matching effects vs. base effects
- Track resource flow throughout game phases
- Measure the impact of dice pool composition on strategy viability

### 2. Game State Simulation

#### Turn-by-Turn Analysis
- Simulate complete game sessions with various strategies
- Track phase transition timing and game length
- Measure milestone achievement rates (e.g., Bloom acquisition)
- Analyze game state progression patterns

#### Rule Variation Testing
- Compare different purchase mechanics:
  - Single vs. multiple purchases per turn
  - Various market cycling rules
  - Different phase transition triggers
- Test combat resolution alternatives
- Evaluate different resilience and damage mechanics

### 3. Multiplayer Dynamics

#### Combat System Analysis
- Test chain attack mechanics for fairness
- Evaluate initiative and turn order systems
- Analyze targeting patterns and their impact
- Measure player elimination rates

#### Balance Mechanisms
- Track runaway leader occurrence rates
- Analyze comeback potential
- Evaluate catch-up mechanics effectiveness
- Test player position impact on win rates

### 4. Market System Analysis

#### Structure Optimization
- Test different market layouts and sizes
- Analyze card availability patterns
- Evaluate market cycling frequency
- Measure impact of face-up vs. face-down cards

#### Distribution Analysis
- Track card acquisition patterns
- Analyze market stagnation scenarios
- Test Bloom card distribution strategies
- Evaluate Seedling card effectiveness

### 5. AI-Driven Strategy Testing

#### Strategy Simulation
- Model different playstyles:
  - Root-focused economy
  - Vine-heavy manipulation
  - Bloom rush strategies
  - Defensive Canopy builds
- Test extreme strategy scenarios
- Identify dominant combinations

#### Edge Case Detection
- Simulate boundary conditions
- Test rule exploits
- Identify degenerate strategies
- Validate balance mechanisms

## Implementation Architecture

### 1. Core Components

#### Game State Management
```kotlin
class GameState {
    val market: Market
    val players: List<Player>
    val phase: GamePhase
    // Additional state tracking
}
```

#### Decision Engine
```kotlin
interface DecisionEngine {
    fun evaluateMove(state: GameState): Decision
    fun calculateProbabilities(): Map<Decision, Double>
}
```

#### Analysis Tools
```kotlin
class GameAnalyzer {
    fun simulateGames(count: Int): SimulationResults
    fun analyzeStrategy(strategy: Strategy): StrategyMetrics
    fun evaluateBalance(): BalanceReport
}
```

### 2. Data Collection

#### Metrics Tracked
- Game duration
- Phase transition timing
- Card acquisition patterns
- Combat resolution statistics
- Player position impact
- Strategy success rates

#### Analysis Output
- Win rate distributions
- Resource efficiency metrics
- Strategy effectiveness scores
- Balance warning indicators
- Timing consistency measures

## Success Criteria

### 1. Balance Metrics
- No strategy wins > 60% of the time
- Player position advantage < 5%
- Comeback rate > 25% when behind
- Resource conversion efficiency within 10% across strategies

### 2. Timing Goals
- 90% of games complete in 60-75 minutes
- Phase transition occurs naturally in 30-45 minutes
- Combat resolution within 15-30 minutes
- Minimal analysis of "dead time" between actions

### 3. Strategy Diversity
- Multiple viable paths to victory
- No dominant card combinations
- Balanced resource allocation options
- Effective counter-strategies exist

## Usage

### 1. Running Simulations
```kotlin
val analyzer = GameAnalyzer()
val results = analyzer.simulateGames(
    count = 1000,
    parameters = SimulationParameters(
        playerCount = 4,
        marketConfig = StandardMarket,
        ruleVariant = RuleSet.STANDARD
    )
)
```

### 2. Analyzing Results
```kotlin
val report = results.generateReport()
report.displayMetrics()
report.highlightImbalances()
report.suggestAdjustments()
```

### 3. Testing Rule Variations
```kotlin
val comparison = analyzer.compareRuleVariants(
    baseRules = RuleSet.STANDARD,
    testRules = RuleSet.EXPERIMENTAL,
    gamesPerVariant = 500
)
```

## Future Enhancements

1. **Advanced AI Strategies**
   - Neural network-based decision making
   - Learning from simulation results
   - Dynamic strategy adaptation

2. **Visualization Tools**
   - Game state progression graphs
   - Strategy effectiveness heat maps
   - Resource flow diagrams

3. **Rule Optimization**
   - Automated parameter tuning
   - Balance suggestion engine
   - Edge case detection system

4. **Performance Improvements**
   - Parallel simulation processing
   - Optimized state management
   - Cached probability calculations 