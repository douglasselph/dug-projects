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
gMeshInfo = {}

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
							
			if not tree.has_key(name):
				tree[name] = [obj]
				
			parent = obj.getParent()
			if parent:
				parentname = parent.getName()
				
				if tree.has_key(parentname):
					tree[parentname].append(obj)
				else:
					tree[parentname] = [parent, obj]
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
	
	convertToTriangles(mesh)
	
	write_children(out, objchildren, classname)
	write_objdata(out, obj)
	write_boundaries(out, mesh)
	write_vertexes(out, mesh)
	write_normals(out, mesh)
	write_indexes(out, mesh)
	write_colors(out, mesh)	
	write_textures(out, mesh)
	write_info(mesh)

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
			
def write_boundaries(out, mesh):
	global gMeshInfo
	
	if len(mesh.verts) == 0:
		return
	
	vert = mesh.verts[0]
	minx = vert.co.x
	maxx = vert.co.x
	miny = vert.co.y
	maxy = vert.co.y
	minz = vert.co.z
	maxz = vert.co.z

	for vert in mesh.verts:
		if vert.co.x < minx:
			minx = vert.co.x
		elif vert.co.x > maxx:
			maxx = vert.co.x
		if vert.co.y < miny:
			miny = vert.co.y
		elif vert.co.y > maxy:
			maxy = vert.co.y
		if vert.co.z < minz:
			minz = vert.co.z
		elif vert.co.z > maxz:
			maxz = vert.co.z

	out.write('\n')
	out.write('\t@Override protected float _getMinX() { return %ff; }\n' % minx);
	out.write('\t@Override protected float _getMaxX() { return %ff; }\n' % maxx);
	out.write('\t@Override protected float _getMinY() { return %ff; }\n' % miny);
	out.write('\t@Override protected float _getMaxY() { return %ff; }\n' % maxy);
	out.write('\t@Override protected float _getMinZ() { return %ff; }\n' % minz);
	out.write('\t@Override protected float _getMaxZ() { return %ff; }\n' % maxz);

	gMeshInfo['minx'] = minx
	gMeshInfo['maxx'] = maxx
	gMeshInfo['miny'] = miny
	gMeshInfo['maxy'] = maxy
	gMeshInfo['minz'] = minz
	gMeshInfo['maxz'] = maxz
	
def write_vertexes(out, mesh):
	global gMegaMax
	numverts = len(mesh.verts)
	
	if numverts == 0:
		return

	out.write('\n')
	out.write('\t@Override\n')
	out.write('\tprotected FloatData getVertexData() {\n');
	out.write('\t\tclass VertexData implements FloatData {\n')
		
	out.write('\t\t\tpublic void fill(FloatBuffer buf) {\n')
		
	gMegaMax = 1700
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
			
			for vert in mesh.verts[start:end]:
				out.write('\t\t\t\tbuf.put(%ff).put(%ff).put(%ff); /* %d */\n' % (vert.co.x, vert.co.y, vert.co.z, index))
				count = count + 3
				index = index + 1
 			out.write('\t\t\t};\n')
	else:
		for vert in mesh.verts:
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

def write_normals(out, mesh):
	global gMegaMax
	max = gMegaMax
	
	numverts = len(mesh.verts)
	if numverts == 0:
		return
	
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
			for vert in mesh.verts[start:end]:
				out.write('\t\t\t\tbuf.put(%ff).put(%ff).put(%ff); /* %d */\n' % (vert.no.x, vert.no.y, vert.no.z, index))
				count = count + 3
				index = index + 1
			out.write('\t\t\t};\n')
	else:
		for vert in mesh.verts:
			out.write('\t\t\t\tbuf.put(%ff).put(%ff).put(%ff); /* %d */\n' % (vert.no.x, vert.no.y, vert.no.z, index))
			index = index + 1
		out.write('\t\t\t};\n')
		count = numverts * 3

	out.write
	out.write('\t\t\tpublic int size() { return %d; }\n\n' % count)
	out.write('\t\t};\n')
	out.write('\t\treturn new NormalData();\n')
	out.write('\t};\n')
	out.write('\n')			

def write_indexes(out, mesh):
	
	global gMegaMax
	max = gMegaMax
	
	max = int(max/3)
	numfaces = len(mesh.faces)
	if numfaces == 0:
		return
	
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
				out.write('.put((short)%d)' % vert.index)
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
				out.write('.put((short)%d)' % vert.index)
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
	
	if writeCoords:
		global gMegaMax
		global gMeshInfo
		
		max = gMegaMax
	
		numverts = len(mesh.verts)
		if numverts == 0:
			return
	
		mx = gMeshInfo['minx']
		my = gMeshInfo['miny']
		sx = gMeshInfo['maxx']-gMeshInfo['minx']
		sy = gMeshInfo['maxy']-gMeshInfo['miny']
		sz = gMeshInfo['maxz']-gMeshInfo['minz']
		
		out.write('\n')
		out.write('\t@Override\n')
		out.write('\tprotected FloatData getTextureData() {\n');
		out.write('\t\tclass TextureData implements FloatData {\n')
 			
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
				for vert in mesh.verts[start:end]:
					out.write('\t\t\t\tbuf.put(%ff).put(%ff); /* %d */\n' % (((vert.co.x-mx)/sx), ((vert.co.y-my)/sy), index))
					count = count + 2
					index = index + 1
				out.write('\t\t\t};\n')
		else:
			for vert in mesh.verts:
				out.write('\t\t\t\tbuf.put(%ff).put(%ff); /* %d */\n' % (((vert.co.x-mx)/sx), ((vert.co.y-my)/sy), index))
				index = index + 1
			out.write('\t\t\t};\n')
			count = numverts * 2

		out.write
		out.write('\t\t\tpublic int size() { return %d; }\n\n' % count)
		out.write('\t\t};\n')
		out.write('\t\treturn new TextureData();\n')
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
		
def write_info(mesh):
	if mesh.faceUV:
		print "Mesh HAS FACE UV TRUE"
		
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