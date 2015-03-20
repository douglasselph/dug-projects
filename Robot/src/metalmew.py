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

        def __init__(self, parent, game):
            self._from={}
            self._to={}
            self._chk_fail=[]
            self._failed=[]
            self._dangerous_ok=False
            self._empty_only=False
            self._game=game
            self._parent=parent

        def clear(self):
            self._from={}
            self._to={}
            self._chk_fail=[]

        # Move the robot to any normal space.
        def move(self, loc):
            bot=self._game.robots[loc]
            for dir in self._parent._DIRS:
                dloc=self._parent.get_loc(bot.location, dir)
                self.chk_add(loc, dloc)

        def chk_add(self, floc, tloc):
            if self.is_ok(floc, tloc):
                self.add(floc, tloc)
            elif not floc in self._chk_fail:
                self._chk_fail.append(floc)

        def is_ok(self, floc, loc):
            if not self._parent.is_normal(loc): 
                return False
            if loc in self._parent._OCCUPIED or loc in self._parent._NEW_MOVES:
                return False
            if self._parent._STC and self._parent.is_spawn(loc) and not self._parent.is_spawn(floc):
                return False
            return True

        def set_empty_only(self, flag):
            self._empty_only=flag

        def set_dangerous_ok(self, flag):
            self._dangerous_ok=flag

        def has(self, floc):
            return floc in self._from.keys()

        def add_to_failed(self, floc):
            self._failed.append(floc)

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
                self._to[tloc]=[floc]

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

        # Once all moves for all robots are entered, apply the moves toward those robots.
        def apply(self):
            self.detect_problems()
            self.do_moves()

        def detect_problems(self):
            self.detect_collisions()
            self.detect_occupied()
            self.detect_bumps()

        def show(self, prefix):
            line=prefix + " FROM:"
            for floc in self._from.keys():
                line += "[" + str(floc) + ":" 
                line += str(self._from[floc])
                line += "];"
            line+=" TO:"
            for tloc in self._to.keys():
                line += "[" + str(tloc) + ":" 
                line += str(self._to[tloc])
                line += "];"
            return line

        # If more than one robot wants to move to the same square, resolve the conflict
        # by letting the robot with fewer choices have that square. The other robot
        # will select another square. In the case of ties, it is possible a robot
        # will be left with no place to go. Oh well. That's better than 2 robots bumping.
        def detect_collisions(self):
            for tloc in self._to.keys():
                if len(self._to[tloc]) > 1:
                    least_floc=None
                    least_count=1000
                    tolist=self._to[tloc]
                    for floc in tolist:
                        count = len(self._from[floc])
                        if count < least_count:
                            least_floc = floc
                            least_count = count
                    self.only(least_floc, tloc)

        # See if two robots are moving into each other. If so, 
        # then give priority to the lower HP one.
        def detect_bumps(self):
            collides=[]
            for floc in self._from.keys():
                for tloc in self._from[floc]:
                    if tloc in self._from.keys():
                        for tloc2 in self._from[tloc]:
                            if tloc2 == floc:
                                inList=False
                                for collide in collides:
                                    if collide[0] == floc and collide[1] == tloc:
                                        inList=True
                                        break
                                    if collide[1] == floc and collide[0] == tloc:
                                        inList=True
                                        break
                                if not inList:
                                    list=[]
                                    list.append(floc)
                                    list.append(tloc)
                                    collides.append(list)
            if len(collides) > 0:
                for collide in collides:
                    floc=collide[0]
                    floc2=collide[1]
                    if self._game.robots[floc].hp < self._game.robots[floc2].hp:
                        self.remove(floc2, floc)
                    else:
                        self.remove(floc, floc2)

        # If a robot has a choice and one the squares he could move to is occupied
        # with another robot (friendly or enemy), then choose the vacant square.
        def detect_occupied(self):
            for floc in self._from.keys():
                if len(self._from[floc]) > 1:
                    occupied=[]
                    for tloc in self._from[floc]:
                        if self._parent.has_robot(self._game, tloc):
                            occupied.append(tloc)
                    if occupied > 0 and len(occupied) < len(self._from[floc]):
                        for tloc in occupied:
                            self._from[floc].remove(tloc)
                            if floc in self._to[tloc]:
                                self._to[tloc].remove(floc)

        # Return True if a location is too dangerous for a robot to move into
        def too_dangerous(self, floc, tloc):
            bot=self._game.robots[floc]
            adj=self._parent.AdjacentMap(self._parent, self._game, tloc) 
            if adj.count() > 0:
                strength=0
                remove=False
                for ebot in adj.get_bots():
                    if ebot.hp >= bot.hp:
                        return True
                    strength+= ebot.hp
                if strength >= bot.hp:
                    return True
            return False
            
        # Return dictionary of moves we have computed.
        # The key is from, the value is the target
        def get_moves(self):
            moves={}
            for floc in self._from.keys():
                count=len(self._from[floc])
                if count > 1:
                    tloc=self.select_move(floc)
                elif count == 1:
                    tloc=self._from[floc][0]
                else: 
                    if not floc in self._failed:
                        self._failed.append(floc)
                    tloc=None
                if tloc != None:
                    moves[floc]=tloc

            for floc in self._chk_fail:
                if not floc in self._failed:
                    if not floc in moves.keys():
                        self._failed.append(floc)
                
            return moves

        # Actually apply the moves recorded
        def do_moves(self):
            moves=self.get_moves();
            for floc in moves.keys():
                self._parent.apply_move(floc, moves[floc])

        # Return a list of all non-spawn squares
        def filter_out_spawn(self, moves):
            filtered=[]
            for loc in moves:
                if not self._parent.is_spawn(loc):
                    filtered.append(loc)
            return filtered

        # Return a list of all robot free squares
        def filter_out_occupied(self, moves):
            filtered=[]
            for loc in moves:
                if not self._parent.has_robot(self._game, loc):
                    filtered.append(loc)
            return filtered

        # Return a list of all robot free squares
        def filter_out_dangerous(self, floc, moves):
            filtered=[]
            for loc in moves:
                if not self.too_dangerous(floc, loc):
                    filtered.append(loc)
            return filtered

        def select_safest(self, moves):
            best_tloc=[]
            best_count=1000
            for tloc in moves:
                adj = self._parent.AdjacentMap(self._parent, self._game, tloc)
                if adj.count() < best_count:
                    best_tloc=[tloc]
                    best_count=adj.count()
                elif adj.count() == best_count:
                    best_tloc.append(tloc)
            count=len(best_tloc)
            if count == 1:
                return best_tloc[0]
            select=random.randint(0,count-1)
            return best_tloc[select]

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
            choices=self.select_move_filter(floc, True, True, True)
            if len(choices) > 0:
                return self.select_safest(choices)
            choices=self.select_move_filter(floc, self._parent._STC, False, True)
            if len(choices) > 0:
                return self.select_safest(choices)
            if self._parent._STC:
                choices=self.select_move_filter(floc, True, False, False)
                if len(choices) > 0:
                    return self.select_safest(choices)
            return self.select_safest(self._from[floc])

        def select_move_filter(self, floc, filter_spawned, filter_occupied, filter_dangerous):
            # Choose non-spawn square if possible
            choices=list(self._from[floc])
            if filter_spawned:
                choices=self.filter_out_spawn(choices)
            if filter_occupied:
                choices=self.filter_out_occupied(choices)
            if filter_dangerous:
                choices=self.filter_out_dangerous(floc, choices)
            return choices

        def get_failed(self):
            return self._failed

    class AdjacentMap:

        def __init__(self, parent, game, loc):
            self._adj=[]
            self._weakest_loc=None
            self._weakest_hp=1000
            self._strongest_loc=None
            self._strongest_hp=0
            self._game=game
            for dir in parent._DIRS:
                dloc=parent.get_loc(loc, dir)
                if parent.has_enemy(dloc):
                    self._adj.append(dloc)
                    ebot=game.robots[dloc]
                    if ebot.hp < self._weakest_hp:
                        self._weakest_loc=dloc
                        self._weakest_hp=ebot.hp
                    if ebot.hp > self._strongest_hp:
                        self._strongest_loc=dloc
                        self._strongest_hp=ebot.hp

        def count(self):
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
            bots=[]
            for loc in self._adj:
                bots.append(self._game.robots[loc])
            return bots

    # Base class
    class AttackMap:

        def __init__(self, parent, game, loc):
            self._parent=parent
            self._game=game
            self._loc=loc
            self._dirs=[]

        def get_dirs(self):
            return self._dirs

        def get_bot(self, dir):
            loc = self._parent.get_loc(self._loc, dir)
            return self._game.robots[loc]

        def get_loc(self, dir):
            return self._parent.get_loc(self._loc, dir)

        def count(self):
            return len(self._dirs.keys())

    # Compute diagonal enemies
    class DiagonalMap(AttackMap):

        def __init__(self, parent, game, loc):
            parent.AttackMap.__init__(self, parent, game, loc)
            # Check diagonal possibilities
            for diag_dir in self._parent._DIAG:
                diag_loc=self._parent.get_loc(loc, diag_dir)
                if self._parent.has_enemy(diag_loc):
                    self._dirs.append(diag_dir)

    # Compute 2 sq orthogonal enemies
    class Ortho2sqMap(AttackMap):

        def __init__(self, parent, game, loc):
            parent.AttackMap.__init__(self, parent, game, loc)
            # Check 2sq orhtogonal possibilities:
            for dir2 in self._parent._DIR2:
                loc2=self._parent.get_loc(loc, dir2)
                if self._parent.has_enemy(loc2):
                    self._dirs.append(dir2)

    # Compute 2 sq horse move enemies
    # That is one square forward and then one square diagonal.
    class HorseMoveMap(AttackMap):

        def __init__(self, parent, game, loc):
            parent.AttackMap.__init__(self, parent, game, loc)
            # Check 2sq orhtogonal possibilities:
            for dir2 in self._parent._HORSEDIR:
                loc2=self._parent.get_loc(loc, dir2)
                if self._parent.has_enemy(loc2):
                    self._dirs.append(dir2)

    class GuessAttackMap:

        def __init__(self, parent, game, loc):

            self._parent=parent
            self._game=game
            self._loc=loc
            self._weights={}
            self._missesok=False

        def set_misses_ok(self, flag):
            self._missesok=flag

        def init(self):

            for dir in self._parent._DIRS:
                self._weights[dir]=0

            # Check diagonal possibilities
            diagMap = self._parent.DiagonalMap(self._parent, self._game, self._loc)
            for diag_dir in diagMap.get_dirs():
                for adir in self._parent._DADJ[diag_dir]:
                    tloc=diagMap.get_loc(adir)
                    if self.is_attackable(tloc):
                        ebot=diagMap.get_bot(diag_dir)
                        self.add_attack(adir, tloc, ebot)

            # Check 2sq orthogonal possibilities:
            orthoMap = self._parent.Ortho2sqMap(self._parent, self._game, self._loc)
            for dir2 in orthoMap.get_dirs():
                adir=self._parent._DADJ2[dir2]
                tloc=orthoMap.get_loc(adir)
                if self.is_attackable(tloc):
                    ebot = orthoMap.get_bot(dir2)
                    self.add_attack(adir, tloc, ebot)

        def add_attack(self, adir, tloc, ebot):
            x = rg.settings.robot_hp/2
            weight = x + self._parent.get_hp_weight(ebot.hp)
            weight-=self._parent.miss_get(self._loc, tloc)
            if self._parent.is_attacking(ebot.location):
                weight+=x
            adj = self._parent.AdjacentMap(self._parent, self._game, tloc)
            weight += adj.count() * x/2
            self._weights[adir] += weight

        def is_attackable(self, loc):
            if not self._parent.is_normal(loc):
                return False
            if loc in self._parent._FRIENDLIES:
                if not self._parent.is_moving(loc):
                    return False
            if not self._missesok:
                if self._parent.miss_get(self._loc, loc) > 0:
                    return False
            return True
            
        def get_best_attack(self):
            largest_dir=[]
            largest_size=-1
            for dir in self._weights.keys():
                if self._weights[dir] > largest_size:
                    largest_size=self._weights[dir]
                    largest_dir=[dir]
                elif self._weights[dir] == largest_size:
                    largest_dir.append(dir)
            if largest_size > 0 and len(largest_dir) > 0:
                if len(largest_dir) == 1:
                    use_dir=largest_dir[0]
                else:
                    select=random.randint(0,len(largest_dir)-1)
                    use_dir=largest_dir[select]
                tloc=self._parent.get_loc(self._loc, use_dir)
                return tloc
            return None

        def attack(self):
            tloc=self.get_best_attack()
            if tloc != None:
                self._parent.apply_guess_attack(self._loc, tloc)

    # Size of the GAME MAP.
    _MAPSIZE=19
    # Suicide damage
    _SUICIDE_DAMAGE=15
    # The turn we last processed globals
    _CURTURN=0
    # Orthogonal directions
    _DIRS=[(0,-1),(1,0),(0,1),(-1,0)]
    # Diagonal directions
    _DIAG=[(1,-1),(1,1),(-1,1),(-1,-1)]
    # 2sq orthogonal directions
    _DIR2=[(0,-2),(2,0),(0,2),(-2,0)]
    # Horse move directions
    _HORSEDIR=[(1,-2),(-1,-2),(2,-1),(2,1),(1,2),(-1,2),(-2,1),(-2,-1)]
    # Diagonal adjacents
    _DADJ = {(1,-1) :[(1,0),(0,-1)],\
             (1,1)  :[(1,0),(0,1)],\
                 (-1,1) :[(-1,0),(0,1)],\
                 (-1,-1):[(-1,0),(0,-1)]}
    # 2sq adjacent directions
    _DADJ2 = {(0,-2):(0,-1),\
                  (2,0) :(1,0),\
                  (0,2) :(0,1),\
                  (-2,0):(-1,0)}
    # 2sq fent directions
    _FEINT2 = {(0,-2):[(1,0),(-1,0)],\
                  (2,0) :[(0,1),(0,-1)],\
                  (0,2) :[(1,0),(-1,0)],\
                  (-2,0):[(0,-1),(0,1)]}
    # Horse move adjacents
    _HORSEADJ={(1,-2):(0,-1),
                   (-1,-2):(0,-1),
               (2,-1):(1,0),
               (2,1):(1,0),
               (1,2):(0,1),
               (-1,2):(0,1),
               (-2,1):(-1,0),
               (-2,-1):(-1,0)}

    # Spawn turn coming: True if new robots are to be spawned soon.
    _STC=False
    # List of commands to issue for each robot, keyed by location. The value is the command. 
    _CMDS={}
    # List of new locations robots are currently intending to move into.
    _NEW_MOVES=[]
    # List of all locations robots are currently intending to move into or intending to stay in
    _OCCUPIED=[]
    # List of locations we are attacking
    _ATTACKING=[]
    # List of empty locations we are attacking because we are guessing an enemy robot might move to it
    _GUESS_ATTACKS=[]
    # A list of misses. Key is the location from, value is a list of attacks missed.
    _MISSED={}
    # The list of locations to apply have the robot guard because they are fine where they are.
    _STAY_PUT=[]
    # Convenience: list of locations containing friendly robots
    _FRIENDLIES=[]
    # Convenience: list of locations containing enemy robots
    _ENEMIES=[]
    # Convenience: list of location containing friedly units that still need moves.
    _REMAINING=[]

    # DEBUG
    _LOG=False
    _LOOKAT=[]
    _DEBUG_TURNS=[]

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
            self._LOG=True
        else:
            self._LOG=False

        if self.init(game):
            self.ponder_on_spawned(game)
            self.ponder_make_way(game)
            self.ponder_run_away(game, 9, 4)
            self.ponder_make_way(game)
            self.ponder_run_away(game, self._SUICIDE_DAMAGE, 2)
            self.ponder_make_way(game)
            self.ponder_adj_enemies(game)
            self.ponder_make_way(game)
            self.ponder_guard(game)
            self.ponder_guess_attack(game, False)
            self.ponder_surround_realize(game)
            self.ponder_surround_prepare(game)
            self.ponder_make_way(game)
            self.ponder_chase_enemy(game)
            self.ponder_guess_attack(game, True)

        cmd = self.get_cmd()
        if self._LOG:
            print "ROBOT ", self.robot_id, "@", self.location, ":", cmd
        return cmd

    # The following computes the moves for ALL robots when the first robot move for a new turn are requested.
    # After it is computed, all robots will have moves assigned, and subsequent calls for the same turn
    # will simply return the result.
    def init(self, game):
        if self._CURTURN == game.turn:
            return False

        # Compute Spawn Turn Coming
        remainder = game.turn % rg.settings.spawn_every
        self._STC=(remainder == 0 or remainder == (rg.settings.spawn_every-1))

        # Compute friendly and enemy robot locations (Convenience)
        self._FRIENDLIES=[]
        self._ENEMIES=[]
        for loc in game.robots.keys():
            bot=game.robots[loc]
            if bot.player_id == self.player_id:
                self._FRIENDLIES.append(loc)
            else:
                self._ENEMIES.append(loc)

        # Determine misses
        self.miss_count(game)

        self._REMAINING=list(self._FRIENDLIES)
        self._CMDS={}
        self._OCCUPIED=[]
        self._ATTACKING=[]
        self._NEW_MOVES=[]
        self._STAY_PUT=[]
        self._GUESS_ATTACKS=[]
        self._CURTURN=game.turn

        return True

    #####################
    # MISS SUPPORT
    #####################

    # Record each miss from last turn attacks
    def miss_count(self, game):
        for floc in self._CMDS.keys():
            if self._CMDS[floc][0] == 'attack':
                tloc=self._CMDS[floc][1]
                if self.has_enemy(tloc):
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
            count=0
            for loc in self._MISSED[floc]:
                if loc == tloc:
                    count += 1
            return count
        return 0

    ####################
    # PONDER FUNCTIONS #
    ####################

    # If a robot is on a spawn square and STC is True, then they get the move that robot off.
    def ponder_on_spawned(self, game):

        movemap = self.MoveMap(self, game)

        for loc in self._REMAINING:
            if self.is_spawn(loc):
                movemap.move(loc)

        movemap.apply()

    # If there are locations that we are OCCUPIED that a robot is
    # currently on, that robot must move off.
    def ponder_make_way(self, game):
        if len(self._NEW_MOVES) > 0:
            movemap = self.MoveMap(self, game)
            movemap.set_empty_only(True)

            for loc in self._NEW_MOVES:
                if self.has_friendly(loc) and loc in self._REMAINING:
                    movemap.move(loc)
                if loc not in self._OCCUPIED:
                    self._OCCUPIED.append(loc)

            movemap.apply()
            self._NEW_MOVES=[]

    # If there are units close to death, they should run away.
    def ponder_run_away(self, game, threshold, safe_dist):
        movemap = self.MoveMap(self, game)
        for loc in self._REMAINING:
            bot=game.robots[loc]
            if bot.hp <= threshold:
                # Select a direction farthest from the closest enemy
                eloc=self.closest_enemy(loc)
		if eloc != None:
		    dist=rg.dist(loc, eloc)
		    if dist < safe_dist:
			for dir in self._DIRS:
			    tloc=self.get_loc(loc, dir)
			    tdist=rg.dist(tloc, eloc)
			    if tdist > dist:
				movemap.chk_add(loc, tloc)
			if not movemap.has(loc):
			    movemap.add_to_failed(loc)
		    else:
			movemap.add_to_failed(loc)

        movemap.apply()

        for loc in movemap.get_failed():
            self.apply_stay_put(loc)

    # If there units we previously thought would be better off staying put rather
    # than chasing down an enemy, if they have nothing else to do, then set them to guard
    def ponder_guard(self, game):
        guarding=[]
        for loc in self._STAY_PUT:
            if loc in self._REMAINING:
                guarding.append(loc)
        for loc in guarding:
            self.apply_guard(loc)
        self._STAY_PUT=[]

    # If there are adjacent enemies do the following
    #   - Consider suicide if 2 or more
    #   - If they all have less hp then attack the weakest
    #   - If there is only one adjacent, and it has at least one other friendly next to it, attack it
    #   - Dodge
    #   - If unable to dodge then attack.
    def ponder_adj_enemies(self, game):
        movemap = self.MoveMap(self, game)
        suicides=[]
        attacking={}
        for loc in self._REMAINING:
            bot=game.robots[loc]
            adj = self.AdjacentMap(self, game, loc)
            dodge=False
            count = adj.count()
            if count >= 2:
                if count >= 4:
                    consider=rg.settings.robot_hp
                elif count >= 3:
                    consider=rg.settings.robot_hp/2
                else:
                    consider=rg.settings.robot_hp/3
                if bot.hp < consider:
                    chance=self.get_hp_weight(bot.hp)+20*count
                    if self.roll_die(chance):
                        suicides.append(loc)
                        continue
                if count >= 4 or (bot.hp > adj.get_strongest_hp() and bot.hp > self._SUICIDE_DAMAGE):
                    attacking[loc]=adj.get_weakest_loc()
                else:
                    dodge=True
            elif count >= 1:
                if bot.hp <= self._SUICIDE_DAMAGE:
                    dodge=True
                elif bot.hp > adj.get_strongest_hp():
                    attacking[loc]=adj.get_strongest_loc()
                else:
                    # Count their adjacents to see if we are surrounding
                    count=0
                    for dir in self._DIRS:
                        dloc=self.get_loc(adj.get_strongest_loc(), dir)
                        if self.has_friendly(dloc):
                            count += 1
                    if count > 1:
                        attacking[loc]=adj.get_strongest_loc()
                    else:
			dodge=True

            if dodge and adj.get_strongest_loc() != None:
		dir=self.get_dir(loc, adj.get_strongest_loc())
                if self.valid_dir(dir):
                    for ddir in self._DIRS:
			if ddir != dir:
                            dloc=self.get_loc(loc, ddir)
                            movemap.chk_add(loc, dloc)

	for loc in suicides:
	    self.apply_suicide(loc)

        movemap.apply()

        if len(movemap.get_failed()) > 0:
            for loc in movemap.get_failed():
                adj = self.AdjacentMap(self, game, loc)
                attacking[loc]=adj.get_weakest_loc()

            for loc in attacking.keys():
                self.apply_attack(loc, attacking[loc])

    # If an enemy is diagonally adjacent or 2 sq away orthogonally adjacent
    # consider an attack unless the last time we tried there was a miss.
    def ponder_guess_attack(self, game, missesok):
        for loc in self._REMAINING:
            gmap = self.GuessAttackMap(self, game, loc)
            gmap.set_misses_ok(missesok)
            gmap.init();
            gmap.attack()

    # 
    # If 2 or more friendlies are on the diagonals of the same enemy then
    # see if they can all move in for the kill.
    #
    def ponder_surround_realize(self, game):
        map={}
        # Find possible targets
        for loc in self._REMAINING:
            dmap = self.DiagonalMap(self, game, loc)
            for dir in dmap.get_dirs():
                dloc = dmap.get_loc(dir)
                if dloc in map.keys():
                    map[dloc].append(loc)
                else:
                    map[dloc] = [loc]

        # See if we have some possible close-in attacks
        for dloc in map.keys():
            if len(map[dloc]) > 1:
                movemap = self.MoveMap(self, game)
                movemap.set_dangerous_ok(True)
                for floc in map[dloc]:
                    dir = self.get_dir(floc, dloc)
                    for adir in self._DADJ[dir]:
                        aloc = self.get_loc(floc, adir)
                        movemap.chk_add(floc, aloc)

                movemap.detect_problems()
                moves = movemap.get_moves()
                if moves.keys() > 1:
                    for floc in moves.keys():
                        self.apply_move(floc, moves[floc])

        # At this point, if we don't do anything we are in danger of having the
        # robot move during ponder_chase_enemies(). Therefore just attack 
        # one of the diagonals so they don't walk unsupported into the line of fire
        for dloc in map.keys():
            for floc in map[dloc]:
                if floc in self._REMAINING:
                    gmap = self.GuessAttackMap(self, game, floc)
                    gmap.set_misses_ok(True)
                    gmap.init()
                    gmap.attack()
    # 
    # If there are robots 2 squares away, either orthogonally or by horse move, 
    # try to move them to the enemy's diagonal.
    #
    def ponder_surround_prepare(self, game):
        movemap=self.MoveMap(self, game)
        # Find feints
        for loc in self._REMAINING:
            omap = self.Ortho2sqMap(self, game, loc)
            for dir in omap.get_dirs():
                for adir in self._FEINT2[dir]:
                    tloc = self.get_loc(loc, adir)
                    movemap.chk_add(loc, tloc)
            hmap = self.HorseMoveMap(self, game, loc)
            for dir in hmap.get_dirs():
                adir = self._HORSEADJ[dir]
                tloc = self.get_loc(loc, adir)
                movemap.chk_add(loc, tloc)
        movemap.apply();

    # Chase down the nearest enemy.
    # We want to be careful here not to collide with other friendlies
    def ponder_chase_enemy(self, game):
        movemap=self.MoveMap(self, game)
        movemap.set_empty_only(True)
        for loc in self._REMAINING:
            closest_eloc=self.closest_enemy(loc)
	    if closest_eloc != None:
		dirs=self.get_toward_as_dir(loc, closest_eloc)
		for dir in dirs:
		    tloc=self.get_loc(loc, dir)
		    movemap.chk_add(loc, tloc)
		if not movemap.has(loc):
		    movemap.add_to_failed(loc)
        movemap.apply()

        # Be more forgiving second time around
        if len(movemap.get_failed()) > 0:
            movemap2 = self.MoveMap(self, game)
            for loc in movemap.get_failed():
                closest_eloc=self.closest_enemy(loc)
		if closest_eloc != None:
		    notdir=self.get_dir(closest_eloc, loc)
		    for dir in self._DIRS:
			if dir != notdir:
			    tloc=self.get_loc(loc, dir)
			    movemap2.chk_add(loc, tloc)
            movemap2.apply()

    #######################
    # SUPPORT FUNCTIONS
    #######################

    def apply_move(self, floc, tloc):
        self._CMDS[floc] = ['move', tloc]
        if floc in self._REMAINING:
            self._REMAINING.remove(floc)
        else:
            print "ERROR: ", floc, " not in REMAINING"
        if not tloc in self._NEW_MOVES:
            self._NEW_MOVES.append(tloc)
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

    def apply_stay_put(self, loc):
        if not loc in self._STAY_PUT:
            self._STAY_PUT.append(loc)

    def closest_enemy(self, floc):
        closest_dist=1000
        closest_eloc=None
        for eloc in self._ENEMIES:
            dist=rg.dist(floc, eloc)
            if dist < closest_dist:
                closest_dist=dist
                closest_eloc=eloc
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
    def get_toward_as_dir(self, floc, tloc):
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
            return [(0,ndy)]
        elif ndy == 0:
            return [(ndx,0)]
        elif adx > ady:
            return [(ndx,0),(0,ndy)]
        else:
            return [(0,ndy),(ndx,0)]

    def get_loc(self, loc, dir):
        return (loc[0] + dir[0], loc[1] + dir[1])

    def get_hp_weight(self, hp):
        return rg.settings.robot_hp - hp

    def has_robot(self, game, loc):
        return loc in game.robots

    def has_friendly(self, loc):
        return loc in self._FRIENDLIES

    def has_enemy(self, loc):
        return loc in self._ENEMIES

    def is_normal(self, loc):
        types=rg.loc_types(loc)
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

    def is_attacking(self, loc):
        return loc in self._ATTACKING

    # If we are surrounded then add to suicide chance
    def roll_die(self, chance):
        die=random.randint(1,100)
        return die <= chance

    def valid_dir(self, dir):
        if dir[0] != 0 and dir[1] != 0:
            return False
        if abs(dir[0]) != 1 and abs(dir[1]) != 1:
            return False
        return True
