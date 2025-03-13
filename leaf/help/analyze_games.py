#!/usr/bin/env python3
import os
import re
import glob

# Statistics counters
total_games = 0
player_under_test_won = 0
first_player_won = 0
same_player = 0
different_player = 0
player_under_test_ids = {}
first_player_ids = {}
different_player_games = []

# Cases where player under test wins but first player loses
put_win_first_loss = 0

# Cases where player under test loses but first player wins
put_loss_first_win = 0

# Process each game file
game_files = glob.glob("test-output/testBase_2D4_2D6_*.txt")
game_files = [f for f in game_files if "report" not in f]

for file_path in sorted(game_files):
    game_number = os.path.basename(file_path).replace("testBase_2D4_2D6_", "").replace(".txt", "")
    
    with open(file_path, 'r') as file:
        content = file.readlines()
        
        # Extract the summary section
        summary_start = 0
        for i, line in enumerate(content):
            if "=== GAME SUMMARY ===" in line:
                summary_start = i
                break
        
        if summary_start == 0:
            print(f"Game summary not found in {file_path}")
            continue
        
        # Process the summary section
        player_under_test_id = None
        first_player_id = None
        player_under_test_place = None
        first_player_place = None
        
        for i in range(summary_start, min(summary_start + 20, len(content))):
            line = content[i].strip()
            
            if line.startswith("Player ID under test:"):
                player_under_test_id = line.split(":")[1].strip()
            
            elif line.startswith("Player ID who went first:"):
                first_player_id = line.split(":")[1].strip()
            
            elif line.startswith("Player Under Test Place:"):
                player_under_test_place = line.split(":")[1].strip()
            
            elif line.startswith("Player Who Was First Place:"):
                first_player_place = line.split(":")[1].strip()
        
        if player_under_test_id and first_player_id and player_under_test_place and first_player_place:
            total_games += 1
            
            # Track player IDs
            player_under_test_ids[player_under_test_id] = player_under_test_ids.get(player_under_test_id, 0) + 1
            first_player_ids[first_player_id] = first_player_ids.get(first_player_id, 0) + 1
            
            # Check if player under test won
            if player_under_test_place == "First Place":
                player_under_test_won += 1
                
            # Check if first player won
            if first_player_place == "First Place":
                first_player_won += 1
            
            # Count special cases
            if player_under_test_place == "First Place" and first_player_place != "First Place":
                put_win_first_loss += 1
            
            if player_under_test_place != "First Place" and first_player_place == "First Place":
                put_loss_first_win += 1
            
            # Check if player under test is the same as first player
            if player_under_test_id == first_player_id:
                same_player += 1
            else:
                different_player += 1
                # Record games where they're different
                different_player_games.append({
                    "game": game_number,
                    "player_under_test": player_under_test_id,
                    "first_player": first_player_id,
                    "player_under_test_place": player_under_test_place,
                    "first_player_place": first_player_place
                })

# Print results
print(f"Total games analyzed: {total_games}")
print(f"Player under test won: {player_under_test_won} ({player_under_test_won/total_games*100:.1f}%)")
print(f"First player won: {first_player_won} ({first_player_won/total_games*100:.1f}%)")
print(f"Same player (player under test = first player): {same_player} ({same_player/total_games*100:.1f}%)")
print(f"Different player: {different_player} ({different_player/total_games*100:.1f}%)")

print(f"\nPlayer under test won but first player lost: {put_win_first_loss}")
print(f"Player under test lost but first player won: {put_loss_first_win}")

print(f"\nUnique player under test IDs: {len(player_under_test_ids)}")
print(f"Unique first player IDs: {len(first_player_ids)}")

# Print the most common player IDs
print("\nMost common player under test IDs:")
for player_id, count in sorted(player_under_test_ids.items(), key=lambda x: x[1], reverse=True)[:5]:
    print(f"  Player ID {player_id}: {count} games")

print("\nMost common first player IDs:")
for player_id, count in sorted(first_player_ids.items(), key=lambda x: x[1], reverse=True)[:5]:
    print(f"  Player ID {player_id}: {count} games")

if different_player > 0:
    print("\nGames where player under test differs from first player:")
    for game in different_player_games[:10]:  # Show only the first 10 if there are many
        print(f"Game {game['game']}: Player under test={game['player_under_test']} ({game['player_under_test_place']}), " +
              f"First player={game['first_player']} ({game['first_player_place']})")
    
    if len(different_player_games) > 10:
        print(f"...and {len(different_player_games) - 10} more") 