extends Node

const HTerrain = preload("res://addons/zylann.hterrain/hterrain.gd")
const HTerrainData = preload("res://addons/zylann.hterrain/hterrain_data.gd")
const HTerrainTextureSet = preload("res://addons/zylann.hterrain/hterrain_texture_set.gd")

func _ready():
	var data = HTerrainData.new()
	var size = 513
	data.resize(size)

	var terrain = HTerrain.new()
	terrain.set_data(data)
	terrain.lod_scale = 2.0
	terrain.set_chunk_size(32)
	terrain._collision_enabled = true
	terrain._collision_layer = 1
	terrain._collision_mask = 1
	terrain.set_shader_type(HTerrain.SHADER_CLASSIC4_LITE)
	
	var textureSet = HTerrainTextureSet.new()
	
	terrain.set_texture_set()
#texture_set = ExtResource( 2 )
#render_layers = 1
#shader_params/u_ground_uv_scale = 20
#shader_params/u_depth_blending = true
#shader_params/u_triplanar = false
#shader_params/u_tile_reduction = Plane( 1, 1, 1, 1 )
	add_child(terrain)
	
	# Get the image
	var colormap : Image = data.get_image(HTerrainData.CHANNEL_COLOR)
	# colormap.lock()
	colormap.load("res://t_data/color.png")
	# colormap.unlock()
	
	var heightmap : Image = data.get_image(HTerrainData.CHANNEL_HEIGHT)
	heightmap.load("res://t_data/height.res")
	
	var normalmap : Image = data.get_image(HTerrainData.CHANNEL_NORMAL)
	normalmap.load("res://t_data/normal.png")
	
	var splatmap : Image = data.get_image(HTerrainData.CHANNEL_SPLAT)
	splatmap.load("res://t_data/splat.png")
	
	# Notify the terrain of our change
	data.notify_region_changed(Rect2(0, 0, size, size), HTerrainData.CHANNEL_COLOR)
