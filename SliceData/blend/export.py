import Blender
from Blender import *
import bpy
import os
import math

megaMax = 1700

def write_obj(filename):
	sce = bpy.data.scenes.active
	ob = sce.objects.active
	mesh = Mesh.New()
	mesh.getFromObject(ob.name)
	
	name = os.path.basename(os.path.splitext(filename)[0])
	out = file(filename, 'w')
	
	write_mesh(out, mesh, name)
	
	out.close()
	
	for obj in bpy.data.objects:
		print "Found %s" % (obj.getType())				

def write_mesh(out, mesh, name):
	convertToTriangles(mesh)
			
	out.write('/* THIS IS A GENERATED FILE */\n\n')
			
	out.write('import java.nio.FloatBuffer;\n')
	out.write('import java.nio.ShortBuffer;\n')
	out.write('import com.tipsolutions.jacket.data.FigureData;\n')
	out.write
	out.write('class %s extends FigureData {\n' % name)

	write_boundaries(out, mesh)
	write_vertexes(out, mesh)
	write_normals(out, mesh)
	write_indexes(out, mesh)
	
	if mesh.vertexColors:
		write_colors(out, mesh)						
				
	out.write('};\n')
	
def write_boundaries(out, mesh):
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
	out.write('\t@Override public float _getMinX() { return %ff; }\n' % minx);
	out.write('\t@Override public float _getMaxX() { return %ff; }\n' % maxx);
	out.write('\t@Override public float _getMinY() { return %ff; }\n' % miny);
	out.write('\t@Override public float _getMaxY() { return %ff; }\n' % maxy);
	out.write('\t@Override public float _getMinZ() { return %ff; }\n' % minz);
	out.write('\t@Override public float _getMaxZ() { return %ff; }\n' % maxz);

def write_vertexes(out, mesh):
	global megaMax
	numverts = len(mesh.verts)

	out.write('\n')
	out.write('\t@Override\n')
	out.write('\tprotected FloatData getVertexData() {\n');
	out.write('\t\tclass VertexData implements FloatData {\n')
		
	out.write('\t\t\tpublic void fill(FloatBuffer buf) {\n')
		
	megaMax = 1700
	max = megaMax
			
	if numverts > max:
		num = int(math.ceil(numverts / max))
				
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
				out.write('\t\t\t\tbuf.put(%ff).put(%ff).put(%ff); /* %d */\n' % (vert.co.x, vert.co.y, vert.co.z, vert.index))
				count = count + 3
 			out.write('\t\t\t};\n')
	else:
		for vert in mesh.verts:
			out.write('\t\t\t\tbuf.put(%ff).put(%ff).put(%ff); /* %d */\n' % (vert.co.x, vert.co.y, vert.co.z, vert.index))
 		out.write('\t\t\t};\n')
		count = numverts * 3

	out.write
	out.write('\t\t\tpublic int size() { return %d; }\n\n' % count)
	out.write('\t\t};\n')
	out.write('\t\treturn new VertexData();\n')
	out.write('\t};\n')
	out.write('\n')

def write_normals(out, mesh):
	global megaMax
	max = megaMax
	
	numverts = len(mesh.verts)
	
	out.write('\t@Override\n')
	out.write('\tprotected FloatData getNormalData() {\n');
	out.write('\t\tclass NormalData implements FloatData {\n')
 			
	out.write('\t\t\tpublic void fill(FloatBuffer buf) {\n')

	if numverts > max:
		num = int(math.ceil(numverts / max))
				
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
				out.write('\t\t\t\tbuf.put(%ff).put(%ff).put(%ff); /* %d */\n' % (vert.no.x, vert.no.y, vert.no.z, vert.index))
				count = count + 3
			out.write('\t\t\t};\n')
	else:
		for vert in mesh.verts:
			out.write('\t\t\t\tbuf.put(%ff).put(%ff).put(%ff); /* %d */\n' % (vert.no.x, vert.no.y, vert.no.z, vert.index))
		out.write('\t\t\t};\n')
		count = numverts * 3

	out.write
	out.write('\t\t\tpublic int size() { return %d; }\n\n' % count)
	out.write('\t\t};\n')
	out.write('\t\treturn new NormalData();\n')
	out.write('\t};\n')
	out.write('\n')			

def write_indexes(out, mesh):

	out.write('\t@Override\n')
	out.write('\tprotected ShortData getIndexData() {\n');
	out.write('\t\tclass IndexData implements ShortData {\n');
	
	global megaMax
	max = megaMax
	
	max = int(max/3)
	numfaces = len(mesh.faces)
			
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
			elif i == end:
				start = start + max
				end = end + max
				header = True
			
			out.write('\t\t\t\tbuf')
			for vert in face.v:
				out.write('.put((short)%d)' % vert.index)
				count = count + 1
			out.write('; /* %d */\n' % i)
			i = i + 1
			
			if header:
				out.write('\t\t\t};\n')
				header = False
		if header:
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
	
	max = int(megaMax/4)
	numfaces = len(mesh.faces)

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
		
							
name = os.path.splitext(Blender.Get('filename'))[0]
Blender.Window.FileSelector(write_obj, "Export", '%s.java' % name)