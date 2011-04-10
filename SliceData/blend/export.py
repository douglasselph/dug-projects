import Blender
from Blender import *
import bpy
import os
import math

def write_obj(filename):
	#sce = bpy.data.scenes.active
	#ob = sce.objects.active
	#mesh = Mesh.New()
	#mesh.getFromObject(ob.name)
	
	name = os.path.basename(os.path.splitext(filename)[0])
	out = file(filename, 'w')

	for obj in bpy.data.objects:
		if obj.getType() == 'Mesh':
			mesh = obj.getData()

			out.write('/* THIS IS A GENERATED FILE */\n\n')
			
			out.write('import java.nio.FloatBuffer;\n')
			out.write('import java.nio.ShortBuffer;\n')
			out.write
			out.write('class %s extends FigureData {\n' % name)

			# Boundary computations
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
			out.write('\t@Override public float getMinX() { return %ff; }\n' % minx);
			out.write('\t@Override public float getMaxX() { return %ff; }\n' % maxx);
			out.write('\t@Override public float getMinY() { return %ff; }\n' % miny);
			out.write('\t@Override public float getMaxY() { return %ff; }\n' % maxy);
			out.write('\t@Override public float getMinZ() { return %ff; }\n' % minz);
			out.write('\t@Override public float getMaxZ() { return %ff; }\n' % maxz);

			# Write out vertexes
			count = len(mesh.verts)

			out.write('\n')
			out.write('\t@Override\n')
			out.write('\tFloatData getVertexData() {\n');
			out.write('\t\tclass VertexData implements FloatData {\n')
			out.write('\t\t\tpublic int size() { return %d; }\n\n' % (count*3))
			out.write('\t\t\tpublic void fill(FloatBuffer buf) {\n')
		
			megaMax = 1700
			max = megaMax
			if count > max:
				num = int(math.ceil(count / max))
				
				for x in range(num):
					out.write('\t\t\t\tfill%d(buf);\n' % (x+1))
				out.write('\t\t\t}\n')
				out.write
				for x in range(num):
					out.write
					out.write('\t\t\tvoid fill%d(FloatBuffer buf) {\n' % (x+1))
					start = x * max
					end = start + max
					for vert in mesh.verts[start:end]:
						out.write('\t\t\t\tbuf.put(%ff).put(%ff).put(%ff); /* %d */\n' % (vert.co.x, vert.co.y, vert.co.z, vert.index))
 					out.write('\t\t\t};\n')
			else:
				for vert in mesh.verts:
					out.write('\t\t\t\tbuf.put(%ff).put(%ff).put(%ff); /* %d */\n' % (vert.co.x, vert.co.y, vert.co.z, vert.index))
 				out.write('\t\t\t};\n')

			out.write('\t\t};\n')
			out.write('\t\treturn new VertexData();\n')
			out.write('\t};\n')
			out.write('\n')

			# Write out normals
			out.write('\t@Override\n')
			out.write('\tFloatData getNormalData() {\n');
			out.write('\t\tclass NormalData implements FloatData {\n')
 			out.write('\t\t\tpublic int size() { return %d; }\n\n' % (count*3))
			out.write('\t\t\tpublic void fill(FloatBuffer buf) {\n')

			if count > max:
				num = int(math.ceil(count / max))
				
				for x in range(num):
					out.write('\t\t\t\tfill%d(buf);\n' % (x+1))
				out.write('\t\t\t}\n')
				out.write
				for x in range(num):
					out.write
					out.write('\t\t\tvoid fill%d(FloatBuffer buf) {\n' % (x+1))
					start = x * max
					end = start + max
					for vert in mesh.verts[start:end]:
						out.write('\t\t\t\tbuf.put(%ff).put(%ff).put(%ff); /* %d */\n' % (vert.no.x, vert.no.y, vert.no.z, vert.index))
 					out.write('\t\t\t};\n')
			else:
				for vert in mesh.verts:
					out.write('\t\t\t\tbuf.put(%ff).put(%ff).put(%ff); /* %d */\n' % (vert.no.x, vert.no.y, vert.no.z, vert.index))
 				out.write('\t\t\t};\n')
			out.write('\t\t};\n')
			out.write('\t\treturn new NormalData();\n')
			out.write('\t};\n')
			out.write('\n')

			# Write out indexes
			out.write('\t@Override\n')
			out.write('\tShortData getIndexData() {\n');
			out.write('\t\tclass IndexData implements ShortData {\n');

			max = int(max/3)
			count = len(mesh.faces)

			out.write('\t\t\tpublic int size() { return %d; }\n\n' % (count*3))
			out.write('\t\t\tpublic void fill(ShortBuffer buf) {\n');

			if count > max:
				num = int(math.ceil(float(count) / float(max)))
				for x in range(num):
					out.write('\t\t\t\tfill%d(buf);\n' % (x+1))
				out.write('\t\t\t}\n')
				out.write

				cCount = 0
				for x in range(num):
					out.write
					out.write('\t\t\tvoid fill%d(ShortBuffer buf) {\n' % (x+1))
					start = int(x * max)
					end = int(start + max)
					for face in mesh.faces[start:end]:
						out.write('\t\t\t\tbuf')
						for vert in face.v:
							out.write('.put((short)%d)' % vert.index)
						out.write('; /* %d */\n' % (cCount))
						cCount = cCount + 1
					out.write('\t\t\t};\n')
			else:
				cCount = 0
				for face in mesh.faces:
					out.write('\t\t\t\tbuf')
					for vert in face.v:
						out.write('.put((short)%d)' % vert.index)
					out.write('; /* %d */\n' % (cCount))
					cCount = cCount + 1
      				out.write('\t\t\t};\n')

			out.write('\t\t};\n')
			out.write('\t\treturn new IndexData();\n')
			out.write('\t};\n')
			out.write('\n')

			# Write out colors
			if mesh.hasVertexColours():
				max = int(megaMax/4)
				count = len(mesh.faces)

				out.write('\t@Override\n')
				out.write('\tShortData getColorData() {\n');
				out.write('\t\tclass ColorData implements ShortData {\n');
				out.write('\t\t\tpublic int size() { return %d; }\n\n' % (count*4))
				out.write('\t\t\tpublic void fill(ShortBuffer buf) {\n');

				if count > max:
					num = int(math.ceil(float(count) / float(max)))
					for x in range(num):
						out.write('\t\t\t\tfill%d(buf);\n' % (x+1))
					out.write('\t\t\t}\n')
					out.write

					cCount = 0
					for x in range(num):
						out.write
						out.write('\t\t\tvoid fill%d(ShortBuffer buf) {\n' % (x+1))
						start = int(x * max)
						end = int(start + max)
						for face in mesh.faces[start:end]:
							for col in mesh.col:
								out.write('\t\t\t\tbuf.put((short)%d).put((short)%d).put((short)%d).put((short)%d); /* %d */\n' % 
									(col.r, col.g, col.b, col.a, cCount))
								cCount = cCount + 1
						out.write('\t\t\t};\n')
				else:
					cCount = 0
					for face in mesh.faces:
						for col in mesh.col:
							out.write('\t\t\t\tbuf.put((short)%d).put((short)%d).put((short)%d).put((short)%d); /* %d */\n' % 
								(col.r, col.g, col.b, col.a, cCount))
							cCount = cCount + 1
      					out.write('\t\t\t};\n')

				out.write('\t\t};\n')
				out.write('\t\treturn new ColorData();\n')
				out.write('\t};\n')
			out.write('};\n')
       
			#for name in mesh.getVertGroupNames():
			#	print 'Group ' + name + ':'
			#	print mesh.getVertsFromGroup(name)
		else:
			print "Skipping %s" % (obj.getType())

	out.close()			
				
name = os.path.splitext(Blender.Get('filename'))[0]
Blender.Window.FileSelector(write_obj, "Export", '%s.java' % name)
      
    
