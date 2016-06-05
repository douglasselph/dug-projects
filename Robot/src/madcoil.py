#
# Adjacents:
#   If more than one enemy, escape towards nearest friendly.
#   If one enemy:
#      If too weak: feint towards center.
#  	   If blocked, go any direction.
#      If supported, move into attacker
#      If not supported, attack if not too weak.
#      If too weak, feint towards center.
#
# Diagonals:
#   If 3 or more diagonal enemy, escape.
#   If 2 diagonal enemy on opposite corners, escape.
#   If 2 diagonal enemy on one side:
#	If enemy is inside, feint towards:
#	    nearest friendly, or
#	    towards center
#       If enemy is outside:
#	    attack between.
#   If 1 diagonal,
#	If enemy is inside:
#	    If only one square open, pounce on that one.
#	    Otherwise, if both open, scan for least enemy populated one, and go there.
#	    If both equal, choose one closer to center.
#	If enemy is outside:
#	    If both open, guess attack
#    	    If one has friendly, pounce on other one

import rg
import random
import string
from twisted.test.test_plugin import AdjacentPackageTests
from fcntl import flock
from matplotlib.pyplot import flag
from __builtin__ import True
from gnome_sudoku.gnome_sudoku import SafeStdout
from rgkit.rg import locs_around
from chardet.test import count

class Robot:

   class AdjEnemyMap:

        def __init__(self, parent, tloc):
            self._adj = []
            self._parent = parent
            self._tloc = tloc
            self._surrounded=True
            
            for tdir in parent._DIRS:
                dloc = parent.get_loc(tloc, tdir)
                if parent.is_enemy(dloc):
                    self._adj.append(dloc)
                elif self._parent.is_normal(dloc):
                    self._surrounded=False

        def get_count(self):
            return len(self._adj)
        
        def get_loc(self):
            return self._tloc
        
        def get_enemies(self):
            return self._adj

        def is_surrounded(self):
            return self._surrounded
    
    #
    # Class to manage a dodging robot.
    # Usage:
    #    Call consider() function to prepare a robot to be moved.
    #    Call various select_*() functions to choose the type of movement to consider.
    #    Will return False if there was none.
    #    Call apply() to actually affect the moving map to robots.
    #
    class Dodge:
        
        def __init__(self, parent):
            self._parent = parent
            self._is_enemy={}
            self._is_spawn={}
            self._is_friend={}
            self._is_normal={}
            self._moving={}
            self._to={}
            self._alternates={}
            self._safe_only=False
        
        def set_safe_only(self, flag):
            self._safe_only=flag
             
        def consider(self, floc):
            for tdir in self._parent._DIRS:
                tloc=self._parent.get_dir(floc, tdir)
                if self._parent.is_moving_into(tloc) or self._parent.is_stationary(tloc):
                    continue
                if self._parent.is_enemy(tloc):
                    self._parent.dict_add(self._is_enemy, floc, tloc)
                elif self._parent.is_friendly(tloc):
                    self._parent.dict_add(self._is_friend, floc, tloc)
                if self._parent.is_spawn(tloc):
                    self._parent.dict_add(self._is_spawn, floc, tloc)
                elif self._parent.is_normal(tloc):
                    self._parent.dict_add(self._is_normal, floc, tloc)
        
        # Private
        # Register a move from the 'floc' to the 'tloc'.
        def _move(self, floc, tloc):
            self._moving[floc]=tloc
            # Key: TO location, Value: list of from locations
            self._parent.dict_add(self._to, tloc, floc)
        
        def _score_alternative(self, floc, locs):
            self._alternates[floc]=locs
            
        # Private
        def _select(self, floc, locs):
            safe=[]
            safest=[]
            safest_count=5
            for tloc in locs:
                adj = self._parent.AdjEnemyMap(self._parent, tloc)
                if adj.get_count() == 0:
                    safe.append(tloc)
                elif adj.get_count() < safest_count:
                    safest=[tloc]
                    safest_count=adj.get_count()
                elif adj.get_count() == safest_count:
                    safest.append(tloc)
                    safest_count=adj.get_count()
            if len(safe) > 1:
                locs=self._parent.get_locs_sorted_by_center_list(safe)
                self._move(floc, locs[0])
                self._score_alternates(floc, locs[1:])
                return True
            elif len(safe) == 1:
                self._move(floc, safe[0])
                return True
            
            if not self._safe_only and len(safest) > 0:
                if len(safest) == 1:
                    self._move(floc, safest[0])
                else:
                    locs=self._parent.get_locs_sorted_by_center_list(safest)
                    self._move(floc, safest[0])
                    self._score_alternates(floc, safest[1:])
                return True
            return False
    
        def select(self):
            self.set_safe_only(True)
            self.select_normal()
            self.select_friend()
            
            if self._STC2:
                self.select_spawn()
            
            self.set_safe_only(False)
            self.select_normal()
            self.select_friend()
            self.select_enemy()
            self.resolve_collisions()

        # Public:      
        # Move all considering robots to an open square.
        # SafeOnly: Anything  next to an enemy will not be considered.
        def select_normal(self):
            for floc in self._is_normal.keys():
                if not floc in self._moving.keys():
                    self._select(floc, self._is_normal[floc])
        
        # Public: 
        # Move all considering robots to a spawn square, if any
        # SafeOnly: Anything  next to an enemy will not be considered.
        def select_spawn(self):
            for floc in self._is_spawn.keys():
                if not floc in self._moving.keys():
                    self._select(floc, self._is_spawn[floc])

        # Move all considering robots to square with a friend in it, if any
        def select_friend(self):
            for floc in self._is_friend.keys():
                if not floc in self._moving.keys():
                    locs=self._parent.get_locs_sorted_by_center_list(self._is_friend[floc])
                    self._move(floc, locs[0])
                    if len(locs) > 1:
                        self._score_alternates(floc, locs[1:])
        
        # Move all considering robots to a square with an enemy on it, if any
        def select_enemy(self):
            for floc in self._is_enemy.keys():
                if not floc in self._moving.keys():
                    weakest=self._parent.get_weakest(self._is_enemy[floc])
                    if len(weakest) > 1:
                        locs=self._parent.get_locs_sorted_by_center_list(weakest)
                        self._move(floc, locs[0])
                        self._score_alternates(floc, locs[1:])
                    elif len(weakest) == 1:
                        self._move(floc, weakest[0])
                        
        # Resolve collisions
        # Any robot moving into the same square, well, only one of them gets to do it.
        # Choose the one with the fewest alternates. If tied, then select the weakest robot
        # to get the choice.
        def resolve_collisions(self):
            tlocs=self._to.keys()
            for tloc in tlocs:
                if len(self._to[tloc]) > 1:
                    # Choose the ones with the fewest alternates
                    select=[]
                    select_count=10
                    for floc in self._to[tloc]:
                        if floc in self._alternates.keys():
                            count=len(self._alternates[floc])
                        else:
                            count=0
                        if count < select_count:
                            select_count=count
                            select=[floc]
                        elif count == select_count:
                            select.append(floc)
                    
                    # From these choose the weakest.
                    if len(select) > 1:
                        remaining=list(select)
                        select=[]
                        select_hp=60
                        for floc in remaining:
                            fbot=self._parent._game[floc]
                            if fbot.hp < select_hp:
                                select_hp = fbot.hp
                                select=[floc]
                            elif fbot.hp == select_hp:
                                select.append(floc)
                                
                    # From these just choose the first.
                    if len(select) > 0:
                        selected=select[0] 
                        # Everyone else must select their alternative
                        for floc in self._to[tloc]:
                           if floc != selected:
                               if floc in self._alternates.keys():
                                   tloc2=self._alternates[floc][0]
                                   self._move(floc, tloc2)
                                   self._alternates[floc].remove(tloc2)
                        self._to[tloc]=[selected]
                    
        # Apply all moves in the passed dictionary map.
        def apply(self):
            for floc in self._move.keys():
                self._parent.apply_move(floc, self._move[floc])

    class Attack(self, floc, attacking):
        
    # Analysis of an enemy.
    # This will ultimately define the coil groups.
    class TargetMap:
        
        def __init__(self, parent, eloc):
            self._parent = parent
            self._eloc = eloc
            # Adjacent friendly
            self._friendly1 = []
            # Friendlies 2 walking distance away
            self._friendly2 = []
            # Friendlies 3 walking distance away
            self._friendly3 = []
            # Friendlies 4 or 5 away
            self._friendly5 = []
            # Adjacent square code analysis. Key=DIR, Value=tuple(code, optional count)
            self._adjacents = {}
            
            for bloc in self._parent._REMAINING:
                wdist = rg.wdist(bloc, self._eloc)
                if wdist == 1:
                    self._friendly1.append(bloc)
                elif wdist == 2:
                    self._friendly2.append(bloc)
                elif wdist == 3:
                    self._friendly3.append(bloc):
                elif wdist <= 5:
                    self._friendly5.append(bloc)
                    
        def has_friendly(self, floc):
            return floc in self._friendly1 + self._friendly2 + self._friendly3 + self._friendly5
        
        def has_friendly1(self, floc):
            return floc in self._friendly1
        
        def has_friendly2(self, floc):
            return floc in self._friendly2

        def has_friendly3(self, floc):
            return floc in self._friendly3
        
        def has_friendly5(self, floc):
            return floc in self._friendly5

        def has_friendly_within3(self, floc):
            return floc in self._friendly1 or floc in self._friendly2 or floc in self._friendly3
        
        def get_friendly1(self):
            return self._friendly1
        
        def has_friendly1(self):
            return len(self._friendly1) > 0
        
        def get_count1(self):
            return len(self._friendly1)
     
        # Return the number of friendlies within 2 or 3
        def get_count23(self):
            return len(self._friendly2 + self._friendly3)
        
         # Return the number of friendlies within 3
        def get_count3(self):
            return len(self._friendly1 + self._friendly2 + self._friendly3)
       
        def get_eloc(self):
            return self._eloc
        
        def remove(self, floc):
            if self._hasfriendly1(floc):
                self._friendly1.remove(floc)
            if self._hasfriendly2(floc):
                self._friendly2.remove(floc)
            if self._hasfriendly3(floc):
                self._friendly3.remove(floc)
            if self._hasfriendly5(floc):
                self._friendly5.remove(floc)
                
    # Size of the GAME MAP.
    _MAPSIZE = 19
    # Suicide damage
    _SUICIDE_DAMAGE = 15
    # Min attack damage
    _MIN_ATTACK_DAMAGE=8
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
             (1, 1) :[(1, 0), (0, 1)], \
             (-1, 1):[(-1, 0), (0, 1)], \
             (-1, -1):[(-1, 0), (0, -1)]}

    # Spawn turn coming: True if new robots are to be spawned next turn
    _STC1 = False
    # Spawn turn coming: True if new robots in two turns
    _STC2 = False
    # List of commands to issue for each robot, keyed by location. The value is the command. 
    _CMDS = {}
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
    # This list holds robots which have to move. Key: robot which needs to move, Value: the robot reason.
    _MAKE_WAY = {}
    
    # CODES
    _COCE_OPEN=0
    _CODE_ENEMY=1
    _CODE_EDGE=2
    _CODE_FRIEND=3
    _CODE_NEAREST_FRIEND=4
    _CODE_NEAREST_EDGE=5
    
    # DEBUG
    _LOG = False
    _LOOKAT = [(13,6),(13,5),(12,5),(14,5),(15,4)]
    _DEBUG_TURNS = [14]
    
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
            self.ponder_on_spawned()
            self.ponder_adj_enemies()
            self.ponder_assign_targets()
            
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
        self._STC1 = (remainder == 0)
        self._STC2 = (remainder == (rg.settings.spawn_every - 1))

        # Compute friendly and enemy robot locations (Convenience)
        self._FRIENDLIES = []
        self._ENEMIES = []
        self._MAKE_WAY = {}
        self._CMDS = {}
        
        for loc in self._game.robots.keys():
            bot = self._game.robots[loc]
            if bot.player_id == self.player_id:
                self._FRIENDLIES.append(loc)
            else:
                self._ENEMIES.append(loc)
        
        self._REMAINING=list(self._FRIENDLIES)
        
        return True
 
     # 
     # ponder functions
     #
     
     # If a robot is on a spawn square and the STC is next turn, then they need to move off.
     def ponder_on_spawned(self):

        if self._STC1 or self._STC2:            
            dodge=self.Dodge(this)
            
            for floc in self._REMAINING:
                if self.is_spawn(floc):
                    dodge.consider(floc)
            
            dodge.select()
            dodge.apply()
            self.ponder_make_way()
    
    def ponder_make_way(self):
        
        # Infinite loop safety:
        counter=0
        while len(self._MAKE_WAY.keys()) > 0 and counter < 20: 
            dodge=self.Dodge(this)
            for floc in self._MAKE_WAY.keys():
                dodge.consider(floc)
            self._MAKE_WAY={}
            dodge.select()
            dodge.apply()
            counter += 1

    # Deal with robots surrounded by too many enemies.
    # If other friendlies are next to one of the robots attack.
    # If we are alone and low on health then suicide. Low chance.
    # Guard if we can survive.
    def ponder_adj_enemies(self):
        
        suicides=[]
        dodge=self.Dodge(this)
        
        for floc in self._REMAINING:
            fadj = self.AdjEnemyMap(this, floc)
            fbot = self._game.robots[floc]
            if fadj.is_surrounded():
                min_damage = self._MIN_ATTACK_DAMAGE * fadj.get_count()
                if min_damage >= fbot.hp:
                    suicides.append(floc)
                    continue
            elif fadj.get_count() > 1:
                dodge.consider(floc)
      
        for floc in suicides:
            self.apply_suicide(floc)
            
        dodge.select()
        dodge.apply()
        
        self.ponder_make_way()
  
    # Assign the given floc to the given target map
    def assign_tmap(self, to_tmap, floc):
        for eloc in self._TGTMAP.keys():
            tmap=self._TGTMAP[eloc]
            if tmap != to_tmap:
                tmap.remove(floc)
        self._ASSIGNED[floc]=tmap

    # 
    # Each friendly should be assigned to exactly one target map
    # At this point we should not have friendlies with more than one adjacent enemy
    #
    def ponder_assign_targets(self):
    
        for eloc in self._ENEMIES:
            self._TGTMAP[eloc] = self.TargetMap(this, eloc)
        
        assign={}        
        # First, since all friendlies with more than one adjacent enemy has been dealt with,
        # the rest should have exactly one. For these select them to be part of the logical
        # adjacent target map if they have enough support. They may end up attacking or 
        # if we are not ready move away. That will be dealt with later.
        for tmap in self._TGTMAP.keys():
            if tmap.has_friendly1():
                if tmap.get_count1() > 1:
                    print "ERROR: target map ", tmap.get_eloc() + " has ", tmap.get_count1(), " unassigned friendlies."
                if tmap.get_count23() >= 3:
                    # This tmap has enough potential support.
                    floc=tmap.get_friendly1()[0]
                    assign[floc]=tmap
        
        for floc in assign.keys():
            self.assign_tmap(assign[floc], floc)
        
        # Now deal with 2 away players, seeing if they have enough support.
    # 
    # CMD SUPPORT 
    #
    def get_cmd(self):
        if self.location in self._CMDS.keys():
            return self._CMDS[self.location]
        return ['guard']
    
    def apply_move(self, floc, tloc):
        self._CMDS[floc] = ['move', tloc]
        if floc in self._REMAINING:
            self._REMAINING.remove(floc)
        else:
            print "ERROR: location ", floc, " has already moved (not in REMAINING)"
            
        if self.is_friendly(tloc) and tloc in self._REMAINING:
            self._MAKE_WAY[tloc] = floc

    def apply_attack(self, floc, tloc):
        self._CMDS[floc] = ['attack', tloc]
        self._REMAINING.remove(floc)

    def apply_suicide(self, floc):
        self._CMDS[floc] = ['suicide']
        self._REMAINING.remove(floc)

    def apply_guard(self, floc):
        self._CMDS[floc] = ['guard']
        self._REMAINING.remove(floc)
            
    # 
    # SUPPORT FUNCTIONS
    #
    
    # Return True if floc can reach tloc by some safe path.
    # That is, something not obstructed by enemies or adjacent enemies.
    # Friendlies are ok.
    def can_reach_safely(self, floc, tloc):
        if floc == tloc:
            return True
        dirs = self.get_dirs(floc, tloc)
        ok=[]
        for dir in dirs:
            nloc = self.get_loc(floc, dir)
            if nloc == tloc:
                return True
            if self.is_enemy(nloc):
                continue
            near_enemy=False
            for adir in self._DIRS:
                aloc = self.get_loc(nloc, adir)
                if aloc != tloc and nloc != floc and self.is_enemy(aloc):
                    near_enemy=True
                    break
            if near_enemy:
                continue
            ok.append(nloc)
            
        for nloc in ok:
            if self.can_reach(nloc, tloc):
                return True
        return False
        
    def dict_add(self, dict, key, val):
        if key in dict.keys():
            dict[key].append(val)
        else:
            dict[key] = [val]
            
    # Return closest robot from passed in list to passed in location
    def get_closest(self, tloc, robots):
        closest_dist = 1000
        closest_floc = None
        for rloc in robots:
            dist = rg.dist(rloc, tloc)
            if dist < closest_dist:
                closest_dist = dist
                closest_rloc = floc
        return closest_floc

    # Return list of directions from floc to tloc.
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
 
        return [(ndx, 0), (0, ndy)]

    def get_loc(self, loc, tdir):
        return (loc[0] + tdir[0], loc[1] + tdir[1])

    # Return the locations ordered by the ones that get us closer to the center first.
    def get_locs_sorted_by_center_list(self, locs):
        elements=[]
        for tloc in locs:
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

    # Return locations holding the robots with the lowest HP.
    def get_weakest(self, locs):
        weakest=[]
        for loc in locs:
            if loc in self._game.robots.keys():
                if len(weakest) == 0:
                    weakest.append(loc)
                else:
                    if self._game.robots[loc].hp < self._game.robots[weakest[0]].hp:
                        weakest=[loc]
                    elif self._game.robots[loc].hp == self._game.robots[weakest[0]].hp:
                        weakest.append(loc)
        return weakest
    
    def can_dodge(self, floc):
        for tdir in self._DIRS:
            tloc = self.get_loc(floc, tdir)
            if self.is_normal(tloc) and not self.is_enemy(tloc):
                return True
        return False
    
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

    # Return true if someone is moving into tloc
    def is_moving_into(self, tloc):
        for floc in self._CMDS.keys():
            cmd = self._CMDS[floc]
            if cmd[0] == 'move' and cmd[1] == tloc:
                return True
        return False

    # Return true if the robot is already going to stay where it is
    def is_stationary(self, floc):
        if floc in self._CMDS.keys():
            cmd = self._CMDS[floc]
            return cmd[0] != 'move'
        return False
    
    # Return True if there is a safe walking around from floc to tloc
    def has_safe_route(self, floc, tloc):
             