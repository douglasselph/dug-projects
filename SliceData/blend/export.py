#
# TIP Solutions, Shape molded, OpenGL exporter
#
import Blender
from Blender import *
import bpy
import os
import math

gMegaMax = 1700
gFileData = {}

class MyVert:
	uv = None
	
	def __init__(self, vert):
		self.co = vert.co
		self.no = vert.no

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
	verts = []
	faceVertsOverride = {}
	extraVerts = {}
	
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
		
gMeshInfo = MeshInfo()
	
def write_obj(filename):
	global gMeshTree
	
	# rootname = os.path.basename(os.path.splitext(filename)[0])
	gMeshTree = build_tree()
	dirname = os.path.dirname(filename)
	topobjname = get_topname()
	if topobjname == '':
		raise NoTopObjName
	else:
		write_mesh(dirname, None, topobjname)
	
def build_tree():
	global bpy
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
		else:
			print "Skipping %s of type %s" % (name, type)
		
	return tree
	
def get_topname():
	global gMeshTree
	topname = ''
	topchildcount = -1
	
	for objname in gMeshTree.keys():
		count = get_childcount(objname)
		print 'Mesh %s %d' % (objname, count)
		
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
	
	print "Writing '%s'" % objname
	
	list = gMeshTree[objname]
	obj = list[0]
	
	objchildren = list[1:]
		
	if not os.path.isdir(dirname):
		dirname = os.path.dirname(dirname)
	
	dotpos = objname.find('.')
	if dotpos >= 0:
		root_objname = objname[0:dotpos]
	else:
		root_objname = objname
	
	if basename == None:
		classname = objname.replace('.','')
		shapename = root_objname
	else:
		classname = '%s_%s' % (basename, objname.replace('.',''))
		shapename = '%s_%s' % (basename, root_objname)
	
	if gFileData.has_key(root_objname):
		filename = gFileData[root_objname]
		out = file(filename, 'a')
		out.write('\n')
	else:
		filename = "%s/%s.java" % (dirname, shapename)
		
		gFileData[root_objname] = filename
		
		out = file(filename, 'w')
		out.write('/* THIS IS A GENERATED FILE */\n\n')
			
		out.write('import java.nio.FloatBuffer;\n')
		out.write('import java.nio.ShortBuffer;\n')
		out.write('import com.tipsolutions.jacket.data.ShapeData;\n')
		out.write('import com.tipsolutions.jacket.math.Matrix4f;\n\n')
		
	out.write('\n')
	out.write('class %s extends ShapeData {\n' % classname)
				
	mesh = Mesh.New()
	mesh.getFromObject(objname) 
	
	#In theory more efficient, but totally different class:
	nmesh = obj.getData()
	
	convertToTriangles(mesh)
	
	write_children(out, objchildren, classname)
	write_objdata(out, obj)
	
	gMeshInfo.collect_data(mesh)
	
	write_boundaries(out)
	write_vertexes(out)
	write_normals(out)
	write_indexes(out, mesh)
	write_colors(out, mesh)	
	write_textures(out, mesh)
	write_vertexgroups(out, nmesh)
	write_debuginfo(mesh)

	out.write('};\n')
	out.close()
	
	print "Created %s" % filename
	
	for child in objchildren:
		write_mesh(dirname, shapename, child.getName())
	
def write_children(out, objchildren, classname):
	if len(objchildren) > 0:
		out.write
		out.write('\t@Override protected ShapeData [] _getChildren() {\n')
		out.write('\t\tShapeData [] children = new ShapeData[%d];\n' % len(objchildren))
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
	out.write('\t@Override protected Matrix4f _getMatrix() {\n');
	out.write('\t\treturn new Matrix4f(%ff, %ff, %ff, %ff,\n'  % (matrix[0][0], matrix[1][0], matrix[2][0], matrix[3][0]))
	out.write('\t\t                    %ff, %ff, %ff, %ff,\n'  % (matrix[0][1], matrix[1][1], matrix[2][1], matrix[3][1]))
	out.write('\t\t                    %ff, %ff, %ff, %ff,\n'  % (matrix[0][2], matrix[1][2], matrix[2][2], matrix[3][2]))
	out.write('\t\t                    %ff, %ff, %ff, %ff);\n' % (matrix[0][3], matrix[1][3], matrix[2][3], matrix[3][3]))
	out.write('\t}')	
		
def write_boundaries(out):
	global gMeshInfo
	
	out.write('\n')
	out.write('\t@Override protected float _getMinX() { return %ff; }\n' % gMeshInfo.minx);
	out.write('\t@Override protected float _getMaxX() { return %ff; }\n' % gMeshInfo.maxx);
	out.write('\t@Override protected float _getMinY() { return %ff; }\n' % gMeshInfo.miny);
	out.write('\t@Override protected float _getMaxY() { return %ff; }\n' % gMeshInfo.maxy);
	out.write('\t@Override protected float _getMinZ() { return %ff; }\n' % gMeshInfo.minz);
	out.write('\t@Override protected float _getMaxZ() { return %ff; }\n' % gMeshInfo.maxz);

def write_vertexes(out):
	global gMegaMax
	global gMeshInfo
	
	numverts = gMeshInfo.getNumVerts()
	
	out.write('\n')
	out.write('\t@Override\n')
	out.write('\tprotected FloatData getVertexData() {\n');
	out.write('\t\tclass VertexData implements FloatData {\n')	
	out.write('\t\t\tpublic void fill(FloatBuffer buf) {\n')
		
	max = gMegaMax
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
			start = x * max
			end = start + max
			
			for vert in gMeshInfo.verts[start:end]:
				out.write('\t\t\t\tbuf.put(%ff).put(%ff).put(%ff); /* %d */\n' % (vert.co.x, vert.co.y, vert.co.z, index))
				count = count + 3
				index = index + 1
 			out.write('\t\t\t};\n')
	else:
		for vert in gMeshInfo.verts:
			out.write('\t\t\t\tbuf.put(%ff).put(%ff).put(%ff); /* %d */\n' % (vert.co.x, vert.co.y, vert.co.z, index))
			index = index + 1
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
	
	out.write('\t@Override\n')
	out.write('\tprotected FloatData getNormalData() {\n');
	out.write('\t\tclass NormalData implements FloatData {\n')
	out.write('\t\t\tpublic void fill(FloatBuffer buf) {\n')

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
			start = x * max
			end = start + max
			for vert in gMeshInfo.verts[start, end]:
				out.write('\t\t\t\tbuf.put(%ff).put(%ff).put(%ff); /* %d */\n' % (vert.no.x, vert.no.y, vert.no.z, index))
				count = count + 3
				index = index + 1
			out.write('\t\t\t};\n')
	else:
		for vert in gMeshInfo.verts:
			out.write('\t\t\t\tbuf.put(%ff).put(%ff).put(%ff); /* %d */\n' % (vert.no.x, vert.no.y, vert.no.z, index))
			index = index + 1
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
	
	max = int(gMegaMax/3)
	numfaces = len(mesh.faces)
	
	out.write('\t@Override\n')
	out.write('\tprotected ShortData getIndexData() {\n');
	out.write('\t\tclass IndexData implements ShortData {\n');	
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
				x = x + 1
				header = False
			
			out.write('\t\t\t\tbuf')
			for vert in face.v:
				out.write('.put((short)%d)' % gMeshInfo.getVertIndex(face.index,vert.index))
				count = count + 1
			out.write('; /* %d */\n' % i)
			i = i + 1
			
			if i == end:
				out.write('\t\t\t};\n')
				header = True
				start = start + max
				end = end + max
		if not header:
			out.write('\t\t\t};\n')
	else:
		count = 0
		i = 0
		for face in mesh.faces:
			out.write('\t\t\t\tbuf')
			for vert in face.v:
				out.write('.put((short)%d)' % gMeshInfo.getVertIndex(face.index, vert.index))
				count = count + 1
			out.write('; /* %d */\n' % i)
			i = i + 1
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
	if numfaces == 0:
		return

	out.write('\t@Override\n')
	out.write('\tprotected ShortData getColorData() {\n');
	out.write('\t\tclass ColorData implements ShortData {\n');				
	out.write('\t\t\tpublic void fill(ShortBuffer buf) {\n');

	if numfaces > max:
		num = int(math.ceil(float(numfaces) / float(max)))
		for x in range(num):
			out.write('\t\t\t\tfill%d(buf);\n' % (x+1))
		out.write('\t\t\t}\n')
		out.write

		count = 0
		i = 0
		for x in range(num):
			out.write
			out.write('\t\t\tvoid fill%d(ShortBuffer buf) {\n' % (x+1))
			start = int(x * max)
			end = int(start + max)
			for face in mesh.faces[start:end]:
				for col in mesh.col:
					out.write('\t\t\t\tbuf.put((short)%d).put((short)%d).put((short)%d).put((short)%d); /* %d */\n' % 
						(col.r, col.g, col.b, col.a, i))
					i = i + 1
			out.write('\t\t\t};\n')
	else:
		i = 0
		for face in mesh.faces:
			for col in mesh.col:
				out.write('\t\t\t\tbuf.put((short)%d).put((short)%d).put((short)%d).put((short)%d); /* %d */\n' % 
					(col.r, col.g, col.b, col.a, i))
				i = i + 1
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
				out.write('\tprotected String _getTextureFilename() { return "%s"; }\n' % os.path.basename(im.getFilename().lstrip('/')))
				writeCoords = True
				break
	
	if writeCoords:
		global gMegaMax
		global gMeshInfo
		
		max = gMegaMax
		numverts = gMeshInfo.getNumVerts()
	
		out.write('\n')
		out.write('\t@Override\n')
		out.write('\tprotected FloatData getTextureData() {\n');
		out.write('\t\tclass TextureData implements FloatData {\n')
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
					out.write
					out.write('\t\t\tvoid fill%d(FloatBuffer buf) {\n' % (x+1))
					start = x * max
					end = start + max
					for vert in gMeshInfo.verts[start:end]:
						out.write('\t\t\t\tbuf.put(%ff).put(%ff); /* %d */\n' % (((vert.co.x-mx)/sx), ((vert.co.y-my)/sy), index))
						count = count + 2
						index = index + 1
					out.write('\t\t\t};\n')
			else:
				for vert in gMeshInfo.verts:
					out.write('\t\t\t\tbuf.put(%ff).put(%ff); /* %d */\n' % (((vert.co.x-mx)/sx), ((vert.co.y-my)/sy), index))
					index = index + 1
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
					start = x * max
					end = start + max
					for vert in gMeshInfo.verts[start:end]:
						out.write('\t\t\t\tbuf.put(%ff).put(%ff); /* %d */\n' % (vert.uv.x, vert.uv.y, index))
						count = count + 2
						index = index + 1
					out.write('\t\t\t};\n')
			else:
				for vert in gMeshInfo.verts:
					out.write('\t\t\t\tbuf.put(%ff).put(%ff); /* %d */\n' % (vert.uv.x, vert.uv.y, index))
					index = index + 1
				out.write('\t\t\t};\n')
				count = numverts * 2
				
		out.write
		out.write('\t\t\tpublic int size() { return %d; }\n\n' % count)
		out.write('\t\t};\n')
		out.write('\t\treturn new TextureData();\n')
		out.write('\t};\n')
		out.write('\n')			

def write_vertexgroups(out, mesh):
	names = mesh.getVertGroupNames()
	if len(names) <= 0:
		return

	out.write('\t@Override\n')
	out.write('\tprotected VertexGroupData [] getVertexGroups() {\n')
	out.write('\t\tVertexGroupData [] data = new VertexGroupData[%d];\n' % len(names))
	out.write('\n')
	
	group = 0
	vertPerLine = 5
	
	for groupname in names:
		out.write('\t\tdata[%d] = new VertexGroupData() {\n' % group);
		out.write('\t\t\tpublic String getName() { return "%s"; }\n' % groupname)
		out.write('\t\t\tpublic void fill(ShortBuffer buf) {\n');
		c = 0
		count = 0
		out.write('\t\t\t\tbuf');
		for vert in mesh.getVertsFromGroup(groupname):
			if c >= vertPerLine:
				out.write(';\n\t\t\t\tbuf');
				c = 1
			else:
				c = c + 1
			out.write('.put((short)%d)' % vert);
			count = count + 1
			once = True
		out.write(';\n\t\t\t}\n')
		out.write('\t\t\tpublic int size() { return %d; }\n' % count)
		out.write('\t\t};\n')
		group = group + 1
		
	out.write('\t\treturn data;\n')
	out.write('\t};\n')
	out.write('\n')	
		
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
						
name = os.path.splitext(Blender.Get('filename'))[0]
Blender.Window.FileSelector(write_obj, "Export", name)