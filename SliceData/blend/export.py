#arm
# TIP Solutions, Shape molded, OpenGL exporter
#
import Blender
from Blender import *
import bpy
import os
import math

gMegaMax = 2000
gFileData = {}

class MyVert:
	def __init__(self, vert):
		self.co = vert.co
		self.no = vert.no
		self.uv = None

	def setCol(self, col):
		self.col = col
	
	def setUV(self, uv):
		self.uv = uv
		
	def hasUV(self):
		return self.uv != None
	
	def matchUV(self, uv):
		if (self.uv.x == uv.x and self.uv.y == uv.y):
			return True
		return False

class MeshInfo:
	
	def __init__(self):
		self.verts = []
		self.faceVertsOverride = {}
		self.extraVerts = {}
		self.armData = None
		self.armObjects = []
		self.pkgName = ""
		
		global bpy
		self.fps = bpy.data.scenes.active.getRenderingContext().fps
	
	def collect_data(self, mesh):
		self.verts = []
		self.faceVertsOverride = {}
		self.extraVerts = {}
		
		vert = mesh.verts[0]
		self.minx = vert.co.x
		self.maxx = vert.co.x
		self.miny = vert.co.y
		self.maxy = vert.co.y
		self.minz = vert.co.z
		self.maxz = vert.co.z

		for vert in mesh.verts:
			if vert.co.x < self.minx:
				self.minx = vert.co.x
			elif vert.co.x > self.maxx:
				self.maxx = vert.co.x
			if vert.co.y < self.miny:
				self.miny = vert.co.y
			elif vert.co.y > self.maxy:
				self.maxy = vert.co.y
			if vert.co.z < self.minz:
				self.minz = vert.co.z
			elif vert.co.z > self.maxz:
				self.maxz = vert.co.z
				
			self.verts.append(MyVert(vert))
				
		if mesh.vertexColors:
			print "Not implemented: collecting of vertex colors"
		
		if mesh.faceUV:
			for face in mesh.faces:
				for i in range(len(face.verts)):
					self.addUV(face.index, face.verts[i].index, face.uv[i])
	
	# Need duplicate vertexes to handle uv texture placement.
	# That is, a single vertex could represent many different places
	# on the same texture bitmap.
	def addUV(self, face_index, vert_index, uv):
		vert = self.verts[vert_index]
		
		if not vert.hasUV():
			vert.setUV(uv)
		elif not vert.matchUV(uv):
			# First check for existing matching override
			if self.extraVerts.has_key(vert_index):
				for xtra_index in self.extraVerts[vert_index]:		
					if self.verts[xtra_index].matchUV(uv):
						self.faceVertsOverride[face_index,vert_index] = xtra_index
						return
					
			# No match: make new entry
			newvert = MyVert(vert);
			newvert.setUV(uv)
			newvertIndex = len(self.verts)
			self.verts.append(newvert)
			
			if self.extraVerts.has_key(vert_index):
				self.extraVerts[vert_index].append(newvertIndex)
			else:
				self.extraVerts[vert_index] = [newvertIndex]
				
			self.faceVertsOverride[face_index,vert_index] = newvertIndex
	
	def getVertIndex(self, face_index, vert_index):
		if self.faceVertsOverride.has_key((face_index,vert_index)):
			return self.faceVertsOverride[face_index,vert_index]
		return vert_index
	
	def getNumVerts(self):
		return len(self.verts)
	
	def collect_arm_data(self, mesh):
		armobj = self.getArmatureFor(mesh.name)
		if armobj != None:
			print "Found armature data for '%s'\n" % mesh.name
			
			self.armData = ArmData()
			self.armData.collect_data(armobj, mesh)
			self.armData.collect_anim_data(armobj)
		else:
			self.armData = None
			print "No armature data found for '%s'" % mesh.name
		
	def getArmatureFor(self, name):
		for armobj in self.armObjects:
			if armobj.getName().endswith(name):
				return armobj
		return None	
		
class Bone:
	
	def __init__(self, name):
		self.index = None
		self.verts = []
		self.joints = []
		self.jointParent = None
		self.animData = {}
		self.name = name
		
	def hasJoints(self):
		return (len(self.joints) > 0)
	
	def getNumJoints(self):
		return len(self.joints)
	
	def getJoints(self):
		return self.joints
	
	def addVertIndex(self, vert_index):
		global gMeshInfo
		
		if not findInList(self.verts, vert_index):
			self.verts.append(vert_index)
		
			if gMeshInfo.extraVerts.has_key(vert_index):
				for xtra_vert_index in gMeshInfo.extraVerts[vert_index]:
					self.verts.append(xtra_vert_index)

	def getSortedVerts(self):
		return range_sorted(self.verts)
	
	def readyAnimData(self, iponame, name):
		if self.animData.has_key(iponame):
			ipodict = self.animData[iponame]
		else:
			ipodict = {}
			self.animData[iponame] = ipodict
			
		ipodict[name] = []
					
	def recordAnimData(self, iponame, name, pt):
		opdict = self.animData[iponame]
		list = opdict[name]
		last_pos = len(list)-1
		last_pos2 = last_pos-1
		if last_pos2 >= 0:
 			last_val = list[last_pos]
			last_val2 = list[last_pos2]
		
			if last_val[1] == last_val2[1] and last_val[1] == pt[1]:
				opdict[name][last_pos] = pt
			else:
				opdict[name].append(pt)
		else:
			opdict[name].append(pt)
	
	def writeAnimSubData(self, out, tab, opname, ckey, key):
		opdict = self.animData[opname]
		if opdict.has_key(key):
			if len(opdict[key]) > 1:
				out.write('%s\tif (type == AnimType.%s) {\n' % (tab, ckey))
				out.write('%s\t\treturn new float [] {\n' % tab)
				for pt in opdict[key]:
					out.write('%s\t\t\t%ff, %ff,\n' % (tab, convertToTime(pt[0]), pt[1]))
				out.write('%s\t\t};\n' % tab)
				out.write('%s\t}\n' % tab)
	
	def writeAnimData(self, out, tab):
		set = 0
		for opname in self.animData.keys():
			out.write('%sif (set == %d) {\n' % (tab, set))
			self.writeAnimSubData(out, tab, opname, 'LOC_X', 'LocX')
			self.writeAnimSubData(out, tab, opname, 'LOC_Y', 'LocY')
			self.writeAnimSubData(out, tab, opname, 'LOC_Z', 'LocZ')
			self.writeAnimSubData(out, tab, opname, 'SCALE_X', 'ScaleX')
			self.writeAnimSubData(out, tab, opname, 'SCALE_Y', 'ScaleY')
			self.writeAnimSubData(out, tab, opname, 'SCALE_Z', 'ScaleZ')
			self.writeAnimSubData(out, tab, opname, 'QUAT_X', 'QuatX')
			self.writeAnimSubData(out, tab, opname, 'QUAT_Y', 'QuatY')
			self.writeAnimSubData(out, tab, opname, 'QUAT_Z', 'QuatZ')
			self.writeAnimSubData(out, tab, opname, 'QUAT_W', 'QuatW')
			self.writeAnimSubData(out, tab, opname, 'ROT_X', 'RotX')
			self.writeAnimSubData(out, tab, opname, 'ROT_Y', 'RotY')
			self.writeAnimSubData(out, tab, opname, 'ROT_Z', 'RotZ')
			out.write('%s}\n' % tab)
			set = set + 1
	
class Joint:
	
	def __init__(self, bones):
		self.index = None
		self.verts = []
		self.bones = bones
		
	def match(self, bones):
		for i in range(len(bones)):
			if self.bones[i] != bones[i]:
				return False
		return True
	
	def getBoneIndexes(self, armdata):
		result = []
		for bonename in self.bones:
			if armdata.bones.has_key(bonename):
				result.append(armdata.bones[bonename].index)
			else:
				result.append(-1)
		return result
	
	def getBoneSum(self, armdata):
		sum = 0
		for bonename in self.bones:
			if armdata.bones.has_key(bonename):
				sum = sum + armdata.bones[bonename].index
		return sum
	
	def addVertIndex(self, vert_index):
		global gMeshInfo
		
		if not findInList(self.verts, vert_index):				
				self.verts.append(vert_index)
				
				if gMeshInfo.extraVerts.has_key(vert_index):
					for xtra_vert_index in gMeshInfo.extraVerts[vert_index]:
						self.verts.append(xtra_vert_index)
	
	def getSortedVerts(self):
		return range_sorted(self.verts)
	
class ArmData:
	
	def collect_data(self, armobj, mesh):
		self.arm = armobj.getData()
		self.bones = {}
		self.joints = {}
		self.bonelist = []
		self.jointlist = []
		
		groupnames = mesh.getVertGroupNames()
		
		if len(groupnames) <= 0:
			print "*ARM-ERROR*: No group names for mesh '%s'" % mesh.name
			return
		
		for bonekey in self.arm.bones.keys():
			if not findInList(groupnames, bonekey):
				print "*ARM-ERROR*: No vertex group for bone '%s' (skipping)" % bonekey
				continue
				
			if not self.bones.has_key(bonekey):
				self.bones[bonekey] = Bone(bonekey)
			
			bone = self.bones[bonekey]
			verts = mesh.getVertsFromGroup(bonekey)
			
			for vert_index in mesh.getVertsFromGroup(bonekey):
				influences = mesh.getVertexInfluences(vert_index)
	
				bone.addVertIndex(vert_index)
					
				if len(influences) > 1:
					influences.sort()
					bonenames = []
					for inf in influences:
						bonenames.append(inf[0])
						
					jointkey = self.getJointKey(bonenames)
					if not self.joints.has_key(jointkey):
						self.joints[jointkey] = Joint(bonenames)
	
					self.joints[jointkey].addVertIndex(vert_index)
					
					if not findInList(self.joints[jointkey].verts, vert_index):				
						self.joints[jointkey].verts.append(vert_index)
					
		# Get Base bones
		basebones = []
		for bonekey in self.bones.keys():
			blender_bone = self.arm.bones[bonekey]
			root = self.getRootBone(blender_bone)
			if not findInList(basebones, root.name):
				basebones.append(root.name)
				
		if len(basebones) == 0:
			print "*ARM-ERROR*: No root bones?"
			return
		
		# Build bone list:	
		self.bonelist = []
		processed = {}
		
		for bonename in basebones:
			self.buildBoneList(processed, bonename)		
		
		# Set bone indexes:		
		pos = 0
		for bone in self.bonelist:
			bone.index = pos
			pos = pos + 1
		
		# Build joint list:
		jointlist = []
		for jointkey in self.joints.keys():
			jointlist.append(self.joints[jointkey])
		
		self.jointlist = sorted(jointlist, key=lambda joint: joint.getBoneSum(self))
			
		# Set joint indexes
		pos = 0
		for joint in self.jointlist:
			joint.index = pos
			pos = pos + 1
		
		# Compute joints for each bone	
		for bone in self.bonelist:
			bone.jointParent = self.getJointParent(bone.name)
			bone.joints = self.getJoints(bone)
				
	def getRootBone(self, blender_bone):
		parent = blender_bone.parent
		if parent == None:
			return blender_bone
		if self.bones.has_key(parent.name):
			return self.getRootBone(parent)
		return blender_bone
		
	def buildBoneList(self, processed, bonename):
		if processed.has_key(bonename):
			return
		processed[bonename] = True
		if self.bones.has_key(bonename):
	 		self.bonelist.append(self.bones[bonename])
			blender_bone = self.arm.bones[bonename]
			for child in blender_bone.children:
				self.buildBoneList(processed, child.name)
			
	def hasData(self):
		if self.arm != None and len(self.bones) > 0 and len(self.bonelist) > 0:
			return True
		return False	
	
	def getJointParent(self, bonename):
		blender_bone = self.arm.bones[bonename]
		blender_parent = blender_bone.parent
		if blender_parent == None:
			return None
		if not self.bones.has_key(blender_parent.name):
			return None
		bone = self.bones[bonename]
		parent = self.bones[blender_parent.name]
		for key in self.joints.keys():
			joint = self.joints[key]
			if findInList(joint.bones, bone.name) and findInList(joint.bones, parent.name):
				return joint
		return None
	
	def getJoints(self, bone):
		result = []
		for key in self.joints.keys():
			joint = self.joints[key]
			if joint != bone.jointParent and findInList(joint.bones, bone.name):
				result.append(joint)
		return result
	
	def getJointKey(self, bonenames):
		return ','.join(bonenames)
	
	def collect_anim_data(self, armobj):
		armact = armobj.getAction()
		
		if armact == None:
			armipo = armobj.getIpo()
			
			if armipo == None:
				print "No armature actions found"
				return
			
			arm = armobj.getData()
			bonekeys = arm.bones.keys()
			bonename = bonekeys[0]
			if self.bones.has_key(bonename):
				bone = self.bones[bonename]
				self.collect_anim_data_bone(armipo, bone)
			else:
				print "*ARM-ERROR*: No bone found named '%s'" % bonename
				print " Bone names were: %s" % self.bones.keys()
		else:
			for channelName in armact.getChannelNames():
					if not self.bones.has_key(channelName):
						print "Skipping channel %s: no such bone" % channelName
						continue
				
					bone = self.bones[channelName]
					armipo = armact.getChannelIpo(channelName)
		
					self.collect_anim_data_bone(armipo, bone)
					
	def collect_anim_data_bone(self, armipo, bone):
		for curve in armipo.curves:
				name = curve.getName()
				bone.readyAnimData(armipo.name, name)
				for beztriplet in curve.bezierPoints:
					bone.recordAnimData(armipo.name, name, beztriplet.pt)
					
gMeshInfo = MeshInfo()
	
def write_obj(filename):
	global gMeshTree
	global gMeshInfo
	
	gMeshInfo.pkgName = Blender.Draw.PupStrInput("Package Name: ", "data", 25)
		
	print '-- [write_obj(%s)] --' % filename
	
	gMeshTree = build_tree()
	dirname = os.path.dirname(filename)
	topobjname = get_topname()
	if topobjname == '':
		raise NoTopObjName
	else:
		write_mesh(dirname, None, topobjname)
	
def build_tree():
	global bpy
	global gMeshInfo
	tree = {}
	
	for obj in bpy.data.objects:
		name = obj.getName()
		type = obj.getType()

		if type == "Mesh":
			if name.startswith("Mesh."):
				print "Skipping %s" % name
				continue
				
			print "Found %s" % name
							
			if not tree.has_key(name):
				tree[name] = [obj]
				
			parent = obj.getParent()
			if parent and (parent.getType() == "Mesh"):
				parentname = parent.getName()
				
				if tree.has_key(parentname):
					tree[parentname].append(obj)
				else:
					tree[parentname] = [parent, obj]
		elif type == "Armature":
			print "Found Armature %s" % name
			gMeshInfo.armObjects.append(obj)
		else:
			print "Skipping %s of type %s" % (name, type)
		
	return tree
	
def get_topname():
	global gMeshTree
	topname = ''
	topchildcount = -1
	
	for objname in gMeshTree.keys():
		count = get_childcount(objname)
		print 'TOP Mesh %s %d' % (objname, count)
		
		if count > topchildcount:
			topchildcount = count
			topname = objname
	
	return topname
	
def get_childcount(name):
	global gMeshTree
	if not gMeshTree.has_key(name):		
		return 0
	
	children = gMeshTree[name][1:]
	count = len(children)
	for child in children:
		count = count + get_childcount(child.getName())
	return count
	
def write_mesh(dirname, basename, objname):
	global gMeshTree
	global gFileData
	global gMeshInfo
	
	if not gMeshTree.has_key(objname):
		print "Error: not such object name found: '%s'" % objname
		return
	
	print "(%s) Writing '%s'" % (basename, objname)
	
	list = gMeshTree[objname]
	obj = list[0]
	objchildren = list[1:]
		
	if not os.path.isdir(dirname):
		dirname = os.path.dirname(dirname)
	
	use_objname = objname.replace('.','')
	
	if basename == None:
		classname = use_objname
		shapename = use_objname
	else:
		classname = '%s_%s' % (basename, use_objname)
		shapename = '%s_%s' % (basename, use_objname)
	
	if gFileData.has_key(use_objname):
		filename = gFileData[use_objname]
		out = file(filename, 'a')
		out.write('\n')
	else:
		filename = "%s/%s.java" % (dirname, shapename)
		
		gFileData[use_objname] = filename
		
		out = file(filename, 'w')
		out.write('/* THIS IS A GENERATED FILE */\n\n')
		
		if gMeshInfo.pkgName != "":
			out.write('package %s;\n' % gMeshInfo.pkgName)
			
		out.write('import java.nio.FloatBuffer;\n')
		out.write('import java.nio.ShortBuffer;\n')
		out.write('import com.tipsolutions.jacket.shape.Shape;\n')
		out.write('import com.tipsolutions.jacket.math.Matrix4f;\n\n')
		
	out.write('\n')
	out.write('public class %s extends Shape {\n' % classname)
				
	mesh = Mesh.New()
	mesh.getFromObject(objname) 
	
	#In theory more efficient, but totally different class:
	nmesh = obj.getData()
	
	convertToTriangles(mesh)
	
	write_children(out, objchildren, classname)
	write_objdata(out, obj)
	
	gMeshInfo.collect_data(mesh)
	gMeshInfo.collect_arm_data(nmesh)
	
	write_boundaries(out)
	write_vertexes(out)
	write_normals(out)
	write_indexes(out, mesh)
	write_colors(out, mesh)	
	write_textures(out, mesh)
	write_armature(out)
	# write_debuginfo(mesh)

	out.write('};\n')
	out.close()
	
	print "Created %s" % filename
	
	for child in objchildren:
		write_mesh(dirname, shapename, child.getName())
	
def write_children(out, objchildren, classname):
	if len(objchildren) > 0:
		out.write
		out.write('\t@Override protected Shape [] dGetChildren() {\n')
		out.write('\t\tShape [] children = new Shape[%d];\n' % len(objchildren))
		i = 0
		for child in objchildren:
			childname = "%s_%s" % (classname, child.getName().replace('.',''))
			out.write('\t\tchildren[%d] = new %s();\n' % (i, childname))
			i = i + 1
		out.write('\t\treturn children;\n')
		out.write('\t}\n');
			
def write_objdata(out, obj):
	
	matrix = obj.getMatrix("localspace")
	# 
	# Blender uses row order translations, that is: [x y z w] x M
	# But OpenGL, uses column order, that is: 
	#         [x
	#    M x   y 
	#          z
	#          w]
	# Also need to invert matrix rotations
	out.write('\n')
	out.write('\t@Override protected Matrix4f dGetMatrix() {\n');
	out.write('\t\treturn new Matrix4f(%ff, %ff, %ff, %ff,\n'  % (matrix[0][0], matrix[1][0], matrix[2][0], matrix[3][0]))
	out.write('\t\t                    %ff, %ff, %ff, %ff,\n'  % (matrix[0][1], matrix[1][1], matrix[2][1], matrix[3][1]))
	out.write('\t\t                    %ff, %ff, %ff, %ff,\n'  % (matrix[0][2], matrix[1][2], matrix[2][2], matrix[3][2]))
	out.write('\t\t                    %ff, %ff, %ff, %ff);\n' % (matrix[0][3], matrix[1][3], matrix[2][3], matrix[3][3]))
	out.write('\t}')	
		
def write_boundaries(out):
	global gMeshInfo
	
	out.write('\n')
	out.write('\t@Override protected float dGetMinX() { return %ff; }\n' % gMeshInfo.minx);
	out.write('\t@Override protected float dGetMaxX() { return %ff; }\n' % gMeshInfo.maxx);
	out.write('\t@Override protected float dGetMinY() { return %ff; }\n' % gMeshInfo.miny);
	out.write('\t@Override protected float dGetMaxY() { return %ff; }\n' % gMeshInfo.maxy);
	out.write('\t@Override protected float dGetMinZ() { return %ff; }\n' % gMeshInfo.minz);
	out.write('\t@Override protected float dGetMaxZ() { return %ff; }\n' % gMeshInfo.maxz);

def write_vertexes(out):
	global gMegaMax
	global gMeshInfo
	
	numverts = gMeshInfo.getNumVerts()
	
	out.write('\n')
	out.write('\t@Override\n')
	out.write('\tprotected dFloatBuf dGetVertexDef() {\n');
	out.write('\t\tclass VertexData implements dFloatBuf {\n')	
	out.write('\t\t\tpublic void fill(FloatBuffer buf) {\n')
		
	max = gMegaMax
	perLine = 5
	index = 0
			
	if numverts > max:
		num = int(math.ceil(float(numverts) / float(max)))
				
		for x in range(num):
			out.write('\t\t\t\tfill%d(buf);\n' % (x+1))
		out.write('\t\t\t}\n')
		out.write
		count = 0
		for x in range(num):
			out.write
			out.write('\t\t\tvoid fill%d(FloatBuffer buf) {\n' % (x+1))
			out.write('\t\t\t\tbuf.put(new float [] {\n')
			start = x * max
			end = start + max
			perLineCount = 0
			
			for vert in gMeshInfo.verts[start:end]:
				if perLineCount == 0:
					out.write('\t\t\t\t\t')
				out.write('%ff, %ff, %ff, ' % (vert.co.x, vert.co.y, vert.co.z))
				count = count + 3
				index = index + 1
				perLineCount = perLineCount + 1
				if perLineCount >= perLine:
					out.write('/* %d -> %d */\n' % (index-perLineCount, index-1))
					perLineCount = 0
			if perLineCount > 0:
				out.write('/* %d -> %d */\n' % (index-perLineCount, index-1))
			out.write('\t\t\t\t});\n')
 			out.write('\t\t\t};\n')
	else:
		out.write('\t\t\t\tbuf.put(new float [] {\n')
		perLineCount = 0
		
		for vert in gMeshInfo.verts:
			if perLineCount == 0:
				out.write('\t\t\t\t\t')
				
			out.write('%ff, %ff, %ff, ' % (vert.co.x, vert.co.y, vert.co.z))
			
			perLineCount = perLineCount + 1
			index = index + 1
			if perLineCount >= perLine:
				out.write('/* %d -> %d */\n' % (index-perLineCount,index-1))
				perLineCount = 0
		if perLineCount > 0:
			out.write('/* %d -> %d */\n' % (index-perLineCount,index-1))
			
		out.write('\t\t\t\t});\n')
 		out.write('\t\t\t};\n')
		count = numverts * 3

	out.write
	out.write('\t\t\tpublic int size() { return %d; }\n\n' % count)
	out.write('\t\t};\n')
	out.write('\t\treturn new VertexData();\n')
	out.write('\t};\n')
	out.write('\n')

def write_normals(out):
	global gMegaMax
	global gMeshInfo
	
	max = gMegaMax
	numverts = gMeshInfo.getNumVerts()
	perLine = 5
	
	out.write('\t@Override\n')
	out.write('\tprotected dFloatBuf dGetNormalDef() {\n');
	out.write('\t\tclass NormalData implements dFloatBuf {\n')
	out.write('\t\t\tpublic void fill(FloatBuffer buf) {\n')

	index = 0
	
	if numverts > max:
		num = int(math.ceil(float(numverts) / float(max)))
				
		for x in range(num):
			out.write('\t\t\t\tfill%d(buf);\n' % (x+1))
		out.write('\t\t\t}\n')
		out.write
		count = 0
		perLineCount = 0
		
		for x in range(num):
			out.write('\n')
			out.write('\t\t\tvoid fill%d(FloatBuffer buf) {\n' % (x+1))
			out.write('\t\t\t\tbuf.put(new float [] {\n')
			start = x * max
			end = start + max
			for vert in gMeshInfo.verts[start:end]:
				if perLineCount == 0:
					out.write('\t\t\t\t\t')
				out.write('%ff, %ff, %ff, ' % (vert.no.x, vert.no.y, vert.no.z))
				perLineCount = perLineCount + 1
				count = count + 3
				index = index + 1
				if perLineCount >= perLine:
					out.write('/* %d -> %d */\n' % (index-perLineCount,index-1))
					perLineCount = 0
			out.write('\t\t\t\t});\n')
			out.write('\t\t\t};\n')
	else:
		perLineCount = 0
		out.write('\t\t\t\tbuf.put(new float [] {\n')
		
		for vert in gMeshInfo.verts:
			if perLineCount == 0:
				out.write('\t\t\t\t\t')
			out.write('%ff, %ff, %ff, ' % (vert.no.x, vert.no.y, vert.no.z))
			index = index + 1
			perLineCount = perLineCount + 1
			
			if perLineCount >= perLine:
				out.write(' /* %d -> %d */\n' % (index-perLineCount,index-1))
				perLineCount = 0
				
		if perLineCount > 0:
				out.write(' /* %d -> %d */\n' % (index-perLineCount,index-1))
				
		out.write('\t\t\t\t});\n')
		out.write('\t\t\t};\n')
		count = numverts * 3

	out.write('\n')
	out.write('\t\t\tpublic int size() { return %d; }\n\n' % count)
	out.write('\t\t};\n')
	out.write('\t\treturn new NormalData();\n')
	out.write('\t};\n')
	out.write('\n')			

def write_indexes(out, mesh):
	global gMeshInfo
	global gMegaMax
	
	max = int(gMegaMax)
	perLine = 10
	numfaces = len(mesh.faces)
	
	out.write('\t@Override\n')
	out.write('\tprotected dShortBuf dGetIndexDef() {\n');
	out.write('\t\tclass IndexData implements dShortBuf {\n');	
	out.write('\t\t\tpublic void fill(ShortBuffer buf) {\n');

	if numfaces > max:
		num = int(math.ceil(float(numfaces) / float(max)))
		for x in range(num):
			out.write('\t\t\t\tfill%d(buf);\n' % (x+1))
		out.write('\t\t\t}\n')
		out.write

		count = 0
		i = 0
		x = 0
		header = True
		start = 0
		end = max-1
		
		for face in mesh.faces:
			if header:
				out.write
				out.write('\t\t\tvoid fill%d(ShortBuffer buf) {\n' % (x+1))
				out.write('\t\t\t\tbuf.put(new short [] {\n')
				x = x + 1
				header = False
				perLineCount = 0
			
			if perLineCount == 0:
				out.write('\t\t\t\t\t')
				
			for vert in face.v:
				out.write('%d, ' % gMeshInfo.getVertIndex(face.index,vert.index))
				count = count + 1
				
			i = i + 1
			perLineCount = perLineCount + 1
			if perLineCount >= perLine or i == end:	
				out.write('/* %d -> %d */\n' % (i-perLineCount,i-1))
				perLineCount = 0
			
			if i == end:
				out.write('\t\t\t\t});\n')
				out.write('\t\t\t};\n')
				header = True
				start = start + max
				end = end + max
		if not header:
			out.write('\t\t\t\t});\n')
			out.write('\t\t\t};\n')
	else:
		count = 0
		i = 0
		perLineCount = 0
		perLineStart_i = 0
		
		out.write('\t\t\t\tbuf.put(new short [] {\n')
		
		for face in mesh.faces:
			if perLineCount == 0:
				out.write('\t\t\t\t\t')
					
			for vert in face.v:
				out.write('%d,' % gMeshInfo.getVertIndex(face.index, vert.index))				
				count = count + 1
				
			i = i + 1
			perLineCount = perLineCount + 1
			if perLineCount >= perLine:
				out.write('/* %d -> %d */\n' % (i-perLineCount, i-1))
				perLineCount = 0
				
		if perLineCount > 0:
			out.write('/* %d -> %d */\n' % (i-perLineCount,i-1))
			
		out.write('\t\t\t\t});\n')
		out.write('\t\t\t};\n')

	out.write
	out.write('\t\t\tpublic int size() { return %d; }\n\n' % count)
	out.write('\t\t};\n')
	out.write('\t\treturn new IndexData();\n')
	out.write('\t};\n')
	out.write('\n')	

def write_colors(out, mesh):
	global gMegaMax
	
	if not mesh.vertexColors:
		return
		
	max = int(gMegaMax/4)
	numfaces = len(mesh.faces)
	perLine = 5
	if numfaces == 0:
		return

	out.write('\t@Override\n')
	out.write('\tprotected dShortBuf dGetColorDef() {\n');
	out.write('\t\tclass ColorData implements dShortBuf {\n');				
	out.write('\t\t\tpublic void fill(ShortBuffer buf) {\n');

	if numfaces > max:
		num = int(math.ceil(float(numfaces) / float(max)))
		for x in range(num):
			out.write('\t\t\t\tfill%d(buf);\n' % (x+1))
		out.write('\t\t\t}\n')
		out.write('\n')

		count = 0
		i = 0
		for x in range(num):
			out.write
			out.write('\t\t\tvoid fill%d(ShortBuffer buf) {\n' % (x+1))
			out.write('\t\t\t\tbuf.put(new short [] {\n')
			start = int(x * max)
			end = int(start + max)
			perLineCount = 0
			for face in mesh.faces[start:end]:
				if perLineCount == 0:
					out.write('\t\t\t\t\t')
				for col in mesh.col:
					out.write('%d,%d,%d,%d,' % 
						(col.r, col.g, col.b, col.a))
					i = i + 1
					perLineCount = perLineCount + 1
					if perLineCount >= perLine:
						out.write('/* %d -> %d */\n' % (i - perLineCount, i-1))
						perLineCount = 0
				
			if perLineCount == 0:
				out.write('/* %d -> %d */\n' % (i - perLineCount, i-1))
			out.write('\t\t\t\t});\n')
			out.write('\t\t\t};\n')
	else:
		i = 0
		perLineCount = 0
		out.write('\t\t\t\tbuf.put(new short [] {\n')
		for face in mesh.faces:
			for col in mesh.col:
				out.write('%d,%d,%d,%d,' % (col.r, col.g, col.b, col.a))
				i = i + 1
				perLineCount = perLineCount + 1
				if perLineCount >= perLine:
					out.write('/* %d -> %d */\n' % (i - perLineCount, i-1))
					perLineCount = 0
		if perLineCount == 0:
			out.write('/* %d -> %d */\n' % (i - perLineCount, i-1))
		out.write('\t\t\t\t});\n')
		out.write('\t\t\t};\n')

	count = i * 4
	out.write
	out.write('\t\t\tpublic int size() { return %d; }\n\n' % count)
	out.write('\t\t};\n')
	out.write('\t\treturn new ColorData();\n')
	out.write('\t};\n')
				
def write_textures(out, mesh):
	writeCoords = False;
	
	for mat in mesh.materials:
		for mtex in mat.getTextures():
			if not mtex:
				continue

			tex = mtex.tex
			im = tex.getImage()
			if im:
				out.write('\t@Override\n')
				out.write('\tprotected String dGetTextureFilename() { return "%s"; }\n' % os.path.basename(im.getFilename().lstrip('/')))
				writeCoords = True
				break
	
	if writeCoords:
		global gMegaMax
		global gMeshInfo
		
		max = gMegaMax
		numverts = gMeshInfo.getNumVerts()
		perLine = 10
	
		out.write('\n')
		out.write('\t@Override\n')
		out.write('\tprotected dFloatBuf dGetTextureDef() {\n');
		out.write('\t\tclass TextureData implements dFloatBuf {\n')
		out.write('\t\t\tpublic void fill(FloatBuffer buf) {\n')

		index = 0
	
		if not mesh.faceUV:
			mx = gMeshInfo.minx
			my = gMeshInfo.miny
			sx = gMeshInfo.maxx-gMeshInfo.minx
			sy = gMeshInfo.maxy-gMeshInfo.miny
			sz = gMeshInfo.maxz-gMeshInfo.minz

			if numverts > max:
				num = int(math.ceil(float(numverts) / float(max)))
				
				for x in range(num):
					out.write('\t\t\t\tfill%d(buf);\n' % (x+1))
				out.write('\t\t\t}\n')
				out.write('\n')
				count = 0
				for x in range(num):
					out.write('\n')
					out.write('\t\t\tvoid fill%d(FloatBuffer buf) {\n' % (x+1))
					out.write('\t\t\t\tbuf.put(new float [] {\n')
					start = x * max
					end = start + max
					perLineCount = 0
					for vert in gMeshInfo.verts[start:end]:
						if perLineCount == 0:
							out.write('\t\t\t\t\t')
						out.write('%ff, %ff, ' % (((vert.co.x-mx)/sx), ((vert.co.y-my)/sy)))
						perLineCount = perLineCount + 1
						count = count + 2
						index = index + 1
						if perLineCount >= perLine:
							out.write('/* %d -> %d */\n' % (index-perLineCount, index-1))
							perLineCount = 0
					if perLineCount > 0:
						out.write('/* %d -> %d */\n' % (index-perLineCount, index-1))
					out.write('});\n')
					out.write('\t\t\t};\n')
			else:
				perLineCount = 0
				out.write('\t\t\t\tbuf.put(new float [] {\n')
				for vert in gMeshInfo.verts:
					if perLineCount == 0:
						out.write('\t\t\t\t\t')
					out.write('%ff, %ff, ' % (((vert.co.x-mx)/sx), ((vert.co.y-my)/sy)))
					index = index + 1
					perLineCount = perLineCount + 1
					if perLineCount >= perLine:
						out.write('/* %d -> %d */\n' % (index-perLineCount,index-1))
						perLineCount = 0
				if perLineCount > 0:
					out.write('/* %d -> %d */\n' % (index-perLineCount,index-1))
				out.write('});\n')
				out.write('\t\t\t};\n')
				count = numverts * 2
		else:
			if numverts > max:
				num = int(math.ceil(float(numverts) / float(max)))
				
				for x in range(num):
					out.write('\t\t\t\tfill%d(buf);\n' % (x+1))
				out.write('\t\t\t}\n')
				out.write
				count = 0
				for x in range(num):
					out.write('\n')
					out.write('\t\t\tvoid fill%d(FloatBuffer buf) {\n' % (x+1))
					out.write('\t\t\t\tbuf.put(new float [] {\n')
					start = x * max
					end = start + max
					perLineCount = 0
					for vert in gMeshInfo.verts[start:end]:
						if perLineCount == 0:
							out.write('\t\t\t\t\t')
						out.write('%ff, %ff, ' % (vert.uv.x, vert.uv.y))
						count = count + 2
						index = index + 1
						perLineCount = perLineCount + 1
						if perLineCount >= perLine:
							out.write('/* %d -> %d */' % (index-perLineCount, index-1))
							perLineCount = 0
					if perLineCount > 0:
						out.write('/* %d -> %d */' % (index-perLineCount, index-1))
					out.write('});\n')
					out.write('\t\t\t};\n')
			else:
				out.write('\t\t\t\tbuf.put(new float [] {\n')
				perLineCount = 0
				
				for vert in gMeshInfo.verts:
					if perLineCount == 0:
						out.write('\t\t\t\t\t')
					out.write('%ff, %ff, ' % (vert.uv.x, vert.uv.y))
					perLineCount = perLineCount + 1
					index = index + 1
					if perLineCount >= perLine:
						out.write('/* %d -> %d */\n' % (index - perLineCount, index-1))
						perLineCount = 0
					
				if perLineCount > 0:
					out.write('/* %d -> %d */\n' % (index - perLineCount, index-1))
					
				out.write('\t\t\t\t});\n')
				
				out.write('\t\t\t};\n')
				count = numverts * 2
				
		out.write('\n')
		out.write('\t\t\tpublic int size() { return %d; }\n\n' % count)
		out.write('\t\t};\n')
		out.write('\t\treturn new TextureData();\n')
		out.write('\t};\n')
		out.write('\n')			

def write_armature(out):
	global gMeshInfo
	
	if gMeshInfo.armData == None:
		print "No armature data"
		return
	if not gMeshInfo.armData.hasData():
		print "Empty armature data"
		return
	
	out.write('\t@Override\n')
	out.write('\tprotected dBone [] dGetBonesDef() {\n')
	out.write('\t\tdBone [] bones = new dBone[%d];\n' % len(gMeshInfo.armData.bonelist))
	out.write('\n')	
	
	vertPerLine = 10
	
	for bone in gMeshInfo.armData.bonelist:
		out.write('\t\tbones[%d] = new dBone() {\n' % bone.index);
		out.write('\t\t\t@Override public String getName() { return "%s"; }\n' % bone.name)
		out.write('\t\t\t@Override public void fill(ShortBuffer buf) {\n');
		
		c = 0
		count = 0
		out.write('\t\t\t\tbuf.put(new short [] {\n\t\t\t\t\t');
		for vert in bone.getSortedVerts():
			if c >= vertPerLine:
				out.write('\n\t\t\t\t\t');
				c = 1
			else:
				c = c + 1
			out.write('%d,' % vert);
			count = count + 1
			once = True
		
		out.write('});\n\t\t\t}\n')
		out.write('\t\t\t@Override public int size() { return %d; }\n' % count)
		out.write('\t\t\t@Override public int [] getJoints() {\n');
		
		if bone.hasJoints():
			out.write('\t\t\t\tint [] joints = new int[%d];\n' % bone.getNumJoints())
			pos = 0
			for joint in bone.getJoints():
				out.write('\t\t\t\tjoints[%d] = %d;\n' % (pos, joint.index))
				pos = pos + 1
			out.write('\t\t\t\treturn joints;\n')
		else:
			out.write('\t\t\t\treturn null;\n')
		
		out.write('\t\t\t};\n');
		
		if bone.jointParent != None:
			out.write('\t\t\t@Override public int getJointParent() { return %d; }\n' % bone.jointParent.index)
		
		if len(bone.animData) > 0:
			out.write('\t\t\t@Override public String [] getAnimSets() {\n')
			out.write('\t\t\t\treturn new String [] {\n')
			for opname in bone.animData.keys():
				out.write('\t\t\t\t\t"%s",\n' % opname)
			out.write('\t\t\t\t};\n')
			out.write('\t\t\t}\n')
			out.write('\t\t\t@Override public float [] getAnimKnotPts(int set, AnimType type) {\n')
			bone.writeAnimData(out, '\t\t\t\t')
			out.write('\t\t\t\treturn null;\n')
			out.write('\t\t\t}\n')
			
		out.write('\t\t};\n')
	
	out.write('\t\treturn bones;\n')
	out.write('\t};\n')
	out.write('\n')
	
	numJoints = len(gMeshInfo.armData.joints.keys())
	
	if numJoints > 0:
		out.write('\t@Override\n')
		out.write('\tprotected dJoint [] dGetJointsDef() {\n')
		out.write('\t\tdJoint [] joints = new dJoint[%d];\n' % numJoints)
	
		for joint in gMeshInfo.armData.jointlist:
			out.write('\t\tjoints[%d] = new dJoint() {\n' % joint.index)
			out.write('\t\t\t@Override public void fill(ShortBuffer buf) {\n')
		
			c = 0
			count = 0
			out.write('\t\t\t\tbuf.put(new short [] {\n\t\t\t\t\t');
			for vert in joint.getSortedVerts():
				if c >= vertPerLine:
					out.write('\n\t\t\t\t\t')
					c = 1
				else:
					c = c + 1
				out.write('%d,' % vert)
				count = count + 1
				once = True
		
			out.write('});\n\t\t\t}\n')
			out.write('\t\t\t@Override public int size() { return %d; }\n' % count)
			out.write('\t\t\t@Override public int [] getBones() {\n')
		
			boneindexes = joint.getBoneIndexes(gMeshInfo.armData)
		
			out.write('\t\t\t\tint [] bones = new int[%d];\n' % len(boneindexes))
			pos = 0
			for index in boneindexes:
				out.write('\t\t\t\tbones[%d] = %d;\n' % (pos, index))
				pos = pos + 1
			out.write('\t\t\t\treturn bones;\n')
			out.write('\t\t\t};\n');
			out.write('\t\t};\n')
		out.write('\t\treturn joints;\n')
		out.write('\t};\n')
		out.write('\n')	
		
def findInList(list, key):
	for k in list:
		if k == key:
			return True
	return False

# Examine list, and create a result list with these conditions:
#  If N+1 > N then N+1 is disconnected from N.
#  If N+1 < N then a range is represented from N+1 to N
def range_sorted(values):
	values = sorted(values)
	result = []
	previous = None
	start_range = None
	
	for value in values:
		if previous == None:
			previous = value
			start_range = None
		elif value == previous+1:
			if start_range == None:
				start_range = previous
			previous = value
		else:
			if start_range != None:
				if previous > start_range+1:
					result.append(previous)
					result.append(start_range)
				else:
					result.append(start_range)
					result.append(previous)
				previous = value
				start_range = None
			else:
				result.append(previous)
				previous = value
				
	if start_range != None:
		if previous > start_range+1:
			result.append(previous)
			result.append(start_range)
		else:
			result.append(start_range)
			result.append(previous)
	elif previous != None:
		result.append(previous)
		
	return result
		
def convertToTime(framex):
	global gMeshInfo
	return framex / gMeshInfo.fps
	
def convertToTriangles(mesh):
	global bpy
	sce = bpy.data.scenes.active
	
	editmode = Window.EditMode()
	if editmode:
		Window.EditMode(0)
		
	has_quads = False
			
	for f in mesh.faces:
		if len(f) == 4:
			has_quads = True
			break
				 
	if has_quads:
		oldmode = Mesh.Mode()
		Mesh.Mode(Mesh.SelectModes['FACE'])
			
		mesh.sel = True
		tempob = sce.objects.new(mesh)
		mesh.quadToTriangle(0) # more=0 shortest length
		oldmode = Mesh.Mode(oldmode)
				
		sce.objects.unlink(tempob)
		Mesh.Mode(oldmode)			
		
def write_debuginfo(mesh):
	for mat in mesh.materials:
		print "+++ Material %s" % mat.getName()
		print "RGB Col %s" % mat.getRGBCol()
		print "Spec Col %s" % mat.getSpecCol()
		
		for mtex in mat.getTextures():
			if not mtex:
				continue

			print "Texture Blend Color = %f,%f,%f" % (mtex.col)
			print "Texture Blend Color Factor = %f" % mtex.colfac
			print "Texture Offset %f,%f,%f" % (mtex.ofs)
			print "Texture Size %f,%f,%f" % (mtex.size)
			print "Texture UV Layer = %s" % mtex.uvlayer

			tex = mtex.tex
			im = tex.getImage()
			if im:
				print "Texture Image Filename %s" % im.getFilename()
			print "---"	
			
	print "--- MESH DONE"

dirname = os.path.splitext(Blender.Get('filename'))[0]
Blender.Window.FileSelector(write_obj, "Export", dirname)