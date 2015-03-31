# MetalMew bot by WormDog

import rg
import random
import string

#
# Some terms:
#   DHP : MaxHP-HP
#   MHP : MAX HP
#   DMM : Directional Move Map
#   DAM : Directional Attack Map
#   STC : Spawn turn coming flag
#
class Robot:

    class MoveMap:

        def __init__(self, parent):
            self._from = {}
            self._to = {}
            self._chk_fail = []
            self._failed = []
            self._dangerous_ok = False
            self._toward_center = True
            self._filter_old = True
            self._towards_enemy = False
            self._blocking_allowed=True
            self._parent = parent
            # List of friendlies that have not yet moved, that are blocking a move that would have otherwise occurred
            self._blocking = []

        def clear(self):
            self._from = {}
            self._to = {}
            self._chk_fail = []

        # Move the indicated robot to any normal space.
        def move(self, loc):
            bot = self._parent._game.robots[loc]
            for tdir in self._parent._DIRS:
                dloc = self._parent.get_loc(bot.location, tdir)
                self.chk_add(loc, dloc)

        def chk(self, floc, tloc):
            if self.is_ok(floc, tloc):
                return True
            if not floc in self._chk_fail:
                self._chk_fail.append(floc)
            return False
                
        def chk_add(self, floc, tloc):
            if self.chk(floc, tloc):
                self.add(floc, tloc)
                return True
            return False
                
        def chk_add_dir(self, floc, tdir):
            tloc = self._parent.get_loc(floc, tdir)
            self.chk_add(floc, tloc)

        def is_ok(self, floc, tloc):
            if not self._parent.is_normal(tloc): 
                return False
            if tloc in self._parent._OCCUPIED or tloc in self._parent._MOVING_INTO:
                return False
            if self._parent.is_enemy(tloc):
                return False
            if self._blocking_allowed and self._parent.is_friendly(tloc) and not self._parent.is_moving(tloc):
                if tloc not in self._blocking:
                    self._blocking.append(tloc)
                return False
            if self._parent._STC and self._parent.is_spawn(tloc) and not self._parent.is_spawn(floc):
                return False
            return True

        def set_dangerous_ok(self, flag):
            self._dangerous_ok = flag

        def set_blocking_allowed(self, flag):
            self._blocking_allowed=flag
            
        def clear_blocking(self):
            self._blocking=[]
            
        def has(self, floc):
            return floc in self._from.keys()

        def add_to_failed(self, floc):
            self._failed.append(floc)

        def get_failed(self):
            return self._failed
        
        def get_blocking(self):
            return self._blocking
        # Add the specified possible move for a robot. The robot is identified
        # by the fromloc and it's target move spot by toloc.
        def add(self, floc, tloc):
            if floc in self._from.keys():
                self._from[floc].append(tloc)
            else:
                self._from[floc] = [tloc]

            if tloc in self._to.keys():
                self._to[tloc].append(floc)
            else:
                self._to[tloc] = [floc]
                
        def insert(self, floc, tloc):
            if floc in self._from.keys():
                self._from[floc].insert(0, tloc)
            else:
                self._from[floc] = [tloc]

            if tloc in self._to.keys():
                self._to[tloc].insert(0, floc)
            else:
                self._to[tloc] = [floc]

        # Only the attack from floc is attacking tloc
        # Remove all other attacks scheduled
        def only(self, floc, tloc):
            for f in self._to[tloc]:
                if f != floc:
                    self._from[f].remove(tloc)
            self._to[tloc] = [floc]

        # Cancel the attack from floc to tloc
        def remove(self, floc, tloc):
            self._from[floc].remove(tloc)
            self._to[tloc].remove(floc)

        def show(self, prefix):
            line = prefix + " FROM:"
            for floc in self._from.keys():
                line += "[" + str(floc) + ":" 
                line += str(self._from[floc])
                line += "];"
            line += " TO:"
            for tloc in self._to.keys():
                line += "[" + str(tloc) + ":" 
                line += str(self._to[tloc])
                line += "];"
            print line

        def detect_problems(self):
            self.detect_collisions()
            self.detect_occupied()
            self.detect_bumps()
            
        # If more than one robot wants to move to the same square, resolve the conflict
        # by letting the robot with fewer choices have that square. The other robot
        # will select another square. In the case of ties, it is possible a robot
        # will be left with no place to go. Oh well. That's better than 2 robots bumping.
        def detect_collisions(self):
            for tloc in self._to.keys():
                if len(self._to[tloc]) > 1:
                    least_floc = None
                    least_count = 1000
                    tolist = self._to[tloc]
                    for floc in tolist:
                        count = len(self._from[floc])
                        if count < least_count:
                            least_floc = floc
                            least_count = count
                    self.only(least_floc, tloc)

        # See if two robots are moving into each other. If so, 
        # then give priority to the lower HP one.
        def detect_bumps(self):
            collides = []
            for floc in self._from.keys():
                for tloc in self._from[floc]:
                    if tloc in self._from.keys():
                        for tloc2 in self._from[tloc]:
                            if tloc2 == floc:
                                inList = False
                                for collide in collides:
                                    if collide[0] == floc and collide[1] == tloc:
                                        inList = True
                                        break
                                    if collide[1] == floc and collide[0] == tloc:
                                        inList = True
                                        break
                                if not inList:
                                    nlist = []
                                    nlist.append(floc)
                                    nlist.append(tloc)
                                    collides.append(nlist)
            if len(collides) > 0:
                for collide in collides:
                    floc = collide[0]
                    floc2 = collide[1]
                    if self._parent._game.robots[floc].hp < self._parent._game.robots[floc2].hp:
                        self.remove(floc2, floc)
                    else:
                        self.remove(floc, floc2)

        # If a robot has a choice and one the squares he could move to is occupied
        # with another robot (friendly or enemy), then choose the vacant square.
        def detect_occupied(self):
            for floc in self._from.keys():
                if len(self._from[floc]) > 1:
                    occupied = []
                    for tloc in self._from[floc]:
                        if self._parent.has_robot(tloc):
                            occupied.append(tloc)
                    if occupied > 0 and len(occupied) < len(self._from[floc]):
                        for tloc in occupied:
                            self._from[floc].remove(tloc)
                            if floc in self._to[tloc]:
                                self._to[tloc].remove(floc)

        # Return True if a location is too dangerous for a robot to move into
        def too_dangerous(self, floc, tloc):
            bot = self._parent._game.robots[floc]
            adj = self._parent.AdjEnemyMap(self._parent, tloc) 
            if adj.get_count() > 0:
                strength = 0
                for ebot in adj.get_bots():
                    if ebot.hp >= bot.hp:
                        return True
                    strength += ebot.hp
                if strength >= bot.hp:
                    return True
            return False
        
        # Apply the moves to the robots.
        def apply(self):
            moves = self.get_moves();
            for floc in moves.keys():
                self._parent.apply_move(floc, moves[floc])           
        # Return dictionary of moves we have computed.
        # The key is from, the value is the target
        def get_moves(self):
            self.detect_problems()
            moves = {}
            for floc in self._from.keys():
                count = len(self._from[floc])
                if count > 1:
                    tloc = self.select_move(floc)
                elif count == 1:
                    tloc = self._from[floc][0]
                else:
                    if not floc in self._failed:
                        self._failed.append(floc)
                    tloc = None
                if tloc != None:
                    moves[floc] = tloc

            for floc in self._chk_fail:
                if not floc in self._failed:
                    if not floc in moves.keys():
                        self._failed.append(floc)
                
            return moves

        #
        # First, filter out:
        #   - All spawned
        #   - All occupied by enemy or friendly
        #   - All too dangerous (because they are moving into line of fire)
        # If that leaves us with no choices, then:
        #   - the same but allow occupied
        #   - and unless STC, also allow spawned
        # If that leaves no choices:
        #   - If STC and on a spawn then everything but spawned
        #
        def select_move(self, floc):
            choices = self.select_move_filter(floc, True, True, True)
            if len(choices) == 0:
                choices = self.select_move_filter(floc, self._parent._STC, False, True)
                if len(choices) == 0 and self._parent._STC:
                    choices = self.select_move_filter(floc, True, False, False)
            if len(choices) == 0:
                choices = list(self._from[floc])
            if len(choices) > 1 and self._filter_old:
                choices = self.filter_out_old(floc, choices)
            elif len(choices) == 1:
                return choices[0]
            safest = self.select_safest(choices)
            if len(safest) == 1:
                return safest[0]
            if self._toward_center:
                choices = self.filter_nearer_to_center(choices)
            if len(choices) > 1 and self._towards_enemy and self._parent._game.robots[floc].hp >= rg.settings.robot_hp:
                choices = self.filter_closer_to_enemy(floc, choices)
            return choices[0]
            
        def select_safest(self, moves):
            best_tloc = []
            best_count = 1000
            for tloc in moves:
                adj = self._parent.AdjEnemyMap(self._parent, tloc)
                if adj.get_count() < best_count:
                    best_tloc = [tloc]
                    best_count = adj.get_count()
                elif adj.get_count() == best_count:
                    best_tloc.append(tloc)
            return best_tloc
        
        def filter_closer_to_enemy(self, floc, moves):
            filtered = []
            best_dist = 1000
            for tloc in moves:
                dist = self._parent.get_dist_to_closest_enemy(tloc)
                if dist < best_dist:
                    filtered = [tloc]
                    best_dist = dist
                elif dist == best_dist:
                    filtered.append(tloc)
            return filtered
                            
        def filter_nearer_to_center(self, moves):
            filtered = []
            best_w = 1000
            for loc in moves:
                w = self._parent.get_center_weight(loc)
                if w < best_w:
                    best_w = w;
                    filtered = [loc]
                elif w == best_w:
                    filtered.append(loc)
            return filtered
                
        
        def select_move_filter(self, floc, filter_spawned, filter_occupied, filter_dangerous):
            # Choose non-spawn square if possible
            choices = list(self._from[floc])
            if filter_spawned:
                choices = self.filter_out_spawn(choices)
            if filter_occupied:
                choices = self.filter_out_occupied(choices)
            if filter_dangerous:
                choices = self.filter_out_dangerous(floc, choices)
           
            return choices

        # Return a list of all non-spawn squares
        def filter_out_spawn(self, moves):
            filtered = []
            for loc in moves:
                if not self._parent.is_spawn(loc):
                    filtered.append(loc)
            return filtered

        # Return a list of all robot free squares
        def filter_out_occupied(self, moves):
            filtered = []
            for loc in moves:
                if not self._parent.has_robot(loc):
                    filtered.append(loc)
            return filtered

        # Return a list of all robot free squares
        def filter_out_dangerous(self, floc, moves):
            filtered = []
            for loc in moves:
                if not self.too_dangerous(floc, loc):
                    filtered.append(loc)
            return filtered
      
        def filter_out_old(self, floc, moves):
            robot_id = self._parent._game.robots[floc].robot_id
            if not robot_id in self._parent._LAST_TURN.keys():
                return moves
            filtered = []
            last_turn_loc = self._parent._LAST_TURN[robot_id]
            for loc in moves:
                if loc != last_turn_loc:
                    filtered.append(loc)
            return filtered

    class AdjFriendMap:

        def __init__(self, parent, loc):
            self._parent = parent
            self._count=0
            for tdir in parent._DIRS:
                dloc = parent.get_loc(loc, tdir)
                if self._parent.is_friendly(dloc):
                    self._count += 1
                  
        def get_count(self):
            return self._count               

    class AdjEnemyMap:

        def __init__(self, parent, loc):
            self._adj = []
            self._weakest_loc = None
            self._weakest_hp = 1000
            self._strongest_loc = None
            self._strongest_hp = 0
            self._parent = parent
            for tdir in parent._DIRS:
                dloc = parent.get_loc(loc, tdir)
                if parent.is_enemy(dloc):
                    self._adj.append(dloc)
                    ebot = parent._game.robots[dloc]
                    if ebot.hp < self._weakest_hp:
                        self._weakest_loc = dloc
                        self._weakest_hp = ebot.hp
                    if ebot.hp > self._strongest_hp:
                        self._strongest_loc = dloc
                        self._strongest_hp = ebot.hp

        def get_count(self):
            return len(self._adj)

        def get_strongest_hp(self):
            return self._strongest_hp

        def get_strongest_loc(self):
            return self._strongest_loc

        def get_weakest_hp(self):
            return self._weakest_hp

        def get_weakest_loc(self):
            return self._weakest_loc

        def get_locs(self):
            return self.adj_

        def get_bots(self):
            bots = []
            for loc in self._adj:
                bots.append(self._parent._game.robots[loc])
            return bots
  
    # Base class
    class AttackMap:

        def __init__(self, parent, loc):
            self._parent = parent
            self._loc = loc
            self._dirs = []

        def get_dirs(self):
            return self._dirs

        def get_bot(self, tdir):
            loc = self._parent.get_loc(self._loc, tdir)
            return self._parent._game.robots[loc]

        def get_loc(self, tdir):
            return self._parent.get_loc(self._loc, tdir)

        def get_count(self):
            return len(self._dirs.keys())

    # Compute diagonal enemies
    class DiagonalMap(AttackMap):

        def __init__(self, parent, loc):
            parent.AttackMap.__init__(self, parent, loc)
            # Check diagonal possibilities
            for diag_dir in self._parent._DIAG:
                diag_loc = self._parent.get_loc(loc, diag_dir)
                if self._parent.is_enemy(diag_loc):
                    self._dirs.append(diag_dir)

    # Compute 2 sq orthogonal enemies
    class Ortho2sqMap(AttackMap):

        def __init__(self, parent, loc):
            parent.AttackMap.__init__(self, parent, loc)
            # Check 2sq orthogonal possibilities:
            for dir2 in self._parent._DIR2:
                loc2 = self._parent.get_loc(loc, dir2)
                if self._parent.is_enemy(loc2):
                    self._dirs.append(dir2)

    class GuessAttackMap:

        def __init__(self, parent):
            self._parent = parent
            # Key: floc, Value: dictionary of the 4 DIRS
            self._weights = {}
            self._attacking = []

        def guess_attack(self, floc):
            self._weights[floc] = {}
            for adir in self._parent._DIRS:
                self._weights[floc][adir] = 0
            # Check diagonal possibilities
            diagMap = self._parent.DiagonalMap(self._parent, floc)
            for diag_dir in diagMap.get_dirs():
                for adir in self._parent._DADJ[diag_dir]:
                    tloc = diagMap.get_loc(adir)
                    if self.is_attackable(tloc):
                        ebot = diagMap.get_bot(diag_dir)
                        self.add_attack(floc, adir, tloc, ebot)
            # Check 2sq orthogonal possibilities:
            orthoMap = self._parent.Ortho2sqMap(self._parent, floc)
            for dir2 in orthoMap.get_dirs():
                adir = self._parent._DADJ2[dir2]
                tloc = orthoMap.get_loc(adir)
                if self.is_attackable(tloc):
                    ebot = orthoMap.get_bot(dir2)
                    self.add_attack(floc, adir, tloc, ebot)

        # Add an attack in the given direction, at the target location.
        # The enemy robot that it is near it is ebot.
        def add_attack(self, floc, adir, tloc, ebot):
            x = rg.settings.robot_hp / 2
            weight = x + self._parent.get_hp_weight(ebot.hp)
            weight -= self._parent.miss_get(floc, tloc)
            if self.is_attacking(tloc):
                weight = self.bounded_reduce(weight, 10)
            if self._parent.is_moving(tloc):
                weight = self.bounded_reduce(weight, 5)
            if self.has_friendlies(tloc):
                weight = self.bounded_reduce(weight, 3)
            self._weights[floc][adir] += weight
            if not tloc in self._attacking:
                self._attacking.append(tloc)

        def is_attacking(self, tloc):
            if self._parent.is_attacking(tloc):
                return True
            if tloc in self._attacking:
                return True
            return False
                
        def is_attackable(self, loc):
            return self._parent.is_normal(loc)
        
        def bounded_reduce(self, weight, amt):
            if weight > amt:
                return weight - amt
            return 1
    
        def has_friendlies(self, tloc):
            adjmap=self._parent.AdjFriendMap(self._parent, tloc)
            return adjmap.get_count() >= 2
            
        def get_best_attack(self, floc):
            largest_dir = []
            largest_size = -1
            for tdir in self._weights[floc].keys():
                if self._weights[floc][tdir] > largest_size:
                    largest_size = self._weights[floc][tdir]
                    largest_dir = [tdir]
                elif self._weights[floc][tdir] == largest_size:
                    largest_dir.append(tdir)
            if largest_size > 0 and len(largest_dir) > 0:
                if len(largest_dir) == 1:
                    use_dir = largest_dir[0]
                else:
                    select = random.randint(0, len(largest_dir) - 1)
                    use_dir = largest_dir[select]
                tloc = self._parent.get_loc(floc, use_dir)
                return tloc
            return None

        def apply(self):
            movemap = self._parent.MoveMap(self._parent)
            for floc in self._weights.keys():
                tloc = self.get_best_attack(floc)
                if tloc != None:
                    if self.is_too_many_misses(floc, tloc):
                        self.apply_feint(floc, tloc, movemap)
                    else:
                        self._parent.apply_guess_attack(floc, tloc)
            movemap.apply()

        # Return true if we simply have missed too many times to try again.
        def is_too_many_misses(self, floc, tloc):
            return self._parent.miss_get(floc, tloc) >= 3
        
        # Instead of guess attacking the square, do a feint.
        def apply_feint(self, floc, tloc, movemap):
            dirs=self._parent.get_dirs(floc, tloc)
            for mdir in dirs:
                dir1 = self._parent._FEINT[mdir][0]
                dir2 = self._parent._FEINT[mdir][1]
                tloc1 = self._parent.get_loc(floc, dir1)
                tloc2 = self._parent.get_loc(floc, dir2)
                if self._parent.get_closer_to_center(tloc1, tloc2) == tloc1:
                    movemap.chk_add(floc, tloc1)
                    movemap.chk_add(floc, tloc2)
                else:
                    movemap.chk_add(floc, tloc2)
                    movemap.chk_add(floc, tloc1)

    #
    # Analysis of enemy
    # Has a "count" and "weight".
    # The "count" is the number of friendlies within a 3 sq radius.
    # The "weight" is a weight based on how close the friendlies are to the target.
    #  The closer the higher overall weight.
    #
    class TargetMap:
        
        def __init__(self, parent, eloc):
            self._parent = parent
            self._eloc = eloc
            # Weight count of how near friendlies are.
            self._weight = 0
            # List of friendlies near this target
            self._friendlies = []
            # List of friendlies adjacent to target
            self._adjacents = []
            # The list of friendlies with a walk distance of 2 of this target
            self._near = []
            # the List of friendlies assigned to this target
            self._assigned = []
            # If we are all out attacking this target or not
            self._attacking = False
            # The list of enemies within 2 of this one.
            self._near_enemies=[]
            # The list of enemies adjacent to this one.
            self._adj_enemies=[]
                
            for loc in self._parent._game.robots.keys():
                dist = rg.dist(loc, self._eloc)
                if dist < 3:
                    wdist = rg.wdist(loc, self._eloc)
                    if self._parent.is_friendly(loc):
                        self._weight += 3 - wdist
                        self._friendlies.append(loc)
                        if wdist == 1:
                            self._adjacents.append(loc)
                        elif wdist == 2:
                            self._near.append(loc)
                    else:
                        if wdist == 2:
                            self._near_enemies.append(loc)
                        elif wdist == 1:
                            self._adj_enemies.append(loc)
                        
                        
        def has_friendly(self, floc):
            return floc in self._friendlies
        
        def is_adjacent(self, floc):
            return floc in self._adjacents

        def is_assigned(self, floc):
            return floc in self._assigned
    
        def get_enemies(self):
            return self._near_enemies + self._adj_enemies
        
        def is_attacking(self):
            return self._attacking
        
        def get_weight(self):
            return self._weight
        
        def get_count(self):
            return len(self._friendlies)
        
        def get_target_loc(self):
            return self._eloc
        
        def add_assigned(self, floc):
            self._assigned.append(floc)
                
        # Compute if are all out attacking this target.
        def compute_attacking(self):
            
            flag=False
            # If one unit already attacking then yes.
            if self._parent.is_attacking(self._eloc):
                flag = True
            else:
                count = len(self._near) + len(self._adjacents)
                numenemies=len(self._parent._ENEMIES)
                numfriendlies=len(self._parent._FRIENDLIES)
                if numenemies > numfriendlies:
                    trigger = 4
                else:
                    trigger = 3
                flag = (count >= trigger)
            
            if flag:
                if not self.is_too_dangerous_to_attack():
                    self._attacking = True
            
        # Any robot on an adjacent, which has not yet moved, should move off.
        def adjacents_dodge(self, movemap):
            for floc in self._adjacents:
                if floc in self._parent._REMAINING and floc in self._assigned:
                    for tdir in self._parent._DIRS:
                        movemap.chk_add_dir(floc, tdir)
        
        # Fill the dictionary up with adjacent attacks
        def compute_adj_attacking(self):
            attacking = {}
            for floc in self._adjacents:
                if floc in self._parent._REMAINING and floc in self._assigned:
                    amap = self._parent.AdjEnemyMap(self._parent, floc)
                    if amap.get_weakest_loc() != None:
                        attacking[floc] = amap.get_weakest_loc();
            return attacking
                        
        # Any robot on an adjacent, should simply attack the weaker nearer robot.
        def adjacents_attack(self):
            attacking = self.compute_adj_attacking()
            for floc in attacking.keys():
                self._parent.apply_attack(floc, attacking[floc])
                
        # Any robot 2sq away should close in for the kill
        def near_pounce(self, movemap):
            for floc in self._near:
                if floc in self._parent._REMAINING and floc in self._assigned:
                    dirs = self._parent.get_dirs(floc, self._eloc)
                    for tdir in dirs:
                        movemap.chk_add_dir(floc, tdir)
        
        def is_dangerous(self, floc, tloc):
            return self._parent._game.robots[floc].hp < self._parent._game.robots[tloc].hp

        # Pretend we want to attack, see if this is way too dangerous.
        def is_too_dangerous_to_attack(self):
            # Consider only if collective HP is too small
            friendly_hp = 0
            enemy_hp = 0;
            for floc in self._friendlies:
                friendly_hp += self._parent._game.robots[floc].hp
            for eloc in self.get_enemies():
                enemy_hp += self._parent._game.robots[eloc].hp
            if friendly_hp > enemy_hp:
                return False
            
            # Otherwise if we are out numbered, then make sure it is not too dangerous
            attacking = self.compute_adj_attacking()
            for floc in attacking.keys():
                if self.is_dangerous(floc, attacking[floc]):
                    return True
            movemap = self._parent.MoveMap(self._parent)
            self.near_pounce(movemap)
            moves = movemap.get_moves()
            for floc in moves.keys():
                adj = self._parent.AdjEnemyMap(self._parent, moves[floc])
                if adj.get_count() > 0:
                    if self.is_dangerous(floc, adj.get_strongest_loc()):
                        return True
            return False
            
    # Size of the GAME MAP.
    _MAPSIZE = 19
    # Suicide damage
    _SUICIDE_DAMAGE = 15
    # Max attack damage
    _MAX_ATTACK_DAMAGE=10
    # The turn we last processed globals
    _CURTURN = 0
    # Orthogonal directions
    _DIRS = [(0, -1), (1, 0), (0, 1), (-1, 0)]
    # Diagonal directions
    _DIAG = [(1, -1), (1, 1), (-1, 1), (-1, -1)]
    # 2sq orthogonal directions
    _DIR2 = [(0, -2), (2, 0), (0, 2), (-2, 0)]
    # Diagonal adjacents
    _DADJ = {(1, -1):[(1, 0), (0, -1)], \
            (1, 1)  :[(1, 0), (0, 1)], \
            (-1, 1) :[(-1, 0), (0, 1)], \
            (-1, -1):[(-1, 0), (0, -1)]}
    # 2sq adjacent directions
    _DADJ2 = {(0, -2):(0, -1), \
              (2, 0) :(1, 0), \
              (0, 2) :(0, 1), \
              (-2, 0):(-1, 0)}
    _FEINT = {(0, -1):[(-1, 0), (1, 0)],
            (0, 1):[(-1, 0), (1, 0)],
            (-1, 0):[(0, 1), (0, -1)],
            (1, 0):[(0, 1), (0, -1)]}

    # Spawn turn coming: True if new robots are to be spawned soon.
    _STC = False
    # List of commands to issue for each robot, keyed by location. The value is the command. 
    _CMDS = {}
    # List of new locations robots are currently intending to move into.
    _NEW_MOVES = []
    # List of locations we are moving into
    _MOVING_INTO=[]
    # List of all locations robots are currently intending to move into or intending to stay in
    _OCCUPIED = []
    # List of locations we are attacking
    _ATTACKING = []
    # List of empty locations we are attacking because we are guessing an enemy robot might move to it
    _GUESS_ATTACKS = []
    # A list of misses. Key is the location from, value is a list of attacks missed.
    _MISSED = {}
    # The list of locations to apply have the robot guard because they are fine where they are.
    _STAY_PUT = []
    # Convenience: list of locations containing friendly robots
    _FRIENDLIES = []
    # Convenience: list of locations containing enemy robots
    _ENEMIES = []
    # Convenience: list of location containing friedly units that still need moves.
    _REMAINING = []
    # Target Map for each enemy
    _TARGETS = {}
    # Each friendly is assigned to one target
    _ASSIGNED = {}
    # Where a unit friendly was last turn
    _LAST_TURN = {}

    # DEBUG
    _LOG = False
    _LOOKAT = [(6,8),(4,9),(3,15)]
    _DEBUG_TURNS = [44]

    def showcmd(self, prefix, loc):
        if self._LOG:
            if loc in self._CMDS.keys():
                print prefix, " CMD: ", loc, "->", self._CMDS[loc]

    def showcmds(self, prefix):
        if self._LOG:
            for loc in self._LOOKAT:
                self.showcmd(prefix, loc)

    def act(self, game):

        if game.turn in self._DEBUG_TURNS:
            self._LOG = True
        else:
            self._LOG = False

        self._game = game

        if self.init():
            self.ponder_on_spawned()
            self.ponder_make_way()
            self.showcmds("BEFORE RUN AWAY1")
            self.ponder_run_away(self._MAX_ATTACK_DAMAGE, 4)
            self.showcmds("AFTER RUN AWAY1")
            self.ponder_make_way()
            self.ponder_run_away(self._SUICIDE_DAMAGE, 2)
            self.showcmds("AFTER RUN AWAY2")
            self.ponder_make_way()
            self.ponder_adj_enemies()
            self.showcmds("AFTER ADJ ENEMIES")
            self.ponder_make_way()
            self.ponder_targets()
            self.showcmds("AFTER TARGETS")
            self.ponder_guard()
            self.ponder_guess_attack()
            self.showcmds("AFTER GUESS ATTACK")
            self.ponder_make_way()
            self.ponder_chase_enemy()
            self.showcmds("AFTER CHASE")


            self.record_last_turn()

        cmd = self.get_cmd()
        if self._LOG:
            print "ROBOT ", self.robot_id, "@", self.location, ":", cmd
        return cmd

    # The following computes the moves for ALL robots when the first robot move for a new turn are requested.
    # After it is computed, all robots will have moves assigned, and subsequent calls for the same turn
    # will simply return the result.
    def init(self):
        if self._CURTURN == self._game.turn:
            return False

        # Compute Spawn Turn Coming
        remainder = self._game.turn % rg.settings.spawn_every
        self._STC = (remainder == 0 or remainder == (rg.settings.spawn_every - 1))

        # Compute friendly and enemy robot locations (Convenience)
        self._FRIENDLIES = []
        self._ENEMIES = []
        for loc in self._game.robots.keys():
            bot = self._game.robots[loc]
            if bot.player_id == self.player_id:
                self._FRIENDLIES.append(loc)
            else:
                self._ENEMIES.append(loc)

        # Determine misses
        self.miss_count()
        
        self._REMAINING = list(self._FRIENDLIES)
        self._CMDS = {}
        self._OCCUPIED = []
        self._ATTACKING = []
        self._NEW_MOVES = []
        self._MOVING_INTO = []
        self._STAY_PUT = []
        self._GUESS_ATTACKS = []
        self._CURTURN = self._game.turn
        return True

    def record_last_turn(self):
        self._LAST_TURN = {}
        for loc in self._FRIENDLIES:
            robot_id = self._game.robots[loc].robot_id
            self._LAST_TURN[robot_id] = loc
            
    #####################
    # MISS SUPPORT
    #####################

    # Record each miss from last turn attacks
    def miss_count(self):
        for floc in self._CMDS.keys():
            if self._CMDS[floc][0] == 'attack':
                tloc = self._CMDS[floc][1]
                if self.is_enemy(tloc):
                    self.miss_clear(floc)
                elif floc in self._MISSED.keys():
                    self._MISSED[floc].append(tloc)
                else:
                    self._MISSED[floc] = [tloc]

    def miss_clear(self, floc):
        if floc in self._MISSED.keys():
            self._MISSED[floc] = []

    def miss_get(self, floc, tloc):
        if floc in self._MISSED.keys():
            count = 0
            for loc in self._MISSED[floc]:
                if loc == tloc:
                    count += 1
            return count
        return 0

    ####################
    # PONDER FUNCTIONS #
    ####################

    # If a robot is on a spawn square and STC is True, then they get the move that robot off.
    def ponder_on_spawned(self):

        if self._STC:
            movemap = self.MoveMap(self)
            movemap.set_blocking_allowed(False)
    
            for loc in self._REMAINING:
                if self.is_spawn(loc):
                    movemap.move(loc)
    
            movemap.apply()

    # If there are locations that we are OCCUPIED that a robot is
    # currently on, that robot must move off.
    def ponder_make_way(self):
        if len(self._NEW_MOVES) > 0:
            movemap = self.MoveMap(self)
            movemap.set_blocking_allowed(False)

            for loc in self._NEW_MOVES:
                if self.is_friendly(loc) and loc in self._REMAINING:
                    movemap.move(loc)
                if loc not in self._OCCUPIED:
                    self._OCCUPIED.append(loc)

            movemap.apply()
            self._NEW_MOVES = []

    # If there are units close to death, they should run away.
    def ponder_run_away(self, threshold, safe_dist):
        movemap = self.MoveMap(self)
        movemap.set_blocking_allowed(False)
        for loc in self._REMAINING:
            bot = self._game.robots[loc]
            if bot.hp <= threshold:
                # Select a direction farthest from the closest enemy
                eloc = self.closest_enemy(loc)
                if eloc != None:
                    dist = rg.dist(loc, eloc)
                    if dist < safe_dist:
                        self.do_run_away(movemap, dist, loc, eloc)
                        if not movemap.has(loc):
                            if len(movemap.get_blocking()) > 0:
                                movemap.set_blocking_allowed(False)
                                self.do_run_away(movemap, dist, loc, eloc)
                                movemap.set_blocking_allowed(True)
                            if not movemap.has(loc):
                                movemap.add_to_failed(loc)
                    else:
                        movemap.add_to_failed(loc)

        movemap.apply()

        for loc in movemap.get_failed():
            self.apply_prefer_stay_put(loc)

    def do_run_away(self, movemap, beyond, floc, eloc):
        best_dist = 0
        movemap.clear_blocking()
        for tdir in self._DIRS:
            tloc = self.get_loc(floc, tdir)
            tdist = rg.dist(tloc, eloc)
            # Anything moving farther away will do
            if tdist > beyond:
                if movemap.chk(floc, tloc):
                    if tdist >= best_dist:
                        movemap.insert(floc, tloc)
                        best_dist = tdist
                    else:
                        movemap.add(floc, tloc)
      
    # If there units we previously thought would be better off staying put rather
    # than chasing down an enemy, if they have nothing else to do, then set them to guard
    def ponder_guard(self):
        guarding = []
        for loc in self._STAY_PUT:
            if loc in self._REMAINING:
                guarding.append(loc)
        for loc in guarding:
            self.apply_guard(loc)
        self._STAY_PUT = []
    
    #
    # Consider suicide chance for friendlies with adjacent enemies.
    # If we are surrounded, then force attack if not suicide.
    # Otherwise if we are not too weak, go ahead and attack.
    # If we are way too weak, run away.
    # Other cases are decided later.
    #
    def ponder_adj_enemies(self):
        movemap = self.MoveMap(self)
        suicides = []
        attacking = {}
        for loc in self._REMAINING:
            bot = self._game.robots[loc]
            adj = self.AdjEnemyMap(self, loc)
            count = adj.get_count()
            if count >= 1:
                if count >= 4:
                    consider = rg.settings.robot_hp
                elif count >= 3:
                    consider = rg.settings.robot_hp / 2
                elif count >= 2:
                    consider = rg.settings.robot_hp / 3
                else:
                    consider = 0
                if bot.hp < consider:
                    chance = self.get_hp_weight(bot.hp) + 20 * count
                    if self.roll_die(chance):
                        suicides.append(loc)
                        continue
                if count >= 4 or (bot.hp > adj.get_strongest_hp() and bot.hp > self._SUICIDE_DAMAGE):
                    attacking[loc] = adj.get_weakest_loc()
                elif bot.hp < adj.get_weakest_hp():
                    tdir = self.get_dir(loc, adj.get_strongest_loc())
                    if self.valid_dir(tdir):
                        for ddir in self._DIRS:
                            if ddir != tdir:
                                movemap.chk_add_dir(loc, ddir)               

        for loc in suicides:
            self.apply_suicide(loc)

        movemap.apply()

        if len(movemap.get_failed()) > 0:
            for loc in movemap.get_failed():
                adj = self.AdjEnemyMap(self, loc)
                attacking[loc] = adj.get_weakest_loc()

        for loc in attacking.keys():
            self.apply_attack(loc, attacking[loc])

    def ponder_targets(self):
        # Build target maps
        self._TARGETS = {}
        for eloc in self._ENEMIES:
            self._TARGETS[eloc] = self.TargetMap(self, eloc)
        
        # Assign each friendly to one target
        # Choose the target with the largest count.
        # If tie, then chose with the largest weight
        #
        self._ASSIGNED = {}
        for floc in self._REMAINING:
            bmap = None
            for eloc in self._TARGETS.keys():
                tmap = self._TARGETS[eloc]
                if tmap.has_friendly(floc):
                    if bmap == None:
                        bmap = tmap
                        continue
                    badj = bmap.is_adjacent(floc)
                    tadj = tmap.is_adjacent(floc)
                    # If the best is an adjacent enemy, this trumps any other enemy that is not.
                    if badj and not tadj:
                        continue
                    # If the consider is an adjacent, this immediately trumps the best if not.
                    if tadj and not badj:
                        bmap = tmap
                    elif tmap.get_count() > bmap.get_count():
                        bmap = tmap
                    elif tmap.get_count() == bmap.get_count():
                        if tmap.get_weight() > bmap.get_weight():
                            bmap = tmap
            if bmap != None:
                self._ASSIGNED[floc] = bmap.get_target_loc()
                bmap.add_assigned(floc)
                
        movemap = self.MoveMap(self)
        for eloc in self._TARGETS.keys():
            tmap = self._TARGETS[eloc]
            tmap.compute_attacking()
            if tmap.is_attacking():
                tmap.adjacents_attack()
                tmap.near_pounce(movemap)
            else:
                tmap.adjacents_dodge(movemap)

        movemap.apply() 
                
    # If an enemy is diagonally adjacent or 2 sq away orthogonally adjacent
    # consider an attack unless the last time we tried there was a miss.
    def ponder_guess_attack(self):
        gmap = self.GuessAttackMap(self)
        for floc in self._REMAINING:
            gmap.guess_attack(floc)
        gmap.apply()

    # Chase down the nearest enemy.
    # We want to be careful here not to collide with other friendlies
    def ponder_chase_enemy(self):
        self.showcmds("BEFORE 1st CHASE")
        movemap = self.MoveMap(self)
        remaining = list(self._REMAINING)
        for loc in remaining:
            closest_eloc = self.closest_enemy(loc)
            if closest_eloc != None:
                dirs = self.get_dirs(loc, closest_eloc)
                for tdir in dirs:
                    movemap.chk_add_dir(loc, tdir)
                if not movemap.has(loc):
                    movemap.add_to_failed(loc)
        movemap.apply()

        self.showcmds("AFTER 1st CHASE")
        # Be more forgiving second time around
        if len(movemap.get_failed()) > 0:
            movemap2 = self.MoveMap(self)
            for loc in movemap.get_failed():
                closest_eloc = self.closest_enemy(loc)
                if closest_eloc != None:
                    notdir = self.get_dir(closest_eloc, loc)
                    for tdir in self._DIRS:
                        if tdir != notdir:
                            movemap2.chk_add_dir(loc, tdir)
            movemap2.apply()

    #######################
    # SUPPORT FUNCTIONS
    #######################

    def apply_move(self, floc, tloc):
        self._CMDS[floc] = ['move', tloc]
        if floc in self._REMAINING:
            self._REMAINING.remove(floc)
        else:
            print "ERROR: location ", floc, " has already moved (not in REMAINING)"
        if not tloc in self._NEW_MOVES:
            self._NEW_MOVES.append(tloc)
        if not tloc in self._MOVING_INTO:
            self._MOVING_INTO.append(tloc)
        self.miss_clear(floc)

    def apply_attack(self, floc, tloc):
        self._CMDS[floc] = ['attack', tloc]
        self._REMAINING.remove(floc)
        if not floc in self._OCCUPIED:
            self._OCCUPIED.append(floc)
        if not tloc in self._ATTACKING:
            self._ATTACKING.append(tloc)

    def apply_guess_attack(self, floc, tloc):
        self._CMDS[floc] = ['attack', tloc]
        self._REMAINING.remove(floc)
        self._GUESS_ATTACKS.append(tloc)
        if not floc in self._OCCUPIED:
            self._OCCUPIED.append(floc)
        if not tloc in self._ATTACKING:
            self._ATTACKING.append(tloc)

    def apply_suicide(self, loc):
        self._CMDS[loc] = ['suicide']
        self._REMAINING.remove(loc)
        if not loc in self._OCCUPIED:
            self._OCCUPIED.append(loc)

    def apply_guard(self, loc):
        self._CMDS[loc] = ['guard']
        self._REMAINING.remove(loc)
        if not loc in self._OCCUPIED:
            self._OCCUPIED.append(loc)

    def apply_prefer_stay_put(self, loc):
        if not loc in self._STAY_PUT:
            self._STAY_PUT.append(loc)

    def closest_enemy(self, floc):
        closest_dist = 1000
        closest_eloc = None
        for eloc in self._ENEMIES:
            dist = rg.dist(floc, eloc)
            if dist < closest_dist:
                closest_dist = dist
                closest_eloc = eloc
        return closest_eloc

    def get_cmd(self):
        if self.location in self._CMDS.keys():
            return self._CMDS[self.location]
        return ['guard']

    # Return direction of floc to tloc
    def get_dir(self, floc, tloc):
        dx = tloc[0] - floc[0]
        dy = tloc[1] - floc[1]
        adx = abs(dx)
        ady = abs(dy)
        if adx != 0:
            ndx = dx / adx
        else:
            ndx = 0
        if ady != 0:
            ndy = dy / ady
        else:
            ndy = 0
        return (ndx, ndy)

    # Just like the rg.toward() function but returns one or more directions, not a single direction
    # The directions returned are in order of priority
    def get_dirs(self, floc, tloc):
        dx = tloc[0] - floc[0]
        dy = tloc[1] - floc[1]
        adx = abs(dx)
        ady = abs(dy)
        if adx != 0:
            ndx = dx / adx
        else:
            ndx = 0

        if ady != 0:
            ndy = dy / ady
        else:
            ndy = 0

        if ndx == 0:
            return [(0, ndy)]
        elif ndy == 0:
            return [(ndx, 0)]
        elif adx > ady:
            return [(ndx, 0), (0, ndy)]
        elif ady > adx:
            return [(0, ndy), (ndx, 0)]
        # Return direction which is closest to the center_point first
        cx1 = floc[0] + ndx
        cy1 = floc[1]
        cx2 = floc[0]
        cy2 = floc[1] + ndy
        cloc1=(cx1,cy1)
        cloc2=(cx2,cy2)
        if self.get_closer_to_center(cloc1, cloc2) == cloc1:
            return [(ndx, 0), (0, ndy)]            
        return [(0, ndy), (ndx, 0)]
            
    # Return the direction closer to the center
    def get_closer_to_center(self, cloc1, cloc2):
        w1 = self.get_center_weight(cloc1)
        w2 = self.get_center_weight(cloc2)
        if w1 < w2:
            return cloc1
        return cloc2
    
    def get_center_weight(self, loc):
        return abs(loc[0] - rg.CENTER_POINT[0]) + abs(loc[1] - rg.CENTER_POINT[1])
       
    def get_loc(self, loc, tdir):
        return (loc[0] + tdir[0], loc[1] + tdir[1])

    def get_hp_weight(self, hp):
        return rg.settings.robot_hp - hp

    def has_robot(self, loc):
        return loc in self._game.robots

    def is_friendly(self, loc):
        return loc in self._FRIENDLIES

    def is_enemy(self, loc):
        return loc in self._ENEMIES

    def is_normal(self, loc):
        types = rg.loc_types(loc)
        if 'obstacle' in types or 'invalid' in types:
            return False
        return 'normal' in types

    def is_spawn(self, loc):
        return 'spawn' in rg.loc_types(loc)

    def is_occupied(self, loc):
        return loc in self._OCCUPIED or loc in self._NEW_MOVES or loc in self._ENEMIES or loc in self._MOVING_INTO

    def is_moving(self, loc):
        if loc in self._CMDS.keys():
            return self._CMDS[loc][0] == 'move'
        return False

    def is_attacking(self, loc):
        return loc in self._ATTACKING

    def is_attacking_from_to(self, floc, tloc):
        if floc in self._CMDS.keys():
            return self._CMDS[floc][0] == 'attack' and self._CMDS[floc][1] == tloc
        return False
        
    # If we are surrounded then add to suicide chance
    def roll_die(self, chance):
        die = random.randint(1, 100)
        return die <= chance

    def valid_dir(self, tdir):
        if tdir[0] != 0 and tdir[1] != 0:
            return False
        if abs(tdir[0]) != 1 and abs(tdir[1]) != 1:
            return False
        return True 
 
    def get_dist_to_closest_enemy(self, floc):
        best = 1000
        for eloc in self._ENEMIES:
            dist = rg.dist(floc, eloc)
            if dist < best:
                best = dist
        return best
            
                
