# Humpty Dumpty by WormDog (Douglas Selph)

# On Spawn Corner:
#   Move off, toward best F.
#   If Blocked: Other direction.
#   If Blocked: towards best F.
#   If Blocked: any open.
# On Spawn Side:
#   Move off
#   If Blocked: feint towards best F.
# F/E Alone dist 3:
#   Move towards best F. 
# F Alone 3:
#   Surrounded: suicide
#   3 Adj E:
#   Open: If safe, move, otherwise suicide.
#   2 Adj E:
#       I  f both open safe, move towards best F.
#       If 1 safe open, move towards that.
#       If no safe open, suicide.
#   1 Adj E:
#       If stronger and healthy, attack.
#       Otherwise safe move towards best F.
#   4 Diag E: Guard
#   2,3 Diag E: 
#       Safe move towards best F.
#   1 Diag E:
#       If Recall was MOVE and healthy, then guess attack.
#       Otherwise safe move towards best F.
#   1+ E 2,3 :
#       Safe move towards best F.
# 1+ F 2+:
#   1 E 1,2:
#       If 1 F then call other to come
#           If other is busy with another E, then move strategically. Perhaps coordinated move for both such that both F’s are now closer.
#           Otherwise if E Adj, attack.
#           Otherwise guard and wait for other F to arrive.
#       If 2+ F group is busy, move toward group.
#       If 2+ F group is moving toward E, then if HP OK:
#           Attack E if Adj
#           Move strategically depending on location of other group.
#           Or guard if not necessary.
#       If LOW HP, run-away.
#           If unable to do so safely, suicide if Adj. Else Guard.
#   No E within 2:
#       Move toward other group strategically, forming wall across closest E group.
#
# 1+ F 1: (i.e. in group)
#   1+ Adj E: If healthy, attack shared E. Otherwise run-away/suicide.
#   1+ Diag E: Move towards shared E.
#   Otherwise group move towards closest least dense E.
#
# Select Best F:
#   Find closest F
#   If more than one F closest (same distance, or near same distance):
#       Move towards the one needing more support (E to F ratio not good).
#
# Safe Move: (move toward chosen F notes)
#   If diagonal, choose:
#       If one direction has high E density choose the other.
#       Otherwise the leg whose result is closer to center.
#
# F Groups:
#   During data collection stage identify all F’s that are 1 away from each
#   other and assign them to a group #. Lone F’s get assigned 0.
#
# Recall:
#   When recall is requested:
#   Data collection: For shared diagonal squares see if E in question is the
#     only that can move there. If so, store E and diagonal locations. Next turn
#     check to see if these are now occupied with E. If so, store preference MOVE.
#   Otherwise store preference ATTACK.
#
#   Return the most common computed. First time should be returned as UNKNOWN.

class Robot:

    #
    # Analyze all the immediately adjacent elements next to a given robot.
    #
    class MoveMap:

        def __init__(self, parent, tloc):
            self._parent = parent
            self._loc = tloc            

    # Compute the danger score next to the given square.
    class SafeMap:

        def __init__(self, parent):

            self._parent = parent
            self._score = {}
                    
        score = 0

        for tdir in self._parent._DIRS:
            dloc = self._parent.get_loc(tloc, tdir)
            if self._parent.is_enemy(dloc):
                score = score + 1

        return score
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
    # 2 squares out
    _MOVE2 = [(-2, -2), (-1, -2), (0, -2), (1, -2), (2, -2), \
              (-2, -1), (2, -1), \
              (-2, 0), (2, 0), \
              (-2, 1), (2, 1), \
              (-2, 2), (-1, 2), (0, 2), (1, 2), (2, 2)]
    # 3 squares out
    _MOVE3 = [(-3, -3), (-2, -3), (-1, -3), (0, -3), (1, -3), (2, -3), (3, -3), \
              (-3, -2), (3, -2), \
              (-3, -1), (3, -1), \
              (-3, 0), (3, 0), \
              (-3, 1), (3, 1), \
              (-3, 2), (3, 2), \
              (-3, 3), (-2, 3), (-1, 3), (0, 3), (1, 3), (2, 3), (3, 3)]

    # List of commands to issue for each robot, keyed by location. The value is the command. 
    _CMDS = {}
    # Convenience: list of locations containing friendly robots
    _FRIENDLIES = []
    # Convenience: list of locations containing enemy robots
    _ENEMIES = []
    # Convenience: list of location containing friendly units that still need moves.
    _REMAINING = []
    # Save danger score: number of enemies adjacent to the given location
    _SCORE = {}
    # To save computing power remember distances to center
    _DIST_TO_CENTER = {}
    # This list holds robots which have to move. Key: robot which needs to move, Value: the robot reason.
    _MAKE_WAY = {}
    # Spawn turn coming: True if new robots are to be spawned next turn
    _STC1 = False
    # Spawn turn coming: True if new robots in two turns
    _STC2 = False
    
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
            
        cmd = self.get_cmd()
        if self._LOG:
            print "ROBOT ", self.robot_id, "@", self.location, ":", cmd
            
        return cmd

    def init(self):
        if self._CURTURN == self._game.turn:
            return False

        # Compute Spawn Turn Coming
        remainder = self._game.turn % rg.settings.spawn_every
        self._STC1 = (remainder == 0)
        self._STC2 = (remainder == (rg.settings.spawn_every - 1))

        self._FRIENDLIES = []
        self._ENEMIES = []
        self._MAKE_WAY = {}
        self._CMDS = {}
        self._SCORE = {}
        
        for loc in self._game.robots.keys():
            bot = self._game.robots[loc]
            if bot.player_id == self.player_id:
                self._FRIENDLIES.append(loc)
            else:
                self._ENEMIES.append(loc)
        
        self._REMAINING=list(self._FRIENDLIES)

        return True
        
    # Original Idea:
    #   On Spawn Corner:
    #     Move off, toward best F.
    #       If Blocked: Other direction.
    #       If Blocked: towards best F.
    #       If Blocked: any open.
    #   On Spawn Side:
    #       Move off
    #       If Blocked: feint towards best F.
    #
    # Current:
    #    Get possible locations sorted by
    #       safest, non-spawn.
    def ponder_on_spawned(self):
        remaining=list(self._REMAINING)
        for floc in remaining:
            if self.is_spawn(floc):
                self.move_off_spawn(floc)
               
    # Move the indicated robot off it's spawn square.
    # Will first check for safest non-spawn location nearest a friendly.
    # Otherwise if not a spawn turn coming, will select any safe square.
    # Otherwise will consider the safest square.
    def move_off_spawn(self, floc):
        # All possible locations not near enemy nor on spawn.
        open_locs = self.get_safe_locs_no_spawn(floc)
        if self.apply_best_move(floc, open_locs):
            return True
        
        if not self._STC1 and not self._STC2:
            # All possible locations not near enemy, spawn ok
            open_locs = self.get_safe_locs(floc)
            if self.apply_best_move(floc, open_locs):
                return True
        
        all_locs = self.get_open_locs(floc)
        return self.apply_best_move(floc, open_locs)

    # Select the move for the given robot from the list. If there are no locs in open_locs, return false.
    # If there are more than one, select the one nearer to a friendly.
    def apply_best_move(self, floc, open_locs):
        if len(open_locs) == 0:
            return False
        if len(open_locs) == 1:
            self.apply_move(floc, open_locs[0])
        else:
            # Choose location heading toward closest F
            loc=self.select_closest_friendly_loc(floc, open_locs)
            self.apply_move(floc, loc)
        return True

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


    # Return the list of open move spots from the given location.
    # Return sorted by the safest.
    def get_open_locs(self, tloc):
        elements = []
        for tdir in self._DIRS:
            dloc = self.get_loc(tloc, tdir)
            if self.is_normal(dloc):
                elements.append((dloc, self.get_danger_score(dloc)))

        esorted=sorted(elements, key=lamda ele: ele[1])
        lsorted=[]
        for ele in esorted:
            lsorted.append(ele[0])
        return lsorted

    # Return the list of open move spots from the given location that
    # have no adjacent enemies and is not a spawn square.
    def get_safe_locs_no_spawn(self, tloc):
        locs = []
        
        for tdir in self._DIRS:
            dloc = self.get_loc(tloc, tdir)
            if self.is_normal(dloc) and not self.is_enemy(dloc) and not self.is_spawn(dloc) and self.get_danger_score(dloc) == 0:
                locs.append(dloc)

        return locs

    # Return the list of open move spots from the given location that
    # have no adjacent enemies.
    def get_safe_locs(self, tloc):
        locs = []
        
        for tdir in self._DIRS:
            dloc = self.get_loc(tloc, tdir)
            if self.is_normal(dloc) and not self.is_enemy(dloc) and self.get_danger_score(dloc) == 0:
                locs.append(dloc)

        return locs

    # Return the loc from 'locs' that is nearest to a friendly from 'floc'.
    def select_closest_friendly_loc(self, floc, locs):
        closest_dist = 1000
        closest_loc = None
        for loc in locs:
            dist = rg.dist(loc, floc)
            if dist < closest_dist:
                closest_dist = dist
                closest_loc = loc
        return closest_loc

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

    # Return how many enemies are adjacent to the given location.
    def get_danger_score(self, tloc):

        if tloc not in self._SCORE.keys():
            score = 0

            for tdir in self._parent._DIRS:
                dloc = self._parent.get_loc(tloc, tdir)
                if self._parent.is_enemy(dloc):
                    score = score + 1

            self._SCORE[tloc] = score

        return self._SCORE[tloc]

