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

    # Manages movement collisions between robots that want the same square.
    class MoveMapFlag:
        
        def __init__(self, parent):
            self._from = {}
            self._to = {}
            self._failed = []
            self._parent = parent
            # True: spawn squares OK unless STC, False: spawn squares not okay
            self._flag_spawn_ok = True
            # True: enemy robot count must be 0, False: ignore
            self._flag_safe_only = True
            # True: no friends or enemies, False: ignore
            self._flag_unoccupied_only = True
            # True: no enemies, False: ignore
            self._flag_enemy_free = True
            # Consider only moves that are farther away from this location
            self._farther_than_loc = {}
            # Priority given to these robots on ties
            self._priority = []
   
        def set_spawn_ok(self, flag):
            self._flag_spawn_ok = flag
        
        def set_safe_only(self, flag):
            self._flag_safe_only = flag
            
        def set_unoccupied_only(self, flag):
            self._flag_unoccupied_only = flag
        
        def set_enemy_free(self, flag):
            self._flag_enemy_free = flag
            
        def set_farther_than_loc(self, floc, tloc):
            self._farther_than_loc[floc] = tloc
        
        def set_priority(self, floc):
            if not floc in self._priority:
                self._priority.append(floc)
        # Add the specified possible move for a robot. The robot is identified
        # by the floc and it's target move spot by tloc.
        #
        # If a robot is given more than one location to move to, the one given
        # first takes precedence over later ones.
        def add(self, floc, tloc):
            if floc in self._from.keys():
                if tloc not in self._from[floc]:
                    self._from[floc].append(tloc)
            else:
                self._from[floc] = [tloc]

            if tloc in self._to.keys():
                if floc not in self._to[tloc]:
                    self._to[tloc].append(floc)
            else:
                self._to[tloc] = [floc]

        def is_ok(self, floc, tloc):
            if not self._parent.is_normal(tloc):
                return False
            
            if self._parent.is_spawn(tloc):
                if not self._flag_spawn_ok or self._parent._STC:
                    return False
                
            if self._flag_safe_only:
                if self._parent.count_enemies(tloc) > 0:
                    return False
            
            if self._flag_unoccupied_only:
                if self._parent.has_robot(tloc):
                    return False
            
            if self._flag_enemy_free:
                if self._parent.is_enemy(tloc):
                    return False
            
            if self._parent.is_occupied(tloc):
                return False
                
            if floc in self._farther_than_loc.keys():
                eloc = self._farther_than_loc[floc]
                fdist = rg.dist(floc, eloc)
                tdist = rg.dist(tloc, eloc)
                if tdist <= fdist:
                    return False

            return True
        
        def add_if_ok(self, floc, tloc):
            if self.is_ok(floc, tloc):
                self.add(floc, tloc)
                return True
            return False
    
        def add_if_ok_dir(self, floc, tdir):
            tloc = self._parent.get_loc(floc, tdir)
            return self.add_if_ok(floc, tloc)
        
        # Add moves to any squares toward the conditions that we have established.
        # See set_() methods.
        def add_moves(self, floc):
            locs=self._parent.get_locs_sorted_by_center(floc)
            for tloc in locs:
                if self.is_ok(floc, tloc):
                    self.add(floc, tloc)
            if not self.has_moves(floc):
                self.add_failed(floc)
                
        def has_failed(self):
            return len(self._failed) > 0
        
        def reset_moves(self):
            self._from = {}
            self._to = {}
            self._failed = []
            
        def apply_again_failed(self):
            failed = list(self._failed)
            self.reset_moves()
            for floc in failed:
                self.add_moves(floc)
            self.apply()
                
        def add_failed(self, floc):
            if not floc in self._failed:
                self._failed.append(floc)

        def get_failed(self):
            return self._failed
        
        # Apply the moves to the robots.
        def apply(self):
            moves = self.get_moves();
            for floc in moves.keys():
                self._parent.apply_move(floc, moves[floc])
                        
        # Return dictionary of moves we have computed.
        # The key is from, the value is the target
        def get_moves(self):
            self.detect_collisions()
            moves = {}
            for floc in self._from.keys():
                if len(self._from[floc]) > 0:
                    tloc=self._from[floc][0]
                    moves[floc] = tloc
                else:
                    self.add_failed(floc)
                             
            return moves
                
        # If more than one robot wants to move to the same square, resolve the conflict
        # by letting the robot with fewer choices have that square. The other robot
        # will select another square. In the case of ties, it is possible a robot
        # will be left with no place to go. Add them to the failed list.
        def detect_collisions(self):
            for tloc in self._to.keys():
                if len(self._to[tloc]) > 1:
                    least_floc = []
                    least_count = 1000
                    tolist = self._to[tloc]
                    for floc in tolist:
                        count = len(self._from[floc])
                        if count < least_count:
                            least_floc = [floc]
                            least_count = count
                        elif count == least_count:
                            if floc in self._priority:
                                least_floc.insert(0, floc)
                            else:
                                least_floc.append(floc)
                        
                    if len(least_floc) > 0:
                        self.only(least_floc[0], tloc)
        
        # Only permit the move from the floc to the tloc
        # Remove all other mvoes
        def only(self, floc, tloc):
            for f in self._to[tloc]:
                if f != floc:
                    self._from[f].remove(tloc)
            self._to[tloc] = [floc]
        
        def has_moves(self, floc):
            if floc not in self._from.keys():
                return False
            if len(self._from[floc]) > 0:
                return True
            return False
        
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

        # OLD, KEEPING AROUND JUST IN CASE: 
        def _old_filter_closer_to_enemy(self, floc, moves):
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
                            
        def old_filter_nearer_to_center(self, moves):
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
        
        def OLD_filter_out_old(self, floc, moves):
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
        
    class AdjFriendMapDir:

        def __init__(self, parent, loc, indir):
            self._parent = parent
            self._count=0
            dirs = parent._DIRS + parent._DIAG + parent._DIR2
            for tdir in dirs:
                if self.match(indir, tdir):
                    dloc = parent.get_loc(loc, tdir)
                    if self._parent.is_friendly(dloc):
                        self._count += 1
                  
        def get_count(self):
            return self._count
    
        def match(self, indir, tdir):
            if indir[0] != 0:
                return tdir[0] == indir[0]
            else:
                return tdir[1] == indir[1]

    class AdjEnemyMap:

        def __init__(self, parent, loc):
            self._adj = []
            self._weakest_loc = None
            self._weakest_hp = 1000
            self._strongest_loc = None
            self._strongest_hp = 0
            self._parent = parent
            # Enemies are considered weaker if they are being surrounded by us.
            self._weakest_adjusted_loc = None
            self._weakest_adjusted_hp = 1000
            self._collective_enemy_hp = 0
            self._num_enemies = 0
            
            for tdir in parent._DIRS:
                dloc = parent.get_loc(loc, tdir)
                if parent.is_enemy(dloc):
                    self._adj.append(dloc)
                    self._num_enemies += 1
                    ebot = parent._game.robots[dloc]
                    self._collective_enemy_hp += ebot.hp
                    if ebot.hp < self._weakest_hp:
                        self._weakest_loc = dloc
                        self._weakest_hp = ebot.hp
                    if ebot.hp > self._strongest_hp:
                        self._strongest_loc = dloc
                        self._strongest_hp = ebot.hp
                    count = self._parent.count_friendlies(dloc)
                    adjusted_hp = ebot.hp
                    if count > 1:
                        adjusted_hp -= self._parent._ADJUST_PER_BOT * (count-1)
                        if adjusted_hp < self._weakest_adjusted_hp:
                            self._weakest_adjusted_loc = dloc
                            self._weakest_adjusted_hp = adjusted_hp
                            
            if self._weakest_adjusted_loc == None:
                self._weakest_adjusted_loc = self._weakest_loc
                self._weakest_adjusted_hp = self._weakest_hp

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
        
        # The weakest adjacent enemy who HP is reduced per friendly bot.
        def get_weakest_adjusted_hp(self):
            return self._weakest_adjusted_hp

        def get_weakest_adjusted_loc(self):
            return self._weakest_adjusted_loc

        def get_collective_enemy_hp(self):
            return self._collective_enemy_hp
        
        def has_adjusted(self):
            return self._weakest_adjusted_loc != None
        
        def get_locs(self):
            return self.adj_

        def get_bots(self):
            bots = []
            for loc in self._adj:
                bots.append(self._parent._game.robots[loc])
            return bots
        
        def get_num_enemies(self):
            return self._num_enemies
  
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
            if self._parent.count_friendlies(tloc) >= 2:
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
            return self._parent.is_normal(loc) and not loc in self._parent._OCCUPIED
        
        def bounded_reduce(self, weight, amt):
            if weight > amt:
                return weight - amt
            return 1
            
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
            movemap = self._parent.MoveMapFlag(self._parent)
            movemap.set_safe_only(True)
            movemap.set_spawn_ok(True)
            movemap.set_unoccupied_only(True)
 
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
            tlocs=[]
            for mdir in dirs:
                dir1 = self._parent._FEINT[mdir][0]
                dir2 = self._parent._FEINT[mdir][1]
                tloc1 = self._parent.get_loc(floc, dir1)
                tloc2 = self._parent.get_loc(floc, dir2)
                if self._parent.get_closer_to_center(tloc1, tloc2) == tloc1:
                    tlocs.append(tloc1)
                    tlocs.append(tloc2)
                else:
                    tlocs.append(tloc2)
                    tlocs.append(tloc1)
            was_safe=False
            for tloc in tlocs:
                if self._parent.count_enemies(tloc) == 0:
                    movemap.add_if_ok(floc, tloc)
                    was_safe=True
            if not was_safe:
                self._parent.apply_guess_attack(floc, tlocs[0])

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
            # List of enemies near this target
            self._enemies = []
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
                        self._enemies.append(loc)
                        if wdist == 2:
                            self._near_enemies.append(loc)
                        elif wdist == 1:
                            self._adj_enemies.append(loc)
                        
        def show(self):
            print "TARGET ", self._eloc, " ATTACKING: ", self._attacking
            print "  ASSIGNED:", self._assigned
            print "  ADJACENTS: ", self._adjacents
            print "  NEAR: ", self._near
            
        def has_friendly(self, floc):
            return floc in self._friendlies
        
        def is_adjacent(self, floc):
            return floc in self._adjacents

        def is_assigned(self, floc):
            return floc in self._assigned
    
        def get_enemies(self):
            return self._enemies
        
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
            
            count = len(self._near) + len(self._adjacents)
            numenemies=len(self._parent._ENEMIES)
            numfriendlies=len(self._parent._FRIENDLIES)
            if numenemies > numfriendlies:
                trigger = 4
            else:
                trigger = 3
            flag=False
            # If one unit already attacking then yes.
            if self._parent.is_attacking(self._eloc):
                flag = True
            else:
                flag = (count >= trigger)
            
            if flag:
                if not self.is_too_dangerous_to_pounce(trigger):
                    self._attacking = True
            
        # Any robot on an adjacent, which has not yet moved, should move off.
        def adjacents_dodge(self, movemap):
            for floc in self._adjacents:
                if floc in self._parent._REMAINING and floc in self._assigned:
                    # Do feint directions
                    tdir = self._parent.get_dir(floc, self._eloc)
                    if tdir in self._parent._FEINT.keys():
                        for fdir in self._parent._FEINT[tdir]:
                            movemap.add_if_ok_dir(floc, fdir)
                    # Finally add the move-away direction
                    tdir = self._parent.get_opposite_dir(tdir)
                    movemap.add_if_ok_dir(floc, tdir)
        
        # Fill the dictionary up with adjacent attacks
        # That is, the adjacents may not necessarily be attacking the target if
        # there are other robots weaker next to it.
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
                        movemap.add_if_ok_dir(floc, tdir)
                        # Choose only one possible feint direction as well just in case we need it.
                        # The direction with the less friendlies so as to spread our selves out more.
                        # Ignore the other one.
                        if len(dirs) == 1:
                            best_adir=None
                            best_count=1000
                            for adir in self._parent._FEINT[tdir]:
                                count=self._parent.count_friendlies_in_dir(self._eloc, adir)
                                if best_adir == None or count < best_count:
                                    best_adir=adir
                                    best_count=count
                            if best_adir != None:
                                if movemap.add_if_ok_dir(floc, best_adir):
                                    movemap.set_priority(floc)
   
        def is_dangerous(self, floc, tloc):
            return self._parent._game.robots[floc].hp < self._parent._game.robots[tloc].hp

        # Pretend we want to attack, see if this is way too dangerous.
        def is_too_dangerous_to_pounce(self, trigger):
            # Assume an attack, move each robot in pounce mode.
            # If the target square is too dangerous don't count it.
            # If that leaves us with too few robots abort.
            movemap = self._parent.MoveMapFlag(self._parent)
            movemap.set_safe_only(False)
            movemap.set_spawn_ok(True)
            movemap.set_unoccupied_only(True)
            self.near_pounce(movemap)
            moves = movemap.get_moves()
            count = len(self._adjacents)
            we_outnumber_enemy = (len(self._friendlies) > len(self._enemies))
            for floc in moves.keys():
                fbot = self._parent._game.robots[floc]
                adj = self._parent.AdjEnemyMap(self._parent, moves[floc])
                if adj.get_collective_enemy_hp() <= fbot.hp:
                    count += 1                
                elif we_outnumber_enemy and fbot.hp > self._parent._MAX_ATTACK_DAMAGE*adj.get_num_enemies():
                    count += 1
            # If we have so many possible pouncers then accept
            if count >= trigger:
                return False
            
            # If we out number the enemies then attack if only 2.
            if we_outnumber_enemy:
                attacking = self.compute_adj_attacking()
                # We out number them, if 2 more adjacents, go ahead. 
                if len(attacking) > 1:
                    return False
                # If only one, then attack if we are stronger
                for floc in attacking.keys():
                    if self.is_dangerous(floc, attacking[floc]):
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
    # Amount to adjust HP per adjacent friendly unit, to see if we should dodge it
    _ADJUST_PER_BOT=8
    # Orthogonal directions
    _DIRS = [(0, -1), (1, 0), (0, 1), (-1, 0)]
    # Diagonal directions
    _DIAG = [(1, -1), (1, 1), (-1, 1), (-1, -1)]
    # 2sq orthogonal directions
    _DIR2 = [(0, -2), (2, 0), (0, 2), (-2, 0)]
    # Diagonal adjacents
    _DADJ = {(1, -1):[(1, 0), (0, -1)], \
             (1, 1) :[(1, 0), (0, 1)], \
             (-1, 1):[(-1, 0), (0, 1)], \
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
    # Convenience: list of location containing friendly units that still need moves.
    _REMAINING = []
    # Target Map for each enemy
    _TARGETS = {}
    # Each friendly is assigned to one target
    _ASSIGNED = {}
    # To save computing power remember distances to center
    _DIST_TO_CENTER = {}
    # Where a unit friendly was last turn
    _LAST_TURN = {}

    # DEBUG
    _LOG = False
    _LOOKAT = [(14,15),(14,13),(13,13),(13,14)]
    _DEBUG_TURNS = [17]
    
    def showcmd(self, prefix, loc):
        if self._LOG:
            if loc in self._CMDS.keys():
                print prefix, "CMD: ", loc, "->", self._CMDS[loc]

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
            self.ponder_on_spawned_emergency()
            self.showcmds("AFTER SPAWNED EMERGENCY")
            self.ponder_make_way()
            self.showcmds("AFTER MAKE WAY 1")
            self.ponder_run_away(self._MAX_ATTACK_DAMAGE, 4)
            self.showcmds("AFTER RUN AWAY 1")
            self.ponder_make_way()
            self.showcmds("AFTER MAKE WAY 2")
            self.ponder_run_away(self._SUICIDE_DAMAGE, 2)
            self.showcmds("AFTER RUN AWAY 2")
            self.ponder_make_way()
            self.showcmds("AFTER MAKE WAY 3")
            self.ponder_adj_enemies()
            self.showcmds("AFTER ADJ ENEMIES")
            self.ponder_make_way()
            self.showcmds("AFTER MAKE WAY 4")
            self.ponder_targets()
            self.showcmds("AFTER TARGETS")
            self.ponder_on_spawned_safe()
            self.showcmds("AFTER SPAWNED SAFE")
            self.ponder_guard()
            self.showcmds("AFTER GUARD")
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

    # If a robot is on a spawn square and the STC is next turn, then they need to emergency move off.
    def ponder_on_spawned_emergency(self):

        if self._STC:

            movemap = self.MoveMapFlag(self)
            movemap.set_safe_only(True)
            movemap.set_spawn_ok(False)
            movemap.set_unoccupied_only(True)
        
            for floc in self._REMAINING:
                if self.is_spawn(floc):
                    movemap.add_moves(floc)
                    movemap.apply()

            if not movemap.has_failed():
                return
       
            movemap.set_unoccupied_only(False)
            movemap.apply_again_failed()
                
            if not movemap.has_failed():
                return
            
            movemap.set_safe_only(False)
            movemap.apply_again_failed()
            
            if not movemap.has_failed():
                return
 
            for floc in movemap.get_failed():
                print "ponder_on_spawned(): ROBOT ", floc, " FAILED TO MOVE"
                
    # If a robot is on a spawn square and we have reached this point, move them off to be safe.
    # If they are in the corner, they will move off any way later on with chase_enemy.
    def ponder_on_spawned_safe(self):

        movemap = self.MoveMapFlag(self)
        movemap.set_safe_only(True)
        movemap.set_spawn_ok(False)
        movemap.set_unoccupied_only(True)
        
        for floc in self._REMAINING:
            if self.is_spawn(floc):
                movemap.add_moves(floc)
                
        movemap.apply()

    # If a robot is on a location that another robot wants to move to
    # then that robots must move off of where they are at.
    # Considerations:
    #   - spawn only okay if not STC
    #   - otherwise every is okay, except prefer non-dangerous
    def ponder_make_way(self):
        if len(self._NEW_MOVES) > 0:
            movemap = self.MoveMapFlag(self)
            
            movemap.set_safe_only(True)
            movemap.set_spawn_ok(True)
            movemap.set_unoccupied_only(True)

            for floc in self._NEW_MOVES:
                if self.is_friendly(floc) and floc in self._REMAINING:
                    movemap.add_moves(floc)

            self._NEW_MOVES = []

            movemap.apply()
            
            if not movemap.has_failed():
                return
            
            movemap.set_unoccupied_only(False)
            movemap.apply_again_failed()
            
            if not movemap.has_failed():
                return
            
            movemap.set_safe_only(False)
            movemap.apply_again_failed()
                

    # If there are units close to death, they should run away.
    def ponder_run_away(self, threshold, safe_dist):
        movemap = self.MoveMapFlag(self)
        
        movemap.set_safe_only(True)
        movemap.set_spawn_ok(True)
        movemap.set_unoccupied_only(True)

        stay_put=[]
        
        for floc in self._REMAINING:
            fbot = self._game.robots[floc]
            if fbot.hp <= threshold:
                # Select a direction farthest from the closest enemy
                eloc = self.closest_enemy(floc)
                if eloc != None:
                    movemap.set_farther_than_loc(floc, eloc)
                    # TODO: the moves added should be prioritized by the fartherest the way the higher the choice.
                    movemap.add_moves(floc)
                else:
                    stay_put.append(floc)

        movemap.apply()
        
        movemap.set_unoccupied_only(False)
        movemap.apply_again_failed()
        
        # At this point, the unit might suicide later if they still failed

        for floc in stay_put:
            self.apply_prefer_stay_put(floc)
     
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
    # If we get here, then the robot is assumed to be not too weak.
    # Consider suicide chance for friendlies with adjacent enemies.
    # If we are surrounded, then force attack if not suicide.
    # Otherwise if we are not too weak, go ahead and attack.
    # If we are way too weak, run away. But if we have support maybe not.
    # Other cases are decided later.
    #
    def ponder_adj_enemies(self):
        movemap = self.MoveMapFlag(self)
        
        movemap.set_safe_only(True)
        movemap.set_spawn_ok(False)
        movemap.set_unoccupied_only(True)

        suicides = []
        attacking = {}
        for floc in self._REMAINING:
            fbot = self._game.robots[floc]
            adj = self.AdjEnemyMap(self, floc)
            num_enemy = adj.get_count()
            num_normal = self.count_normal(floc)
            if num_enemy >= 1:
                if num_enemy >= num_normal:
                    consider = rg.settings.robot_hp / 2
                elif num_enemy >= num_normal-1 and num_enemy > 2:
                    consider = rg.settings.robot_hp / 5
                elif num_enemy >= num_normal-2 and num_enemy > 1:
                    consider = rg.settings.robot_hp / 10
                else:
                    consider = 0
                if fbot.hp < consider:
                    chance = self.get_hp_weight(fbot.hp) + 20 * num_enemy
                    if self.roll_die(chance):
                        suicides.append(floc)
                        continue
                # We are surrounded
                if num_enemy >= num_normal:
                    max_damage = self._MAX_ATTACK_DAMAGE * num_enemy
                    if fbot.hp < max_damage:
                        suicides.append(floc)
                    else:
                        attacking[floc] = adj.get_weakest_loc()
                    continue
                
                if num_enemy == 1:
                    # If we are stronger than the robot, attack.
                    # Otherwise move away.
                    if fbot.hp >= adj.get_weakest_adjusted_hp():
                        attacking[floc] = adj.get_weakest_adjusted_loc()
                    else:
                        movemap.add_moves(floc)
                else:
                    # Too many enemies, run away
                    movemap.add_moves(floc)

        for loc in suicides:
            self.apply_suicide(loc)

        movemap.apply()
        
        if movemap.has_failed():
            movemap.set_spawn_ok(True)
            movemap.apply_again_failed()
            if movemap.has_failed():
                movemap.set_unoccupied_only(False)
                movemap.apply_again_failed()
                if movemap.has_failed():
                    for loc in movemap.get_failed():
                        adj = self.AdjEnemyMap(self, loc)
                        if adj.get_count() > 0:
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
                
        movemap = self.MoveMapFlag(self)
        movemap.set_safe_only(False)
        movemap.set_spawn_ok(True)
        movemap.set_unoccupied_only(True)

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
    def ponder_chase_enemy(self):
        movemap = self.MoveMapFlag(self)
        movemap.set_safe_only(True)
        movemap.set_spawn_ok(True)
        movemap.set_unoccupied_only(True)
        remaining = list(self._REMAINING)
        for floc in remaining:
            closest_eloc = self.closest_enemy(floc)
            if closest_eloc != None:
                dirs = self.get_dirs(floc, closest_eloc)
                for tdir in dirs:
                    movemap.add_if_ok_dir(floc, tdir)
                    
                if not movemap.has_moves(floc):
                    movemap.add_failed(floc)
                    
        movemap.apply()

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
        if not tloc in self._OCCUPIED:
            self._OCCUPIED.append(tloc)
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

    # Return closest enemy to passed in location
    def closest_enemy(self, floc):
        closest_dist = 1000
        closest_eloc = None
        for eloc in self._ENEMIES:
            dist = rg.dist(floc, eloc)
            if dist < closest_dist:
                closest_dist = dist
                closest_eloc = eloc
        return closest_eloc

    # Return the number of friendlies adjacent to the location
    def count_friendlies(self, loc):
        adj = self.AdjFriendMap(self, loc)
        return adj.get_count()
    
    # Return the number of enemies adjacent to the location
    def count_enemies(self, loc):
        adj = self.AdjEnemyMap(self, loc)
        return adj.get_count()
    
    def count_friendlies_in_dir(self, loc, indir):
        adj = self.AdjFriendMapDir(self, loc, indir)
        return adj.get_count()

    def count_normal(self, loc):
        count = 0
        for tdir in self._DIRS:
            tloc = self.get_loc(loc, tdir)
            if self.is_normal(tloc):
                count += 1
        return count
    
    def get_cmd(self):
        if self.location in self._CMDS.keys():
            return self._CMDS[self.location]
        return ['guard']

    # Return raw direction, that is, -2,0 is okay
    def get_raw_dir(self, floc, tloc):
        dx = tloc[0] - floc[0]
        dy = tloc[1] - floc[1]
        return (dx,dy)
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
    
    # Return opposite direction
    def get_opposite_dir(self, tdir):
        return (tdir[0]*-1, tdir[1]*-1)
    
    # Return the direction closer to the center
    def get_closer_to_center(self, cloc1, cloc2):
        w1 = self.get_dist_to_center(cloc1)
        w2 = self.get_dist_to_center(cloc2)
        if w1 < w2:
            return cloc1
        return cloc2
    
    # Return the direction order by the ones that get us closer to the center first.
    def get_locs_sorted_by_center(self, floc):
        elements=[]
        for fdir in self._DIRS:
            tloc = self.get_loc(floc, fdir)
            elements.append((tloc, self.get_dist_to_center(tloc)))
        eles_sorted=sorted(elements, key=lambda ele: ele[1])
        locs_sorted=[]
        for ele in eles_sorted:
            locs_sorted.append(ele[0])
        return locs_sorted
    
    def get_dist_to_center(self, loc):
        if not loc in self._DIST_TO_CENTER.keys():
            self._DIST_TO_CENTER[loc] = rg.dist(loc, rg.CENTER_POINT)
        return self._DIST_TO_CENTER[loc]

    def get_loc(self, loc, tdir):
        return (loc[0] + tdir[0], loc[1] + tdir[1])

    def get_hp_weight(self, hp):
        return rg.settings.robot_hp - hp

    def get_dist_to_closest_enemy(self, floc):
        best = 1000
        for eloc in self._ENEMIES:
            dist = rg.dist(floc, eloc)
            if dist < best:
                best = dist
        return best
    
    def has_robot(self, floc):
        return floc in self._game.robots

    def is_friendly(self, floc):
        return floc in self._FRIENDLIES

    def is_enemy(self, floc):
        return floc in self._ENEMIES

    def is_normal(self, loc):
        types = rg.loc_types(loc)
        if 'obstacle' in types or 'invalid' in types:
            return False
        return 'normal' in types

    def is_spawn(self, loc):
        return 'spawn' in rg.loc_types(loc)

    def is_occupied(self, loc):
        return loc in self._OCCUPIED or loc in self._NEW_MOVES or loc in self._ENEMIES

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
    
    # Return all locations without any adjacent enemies
    def get_safest_adj_locs(self, floc):
        locs=[]
        for tdir in self._DIRS:
            tloc = self.get_loc(floc, tdir)
            if not self.is_enemy(tloc) and self.is_normal(tloc):
                adj = self.AdjEnemyMap(self, tloc)
                if adj.get_count() == 0:
                    locs.append(tloc)
        return locs
       
