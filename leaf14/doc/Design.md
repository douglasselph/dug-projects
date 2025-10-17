# Leaf to Legend: Design Document

## Project Purpose
This project serves as an analysis and simulation tool for balancing and refining the physical card game "Leaf to Legend". Rather than implementing a playable digital version, this system enables:

1. Testing rule decisions and their impact on gameplay
2. Balancing card costs, effects, and interactions
3. Simulating various player strategies and outcomes
4. Verifying game length and phase timing
5. Analyzing the effectiveness of catch-up mechanics
6. Testing market dynamics and card availability

## Game Overview
Leaf to Legend is a deck and dice building game designed for a 60-75 minute play session, structured in two distinct phases:

1. **Cultivation Phase** (~30-45 minutes)
   - Players build their deck and dice pool
   - Focus on strategic resource management
   - Culminates when players acquire Bloom cards

2. **Battle Phase** (~15-30 minutes)
   - Players command their developed Plant Creatures
   - Tactical combat using built resources
   - Quick resolution while maintaining strategic depth

## Core Design Principles

### 1. Dynamic Deck-and-Dice Building System
- Dual resource management (cards and dice)
- Cards represent Plant Creature aspects:
  - Seedling: Starting resources
  - Root: Foundation and resource generation
  - Canopy: Defense and stability
  - Vine: Tactical manipulation
  - Bloom: Power spikes and phase transition
- Dice represent energy and capability
- Strategic balance between cards and dice is crucial

### 2. Meaningful Decisions Every Turn
- Players always have productive options
- Poor dice rolls shouldn't negate player agency
- Multiple viable paths to victory
- Luck mitigation through strategic choices
- Similar decision weight to games like Dominion

### 3. Natural Phase Transition
- Cultivation flows naturally into Battle
- Phase transition feels climactic, not arbitrary
- Bloom cards serve as natural progression markers
- Each phase maintains its strategic depth

### 4. Thematic Growth Mechanics
- All mechanics reinforce organic growth theme
- Flourish Types reflect natural plant development
- Card effects match their thematic role
- Growth feels incremental and satisfying

### 5. Strategic Market System
- Structured card acquisition
- Different purchase mechanics per Flourish Type
- Limited availability creates strategic choices
- Market composition affects strategy

### 6. Balanced Combat System
Key Design Goals:
- Prevent runaway leaders
- Maintain engagement for all players
- Chain attack system ensures fair participation
- Catch-up mechanics prevent early elimination

Implementation Focus:
- Simulate various combat scenarios
- Test damage absorption rates
- Analyze chain position impact
- Verify catch-up mechanism effectiveness

### 7. Player Interaction Design
- Plant-themed combat mechanics
- Interactive but not oppressive
- Defensive options always available
- Strategic retaliation mechanics

### 8. Complexity and Replayability
- Deep strategy without overwhelming rules
- Emergent gameplay from card combinations
- Variable market setup
- Balance between luck and strategy

### 9. Combat Incentives
- Rewards for aggressive play
- Viable defensive strategies
- Fair targeting mechanics
- Natural power balance

### 10. Game Pacing
Target Metrics:
- Setup: 5 minutes
- Cultivation Phase: 30-45 minutes
- Battle Phase: 15-30 minutes
- Total Game Time: 60-75 minutes

Success Criteria:
- Players feel progression throughout
- No dead time or waiting
- Satisfying arc from start to finish
- Climactic final battles

## Analysis Priorities

### 1. Time Management
- Track phase lengths across simulations
- Identify factors affecting game duration
- Optimize transition timing
- Ensure consistent play experience

### 2. Balance Testing
- Card cost-to-effect ratios
- Dice upgrade paths
- Market availability impact
- Combat damage calculations

### 3. Strategy Analysis
- Test various deck compositions
- Evaluate dice pool strategies
- Analyze card synergies
- Measure strategy success rates

### 4. Catch-up Mechanics
- Monitor player elimination rates
- Test comeback potential
- Analyze chain position impact
- Verify bonus die effectiveness

### 5. Market Dynamics
- Card availability patterns
- Purchase timing impact
- Bloom card acquisition rates
- Resource economy flow

## Success Metrics
1. Game Length
   - Consistent 60-75 minute playtime
   - Cultivation Phase ends naturally around 30-45 minutes
   - Battle Phase resolves within 15-30 minutes

2. Player Engagement
   - All players remain competitive until late game
   - Multiple viable strategies exist
   - Decisions remain meaningful throughout

3. Balance Indicators
   - No dominant strategy emerges
   - First player advantage < 5%
   - Player position advantage < 3%
   - Comeback possibility > 25%

4. Strategic Depth
   - Multiple winning strategies observed
   - Card synergies create emergent gameplay
   - Market composition affects strategy selection

## Implementation Focus
This project prioritizes:
1. Accurate simulation of game mechanics
2. Detailed data collection and analysis
3. Easy modification of game parameters
4. Rapid testing of rule variations
5. Statistical analysis of outcomes

The goal is to refine and balance the physical card game through data-driven insights and systematic testing. 