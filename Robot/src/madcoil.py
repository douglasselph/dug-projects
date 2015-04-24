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

class Robot:

   class AdjEnemyMap:

        def __init__(self, parent, tloc):
            self._adj = []
            self._parent = parent
            self._tloc = tloc
            
            for tdir in parent._DIRS:
                dloc = parent.get_loc(tloc, tdir)
                if parent.is_enemy(dloc):
                    self._adj.append(dloc)

        def get_count(self):
            return len(self._adj)
        
        def get_loc(self):
            return self._tloc

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
            self.ponder_make_way()
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
            
            moving={}
            is_enemy={}
            is_spawn={}
            is_friend={}
            
            for floc in self._REMAINING:
                if self.is_spawn(floc):
                    for tdir in self._DIRS:
                        tloc=self.get_dir(floc, tdir)
                        if self.is_enemy(tloc):
                            self.dict_add(is_enemy, floc, tloc)
                        elif self.is_friendly(tloc):
                            self.dict_add(is_friend, floc, tloc)
                        elif self.is_spawn(tloc):
                            self.dict_add(is_spawn, floc, tloc)
                        elif self.is_normal(tloc):
                            self.dict_add(moving, floc, tloc)
                        
                    if not floc in moving.keys():
                        if floc in is_friend.keys():
                            moving[floc]=is_friend[floc][0]
                        if floc in is_spawn.keys():
                            moving[floc]=is_spawn[floc][0]
                        elif floc in is_enemy.keys():
                            moving[floc]=is_enemy[floc][0]
            
            self.select_safest(moving)
    
    def ponder_make_way(self):
        
        while len(self._MAKE_WAY.keys()) > 0: 
            moving={}
            is_enemy={}
            is_friend={}
            counter=0
            for floc in self._MAKE_WAY.keys():
                for tdir in self._DIRS:
                    tloc=self.get_dir(floc, tdir)
                    if self.is_enemy(tloc):
                        self.dict_add(is_enemy, floc, tloc)
                    elif self.is_friendly(tloc):
                        self.dict_add(is_friend, floc, tloc)
                    elif self.is_normal(tloc):
                        self.dict_add(moving, floc, tloc)

            self._MAKE_WAY={}
            self.select_safest(moving)
            
            # Infinite loop safety:
            counter += 1
            if counter > 20:
                print "INFINITE LOOP ERROR!"
                break
    
    # 
    # Each friendly should be assigned to exactly one target map
    #
    def ponder_assign_targets(self):
    
        for eloc in self._ENEMIES:
            self._TGTMAP[eloc] = self.TargetMap(this, eloc)
        
        _ASSIGNED={}
        
        mapany={}
        map1={}
        map2={}
        map3={}
        map5={}
        
        for floc in self._REMAINING:
            for tmap in self._TGTMAP.keys():
                if tmap.has_friendly(floc):
                    self.dict_add(mapany, floc, tmap)
                if tmap.has_friendly1(floc):
                    self.dict_add(map1, floc, tmap)
                if tmap.has_friendly2(floc):
                    self.dict_add(map2, floc, tmap)
                if tmap.has_friendly3(floc):
                    self.dict_add(map3, floc, tmap)
                if tmap.has_friendly5(floc):
                    self.dict_add(map5, floc, tmap)


        
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

    def apply_suicide(self, loc):
        self._CMDS[loc] = ['suicide']
        self._REMAINING.remove(loc)

    def apply_guard(self, loc):
        self._CMDS[loc] = ['guard']
        self._REMAINING.remove(loc)


    # Move to safest square
    def select_safest(self, moving):
        for floc in moving.keys():
            # Select safest square if more than one.
            if len(moving[floc]) > 1:
                safest=None
                for tloc in moving[floc]:
                    if not self.is_moving_into(tloc):
                        adj = self.AdjEnemyMap(self, tloc)
                        if safest == None:
                            safest=adj
                        elif adj.get_count() < safest.get_count():
                            safest=adj
                if adj != None:
                    self.apply_move(floc, safest.get_loc())
            else:
                tloc=moving[floc][0]
                if not self.is_moving_into(tloc):
                    self.apply_move(floc, safest.get_loc())
  
    # 
    # SUPPORT FUNCTIONS
    #
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

    def get_loc(self, loc, tdir):
        return (loc[0] + tdir[0], loc[1] + tdir[1])

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
