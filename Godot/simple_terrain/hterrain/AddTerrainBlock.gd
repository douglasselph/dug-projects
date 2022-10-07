extends Node

const HTerrain = preload("res://addons/zylann.hterrain/hterrain.gd")
const HTerrainData = preload("res://addons/zylann.hterrain/hterrain_data.gd")

func _ready():
	var data = HTerrainData.new()
	var size = 513
	data.resize(size)

	var terrain = HTerrain.new()
	terrain.set_data(data)
	terrain.set_as_toplevel()
	add_child(terrain)
	
	# Get the image
	var colormap : Image = data.get_image(HTerrainData.CHANNEL_COLOR)
	
	# Modify the image
	colormap.lock()
	for x in range(size):
		for y in range(size):
			colormap.set_pixel(x, y, Color(1, 0, 0))
	colormap.unlock()

	# Notify the terrain of our change
	data.notify_region_changed(Rect2(0, 0, size, size), HTerrainData.CHANNEL_COLOR)
